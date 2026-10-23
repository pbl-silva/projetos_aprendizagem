package br.com.spbank.cartao.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.autenticacao.application.model.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.in.*;
import br.com.spbank.cartao.application.port.out.*;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

class CardRenewalServiceTest {
    private final UUID customerId=UUID.randomUUID(), oldId=UUID.randomUUID();
    private final Map<UUID,CardInstrument> stored=new ConcurrentHashMap<>();
    private final CardInstrumentPersistence cards=mock(CardInstrumentPersistence.class);
    private final AuthenticationPersistence customers=mock(AuthenticationPersistence.class);
    private final CardCredentialVault vault=mock(CardCredentialVault.class);
    private final CardCreditAllocationPersistence lines=mock(CardCreditAllocationPersistence.class);
    private final Instant now=Instant.parse("2026-10-01T12:00:00Z");
    private static final String PASSWORD_HASH=PasswordHasher.hash("Senha@123");
    private final Customer customer=new Customer(customerId,"Cliente","52998224725",LocalDate.of(1990,1,1),
        "11987654321","cliente@example.com",null,CustomerPlan.STANDARD,true);

    @BeforeEach void setUp() {
        when(cards.ownerId(any())).thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(0))).map(row -> java.util.Objects.requireNonNull(row).customerId()));
        when(cards.findForUpdate(eq(customerId),any())).thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(1))));
        when(cards.cards(customerId)).thenAnswer(i -> List.copyOf(stored.values()));
        when(cards.successor(any())).thenAnswer(i -> stored.values().stream().filter(c -> Objects.equals(c.previousCardId(),i.getArgument(0))).findFirst());
        doAnswer(i -> {CardInstrument card=i.getArgument(0); stored.put(card.id(),card); return null;}).when(cards).save(any());
        when(customers.findCustomerByIdForUpdate(customerId)).thenReturn(Optional.of(customer));
        when(customers.findCustomerById(customerId)).thenReturn(Optional.of(customer));
        when(customers.findCredentialByCustomerId(customerId)).thenReturn(Optional.of(new AccessCredential(customerId,"cliente",PASSWORD_HASH,true)));
        when(vault.issue(CardBrand.BRISA)).thenAnswer(i -> new CardCredentials("cipher-pan","cipher-cvv",UUID.randomUUID().toString(),"4321"));
        when(lines.lines(customerId)).thenReturn(List.of(new CardCreditLine(CardBrand.BRISA,new BigDecimal("1000.00"),new BigDecimal("200.00"))));
        stored.put(oldId,old(LocalDate.of(2026,10,31),CardState.ACTIVE));
    }

    @Test void startsExactlyThirtyDaysBeforeExpiryAndNotAtThirtyOne() {
        lifecycle(Instant.parse("2026-09-30T12:00:00Z")).process(oldId);
        assertThat(stored).hasSize(1);
        lifecycle(now).process(oldId);
        assertThat(stored).hasSize(2);
        assertThat(successor().state()).isEqualTo(CardState.PENDING_ACTIVATION);
        assertThat(stored.get(oldId).state()).isEqualTo(CardState.ACTIVE);
        verify(lines,never()).saveLines(any(),any());
    }
    @Test void renewalIsRepeatedSafelyAndGetsNewCredentialsWithoutAutomaticActivation() {
        lifecycle(now).process(oldId); var first=successor();
        lifecycle(now).process(oldId);
        assertThat(successor()).isEqualTo(first); assertThat(stored).hasSize(2);
        assertThat(first.encryptedPan()).isEqualTo("cipher-pan"); assertThat(first.pinHash()).isNull();
        assertThat(first.customerId()).isEqualTo(customerId); assertThat(first.brand()).isEqualTo(CardBrand.BRISA);
        verify(vault,times(1)).issue(CardBrand.BRISA);
    }
    @Test void renewalDoesNotDependOnTheLineStillHavingANonzeroAllocation() {
        when(lines.lines(customerId)).thenReturn(List.of());
        lifecycle(now).process(oldId);
        assertThat(stored).hasSize(2); verifyNoInteractions(lines);
    }
    @Test void terminalCardsAndPendingCardsAreNotRenewed() {
        for(var state:List.of(CardState.CANCELLED,CardState.REPLACED,CardState.EXPIRED,CardState.PENDING_ACTIVATION)) {
            stored.put(oldId,old(LocalDate.of(2026,10,31),state)); lifecycle(now).process(oldId);
        }
        assertThat(stored).hasSize(1); verifyNoInteractions(vault);
    }
    @Test void expiryIsPersistedOnlyAfterLocalEndOfMonthAndDoesNotIssueAfterMissingTheWindow() {
        lifecycle(Instant.parse("2026-11-01T02:59:59Z")).process(oldId);
        assertThat(stored.get(oldId).state()).isEqualTo(CardState.ACTIVE);
        lifecycle(Instant.parse("2026-11-01T03:00:00Z")).process(oldId);
        assertThat(stored.get(oldId).state()).isEqualTo(CardState.EXPIRED);
        assertThat(successor().state()).isEqualTo(CardState.PENDING_ACTIVATION);
        stored.clear(); stored.put(oldId,old(LocalDate.of(2026,10,31),CardState.ACTIVE));
        lifecycle(Instant.parse("2026-11-01T03:00:00Z")).process(oldId);
        assertThat(stored).hasSize(1); assertThat(stored.get(oldId).state()).isEqualTo(CardState.EXPIRED);
    }
    @Test void pendingInstrumentsAlsoExpireWithoutProducingSuccessors() {
        stored.put(oldId,old(LocalDate.of(2026,9,30),CardState.PENDING_ACTIVATION)); lifecycle(now).process(oldId);
        assertThat(stored.get(oldId).state()).isEqualTo(CardState.EXPIRED); assertThat(stored).hasSize(1);
    }
    @Test void activationReplacesTheOldCardBeforeSavingTheNewActiveInstrument() {
        lifecycle(now).process(oldId); var next=successor(); clearInvocations(cards);
        var result=instrument().activate(customerId,next.id(),"Senha@123","0012","0012");
        assertThat(result.state()).isEqualTo(CardState.ACTIVE);
        assertThat(stored.get(oldId).state()).isEqualTo(CardState.REPLACED);
        var order=inOrder(cards);
        order.verify(cards).save(argThat(c -> c.id().equals(oldId)&&c.state()==CardState.REPLACED));
        order.verify(cards).save(argThat(c -> c.id().equals(next.id())&&c.state()==CardState.ACTIVE));
        verify(lines,never()).saveLines(any(),any());
    }
    @Test void blocksAppliedAfterRenewalAreStillPresentAtActivation() {
        lifecycle(now).process(oldId); var next=successor();
        stored.put(oldId,stored.get(oldId).withBlocks(true,true,"Perda","Análise"));
        var result=instrument().activate(customerId,next.id(),"Senha@123","0012","0012");
        assertThat(result.voluntaryBlocked()).isTrue(); assertThat(result.administrativeBlocked()).isTrue();
    }
    @Test void cancellingThePendingRenewalAndReplacingItStillDeactivatesTheLiveAncestor() {
        lifecycle(now).process(oldId); var pending=successor();
        var service=instrument(); service.cancel(customerId,pending.id());
        var replacement=service.replace(customerId,pending.id());
        service.activate(customerId,replacement.id(),"Senha@123","0012","0012");
        assertThat(stored.get(oldId).state()).isEqualTo(CardState.REPLACED);
        assertThat(stored.get(pending.id()).state()).isEqualTo(CardState.CANCELLED);
        assertThat(stored.get(replacement.id()).state()).isEqualTo(CardState.ACTIVE);
    }
    @Test void oldReplacedAncestorsDoNotResurrectHistoricalBlocks() {
        lifecycle(now).process(oldId); var next=successor();
        stored.put(oldId,stored.get(oldId).withBlocks(true,true,"Antigo","Antigo"));
        instrument().activate(customerId,next.id(),"Senha@123","0012","0012");
        stored.put(next.id(),stored.get(next.id()).withBlocks(false,false,null,null));
        lifecycle(Instant.parse("2029-10-01T12:00:00Z")).process(next.id());
        var third=cards.successor(next.id()).orElseThrow();
        var later=new CardInstrumentService(cards,lines,customers,mock(AdministrationPersistence.class),vault,
            Clock.fixed(Instant.parse("2029-10-01T12:00:00Z"),ZoneOffset.UTC),"America/Sao_Paulo");
        var result=later.activate(customerId,third.id(),"Senha@123","0012","0012");
        assertThat(result.administrativeBlocked()).isFalse(); assertThat(result.voluntaryBlocked()).isFalse();
    }
    @Test void technicalFailureDuringActivationRollsBackBothInstrumentStatesThroughSpring() {
        lifecycle(now).process(oldId); var next=successor();
        var snapshot=new HashMap<>(stored);
        var manager=java.util.Objects.requireNonNull(mock(PlatformTransactionManager.class)); var status=new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        java.util.Objects.requireNonNull(doAnswer(i -> {stored.clear();stored.putAll(snapshot);return null;}).when(manager)).rollback(status);
        doAnswer(i -> {CardInstrument c=i.getArgument(0); if(c.id().equals(next.id())&&c.state()==CardState.ACTIVE)throw new IllegalStateException("Falha técnica"); stored.put(c.id(),c);return null;}).when(cards).save(any());
        var proxy=proxy(instrument(),manager,CardInstrumentUseCase.class);
        assertThatThrownBy(() -> proxy.activate(customerId,next.id(),"Senha@123","0012","0012")).isInstanceOf(IllegalStateException.class);
        java.util.Objects.requireNonNull(verify(manager)).rollback(status); assertThat(stored).isEqualTo(snapshot);
    }
    @Test void concurrentWorkersSerializeOnTheCustomerAndCreateOnlyOneSuccessor() throws Exception {
        var lock=new ReentrantLock(); var status=new ThreadLocal<TransactionStatus>();
        var manager=java.util.Objects.requireNonNull(mock(PlatformTransactionManager.class));
        when(manager.getTransaction(any())).thenAnswer(i -> {var value=new SimpleTransactionStatus();status.set(value);return value;});
        when(customers.findCustomerByIdForUpdate(customerId)).thenAnswer(i -> {lock.lock();return Optional.of(customer);});
        java.util.Objects.requireNonNull(doAnswer(i -> {if(lock.isHeldByCurrentThread())lock.unlock();status.remove();return null;}).when(manager)).commit(br.com.spbank.MockitoArguments.nonNullMarker(any()));
        java.util.Objects.requireNonNull(doAnswer(i -> {if(lock.isHeldByCurrentThread())lock.unlock();status.remove();return null;}).when(manager)).rollback(br.com.spbank.MockitoArguments.nonNullMarker(any()));
        var worker=proxy(lifecycle(now),manager,ProcessCardLifecycleUseCase.class);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            var first=pool.submit(() -> {await(gate);worker.process(oldId);});
            var second=pool.submit(() -> {await(gate);worker.process(oldId);});
            gate.countDown();first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
        }
        assertThat(stored).hasSize(2);verify(vault,times(1)).issue(CardBrand.BRISA);
    }
    @Test void customerIsLockedBeforeInstrumentAndEachExecutionRequestsANewTransaction() {
        var manager=java.util.Objects.requireNonNull(mock(PlatformTransactionManager.class));
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        proxy(lifecycle(now),manager,ProcessCardLifecycleUseCase.class).process(oldId);
        var order=inOrder(customers,cards);
        order.verify(cards).ownerId(oldId);
        order.verify(customers).findCustomerByIdForUpdate(customerId);
        order.verify(cards).findForUpdate(customerId,oldId);
        java.util.Objects.requireNonNull(verify(manager)).getTransaction(argThat(def -> java.util.Objects.requireNonNull(def).getPropagationBehavior()==org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }
    private static void await(CountDownLatch gate) {try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
    private static <T> T proxy(Object service,PlatformTransactionManager manager,Class<T> type) {
        var interceptor=new TransactionInterceptor();interceptor.setTransactionManager(java.util.Objects.requireNonNull(manager));interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory=new ProxyFactory(java.util.Objects.requireNonNull(service));factory.addAdvice(interceptor);return type.cast(factory.getProxy());
    }
    private CardInstrument successor(){return cards.successor(oldId).orElseThrow();}
    private ProcessCardLifecycleService lifecycle(Instant instant){return new ProcessCardLifecycleService(cards,customers,vault,Clock.fixed(instant,ZoneOffset.UTC),"America/Sao_Paulo");}
    private CardInstrumentService instrument(){return new CardInstrumentService(cards,lines,customers,mock(AdministrationPersistence.class),vault,Clock.fixed(now,ZoneOffset.UTC),"America/Sao_Paulo");}
    private CardInstrument old(LocalDate expiry,CardState state){return new CardInstrument(oldId,customerId,CardBrand.BRISA,null,"Cliente","1234","old-hash","old-pan","old-cvv",PASSWORD_HASH,state,0,false,false,false,null,null,expiry,now.minus(Duration.ofDays(1000)));}
}

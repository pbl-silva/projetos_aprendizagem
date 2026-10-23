package br.com.spbank.cartao.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.administracao.application.model.AdministratorCredential;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.autenticacao.application.model.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.cartao.application.exception.CardPinException;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.in.CardInstrumentUseCase;
import br.com.spbank.cartao.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

class CardInstrumentServiceTest {
    private final UUID customerId = UUID.randomUUID(), adminId = UUID.randomUUID();
    private final Map<UUID, CardInstrument> stored = new LinkedHashMap<>();
    private final List<CardBlockAudit> audits = new ArrayList<>();
    private final AuthenticationPersistence customers = mock(AuthenticationPersistence.class);
    private final AdministrationPersistence admins = mock(AdministrationPersistence.class);
    private final CardInstrumentPersistence cards = mock(CardInstrumentPersistence.class);
    private final CardCreditAllocationPersistence lines = mock(CardCreditAllocationPersistence.class);
    private final CardCredentialVault vault = mock(CardCredentialVault.class);
    private static final String PASSWORD_HASH = PasswordHasher.hash("Senha@123");
    private final Instant now = Instant.parse("2026-10-23T12:00:00Z");
    private CardInstrumentService service;

    @BeforeEach void setUp() {
        service = at(now);
        var customer = new Customer(customerId, "Cliente", "52998224725", LocalDate.of(1990,1,1),
            "11987654321", "cliente@example.com", null, CustomerPlan.STANDARD, true);
        when(customers.findCustomerById(customerId)).thenReturn(Optional.of(customer));
        when(customers.findCustomerByIdForUpdate(customerId)).thenReturn(Optional.of(customer));
        when(customers.findCredentialByCustomerId(customerId)).thenReturn(Optional.of(new AccessCredential(customerId,"cliente", PASSWORD_HASH,true)));
        when(admins.findCredentialById(adminId)).thenReturn(Optional.of(new AdministratorCredential(adminId,"Gerente","gerente","hash",true)));
        when(lines.lines(customerId)).thenReturn(List.of(new CardCreditLine(CardBrand.BRISA,new BigDecimal("2000.00"),new BigDecimal("200.00"))));
        when(cards.cards(customerId)).thenAnswer(i -> List.copyOf(stored.values()));
        when(cards.find(eq(customerId),any())).thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(1))));
        when(cards.findForUpdate(eq(customerId),any())).thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(1))));
        when(cards.successor(any())).thenAnswer(i -> stored.values().stream().filter(c -> Objects.equals(c.previousCardId(),i.getArgument(0))).findFirst());
        doAnswer(i -> {CardInstrument c=i.getArgument(0);stored.put(c.id(),c);return null;}).when(cards).save(any());
        doAnswer(i -> {audits.add(i.getArgument(0));return null;}).when(cards).audit(any());
        when(vault.issue(any())).thenAnswer(i -> new CardCredentials("encrypted-pan","encrypted-cvv",UUID.randomUUID().toString(),"1234"));
        when(vault.reveal(any())).thenAnswer(i -> {CardInstrument c=i.getArgument(0);return new CardDetails(c.holderName(),"9901000000001234","007",c.expiresOn());});
    }

    @Test void issuanceNeedsAllocatedLineAndStartsPendingWithoutChangingDebt() {
        var result = service.issue(customerId,CardBrand.BRISA);
        assertThat(result.state()).isEqualTo(CardState.PENDING_ACTIVATION);
        assertThat(result.expiresOn()).isEqualTo(LocalDate.of(2029,10,31));
        assertThat(result.committed()).isEqualByComparingTo("200");
        assertThatThrownBy(() -> service.issue(customerId,CardBrand.ELU)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.issue(customerId,CardBrand.BRISA)).isInstanceOf(BusinessException.class);
        verify(lines,never()).saveLines(any(),any());
    }
    @Test void activationHashesPinPreservingInitialZeroAndRequiresMatchingConfirmation() {
        var card=service.issue(customerId,CardBrand.BRISA);
        assertThatThrownBy(() -> service.activate(customerId,card.id(),"Senha@123","0012","0013")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.activate(customerId,card.id(),"errada","0012","0012")).isInstanceOf(BusinessException.class);
        service.activate(customerId,card.id(),"Senha@123","0012","0012");
        var persisted=stored.get(card.id());
        assertThat(persisted.state()).isEqualTo(CardState.ACTIVE);
        assertThat(persisted.pinHash()).isNotEqualTo("0012");
        assertThat(PasswordHasher.matches("0012",persisted.pinHash())).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"123","12345","１２３４","12a4"})
    void pinMustHaveExactlyFourAsciiDigits(String pin) {
        var card=service.issue(customerId,CardBrand.BRISA);
        assertThatThrownBy(() -> service.activate(customerId,card.id(),"Senha@123",pin,pin)).isInstanceOf(BusinessException.class);
    }
    @Test void newPinCannotEqualLoginPassword() {
        var hash=PasswordHasher.hash("0012");
        when(customers.findCredentialByCustomerId(customerId)).thenReturn(Optional.of(new AccessCredential(customerId,"cliente",hash,true)));
        var card=service.issue(customerId,CardBrand.BRISA);
        assertThatThrownBy(() -> service.activate(customerId,card.id(),"0012","0012","0012")).isInstanceOf(BusinessException.class);
    }
    @Test void threeConsecutiveErrorsBlockAndNoFurtherSecretIsRevealed() {
        var card=active();
        for(int i=0;i<3;i++) assertThatThrownBy(() -> service.details(customerId,card.id(),"9999")).isInstanceOf(CardPinException.class);
        assertThat(stored.get(card.id()).pinFailures()).isEqualTo(3);
        assertThat(stored.get(card.id()).pinBlocked()).isTrue();
        assertThatThrownBy(() -> service.details(customerId,card.id(),"0012")).isInstanceOf(CardPinException.class);
        verify(vault,never()).reveal(any());
    }
    @Test void correctPinResetsConsecutiveFailures() {
        var card=active();
        assertThatThrownBy(() -> service.details(customerId,card.id(),"9999")).isInstanceOf(CardPinException.class);
        assertThat(service.details(customerId,card.id(),"0012").cvv()).isEqualTo("007");
        assertThat(stored.get(card.id()).pinFailures()).isZero();
    }
    @Test void normalPinChangeNeedsCurrentPin() {
        var card=active();
        assertThatThrownBy(() -> service.changePin(customerId,card.id(),"9999","0007","0007")).isInstanceOf(CardPinException.class);
        service.changePin(customerId,card.id(),"0012","0007","0007");
        assertThat(PasswordHasher.matches("0007",stored.get(card.id()).pinHash())).isTrue();
        assertThat(stored.get(card.id()).pinFailures()).isZero();
    }
    @Test void recoveryRemovesOnlyPinBlockAndPreservesOtherCauses() {
        var card=active(); service.block(customerId,card.id(),"Perda");
        service.administrativeBlock(adminId,customerId,card.id(),"ADMINISTRATIVE",true,"Análise");
        for(int i=0;i<3;i++) assertThatThrownBy(() -> service.details(customerId,card.id(),"9999")).isInstanceOf(CardPinException.class);
        var recovered=service.recoverPin(customerId,card.id(),"Senha@123","0007","0007");
        assertThat(recovered.pinBlocked()).isFalse();
        assertThat(recovered.voluntaryBlocked()).isTrue();
        assertThat(recovered.administrativeBlocked()).isTrue();
    }
    @Test void administrativeUnblocksAreIndependentAndAudited() {
        var card=active(); service.block(customerId,card.id(),"Perda");
        service.administrativeBlock(adminId,customerId,card.id(),"ADMINISTRATIVE",true,"Análise");
        var result=service.administrativeBlock(adminId,customerId,card.id(),"VOLUNTARY",false,"Identidade confirmada");
        assertThat(result.voluntaryBlocked()).isFalse(); assertThat(result.administrativeBlocked()).isTrue();
        result=service.administrativeBlock(adminId,customerId,card.id(),"ADMINISTRATIVE",false,"Análise concluída");
        assertThat(result.administrativeBlocked()).isFalse();
        assertThat(audits).hasSize(4); assertThat(audits.getLast().actorId()).isEqualTo(adminId);
    }
    @Test void cancelledInstrumentCannotBeActivatedOrRevealedAndReplacementSharesLine() {
        var card=active(); service.block(customerId,card.id(),"Perda");
        service.administrativeBlock(adminId,customerId,card.id(),"ADMINISTRATIVE",true,"Análise");
        service.cancel(customerId,card.id());
        assertThatThrownBy(() -> service.details(customerId,card.id(),"0012")).isInstanceOf(BusinessException.class);
        var successor=service.replace(customerId,card.id());
        assertThat(successor.id()).isNotEqualTo(card.id());
        assertThat(successor.state()).isEqualTo(CardState.PENDING_ACTIVATION);
        assertThat(successor.voluntaryBlocked()).isTrue(); assertThat(successor.administrativeBlocked()).isTrue();
        assertThat(successor.committed()).isEqualByComparingTo("200");
        assertThat(service.replace(customerId,card.id()).id()).isEqualTo(successor.id());
        assertThat(stored).hasSize(2);
        assertThatThrownBy(() -> service.activate(customerId,card.id(),"Senha@123","1234","1234")).isInstanceOf(BusinessException.class);
        verify(lines,never()).saveLines(any(),any());
    }
    @Test void expiryUsesEndOfMonthInSaoPaulo() {
        var card=active();
        assertThat(at(Instant.parse("2029-11-01T02:59:59Z")).cards(customerId).getFirst().state()).isEqualTo(CardState.ACTIVE);
        var expired=at(Instant.parse("2029-11-01T03:00:00Z"));
        assertThat(expired.cards(customerId).getFirst().state()).isEqualTo(CardState.EXPIRED);
        assertThatThrownBy(() -> expired.details(customerId,card.id(),"0012")).isInstanceOf(BusinessException.class);
    }
    @Test void anotherCustomerOrUnknownAdministratorCannotAccessCard() {
        var card=active(); var other=UUID.randomUUID();
        assertThatThrownBy(() -> service.details(other,card.id(),"0012")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.administrativeCards(other,customerId)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.details(customerId,other,"0012")).isInstanceOf(NotFoundException.class);
    }
    @Test void summaryAndSensitiveModelsHaveSafeStringRepresentations() {
        var card=active();
        assertThat(stored.get(card.id()).toString()).doesNotContain("encrypted-pan","encrypted-cvv",stored.get(card.id()).pinHash());
        assertThat(service.cards(customerId).toString()).doesNotContain("encrypted-pan","encrypted-cvv");
        assertThat(service.details(customerId,card.id(),"0012").toString()).doesNotContain("9901000000001234","007");
    }
    @Test void springTransactionCommitsPinRefusalButRollsBackOtherBusinessErrors() {
        var card=active();
        var manager=java.util.Objects.requireNonNull(mock(PlatformTransactionManager.class));
        var transaction=new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(transaction);
        var interceptor=new TransactionInterceptor();interceptor.setTransactionManager(java.util.Objects.requireNonNull(manager));interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory=new ProxyFactory(java.util.Objects.requireNonNull(service));
        factory.addAdvice(interceptor);
        var proxy=(CardInstrumentUseCase)factory.getProxy();
        assertThatThrownBy(() -> proxy.details(customerId,card.id(),"9999")).isInstanceOf(CardPinException.class);
        java.util.Objects.requireNonNull(verify(manager)).commit(transaction); java.util.Objects.requireNonNull(verify(manager,never())).rollback(br.com.spbank.MockitoArguments.nonNullMarker(any()));
        clearInvocations(manager);
        assertThatThrownBy(() -> proxy.recoverPin(customerId,card.id(),"errada","0012","0012")).isInstanceOf(BusinessException.class);
        java.util.Objects.requireNonNull(verify(manager)).rollback(transaction); java.util.Objects.requireNonNull(verify(manager,never())).commit(br.com.spbank.MockitoArguments.nonNullMarker(any()));
    }

    private CardSummary active() {
        var card=service.issue(customerId,CardBrand.BRISA);
        return service.activate(customerId,card.id(),"Senha@123","0012","0012");
    }
    private CardInstrumentService at(Instant instant) {
        return new CardInstrumentService(cards,lines,customers,admins,vault,Clock.fixed(instant,ZoneOffset.UTC),"America/Sao_Paulo");
    }
}

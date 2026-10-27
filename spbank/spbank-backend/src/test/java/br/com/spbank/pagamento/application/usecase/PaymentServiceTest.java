package br.com.spbank.pagamento.application.usecase;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.pagamento.application.model.PaymentModels.*;
import br.com.spbank.pagamento.application.port.out.*;
import br.com.spbank.pagamento.application.service.*;
import br.com.spbank.autenticacao.application.model.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
class PaymentServiceTest {
    static final String HASH=PasswordHasher.hash("Senha@123");
    final UUID customer=UUID.randomUUID(),accountId=UUID.randomUUID(),productId=UUID.randomUUID();
    final AuthenticationPersistence auth=mock(AuthenticationPersistence.class);
    final AccountPersistence accounts=mock(AccountPersistence.class);
    final AccountEntryPersistence entries=mock(AccountEntryPersistence.class);
    final PaymentSettlement settlement=mock(PaymentSettlement.class);
    final Memory store=new Memory();
    final MutableClock clock=new MutableClock();
    Account account;
    PaymentService service;
    GeneratedCharge generated;
    @BeforeEach void setup(){
        var c=new Customer(customer,"Cliente","11144477735",LocalDate.of(1990,1,1),"11999999999","x@example.test",null,CustomerPlan.STANDARD,true);
        when(auth.findCustomerById(customer)).thenReturn(Optional.of(c));
        when(auth.findCustomerByIdForUpdate(customer)).thenReturn(Optional.of(c));
        when(auth.findCredentialByCustomerId(customer)).thenReturn(Optional.of(new AccessCredential(customer,"cliente",HASH,true)));
        account=new Account(accountId,customer,"Cliente","11144477735","90001","0001","123456",AccountType.CURRENT,new BigDecimal("1000.00"),true);
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(accounts.findAllForUpdate(any())).thenAnswer(i->Map.of(accountId,account));
        when(settlement.settle(any(),any(),any(),any())).thenReturn("STUDY-REFERENCE");
        store.catalog.add(new Product(productId,"Escuro",new BigDecimal("30.00"),true));
        service=new PaymentService(store,settlement,auth,accounts,entries,clock);
        generated=service.generate(customer,"Luz",new BigDecimal("40.00"));
    }
    Command command(LocalDate date){return new Command(accountId,generated.code(),date,"Senha@123");}
    @Test void generatorProducesValidCodeAndNoFinancialEffects(){
        assertThat(PaymentCodeParser.parse(generated.code())).hasSize(44);
        assertThat(generated.charge().dueDate()).isEqualTo(LocalDate.of(2026,11,26));
        assertThat(generated.charge().mask()).doesNotContain(generated.code());
        assertThat(store.rows).isEmpty();verifyNoInteractions(entries,settlement);
    }
    @Test void previewDoesNotDebit(){
        var p=service.preview(customer,accountId,generated.code(),null);
        assertThat(p.amount()).isEqualByComparingTo("40");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
        assertThat(store.rows).isEmpty();verifyNoInteractions(entries);
    }
    @Test void immediateIsExactlyOnce(){
        var key=UUID.randomUUID();var first=service.pay(customer,key,command(null));
        assertThat(service.pay(customer,key,command(null))).isEqualTo(first);
        assertThat(account.getBalance()).isEqualByComparingTo("960");
        assertThat(first.status()).isEqualTo(Status.COMPLETED);verify(entries,times(1)).saveAll(any());
    }
    @Test void keyConflictNeverDebitsAgain(){
        var key=UUID.randomUUID();service.pay(customer,key,command(null));
        assertThatExceptionOfType(IdempotencyConflictException.class).isThrownBy(()->service.pay(customer,key,command(LocalDate.of(2026,10,28))));
        assertThat(account.getBalance()).isEqualByComparingTo("960");
    }
    @Test void immediateReplaySurvivesChangeOfDay(){
        var key=UUID.randomUUID();var first=service.pay(customer,key,command(null));
        clock.day="2026-10-28T15:00:00Z";
        assertThat(service.pay(customer,key,command(null))).isEqualTo(first);
        assertThat(account.getBalance()).isEqualByComparingTo("960");
        verify(entries,times(1)).saveAll(any());
    }
    @Test void wrongPasswordDoesNotConsumeKey(){
        var key=UUID.randomUUID();
        assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.pay(customer,key,new Command(accountId,generated.code(),null,"errada")));
        assertThat(store.rows).isEmpty();assertThat(service.pay(customer,key,command(null)).status()).isEqualTo(Status.COMPLETED);
    }
    @Test void duplicateChargeIsRejectedAcrossKeys(){
        service.pay(customer,UUID.randomUUID(),command(null));
        assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.pay(customer,UUID.randomUUID(),command(null)));
        assertThat(account.getBalance()).isEqualByComparingTo("960");
    }
    @Test void scheduledHasNoReservationAndCanBeCancelled(){
        var p=service.pay(customer,UUID.randomUUID(),command(LocalDate.of(2026,10,28)));
        assertThat(p.status()).isEqualTo(Status.SCHEDULED);
        assertThat(account.getBalance()).isEqualByComparingTo("1000");verifyNoInteractions(entries,settlement);
        assertThat(service.cancel(customer,p.id()).status()).isEqualTo(Status.CANCELLED);
        assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.cancel(customer,p.id()));
    }
    @Test void schedulerRevalidatesAndDebitsOnce(){
        var p=service.pay(customer,UUID.randomUUID(),command(LocalDate.of(2026,10,28)));
        service.execute(p.id());assertThat(account.getBalance()).isEqualByComparingTo("1000");
        clock.day="2026-10-28T15:00:00Z";service.execute(p.id());service.execute(p.id());
        assertThat(account.getBalance()).isEqualByComparingTo("960");verify(entries,times(1)).saveAll(any());
    }
    @Test void insufficientBalanceBecomesTerminalFailure(){
        var p=service.pay(customer,UUID.randomUUID(),command(LocalDate.of(2026,10,28)));
        account.debit(new BigDecimal("990"));clock.day="2026-10-28T15:00:00Z";service.execute(p.id());
        assertThat(service.get(customer,p.id()).status()).isEqualTo(Status.FAILED);
        assertThat(account.getBalance()).isEqualByComparingTo("10");verifyNoInteractions(entries,settlement);
    }
    @Test void technicalFailureEscapesForTransactionRollback(){
        var p=service.pay(customer,UUID.randomUUID(),command(LocalDate.of(2026,10,28)));
        clock.day="2026-10-28T15:00:00Z";
        when(settlement.settle(any(),any(),any(),any())).thenThrow(new IllegalStateException("unavailable"));
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(()->service.execute(p.id()));
        assertThat(account.getBalance()).isEqualByComparingTo("1000");verifyNoInteractions(entries);
    }
    @Test void invalidDatesAreRejected(){
        assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.preview(customer,accountId,generated.code(),LocalDate.of(2026,10,26)));
        assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.preview(customer,accountId,generated.code(),LocalDate.of(2026,11,27)));
    }
    @Test void ownershipIsChecked(){
        assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.preview(customer,UUID.randomUUID(),generated.code(),null));
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(()->service.get(customer,UUID.randomUUID()));
    }
    @Test void rechargeUsesCatalogValueAndMasksPhone(){
        var key=UUID.randomUUID();var cmd=new RechargeCommand(accountId,productId,"(11) 99999-1234","Senha@123");
        var p=service.recharge(customer,key,cmd);
        assertThat(p.amount()).isEqualByComparingTo("30");assertThat(p.targetMask()).isEqualTo("(11) 9****-1234");
        assertThat(p.fingerprint()).doesNotContain("11999991234");assertThat(service.recharge(customer,key,cmd)).isEqualTo(p);
        assertThat(account.getBalance()).isEqualByComparingTo("970");
    }
    @Test void rechargeChangedPhoneConflicts(){
        var key=UUID.randomUUID();service.recharge(customer,key,new RechargeCommand(accountId,productId,"11999991234","Senha@123"));
        assertThatExceptionOfType(IdempotencyConflictException.class).isThrownBy(()->service.recharge(customer,key,new RechargeCommand(accountId,productId,"11999991235","Senha@123")));
    }
    static class MutableClock extends Clock {
        String day="2026-10-27T15:00:00Z";
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return Instant.parse(day);}
    }
    static class Memory implements PaymentPersistence {
        final Map<String,Charge> charges=new HashMap<>();final Map<UUID,Payment> rows=new HashMap<>();final List<Product> catalog=new ArrayList<>();
        public Optional<Charge> charge(String hash){return Optional.ofNullable(charges.get(hash));}
        public void save(Charge c){charges.put(c.hash(),c);}
        public List<Product> products(){return List.copyOf(catalog);}
        public Optional<Product> product(UUID id){return catalog.stream().filter(p->p.id().equals(id)).findFirst();}
        public List<Payment> payments(UUID id){return rows.values().stream().filter(p->p.customerId().equals(id)).toList();}
        public Optional<UUID> owner(UUID id){return Optional.ofNullable(rows.get(id)).map(p->p.customerId());}
        public List<UUID> due(LocalDate date,int limit){return rows.values().stream().filter(p->p.status()==Status.SCHEDULED&&!p.scheduledFor().isAfter(date)).limit(limit).map(p->p.id()).toList();}
        public void save(Payment p){rows.put(p.id(),p);}
    }
}

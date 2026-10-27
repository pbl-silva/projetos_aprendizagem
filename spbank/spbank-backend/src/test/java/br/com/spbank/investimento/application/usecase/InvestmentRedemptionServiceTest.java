package br.com.spbank.investimento.application.usecase;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.out.*;
import br.com.spbank.investimento.application.service.*;
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
class InvestmentRedemptionServiceTest {
    static final String HASH=PasswordHasher.hash("Senha@123");
    final UUID customer=UUID.randomUUID(),accountId=UUID.randomUUID(),productId=UUID.randomUUID(),positionId=UUID.randomUUID();
    final AuthenticationPersistence auth=mock(AuthenticationPersistence.class);
    final AccountPersistence accounts=mock(AccountPersistence.class);
    final AccountEntryPersistence entries=mock(AccountEntryPersistence.class);
    final InvestmentPersistence positions=mock(InvestmentPersistence.class);
    final InvestmentReferenceData reference=mock(InvestmentReferenceData.class);
    final Memory redemptions=new Memory();final MutableClock clock=new MutableClock();
    InvestmentRedemptionService service;Account account;
    @BeforeEach void setup(){
        var c=new Customer(customer,"Cliente","11144477735",LocalDate.of(1990,1,1),"11999999999","x@example.test",null,CustomerPlan.STANDARD,true);
        when(auth.findCustomerById(customer)).thenReturn(Optional.of(c));when(auth.findCustomerByIdForUpdate(customer)).thenReturn(Optional.of(c));
        when(auth.findCredentialByCustomerId(customer)).thenReturn(Optional.of(new AccessCredential(customer,"cliente",HASH,true)));
        account=new Account(accountId,customer,"Cliente","11144477735","90001","0001","123456",AccountType.CURRENT,new BigDecimal("1000.00"),true);
        when(accounts.findAllForUpdate(any())).thenReturn(Map.of(accountId,account));
        var product=new InvestmentProduct(productId,"Fundo",InvestmentProduct.Type.FUND,new BigDecimal("100"),new BigDecimal("100"),"LOW",true,new InvestmentProduct.FundTerms(0,0,1),null);
        var p=new InvestmentPosition(positionId,UUID.randomUUID(),customer,accountId,product,new BigDecimal("200"),LocalDate.of(2026,10,23),new BigDecimal("20"),BigDecimal.TEN,LocalDate.of(2026,10,23),null);
        when(positions.positions(customer)).thenReturn(List.of(p));when(positions.quote(eq(productId),any())).thenReturn(Optional.of(new InvestmentQuote(productId,LocalDate.of(2026,10,23),BigDecimal.TEN)));
        var calendar=new InvestmentBusinessCalendar(positions);
        service=new InvestmentRedemptionService(positions,redemptions,auth,accounts,entries,calendar,new InvestmentValuationCalculator(positions,reference,calendar),clock);
    }
    InvestmentRedemption request(String units,UUID key){return service.requestAmount(customer,positionId,key,new BigDecimal(units).multiply(BigDecimal.TEN),"Senha@123");}
    @Test void previewHasNoFinancialEffects(){var p=service.previewAmount(customer,positionId,new BigDecimal("100"));assertThat(p.estimatedAmount()).isEqualByComparingTo("100");assertThat(p.paymentDate()).isEqualTo(LocalDate.of(2026,10,28));assertThat(redemptions.rows).isEmpty();verifyNoInteractions(entries);}
    @Test void partialMinimumAndOverdrawAreRejected(){assertThatExceptionOfType(BusinessException.class).isThrownBy(()->request("11",UUID.randomUUID()));assertThatExceptionOfType(BusinessException.class).isThrownBy(()->request("21",UUID.randomUUID()));assertThat(redemptions.rows).isEmpty();}
    @Test void wholePositionMayBeClosedBelowRemainingMinimum(){var r=request("20",UUID.randomUUID());assertThat(r.amount()).isEqualByComparingTo("200");assertThat(r.status()).isEqualTo("PENDING");verifyNoInteractions(entries);}
    @Test void replayAndPasswordAreRevalidated(){var key=UUID.randomUUID();var first=request("10",key);assertThat(request("10.0",key)).isEqualTo(first);assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.requestAmount(customer,positionId,key,new BigDecimal("100"),"errada"));assertThatExceptionOfType(IdempotencyConflictException.class).isThrownBy(()->request("20",key));assertThat(redemptions.rows).hasSize(1);}
    @Test void onlyOnePendingPerLot(){request("10",UUID.randomUUID());assertThatExceptionOfType(BusinessException.class).isThrownBy(()->request("10",UUID.randomUUID()));}
    @Test void payoutIsNextBusinessDayAndExactlyOnce(){request("10",UUID.randomUUID());service.process(customer);assertThat(account.getBalance()).isEqualByComparingTo("1000");clock.day="2026-10-28T15:00:00Z";service.process(customer);service.process(customer);assertThat(account.getBalance()).isEqualByComparingTo("1100");verify(entries,times(1)).saveAll(any());assertThat(request("10",UUID.randomUUID()).amount()).isEqualByComparingTo("100");}
    @Test void weekendConversionUsesEffectiveQuoteAndD1Payment(){clock.day="2026-10-24T15:00:00Z";var r=request("10",UUID.randomUUID());assertThat(r.status()).isEqualTo("CONVERSION_PENDING");clock.day="2026-10-26T15:00:00Z";when(positions.quote(eq(productId),any())).thenReturn(Optional.of(new InvestmentQuote(productId,LocalDate.of(2026,10,26),new BigDecimal("12"))));service.process(customer);var converted=redemptions.rows.get(r.id());assertThat(converted.amount()).isEqualByComparingTo("100");assertThat(converted.units()).isEqualByComparingTo("8.333333333333");assertThat(converted.paymentDate()).isEqualTo(LocalDate.of(2026,10,27));assertThat(account.getBalance()).isEqualByComparingTo("1000");}
    @Test void weekendConversionRevalidatesMinimumWithoutCashEffects(){clock.day="2026-10-24T15:00:00Z";var r=request("10",UUID.randomUUID());clock.day="2026-10-26T15:00:00Z";when(positions.quote(eq(productId),any())).thenReturn(Optional.of(new InvestmentQuote(productId,LocalDate.of(2026,10,26),new BigDecimal("9"))));service.process(customer);assertThat(redemptions.rows.get(r.id()).status()).isEqualTo("FAILED");assertThat(account.getBalance()).isEqualByComparingTo("1000");verifyNoInteractions(entries);}
    @Test void cdbMaturityCreditsContractualValueOnce(){
        var product=new InvestmentProduct(productId,"CDB",InvestmentProduct.Type.CDB,new BigDecimal("500"),new BigDecimal("500"),"LOW",true,null,new InvestmentProduct.CdbTerms(new BigDecimal("110"),3,"AT_MATURITY"));
        var p=new InvestmentPosition(positionId,UUID.randomUUID(),customer,accountId,product,new BigDecimal("500"),LocalDate.of(2026,10,23),null,null,null,LocalDate.of(2026,10,26));
        when(positions.positions(customer)).thenReturn(List.of(p));
        when(reference.cdiPeriods(any(),any())).thenReturn(List.of(new InvestmentCdiPeriod(LocalDate.of(2026,1,1),LocalDate.of(2026,12,31),new BigDecimal("1.001"),252,"TEST")));
        service.process(customer);service.process(customer);
        assertThat(account.getBalance()).isEqualByComparingTo("1500.55");verify(entries,times(1)).saveAll(any());
        assertThat(redemptions.rows).hasSize(1);
    }
    @Test void cannotReadAnothersPosition(){assertThatExceptionOfType(NotFoundException.class).isThrownBy(()->service.previewAmount(customer,UUID.randomUUID(),new BigDecimal("100")));}
    static class MutableClock extends Clock {
        String day="2026-10-27T15:00:00Z";public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return Instant.parse(day);}
    }
    static class Memory implements InvestmentRedemptions {
        final Map<UUID,InvestmentRedemption> rows=new HashMap<>();
        public List<InvestmentRedemption> list(UUID id){return rows.values().stream().filter(r->r.customerId().equals(id)).toList();}
        public List<UUID> customers(){return rows.values().stream().map(r->r.customerId()).distinct().toList();}
        public void save(InvestmentRedemption r){rows.put(r.id(),r);}
    }
}

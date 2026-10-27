package br.com.spbank.investimento.application.usecase;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.out.InvestmentPersistence;
import br.com.spbank.investimento.application.service.InvestmentBusinessCalendar;
import br.com.spbank.autenticacao.application.model.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.*;
import br.com.spbank.shared.application.model.CustomerPlan;
import br.com.spbank.shared.application.exception.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
class InvestmentServiceTest {
    private static final String HASH=PasswordHasher.hash("Senha@123");
    private final UUID customerId=UUID.randomUUID(),accountId=UUID.randomUUID(),fundId=UUID.randomUUID(),cdbId=UUID.randomUUID();
    private final AuthenticationPersistence customers=mock(AuthenticationPersistence.class);
    private final AccountPersistence accounts=mock(AccountPersistence.class);
    private final AccountEntryPersistence entries=mock(AccountEntryPersistence.class);
    private final Memory persistence=new Memory();
    private final br.com.spbank.investimento.application.port.out.InvestmentReferenceData reference=mock(br.com.spbank.investimento.application.port.out.InvestmentReferenceData.class);
    private final MutableClock clock=new MutableClock();
    private Account account;
    private InvestmentService service;
    @BeforeEach void setup(){
        var customer=new Customer(customerId,"Cliente","11144477735",LocalDate.of(1990,1,1),"11999999999","cliente@exemplo.test",null,CustomerPlan.STANDARD,true);
        when(customers.findCustomerById(customerId)).thenReturn(Optional.of(customer));
        when(customers.findCustomerByIdForUpdate(customerId)).thenReturn(Optional.of(customer));
        when(customers.findCredentialByCustomerId(customerId)).thenReturn(Optional.of(new AccessCredential(customerId,"cliente",HASH,true)));
        account=new Account(accountId,customerId,"Cliente","11144477735","999","0001","10001",AccountType.CURRENT,money("5000"),true);
        when(accounts.findById(accountId)).thenAnswer(i->Optional.of(account));
        when(accounts.findAllForUpdate(any())).thenAnswer(i->Map.of(accountId,account));
        persistence.products.put(fundId,new InvestmentProduct(fundId,"Fundo",InvestmentProduct.Type.FUND,money("100"),money("100"),"MODERATE",true,new InvestmentProduct.FundTerms(0,0,1),null));
        persistence.products.put(cdbId,new InvestmentProduct(cdbId,"CDB",InvestmentProduct.Type.CDB,money("500"),money("500"),"LOW",true,null,new InvestmentProduct.CdbTerms(new BigDecimal("110"),360,"AT_MATURITY")));
        persistence.quote=new InvestmentQuote(fundId,LocalDate.of(2026,10,23),new BigDecimal("3"));
        service=new InvestmentService(persistence,customers,accounts,entries,new InvestmentBusinessCalendar(persistence),clock,JsonMapper.builder().build(),new br.com.spbank.investimento.application.service.InvestmentValuationCalculator(persistence,reference,new InvestmentBusinessCalendar(persistence)),reference,mock(br.com.spbank.investimento.application.port.out.InvestmentRedemptions.class));
    }
    @Test void catalogSeparatesFundAndCdbAndFgc(){var products=service.products(customerId);assertThat(products).hasSize(2);var fund=persistence.products.get(fundId);var cdb=persistence.products.get(cdbId);assertThat(fund.cdb()).isNull();assertThat(fund.fgcEligible()).isFalse();assertThat(cdb.fund()).isNull();assertThat(cdb.fgcEligible()).isTrue();}
    @Test void hybridProductIsRejected(){assertThatIllegalArgumentException().isThrownBy(()->new InvestmentProduct(cdbId,"Inválido",InvestmentProduct.Type.CDB,money("500"),money("500"),"LOW",true,new InvestmentProduct.FundTerms(0,0,1),persistence.products.get(cdbId).cdb()));}
    @Test void previewDoesNotWriteDebitReserveOrConsumeKey(){var preview=service.preview(customerId,accountId,fundId,money("100"));assertThat(preview.estimatedUnits()).isEqualByComparingTo("33.333333333333");assertThat(preview.quote().date()).isEqualTo(LocalDate.of(2026,10,23));assertThat(account.getBalance()).isEqualByComparingTo("5000");assertThat(persistence.applications).isEmpty();assertThat(persistence.positions).isEmpty();assertThat(persistence.operations).isEmpty();verifyNoInteractions(entries);verify(accounts,never()).saveAll(any());}
    @Test void fundCreatesOneDebitEntryAndImmutableDatedLot(){var result=apply(fundId,"100",UUID.randomUUID());assertThat(result.status()).isEqualTo(InvestmentApplication.Status.COMPLETED);assertThat(result.balanceAfter()).isEqualByComparingTo("4900");var lot=persistence.positions.values().iterator().next();assertThat(lot.units()).isEqualByComparingTo("33.333333333333");assertThat(lot.unitPrice()).isEqualByComparingTo("3");assertThat(lot.maturityDate()).isNull();verify(entries).saveAll(argThat(rows->rows.size()==1&&rows.iterator().next().type()==EntryType.INVESTMENT_APPLICATION));}
    @Test void cdbHasPrincipalAndMaturityWithoutInventedUnits(){apply(cdbId,"500",UUID.randomUUID());var lot=persistence.positions.values().iterator().next();assertThat(lot.units()).isNull();assertThat(lot.unitPrice()).isNull();assertThat(lot.quoteDate()).isNull();assertThat(lot.maturityDate()).isEqualTo(LocalDate.of(2027,10,18));assertThat(lot.product().cdb().cdiPercent()).isEqualByComparingTo("110");}
    @Test void replayRequiresPasswordAndNeverDebitsTwice(){var key=UUID.randomUUID();var first=apply(fundId,"100",key);assertThat(apply(fundId,"100",key)).isEqualTo(first);assertThat(account.getBalance()).isEqualByComparingTo("4900");assertThat(persistence.positions).hasSize(1);verify(entries,times(1)).saveAll(any());assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.apply(customerId,key,new InvestmentCommand(accountId,fundId,money("100"),"errada")));}
    @Test void sameKeyWithAnotherIntentConflicts(){var key=UUID.randomUUID();apply(fundId,"100",key);assertThatExceptionOfType(IdempotencyConflictException.class).isThrownBy(()->apply(fundId,"200",key));assertThat(account.getBalance()).isEqualByComparingTo("4900");}
    @Test void wrongPasswordDoesNotConsumeKey(){var key=UUID.randomUUID();assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.apply(customerId,key,new InvestmentCommand(accountId,fundId,money("100"),"errada")));assertThat(persistence.operations).isEmpty();assertThat(persistence.applications).isEmpty();apply(fundId,"100",key);assertThat(account.getBalance()).isEqualByComparingTo("4900");}
    @Test void rejectsFractionalCentsZeroAndBelowMinimum(){for(var value:List.of("0","-1","100.001","99.99"))assertThatExceptionOfType(BusinessException.class).isThrownBy(()->apply(fundId,value,UUID.randomUUID()));assertThat(persistence.applications).isEmpty();verifyNoInteractions(entries);}
    @Test void insufficientFundsAndInactiveProductsDoNotWrite(){assertThatExceptionOfType(BusinessException.class).isThrownBy(()->apply(cdbId,"6000",UUID.randomUUID()));var old=persistence.products.get(cdbId);persistence.products.put(cdbId,new InvestmentProduct(old.id(),old.name(),old.type(),old.minimumApplication(),old.minimumRemaining(),old.risk(),false,null,old.cdb()));assertThatExceptionOfType(BusinessException.class).isThrownBy(()->apply(cdbId,"500",UUID.randomUUID()));assertThat(persistence.applications).isEmpty();assertThat(account.getBalance()).isEqualByComparingTo("5000");}
    @Test void ownershipAndInactiveAccountAreRevalidated(){account=new Account(accountId,UUID.randomUUID(),"Alheio","11144477735","999","0001","10001",AccountType.CURRENT,money("5000"),true);assertThatExceptionOfType(BusinessException.class).isThrownBy(()->apply(fundId,"100",UUID.randomUUID()));account=new Account(accountId,customerId,"Cliente","11144477735","999","0001","10001",AccountType.CURRENT,money("5000"),false);assertThatExceptionOfType(BusinessException.class).isThrownBy(()->apply(fundId,"100",UUID.randomUUID()));assertThat(persistence.positions).isEmpty();}
    @Test void completedContractDoesNotChangeWhenCatalogChanges(){apply(cdbId,"500",UUID.randomUUID());var original=persistence.positions.values().iterator().next();var old=persistence.products.get(cdbId);persistence.products.put(cdbId,new InvestmentProduct(old.id(),"Novo CDB",old.type(),money("500"),money("500"),old.risk(),true,null,new InvestmentProduct.CdbTerms(new BigDecimal("90"),180,"AT_MATURITY")));assertThat(original.product().cdb().cdiPercent()).isEqualByComparingTo("110");assertThat(original.product().cdb().termDays()).isEqualTo(360);}
    @Test void pendingWeekendHasNoDebitAndIsProcessedOnlyOnceUsingEffectiveQuote(){clock.at("2026-10-24T15:00:00Z");var pending=apply(fundId,"100",UUID.randomUUID());assertThat(pending.status()).isEqualTo(InvestmentApplication.Status.PENDING);assertThat(pending.effectiveDate()).isEqualTo(LocalDate.of(2026,10,26));assertThat(account.getBalance()).isEqualByComparingTo("5000");assertThat(persistence.positions).isEmpty();service.process(pending.id());assertThat(account.getBalance()).isEqualByComparingTo("5000");clock.at("2026-10-26T15:00:00Z");persistence.quote=new InvestmentQuote(fundId,LocalDate.of(2026,10,26),new BigDecimal("4"));service.process(pending.id());service.process(pending.id());assertThat(account.getBalance()).isEqualByComparingTo("4900");assertThat(persistence.positions.values().iterator().next().units()).isEqualByComparingTo("25");verify(entries,times(1)).saveAll(any());}
    @Test void recurringAndExactHolidaysAdvanceEffectiveDate(){clock.at("2026-10-24T15:00:00Z");persistence.holidays.add(LocalDate.of(2026,10,26));persistence.holidays.add(LocalDate.of(2026,10,27));assertThat(service.preview(customerId,accountId,fundId,money("100")).effectiveDate()).isEqualTo(LocalDate.of(2026,10,28));}
    @Test void failedPendingOrderNeverCreatesADebitOrLot(){clock.at("2026-10-24T15:00:00Z");var pending=apply(cdbId,"500",UUID.randomUUID());account.debit(money("4900"));clock.at("2026-10-26T15:00:00Z");service.process(pending.id());assertThat(service.application(customerId,pending.id()).status()).isEqualTo(InvestmentApplication.Status.FAILED);assertThat(account.getBalance()).isEqualByComparingTo("100");assertThat(persistence.positions).isEmpty();verifyNoInteractions(entries);}
    @Test void missingQuoteDoesNotCreateAPosition(){persistence.quote=null;assertThatExceptionOfType(BusinessException.class).isThrownBy(()->service.preview(customerId,accountId,fundId,money("100")));assertThat(persistence.positions).isEmpty();}
    @Test void queriesNeverReturnAnotherCustomersData(){apply(fundId,"100",UUID.randomUUID());assertThat(service.positions(customerId)).hasSize(1);assertThat(persistence.positions(UUID.randomUUID())).isEmpty();assertThatExceptionOfType(NotFoundException.class).isThrownBy(()->service.application(customerId,UUID.randomUUID()));}
    @Test void commandNeverIncludesPasswordInLogs(){assertThat(new InvestmentCommand(accountId,fundId,money("100"),"segredo-unico").toString()).doesNotContain("segredo-unico");}
    @Test void valuationReadsDoNotCreditTheAccountOrPersistFinancialState(){apply(cdbId,"500",UUID.randomUUID());clearInvocations(accounts,entries);var first=service.positions(customerId);var second=service.positions(customerId);assertThat(second).isEqualTo(first);assertThat(first.getFirst().grossValue()).isEqualByComparingTo("500.00");assertThat(account.getBalance()).isEqualByComparingTo("4500");verify(reference,never()).saveValuation(any());verify(accounts,never()).saveAll(any());verifyNoInteractions(entries);}
    @Test void valuationProcessorPersistsDerivedAuditWithoutChangingCash(){apply(fundId,"100",UUID.randomUUID());clearInvocations(accounts,entries);service.processValuations(customerId);verify(reference).saveValuation(any());verify(accounts,never()).saveAll(any());verifyNoInteractions(entries);assertThat(account.getBalance()).isEqualByComparingTo("4900");}
    private InvestmentApplication apply(UUID product,String amount,UUID key){return service.apply(customerId,key,new InvestmentCommand(accountId,product,money(amount),"Senha@123"));}
    private static BigDecimal money(String value){return new BigDecimal(value);}
    private static class MutableClock extends Clock {private Instant now=Instant.parse("2026-10-23T15:00:00Z");void at(String value){now=Instant.parse(value);}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}}
    private static class Memory implements InvestmentPersistence {
        final Map<UUID,InvestmentProduct> products=new HashMap<>();final Map<UUID,InvestmentApplication> applications=new HashMap<>();final Map<UUID,InvestmentPosition> positions=new HashMap<>();final Map<UUID,InvestmentOperation> operations=new HashMap<>();final Set<LocalDate> holidays=new HashSet<>();InvestmentQuote quote;
        public List<InvestmentProduct> products(){return List.copyOf(products.values());}public Optional<InvestmentProduct> product(UUID id,boolean locked){return Optional.ofNullable(products.get(id));}
        public Optional<InvestmentQuote> quote(UUID id,LocalDate through){return Optional.ofNullable(quote).filter(q->q.productId().equals(id)&&!q.date().isAfter(through));}
        public boolean holiday(LocalDate date){return holidays.contains(date);}
        public Set<LocalDate> holidays(LocalDate from,LocalDate through){return holidays.stream().filter(d->!d.isBefore(from)&&!d.isAfter(through)).collect(java.util.stream.Collectors.toSet());}public List<InvestmentPosition> positions(UUID owner){return positions.values().stream().filter(p->p.customerId().equals(owner)).toList();}
        public Optional<InvestmentApplication> application(UUID owner,UUID id,boolean locked){return Optional.ofNullable(applications.get(id)).filter(a->a.customerId().equals(owner));}
        public Optional<UUID> owner(UUID id){return Optional.ofNullable(applications.get(id)).map(a->a.customerId());}public List<UUID> due(LocalDate through,int limit){return applications.values().stream().filter(a->a.status()==InvestmentApplication.Status.PENDING&&!a.effectiveDate().isAfter(through)).limit(limit).map(a->a.id()).toList();}
        public Optional<InvestmentOperation> operation(UUID owner,UUID key){return Optional.ofNullable(operations.get(key)).filter(o->o.customerId().equals(owner));}public void save(InvestmentApplication a){applications.put(a.id(),a);}public void save(InvestmentPosition p){positions.put(p.id(),p);}public void save(InvestmentOperation o){operations.put(o.key(),o);}
    }
}

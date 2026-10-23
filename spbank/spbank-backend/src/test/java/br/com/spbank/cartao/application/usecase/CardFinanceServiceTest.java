package br.com.spbank.cartao.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static br.com.spbank.cartao.application.service.CardFinancePolicy.*;
import br.com.spbank.autenticacao.application.model.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.administracao.application.model.AdministratorCredential;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.model.finance.*;
import br.com.spbank.cartao.application.port.in.CardInstrumentUseCase;
import br.com.spbank.cartao.application.port.out.*;
import br.com.spbank.cartao.application.service.CardBillingCalendar;
import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;

class CardFinanceServiceTest {
    private final UUID customerId=UUID.randomUUID(),adminId=UUID.randomUUID(),accountId=UUID.randomUUID(),cardId=UUID.randomUUID();
    private final CardFinancePersistence persistence=mock(CardFinancePersistence.class);
    private final AuthenticationPersistence customers=mock(AuthenticationPersistence.class);
    private final AdministrationPersistence administrators=mock(AdministrationPersistence.class);
    private final CardInstrumentPersistence instruments=mock(CardInstrumentPersistence.class);
    private final CardInstrumentUseCase instrumentUseCase=mock(CardInstrumentUseCase.class);
    private final CardCreditAllocationPersistence lines=mock(CardCreditAllocationPersistence.class);
    private final AccountPersistence accounts=mock(AccountPersistence.class);
    private final AccountEntryPersistence entries=mock(AccountEntryPersistence.class);
    private final CardCredentialVault vault=mock(CardCredentialVault.class);
    private static final String HASH=PasswordHasher.hash("Senha@123");
    private CardFinanceState stored=new CardFinanceState();
    private final Map<CardBrand,CardCreditLine> limits=new EnumMap<>(CardBrand.class);
    private BigDecimal balance=new BigDecimal("5000.00");
    private CustomerPlan plan=CustomerPlan.STANDARD;
    private final MutableClock clock=new MutableClock();
    private CardInstrument instrument;
    private CardDetails details;
    private CardFinanceService finance;

    @BeforeEach void setUp(){
        var credentials=new CardCredentials("cipher-pan","cipher-cvv","fingerprint","1234");
        instrument=new CardInstrument(cardId,customerId,CardBrand.BRISA,null,"Cliente",credentials.lastFour(),credentials.panFingerprint(),credentials.encryptedPan(),credentials.encryptedCvv(),HASH,
            CardState.ACTIVE,0,false,false,false,null,null,LocalDate.of(2029,10,31),clock.instant());details=new CardDetails("Cliente","9901000000001234","007",LocalDate.of(2029,10,31));
        when(vault.reveal(any())).thenAnswer(i->details);
        finance=new CardFinanceService(persistence,customers,administrators,instruments,instrumentUseCase,vault,lines,accounts,entries,
            new CardBillingCalendar("America/Sao_Paulo","2026-11-02"),clock,JsonMapper.builder().build());
        when(customers.findCustomerByIdForUpdate(customerId)).thenAnswer(i->Optional.of(customer()));
        when(customers.findCredentialByCustomerId(customerId)).thenReturn(Optional.of(new AccessCredential(customerId,"cliente",HASH,true)));
        when(administrators.findCredentialById(adminId)).thenReturn(Optional.of(new AdministratorCredential(adminId,"Gerente","gerente",HASH,true)));
        when(instruments.cards(customerId)).thenAnswer(i->List.of(instrument));
        when(instrumentUseCase.details(customerId,cardId,"0012")).thenReturn(details);
        for(var brand:CardBrand.values())limits.put(brand,new CardCreditLine(brand,brand==CardBrand.BRISA?new BigDecimal("5000.00"):ZERO,ZERO));
        when(lines.lines(customerId)).thenAnswer(i->List.copyOf(limits.values()));
        doAnswer(i->{List<CardCreditLine> values=i.getArgument(1);for(var value:values)limits.put(value.brand(),value);return null;}).when(lines).saveLines(eq(customerId),any());
        when(persistence.load(customerId)).thenAnswer(i->copy(stored));
        doAnswer(i->{stored=copy(i.getArgument(0));return null;}).when(persistence).save(any());
        when(accounts.findById(accountId)).thenAnswer(i->Optional.of(account()));
        when(accounts.findAllForUpdate(List.of(accountId))).thenAnswer(i->Map.of(accountId,account()));
        doAnswer(i->{Collection<Account> values=i.getArgument(0);balance=values.iterator().next().getBalance();return null;}).when(accounts).saveAll(any());
    }

    @Test void standardPurchaseHasFixedContractInterestAndDoesNotDebitAnAccount(){
        var result=buy("100.00",2);assertThat(result.status()).isEqualTo("APPROVED");assertThat(result.amount()).isEqualByComparingTo("104.00");
        assertThat(stored.cardObligations).hasSize(2);assertThat(committed()).isEqualByComparingTo("104.00");
        assertThat(balance).isEqualByComparingTo("5000");verifyNoInteractions(accounts,entries);
        plan=CustomerPlan.PLUS;finance.overview(customerId);assertThat(stored.cardPurchases.getFirst().interest()).isEqualByComparingTo("4");
    }
    @Test void previewIsFinanciallyReadOnly(){var preview=finance.preview(customerId,command("100",2));assertThat(preview.total()).isEqualByComparingTo("104");assertThat(stored.cardPurchases).isEmpty();assertThat(committed()).isZero();verify(persistence,never()).save(any());}
    @Test void plusHasTwelveInterestFreeInstallments(){plan=CustomerPlan.PLUS;var result=buy("120.00",12);assertThat(result.status()).isEqualTo("APPROVED");assertThat(stored.cardObligations).hasSize(12);assertThat(stored.cardPurchases.getFirst().interest()).isZero();}
    @Test void refusedAttemptIsRecordedWithoutDebtOrInvoiceCharge(){var result=buy("9.99",1);assertThat(result.status()).isEqualTo("DECLINED");assertThat(stored.cardAttempts).hasSize(1);assertThat(stored.cardPurchases).isEmpty();assertThat(committed()).isZero();}
    @Test void blockedCardAndAdjustmentAfterDowngradePreventPurchases(){instrument=instrument.withBlocks(false,true,null,"Análise");assertThat(buy("100",1).status()).isEqualTo("DECLINED");instrument=instrument.withBlocks(false,false,null,null);limits.put(CardBrand.BRISA,new CardCreditLine(CardBrand.BRISA,new BigDecimal("6000.00"),ZERO));assertThat(buy("100",1).status()).isEqualTo("DECLINED");assertThat(committed()).isZero();}
    @Test void approvalIsIdempotentAndChangedIntentionConflicts(){var key=UUID.randomUUID();var first=finance.purchase(customerId,key,command("100.00",2));assertThat(finance.purchase(customerId,key,command("100.00",2))).isEqualTo(first);assertThat(stored.cardPurchases).hasSize(1);assertThatThrownBy(()->finance.purchase(customerId,key,command("101.00",2))).isInstanceOf(IdempotencyConflictException.class);}
    @Test void paymentByOwnedAccountIsAtomicIdempotentAndPaysInterestFirst(){buy("100",2);var key=UUID.randomUUID();var result=finance.payAccount(customerId,key,CardBrand.BRISA,new BigDecimal("1.00"),accountId,"Senha@123");assertThat(balance).isEqualByComparingTo("4999");assertThat(committed()).isEqualByComparingTo("103");var row=stored.cardObligations.stream().min(Comparator.comparingInt(obligation -> java.util.Objects.requireNonNull(obligation).sequence())).orElseThrow();assertThat(row.remainingInterest()).isEqualByComparingTo("1");assertThat(row.remainingPrincipal()).isEqualByComparingTo("50");assertThat(finance.payAccount(customerId,key,CardBrand.BRISA,new BigDecimal("1.00"),accountId,"Senha@123")).isEqualTo(result);assertThat(balance).isEqualByComparingTo("4999");verify(entries,times(1)).saveAll(any());}
    @Test void agreementEntryCannotDebitAnAccountBeforeReconcilingExistingCredit(){var paid=buy("100",1);finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("100.00"),accountId,"Senha@123");var refund=finance.requestRefund(customerId,UUID.randomUUID(),paid.objectId(),new BigDecimal("100.00"),"Devolução","Senha@123");finance.decideRefund(adminId,customerId,UUID.randomUUID(),refund.objectId(),true,"Recebido");buy("300",1);var proposal=finance.propose(customerId,UUID.randomUUID(),CardBrand.BRISA,List.of(stored.cardInvoices.getFirst().id()),null,3,"Senha@123");assertThatThrownBy(()->finance.payProposal(customerId,UUID.randomUUID(),proposal.objectId(),accountId,"Senha@123")).isInstanceOf(BusinessException.class);assertThat(balance).isEqualByComparingTo("4900");assertThat(stored.cardAgreements).isEmpty();}
    @Test void wrongPasswordCannotConsumeTheKeyOrDebitMoney(){buy("100",1);var key=UUID.randomUUID();assertThatThrownBy(()->finance.payAccount(customerId,key,CardBrand.BRISA,new BigDecimal("10.00"),accountId,"errada")).isInstanceOf(BusinessException.class);assertThat(balance).isEqualByComparingTo("5000");finance.payAccount(customerId,key,CardBrand.BRISA,new BigDecimal("10.00"),accountId,"Senha@123");assertThat(balance).isEqualByComparingTo("4990");}
    @Test void anotherCustomersAccountIsNotAllowed(){buy("100",1);assertThatThrownBy(()->finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("10.00"),UUID.randomUUID(),"Senha@123")).isInstanceOf(BusinessException.class);assertThat(balance).isEqualByComparingTo("5000");}
    @Test void boletoDoesNotSettleBeforeManagerConfirmationAndCannotBeConfirmedTwice(){buy("100",1);var pending=finance.boleto(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("30.00"),null,"Senha@123");assertThat(committed()).isEqualByComparingTo("100");var key=UUID.randomUUID();var first=finance.confirmBoleto(adminId,customerId,key,pending.objectId(),"Recebido");assertThat(committed()).isEqualByComparingTo("70");assertThat(finance.confirmBoleto(adminId,customerId,key,pending.objectId(),"Recebido")).isEqualTo(first);assertThatThrownBy(()->finance.confirmBoleto(adminId,customerId,UUID.randomUUID(),pending.objectId(),"Repetido")).isInstanceOf(BusinessException.class);assertThat(committed()).isEqualByComparingTo("70");verifyNoInteractions(accounts,entries);}
    @Test void refundStartsFromFinalInstallmentsAndReversesOriginalPurchaseInterest(){var purchase=buy("100",2);var request=finance.requestRefund(customerId,UUID.randomUUID(),purchase.objectId(),new BigDecimal("25.00"),"Devolução parcial","Senha@123");finance.decideRefund(adminId,customerId,UUID.randomUUID(),request.objectId(),true,"Aprovado");var last=stored.cardObligations.stream().max(Comparator.comparingInt(obligation -> java.util.Objects.requireNonNull(obligation).sequence())).orElseThrow();assertThat(last.remainingPrincipal()).isEqualByComparingTo("25");assertThat(last.remainingInterest()).isEqualByComparingTo("1");assertThat(committed()).isEqualByComparingTo("78");var rest=finance.requestRefund(customerId,UUID.randomUUID(),purchase.objectId(),new BigDecimal("75.00"),"Devolução restante","Senha@123");finance.decideRefund(adminId,customerId,UUID.randomUUID(),rest.objectId(),true,"Aprovado");assertThat(committed()).isZero();assertThat(stored.cardPurchases.getFirst().refundedInterest()).isEqualByComparingTo("4");}
    @Test void competingRefundRequestsAreRevalidatedAtApproval(){var purchase=buy("100",1);var a=finance.requestRefund(customerId,UUID.randomUUID(),purchase.objectId(),new BigDecimal("70.00"),"A","Senha@123");var b=finance.requestRefund(customerId,UUID.randomUUID(),purchase.objectId(),new BigDecimal("70.00"),"B","Senha@123");finance.decideRefund(adminId,customerId,UUID.randomUUID(),a.objectId(),true,"Aprovado");assertThatThrownBy(()->finance.decideRefund(adminId,customerId,UUID.randomUUID(),b.objectId(),true,"Aprovado")).isInstanceOf(BusinessException.class);assertThat(committed()).isEqualByComparingTo("30");}
    @Test void refundOfPaidPurchaseBecomesCreditInSameLineWithoutBankTransfer(){var purchase=buy("100",1);finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("100.00"),accountId,"Senha@123");var request=finance.requestRefund(customerId,UUID.randomUUID(),purchase.objectId(),new BigDecimal("100.00"),"Devolução","Senha@123");finance.decideRefund(adminId,customerId,UUID.randomUUID(),request.objectId(),true,"Recebido");assertThat(balance).isEqualByComparingTo("4900");assertThat(stored.billingLines.getFirst().creditBalance()).isEqualByComparingTo("100");assertThat(committed()).isZero();}
    @Test void futureInvoiceReconcilesOwnedCreditWithoutAnotherDebit(){var purchase=buy("100",1);finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("100.00"),accountId,"Senha@123");var request=finance.requestRefund(customerId,UUID.randomUUID(),purchase.objectId(),new BigDecimal("100.00"),"Devolução","Senha@123");finance.decideRefund(adminId,customerId,UUID.randomUUID(),request.objectId(),true,"Recebido");buy("60",1);clock.at("2026-10-03T03:00:00Z");finance.overview(customerId);assertThat(committed()).isZero();assertThat(stored.billingLines.getFirst().creditBalance()).isEqualByComparingTo("40");assertThat(balance).isEqualByComparingTo("4900");}
    @Test void anticipationChargesOnlyAfterRevalidatingExpectedAmountAndAppliesDiscount(){var purchase=buy("100",2);var preview=finance.anticipation(customerId,CardBrand.BRISA,purchase.objectId(),"PURCHASE",2);assertThat(preview.discount()).isPositive();assertThatThrownBy(()->finance.anticipate(customerId,UUID.randomUUID(),CardBrand.BRISA,purchase.objectId(),"PURCHASE",2,new BigDecimal("1.00"),accountId,"Senha@123")).isInstanceOf(BusinessException.class);assertThat(balance).isEqualByComparingTo("5000");finance.anticipate(customerId,UUID.randomUUID(),CardBrand.BRISA,purchase.objectId(),"PURCHASE",2,preview.amount(),accountId,"Senha@123");assertThat(committed()).isZero();assertThat(stored.cardObligations).allSatisfy(o->assertThat(o.status()).isEqualTo("ANTICIPATED"));assertThat(balance).isEqualByComparingTo(new BigDecimal("5000.00").subtract(preview.amount()));}
    @Test void interestFreeAnticipationHasNoDiscount(){plan=CustomerPlan.PLUS;var purchase=buy("100",2);assertThat(finance.anticipation(customerId,CardBrand.BRISA,purchase.objectId(),"PURCHASE",2).discount()).isZero();}
    @Test void lateFineIsOnceAndMoraIsDailyNonCompoundingUsingInitialPlan(){buy("100",1);clock.at("2026-10-14T12:00:00Z");finance.processCustomer(customerId);var episode=stored.cardLateEpisodes.getFirst();assertThat(episode.fineCharged()).isEqualByComparingTo("2");assertThat(episode.moraCharged()).isEqualByComparingTo("0.07");var previous=committed();plan=CustomerPlan.PLUS;finance.processCustomer(customerId);assertThat(committed()).isEqualByComparingTo(previous);assertThat(stored.cardLateEpisodes.getFirst().plan()).isEqualTo(CustomerPlan.STANDARD);assertThat(buy("20",1).status()).isEqualTo("DECLINED");}
    @Test void latePaymentPaysChargesBeforePrincipalAndReducesFutureMoraBase(){buy("100",1);clock.at("2026-10-14T12:00:00Z");finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("52.07"),accountId,"Senha@123");assertThat(stored.cardObligations.getFirst().remainingPrincipal()).isEqualByComparingTo("50");clock.at("2026-10-15T12:00:00Z");finance.processCustomer(customerId);assertThat(stored.cardLateEpisodes.getFirst().moraCharged()).isEqualByComparingTo("0.08");}
    @Test void dueDayChangePreservesCurrentInvoiceAndReportsFutureMonth(){buy("100",2);var original=stored.cardInvoices.getFirst();var result=finance.changeDueDay(customerId,UUID.randomUUID(),CardBrand.BRISA,25,"Senha@123");assertThat(result.message()).isEqualTo("2026-11");assertThat(stored.cardInvoices.getFirst().nominalDue()).isEqualTo(original.nominalDue());assertThat(stored.cardObligations.stream().filter(o->o.sequence()==2).findFirst().orElseThrow().nominalDue().getDayOfMonth()).isEqualTo(25);}
    @Test void emptyCyclesAreCreatedAndExportableButCannotReceiveZeroPayment(){finance.overview(customerId);clock.at("2026-11-04T12:00:00Z");finance.processCustomer(customerId);assertThat(stored.cardInvoices).hasSize(3);var empty=stored.cardInvoices.getFirst();assertThat(finance.invoice(customerId,empty.id()).message()).isEqualTo("Sem valor a pagar");assertThatThrownBy(()->finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,ZERO,accountId,"Senha@123")).isInstanceOf(BusinessException.class);}
    @Test void preferredAccountDoesNotCauseAutomaticDebit(){buy("100",1);finance.preference(customerId,UUID.randomUUID(),accountId,"Senha@123");assertThat(balance).isEqualByComparingTo("5000");finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("10.00"),null,"Senha@123");assertThat(balance).isEqualByComparingTo("4990");}
    @Test void agreementProposalDoesNotChangeDebtUntilActualEntryAndIncludesEntryInCount(){buy("300",1);var invoice=stored.cardInvoices.getFirst();var proposalResult=finance.propose(customerId,UUID.randomUUID(),CardBrand.BRISA,List.of(invoice.id()),null,3,"Senha@123");var proposal=stored.cardProposals.getFirst();assertThat(proposal.total()).isEqualByComparingTo("318");assertThat(proposal.entry()).isEqualByComparingTo("106");assertThat(committed()).isEqualByComparingTo("300");finance.payProposal(customerId,UUID.randomUUID(),proposalResult.objectId(),accountId,"Senha@123");assertThat(balance).isEqualByComparingTo("4894");assertThat(committed()).isEqualByComparingTo("212");assertThat(stored.cardAgreements).hasSize(1);assertThat(stored.cardObligations.stream().filter(o->o.sourceType().equals("AGREEMENT")).count()).isEqualTo(3);}
    @Test void lateEntryPaysCurrentDebtAndKeepsExcessAsOwnedCreditInsteadOfActivatingProposal(){buy("300",1);var invoice=stored.cardInvoices.getFirst();var proposal=finance.propose(customerId,UUID.randomUUID(),CardBrand.BRISA,List.of(invoice.id()),null,3,"Senha@123");finance.payAccount(customerId,UUID.randomUUID(),CardBrand.BRISA,new BigDecimal("280.00"),accountId,"Senha@123");clock.at("2026-10-02T12:00:00Z");var entry=finance.payProposal(customerId,UUID.randomUUID(),proposal.objectId(),accountId,"Senha@123");assertThat(stored.cardAgreements).isEmpty();assertThat(stored.cardProposals.getFirst().status()).isEqualTo("EXPIRED");assertThat(committed()).isZero();assertThat(entry.creditedAmount()).isEqualByComparingTo("86");assertThat(stored.billingLines.getFirst().creditBalance()).isEqualByComparingTo("86");}
    @Test void renegotiationExcludesFutureInterestAndPreservesPreviousContract(){buy("600",1);var invoice=stored.cardInvoices.getFirst();var first=finance.propose(customerId,UUID.randomUUID(),CardBrand.BRISA,List.of(invoice.id()),null,3,"Senha@123");finance.payProposal(customerId,UUID.randomUUID(),first.objectId(),accountId,"Senha@123");var agreement=stored.cardAgreements.getFirst();var previous=committed();plan=CustomerPlan.PLUS;var next=finance.propose(customerId,UUID.randomUUID(),CardBrand.BRISA,List.of(),agreement.id(),3,"Senha@123");var proposal=stored.cardProposals.stream().filter(p->p.id().equals(next.objectId())).findFirst().orElseThrow();assertThat(proposal.debt()).isLessThan(previous);finance.payProposal(customerId,UUID.randomUUID(),next.objectId(),accountId,"Senha@123");assertThat(stored.cardAgreements.stream().filter(a->a.id().equals(agreement.id())).findFirst().orElseThrow().status()).isEqualTo("REPLACED");assertThat(stored.cardAgreements).hasSize(2);}
    @Test void financeModelsAndOperationSnapshotsContainNoCardSecrets(){buy("100",2);var operation=stored.cardFinancialOperations.getFirst();assertThat(operation.responseJson()).doesNotContain(details.pan(),"Senha@123","0012",instrument.encryptedPan());}

    private CardFinanceResult buy(String amount,int installments){return finance.purchase(customerId,UUID.randomUUID(),command(amount,installments));}
    private CardPurchaseCommand command(String amount,int installments){return new CardPurchaseCommand(details.pan(),"2029-10",details.cvv(),"Loja de estudos",new BigDecimal(amount),installments,"0012");}
    private BigDecimal committed(){return limits.get(CardBrand.BRISA).committed();}
    private Customer customer(){return new Customer(customerId,"Cliente","52998224725",LocalDate.of(1990,1,1),"11987654321","cliente@example.com",null,plan,true);}
    private Account account(){return new Account(accountId,customerId,"Cliente","52998224725","90001","0001","12345",AccountType.CURRENT,balance,true);}
    @SuppressWarnings("unchecked") private static CardFinanceState copy(CardFinanceState original){var copy=new CardFinanceState();try{for(var field:CardFinanceState.class.getFields())((List<Object>)field.get(copy)).addAll((List<Object>)field.get(original));return copy;}catch(IllegalAccessException ex){throw new AssertionError(ex);}}
    private static final class MutableClock extends Clock {private Instant current=Instant.parse("2026-10-01T12:00:00Z");void at(String instant){current=Instant.parse(instant);}@Override public Instant instant(){return current;}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId zone){return Clock.fixed(current,zone);}}
}

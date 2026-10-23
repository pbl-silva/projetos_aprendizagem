package br.com.spbank.cartao.adapter.out.persistence.mysql;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.*;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.*;
import br.com.spbank.cartao.application.model.finance.*;
import br.com.spbank.cartao.application.port.out.CardFinancePersistence;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public final class CardFinanceMysqlPersistence implements CardFinancePersistence {
 private final CardLateAllocationJpaRepository cardLateAllocations;
 private final CardProposalInvoiceJpaRepository cardProposalInvoices;
    private final BillingLineJpaRepository billingLines;
    private final CardPurchaseJpaRepository cardPurchases;
    private final CardObligationJpaRepository cardObligations;
    private final CardInvoiceJpaRepository cardInvoices;
    private final CardInvoiceItemJpaRepository cardInvoiceItems;
    private final CardAttemptJpaRepository cardAttempts;
    private final CardRefundRequestJpaRepository cardRefundRequests;
    private final CardRefundJpaRepository cardRefunds;
    private final CardPaymentJpaRepository cardPayments;
    private final CardSettlementJpaRepository cardSettlements;
    private final CardProposalJpaRepository cardProposals;
    private final CardAgreementJpaRepository cardAgreements;
    private final CardLateEpisodeJpaRepository cardLateEpisodes;
    private final CardDueDayChangeJpaRepository cardDueDayChanges;
    private final CardPaymentPreferenceJpaRepository cardPaymentPreferences;
    private final CardFinancialOperationJpaRepository cardFinancialOperations;
    private final CardAgreementInstallmentJpaRepository cardAgreementInstallments;
    public CardFinanceMysqlPersistence(CardLateAllocationJpaRepository cardLateAllocations, CardProposalInvoiceJpaRepository cardProposalInvoices, BillingLineJpaRepository billingLines, CardPurchaseJpaRepository cardPurchases, CardObligationJpaRepository cardObligations, CardInvoiceJpaRepository cardInvoices, CardInvoiceItemJpaRepository cardInvoiceItems, CardAttemptJpaRepository cardAttempts, CardRefundRequestJpaRepository cardRefundRequests, CardRefundJpaRepository cardRefunds, CardPaymentJpaRepository cardPayments, CardSettlementJpaRepository cardSettlements, CardProposalJpaRepository cardProposals, CardAgreementJpaRepository cardAgreements, CardLateEpisodeJpaRepository cardLateEpisodes, CardDueDayChangeJpaRepository cardDueDayChanges, CardPaymentPreferenceJpaRepository cardPaymentPreferences, CardFinancialOperationJpaRepository cardFinancialOperations, CardAgreementInstallmentJpaRepository cardAgreementInstallments) {
this.cardLateAllocations=cardLateAllocations;
this.cardProposalInvoices=cardProposalInvoices;
        this.billingLines = billingLines;
        this.cardPurchases = cardPurchases;
        this.cardObligations = cardObligations;
        this.cardInvoices = cardInvoices;
        this.cardInvoiceItems = cardInvoiceItems;
        this.cardAttempts = cardAttempts;
        this.cardRefundRequests = cardRefundRequests;
        this.cardRefunds = cardRefunds;
        this.cardPayments = cardPayments;
        this.cardSettlements = cardSettlements;
        this.cardProposals = cardProposals;
        this.cardAgreements = cardAgreements;
        this.cardLateEpisodes = cardLateEpisodes;
        this.cardDueDayChanges = cardDueDayChanges;
        this.cardPaymentPreferences = cardPaymentPreferences;
        this.cardFinancialOperations = cardFinancialOperations;
        this.cardAgreementInstallments = cardAgreementInstallments;
    }
    @Override public CardFinanceState load(UUID customerId) {
        var state=new CardFinanceState();
        state.billingLines.addAll(billingLines.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardPurchases.addAll(cardPurchases.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardObligations.addAll(cardObligations.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardInvoices.addAll(cardInvoices.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardInvoiceItems.addAll(cardInvoiceItems.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardAttempts.addAll(cardAttempts.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardRefundRequests.addAll(cardRefundRequests.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardRefunds.addAll(cardRefunds.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardPayments.addAll(cardPayments.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardSettlements.addAll(cardSettlements.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardProposals.addAll(cardProposals.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardAgreements.addAll(cardAgreements.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardLateEpisodes.addAll(cardLateEpisodes.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardDueDayChanges.addAll(cardDueDayChanges.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardPaymentPreferences.addAll(cardPaymentPreferences.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardFinancialOperations.addAll(cardFinancialOperations.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardObligations.addAll(cardAgreementInstallments.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardProposalInvoices.addAll(cardProposalInvoices.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        state.cardLateAllocations.addAll(cardLateAllocations.findByCustomerId(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList());
        return state;
    }
    @Override public void save(CardFinanceState state) {
        billingLines.saveAllAndFlush(java.util.Objects.requireNonNull(state.billingLines.stream().map(BillingLineData::new).toList()));
        cardPurchases.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardPurchases.stream().map(CardPurchaseData::new).toList()));
        cardObligations.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardObligations.stream().filter(row -> !row.sourceType().equals("AGREEMENT")).map(CardObligationData::new).toList()));
        cardInvoices.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardInvoices.stream().map(CardInvoiceData::new).toList()));
        cardInvoiceItems.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardInvoiceItems.stream().map(CardInvoiceItemData::new).toList()));
        cardAttempts.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardAttempts.stream().map(CardAttemptData::new).toList()));
        cardRefundRequests.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardRefundRequests.stream().map(CardRefundRequestData::new).toList()));
        cardRefunds.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardRefunds.stream().map(CardRefundData::new).toList()));
        cardPayments.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardPayments.stream().map(CardPaymentData::new).toList()));
        cardSettlements.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardSettlements.stream().map(CardSettlementData::new).toList()));
        cardProposals.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardProposals.stream().map(CardProposalData::new).toList()));
        cardAgreements.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardAgreements.stream().map(CardAgreementData::new).toList()));
        cardLateEpisodes.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardLateEpisodes.stream().map(CardLateEpisodeData::new).toList()));
        cardDueDayChanges.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardDueDayChanges.stream().map(CardDueDayChangeData::new).toList()));
        cardPaymentPreferences.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardPaymentPreferences.stream().map(CardPaymentPreferenceData::new).toList()));
        cardFinancialOperations.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardFinancialOperations.stream().map(CardFinancialOperationData::new).toList()));
        cardAgreementInstallments.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardObligations.stream().filter(row -> row.sourceType().equals("AGREEMENT")).map(CardAgreementInstallmentData::new).toList()));
        cardProposalInvoices.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardProposalInvoices.stream().map(CardProposalInvoiceData::new).toList()));
        cardLateAllocations.saveAllAndFlush(java.util.Objects.requireNonNull(state.cardLateAllocations.stream().map(CardLateAllocationData::new).toList()));
    }
    @Override public List<UUID> customerIds() {return cardInvoices.findDistinctCustomerIds();}
}

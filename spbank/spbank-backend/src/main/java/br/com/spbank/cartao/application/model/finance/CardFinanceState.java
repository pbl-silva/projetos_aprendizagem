package br.com.spbank.cartao.application.model.finance;
import java.util.*;

public final class CardFinanceState {
 public final List<CardLateAllocation> cardLateAllocations=new ArrayList<>();
 public final List<CardProposalInvoice> cardProposalInvoices=new ArrayList<>();
    public final List<BillingLine> billingLines = new ArrayList<>();
    public final List<CardPurchase> cardPurchases = new ArrayList<>();
    public final List<CardObligation> cardObligations = new ArrayList<>();
    public final List<CardInvoice> cardInvoices = new ArrayList<>();
    public final List<CardInvoiceItem> cardInvoiceItems = new ArrayList<>();
    public final List<CardAttempt> cardAttempts = new ArrayList<>();
    public final List<CardRefundRequest> cardRefundRequests = new ArrayList<>();
    public final List<CardRefund> cardRefunds = new ArrayList<>();
    public final List<CardPayment> cardPayments = new ArrayList<>();
    public final List<CardSettlement> cardSettlements = new ArrayList<>();
    public final List<CardProposal> cardProposals = new ArrayList<>();
    public final List<CardAgreement> cardAgreements = new ArrayList<>();
    public final List<CardLateEpisode> cardLateEpisodes = new ArrayList<>();
    public final List<CardDueDayChange> cardDueDayChanges = new ArrayList<>();
    public final List<CardPaymentPreference> cardPaymentPreferences = new ArrayList<>();
    public final List<CardFinancialOperation> cardFinancialOperations = new ArrayList<>();
    public static <T> void put(List<? super T> rows,T row) {
        if (!(row instanceof CardFinanceRecord record)) throw new IllegalArgumentException("Registro financeiro obrigatório");
        rows.removeIf(old -> {
            if (!(old instanceof CardFinanceRecord previous)) throw new IllegalArgumentException("Coleção financeira inválida");
            return previous.id().equals(record.id());
        });
        rows.add(row);
    }
}

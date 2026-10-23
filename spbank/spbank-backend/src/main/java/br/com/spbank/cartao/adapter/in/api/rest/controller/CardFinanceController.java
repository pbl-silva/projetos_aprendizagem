package br.com.spbank.cartao.adapter.in.api.rest.controller;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CardFinanceDtos.*;
import br.com.spbank.cartao.adapter.out.document.CardInvoiceDocumentExporter;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.finance.*;
import br.com.spbank.cartao.application.usecase.CardFinanceService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cards")
public final class CardFinanceController {
    private final CardFinanceService finance;
    private final CardInvoiceDocumentExporter exporter;
    public CardFinanceController(CardFinanceService finance,CardInvoiceDocumentExporter exporter){this.finance=finance;this.exporter=exporter;}
    public record Overview(List<BillingLine> billingLines,List<CardPurchase> purchases,List<CardObligation> installments,
            List<CardInvoice> invoices,List<CardAttempt> attempts,List<CardRefundRequest> refundRequests,List<CardRefund> refunds,
            List<CardPayment> payments,List<CardProposal> proposals,List<CardAgreement> agreements,List<CardLateEpisode> lateEpisodes,
            List<CardDueDayChange> dueDayChanges,List<CardPaymentPreference> preferences){}
    private Overview view(CardFinanceState state){return new Overview(state.billingLines,state.cardPurchases,state.cardObligations,state.cardInvoices,
        state.cardAttempts,state.cardRefundRequests,state.cardRefunds,state.cardPayments,state.cardProposals,state.cardAgreements,state.cardLateEpisodes,state.cardDueDayChanges,state.cardPaymentPreferences);}
    @GetMapping("/finance") public Overview overview(@RequestAttribute(CUSTOMER_ID)UUID customer){return view(finance.overview(customer));}
    @PostMapping("/purchases/preview") public Object preview(@RequestAttribute(CUSTOMER_ID)UUID customer,@Valid @RequestBody Purchase body){return finance.preview(customer,body.command());}
    @PostMapping("/purchases") public CardFinanceResult purchase(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Purchase body){return finance.purchase(customer,key,body.command());}
    @GetMapping("/purchases") public Object purchases(@RequestAttribute(CUSTOMER_ID)UUID customer){return finance.overview(customer).cardPurchases;}
    @GetMapping("/purchase-attempts") public Object attempts(@RequestAttribute(CUSTOMER_ID)UUID customer){return finance.overview(customer).cardAttempts;}
    @PostMapping("/purchases/{id}/refund-requests") public Object refund(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@Valid @RequestBody Refund body){return finance.requestRefund(customer,key,id,body.principal(),body.reason(),body.confirmationPassword());}
    @GetMapping("/invoices") public Object invoices(@RequestAttribute(CUSTOMER_ID)UUID customer){return finance.overview(customer).cardInvoices;}
    @GetMapping("/invoices/{id}") public Object invoice(@RequestAttribute(CUSTOMER_ID)UUID customer,@PathVariable UUID id){return finance.invoice(customer,id);}
    @GetMapping("/due-day/{brand}") public Object dueDay(@RequestAttribute(CUSTOMER_ID)UUID customer,@PathVariable CardBrand brand){return finance.overview(customer).billingLines.stream().filter(l->l.brand()==brand).toList();}
    @PutMapping("/due-day") public Object dueDay(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody DueDay body){return finance.changeDueDay(customer,key,body.brand(),body.dueDay(),body.confirmationPassword());}
    @GetMapping("/invoices/payments/preference") public Object preference(@RequestAttribute(CUSTOMER_ID)UUID customer){return finance.overview(customer).cardPaymentPreferences;}
    @PutMapping("/invoices/payments/preference") public Object preference(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Preference body){return finance.preference(customer,key,body.accountId(),body.confirmationPassword());}
    @PostMapping("/invoices/payments/account") public Object account(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Payment body){return finance.payAccount(customer,key,body.brand(),body.amount(),body.accountId(),body.confirmationPassword());}
    @GetMapping("/invoices/payments/boletos") public Object boletos(@RequestAttribute(CUSTOMER_ID)UUID customer){return finance.overview(customer).cardPayments.stream().filter(p->p.method().equals("BOLETO")).toList();}
    @PostMapping("/invoices/payments/boletos") public Object boleto(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Payment body){return finance.boleto(customer,key,body.brand(),body.amount(),null,body.confirmationPassword());}
    @PostMapping("/installments/anticipation/preview") public Object anticipatePreview(@RequestAttribute(CUSTOMER_ID)UUID customer,@Valid @RequestBody AnticipationPreview body){return finance.anticipation(customer,body.brand(),body.sourceId(),"PURCHASE",body.count());}
    @PostMapping("/installments/anticipation/account") public Object anticipate(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Anticipation body){return finance.anticipate(customer,key,body.brand(),body.sourceId(),"PURCHASE",body.count(),body.expectedAmount(),body.accountId(),body.confirmationPassword());}
    @PostMapping("/installments/anticipation/agreements/preview") public Object agreementPreview(@RequestAttribute(CUSTOMER_ID)UUID customer,@Valid @RequestBody AnticipationPreview body){return finance.anticipation(customer,body.brand(),body.sourceId(),"AGREEMENT",body.count());}
    @PostMapping("/installments/anticipation/agreements/account") public Object agreementAnticipate(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Anticipation body){return finance.anticipate(customer,key,body.brand(),body.sourceId(),"AGREEMENT",body.count(),body.expectedAmount(),body.accountId(),body.confirmationPassword());}
    @GetMapping("/agreements") public Object agreements(@RequestAttribute(CUSTOMER_ID)UUID customer){return finance.overview(customer).cardAgreements;}
    @GetMapping("/agreements/proposals") public Object proposals(@RequestAttribute(CUSTOMER_ID)UUID customer){return finance.overview(customer).cardProposals;}
    @PostMapping("/agreements/proposals") public Object propose(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Proposal body){return finance.propose(customer,key,body.brand(),body.invoiceIds(),body.previousAgreementId(),body.installments(),body.confirmationPassword());}
    @PostMapping("/agreements/proposals/{id}/account") public Object entry(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@Valid @RequestBody ProposalPayment body){return finance.payProposal(customer,key,id,body.accountId(),body.confirmationPassword());}
    @PostMapping("/agreements/proposals/{id}/boletos") public Object entryBoleto(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@PathVariable UUID id,@Valid @RequestBody Password body){var state=finance.overview(customer);var proposal=state.cardProposals.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new br.com.spbank.shared.application.exception.NotFoundException("CARD_PROPOSAL_NOT_FOUND"));return finance.boleto(customer,key,proposal.brand(),proposal.entry(),id,body.confirmationPassword());}
    @PostMapping("/credit/reconciliation/{brand}") public Object reconcile(@RequestAttribute(CUSTOMER_ID)UUID customer,@RequestHeader("Idempotency-Key")UUID key,@PathVariable CardBrand brand,@Valid @RequestBody Password body){return finance.reconcile(customer,key,brand,body.confirmationPassword());}
    @GetMapping("/invoices/{id}/export.csv") public ResponseEntity<byte[]> csv(@RequestAttribute(CUSTOMER_ID)UUID customer,@PathVariable UUID id){return ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Disposition","attachment; filename=fatura-"+id+".csv").contentType(new MediaType("text","csv",java.util.Objects.requireNonNull(java.nio.charset.StandardCharsets.UTF_8))).body(exporter.csv(finance.invoice(customer,id)));}
    @GetMapping("/invoices/{id}/export.pdf") public ResponseEntity<byte[]> pdf(@RequestAttribute(CUSTOMER_ID)UUID customer,@PathVariable UUID id){return ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Disposition","attachment; filename=fatura-"+id+".pdf").contentType(MediaType.APPLICATION_PDF).body(exporter.pdf(finance.invoice(customer,id)));}
}

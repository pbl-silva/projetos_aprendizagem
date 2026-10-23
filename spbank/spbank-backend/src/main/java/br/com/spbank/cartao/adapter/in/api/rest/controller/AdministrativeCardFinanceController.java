package br.com.spbank.cartao.adapter.in.api.rest.controller;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CardFinanceDtos.*;
import br.com.spbank.cartao.application.usecase.CardFinanceService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/cards")
public final class AdministrativeCardFinanceController {
    private final CardFinanceService finance;
    public AdministrativeCardFinanceController(CardFinanceService finance){this.finance=finance;}
    @GetMapping("/invoices/payments/boletos") public Object allBoletos(@RequestAttribute(ADMINISTRATOR_ID)UUID admin){finance.administrator(admin);return finance.customerIds().stream().flatMap(customer->finance.administrativeOverview(admin,customer).cardPayments.stream()).filter(p->p.method().equals("BOLETO")&&p.status().equals("PENDING")).toList();}
    @PostMapping("/invoices/payments/boletos/{id}/confirmation") public Object allConfirm(@RequestAttribute(ADMINISTRATOR_ID)UUID admin,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Confirmation body){finance.administrator(admin);for(var customer:finance.customerIds())if(finance.administrativeOverview(admin,customer).cardPayments.stream().anyMatch(payment->payment.id().equals(id)))return finance.confirmBoleto(admin,customer,key,id,body.reason());throw new br.com.spbank.shared.application.exception.NotFoundException("CARD_PAYMENT_NOT_FOUND");}
    @GetMapping("/customers/{customerId}/refund-requests") public Object refunds(@RequestAttribute(ADMINISTRATOR_ID)UUID admin,@PathVariable UUID customerId){return finance.administrativeOverview(admin,customerId).cardRefundRequests;}
    @PostMapping("/customers/{customerId}/refund-requests/{id}/decision") public Object decision(@RequestAttribute(ADMINISTRATOR_ID)UUID admin,@PathVariable UUID customerId,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Decision body){return finance.decideRefund(admin,customerId,key,id,body.approved(),body.reason());}
    @GetMapping("/customers/{customerId}/invoices/payments/boletos") public Object boletos(@RequestAttribute(ADMINISTRATOR_ID)UUID admin,@PathVariable UUID customerId){return finance.administrativeOverview(admin,customerId).cardPayments.stream().filter(p->p.method().equals("BOLETO")&&p.status().equals("PENDING")).toList();}
    @PostMapping("/customers/{customerId}/invoices/payments/boletos/{id}/confirmation") public Object confirm(@RequestAttribute(ADMINISTRATOR_ID)UUID admin,@PathVariable UUID customerId,@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@Valid @RequestBody Confirmation body){return finance.confirmBoleto(admin,customerId,key,id,body.reason());}
}

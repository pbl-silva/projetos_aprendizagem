package br.com.spbank.pagamento.adapter.in.api.rest.controller;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import br.com.spbank.pagamento.application.model.PaymentModels.*;
import br.com.spbank.pagamento.application.port.in.PaymentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/payments")
public class PaymentController {
 private final PaymentUseCase service;public PaymentController(PaymentUseCase service){this.service=service;}
 public record Study(@NotBlank @Size(max=100) String name,@NotNull @Positive @Digits(integer=8,fraction=2) BigDecimal amount){}
 public record PreviewRequest(@NotNull UUID accountId,@NotBlank @Size(max=100) String code,@Nullable @FutureOrPresent LocalDate scheduledFor){@Override public String toString(){return "PaymentPreview[redacted]";}}
 public record Confirm(@NotNull UUID accountId,@NotBlank @Size(max=100) String code,@Nullable @FutureOrPresent LocalDate scheduledFor,@NotBlank @Size(max=128) String confirmationPassword){@Override public String toString(){return "PaymentConfirmation[redacted]";}}
 public record RechargePreview(@NotNull UUID accountId,@NotNull UUID productId,@NotBlank @Size(max=25) String phone){@Override public String toString(){return "RechargePreview[redacted]";}}
 public record Recharge(@NotNull UUID accountId,@NotNull UUID productId,@NotBlank @Size(max=25) String phone,@NotBlank @Size(max=128) String confirmationPassword){@Override public String toString(){return "RechargeConfirmation[redacted]";}}
 @PostMapping("/study-charges") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public GeneratedCharge generate(@RequestAttribute(CUSTOMER_ID) UUID c,@Valid @RequestBody Study r){return service.generate(c,r.name(),r.amount());}
 @PostMapping("/preview") public Preview preview(@RequestAttribute(CUSTOMER_ID) UUID c,@Valid @RequestBody PreviewRequest r){return service.preview(c,r.accountId(),r.code(),r.scheduledFor());}
 @PostMapping public ResponseEntity<Payment> pay(@RequestAttribute(CUSTOMER_ID) UUID c,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Confirm r){var result=service.pay(c,key,new Command(r.accountId(),r.code(),r.scheduledFor(),r.confirmationPassword()));return ResponseEntity.status(result.status()==Status.SCHEDULED?202:201).body(result);}
 @GetMapping public List<Payment> history(@RequestAttribute(CUSTOMER_ID) UUID c){return service.history(c).stream().filter(p->p.kind().equals("BILL")).toList();}
 @GetMapping("/scheduled") public List<Payment> scheduled(@RequestAttribute(CUSTOMER_ID) UUID c){return service.history(c).stream().filter(p->p.kind().equals("BILL")&&p.status()==Status.SCHEDULED).toList();}
 @GetMapping("/{id}") public Payment get(@RequestAttribute(CUSTOMER_ID) UUID c,@PathVariable UUID id){return service.get(c,id);}
 @DeleteMapping("/{id}") public Payment cancel(@RequestAttribute(CUSTOMER_ID) UUID c,@PathVariable UUID id){return service.cancel(c,id);}
 @GetMapping("/recharges/products") public List<Product> products(@RequestAttribute(CUSTOMER_ID) UUID c){return service.products(c);}
 @PostMapping("/recharges/preview") public Preview rechargePreview(@RequestAttribute(CUSTOMER_ID) UUID c,@Valid @RequestBody RechargePreview r){return service.rechargePreview(c,r.accountId(),r.productId(),r.phone());}
 @PostMapping("/recharges") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Payment recharge(@RequestAttribute(CUSTOMER_ID) UUID c,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Recharge r){return service.recharge(c,key,new RechargeCommand(r.accountId(),r.productId(),r.phone(),r.confirmationPassword()));}
 @GetMapping("/recharges") public List<Payment> recharges(@RequestAttribute(CUSTOMER_ID) UUID c){return service.history(c).stream().filter(p->p.kind().equals("RECHARGE")).toList();}
 @GetMapping("/recharges/{id}") public Payment rechargeGet(@RequestAttribute(CUSTOMER_ID) UUID c,@PathVariable UUID id){var p=service.get(c,id);if(!p.kind().equals("RECHARGE"))throw new br.com.spbank.shared.application.exception.NotFoundException("PAYMENT_NOT_FOUND");return p;}
}

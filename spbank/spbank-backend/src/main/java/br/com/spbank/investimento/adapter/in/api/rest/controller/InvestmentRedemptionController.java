package br.com.spbank.investimento.adapter.in.api.rest.controller;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import br.com.spbank.investimento.application.port.in.InvestmentRedemptionUseCase;
import br.com.spbank.investimento.application.model.InvestmentRedemption;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/investments/redemptions")
public class InvestmentRedemptionController {
 private final InvestmentRedemptionUseCase service;
 public InvestmentRedemptionController(InvestmentRedemptionUseCase service){this.service=service;}
 public record Request(@NotNull UUID positionId,@NotNull @Positive @Digits(integer=17,fraction=2) BigDecimal amount,@NotBlank @Size(max=128) String confirmationPassword){@Override public String toString(){return "RedemptionRequest[redacted]";}}
 public record PreviewRequest(@NotNull UUID positionId,@NotNull @Positive @Digits(integer=17,fraction=2) BigDecimal amount){}
 @GetMapping public List<InvestmentRedemption> history(@RequestAttribute(CUSTOMER_ID) UUID customer){return service.history(customer);}
 @PostMapping("/preview") public InvestmentRedemptionUseCase.Preview preview(@RequestAttribute(CUSTOMER_ID) UUID customer,@Valid @RequestBody PreviewRequest request){return service.previewAmount(customer,request.positionId(),request.amount());}
 @PostMapping public InvestmentRedemption request(@RequestAttribute(CUSTOMER_ID) UUID customer,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Request request){return service.requestAmount(customer,request.positionId(),key,request.amount(),request.confirmationPassword());}
}

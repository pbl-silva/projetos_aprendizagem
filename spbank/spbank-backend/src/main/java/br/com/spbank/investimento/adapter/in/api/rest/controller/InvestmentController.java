package br.com.spbank.investimento.adapter.in.api.rest.controller;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import br.com.spbank.investimento.adapter.in.api.rest.dto.InvestmentDtos;
import br.com.spbank.investimento.application.model.InvestmentApplication;
import br.com.spbank.investimento.application.port.in.InvestmentUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/investments")
public final class InvestmentController {
    private final InvestmentUseCase investments;
    public InvestmentController(InvestmentUseCase investments){this.investments=investments;}
    @GetMapping("/products") public List<InvestmentDtos.Product> products(@RequestAttribute(CUSTOMER_ID) UUID customer){return investments.products(customer).stream().map(p->InvestmentDtos.Product.from(p.product(),p.quote(),p.notice())).toList();}
    @PostMapping("/preview") public InvestmentDtos.Preview preview(@RequestAttribute(CUSTOMER_ID) UUID customer,@Valid @RequestBody InvestmentDtos.PreviewRequest request){return InvestmentDtos.Preview.from(investments.preview(customer,request.accountId(),request.productId(),request.amount()));}
    @PostMapping public ResponseEntity<InvestmentApplication> apply(@RequestAttribute(CUSTOMER_ID) UUID customer,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody InvestmentDtos.Confirmation request){
        var result=investments.apply(customer,key,request.command());
        var location=URI.create("/api/v1/investments/applications/"+result.id());
        return ResponseEntity.status(result.status()==InvestmentApplication.Status.PENDING?202:201).location(java.util.Objects.requireNonNull(location)).body(result);}
    @GetMapping("/positions") public List<InvestmentDtos.Position> positions(@RequestAttribute(CUSTOMER_ID) UUID customer){return investments.positions(customer).stream().map(p->InvestmentDtos.Position.from(p)).toList();}
    @GetMapping("/applications/{id}") public InvestmentApplication application(@RequestAttribute(CUSTOMER_ID) UUID customer,@PathVariable UUID id){return investments.application(customer,id);}
}

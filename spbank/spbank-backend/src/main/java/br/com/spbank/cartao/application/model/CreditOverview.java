package br.com.spbank.cartao.application.model;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.util.List;
public record CreditOverview(CustomerPlan customerPlan, BigDecimal ceiling,
        BigDecimal approvedTotal, List<CreditRequest> requests) {
    public CreditOverview { requests = List.copyOf(requests); }
}

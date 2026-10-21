
package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CreditOverview;
import br.com.spbank.shared.application.model.CustomerPlan;

import java.math.BigDecimal;
import java.util.List;

public record CreditOverviewDto(

        CustomerPlan customerPlan,
        BigDecimal ceiling,
        BigDecimal approvedTotal,
        List<CreditRequestDto> requests

) {

    public static CreditOverviewDto from(
            CreditOverview overview
    ) {

        return new CreditOverviewDto(
                overview.customerPlan(),
                overview.ceiling(),
                overview.approvedTotal(),
                overview.requests()
                        .stream()
                        .map(CreditRequestDto::from)
                        .toList()
        );
    }
}

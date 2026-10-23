package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.CustomerCardCredit;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record CreditAllocationSummaryDto(BigDecimal approvedTotal, BigDecimal allocatedTotal,
        BigDecimal unallocatedTotal, List<Line> lines) {

    public record Line(CardBrand brand, BigDecimal allocated, BigDecimal committed,
            BigDecimal available, BigDecimal excess) {
    }

    public static CreditAllocationSummaryDto from(CustomerCardCredit credit) {
        return new CreditAllocationSummaryDto(credit.approvedTotal(), credit.allocatedTotal(),
            credit.unallocatedTotal(), Arrays.stream(CardBrand.values()).map(brand -> {
                var line = credit.lines().get(brand);
                return new Line(brand, line.allocated(), line.committed(), line.available(), line.excess());
            }).toList());
    }
}

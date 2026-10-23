package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

public record CreditAllocationDto(
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal brisa,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal mistershard,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal elu,
        @NotBlank @Size(max = 128) String confirmationPassword) {

    public Map<CardBrand, BigDecimal> allocation() {
        return Map.of(CardBrand.BRISA, brisa, CardBrand.MISTERSHARD, mistershard, CardBrand.ELU, elu);
    }

    @Override
    public String toString() {
        return "CreditAllocationDto[brisa=" + brisa + ", mistershard=" + mistershard
            + ", elu=" + elu + ", confirmationPassword=[REDACTED]]";
    }
}

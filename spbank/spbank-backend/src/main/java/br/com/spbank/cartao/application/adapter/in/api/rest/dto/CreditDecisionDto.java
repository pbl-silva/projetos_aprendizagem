
package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CreditDecision;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreditDecisionDto(

        @NotNull
        CreditDecision decision,

        @DecimalMin("0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal offeredTotal,

        @NotBlank
        @Size(max = 240)
        String reason

) {
}

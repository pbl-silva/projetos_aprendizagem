package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixKeyType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PixCreationDto(
    @NotNull(message = "{pix.key.type.required}")
    PixKeyType keyType,

    @NotBlank(message = "{pix.key.required}")
    @Size(max = 120)
    String key,

    @NotNull(message = "{pix.amount.required}")
    @DecimalMin(
        value = "0.01",
        message = "{pix.amount.minimum}"
    )
    @Digits(
        integer = 17,
        fraction = 2,
        message = "{pix.amount.format}"
    )
    BigDecimal amount,

    @FutureOrPresent(
        message = "{pix.date.invalid}"
    )
    LocalDate scheduledFor,

    @Valid
    PixRecurrenceDto recurrence,

    boolean saveFavorite
) {

    @Override
    public String toString() {
        return "PixCreationDto[keyType="
            + keyType
            + ", key=[REDACTED], amount="
            + amount
            + ", scheduledFor="
            + scheduledFor
            + ", recurrence="
            + recurrence
            + ", saveFavorite="
            + saveFavorite
            + "]";
    }
}
package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixFrequency;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PixRecurrenceDto(
    @NotNull(message = "{pix.recurrence.invalid}")
    PixFrequency frequency,

    @NotNull(message = "{pix.recurrence.invalid}")
    @Min(
        value = 2,
        message = "{pix.recurrence.invalid}"
    )
    @Max(
        value = 24,
        message = "{pix.recurrence.invalid}"
    )
    Integer occurrences
) {
}
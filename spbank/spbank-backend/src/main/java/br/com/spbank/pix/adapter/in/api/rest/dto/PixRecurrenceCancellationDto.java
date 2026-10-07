package br.com.spbank.pix.adapter.in.api.rest.dto;

import java.util.UUID;

public record PixRecurrenceCancellationDto(
    UUID recurrenceId,
    int cancelledOccurrences
) {
}
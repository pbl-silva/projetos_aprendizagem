package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.validation.constraints.*;

public final class CardInstrumentDtos {
    private CardInstrumentDtos() {}

    public record Issue(@NotNull CardBrand brand,@NotNull @Min(5) @Max(25) Integer dueDay) {}
    public record Activate(@NotBlank @Size(max = 128) String confirmationPassword,
            @NotNull @Pattern(regexp = "[0-9]{4}") String pin,
            @NotNull @Pattern(regexp = "[0-9]{4}") String pinConfirmation) {
        @Override public String toString() { return "Activate[REDACTED]"; }
    }
    public record ChangePin(@NotNull @Pattern(regexp = "[0-9]{4}") String currentPin,
            @NotNull @Pattern(regexp = "[0-9]{4}") String newPin,
            @NotNull @Pattern(regexp = "[0-9]{4}") String pinConfirmation) {
        @Override public String toString() { return "ChangePin[REDACTED]"; }
    }
    public record RecoverPin(@NotBlank @Size(max = 128) String confirmationPassword,
            @NotNull @Pattern(regexp = "[0-9]{4}") String newPin,
            @NotNull @Pattern(regexp = "[0-9]{4}") String pinConfirmation) {
        @Override public String toString() { return "RecoverPin[REDACTED]"; }
    }
    public record Reveal(@NotNull @Pattern(regexp = "[0-9]{4}") String pin) {
        @Override public String toString() { return "Reveal[REDACTED]"; }
    }
    public record Block(@NotBlank @Size(max = 240) String reason) {}
    public record AdministrativeBlock(@NotNull @Pattern(regexp = "VOLUNTARY|ADMINISTRATIVE") String cause,
            @NotNull Boolean blocked, @NotBlank @Size(max = 240) String reason) {}
}

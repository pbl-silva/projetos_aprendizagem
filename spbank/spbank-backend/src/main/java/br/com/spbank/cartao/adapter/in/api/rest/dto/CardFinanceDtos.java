package br.com.spbank.cartao.adapter.in.api.rest.dto;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.finance.CardPurchaseCommand;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public final class CardFinanceDtos {
    private CardFinanceDtos(){}
    public record Purchase(@NotBlank @Pattern(regexp="[0-9]{16}") String pan,@NotBlank @Pattern(regexp="[0-9]{4}-[0-9]{2}") String expiry,
            @NotBlank @Pattern(regexp="[0-9]{3}") String cvv,@NotBlank @Size(max=240) String merchant,
            @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@Min(1) @Max(12) int installments,
            @NotBlank @Pattern(regexp="[0-9]{4}") String pin){
        public CardPurchaseCommand command(){return new CardPurchaseCommand(pan,expiry,cvv,merchant,amount,installments,pin);}
        @Override public String toString(){return "Purchase[REDACTED]";}
    }
    public record Refund(@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal principal,
            @NotBlank @Size(max=240) String reason,@NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "Refund[REDACTED]";}
    }
    public record Decision(@NotNull Boolean approved,@NotBlank @Size(max=240) String reason){}
    public record Confirmation(@NotBlank @Size(max=240) String reason){}
    public record DueDay(@NotNull CardBrand brand,@Min(5) @Max(25) int dueDay,@NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "DueDay[REDACTED]";}
    }
    public record Preference(@NotNull UUID accountId,@NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "Preference[REDACTED]";}
    }
    public record Payment(@NotNull CardBrand brand,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,
            UUID accountId,@NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "Payment[REDACTED]";}
    }
    public record Anticipation(@NotNull CardBrand brand,@NotNull UUID sourceId,@Min(1) @Max(12) int count,
            @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal expectedAmount,UUID accountId,
            @NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "Anticipation[REDACTED]";}
    }
    public record AnticipationPreview(@NotNull CardBrand brand,@NotNull UUID sourceId,@Min(1) @Max(12) int count){}
    public record Proposal(@NotNull CardBrand brand,List<UUID> invoiceIds,UUID previousAgreementId,@Min(2) @Max(12) int installments,
            @NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "Proposal[REDACTED]";}
    }
    public record ProposalPayment(UUID accountId,@NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "ProposalPayment[REDACTED]";}
    }
    public record Password(@NotBlank @Size(max=128) String confirmationPassword){
        @Override public String toString(){return "Password[REDACTED]";}
    }
}

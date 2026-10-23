package br.com.spbank.investimento.adapter.in.api.rest.dto;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.in.InvestmentUseCase;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public final class InvestmentDtos {
    private InvestmentDtos(){}
    public record PreviewRequest(@NotNull UUID accountId,@NotNull UUID productId,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount){}
    public record Confirmation(@NotNull UUID accountId,@NotNull UUID productId,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,
        @NotBlank @Size(max=128) String confirmationPassword){
        public InvestmentCommand command(){return new InvestmentCommand(accountId,productId,amount,confirmationPassword);}
        @Override public String toString(){return "Confirmation[accountId="+accountId+", productId="+productId+", amount="+amount+", confirmationPassword=[REDACTED]]";}
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Product(UUID id,String name,InvestmentProduct.Type type,BigDecimal minimumApplication,BigDecimal minimumRemaining,
        String risk,boolean fgcEligible,InvestmentProduct.FundTerms fund,InvestmentProduct.CdbTerms cdb,InvestmentQuote quote,String notice){
        public static Product from(InvestmentProduct p,InvestmentQuote quote,String notice){return new Product(p.id(),p.name(),p.type(),p.minimumApplication(),p.minimumRemaining(),p.risk(),p.fgcEligible(),p.fund(),p.cdb(),quote,notice);}
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Preview(UUID accountId,Product product,BigDecimal amount,LocalDate effectiveDate,InvestmentQuote quote,
        BigDecimal estimatedUnits,LocalDate estimatedMaturity,boolean gross,String notice){
        public static Preview from(InvestmentUseCase.Preview p){return new Preview(p.accountId(),Product.from(p.product(),p.quote(),p.notice()),p.amount(),p.effectiveDate(),p.quote(),p.estimatedUnits(),p.estimatedMaturity(),p.gross(),p.notice());}
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Position(UUID id,UUID applicationId,UUID accountId,Product product,BigDecimal principal,LocalDate effectiveDate,
        BigDecimal units,BigDecimal unitPrice,LocalDate quoteDate,LocalDate maturityDate,String valuationBasis,boolean gross,
        InvestmentValuation.Status valuationStatus,LocalDate referenceDate,LocalDate calculatedThrough,BigDecimal grossValue,
        BigDecimal grossEarnings,BigDecimal currentUnitPrice,LocalDate currentQuoteDate,int accruedBusinessDays,
        java.util.List<InvestmentCdiPeriod> cdiSources,String valuationErrorCode,String calculationFingerprint,String algorithmVersion){
        public static Position from(InvestmentValuation value){var p=value.position();var quote=value.currentQuote();
            return new Position(p.id(),p.applicationId(),p.accountId(),Product.from(p.product(),null,"Condições contratadas preservadas. Valores brutos, sem impostos; o valor pode variar e não representa garantia de retorno."),p.principal(),p.effectiveDate(),p.units(),p.unitPrice(),p.quoteDate(),p.maturityDate(),"CURRENT_GROSS",true,
                value.status(),value.referenceDate(),value.calculatedThrough(),value.grossValue(),value.grossEarnings(),quote==null?null:quote.price(),quote==null?null:quote.date(),value.accruedBusinessDays(),value.cdiPeriods(),value.errorCode(),value.calculationFingerprint(),value.algorithmVersion());
        }
    }
}

package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;
public record InvestmentValuation(InvestmentPosition position,LocalDate referenceDate,Status status,
        @Nullable BigDecimal grossValue,@Nullable BigDecimal grossEarnings,@Nullable LocalDate calculatedThrough,
        @Nullable InvestmentQuote currentQuote,List<InvestmentCdiPeriod> cdiPeriods,int accruedBusinessDays,
        @Nullable String errorCode,String calculationFingerprint,String algorithmVersion) {
    public enum Status { AVAILABLE,DATA_UNAVAILABLE }
    public InvestmentValuation { cdiPeriods=List.copyOf(cdiPeriods); }
}

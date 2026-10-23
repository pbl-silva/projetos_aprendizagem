package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
public record InvestmentCdiPeriod(LocalDate validFrom,LocalDate validThrough,BigDecimal dailyFactor,int businessDaysBasis,String version) {
    public InvestmentCdiPeriod {
        Objects.requireNonNull(validFrom);Objects.requireNonNull(validThrough);Objects.requireNonNull(dailyFactor);Objects.requireNonNull(version);
        if(validThrough.isBefore(validFrom)||dailyFactor.signum()<=0||businessDaysBasis!=252||version.isBlank()||version.length()>80)
            throw new IllegalArgumentException("Fator CDI interno inválido");
    }
    public boolean covers(LocalDate date){return !date.isBefore(validFrom)&&!date.isAfter(validThrough);}
}

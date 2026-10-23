package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record InvestmentQuote(UUID productId,LocalDate date,BigDecimal price) {
    public InvestmentQuote {if(price.signum()<=0)throw new IllegalArgumentException("Cota inválida");}
}

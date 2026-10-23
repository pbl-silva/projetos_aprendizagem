package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.util.UUID;
public record InvestmentCommand(UUID accountId,UUID productId,BigDecimal amount,String confirmationPassword) {
    @Override public String toString(){return "InvestmentCommand[accountId="+accountId+", productId="+productId+", amount="+amount+", confirmationPassword=[REDACTED]]";}
}

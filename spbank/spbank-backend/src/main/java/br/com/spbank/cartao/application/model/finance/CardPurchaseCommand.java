package br.com.spbank.cartao.application.model.finance;
import java.math.BigDecimal;
public record CardPurchaseCommand(String pan,String expiry,String cvv,String merchant,BigDecimal amount,int installments,String pin){
    @Override public String toString(){return "CardPurchaseCommand[REDACTED]";}
}

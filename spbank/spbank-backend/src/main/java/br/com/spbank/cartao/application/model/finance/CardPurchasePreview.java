package br.com.spbank.cartao.application.model.finance;
import java.math.BigDecimal;
import java.util.List;
import br.com.spbank.cartao.application.service.CardFinancePolicy.Part;
public record CardPurchasePreview(BigDecimal principal,BigDecimal interest,BigDecimal total,List<Part> installments){}

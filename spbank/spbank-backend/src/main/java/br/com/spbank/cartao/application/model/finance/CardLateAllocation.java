package br.com.spbank.cartao.application.model.finance;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import br.com.spbank.cartao.application.model.CardBrand;
public record CardLateAllocation(UUID id, UUID customerId, CardBrand brand, UUID paymentId, UUID obligationId, BigDecimal fine, BigDecimal mora, Instant createdAt) implements CardFinanceRecord {}

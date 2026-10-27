package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
public record InvestmentRedemption(UUID id, UUID customerId, UUID positionId, UUID key, String fingerprint,
 BigDecimal units, BigDecimal amount, LocalDate conversionDate, LocalDate paymentDate, String status,
 Instant requestedAt, @Nullable Instant paidAt) {}

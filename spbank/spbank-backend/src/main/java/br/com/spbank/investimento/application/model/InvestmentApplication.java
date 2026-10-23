package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
public record InvestmentApplication(UUID id,UUID customerId,UUID accountId,UUID productId,BigDecimal amount,
        LocalDate effectiveDate,Status status,Instant requestedAt,@Nullable Instant processedAt,
        @Nullable UUID positionId,@Nullable BigDecimal balanceAfter,@Nullable String failureCode) {
    public enum Status { PENDING,COMPLETED,FAILED }
}

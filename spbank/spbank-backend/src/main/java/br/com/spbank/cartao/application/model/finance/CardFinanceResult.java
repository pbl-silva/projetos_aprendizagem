package br.com.spbank.cartao.application.model.finance;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record CardFinanceResult(UUID operationId,String action,String status,UUID objectId,
        BigDecimal amount,BigDecimal discount,BigDecimal creditedAmount,String message,Instant occurredAt){}

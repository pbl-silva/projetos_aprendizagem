package br.com.spbank.investimento.application.model;
import java.time.Instant;
import java.util.UUID;
public record InvestmentOperation(UUID id,UUID customerId,UUID key,String fingerprint,String responseJson,Instant createdAt) {}

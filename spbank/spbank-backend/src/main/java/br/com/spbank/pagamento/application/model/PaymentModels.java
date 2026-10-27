package br.com.spbank.pagamento.application.model;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
public final class PaymentModels {
 private PaymentModels(){}
 public record Charge(String hash,String mask,String beneficiary,String maskedDocument,BigDecimal amount,LocalDate dueDate,boolean active){}
 public record Product(UUID id,String carrier,BigDecimal amount,boolean active){}
 public enum Status { SCHEDULED,PROCESSING,COMPLETED,FAILED,CANCELLED }
 public record Payment(UUID id,UUID customerId,UUID accountId,String maskedAccount,UUID key,String fingerprint,String kind,
  String targetHash,String targetMask,String beneficiary,BigDecimal amount,LocalDate dueDate,LocalDate scheduledFor,Status status,
  Instant createdAt,@Nullable Instant completedAt,@Nullable BigDecimal balanceAfter,@Nullable String settlementReference,@Nullable String failureCode){}
 public record Preview(UUID accountId,String kind,String beneficiary,String maskedBeneficiaryDocument,String targetMask,BigDecimal amount,LocalDate dueDate,LocalDate scheduledFor,@Nullable BigDecimal estimatedBalanceAfter,boolean simulated){}
 public record GeneratedCharge(String code,Charge charge){}
 public record Command(UUID accountId,String code,@Nullable LocalDate scheduledFor,String confirmationPassword){
  @Override public String toString(){return "PaymentCommand[redacted]";}}
 public record RechargeCommand(UUID accountId,UUID productId,String phone,String confirmationPassword){
  @Override public String toString(){return "RechargeCommand[redacted]";}}
}

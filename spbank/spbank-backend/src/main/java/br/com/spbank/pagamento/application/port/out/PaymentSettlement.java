package br.com.spbank.pagamento.application.port.out;
import java.math.BigDecimal;
import java.util.UUID;
public interface PaymentSettlement { String settle(UUID id,String kind,String targetHash,BigDecimal amount); }

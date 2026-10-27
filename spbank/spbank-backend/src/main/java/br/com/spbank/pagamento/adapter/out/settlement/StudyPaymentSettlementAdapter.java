package br.com.spbank.pagamento.adapter.out.settlement;
import br.com.spbank.pagamento.application.port.out.PaymentSettlement;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;
@Component
public class StudyPaymentSettlementAdapter implements PaymentSettlement {
 public String settle(UUID id,String kind,String targetHash,BigDecimal amount){return "STUDY-"+kind+"-"+id;}
}

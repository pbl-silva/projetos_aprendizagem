package br.com.spbank.investimento.application.port.in;
import br.com.spbank.investimento.application.model.InvestmentRedemption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public interface InvestmentRedemptionUseCase {
 record Preview(UUID positionId,BigDecimal units,BigDecimal estimatedAmount,BigDecimal remainingUnits,LocalDate conversionDate,LocalDate paymentDate){}
 List<InvestmentRedemption> history(UUID customer);
 Preview previewAmount(UUID customer,UUID id,BigDecimal amount);
 InvestmentRedemption requestAmount(UUID customer,UUID id,UUID key,BigDecimal amount,String password);
 void process(UUID customer);
}

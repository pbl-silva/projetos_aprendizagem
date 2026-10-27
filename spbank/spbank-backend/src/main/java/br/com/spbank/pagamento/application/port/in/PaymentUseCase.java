package br.com.spbank.pagamento.application.port.in;
import br.com.spbank.pagamento.application.model.PaymentModels.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.jspecify.annotations.Nullable;
public interface PaymentUseCase {
 GeneratedCharge generate(UUID customer,String name,BigDecimal amount);
 Preview preview(UUID customer,UUID account,String code,@Nullable LocalDate date);
 Preview rechargePreview(UUID customer,UUID account,UUID product,String phone);
 Payment pay(UUID customer,UUID key,Command command);
 Payment recharge(UUID customer,UUID key,RechargeCommand command);
 List<Product> products(UUID customer); List<Payment> history(UUID customer);
 Payment get(UUID customer,UUID id); Payment cancel(UUID customer,UUID id); void execute(UUID id);
}

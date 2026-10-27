package br.com.spbank.pagamento.application.port.out;
import br.com.spbank.pagamento.application.model.PaymentModels.*;
import java.util.*;
import java.time.LocalDate;
public interface PaymentPersistence {
 Optional<Charge> charge(String hash); void save(Charge charge);
 List<Product> products(); Optional<Product> product(UUID id);
 List<Payment> payments(UUID customerId); Optional<UUID> owner(UUID id);
 List<UUID> due(LocalDate date,int limit); void save(Payment payment);
}

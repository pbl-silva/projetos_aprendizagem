package br.com.spbank.cartao.application.port.out;
import br.com.spbank.cartao.application.model.finance.CardFinanceState;
import java.util.*;
public interface CardFinancePersistence {
    CardFinanceState load(UUID customerId);
    void save(CardFinanceState state);
    List<UUID> customerIds();
}

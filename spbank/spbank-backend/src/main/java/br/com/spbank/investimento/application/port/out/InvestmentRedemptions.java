package br.com.spbank.investimento.application.port.out;
import br.com.spbank.investimento.application.model.InvestmentRedemption;
import java.util.*;
public interface InvestmentRedemptions {
 List<InvestmentRedemption> list(UUID customerId);
 List<UUID> customers();
 void save(InvestmentRedemption row);
}

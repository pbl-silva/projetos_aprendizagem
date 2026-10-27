package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentRedemptionData;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface InvestmentRedemptionJpaRepository extends JpaRepository<InvestmentRedemptionData,UUID> {
 List<InvestmentRedemptionData> findByCustomerId(UUID customerId);
}

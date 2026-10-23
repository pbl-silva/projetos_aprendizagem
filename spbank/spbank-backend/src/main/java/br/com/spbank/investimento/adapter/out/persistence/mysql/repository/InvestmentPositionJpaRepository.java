package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentPositionData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface InvestmentPositionJpaRepository extends JpaRepository<InvestmentPositionData,UUID> {
    @org.springframework.data.jpa.repository.Query("select distinct p.customerId from InvestmentPositionData p order by p.customerId")
    List<UUID> customersWithPositions();
    List<InvestmentPositionData> findByCustomerIdOrderByEffectiveDateDescId(UUID customerId);
}

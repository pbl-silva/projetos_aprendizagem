package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentOperationData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface InvestmentOperationJpaRepository extends JpaRepository<InvestmentOperationData,UUID> {
    Optional<InvestmentOperationData> findByCustomerIdAndOperationKey(UUID customerId,UUID operationKey);
}

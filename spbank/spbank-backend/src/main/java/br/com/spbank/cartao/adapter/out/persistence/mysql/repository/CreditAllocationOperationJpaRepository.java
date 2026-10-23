package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CreditAllocationOperationData;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAllocationOperationJpaRepository
        extends JpaRepository<CreditAllocationOperationData, UUID> {
    Optional<CreditAllocationOperationData> findByCustomerIdAndKey(UUID customerId, UUID key);
}

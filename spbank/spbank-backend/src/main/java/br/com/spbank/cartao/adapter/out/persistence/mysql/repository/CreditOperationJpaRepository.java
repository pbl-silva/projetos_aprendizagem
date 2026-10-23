
package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CreditOperationData;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditOperationJpaRepository
        extends JpaRepository<CreditOperationData, UUID> {

    Optional<CreditOperationData> findByCustomerIdAndIdempotencyKey(
            UUID customerId,
            UUID key
    );
}


package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CreditRequestData;
import br.com.spbank.cartao.application.model.CreditRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditRequestJpaRepository
        extends JpaRepository<CreditRequestData, UUID> {

    List<CreditRequestData> findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId
    );

    Optional<CreditRequestData> findByIdAndCustomerId(
            UUID id,
            UUID customerId
    );

    List<CreditRequestData> findByStatusOrderByCreatedAtAsc(
            CreditRequestStatus status
    );
}

package br.com.spbank.administracao.adapter.out.persistence.mysql.repository;

import br.com.spbank.administracao.adapter.out.persistence.mysql.data.CustomerPlanChangeData;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPlanChangeJpaRepository
        extends JpaRepository<CustomerPlanChangeData, UUID> {

    List<CustomerPlanChangeData>
            findByCustomerIdOrderByChangedAtDesc(
                    UUID customerId
            );
}
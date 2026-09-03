package br.com.spbank.administracao.adapter.out.persistence.mysql.repository;

import br.com.spbank.administracao.adapter.out.persistence.mysql.data.AccountPlanChangeData;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPlanChangeJpaRepository
        extends JpaRepository<AccountPlanChangeData, UUID> {

    List<AccountPlanChangeData>
            findByAccountIdOrderByChangedAtDesc(
                    UUID accountId
            );
}
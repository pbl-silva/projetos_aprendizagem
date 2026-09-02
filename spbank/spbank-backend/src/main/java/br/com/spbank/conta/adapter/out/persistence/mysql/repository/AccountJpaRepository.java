package br.com.spbank.conta.adapter.out.persistence.mysql.repository;

import br.com.spbank.conta.adapter.out.persistence.mysql.data.AccountData;
import br.com.spbank.conta.application.model.AccountType;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountJpaRepository
        extends JpaRepository<AccountData, UUID> {

    Optional<AccountData>
            findByBankCodeAndBranchAndAccountNumberAndAccountType(
                    String bankCode,
                    String branch,
                    String accountNumber,
                    AccountType accountType
            );

    List<AccountData>
            findByCustomerIdOrderByAccountType(
                    UUID customerId
            );

    boolean existsByCustomerIdAndAccountType(
            UUID customerId,
            AccountType accountType
    );

    boolean existsByBankCodeAndBranchAndAccountNumberAndAccountType(
            String bankCode,
            String branch,
            String accountNumber,
            AccountType accountType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM AccountData a
            WHERE a.id IN :ids
            ORDER BY a.id
            """)
    List<AccountData> findAllForUpdate(
            @Param("ids")
            Collection<UUID> ids
    );
}
package br.com.spbank.conta.adapter.out.persistence.mysql.repository;

import br.com.spbank.conta.adapter.out.persistence.mysql.data.AccountEntryData;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountEntryJpaRepository
        extends JpaRepository<AccountEntryData, UUID> {

    List<AccountEntryData>
            findByAccountIdOrderByOccurredAtDesc(
                    UUID accountId,
                    Pageable pageable
            );
}
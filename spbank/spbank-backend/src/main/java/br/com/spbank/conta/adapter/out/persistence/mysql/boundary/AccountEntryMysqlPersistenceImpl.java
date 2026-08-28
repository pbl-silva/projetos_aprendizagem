package br.com.spbank.conta.adapter.out.persistence.mysql.boundary;

import br.com.spbank.conta.adapter.out.persistence.mysql.mapper.AccountEntryPersistenceMapper;
import br.com.spbank.conta.adapter.out.persistence.mysql.repository.AccountEntryJpaRepository;
import br.com.spbank.conta.application.model.AccountEntry;
import br.com.spbank.conta.application.port.out.AccountEntryPersistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public final class AccountEntryMysqlPersistenceImpl
        implements AccountEntryPersistence {

    private final AccountEntryJpaRepository repository;

    public AccountEntryMysqlPersistenceImpl(
            AccountEntryJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void saveAll(
            Collection<AccountEntry> entries
    ) {

        repository.saveAll(
                entries.stream()
                        .map(AccountEntryPersistenceMapper::toData)
                        .toList()
        );
    }

    @Override
    public List<AccountEntry> findRecent(
            UUID accountId,
            int limit
    ) {

        return repository
                .findByAccountIdOrderByOccurredAtDesc(
                        accountId,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(AccountEntryPersistenceMapper::toDomain)
                .toList();
    }
}
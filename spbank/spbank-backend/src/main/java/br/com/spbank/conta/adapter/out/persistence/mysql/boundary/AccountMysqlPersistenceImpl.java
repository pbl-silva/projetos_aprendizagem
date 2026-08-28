package br.com.spbank.conta.adapter.out.persistence.mysql.boundary;

import br.com.spbank.conta.adapter.out.persistence.mysql.mapper.AccountPersistenceMapper;
import br.com.spbank.conta.adapter.out.persistence.mysql.repository.AccountJpaRepository;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.port.in.AccountLookup;
import br.com.spbank.conta.application.port.out.AccountPersistence;

import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public final class AccountMysqlPersistenceImpl
        implements AccountPersistence {

    private final AccountJpaRepository repository;
    private final EntityManager entityManager;

    public AccountMysqlPersistenceImpl(
            AccountJpaRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Account> findById(UUID id) {

        return repository
                .findById(id)
                .map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findTarget(
            AccountLookup key
    ) {

        return repository
                .findByBankCodeAndBranchAndAccountNumberAndAccountType(
                        key.bankCode(),
                        key.branch(),
                        key.accountNumber(),
                        key.accountType()
                )
                .map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Map<UUID, Account> findAllForUpdate(
            Collection<UUID> ids
    ) {

        entityManager.flush();
        entityManager.clear();

        return repository
                .findAllForUpdate(ids)
                .stream()
                .map(AccountPersistenceMapper::toDomain)
                .collect(
                        Collectors.toMap(
                                Account::getId,
                                account -> account
                        )
                );
    }

    @Override
    public void saveAll(
            Collection<Account> accounts
    ) {

        repository.saveAll(
                accounts.stream()
                        .map(AccountPersistenceMapper::toData)
                        .toList()
        );
    }
}
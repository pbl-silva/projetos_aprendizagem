package br.com.spbank.conta.adapter.out.persistence.mysql.boundary;

import br.com.spbank.conta.adapter.out.persistence.mysql.mapper.AccountPersistenceMapper;
import br.com.spbank.conta.adapter.out.persistence.mysql.repository.AccountJpaRepository;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.conta.application.port.in.AccountLookup;
import br.com.spbank.conta.application.port.out.AccountPersistence;

import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.List;
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
    public Optional<Account> findById(
            UUID id
    ) {
        return repository
                .findById(id)
                .map(data ->
                        AccountPersistenceMapper.toDomain(data)
                );
    }

    @Override
    public Optional<Account> findTarget(
            AccountLookup lookup
    ) {
        return repository
                .findByBankCodeAndBranchAndAccountNumberAndAccountType(
                        lookup.bankCode(),
                        lookup.branch(),
                        lookup.accountNumber(),
                        lookup.accountType()
                )
                .map(data ->
                        AccountPersistenceMapper.toDomain(data)
                );
    }

    @Override
    public List<Account> findByCustomerId(
            UUID customerId
    ) {
        return repository
                .findByCustomerId(customerId)
                .stream()
                .map(data ->
                        AccountPersistenceMapper.toDomain(data)
                )
                .toList();
    }

    @Override
    public boolean existsByCustomerIdAndType(
            UUID customerId,
            AccountType type
    ) {
        return repository
                .existsByCustomerIdAndAccountType(
                        customerId,
                        type
                );
    }

    @Override
    public boolean existsByBankData(
            String bankCode,
            String branch,
            String accountNumber,
            AccountType type
    ) {
        return repository
                .existsByBankCodeAndBranchAndAccountNumberAndAccountType(
                        bankCode,
                        branch,
                        accountNumber,
                        type
                );
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
                .map(data ->
                        AccountPersistenceMapper.toDomain(data)
                )
                .collect(
                        Collectors.toMap(
                                account -> account.getId(),
                                account -> account
                        )
                );
    }

    @Override
    public void saveAll(
            Collection<Account> accounts
    ) {
        repository.saveAll(
                accounts
                        .stream()
                        .map(account ->
                                AccountPersistenceMapper.toData(account)
                        )
                        .toList()
        );
    }
}
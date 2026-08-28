package br.com.spbank.conta.application.port.out;

import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.port.in.AccountLookup;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AccountPersistence {

    Optional<Account> findById(UUID id);

    Optional<Account> findTarget(AccountLookup lookup);

    Map<UUID, Account> findAllForUpdate(
            Collection<UUID> ids
    );

    void saveAll(
            Collection<Account> accounts
    );
}
package br.com.spbank.conta.application.port.out;

import br.com.spbank.conta.application.model.AccountEntry;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AccountEntryPersistence {

    void saveAll(Collection<AccountEntry> entries);

    List<AccountEntry> findRecent(
            UUID accountId,
            int limit
    );
}
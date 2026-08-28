package br.com.spbank.conta.application.port.in;

import br.com.spbank.conta.application.model.AccountEntry;

import java.util.List;
import java.util.UUID;

public interface ListAccountEntriesUseCase {

    List<AccountEntry> list(
            UUID id,
            int limit
    );
}
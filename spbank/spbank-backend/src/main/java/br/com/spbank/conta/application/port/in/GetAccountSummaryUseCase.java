package br.com.spbank.conta.application.port.in;

import java.util.UUID;

public interface GetAccountSummaryUseCase {

    AccountSummary get(UUID id);
}
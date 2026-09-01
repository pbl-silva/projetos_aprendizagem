package br.com.spbank.autenticacao.application.port.out;

import br.com.spbank.autenticacao.application.model.AccessCredential;
import br.com.spbank.autenticacao.application.model.AccessSession;
import br.com.spbank.autenticacao.application.model.Customer;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationPersistence {

    Optional<AccessCredential> findCredential(
            String normalizedUsername
    );

    Optional<AccessCredential> findCredentialByCustomerId(
            UUID customerId
    );

    boolean credentialExists(
            String normalizedUsername
    );

    Optional<Customer> findCustomerById(
            UUID customerId
    );

    Optional<Customer> findCustomerByCpf(
            String cpf
    );

    void saveCustomer(
            Customer customer
    );

    void saveCredential(
            AccessCredential credential
    );

    Optional<AccessSession> findSession(
            String tokenHash
    );

    void saveSession(
            AccessSession session
    );

    void revokeSession(
            String tokenHash,
            Instant revokedAt
    );

    void selectAccount(
            String tokenHash,
            UUID customerId,
            UUID accountId
    );
}
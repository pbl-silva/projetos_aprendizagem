package br.com.spbank.autenticacao.adapter.out.persistence.mysql;

import br.com.spbank.autenticacao.adapter.out.persistence.mysql.data.AccessCredentialData;
import br.com.spbank.autenticacao.adapter.out.persistence.mysql.data.AccessSessionData;
import br.com.spbank.autenticacao.adapter.out.persistence.mysql.data.CustomerData;
import br.com.spbank.autenticacao.adapter.out.persistence.mysql.repository.AccessCredentialJpaRepository;
import br.com.spbank.autenticacao.adapter.out.persistence.mysql.repository.AccessSessionJpaRepository;
import br.com.spbank.autenticacao.adapter.out.persistence.mysql.repository.CustomerJpaRepository;
import br.com.spbank.autenticacao.application.model.AccessCredential;
import br.com.spbank.autenticacao.application.model.AccessSession;
import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.model.CustomerAddress;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.shared.application.model.CustomerPlan;
import br.com.spbank.shared.application.port.out.CustomerPlanLookup;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public final class AuthenticationMysqlPersistence
        implements AuthenticationPersistence, CustomerPlanLookup {

    private final AccessCredentialJpaRepository credentials;
    private final AccessSessionJpaRepository sessions;
    private final CustomerJpaRepository customers;

    public AuthenticationMysqlPersistence(
            AccessCredentialJpaRepository credentials,
            AccessSessionJpaRepository sessions,
            CustomerJpaRepository customers
    ) {
        this.credentials = credentials;
        this.sessions = sessions;
        this.customers = customers;
    }

    @Override
    public Optional<AccessCredential> findCredential(
            String username
    ) {
        return credentials
                .findByUsernameIgnoreCase(username)
                .map(data ->
                        new AccessCredential(
                                data.getCustomerId(),
                                data.getUsername(),
                                data.getPasswordHash(),
                                data.isActive()
                        )
                );
    }

    @Override
    public Optional<AccessCredential> findCredentialByCustomerId(
            UUID customerId
    ) {
        return credentials
                .findByCustomerId(customerId)
                .map(data ->
                        new AccessCredential(
                                data.getCustomerId(),
                                data.getUsername(),
                                data.getPasswordHash(),
                                data.isActive()
                        )
                );
    }

    @Override
    public boolean credentialExists(
            String username
    ) {
        return credentials.existsByUsernameIgnoreCase(username);
    }

    @Override
    public Optional<Customer> findCustomerById(
            UUID customerId
    ) {
        return customers
                .findById(customerId)
                .map(AuthenticationMysqlPersistence::customer);
    }

    @Override
    public Optional<Customer> findCustomerByIdForUpdate(
            UUID customerId
    ) {
        return customers
                .findForUpdate(customerId)
                .map(AuthenticationMysqlPersistence::customer);
    }

    @Override
    public List<Customer> findAllCustomers() {
        return customers
                .findAll()
                .stream()
                .map(AuthenticationMysqlPersistence::customer)
                .toList();
    }

    @Override
    public Optional<Customer> findCustomerByCpf(
            String cpf
    ) {
        return customers
                .findByCpf(cpf)
                .map(AuthenticationMysqlPersistence::customer);
    }

    @Override
    public void saveCustomer(
            Customer customer
    ) {
        customers.save(data(customer));
    }

    @Override
    public CustomerPlan findPlan(
            UUID customerId
    ) {
        return findCustomerById(customerId)
                .map(Customer::plan)
                .orElseThrow(
                        () -> new NotFoundException("CUSTOMER_NOT_FOUND")
                );
    }

    @Override
    public CustomerPlan findPlanForUpdate(
            UUID customerId
    ) {
        return findCustomerByIdForUpdate(customerId)
                .map(Customer::plan)
                .orElseThrow(
                        () -> new NotFoundException("CUSTOMER_NOT_FOUND")
                );
    }

    @Override
    public void saveCredential(
            AccessCredential credential
    ) {
        credentials.save(
                new AccessCredentialData(
                        credential.customerId(),
                        credential.username(),
                        credential.passwordHash(),
                        credential.active()
                )
        );
    }

    @Override
    public Optional<AccessSession> findSession(
            String tokenHash
    ) {
        return sessions
                .findByTokenHash(tokenHash)
                .map(data ->
                        new AccessSession(
                                data.getId(),
                                data.getCustomerId(),
                                data.getAccountId(),
                                data.getTokenHash(),
                                data.getCreatedAt(),
                                data.getExpiresAt(),
                                data.getRevokedAt()
                        )
                );
    }

    @Override
    public void saveSession(
            AccessSession session
    ) {
        sessions.save(
                new AccessSessionData(
                        session.id(),
                        session.customerId(),
                        session.accountId(),
                        session.tokenHash(),
                        session.createdAt(),
                        session.expiresAt(),
                        session.revokedAt()
                )
        );
    }

    @Override
    public void revokeSession(
            String tokenHash,
            Instant revokedAt
    ) {
        sessions.revoke(tokenHash, revokedAt);
    }

    @Override
    public void selectAccount(
            String tokenHash,
            UUID customerId,
            UUID accountId
    ) {
        sessions.selectAccount(
                tokenHash,
                customerId,
                accountId
        );
    }

    private static Customer customer(
            CustomerData data
    ) {
        return new Customer(
                data.getId(),
                data.getFullName(),
                data.getCpf(),
                data.getBirthDate(),
                data.getMobile(),
                data.getEmail(),
                new CustomerAddress(
                        data.getPostalCode(),
                        data.getStreet(),
                        data.getAddressNumber(),
                        data.getComplement(),
                        data.getDistrict(),
                        data.getCity(),
                        data.getState()
                ),
                data.getPlan(),
                data.isActive()
        );
    }

    private static CustomerData data(
            Customer customer
    ) {
        CustomerAddress address = customer.address();

        return new CustomerData(
                customer.id(),
                customer.fullName(),
                customer.cpf(),
                customer.birthDate(),
                customer.mobile(),
                customer.email(),
                address.postalCode(),
                address.street(),
                address.number(),
                address.complement(),
                address.district(),
                address.city(),
                address.state(),
                customer.plan(),
                customer.active()
        );
    }
}
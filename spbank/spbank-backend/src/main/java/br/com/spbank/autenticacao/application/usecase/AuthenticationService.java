package br.com.spbank.autenticacao.application.usecase;

import br.com.spbank.autenticacao.application.model.AccessCredential;
import br.com.spbank.autenticacao.application.model.AccessSession;
import br.com.spbank.autenticacao.application.model.AuthenticatedSession;
import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.model.CustomerAccount;
import br.com.spbank.autenticacao.application.model.LoginResult;
import br.com.spbank.autenticacao.application.model.RegistrationResult;
import br.com.spbank.autenticacao.application.port.in.AuthenticationUseCase;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountPlan;
import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.UnauthorizedException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService
        implements AuthenticationUseCase {

    private static final String SPBANK_CODE = "90001";
    private static final String DEFAULT_BRANCH = "0001";

    private final AuthenticationPersistence authentication;
    private final AccountPersistence accounts;
    private final Clock clock;
    private final Duration sessionDuration;
    private final SecureRandom random =
            new SecureRandom();

    public AuthenticationService(
            AuthenticationPersistence authentication,
            AccountPersistence accounts,
            Clock clock,
            @Value("${spbank.auth.session-hours:8}")
            long sessionHours
    ) {
        this.authentication = authentication;
        this.accounts = accounts;
        this.clock = clock;
        this.sessionDuration =
                Duration.ofHours(sessionHours);
    }

    @Override
    @Transactional
    public LoginResult login(
            String username,
            String password
    ) {
        String normalized =
                Objects.requireNonNullElse(
                                username,
                                ""
                        )
                        .trim()
                        .toLowerCase(Locale.ROOT);

        AccessCredential credential =
                authentication
                        .findCredential(normalized)
                        .filter(found ->
                                found.active()
                        )
                        .filter(found ->
                                PasswordHasher.matches(
                                        password,
                                        found.passwordHash()
                                )
                        )
                        .orElseThrow(
                                UnauthorizedException::new
                        );

        Customer customer =
                authentication
                        .findCustomerById(
                                credential.customerId()
                        )
                        .filter(Customer::active)
                        .orElseThrow(
                                UnauthorizedException::new
                        );

        Account account =
                accounts
                        .findByCustomerId(
                                customer.id()
                        )
                        .stream()
                        .filter(Account::isActive)
                        .sorted(
                                Comparator.comparing(
                                        accountFound ->
                                                accountFound
                                                        .getAccountType()
                                                        == AccountType.CURRENT
                                                        ? 0
                                                        : 1
                                )
                        )
                        .findFirst()
                        .orElseThrow(
                                UnauthorizedException::new
                        );

        byte[] tokenBytes =
                new byte[32];

        random.nextBytes(
                tokenBytes
        );

        String token =
                Base64
                        .getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                tokenBytes
                        );

        Instant now =
                clock.instant();

        Instant expiresAt =
                now.plus(
                        sessionDuration
                );

        authentication.saveSession(
                new AccessSession(
                        UUID.randomUUID(),
                        customer.id(),
                        account.getId(),
                        PasswordHasher.tokenHash(
                                token
                        ),
                        now,
                        expiresAt,
                        null
                )
        );

        return new LoginResult(
                token,
                expiresAt,
                account.getId(),
                customer.fullName()
        );
    }

    @Override
    @Transactional
    public RegistrationResult register(
            String fullName,
            String cpf,
            String username,
            String password,
            AccountType accountType
    ) {
        String normalizedCpf =
                normalizeCpf(cpf);

        String normalizedUsername =
                normalizeUsername(username);

        if (authentication
                .findCustomerByCpf(
                        normalizedCpf
                )
                .isPresent()) {

            throw new BusinessException(
                    "CUSTOMER_ALREADY_REGISTERED",
                    "customer.already-registered"
            );
        }

        if (authentication
                .credentialExists(
                        normalizedUsername
                )) {

            throw new BusinessException(
                    "USERNAME_ALREADY_REGISTERED",
                    "auth.username.already-registered"
            );
        }

        Customer customer =
                new Customer(
                        UUID.randomUUID(),
                        fullName.trim(),
                        normalizedCpf,
                        true
                );

        Account account =
                createAccount(
                        customer,
                        accountType
                );

        authentication.saveCustomer(
                customer
        );

        accounts.saveAll(
                List.of(account)
        );

        authentication.saveCredential(
                new AccessCredential(
                        customer.id(),
                        normalizedUsername,
                        PasswordHasher.hash(
                                password
                        ),
                        true
                )
        );

        return new RegistrationResult(
                customer.id(),
                account.getId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedSession resolveSession(
            String accessToken
    ) {
        if (accessToken == null
                || accessToken.isBlank()) {

            throw new UnauthorizedException();
        }

        AccessSession session =
                authentication
                        .findSession(
                                PasswordHasher
                                        .tokenHash(
                                                accessToken
                                        )
                        )
                        .orElseThrow(
                                UnauthorizedException::new
                        );

        if (!session.isValidAt(
                clock.instant()
        )) {
            throw new UnauthorizedException();
        }

        return new AuthenticatedSession(
                session.customerId(),
                session.accountId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAccount> listAccounts(
            UUID customerId,
            UUID selectedAccountId
    ) {
        requireActiveCustomer(
                customerId
        );

        return accounts
                .findByCustomerId(
                        customerId
                )
                .stream()
                .filter(Account::isActive)
                .map(account ->
                        toCustomerAccount(
                                account,
                                account
                                        .getId()
                                        .equals(
                                                selectedAccountId
                                        )
                        )
                )
                .toList();
    }

    @Override
    @Transactional
    public CustomerAccount openAccount(
            UUID customerId,
            AccountType accountType
    ) {
        Customer customer =
                requireActiveCustomer(
                        customerId
                );

        if (accounts
                .existsByCustomerIdAndType(
                        customerId,
                        accountType
                )) {

            throw new BusinessException(
                    "ACCOUNT_TYPE_ALREADY_EXISTS",
                    "account.type.already-exists"
            );
        }

        Account account =
                createAccount(
                        customer,
                        accountType
                );

        accounts.saveAll(
                List.of(account)
        );

        return toCustomerAccount(
                account,
                false
        );
    }

    @Override
    @Transactional
    public void selectAccount(
            String accessToken,
            UUID customerId,
            UUID accountId
    ) {
        Account account =
                accounts
                        .findById(
                                accountId
                        )
                        .filter(Account::isActive)
                        .filter(found ->
                                found
                                        .getCustomerId()
                                        .equals(
                                                customerId
                                        )
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "ACCOUNT_ACCESS_DENIED",
                                        "account.access-denied"
                                )
                        );

        authentication.selectAccount(
                PasswordHasher.tokenHash(
                        accessToken
                ),
                customerId,
                account.getId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void confirmPassword(
            UUID accountId,
            String password
    ) {
        UUID customerId =
                accounts
                        .findById(accountId)
                        .map(
                                Account::getCustomerId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "ACCOUNT_UNAVAILABLE",
                                        "account.unavailable"
                                )
                        );

        boolean valid =
                authentication
                        .findCredentialByCustomerId(
                                customerId
                        )
                        .filter(found ->
                                found.active()
                        )
                        .filter(found ->
                                PasswordHasher.matches(
                                        password,
                                        found.passwordHash()
                                )
                        )
                        .isPresent();

        if (!valid) {
            throw new BusinessException(
                    "INVALID_PASSWORD_CONFIRMATION",
                    "auth.confirmation-invalid"
            );
        }
    }

    @Override
    @Transactional
    public void logout(
            String accessToken
    ) {
        if (accessToken != null
                && !accessToken.isBlank()) {

            authentication.revokeSession(
                    PasswordHasher.tokenHash(
                            accessToken
                    ),
                    clock.instant()
            );
        }
    }

    private Customer requireActiveCustomer(
            UUID customerId
    ) {
        return authentication
                .findCustomerById(
                        customerId
                )
                .filter(Customer::active)
                .orElseThrow(
                        UnauthorizedException::new
                );
    }

    private Account createAccount(
            Customer customer,
            AccountType accountType
    ) {
        Objects.requireNonNull(
                accountType,
                "accountType"
        );

        return new Account(
                UUID.randomUUID(),
                customer.id(),
                customer.fullName(),
                customer.cpf(),
                SPBANK_CODE,
                DEFAULT_BRANCH,
                nextAccountNumber(
                        accountType
                ),
                accountType,
                AccountPlan.STANDARD,
                new java.math.BigDecimal(
                        "0.00"
                ),
                true
        );
    }

    private String nextAccountNumber(
            AccountType accountType
    ) {
        for (
                int attempt = 0;
                attempt < 100;
                attempt++
        ) {
            String number =
                    Integer.toString(
                            100_000
                                    + random.nextInt(
                                            900_000
                                    )
                    );

            if (!accounts.existsByBankData(
                    SPBANK_CODE,
                    DEFAULT_BRANCH,
                    number,
                    accountType
            )) {
                return number;
            }
        }

        throw new IllegalStateException(
                "Não foi possível gerar um número de conta único"
        );
    }

    private static CustomerAccount toCustomerAccount(
            Account account,
            boolean selected
    ) {
        return new CustomerAccount(
                account.getId(),
                account.getAccountType(),
                account.getBranch(),
                account.getAccountNumber(),
                account.getBalance(),
                selected
        );
    }

    private static String normalizeCpf(
            String cpf
    ) {
        return Objects
                .requireNonNullElse(
                        cpf,
                        ""
                )
                .replaceAll(
                        "\\D",
                        ""
                );
    }

    private static String normalizeUsername(
            String username
    ) {
        return Objects
                .requireNonNullElse(
                        username,
                        ""
                )
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}
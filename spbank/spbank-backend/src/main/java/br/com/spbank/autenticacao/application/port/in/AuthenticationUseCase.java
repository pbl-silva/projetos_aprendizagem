package br.com.spbank.autenticacao.application.port.in;

import br.com.spbank.autenticacao.application.model.AuthenticatedSession;
import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.model.CustomerAccount;
import br.com.spbank.autenticacao.application.model.LoginResult;
import br.com.spbank.autenticacao.application.model.RegistrationResult;
import br.com.spbank.conta.application.model.AccountType;

import java.util.List;
import java.util.UUID;

public interface AuthenticationUseCase {

    LoginResult login(
            String username,
            String password
    );

    RegistrationResult register(
            CustomerRegistrationCommand command
    );

    Customer profile(
            UUID customerId
    );

    Customer updateProfile(
            UUID customerId,
            CustomerProfileUpdateCommand command
    );

    void changePassword(
            UUID customerId,
            ChangePasswordCommand command
    );

    AuthenticatedSession resolveSession(
            String accessToken
    );

    List<CustomerAccount> listAccounts(
            UUID customerId,
            UUID selectedAccountId
    );

    CustomerAccount openAccount(
            UUID customerId,
            AccountType accountType
    );

    void selectAccount(
            String accessToken,
            UUID customerId,
            UUID accountId
    );

    void confirmPassword(
            UUID accountId,
            String password
    );

    void logout(
            String accessToken
    );
}
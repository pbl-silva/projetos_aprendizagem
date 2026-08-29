package br.com.spbank.autenticacao.application.port.in;

import br.com.spbank.autenticacao.application.model.LoginResult;

import java.util.UUID;

public interface AuthenticationUseCase {

    LoginResult login(
            String username,
            String password
    );

    UUID resolveAccount(
            String accessToken
    );

    void confirmPassword(
            UUID accountId,
            String password
    );

    void logout(
            String accessToken
    );
}
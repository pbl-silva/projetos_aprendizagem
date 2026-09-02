package br.com.spbank.autenticacao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ACCESS_TOKEN;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ACCOUNT_ID;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;

import br.com.spbank.autenticacao.adapter.in.api.rest.dto.ChangePasswordRequestDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.CurrentUserDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.CustomerAccountDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.CustomerAddressDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.CustomerProfileDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.CustomerProfileUpdateDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.LoginRequestDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.LoginResponseDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.OpenAccountRequestDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.RegistrationRequestDto;
import br.com.spbank.autenticacao.adapter.in.api.rest.dto.RegistrationResponseDto;
import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.model.LoginResult;
import br.com.spbank.autenticacao.application.port.in.AuthenticationUseCase;
import br.com.spbank.autenticacao.application.port.in.ChangePasswordCommand;
import br.com.spbank.autenticacao.application.port.in.CustomerProfileUpdateCommand;
import br.com.spbank.autenticacao.application.port.in.CustomerRegistrationCommand;
import br.com.spbank.conta.application.port.in.GetAccountSummaryUseCase;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public final class AuthController {

    private final AuthenticationUseCase authentication;
    private final GetAccountSummaryUseCase accounts;

    public AuthController(
            AuthenticationUseCase authentication,
            GetAccountSummaryUseCase accounts
    ) {
        this.authentication = authentication;
        this.accounts = accounts;
    }

    @PostMapping("/login")
    public LoginResponseDto login(
            @Valid
            @RequestBody
            LoginRequestDto request
    ) {
        LoginResult result =
                authentication.login(
                        request.username(),
                        request.password()
                );

        return new LoginResponseDto(
                result.accessToken(),
                result.expiresAt(),
                result.accountId(),
                result.holderName()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> register(
            @Valid
            @RequestBody
            RegistrationRequestDto request
    ) {
        var result =
                authentication.register(
                        new CustomerRegistrationCommand(
                                request.fullName(),
                                request.cpf(),
                                request.birthDate(),
                                request.mobile(),
                                request.email(),
                                request.address().toModel(),
                                request.username(),
                                request.password(),
                                request.accountType()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new RegistrationResponseDto(
                                result.customerId(),
                                result.accountId()
                        )
                );
    }

    @GetMapping("/profile")
    public CustomerProfileDto profile(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId
    ) {
        return profileDto(
                authentication.profile(
                        customerId
                )
        );
    }

    @PutMapping("/profile")
    public CustomerProfileDto updateProfile(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @Valid
            @RequestBody
            CustomerProfileUpdateDto request
    ) {
        Customer updated =
                authentication.updateProfile(
                        customerId,
                        new CustomerProfileUpdateCommand(
                                request.mobile(),
                                request.email(),
                                request.address().toModel()
                        )
                );

        return profileDto(updated);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @Valid
            @RequestBody
            ChangePasswordRequestDto request
    ) {
        authentication.changePassword(
                customerId,
                new ChangePasswordCommand(
                        request.currentPassword(),
                        request.newPassword(),
                        request.newPasswordConfirmation()
                )
        );
    }

    @GetMapping("/me")
    public CurrentUserDto me(
            @RequestAttribute(ACCOUNT_ID)
            UUID accountId
    ) {
        var account =
                accounts.get(accountId);

        return new CurrentUserDto(
                account.id(),
                account.holderName()
        );
    }

    @GetMapping("/accounts")
    public List<CustomerAccountDto> accounts(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @RequestAttribute(ACCOUNT_ID)
            UUID accountId
    ) {
        return authentication
                .listAccounts(
                        customerId,
                        accountId
                )
                .stream()
                .map(account ->
                        new CustomerAccountDto(
                                account.id(),
                                account.accountType(),
                                account.branch(),
                                account.accountNumber(),
                                account.balance(),
                                account.selected()
                        )
                )
                .toList();
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAccountDto openAccount(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @Valid
            @RequestBody
            OpenAccountRequestDto request
    ) {
        var account =
                authentication.openAccount(
                        customerId,
                        request.accountType()
                );

        return new CustomerAccountDto(
                account.id(),
                account.accountType(),
                account.branch(),
                account.accountNumber(),
                account.balance(),
                false
        );
    }

    @PostMapping("/accounts/{accountId}/select")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectAccount(
            @RequestAttribute(ACCESS_TOKEN)
            String accessToken,

            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @PathVariable
            UUID accountId
    ) {
        authentication.selectAccount(
                accessToken,
                customerId,
                accountId
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestAttribute(ACCESS_TOKEN)
            String accessToken
    ) {
        authentication.logout(
                accessToken
        );
    }

    private static CustomerProfileDto profileDto(
            Customer customer
    ) {
        return new CustomerProfileDto(
                customer.id(),
                customer.fullName(),
                customer.cpf(),
                customer.birthDate(),
                customer.mobile(),
                customer.email(),
                CustomerAddressDto.from(
                        customer.address()
                )
        );
    }
}
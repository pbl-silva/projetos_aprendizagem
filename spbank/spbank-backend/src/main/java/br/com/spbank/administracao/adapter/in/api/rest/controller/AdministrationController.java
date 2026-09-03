package br.com.spbank.administracao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMIN_ACCESS_TOKEN;

import br.com.spbank.administracao.adapter.in.api.rest.dto.AccountPlanChangeDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.AccountPlanUpdateDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministrativeAccountDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministratorLoginRequestDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministratorLoginResponseDto;
import br.com.spbank.administracao.application.port.in.AdministrationUseCase;
import br.com.spbank.administracao.application.port.in.ChangeAccountPlanCommand;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/admin")
public final class AdministrationController {

    private final AdministrationUseCase administration;

    public AdministrationController(
            AdministrationUseCase administration
    ) {
        this.administration = administration;
    }

    @PostMapping("/auth/login")
    public AdministratorLoginResponseDto login(
            @Valid
            @RequestBody
            AdministratorLoginRequestDto request
    ) {

        var result =
                administration.login(
                        request.username(),
                        request.password()
                );

        return new AdministratorLoginResponseDto(
                result.accessToken(),
                result.expiresAt(),
                result.administratorId(),
                result.displayName()
        );
    }

    @GetMapping("/accounts")
    public List<AdministrativeAccountDto> accounts(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId
    ) {

        return administration
                .listAccounts(
                        administratorId
                )
                .stream()
                .map(account ->
                        new AdministrativeAccountDto(
                                account.accountId(),
                                account.holderName(),
                                account.maskedDocument(),
                                account.branch(),
                                account.accountNumber(),
                                account.accountType(),
                                account.accountPlan(),
                                account.active()
                        )
                )
                .toList();
    }

    @PutMapping("/accounts/{accountId}/plan")
    public AccountPlanChangeDto changePlan(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId,

            @PathVariable
            UUID accountId,

            @Valid
            @RequestBody
            AccountPlanUpdateDto request
    ) {

        return AccountPlanChangeDto.from(
                administration.changeAccountPlan(
                        administratorId,
                        accountId,
                        new ChangeAccountPlanCommand(
                                request.accountPlan(),
                                request.reason()
                        )
                )
        );
    }

    @GetMapping(
            "/accounts/{accountId}/plan-history"
    )
    public List<AccountPlanChangeDto> history(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId,

            @PathVariable
            UUID accountId
    ) {

        return administration
                .listAccountPlanHistory(
                        administratorId,
                        accountId
                )
                .stream()
                .map(
                        AccountPlanChangeDto::from
                )
                .toList();
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestAttribute(ADMIN_ACCESS_TOKEN)
            String accessToken
    ) {

        administration.logout(
                accessToken
        );
    }
}
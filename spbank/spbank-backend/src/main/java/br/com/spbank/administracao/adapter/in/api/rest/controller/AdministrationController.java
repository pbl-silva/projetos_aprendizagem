package br.com.spbank.administracao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMIN_ACCESS_TOKEN;

import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministrativeCustomerDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministratorLoginRequestDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministratorLoginResponseDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.CustomerPlanChangeDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.CustomerPlanUpdateDto;
import br.com.spbank.administracao.application.port.in.AdministrationUseCase;
import br.com.spbank.administracao.application.port.in.ChangeCustomerPlanCommand;

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

        var result = administration.login(
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

    @GetMapping("/customers")
    public List<AdministrativeCustomerDto> customers(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId
    ) {

        return administration
                .listCustomers(administratorId)
                .stream()
                .map(AdministrativeCustomerDto::from)
                .toList();
    }

    @PutMapping("/customers/{customerId}/plan")
    public CustomerPlanChangeDto changePlan(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId,

            @PathVariable
            UUID customerId,

            @Valid
            @RequestBody
            CustomerPlanUpdateDto request
    ) {

        return CustomerPlanChangeDto.from(
                administration.changeCustomerPlan(
                        administratorId,
                        customerId,
                        new ChangeCustomerPlanCommand(
                                request.customerPlan(),
                                request.reason()
                        )
                )
        );
    }

    @GetMapping("/customers/{customerId}/plan-history")
    public List<CustomerPlanChangeDto> history(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId,

            @PathVariable
            UUID customerId
    ) {

        return administration
                .listCustomerPlanHistory(
                        administratorId,
                        customerId
                )
                .stream()
                .map(CustomerPlanChangeDto::from)
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
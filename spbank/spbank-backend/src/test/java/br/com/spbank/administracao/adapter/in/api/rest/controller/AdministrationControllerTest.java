package br.com.spbank.administracao.adapter.in.api.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.spbank.administracao.adapter.in.api.rest.dto.AccountPlanUpdateDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministratorLoginRequestDto;
import br.com.spbank.administracao.application.model.AccountPlanChange;
import br.com.spbank.administracao.application.model.AdministratorLoginResult;
import br.com.spbank.administracao.application.port.in.AdministrationUseCase;
import br.com.spbank.administracao.application.port.in.ChangeAccountPlanCommand;
import br.com.spbank.conta.application.model.AccountPlan;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdministrationControllerTest {

    private static final UUID ADMIN_ID =
            UUID.fromString(
                    "cccccccc-cccc-cccc-cccc-cccccccccccc"
            );

    private static final UUID ACCOUNT_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
            );

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-02T15:00:00Z"
            );

    @Test
    void shouldMapAdministrativeLoginWithoutChangingTheToken() {

        AdministrationUseCase administration =
                mock(AdministrationUseCase.class);

        when(
                administration.login(
                        "gerente",
                        "SPBankAdmin@123"
                )
        )
                .thenReturn(
                        new AdministratorLoginResult(
                                "admin-token",
                                NOW.plusSeconds(3600),
                                ADMIN_ID,
                                "Gerente SPBank"
                        )
                );

        var response =
                new AdministrationController(
                        administration
                )
                        .login(
                                new AdministratorLoginRequestDto(
                                        "gerente",
                                        "SPBankAdmin@123"
                                )
                        );

        assertThat(
                response.accessToken()
        )
                .isEqualTo(
                        "admin-token"
                );

        assertThat(
                response.administratorId()
        )
                .isEqualTo(
                        ADMIN_ID
                );

        assertThat(
                response.displayName()
        )
                .isEqualTo(
                        "Gerente SPBank"
                );
    }

    @Test
    void shouldMapPlanUpdateToTheAdministrativeCommand() {

        AdministrationUseCase administration =
                mock(AdministrationUseCase.class);

        AccountPlanChange change =
                new AccountPlanChange(
                        UUID.randomUUID(),
                        ACCOUNT_ID,
                        ADMIN_ID,
                        "Gerente SPBank",
                        AccountPlan.STANDARD,
                        AccountPlan.PLUS,
                        "Critérios internos aprovados",
                        NOW
                );

        when(
                administration.changeAccountPlan(
                        eq(ADMIN_ID),
                        eq(ACCOUNT_ID),
                        any()
                )
        )
                .thenReturn(change);

        var response =
                new AdministrationController(
                        administration
                )
                        .changePlan(
                                ADMIN_ID,
                                ACCOUNT_ID,
                                new AccountPlanUpdateDto(
                                        AccountPlan.PLUS,
                                        "Critérios internos aprovados"
                                )
                        );

        ArgumentCaptor<ChangeAccountPlanCommand> command =
                ArgumentCaptor.forClass(
                        ChangeAccountPlanCommand.class
                );

        verify(administration)
                .changeAccountPlan(
                        eq(ADMIN_ID),
                        eq(ACCOUNT_ID),
                        command.capture()
                );

        assertThat(
                command.getValue()
                        .accountPlan()
        )
                .isEqualTo(
                        AccountPlan.PLUS
                );

        assertThat(
                command.getValue()
                        .reason()
        )
                .isEqualTo(
                        "Critérios internos aprovados"
                );

        assertThat(
                response.previousPlan()
        )
                .isEqualTo(
                        AccountPlan.STANDARD
                );

        assertThat(
                response.newPlan()
        )
                .isEqualTo(
                        AccountPlan.PLUS
                );
    }
}
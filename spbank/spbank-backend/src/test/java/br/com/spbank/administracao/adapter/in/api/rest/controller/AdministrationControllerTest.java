package br.com.spbank.administracao.adapter.in.api.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.spbank.administracao.adapter.in.api.rest.dto.AdministratorLoginRequestDto;
import br.com.spbank.administracao.adapter.in.api.rest.dto.CustomerPlanUpdateDto;
import br.com.spbank.administracao.application.model.AdministratorLoginResult;
import br.com.spbank.administracao.application.model.CustomerPlanChange;
import br.com.spbank.administracao.application.port.in.AdministrationUseCase;
import br.com.spbank.administracao.application.port.in.ChangeCustomerPlanCommand;
import br.com.spbank.shared.application.model.CustomerPlan;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdministrationControllerTest {

    private static final UUID ADMIN_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID CUSTOMER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final Instant NOW =
            Instant.parse("2026-09-02T15:00:00Z");

    @Test
    void shouldMapAdministrativeLoginWithoutChangingTheToken() {
        AdministrationUseCase administration =
                mock(AdministrationUseCase.class);

        when(administration.login("gerente", "SPBankAdmin@123"))
                .thenReturn(new AdministratorLoginResult(
                        "admin-token",
                        NOW.plusSeconds(3600),
                        ADMIN_ID,
                        "Gerente SPBank"
                ));

        var controller = new AdministrationController(administration);

        var response = controller.login(
                new AdministratorLoginRequestDto(
                        "gerente",
                        "SPBankAdmin@123"
                )
        );

        assertThat(response.accessToken()).isEqualTo("admin-token");
        assertThat(response.administratorId()).isEqualTo(ADMIN_ID);
        assertThat(response.displayName()).isEqualTo("Gerente SPBank");
    }

    @Test
    void shouldMapPlanUpdateToTheAdministrativeCommand() {
        AdministrationUseCase administration =
                mock(AdministrationUseCase.class);

        CustomerPlanChange change = new CustomerPlanChange(
                UUID.randomUUID(),
                CUSTOMER_ID,
                ADMIN_ID,
                "Gerente SPBank",
                CustomerPlan.STANDARD,
                CustomerPlan.PLUS,
                "Critérios internos aprovados",
                NOW
        );

        when(administration.changeCustomerPlan(
                eq(ADMIN_ID),
                eq(CUSTOMER_ID),
                any()
        )).thenReturn(change);

        var controller = new AdministrationController(administration);

        var response = controller.changePlan(
                ADMIN_ID,
                CUSTOMER_ID,
                new CustomerPlanUpdateDto(
                        CustomerPlan.PLUS,
                        "Critérios internos aprovados"
                )
        );

        ArgumentCaptor<ChangeCustomerPlanCommand> command =
                ArgumentCaptor.forClass(ChangeCustomerPlanCommand.class);

        verify(administration).changeCustomerPlan(
                eq(ADMIN_ID),
                eq(CUSTOMER_ID),
                command.capture()
        );

        assertThat(command.getValue().customerPlan())
                .isEqualTo(CustomerPlan.PLUS);

        assertThat(command.getValue().reason())
                .isEqualTo("Critérios internos aprovados");

        assertThat(response.previousPlan())
                .isEqualTo(CustomerPlan.STANDARD);

        assertThat(response.newPlan())
                .isEqualTo(CustomerPlan.PLUS);
    }
}
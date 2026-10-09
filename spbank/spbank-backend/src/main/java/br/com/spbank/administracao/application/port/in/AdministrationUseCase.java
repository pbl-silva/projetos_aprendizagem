package br.com.spbank.administracao.application.port.in;

import br.com.spbank.administracao.application.model.AdministrativeCustomer;
import br.com.spbank.administracao.application.model.AdministratorLoginResult;
import br.com.spbank.administracao.application.model.AuthenticatedAdministrator;
import br.com.spbank.administracao.application.model.CustomerPlanChange;

import java.util.List;
import java.util.UUID;

public interface AdministrationUseCase {

    AdministratorLoginResult login(
            String username,
            String password
    );

    AuthenticatedAdministrator resolveSession(
            String accessToken
    );

    List<AdministrativeCustomer> listCustomers(
            UUID administratorId
    );

    CustomerPlanChange changeCustomerPlan(
            UUID administratorId,
            UUID customerId,
            ChangeCustomerPlanCommand command
    );

    List<CustomerPlanChange> listCustomerPlanHistory(
            UUID administratorId,
            UUID customerId
    );

    void logout(
            String accessToken
    );
}
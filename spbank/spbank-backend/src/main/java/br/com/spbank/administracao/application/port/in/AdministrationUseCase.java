package br.com.spbank.administracao.application.port.in;

import br.com.spbank.administracao.application.model.AccountPlanChange;
import br.com.spbank.administracao.application.model.AdministrativeAccount;
import br.com.spbank.administracao.application.model.AdministratorLoginResult;
import br.com.spbank.administracao.application.model.AuthenticatedAdministrator;

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

    List<AdministrativeAccount> listAccounts(
            UUID administratorId
    );

    AccountPlanChange changeAccountPlan(
            UUID administratorId,
            UUID accountId,
            ChangeAccountPlanCommand command
    );

    List<AccountPlanChange> listAccountPlanHistory(
            UUID administratorId,
            UUID accountId
    );

    void logout(
            String accessToken
    );
}
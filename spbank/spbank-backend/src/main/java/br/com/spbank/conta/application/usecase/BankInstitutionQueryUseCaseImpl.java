package br.com.spbank.conta.application.usecase;

import br.com.spbank.conta.application.model.BankInstitution;
import br.com.spbank.conta.application.port.in.ListBankInstitutionsUseCase;
import br.com.spbank.conta.application.port.out.BankInstitutionCatalog;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankInstitutionQueryUseCaseImpl
        implements ListBankInstitutionsUseCase {

    private final BankInstitutionCatalog catalog;

    public BankInstitutionQueryUseCaseImpl(
            BankInstitutionCatalog catalog
    ) {
        this.catalog = catalog;
    }

    @Override
    public List<BankInstitution> listActive() {

        return catalog.findAllActive();
    }
}
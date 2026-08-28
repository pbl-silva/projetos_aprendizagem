package br.com.spbank.conta.application.port.out;

import br.com.spbank.conta.application.model.BankInstitution;

import java.util.List;
import java.util.Optional;

public interface BankInstitutionCatalog {

    Optional<BankInstitution> findActiveByCode(
            String studyCode
    );

    List<BankInstitution> findAllActive();
}
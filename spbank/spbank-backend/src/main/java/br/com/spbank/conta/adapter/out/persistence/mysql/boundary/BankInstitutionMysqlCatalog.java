
package br.com.spbank.conta.adapter.out.persistence.mysql.boundary;

import br.com.spbank.conta.adapter.out.persistence.mysql.data.BankInstitutionData;
import br.com.spbank.conta.adapter.out.persistence.mysql.repository.BankInstitutionJpaRepository;
import br.com.spbank.conta.application.model.BankInstitution;
import br.com.spbank.conta.application.port.out.BankInstitutionCatalog;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public final class BankInstitutionMysqlCatalog
        implements BankInstitutionCatalog {

    private final BankInstitutionJpaRepository repository;

    public BankInstitutionMysqlCatalog(
            BankInstitutionJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<BankInstitution> findActiveByCode(
            String studyCode
    ) {
        return repository
                .findByStudyCodeAndActiveTrue(studyCode)
                .map(data -> toDomain(data));
    }

    @Override
    public List<BankInstitution> findAllActive() {
        return repository
                .findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(data -> toDomain(data))
                .toList();
    }

    private BankInstitution toDomain(
            BankInstitutionData data
    ) {
        return new BankInstitution(
                data.getStudyCode(),
                data.getName(),
                data.isInternal(),
                data.isActive()
        );
    }
}

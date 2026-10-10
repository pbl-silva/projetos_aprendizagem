
package br.com.spbank.conta.adapter.out.persistence.mysql.repository;

import br.com.spbank.conta.adapter.out.persistence.mysql.data.BankInstitutionData;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankInstitutionJpaRepository
        extends JpaRepository<BankInstitutionData, String> {

    Optional<BankInstitutionData> findByStudyCodeAndActiveTrue(
            String studyCode
    );

    List<BankInstitutionData> findAllByActiveTrueOrderByNameAsc();
}

package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardAgreementInstallmentData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardAgreementInstallmentJpaRepository extends JpaRepository<CardAgreementInstallmentData,UUID> {
    List<CardAgreementInstallmentData> findByCustomerId(UUID customerId);
}

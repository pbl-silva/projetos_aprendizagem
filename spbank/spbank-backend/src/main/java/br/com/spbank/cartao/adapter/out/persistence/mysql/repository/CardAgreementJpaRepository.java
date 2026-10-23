package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardAgreementData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardAgreementJpaRepository extends JpaRepository<CardAgreementData,UUID> {
    List<CardAgreementData> findByCustomerId(UUID customerId);
}

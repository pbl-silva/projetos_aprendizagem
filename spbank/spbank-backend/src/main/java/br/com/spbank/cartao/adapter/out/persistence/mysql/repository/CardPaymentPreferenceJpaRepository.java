package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardPaymentPreferenceData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardPaymentPreferenceJpaRepository extends JpaRepository<CardPaymentPreferenceData,UUID> {
    List<CardPaymentPreferenceData> findByCustomerId(UUID customerId);
}

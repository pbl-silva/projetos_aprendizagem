package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardPaymentData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardPaymentJpaRepository extends JpaRepository<CardPaymentData,UUID> {
    List<CardPaymentData> findByCustomerId(UUID customerId);
}

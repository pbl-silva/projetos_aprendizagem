package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardPurchaseData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardPurchaseJpaRepository extends JpaRepository<CardPurchaseData,UUID> {
    List<CardPurchaseData> findByCustomerId(UUID customerId);
}

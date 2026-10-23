package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardSettlementData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardSettlementJpaRepository extends JpaRepository<CardSettlementData,UUID> {
    List<CardSettlementData> findByCustomerId(UUID customerId);
}

package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardObligationData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardObligationJpaRepository extends JpaRepository<CardObligationData,UUID> {
    List<CardObligationData> findByCustomerId(UUID customerId);
}

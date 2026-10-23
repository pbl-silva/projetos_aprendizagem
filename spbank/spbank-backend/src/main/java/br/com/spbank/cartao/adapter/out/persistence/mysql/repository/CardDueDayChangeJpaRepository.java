package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardDueDayChangeData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardDueDayChangeJpaRepository extends JpaRepository<CardDueDayChangeData,UUID> {
    List<CardDueDayChangeData> findByCustomerId(UUID customerId);
}

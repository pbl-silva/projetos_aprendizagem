package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardAttemptData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardAttemptJpaRepository extends JpaRepository<CardAttemptData,UUID> {
    List<CardAttemptData> findByCustomerId(UUID customerId);
}

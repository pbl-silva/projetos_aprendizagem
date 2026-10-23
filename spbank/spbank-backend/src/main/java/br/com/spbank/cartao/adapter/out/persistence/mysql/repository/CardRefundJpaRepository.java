package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardRefundData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardRefundJpaRepository extends JpaRepository<CardRefundData,UUID> {
    List<CardRefundData> findByCustomerId(UUID customerId);
}

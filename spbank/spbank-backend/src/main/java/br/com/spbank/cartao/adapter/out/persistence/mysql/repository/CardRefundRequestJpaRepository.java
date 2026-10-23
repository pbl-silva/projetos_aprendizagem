package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardRefundRequestData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardRefundRequestJpaRepository extends JpaRepository<CardRefundRequestData,UUID> {
    List<CardRefundRequestData> findByCustomerId(UUID customerId);
}

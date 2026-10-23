package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardFinancialOperationData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardFinancialOperationJpaRepository extends JpaRepository<CardFinancialOperationData,UUID> {
    List<CardFinancialOperationData> findByCustomerId(UUID customerId);
}

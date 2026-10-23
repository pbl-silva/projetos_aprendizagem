package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardLateAllocationData;
public interface CardLateAllocationJpaRepository extends JpaRepository<CardLateAllocationData,UUID>{List<CardLateAllocationData> findByCustomerId(UUID customerId);}

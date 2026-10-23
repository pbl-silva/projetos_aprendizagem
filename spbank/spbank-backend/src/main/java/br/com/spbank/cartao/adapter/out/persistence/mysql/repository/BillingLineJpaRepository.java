package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.BillingLineData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BillingLineJpaRepository extends JpaRepository<BillingLineData,UUID> {
    List<BillingLineData> findByCustomerId(UUID customerId);
}

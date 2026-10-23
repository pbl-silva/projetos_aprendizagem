package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardInvoiceItemData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardInvoiceItemJpaRepository extends JpaRepository<CardInvoiceItemData,UUID> {
    List<CardInvoiceItemData> findByCustomerId(UUID customerId);
}

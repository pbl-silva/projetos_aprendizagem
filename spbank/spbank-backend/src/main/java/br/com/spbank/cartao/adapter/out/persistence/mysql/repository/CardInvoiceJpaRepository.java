package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardInvoiceData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface CardInvoiceJpaRepository extends JpaRepository<CardInvoiceData,UUID> {
    List<CardInvoiceData> findByCustomerId(UUID customerId);
    @Query("select distinct c.customerId from CardInstrumentData c") List<UUID> findDistinctCustomerIds();
}

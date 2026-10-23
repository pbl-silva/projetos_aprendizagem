package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardCreditLineData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardCreditLineId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCreditLineJpaRepository extends JpaRepository<CardCreditLineData, CardCreditLineId> {
    List<CardCreditLineData> findByCustomerId(UUID customerId);
}

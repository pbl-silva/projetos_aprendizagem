package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardProposalData;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CardProposalJpaRepository extends JpaRepository<CardProposalData,UUID> {
    List<CardProposalData> findByCustomerId(UUID customerId);
}

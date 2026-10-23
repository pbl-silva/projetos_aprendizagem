package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardProposalInvoiceData;
public interface CardProposalInvoiceJpaRepository extends JpaRepository<CardProposalInvoiceData,UUID>{List<CardProposalInvoiceData> findByCustomerId(UUID customerId);}

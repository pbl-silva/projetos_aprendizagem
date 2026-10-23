package br.com.spbank.cartao.adapter.out.persistence.mysql.repository;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardBlockAuditData;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBlockAuditJpaRepository extends JpaRepository<CardBlockAuditData, UUID> {
}

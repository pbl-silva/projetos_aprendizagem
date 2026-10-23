package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentValuationData;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InvestmentValuationJpaRepository extends JpaRepository<InvestmentValuationData,UUID> {}

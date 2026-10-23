package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentQuoteData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;
public interface InvestmentQuoteJpaRepository extends JpaRepository<InvestmentQuoteData,UUID> {
    Optional<InvestmentQuoteData> findFirstByProductIdAndQuoteDateLessThanEqualOrderByQuoteDateDesc(UUID productId,LocalDate through);
}

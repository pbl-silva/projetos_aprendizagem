package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentProductData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface InvestmentProductJpaRepository extends JpaRepository<InvestmentProductData,UUID> {
    List<InvestmentProductData> findByActiveTrueOrderByName();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from InvestmentProductData p where p.id=:id") Optional<InvestmentProductData> locked(@Param("id") UUID id);
}

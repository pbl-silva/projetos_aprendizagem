package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentApplicationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;
public interface InvestmentApplicationJpaRepository extends JpaRepository<InvestmentApplicationData,UUID> {
    Optional<InvestmentApplicationData> findByCustomerIdAndId(UUID customerId,UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from InvestmentApplicationData a where a.customerId=:customerId and a.id=:id") Optional<InvestmentApplicationData> locked(@Param("customerId") UUID customerId,@Param("id") UUID id);
    @Query("select a.id from InvestmentApplicationData a where a.status='PENDING' and a.effectiveDate<=:through order by a.requestedAt,a.id") List<UUID> due(@Param("through") LocalDate through,org.springframework.data.domain.Pageable page);
}

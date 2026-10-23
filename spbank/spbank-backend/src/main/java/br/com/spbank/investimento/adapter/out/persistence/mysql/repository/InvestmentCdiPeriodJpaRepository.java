package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentCdiPeriodData;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface InvestmentCdiPeriodJpaRepository extends JpaRepository<InvestmentCdiPeriodData,UUID> {
    @Query("select r from InvestmentCdiPeriodData r where r.validFrom<=:through and r.validThrough>=:from order by r.validFrom,r.sourceVersion")
    List<InvestmentCdiPeriodData> overlapping(@Param("from") LocalDate from,@Param("through") LocalDate through);
}

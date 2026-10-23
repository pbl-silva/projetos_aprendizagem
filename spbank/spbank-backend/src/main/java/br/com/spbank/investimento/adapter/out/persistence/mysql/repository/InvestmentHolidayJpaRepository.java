package br.com.spbank.investimento.adapter.out.persistence.mysql.repository;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentHolidayData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.UUID;
public interface InvestmentHolidayJpaRepository extends JpaRepository<InvestmentHolidayData,UUID> {
    boolean existsByExactDateOrMonthDay(LocalDate date,String monthDay);
}

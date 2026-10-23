package br.com.spbank.investimento.application.port.out;
import br.com.spbank.investimento.application.model.InvestmentCdiPeriod;
import br.com.spbank.investimento.application.model.InvestmentValuation;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface InvestmentReferenceData {
    List<InvestmentCdiPeriod> cdiPeriods(LocalDate fromInclusive,LocalDate throughInclusive);
    List<UUID> customersWithPositions();
    void saveValuation(InvestmentValuation valuation);
}

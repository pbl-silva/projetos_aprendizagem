package br.com.spbank.investimento.application.port.in;
import br.com.spbank.investimento.application.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface InvestmentUseCase {
    record ProductView(InvestmentProduct product,boolean fgcEligible,InvestmentQuote quote,String notice){}
    record Preview(UUID accountId,InvestmentProduct product,BigDecimal amount,LocalDate effectiveDate,
        InvestmentQuote quote,BigDecimal estimatedUnits,LocalDate estimatedMaturity,boolean gross,String notice){}
    List<ProductView> products(UUID customerId);
    Preview preview(UUID customerId,UUID accountId,UUID productId,BigDecimal amount);
    InvestmentApplication apply(UUID customerId,UUID key,InvestmentCommand command);
    List<InvestmentValuation> positions(UUID customerId);
    void processValuations(UUID customerId);
    InvestmentApplication application(UUID customerId,UUID id);
    void process(UUID applicationId);
}

package br.com.spbank.investimento.application.port.out;
import br.com.spbank.investimento.application.model.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface InvestmentPersistence {
    List<InvestmentProduct> products();
    Optional<InvestmentProduct> product(UUID id,boolean forUpdate);
    Optional<InvestmentQuote> quote(UUID productId,LocalDate through);
    boolean holiday(LocalDate date);
    java.util.Set<LocalDate> holidays(LocalDate fromInclusive,LocalDate throughInclusive);
    List<InvestmentPosition> positions(UUID customerId);
    Optional<InvestmentApplication> application(UUID customerId,UUID id,boolean forUpdate);
    Optional<UUID> owner(UUID applicationId);
    List<UUID> due(LocalDate through,int limit);
    Optional<InvestmentOperation> operation(UUID customerId,UUID key);
    void save(InvestmentApplication application);
    void save(InvestmentPosition position);
    void save(InvestmentOperation operation);
}

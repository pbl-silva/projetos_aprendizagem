package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardLateEpisode(UUID id, UUID customerId, CardBrand brand, UUID obligationId, CustomerPlan plan, BigDecimal fineRate, BigDecimal moraRate, BigDecimal initialBase, LocalDate lastProcessed, BigDecimal accruedRaw, BigDecimal fineCharged, BigDecimal moraCharged, BigDecimal remainingFine, BigDecimal remainingMora, boolean closed) implements CardFinanceRecord {
public CardLateEpisode withObligationId(UUID value){return new CardLateEpisode(id, customerId, brand, value, plan, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withPlan(CustomerPlan value){return new CardLateEpisode(id, customerId, brand, obligationId, value, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withFineRate(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, value, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withMoraRate(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, value, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withInitialBase(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, value, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withLastProcessed(LocalDate value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, value, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withAccruedRaw(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, lastProcessed, value, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withFineCharged(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, value, moraCharged, remainingFine, remainingMora, closed);}
public CardLateEpisode withMoraCharged(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, value, remainingFine, remainingMora, closed);}
public CardLateEpisode withRemainingFine(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, value, remainingMora, closed);}
public CardLateEpisode withRemainingMora(BigDecimal value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, value, closed);}
public CardLateEpisode withClosed(boolean value){return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, value);}
}

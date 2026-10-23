package br.com.spbank.cartao.application.port.out;

import br.com.spbank.cartao.application.model.CardCreditLine;
import br.com.spbank.cartao.application.model.CreditAllocationOperation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardCreditAllocationPersistence {
    List<CardCreditLine> lines(UUID customerId);
    void saveLines(UUID customerId, List<CardCreditLine> lines);
    Optional<CreditAllocationOperation> findOperation(UUID customerId, UUID key);
    void saveOperation(CreditAllocationOperation operation);
}

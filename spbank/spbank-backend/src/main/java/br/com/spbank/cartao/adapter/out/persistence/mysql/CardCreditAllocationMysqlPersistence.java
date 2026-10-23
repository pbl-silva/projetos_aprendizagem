package br.com.spbank.cartao.adapter.out.persistence.mysql;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardCreditLineData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CreditAllocationOperationData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CardCreditLineJpaRepository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CreditAllocationOperationJpaRepository;
import br.com.spbank.cartao.application.model.CardCreditLine;
import br.com.spbank.cartao.application.model.CreditAllocationOperation;
import br.com.spbank.cartao.application.model.CreditAllocationResult;
import br.com.spbank.cartao.application.port.out.CardCreditAllocationPersistence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class CardCreditAllocationMysqlPersistence implements CardCreditAllocationPersistence {
    private final CardCreditLineJpaRepository lines;
    private final CreditAllocationOperationJpaRepository operations;
    private final JsonMapper json;

    public CardCreditAllocationMysqlPersistence(CardCreditLineJpaRepository lines,
            CreditAllocationOperationJpaRepository operations, JsonMapper json) {
        this.lines = lines;
        this.operations = operations;
        this.json = json;
    }

    @Override
    public List<CardCreditLine> lines(UUID customerId) {
        return lines.findByCustomerId(customerId).stream()
            .map(line -> new CardCreditLine(line.getBrand(), line.getAllocated(), line.getCommitted()))
            .toList();
    }

    @Override
    public void saveLines(UUID customerId, List<CardCreditLine> values) {
        lines.saveAll(java.util.Objects.requireNonNull(values.stream().map(line -> new CardCreditLineData(
            customerId, line.brand(), line.allocated(), line.committed())).toList()));
    }

    @Override
    public Optional<CreditAllocationOperation> findOperation(UUID customerId, UUID key) {
        return operations.findByCustomerIdAndKey(customerId, key).map(data ->
            new CreditAllocationOperation(data.getCustomerId(), data.getKey(), data.getFingerprint(),
                json.readValue(data.getResultJson(), CreditAllocationResult.class)));
    }

    @Override
    public void saveOperation(CreditAllocationOperation operation) {
        operations.save(new CreditAllocationOperationData(operation.result().operationId(),
            operation.customerId(), operation.key(), operation.fingerprint(),
            json.writeValueAsString(operation.result())));
    }
}

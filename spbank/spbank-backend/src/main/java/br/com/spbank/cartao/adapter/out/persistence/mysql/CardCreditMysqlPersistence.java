
package br.com.spbank.cartao.adapter.out.persistence.mysql;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CustomerCreditData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CreditRequestData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CreditOperationData;

import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CustomerCreditJpaRepository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CreditRequestJpaRepository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CreditOperationJpaRepository;

import br.com.spbank.cartao.application.model.CreditOperation;
import br.com.spbank.cartao.application.model.CreditRequest;
import br.com.spbank.cartao.application.model.CreditRequestStatus;
import br.com.spbank.cartao.application.model.CreditResult;

import br.com.spbank.cartao.application.port.out.CardCreditPersistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

@Component
public final class CardCreditMysqlPersistence
        implements CardCreditPersistence {

    private final CustomerCreditJpaRepository credits;
    private final CreditRequestJpaRepository requests;
    private final CreditOperationJpaRepository operations;
    private final JsonMapper json;

    public CardCreditMysqlPersistence(
            CustomerCreditJpaRepository credits,
            CreditRequestJpaRepository requests,
            CreditOperationJpaRepository operations,
            JsonMapper json
    ) {
        this.credits = credits;
        this.requests = requests;
        this.operations = operations;
        this.json = json;
    }

    @Override
    public BigDecimal approvedTotal(
            UUID customerId
    ) {

        return credits
                .findById(customerId)
                .map(d -> d.getApprovedTotal())
                .orElse(new BigDecimal("0.00"));
    }

    @Override
    public void saveApprovedTotal(
            UUID customerId,
            BigDecimal total
    ) {

        credits.save(
                new CustomerCreditData(
                        customerId,
                        total
                )
        );
    }

    @Override
    public List<CreditRequest> requests(
            UUID customerId
    ) {

        return requests
                .findByCustomerIdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(d -> request(d))
                .toList();
    }

    @Override
    public List<CreditRequest> pendingRequests() {

        return requests
                .findByStatusOrderByCreatedAtAsc(
                        CreditRequestStatus.REQUESTED
                )
                .stream()
                .map(d -> request(d))
                .toList();
    }

    @Override
    public Optional<CreditRequest> findRequest(
            UUID customerId,
            UUID requestId
    ) {

        return requests
                .findByIdAndCustomerId(
                        requestId,
                        customerId
                )
                .map(d -> request(d));
    }

    @Override
    public void saveRequest(
            CreditRequest r
    ) {

        requests.saveAndFlush(
                new CreditRequestData(
                        r.id(),
                        r.customerId(),
                        r.requestedTotal(),
                        r.reason(),
                        r.status(),
                        r.offeredTotal(),
                        r.administratorId(),
                        r.decisionReason(),
                        r.createdAt(),
                        r.decidedAt(),
                        r.expiresAt(),
                        r.closedAt()
                )
        );
    }

    @Override
    public Optional<CreditOperation> findOperation(
            UUID customerId,
            UUID key
    ) {

        return operations
                .findByCustomerIdAndIdempotencyKey(
                        customerId,
                        key
                )
                .map(d ->
                        new CreditOperation(
                                d.getCustomerId(),
                                d.getIdempotencyKey(),
                                d.getFingerprint(),
                                json.readValue(
                                        d.getResultJson(),
                                        CreditResult.class
                                )
                        )
                );
    }

    @Override
    public void saveOperation(
            CreditOperation operation
    ) {

        var result = operation.result();

        operations.save(
                new CreditOperationData(
                        result.operationId(),
                        operation.customerId(),
                        result.request().id(),
                        operation.key(),
                        operation.fingerprint(),
                        json.writeValueAsString(result)
                )
        );
    }

    private static CreditRequest request(
            CreditRequestData d
    ) {

        return new CreditRequest(
                d.getId(),
                d.getCustomerId(),
                d.getRequestedTotal(),
                d.getReason(),
                d.getStatus(),
                d.getOfferedTotal(),
                d.getAdministratorId(),
                d.getDecisionReason(),
                d.getCreatedAt(),
                d.getDecidedAt(),
                d.getExpiresAt(),
                d.getClosedAt()
        );
    }
}

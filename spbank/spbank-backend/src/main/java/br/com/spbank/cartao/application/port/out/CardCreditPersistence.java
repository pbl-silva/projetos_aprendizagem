package br.com.spbank.cartao.application.port.out;
import br.com.spbank.cartao.application.model.*;
import java.math.BigDecimal;
import java.util.*;
public interface CardCreditPersistence {
    BigDecimal approvedTotal(UUID customerId);
    void saveApprovedTotal(UUID customerId, BigDecimal total);
    List<CreditRequest> requests(UUID customerId);
    List<CreditRequest> pendingRequests();
    Optional<CreditRequest> findRequest(UUID customerId, UUID requestId);
    void saveRequest(CreditRequest request);
    Optional<CreditOperation> findOperation(UUID customerId, UUID key);
    void saveOperation(CreditOperation operation);
}

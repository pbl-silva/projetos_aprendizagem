package br.com.spbank.cartao.application.port.in;
import br.com.spbank.cartao.application.model.*;
import java.math.BigDecimal;
import java.util.*;
public interface CardCreditUseCase {
    CreditOverview overview(UUID customerId);
    List<CreditRequest> pending(UUID administratorId);
    CreditResult request(UUID customerId, UUID key, BigDecimal total, String reason);
    CreditResult decide(UUID administratorId, UUID customerId, UUID requestId,
        UUID key, CreditDecision decision, BigDecimal total, String reason);
    CreditResult accept(UUID customerId, UUID requestId, UUID key, String password);
    CreditResult cancel(UUID customerId, UUID requestId, UUID key);
}

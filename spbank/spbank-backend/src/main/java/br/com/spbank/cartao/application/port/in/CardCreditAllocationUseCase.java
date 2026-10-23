package br.com.spbank.cartao.application.port.in;

import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.CreditAllocationResult;
import br.com.spbank.cartao.application.model.CustomerCardCredit;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface CardCreditAllocationUseCase {
    CustomerCardCredit allocation(UUID customerId);

    CreditAllocationResult redistribute(UUID customerId, UUID idempotencyKey,
            Map<CardBrand, BigDecimal> allocation, String confirmationPassword);
}

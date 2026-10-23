package br.com.spbank.cartao.application.usecase;

import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.CardCreditLine;
import br.com.spbank.cartao.application.model.CreditAllocationOperation;
import br.com.spbank.cartao.application.model.CreditAllocationResult;
import br.com.spbank.cartao.application.model.CustomerCardCredit;
import br.com.spbank.cartao.application.port.in.CardCreditAllocationUseCase;
import br.com.spbank.cartao.application.port.out.CardCreditAllocationPersistence;
import br.com.spbank.cartao.application.port.out.CardCreditPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.IdempotencyConflictException;
import br.com.spbank.shared.application.exception.UnauthorizedException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardCreditAllocationService implements CardCreditAllocationUseCase {
    private final CardCreditPersistence credit;
    private final CardCreditAllocationPersistence allocations;
    private final AuthenticationPersistence customers;
    private final Clock clock;

    public CardCreditAllocationService(CardCreditPersistence credit,
            CardCreditAllocationPersistence allocations,
            AuthenticationPersistence customers, Clock clock) {
        this.credit = credit;
        this.allocations = allocations;
        this.customers = customers;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerCardCredit allocation(UUID customerId) {
        customers.findCustomerById(customerId)
                .filter(customer -> customer.active())
                .orElseThrow(UnauthorizedException::new);
        return currentCredit(customerId);
    }

    @Override
    public CreditAllocationResult redistribute(UUID customerId, UUID idempotencyKey,
            Map<CardBrand, BigDecimal> requestedAllocation, String confirmationPassword) {
        var customer = lockCustomer(customerId);
        requireValidPassword(customerId, confirmationPassword);
        var normalizedAllocation = normalize(requestedAllocation);
        var fingerprint = fingerprint(customerId, normalizedAllocation);
        var replay = replay(customerId, idempotencyKey, fingerprint);
        if (replay != null) return replay;

        var current = currentCredit(customerId);
        var redistributed = current.redistribute(normalizedAllocation, customer.plan());
        allocations.saveLines(customerId, Arrays.stream(CardBrand.values())
                .map(redistributed.lines()::get).toList());

        var result = new CreditAllocationResult(UUID.randomUUID(), redistributed, clock.instant());
        allocations.saveOperation(new CreditAllocationOperation(
                customerId, idempotencyKey, fingerprint, result));
        return result;
    }

    private CustomerCardCredit currentCredit(UUID customerId) {
        var persisted = allocations.lines(customerId);
        var lines = new EnumMap<CardBrand, CardCreditLine>(CardBrand.class);
        for (var brand : CardBrand.values()) {
            lines.put(brand, new CardCreditLine(brand, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        for (var line : persisted) lines.put(line.brand(), line);
        return new CustomerCardCredit(customerId, credit.approvedTotal(customerId), lines);
    }

    private CreditAllocationResult replay(UUID customerId, UUID idempotencyKey,
            String fingerprint) {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        return allocations.findOperation(customerId, idempotencyKey)
                .map(operation -> {
                    if (!operation.fingerprint().equals(fingerprint)) {
                        throw new IdempotencyConflictException();
                    }
                    return operation.result();
                }).orElse(null);
    }

    private Customer lockCustomer(UUID customerId) {
        return customers.findCustomerByIdForUpdate(customerId)
                .filter(customer -> customer.active())
                .orElseThrow(UnauthorizedException::new);
    }

    private void requireValidPassword(UUID customerId, String confirmationPassword) {
        var valid = customers.findCredentialByCustomerId(customerId)
                .filter(credential -> credential.active())
                .filter(credential -> PasswordHasher.matches(
                        confirmationPassword, credential.passwordHash()))
                .isPresent();
        if (!valid) {
            throw new BusinessException("CARD_CREDIT_PASSWORD", "card.request.password");
        }
    }

    private static Map<CardBrand, BigDecimal> normalize(
            Map<CardBrand, BigDecimal> allocation) {
        if (allocation == null || allocation.size() != CardBrand.values().length) {
            throw new BusinessException("CARD_CREDIT_INVALID_AMOUNT", "card.credit.invalid-amount");
        }
        var normalized = new EnumMap<CardBrand, BigDecimal>(CardBrand.class);
        for (var brand : CardBrand.values()) {
            try {
                var line = new CardCreditLine(brand, allocation.get(brand), BigDecimal.ZERO);
                if (line.allocated().precision() > 19) throw new IllegalArgumentException();
                normalized.put(brand, line.allocated());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("CARD_CREDIT_INVALID_AMOUNT", "card.credit.invalid-amount");
            }
        }
        return Map.copyOf(normalized);
    }

    private static String fingerprint(UUID customerId,
            Map<CardBrand, BigDecimal> allocation) {
        var intention = new StringBuilder("REDISTRIBUTE|").append(customerId);
        for (var brand : CardBrand.values()) {
            intention.append('|').append(brand.name()).append('=')
                    .append(allocation.get(brand).toPlainString());
        }
        return PasswordHasher.tokenHash(intention.toString());
    }
}

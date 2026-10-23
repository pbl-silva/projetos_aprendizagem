package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.spbank.cartao.application.model.CreditOverview;
import br.com.spbank.cartao.application.port.in.CardCreditUseCase;
import br.com.spbank.shared.adapter.in.api.rest.handler.GlobalExceptionHandler;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CardCreditHttpTest {
    private final UUID customerId = UUID.randomUUID();
    private final UUID administratorId = UUID.randomUUID();
    private final CardCreditUseCase credit = mock(CardCreditUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(
                new CardCreditController(credit), new AdministrativeCardCreditController(credit))
            .setControllerAdvice(new GlobalExceptionHandler(messages))
            .build();
    }

    @Test
    void overviewUsesTheAuthenticatedCustomerAndDoesNotAutomaticallyGrantTheCeiling() throws Exception {
        when(credit.overview(customerId)).thenReturn(new CreditOverview(
            CustomerPlan.STANDARD, new BigDecimal("5000.00"), new BigDecimal("0.00"), List.of()));
        mvc.perform(java.util.Objects.requireNonNull(get("/api/v1/cards/credit").requestAttr(CUSTOMER_ID, customerId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerPlan").value("STANDARD"))
            .andExpect(jsonPath("$.ceiling").value(5000))
            .andExpect(jsonPath("$.approvedTotal").value(0));
    }

    @Test
    void requestsRequireAnIdempotencyKey() throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/cards/credit/requests").requestAttr(CUSTOMER_ID, customerId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"requestedTotal":1000,"reason":"Crédito inicial"}
                """)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(credit);
    }

    @Test
    void requestsRejectFractionalCentsBeforeCallingTheService() throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/cards/credit/requests").requestAttr(CUSTOMER_ID, customerId)
            .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"requestedTotal":1000.001,"reason":"Crédito inicial"}
                """)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(credit);
    }

    @Test
    void invalidPasswordReturnsTheDocumentedBusinessError() throws Exception {
        var requestId = UUID.randomUUID();
        var key = UUID.randomUUID();
        when(credit.accept(customerId, requestId, key, "incorreta"))
            .thenThrow(new BusinessException("CARD_CREDIT_PASSWORD", "card.request.password"));
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/cards/credit/requests/{id}/accept", requestId)
            .requestAttr(CUSTOMER_ID, customerId).header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"confirmationPassword":"incorreta"}
                """)))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.errorCode").value("CARD_CREDIT_PASSWORD"))
            .andExpect(jsonPath("$.detail").value("Senha de confirmação inválida"));
    }

    @Test
    void pendingRequestsUseTheAuthenticatedAdministrator() throws Exception {
        when(credit.pending(administratorId)).thenReturn(List.of());
        mvc.perform(java.util.Objects.requireNonNull(get("/api/v1/admin/card-credit/requests")
            .requestAttr(ADMINISTRATOR_ID, administratorId)))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(credit).pending(administratorId);
    }

    @Test
    void administrativeDecisionsRejectUnknownDecisionValues() throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/admin/card-credit/customers/{customer}/requests/{request}/decision",
                customerId, UUID.randomUUID())
            .requestAttr(ADMINISTRATOR_ID, administratorId)
            .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"decision":"APPROVE","offeredTotal":1000,"reason":"Análise"}
                """)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(credit);
    }
}

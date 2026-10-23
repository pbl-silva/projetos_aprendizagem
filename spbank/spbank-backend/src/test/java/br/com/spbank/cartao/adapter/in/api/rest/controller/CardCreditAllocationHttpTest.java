package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditAllocationDto;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.in.CardCreditAllocationUseCase;
import br.com.spbank.shared.adapter.in.api.rest.handler.GlobalExceptionHandler;
import br.com.spbank.shared.application.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CardCreditAllocationHttpTest {
    private final UUID customerId = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();
    private final CardCreditAllocationUseCase credit = mock(CardCreditAllocationUseCase.class);
    private MockMvc mvc;

    @BeforeEach void setUp() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new CardCreditAllocationController(credit))
            .setControllerAdvice(new GlobalExceptionHandler(messages)).build();
    }

    @Test void summaryIncludesServerCalculatedTotalsAndExcessWithoutNegativeAvailability() throws Exception {
        var initial = CustomerCardCredit.initial(customerId);
        var lines = new java.util.EnumMap<>(initial.lines());
        lines.put(CardBrand.BRISA, new CardCreditLine(CardBrand.BRISA, money("1000"), money("1200")));
        when(credit.allocation(customerId)).thenReturn(new CustomerCardCredit(customerId, money("5000"), lines));
        mvc.perform(java.util.Objects.requireNonNull(get("/api/v1/cards/credit/allocation").requestAttr(CUSTOMER_ID, customerId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvedTotal").value(5000))
            .andExpect(jsonPath("$.allocatedTotal").value(1000))
            .andExpect(jsonPath("$.unallocatedTotal").value(4000))
            .andExpect(jsonPath("$.lines[0].brand").value("BRISA"))
            .andExpect(jsonPath("$.lines[0].committed").value(1200))
            .andExpect(jsonPath("$.lines[0].available").value(0))
            .andExpect(jsonPath("$.lines[0].excess").value(200));
    }

    @Test void putUsesSessionCustomerAndPassesAllThreeAmountsAndPassword() throws Exception {
        var desired = Map.of(CardBrand.BRISA, money("2000"), CardBrand.MISTERSHARD, money("1500"), CardBrand.ELU, money("500"));
        var result = new CreditAllocationResult(UUID.randomUUID(), CustomerCardCredit.initial(customerId), Instant.now());
        when(credit.redistribute(customerId, key, desired, "Senha@123")).thenReturn(result);
        mvc.perform(java.util.Objects.requireNonNull(put("/api/v1/cards/credit/allocation").requestAttr(CUSTOMER_ID, customerId)
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content("""
                {"brisa":2000.00,"mistershard":1500.00,"elu":500.00,"confirmationPassword":"Senha@123"}
                """)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operationId").value(result.operationId().toString()))
            .andExpect(content().string(java.util.Objects.requireNonNull(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Senha@123")))));
        verify(credit).redistribute(customerId, key, desired, "Senha@123");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"brisa\":0,\"mistershard\":0,\"confirmationPassword\":\"senha\"}",
        "{\"brisa\":-1,\"mistershard\":0,\"elu\":0,\"confirmationPassword\":\"senha\"}",
        "{\"brisa\":1.001,\"mistershard\":0,\"elu\":0,\"confirmationPassword\":\"senha\"}",
        "{\"brisa\":100000000000000000,\"mistershard\":0,\"elu\":0,\"confirmationPassword\":\"senha\"}",
        "{\"brisa\":0,\"mistershard\":0,\"elu\":0,\"confirmationPassword\":\"\"}"
    })
    void invalidBodiesAreRejectedBeforeCallingTheService(String body) throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(put("/api/v1/cards/credit/allocation").requestAttr(CUSTOMER_ID, customerId)
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(credit);
    }

    @Test void acceptsDecimalStringsSentByTheBrowserForm() throws Exception {
        when(credit.redistribute(eq(customerId), eq(key), any(), eq("senha")))
            .thenReturn(new CreditAllocationResult(UUID.randomUUID(), CustomerCardCredit.initial(customerId), Instant.now()));
        mvc.perform(java.util.Objects.requireNonNull(put("/api/v1/cards/credit/allocation").requestAttr(CUSTOMER_ID, customerId)
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content("""
                {"brisa":"2000.00","mistershard":"1500","elu":"500.0","confirmationPassword":"senha"}
                """)))
            .andExpect(status().isOk());
        verify(credit).redistribute(eq(customerId), eq(key), argThat(values ->
            values.get(CardBrand.BRISA).compareTo(money("2000")) == 0
                && values.get(CardBrand.MISTERSHARD).compareTo(money("1500")) == 0
                && values.get(CardBrand.ELU).compareTo(money("500")) == 0), eq("senha"));
    }

    @Test void missingIdempotencyKeyIsRejected() throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(put("/api/v1/cards/credit/allocation").requestAttr(CUSTOMER_ID, customerId)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"brisa":0,"mistershard":0,"elu":0,"confirmationPassword":"senha"}
                """)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(credit);
    }

    @Test void sensitiveDtoMasksPasswordInLogging() {
        var dto = new CreditAllocationDto(money("0"), money("0"), money("0"), "Senha@123");
        assertThat(dto.toString()).contains("[REDACTED]");
        assertThat(dto.toString()).doesNotContain("Senha@123");
    }

    @Test void wrongPasswordProducesSafeBusinessResponseAndLog() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            when(credit.redistribute(eq(customerId), eq(key), any(), eq("segredo-incorreto")))
                .thenThrow(new BusinessException("CARD_CREDIT_PASSWORD", "card.request.password"));
            mvc.perform(java.util.Objects.requireNonNull(put("/api/v1/cards/credit/allocation").requestAttr(CUSTOMER_ID, customerId)
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content("""
                    {"brisa":0,"mistershard":0,"elu":0,"confirmationPassword":"segredo-incorreto"}
                    """)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Senha de confirmação inválida"));
            assertThat(appender.list).isNotEmpty().allSatisfy(event ->
                assertThat(event.getFormattedMessage()).doesNotContain("segredo-incorreto"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}

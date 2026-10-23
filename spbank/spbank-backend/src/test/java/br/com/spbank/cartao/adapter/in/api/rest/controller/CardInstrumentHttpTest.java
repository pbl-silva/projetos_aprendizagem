package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CardInstrumentDtos.*;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.in.CardInstrumentUseCase;
import br.com.spbank.cartao.application.exception.CardPinException;
import br.com.spbank.shared.adapter.in.api.rest.handler.GlobalExceptionHandler;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CardInstrumentHttpTest {
    private final UUID customerId=UUID.randomUUID(), cardId=UUID.randomUUID(), adminId=UUID.randomUUID();
    private final CardInstrumentUseCase cards=mock(CardInstrumentUseCase.class);
    private MockMvc mvc;
    @BeforeEach void setUp() {
        var messages=new ResourceBundleMessageSource(); messages.setBasename("messages"); messages.setDefaultEncoding("UTF-8");
        mvc=MockMvcBuilders.standaloneSetup(new CardInstrumentController(cards,mock(br.com.spbank.cartao.application.usecase.CardFinanceService.class)),new AdministrativeCardInstrumentController(cards))
            .setControllerAdvice(new GlobalExceptionHandler(messages)).build();
    }
    @Test void commonSummaryDoesNotExposePanCvvOrPinHash() throws Exception {
        var amount=new java.math.BigDecimal("2000.00");
        when(cards.cards(customerId)).thenReturn(List.of(new CardSummary(cardId,CardBrand.BRISA,"1234",CardState.ACTIVE,
            false,false,false,null,null,LocalDate.of(2029,10,31),amount,java.math.BigDecimal.ZERO,amount,java.math.BigDecimal.ZERO)));
        mvc.perform(java.util.Objects.requireNonNull(get("/api/v1/cards").requestAttr(CUSTOMER_ID,customerId)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].lastFour").value("1234"))
            .andExpect(jsonPath("$[0].pan").doesNotExist())
            .andExpect(jsonPath("$[0].cvv").doesNotExist())
            .andExpect(jsonPath("$[0].pinHash").doesNotExist())
            .andExpect(jsonPath("$[0].encryptedPan").doesNotExist());
        verify(cards).cards(customerId);
    }
    @Test void revealedCredentialsUsePostBodyAndCannotBeCached() throws Exception {
        when(cards.details(customerId,cardId,"0012")).thenReturn(new CardDetails("Cliente","9901000000001234","007",LocalDate.of(2029,10,31)));
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/cards/{id}/details",cardId).requestAttr(CUSTOMER_ID,customerId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"0012\"}")))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("Pragma","no-cache"))
            .andExpect(jsonPath("$.pan").value("9901000000001234"))
            .andExpect(jsonPath("$.cvv").value("007"));
        verify(cards).details(customerId,cardId,"0012");
    }
    @Test void activationPreservesLeadingZeros() throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/cards/{id}/activate",cardId).requestAttr(CUSTOMER_ID,customerId)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"confirmationPassword":"Senha@123","pin":"0012","pinConfirmation":"0012"}
                """)))
            .andExpect(status().isOk());
        verify(cards).activate(customerId,cardId,"Senha@123","0012","0012");
    }
    @ParameterizedTest @ValueSource(strings={"123","12345","abcd"})
    void invalidPinNeverReachesUseCase(String pin) throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/cards/{id}/details",cardId).requestAttr(CUSTOMER_ID,customerId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\""+pin+"\"}")))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(cards);
    }
    @Test void pinBlockIsReportedAsBusinessRefusal() throws Exception {
        when(cards.details(customerId,cardId,"9999")).thenThrow(new CardPinException(true));
        mvc.perform(java.util.Objects.requireNonNull(post("/api/v1/cards/{id}/details",cardId).requestAttr(CUSTOMER_ID,customerId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"9999\"}")))
            .andExpect(status().is(422)).andExpect(jsonPath("$.errorCode").value("CARD_PIN_BLOCKED"));
    }
    @Test void adminBlockUsesAdministratorSessionAndMandatoryCauseReasonAndValue() throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(put("/api/v1/admin/cards/customers/{customer}/{card}",customerId,cardId)
            .requestAttr(ADMINISTRATOR_ID,adminId).contentType(MediaType.APPLICATION_JSON).content("""
                {"cause":"VOLUNTARY","blocked":false,"reason":"Identidade confirmada"}
                """)))
            .andExpect(status().isOk());
        verify(cards).administrativeBlock(adminId,customerId,cardId,"VOLUNTARY",false,"Identidade confirmada");
    }
    @Test void omittedBlockValueCannotAccidentallyUnblockAnInstrument() throws Exception {
        mvc.perform(java.util.Objects.requireNonNull(put("/api/v1/admin/cards/customers/{customer}/{card}",customerId,cardId)
            .requestAttr(ADMINISTRATOR_ID,adminId).contentType(MediaType.APPLICATION_JSON).content("""
                {"cause":"ADMINISTRATIVE","reason":"Análise"}
                """)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(cards);
    }
    @Test void sensitiveDtosAreRedactedInLogging() {
        var dtos=List.of(new Activate("senha-secreta","0012","0012"),new ChangePin("0012","0007","0007"),
            new RecoverPin("senha-secreta","0007","0007"),new Reveal("0012"));
        assertThat(dtos).allSatisfy(dto -> {
            assertThat(dto.toString()).contains("REDACTED");
            assertThat(dto.toString()).doesNotContain("senha-secreta","0012","0007");
        });
    }
}

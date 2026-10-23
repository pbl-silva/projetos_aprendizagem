package br.com.spbank.investimento.adapter.in.api.rest.controller;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import br.com.spbank.investimento.adapter.in.api.rest.dto.InvestmentDtos;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.in.InvestmentUseCase;
import br.com.spbank.shared.adapter.in.api.rest.handler.GlobalExceptionHandler;
import br.com.spbank.shared.application.exception.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class InvestmentHttpTest {
    private final InvestmentUseCase service=mock(InvestmentUseCase.class);
    private final UUID customer=UUID.randomUUID(),account=UUID.randomUUID(),product=UUID.randomUUID(),key=UUID.randomUUID();
    private MockMvc mvc;
    @BeforeEach void setup(){var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");mvc=MockMvcBuilders.standaloneSetup(new InvestmentController(service)).setControllerAdvice(new GlobalExceptionHandler(messages)).build();}
    private String body(String amount){return "{\"accountId\":\""+account+"\",\"productId\":\""+product+"\",\"amount\":"+amount+",\"confirmationPassword\":\"senha-secreta\"}";}
    @Test void fractionalCentsRejectedBeforeService() throws Exception {mvc.perform(post("/api/v1/investments").requestAttr(CUSTOMER_ID,customer).header("Idempotency-Key",key.toString()).contentType(MediaType.APPLICATION_JSON).content(body("100.001"))).andExpect(status().isBadRequest());verifyNoInteractions(service);}
    @Test void missingIdempotencyKeyRejectedBeforeService() throws Exception {mvc.perform(post("/api/v1/investments").requestAttr(CUSTOMER_ID,customer).contentType(MediaType.APPLICATION_JSON).content(body("100"))).andExpect(status().isBadRequest());verifyNoInteractions(service);}
    @Test void missingCustomerCannotUseAnArbitraryId() throws Exception {mvc.perform(get("/api/v1/investments/positions")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
    @Test void pendingReturnsAcceptedAndOwnedApplicationLocation() throws Exception {var id=UUID.randomUUID();when(service.apply(eq(customer),eq(key),any())).thenReturn(new InvestmentApplication(id,customer,account,product,new BigDecimal("100"),LocalDate.of(2026,10,26),InvestmentApplication.Status.PENDING,Instant.parse("2026-10-24T12:00:00Z"),null,null,null,null));mvc.perform(post("/api/v1/investments").requestAttr(CUSTOMER_ID,customer).header("Idempotency-Key",key.toString()).contentType(MediaType.APPLICATION_JSON).content(body("100"))).andExpect(status().isAccepted()).andExpect(header().string("Location","/api/v1/investments/applications/"+id)).andExpect(jsonPath("$.status").value("PENDING"));}
    @Test void cdbCatalogOmitsFundFieldsAndHasFgc() throws Exception {var cdb=new InvestmentProduct(product,"CDB",InvestmentProduct.Type.CDB,new BigDecimal("500"),new BigDecimal("500"),"LOW",true,null,new InvestmentProduct.CdbTerms(new BigDecimal("110"),360,"AT_MATURITY"));when(service.products(customer)).thenReturn(List.of(new InvestmentUseCase.ProductView(cdb,true,null,"Valores brutos")));mvc.perform(get("/api/v1/investments/products").requestAttr(CUSTOMER_ID,customer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].fgcEligible").value(true)).andExpect(jsonPath("$[0].fund").doesNotExist()).andExpect(jsonPath("$[0].quote").doesNotExist()).andExpect(jsonPath("$[0].cdb.cdiPercent").value(110));}
    @Test void investmentErrorsHaveDomainAndNoSecrets() throws Exception {when(service.apply(eq(customer),eq(key),any())).thenThrow(new BusinessException("INVESTMENT_PASSWORD","investment.password"));mvc.perform(post("/api/v1/investments").requestAttr(CUSTOMER_ID,customer).header("Idempotency-Key",key.toString()).contentType(MediaType.APPLICATION_JSON).content(body("100"))).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.domain").value("INVESTMENT"));}
    @Test void changedIntentReturnsConflictWithSafeProblem() throws Exception {when(service.apply(eq(customer),eq(key),any())).thenThrow(new br.com.spbank.shared.application.exception.IdempotencyConflictException());mvc.perform(post("/api/v1/investments").requestAttr(CUSTOMER_ID,customer).header("Idempotency-Key",key.toString()).contentType(MediaType.APPLICATION_JSON).content(body("100"))).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT")).andExpect(jsonPath("$.domain").value("INVESTMENT"));}
    @Test void positionsExposeGrossValueAndReferenceWithoutInventingCdbUnits() throws Exception {
        var cdb=new InvestmentProduct(product,"CDB",InvestmentProduct.Type.CDB,new BigDecimal("500"),new BigDecimal("500"),"LOW",true,null,new InvestmentProduct.CdbTerms(new BigDecimal("110"),360,"AT_MATURITY"));
        var position=new InvestmentPosition(UUID.randomUUID(),UUID.randomUUID(),customer,account,cdb,new BigDecimal("500"),LocalDate.of(2026,10,23),null,null,null,LocalDate.of(2027,10,18));
        var value=new InvestmentValuation(position,LocalDate.of(2026,10,27),InvestmentValuation.Status.AVAILABLE,new BigDecimal("500.50"),new BigDecimal("0.50"),LocalDate.of(2026,10,27),null,List.of(),2,null,"a".repeat(64),"INV4-1");
        when(service.positions(customer)).thenReturn(List.of(value));
        mvc.perform(get("/api/v1/investments/positions").requestAttr(CUSTOMER_ID,customer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].grossValue").value(500.50)).andExpect(jsonPath("$[0].grossEarnings").value(0.50)).andExpect(jsonPath("$[0].referenceDate").value("2026-10-27")).andExpect(jsonPath("$[0].valuationStatus").value("AVAILABLE")).andExpect(jsonPath("$[0].valuationBasis").value("CURRENT_GROSS")).andExpect(jsonPath("$[0].units").doesNotExist()).andExpect(jsonPath("$[0].currentUnitPrice").doesNotExist());
    }
    @Test void unavailablePositionOmitsAmountsRatherThanReturningZero() throws Exception {
        var cdb=new InvestmentProduct(product,"CDB",InvestmentProduct.Type.CDB,new BigDecimal("500"),new BigDecimal("500"),"LOW",true,null,new InvestmentProduct.CdbTerms(new BigDecimal("110"),360,"AT_MATURITY"));
        var position=new InvestmentPosition(UUID.randomUUID(),UUID.randomUUID(),customer,account,cdb,new BigDecimal("500"),LocalDate.of(2026,10,23),null,null,null,LocalDate.of(2027,10,18));
        var value=new InvestmentValuation(position,LocalDate.of(2026,10,27),InvestmentValuation.Status.DATA_UNAVAILABLE,null,null,null,null,List.of(),0,"INVESTMENT_CDI_MISSING","b".repeat(64),"INV4-1");
        when(service.positions(customer)).thenReturn(List.of(value));
        mvc.perform(get("/api/v1/investments/positions").requestAttr(CUSTOMER_ID,customer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].valuationStatus").value("DATA_UNAVAILABLE")).andExpect(jsonPath("$[0].grossValue").doesNotExist()).andExpect(jsonPath("$[0].grossEarnings").doesNotExist()).andExpect(jsonPath("$[0].valuationErrorCode").value("INVESTMENT_CDI_MISSING"));
    }
    @Test void sensitiveDtosRedactPassword(){assertThat(new InvestmentDtos.Confirmation(account,product,new BigDecimal("100"),"senha-secreta").toString()).doesNotContain("senha-secreta");}
}

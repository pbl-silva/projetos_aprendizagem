package br.com.spbank.cartao.adapter.out.document;

import static org.assertj.core.api.Assertions.assertThat;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.finance.CardInvoice;
import br.com.spbank.cartao.application.usecase.CardFinanceService.InvoiceView;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class CardInvoiceDocumentExporterTest {
    private InvoiceView view(String message) {
        var invoice=new CardInvoice(UUID.randomUUID(),UUID.randomUUID(),CardBrand.BRISA,"2026-10",
            LocalDate.of(2026,10,10),LocalDate.of(2026,10,12),Instant.parse("2026-10-03T03:00:00Z"),true,new BigDecimal("123.45"));
        return new InvoiceView(invoice,List.of(),List.of(),List.of(),List.of(),new BigDecimal("23.45"),message);
    }
    @Test void csvPreservesExactBalanceAndNeutralizesSpreadsheetFormulas() {
        var csv=new String(new CardInvoiceDocumentExporter().csv(view("=SUM(1,2)")),StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\ufeff");
        assertThat(csv).contains("\"'=SUM(1,2)\"","\"23.45\"");
    }
    @Test void pdfPaginatesAndPreservesReadableBalance() throws Exception {
        var bytes=new CardInvoiceDocumentExporter().pdf(view("Detalhe ".repeat(1000)));
        try(var document=Loader.loadPDF(bytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(document)).contains("BRISA 2026-10","23.45","SALDO_ATUAL");
        }
    }
}

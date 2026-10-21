package com.biblioteca.biblioteca_api;

import com.biblioteca.biblioteca_api.reports.RelatorioPdf;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioPdfTest {
    @Test
    void deveQuebrarLinhasEPaginasEPreservarAcentos() throws Exception {
        byte[] bytes = RelatorioPdf.gerar("Relatório: João da Silva\n" + ("Um título bastante comprido ".repeat(20) + "\n").repeat(30));
        try (var doc = Loader.loadPDF(bytes)) {
            assertTrue(doc.getNumberOfPages() > 1);
            assertTrue(new PDFTextStripper().getText(doc).contains("João da Silva"));
        }
    }
}

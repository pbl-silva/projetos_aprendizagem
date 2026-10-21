package com.biblioteca.biblioteca_api.reports;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RelatorioPdf {
    private RelatorioPdf() {}

    public static byte[] gerar(String texto) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<String> linhas = new ArrayList<>();
            for (String linha : texto.split("\\R", -1)) {
                StringBuilder segura = new StringBuilder();
                linha.codePoints().forEach(code -> {
                    String caractere = new String(Character.toChars(code));
                    try { fonte.encode(caractere); segura.append(caractere); }
                    catch (IllegalArgumentException | IOException exception) { segura.append('?'); }
                });
                String restante = segura.toString();
                while (fonte.getStringWidth(restante) / 1000 * 10 > 500) {
                    int fim = restante.length();
                    while (fonte.getStringWidth(restante.substring(0, fim)) / 1000 * 10 > 500) fim--;
                    int espaco = restante.lastIndexOf(' ', fim);
                    if (espaco > 0) fim = espaco;
                    linhas.add(restante.substring(0, fim));
                    restante = restante.substring(fim).stripLeading();
                }
                linhas.add(restante);
            }
            for (int inicio = 0; inicio < linhas.size(); inicio += 48) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                documento.addPage(pagina);
                try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                    conteudo.beginText(); conteudo.setFont(fonte, 10); conteudo.setLeading(15);
                    conteudo.newLineAtOffset(45, pagina.getMediaBox().getHeight() - 50);
                    for (int i = inicio; i < Math.min(inicio + 48, linhas.size()); i++) {
                        conteudo.showText(linhas.get(i)); conteudo.newLine();
                    }
                    conteudo.endText();
                }
            }
            documento.save(bytes);
            return bytes.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("Não foi possível gerar o relatório PDF", exception); }
    }
}

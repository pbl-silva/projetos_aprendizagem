package com.estacionamento_api.estacionamento.util;

import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Utilitário puro para montar o PDF do relatório com Apache PDFBox 3.x.
 * Não depende do Spring — recebe os dados já prontos e devolve os bytes
 * do PDF.
 *
 * Nota sobre a versão: no PDFBox 3.x as constantes estáticas antigas
 * (PDType1Font.HELVETICA_BOLD) não existem mais — as 14 fontes padrão
 * agora são obtidas via "new PDType1Font(Standard14Fonts.FontName.X)".
 */
public final class RelatorioPdfGenerator {

    private static final float MARGEM = 50;
    private static final float ALTURA_MINIMA = 60; // abaixo disso, quebra de página
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private RelatorioPdfGenerator() {
    }

    public static byte[] gerar(Map<String, Object> ocupacao,
                               Map<String, Object> faturamento,
                               List<ReciboDTO> saidas,
                               LocalDateTime inicio,
                               LocalDateTime fim) throws IOException {

        try (PDDocument document = new PDDocument()) {
            PDFont fonteTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fonteTexto = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            Pagina pagina = novaPagina(document);

            escreverLinha(pagina, fonteTitulo, 18, "Relatório do Estacionamento");
            pagina.y -= 6;
            escreverLinha(pagina, fonteTexto, 10,
                "Período: " + inicio.format(FORMATO_DATA) + " até " + fim.format(FORMATO_DATA));
            pagina.y -= 14;

            escreverLinha(pagina, fonteTitulo, 13, "Ocupação Atual");
            for (Map.Entry<String, Object> entrada : ocupacao.entrySet()) {
                pagina = garantirEspaco(document, pagina);
                escreverLinha(pagina, fonteTexto, 10,
                    formatarChave(entrada.getKey()) + ": " + entrada.getValue());
            }
            pagina.y -= 14;

            pagina = garantirEspaco(document, pagina);
            escreverLinha(pagina, fonteTitulo, 13, "Faturamento do Período");
            for (Map.Entry<String, Object> entrada : faturamento.entrySet()) {
                pagina = garantirEspaco(document, pagina);
                escreverLinha(pagina, fonteTexto, 10,
                    formatarChave(entrada.getKey()) + ": " + entrada.getValue());
            }
            pagina.y -= 14;

            pagina = garantirEspaco(document, pagina);
            escreverLinha(pagina, fonteTitulo, 13, "Histórico de Saídas (" + saidas.size() + ")");

            if (saidas.isEmpty()) {
                pagina = garantirEspaco(document, pagina);
                escreverLinha(pagina, fonteTexto, 10, "Nenhuma saída registrada neste período.");
            } else {
                for (ReciboDTO saida : saidas) {
                    pagina = garantirEspaco(document, pagina);
                    String linha = String.format("%s | Vaga %s | Saída: %s | %s | R$ %s",
                        saida.getPlaca(),
                        saida.getNumeroVaga(),
                        saida.getDataHoraSaida().format(FORMATO_DATA),
                        saida.getModalidade(),
                        saida.getValorFinal());
                    escreverLinha(pagina, fonteTexto, 9, linha);
                }
            }

            pagina.content.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static Pagina novaPagina(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        Pagina pagina = new Pagina();
        pagina.content = new PDPageContentStream(document, page);
        pagina.y = PDRectangle.A4.getHeight() - MARGEM;
        return pagina;
    }

    private static Pagina garantirEspaco(PDDocument document, Pagina pagina) throws IOException {
        if (pagina.y < ALTURA_MINIMA) {
            pagina.content.close();
            return novaPagina(document);
        }
        return pagina;
    }

    private static void escreverLinha(Pagina pagina, PDFont fonte, float tamanho, String texto)
            throws IOException {
        pagina.content.beginText();
        pagina.content.setFont(fonte, tamanho);
        pagina.content.newLineAtOffset(MARGEM, pagina.y);
        pagina.content.showText(texto);
        pagina.content.endText();
        pagina.y -= (tamanho + 6);
    }

    private static String formatarChave(String chave) {
        String comEspacos = chave.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(comEspacos.charAt(0)) + comEspacos.substring(1);
    }

    /** Estado mutável da página atual: o stream aberto e a posição vertical do cursor. */
    private static class Pagina {
        PDPageContentStream content;
        float y;
    }
}

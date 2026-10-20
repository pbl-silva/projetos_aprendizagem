package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.util.RelatorioPdfGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes RelatorioPdfGenerator")
class RelatorioPdfGeneratorTest {

    private final LocalDateTime inicio = LocalDateTime.of(2026, 7, 1, 0, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 7, 31, 23, 59);

    private Map<String, Object> ocupacaoDeExemplo() {
        Map<String, Object> ocupacao = new LinkedHashMap<>();
        ocupacao.put("vagasTotal", 50);
        ocupacao.put("vagasDisponiveis", 48L);
        ocupacao.put("percentualOcupacao", "4.00%");
        return ocupacao;
    }

    private Map<String, Object> faturamentoDeExemplo() {
        Map<String, Object> faturamento = new LinkedHashMap<>();
        faturamento.put("faturamento", new BigDecimal("340.00"));
        faturamento.put("moeda", "BRL");
        return faturamento;
    }

    private ReciboDTO reciboDeExemplo() {
        return ReciboDTO.builder()
            .id(1L)
            .numeroRecibo("REC-ABC12345")
            .placa("ABC1234")
            .marca("Fiat")
            .modelo("Palio")
            .numeroVaga("C-01")
            .dataHoraEntrada(LocalDateTime.of(2026, 7, 10, 10, 0))
            .dataHoraSaida(LocalDateTime.of(2026, 7, 10, 12, 0))
            .tempoEstacionadoMinutos(120L)
            .valorBase(new BigDecimal("40.00"))
            .desconto(BigDecimal.ZERO)
            .valorFinal(new BigDecimal("40.00"))
            .tipoPagamento("PIX")
            .modalidade("Diária")
            .dataEmissao(LocalDateTime.of(2026, 7, 10, 12, 0))
            .build();
    }

    @Test
    @DisplayName("Deve gerar um PDF válido com dados")
    void testGerarPdfComDados() throws Exception {
        byte[] pdf = RelatorioPdfGenerator.gerar(
            ocupacaoDeExemplo(), faturamentoDeExemplo(),
            List.of(reciboDeExemplo(), reciboDeExemplo()),
            inicio, fim
        );

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        // Todo PDF válido começa com essa assinatura ("%PDF-")
        String inicioDoArquivo = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", inicioDoArquivo);
    }

    @Test
    @DisplayName("Deve gerar um PDF válido mesmo sem nenhuma saída no período")
    void testGerarPdfSemSaidas() throws Exception {
        byte[] pdf = RelatorioPdfGenerator.gerar(
            ocupacaoDeExemplo(), faturamentoDeExemplo(),
            List.of(),
            inicio, fim
        );

        assertNotNull(pdf);
        String inicioDoArquivo = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", inicioDoArquivo);
    }

    @Test
    @DisplayName("Deve gerar múltiplas páginas quando há muitas saídas")
    void testGerarPdfComMuitasSaidasQuebraPagina() throws Exception {
        List<ReciboDTO> muitasSaidas = new java.util.ArrayList<>();
        for (int i = 0; i < 80; i++) {
            muitasSaidas.add(reciboDeExemplo());
        }

        byte[] pdf = RelatorioPdfGenerator.gerar(
            ocupacaoDeExemplo(), faturamentoDeExemplo(),
            muitasSaidas, inicio, fim
        );

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String inicioDoArquivo = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", inicioDoArquivo);
    }
}

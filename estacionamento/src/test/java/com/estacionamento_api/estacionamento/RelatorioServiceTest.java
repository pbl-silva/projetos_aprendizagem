package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.dto.PaginaDTO;
import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoPagamento;
import com.estacionamento_api.estacionamento.enums.TipoVaga;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.model.Entrada;
import com.estacionamento_api.estacionamento.model.Saida;
import com.estacionamento_api.estacionamento.model.Vaga;
import com.estacionamento_api.estacionamento.model.Veiculo;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.SaidaRepository;
import com.estacionamento_api.estacionamento.repository.VagaRepository;
import com.estacionamento_api.estacionamento.service.RelatorioService;
import com.estacionamento_api.estacionamento.service.TarifaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes RelatorioService")
class RelatorioServiceTest {

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private SaidaRepository saidaRepository;

    private RelatorioService relatorioService;

    @BeforeEach
    void setUp() {
        relatorioService = new RelatorioService(
            vagaRepository, entradaRepository, saidaRepository, new TarifaService());
    }

    private Saida criarSaida(Long id) {
        Veiculo veiculo = Veiculo.builder()
            .id(1L).placa("ABC1234").tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat").modelo("Palio").pcd(false)
            .build();
        Vaga vaga = Vaga.builder()
            .id(1L).numero("C-01").tipoVaga(TipoVaga.COMUM).disponivel(true)
            .build();
        Entrada entrada = Entrada.builder()
            .id(id).veiculo(veiculo).vaga(vaga)
            .dataHoraEntrada(LocalDateTime.of(2026, 7, 1, 10, 0))
            .ativo(false)
            .build();
        return Saida.builder()
            .id(id)
            .entrada(entrada)
            .tipoPagamento(TipoPagamento.PIX)
            .modalidade(Modalidade.DIARIA)
            .valorPago(new BigDecimal("20.00"))
            .desconto(BigDecimal.ZERO)
            .dataHoraSaida(LocalDateTime.of(2026, 7, 1, 12, 0))
            .dataCriacao(LocalDateTime.of(2026, 7, 1, 12, 0))
            .build();
    }

    @Test
    @DisplayName("Deve retornar a página com os metadados corretos")
    void testListarSaidasPaginadoRetornaMetadadosCorretos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 7, 31, 23, 59);
        Pageable pageable = PageRequest.of(0, 2);

        List<Saida> saidas = List.of(criarSaida(1L), criarSaida(2L));
        // 5 elementos no total, só 2 voltam nessa página -> não é a última
        when(saidaRepository.findByDataHoraSaidaBetween(inicio, fim, pageable))
            .thenReturn(new PageImpl<>(saidas, pageable, 5));

        PaginaDTO<ReciboDTO> resultado = relatorioService.listarSaidasPaginado(inicio, fim, pageable);

        assertEquals(2, resultado.getConteudo().size());
        assertEquals(0, resultado.getPaginaAtual());
        assertEquals(2, resultado.getTamanhoPagina());
        assertEquals(5, resultado.getTotalElementos());
        assertEquals(3, resultado.getTotalPaginas());
        assertFalse(resultado.isUltimaPagina());
    }

    @Test
    @DisplayName("Deve indicar última página corretamente")
    void testListarSaidasPaginadoUltimaPagina() {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 7, 31, 23, 59);
        Pageable pageable = PageRequest.of(2, 2);

        List<Saida> saidas = List.of(criarSaida(5L));
        when(saidaRepository.findByDataHoraSaidaBetween(inicio, fim, pageable))
            .thenReturn(new PageImpl<>(saidas, pageable, 5));

        PaginaDTO<ReciboDTO> resultado = relatorioService.listarSaidasPaginado(inicio, fim, pageable);

        assertEquals(1, resultado.getConteudo().size());
        assertTrue(resultado.isUltimaPagina());
    }

    @Test
    @DisplayName("Deve retornar página vazia quando não há saídas no período")
    void testListarSaidasPaginadoSemResultados() {
        LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 1, 31, 23, 59);
        Pageable pageable = PageRequest.of(0, 10);

        when(saidaRepository.findByDataHoraSaidaBetween(inicio, fim, pageable))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PaginaDTO<ReciboDTO> resultado = relatorioService.listarSaidasPaginado(inicio, fim, pageable);

        assertTrue(resultado.getConteudo().isEmpty());
        assertEquals(0, resultado.getTotalElementos());
        assertTrue(resultado.isUltimaPagina());
    }

    @Test
    @DisplayName("Deve gerar um PDF válido a partir de todas as saídas do período (sem paginar)")
    void testGerarRelatorioPdf() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 7, 31, 23, 59);

        List<Saida> saidas = List.of(criarSaida(1L), criarSaida(2L));
        when(saidaRepository.findByDataHoraSaidaBetween(inicio, fim, Pageable.unpaged()))
            .thenReturn(new PageImpl<>(saidas));
        when(saidaRepository.calcularFaturamento(inicio, fim))
            .thenReturn(new BigDecimal("40.00"));
        when(vagaRepository.countVagasDisponiveis()).thenReturn(48L);
        when(entradaRepository.countVeiculosEstacionados()).thenReturn(2L);

        byte[] pdf = relatorioService.gerarRelatorioPdf(inicio, fim);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String assinatura = new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals("%PDF-", assinatura);
    }
}

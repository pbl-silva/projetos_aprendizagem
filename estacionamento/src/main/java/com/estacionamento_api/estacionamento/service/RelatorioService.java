package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.dto.PaginaDTO;
import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.exception.RequisicaoInvalidaException;
import com.estacionamento_api.estacionamento.model.Saida;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.SaidaRepository;
import com.estacionamento_api.estacionamento.repository.VagaRepository;
import com.estacionamento_api.estacionamento.util.ReciboGenerator;
import com.estacionamento_api.estacionamento.util.RelatorioPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatorioService {
    
    private final VagaRepository vagaRepository;
    private final EntradaRepository entradaRepository;
    private final SaidaRepository saidaRepository;
    private final TarifaService tarifaService;
    
    public Map<String, Object> obterTaxaOcupacao() {
        long vagasDisponiveis = vagaRepository.countVagasDisponiveis();
        long vagasTotal = vagaRepository.count();
        long vagasOcupadas = Math.max(0, vagasTotal - vagasDisponiveis);
        double percentualOcupacao = vagasTotal == 0
            ? 0.0
            : (vagasOcupadas * 100.0) / vagasTotal;
        
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("vagasTotal", vagasTotal);
        resultado.put("vagasDisponiveis", vagasDisponiveis);
        resultado.put("vagasOcupadas", vagasOcupadas);
        resultado.put("percentualOcupacao", 
            String.format(Locale.ROOT, "%.2f%%", percentualOcupacao));
        resultado.put("veiculosEstacionados", 
            entradaRepository.countVeiculosEstacionados());
        
        return resultado;
    }
    
    public Map<String, Object> obterFaturamento(LocalDateTime inicio, 
                                                    LocalDateTime fim) {
        validarPeriodo(inicio, fim);
        BigDecimal faturamento = saidaRepository.calcularFaturamento(inicio, fim);
        if (faturamento == null) {
            faturamento = BigDecimal.ZERO;
        }
        
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("periodo", inicio + " até " + fim);
        resultado.put("faturamento", faturamento);
        resultado.put("moeda", "BRL");
        
        return resultado;
    }

    /**
     * Histórico de saídas do período, paginado. Cada item vem no mesmo
     * formato do recibo emitido na hora (reaproveita ReciboGenerator e
     * TarifaService, em vez de montar um formato novo só pra esse relatório).
     */
    public PaginaDTO<ReciboDTO> listarSaidasPaginado(LocalDateTime inicio, LocalDateTime fim,
                                                     Pageable pageable) {
        validarPeriodo(inicio, fim);
        Page<Saida> pagina = saidaRepository.findByDataHoraSaidaBetween(inicio, fim, pageable);

        List<ReciboDTO> conteudo = pagina.getContent().stream()
            .map(this::converterParaRecibo)
            .collect(Collectors.toList());

        return PaginaDTO.<ReciboDTO>builder()
            .conteudo(conteudo)
            .paginaAtual(pagina.getNumber())
            .tamanhoPagina(pagina.getSize())
            .totalElementos(pagina.getTotalElements())
            .totalPaginas(pagina.getTotalPages())
            .ultimaPagina(pagina.isLast())
            .build();
    }

    /**
     * Exporta o relatório (ocupação + faturamento + histórico completo de
     * saídas do período) em PDF. Diferente de listarSaidasPaginado(), aqui
     * pegamos TODAS as saídas do período (Pageable.unpaged()), já que o PDF
     * é um documento único pra imprimir/arquivar, não uma listagem paginada
     * pra navegar na tela.
     */
    public byte[] gerarRelatorioPdf(LocalDateTime inicio, LocalDateTime fim) throws IOException {
        validarPeriodo(inicio, fim);
        Map<String, Object> ocupacao = obterTaxaOcupacao();
        Map<String, Object> faturamento = obterFaturamento(inicio, fim);

        Page<Saida> todasSaidas = saidaRepository.findByDataHoraSaidaBetween(
            inicio, fim, Pageable.unpaged());

        List<ReciboDTO> saidas = todasSaidas.getContent().stream()
            .map(this::converterParaRecibo)
            .collect(Collectors.toList());

        return RelatorioPdfGenerator.gerar(ocupacao, faturamento, saidas, inicio, fim);
    }

    private ReciboDTO converterParaRecibo(Saida saida) {
        BigDecimal valorBase = tarifaService.calcularValorBase(
            saida.getEntrada().getVeiculo().getTipoVeiculo(),
            saida.getEntrada().getDataHoraEntrada(),
            saida.getDataHoraSaida()
        );
        return ReciboGenerator.gerar(saida, valorBase);
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new RequisicaoInvalidaException("As datas de início e fim são obrigatórias");
        }
        if (inicio.isAfter(fim)) {
            throw new RequisicaoInvalidaException("A data inicial não pode ser posterior à data final");
        }
    }
}

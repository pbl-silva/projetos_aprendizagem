package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.dto.SaidaDTO;
import com.estacionamento_api.estacionamento.model.Entrada;
import com.estacionamento_api.estacionamento.model.Saida;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.SaidaRepository;
import com.estacionamento_api.estacionamento.util.ReciboGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SaidaService {
    
    private final SaidaRepository saidaRepository;
    private final EntradaRepository entradaRepository;
    private final VagaService vagaService;
    private final TarifaService tarifaService;
    
    public ReciboDTO registrarSaida(SaidaDTO dto) {
        log.debug("Registrando saída para entradaId={}", dto.getEntradaId());

        Entrada entrada = entradaRepository.findById(dto.getEntradaId())
            .orElseThrow(() -> new RuntimeException("Entrada não encontrada"));
        
        BigDecimal valorBase = tarifaService.calcularValorBase(
            entrada.getVeiculo().getTipoVeiculo(),
            entrada.getDataHoraEntrada(),
            LocalDateTime.now()
        );
        
        BigDecimal desconto = tarifaService.calcularDesconto(valorBase, dto.getModalidade());
        BigDecimal valorFinal = valorBase.subtract(desconto);
        log.debug("Cálculo da saída (entradaId={}): valorBase={}, desconto={}, valorFinal={}",
            dto.getEntradaId(), valorBase, desconto, valorFinal);
        
        Saida saida = Saida.builder()
            .entrada(entrada)
            .tipoPagamento(dto.getTipoPagamento())
            .modalidade(dto.getModalidade())
            .valorPago(valorFinal)
            .desconto(desconto)
            .build();
        
        Saida salva = saidaRepository.save(saida);
        vagaService.liberarVaga(entrada.getVaga().getId());
        entrada.setAtivo(false);
        entradaRepository.save(entrada);

        log.info("Saída registrada: placa={}, vaga={}, valorFinal={}",
            entrada.getVeiculo().getPlaca(), entrada.getVaga().getNumero(), valorFinal);
        
        return gerarRecibo(salva);
    }
    
    private ReciboDTO gerarRecibo(Saida saida) {
        Entrada entrada = saida.getEntrada();

        BigDecimal valorBase = tarifaService.calcularValorBase(
            entrada.getVeiculo().getTipoVeiculo(),
            entrada.getDataHoraEntrada(),
            saida.getDataHoraSaida()
        );

        return ReciboGenerator.gerar(saida, valorBase);
    }
    
    @Transactional(readOnly = true)
    public ReciboDTO obterRecibo(Long saidaId) {
        log.debug("Consultando recibo da saída id={}", saidaId);
        Saida saida = saidaRepository.findById(saidaId)
            .orElseThrow(() -> new RuntimeException("Saída não encontrada"));
        return gerarRecibo(saida);
    }
}

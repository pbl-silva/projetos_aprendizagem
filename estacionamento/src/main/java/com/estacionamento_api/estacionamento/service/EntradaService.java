package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.dto.EntradaDTO;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.enums.TipoVaga;
import com.estacionamento_api.estacionamento.exception.VagaNaoDisponvelException;
import com.estacionamento_api.estacionamento.model.Entrada;
import com.estacionamento_api.estacionamento.model.Vaga;
import com.estacionamento_api.estacionamento.model.Veiculo;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EntradaService {
    
    private final EntradaRepository entradaRepository;
    private final VeiculoRepository veiculoRepository;
    private final VagaService vagaService;
    
    public EntradaDTO registrarEntrada(EntradaDTO dto) {
        log.debug("Registrando entrada para veiculoId={}", dto.getVeiculoId());

        Veiculo veiculo = veiculoRepository.findById(dto.getVeiculoId())
            .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        if (entradaRepository.findByVeiculoIdAndAtivoTrue(veiculo.getId()).isPresent()) {
            log.warn("Tentativa de registrar entrada para veículo já estacionado: placa={}",
                veiculo.getPlaca());
            throw new RuntimeException("Veículo já está estacionado");
        }
        
        Vaga vaga = encontrarVagaParaVeiculo(veiculo);
        
        Entrada entrada = Entrada.builder()
            .veiculo(veiculo)
            .vaga(vaga)
            .ativo(true)
            .build();
        
        vagaService.ocuparVaga(vaga.getId());
        Entrada salva = entradaRepository.save(entrada);
        log.info("Entrada registrada: id={}, placa={}, vaga={}",
            salva.getId(), veiculo.getPlaca(), vaga.getNumero());
        return converterParaDTO(salva);
    }
    
    private Vaga encontrarVagaParaVeiculo(Veiculo veiculo) {
        TipoVeiculo tipoVeiculo = veiculo.getTipoVeiculo();
        boolean pcd = Boolean.TRUE.equals(veiculo.getPcd());

        if (tipoVeiculo == TipoVeiculo.MOTO) {
            return vagaService.encontrarVagaDisponivel(TipoVaga.MOTO);
        } else if (tipoVeiculo == TipoVeiculo.CARRO_ELETRICO) {
            try {
                return vagaService.encontrarVagaDisponivel(TipoVaga.ELETRICA);
            } catch (VagaNaoDisponvelException e) {
                log.debug("Sem vaga elétrica disponível, tentando vaga comum para placa={}",
                    veiculo.getPlaca());
                return vagaService.encontrarVagaDisponivel(TipoVaga.COMUM);
            }
        } else if (pcd) {
            // Carro PCD: prioriza vaga PCD, mas também pode usar vaga comum
            try {
                return vagaService.encontrarVagaDisponivel(TipoVaga.PCD);
            } catch (VagaNaoDisponvelException e) {
                log.debug("Sem vaga PCD disponível, tentando vaga comum para placa={}",
                    veiculo.getPlaca());
                return vagaService.encontrarVagaDisponivel(TipoVaga.COMUM);
            }
        } else {
            // Carro comum: SOMENTE vaga comum, nunca vaga PCD
            return vagaService.encontrarVagaDisponivel(TipoVaga.COMUM);
        }
    }
    
    @Transactional(readOnly = true)
    public EntradaDTO obterPorId(Long id) {
        Entrada entrada = entradaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Entrada não encontrada"));
        return converterParaDTO(entrada);
    }
    
    @Transactional(readOnly = true)
    public List<EntradaDTO> listarAtivas() {
        return entradaRepository.findAllAtivas().stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    /**
     * Cancela uma entrada registrada por engano (ex: veículo errado, vaga
     * errada). Libera a vaga e remove o registro. Só é permitido enquanto
     * a entrada ainda está ativa — se já houve saída, o registro deve ser
     * preservado como histórico.
     */
    public void cancelarEntrada(Long id) {
        log.debug("Cancelando entrada id={}", id);

        Entrada entrada = entradaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Entrada não encontrada"));

        if (!Boolean.TRUE.equals(entrada.getAtivo())) {
            log.warn("Tentativa de cancelar entrada já finalizada: id={}", id);
            throw new IllegalArgumentException(
                "Não é possível cancelar uma entrada que já foi finalizada");
        }

        vagaService.liberarVaga(entrada.getVaga().getId());
        entradaRepository.delete(entrada);
        log.info("Entrada cancelada: id={}, vaga={} liberada",
            id, entrada.getVaga().getNumero());
    }
    
    private EntradaDTO converterParaDTO(Entrada entrada) {
        return EntradaDTO.builder()
            .id(entrada.getId())
            .dataHoraEntrada(entrada.getDataHoraEntrada())
            .veiculoId(entrada.getVeiculo().getId())
            .placaVeiculo(entrada.getVeiculo().getPlaca())
            .vagaId(entrada.getVaga().getId())
            .numeroVaga(entrada.getVaga().getNumero())
            .ativo(entrada.getAtivo())
            .build();
    }
}

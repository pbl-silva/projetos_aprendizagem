package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.dto.VagaDTO;
import com.estacionamento_api.estacionamento.enums.TipoVaga;
import com.estacionamento_api.estacionamento.exception.VagaNaoDisponvelException;
import com.estacionamento_api.estacionamento.exception.RecursoNaoEncontradoException;
import com.estacionamento_api.estacionamento.model.Vaga;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.VagaRepository;
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
public class VagaService {
    
    private final VagaRepository vagaRepository;
    private final EntradaRepository entradaRepository;
    
    public void inicializarVagas() {
        log.info("Inicializando vagas do estacionamento...");
        
        // Vagas Comuns
        for (int i = 1; i <= 23; i++) {
            criarVaga("C-" + String.format("%02d", i), TipoVaga.COMUM);
        }
        
        // Vagas PCD
        for (int i = 1; i <= 15; i++) {
            criarVaga("PCD-" + String.format("%02d", i), TipoVaga.PCD);
        }
        
        // Vagas Moto
        for (int i = 1; i <= 10; i++) {
            criarVaga("M-" + String.format("%02d", i), TipoVaga.MOTO);
        }
        
        // Vagas Elétricas
        for (int i = 1; i <= 2; i++) {
            criarVaga("E-" + String.format("%02d", i), TipoVaga.ELETRICA);
        }

        log.info("Vagas inicializadas: {} vagas criadas", vagaRepository.count());
    }
    
    private void criarVaga(String numero, TipoVaga tipo) {
        if (vagaRepository.existsByNumero(numero)) {
            return;
        }
        Vaga vaga = Vaga.builder()
            .numero(numero)
            .tipoVaga(tipo)
            .disponivel(true)
            .build();
        vagaRepository.save(vaga);
    }
    
    @Transactional(readOnly = true)
    public VagaDTO obterPorId(Long id) {
        Vaga vaga = vagaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada"));
        return converterParaDTO(vaga);
    }

    /**
     * Atualização administrativa de uma vaga (ex: colocar em manutenção,
     * reclassificar o tipo). Não confundir com ocuparVaga()/liberarVaga(),
     * que são acionadas automaticamente pelo fluxo normal de entrada/saída.
     */
    public VagaDTO atualizar(Long id, VagaDTO dto) {
        log.debug("Atualizando vaga id={} (uso administrativo)", id);

        Vaga vaga = vagaRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada"));

        boolean ocupada = entradaRepository.existsByVagaIdAndAtivoTrue(id);
        if (ocupada && dto.getTipoVaga() != null && dto.getTipoVaga() != vaga.getTipoVaga()) {
            throw new IllegalArgumentException("Não é possível reclassificar uma vaga ocupada");
        }
        if (ocupada && Boolean.TRUE.equals(dto.getDisponivel())) {
            throw new IllegalArgumentException("Não é possível liberar manualmente uma vaga ocupada");
        }
        if (dto.getNumero() != null && !dto.getNumero().equals(vaga.getNumero())) {
            throw new IllegalArgumentException("O número da vaga não pode ser alterado");
        }

        if (dto.getTipoVaga() != null) {
            vaga.setTipoVaga(dto.getTipoVaga());
        }
        if (dto.getDisponivel() != null) {
            vaga.setDisponivel(dto.getDisponivel());
        }

        Vaga atualizada = vagaRepository.save(vaga);
        log.info("Vaga atualizada (admin): id={}, numero={}, tipo={}, disponivel={}",
            atualizada.getId(), atualizada.getNumero(),
            atualizada.getTipoVaga(), atualizada.getDisponivel());
        return converterParaDTO(atualizada);
    }
    
    @Transactional(readOnly = true)
    public List<VagaDTO> listarTodas() {
        return vagaRepository.findAll().stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VagaDTO> listarDisponiveis() {
        return vagaRepository.findAll().stream()
            .filter(Vaga::getDisponivel)
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<VagaDTO> listarDisponiveisPorTipo(TipoVaga tipo) {
        return vagaRepository.findByTipoVagaAndDisponivel(tipo, true).stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }
    
    public Vaga encontrarVagaDisponivel(TipoVaga tipo) {
        return vagaRepository.findFirstByTipoVagaAndDisponivelTrueOrderByIdAsc(tipo)
            .orElseThrow(() -> new VagaNaoDisponvelException(
                "Nenhuma vaga disponível do tipo: " + tipo.getDescricao()
            ));
    }
    
    public void ocuparVaga(Long vagaId) {
        Vaga vaga = vagaRepository.findByIdForUpdate(vagaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada"));
        vaga.setDisponivel(false);
        vagaRepository.save(vaga);
        log.debug("Vaga ocupada: id={}, numero={}", vaga.getId(), vaga.getNumero());
    }
    
    public void liberarVaga(Long vagaId) {
        Vaga vaga = vagaRepository.findByIdForUpdate(vagaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada"));
        vaga.setDisponivel(true);
        vagaRepository.save(vaga);
        log.debug("Vaga liberada: id={}, numero={}", vaga.getId(), vaga.getNumero());
    }
    
    @Transactional(readOnly = true)
    public long obterVagasDisponiveis() {
        return vagaRepository.countVagasDisponiveis();
    }
    
    @Transactional(readOnly = true)
    public double obterTaxaOcupacao() {
        long total = vagaRepository.count();
        if (total == 0) {
            return 0.0;
        }
        long disponiveis = obterVagasDisponiveis();
        return ((total - disponiveis) * 100.0) / total;
    }
    
    private VagaDTO converterParaDTO(Vaga vaga) {
        return VagaDTO.builder()
            .id(vaga.getId())
            .numero(vaga.getNumero())
            .tipoVaga(vaga.getTipoVaga())
            .disponivel(vaga.getDisponivel())
            .build();
    }
}

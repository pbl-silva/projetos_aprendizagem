package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.dto.VeiculoDTO;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.exception.VeiculoNaoEncontradoException;
import com.estacionamento_api.estacionamento.exception.VeiculoNaoPermitidoException;
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
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final EntradaRepository entradaRepository;

    public VeiculoDTO cadastrar(VeiculoDTO dto) {
        log.debug("Cadastrando veículo: placa={}, tipo={}", dto.getPlaca(), dto.getTipoVeiculo());

        if (veiculoRepository.existsByPlaca(dto.getPlaca())) {
            log.warn("Tentativa de cadastro com placa já existente: {}", dto.getPlaca());
            throw new IllegalArgumentException(
                "Já existe um veículo cadastrado com a placa: " + dto.getPlaca());
        }

        boolean pcd = dto.getPcd() != null && dto.getPcd();
        if (pcd && dto.getTipoVeiculo() != TipoVeiculo.CARRO) {
            log.warn("Tentativa de cadastrar {} como PCD (placa={})",
                dto.getTipoVeiculo(), dto.getPlaca());
            throw new VeiculoNaoPermitidoException(
                "Somente veículos do tipo CARRO podem ser cadastrados como PCD");
        }

        Veiculo veiculo = Veiculo.builder()
            .placa(dto.getPlaca())
            .tipoVeiculo(dto.getTipoVeiculo())
            .marca(dto.getMarca())
            .modelo(dto.getModelo())
            .cor(dto.getCor())
            .pcd(pcd)
            .build();

        Veiculo salvo = veiculoRepository.save(veiculo);
        log.info("Veículo cadastrado: id={}, placa={}", salvo.getId(), salvo.getPlaca());
        return converterParaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public VeiculoDTO obterPorId(Long id) {
        Veiculo veiculo = veiculoRepository.findById(id)
            .orElseThrow(() -> new VeiculoNaoEncontradoException(
                "Veículo não encontrado com id: " + id));
        return converterParaDTO(veiculo);
    }

    @Transactional(readOnly = true)
    public VeiculoDTO obterPorPlaca(String placa) {
        Veiculo veiculo = veiculoRepository.findByPlaca(placa)
            .orElseThrow(() -> new VeiculoNaoEncontradoException(
                "Veículo não encontrado com placa: " + placa));
        return converterParaDTO(veiculo);
    }

    @Transactional(readOnly = true)
    public List<VeiculoDTO> listarTodos() {
        log.debug("Listando todos os veículos");
        return veiculoRepository.findAll().stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    public VeiculoDTO atualizar(Long id, VeiculoDTO dto) {
        log.debug("Atualizando veículo id={}", id);

        Veiculo veiculo = veiculoRepository.findById(id)
            .orElseThrow(() -> new VeiculoNaoEncontradoException(
                "Veículo não encontrado com id: " + id));

        if (dto.getPlaca() != null && !dto.getPlaca().equalsIgnoreCase(veiculo.getPlaca())) {
            log.warn("Tentativa de alterar a placa do veículo id={} de {} para {}",
                id, veiculo.getPlaca(), dto.getPlaca());
            throw new IllegalArgumentException(
                "A placa não pode ser alterada. Cadastre um novo veículo caso a placa tenha mudado.");
        }

        boolean pcd = dto.getPcd() != null && dto.getPcd();
        TipoVeiculo tipoVeiculo = dto.getTipoVeiculo() != null
            ? dto.getTipoVeiculo() : veiculo.getTipoVeiculo();

        if (pcd && tipoVeiculo != TipoVeiculo.CARRO) {
            log.warn("Tentativa de marcar {} como PCD na atualização do veículo id={}",
                tipoVeiculo, id);
            throw new VeiculoNaoPermitidoException(
                "Somente veículos do tipo CARRO podem ser cadastrados como PCD");
        }

        veiculo.setTipoVeiculo(tipoVeiculo);
        veiculo.setMarca(dto.getMarca());
        veiculo.setModelo(dto.getModelo());
        veiculo.setCor(dto.getCor());
        veiculo.setPcd(pcd);

        Veiculo atualizado = veiculoRepository.save(veiculo);
        log.info("Veículo atualizado: id={}, placa={}", atualizado.getId(), atualizado.getPlaca());
        return converterParaDTO(atualizado);
    }

    public void deletar(Long id) {
        log.debug("Excluindo veículo id={}", id);

        Veiculo veiculo = veiculoRepository.findById(id)
            .orElseThrow(() -> new VeiculoNaoEncontradoException(
                "Veículo não encontrado com id: " + id));

        if (entradaRepository.existsByVeiculoId(id)) {
            log.warn("Tentativa de excluir veículo id={} (placa={}) com histórico de entradas",
                id, veiculo.getPlaca());
            throw new IllegalArgumentException(
                "Não é possível excluir um veículo com histórico de entradas");
        }

        veiculoRepository.delete(veiculo);
        log.info("Veículo excluído: id={}, placa={}", id, veiculo.getPlaca());
    }

    private VeiculoDTO converterParaDTO(Veiculo veiculo) {
        return VeiculoDTO.builder()
            .id(veiculo.getId())
            .placa(veiculo.getPlaca())
            .tipoVeiculo(veiculo.getTipoVeiculo())
            .marca(veiculo.getMarca())
            .modelo(veiculo.getModelo())
            .cor(veiculo.getCor())
            .pcd(veiculo.getPcd())
            .build();
    }
}

package com.ecommerce.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.api.dto.PagamentoDTO;
import com.ecommerce.api.enums.StatusPagamento;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Pagamento;
import com.ecommerce.api.model.Pedido;
import com.ecommerce.api.repository.PagamentoRepository;
import com.ecommerce.api.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PagamentoService {
    
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    
    public PagamentoDTO criar(PagamentoDTO dto) {
        Pedido pedido = pedidoRepository.findById(dto.getPedidoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pedido não encontrado com ID: " + dto.getPedidoId()
            ));
        
        Pagamento pagamento = Pagamento.builder()
            .pedido(pedido)
            .valor(dto.getValor())
            .metodo(dto.getMetodo())
            .status(StatusPagamento.PENDENTE)
            .dataCriacao(LocalDateTime.now())
            .build();
        
        Pagamento salvo = pagamentoRepository.save(pagamento);
        return converterParaDTO(salvo);
    }
    
    @Transactional(readOnly = true)
    public PagamentoDTO obterPorId(Long id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pagamento não encontrado com ID: " + id
            ));
        return converterParaDTO(pagamento);
    }
    
    @Transactional(readOnly = true)
    public List<PagamentoDTO> listarTodos() {
        return pagamentoRepository.findAll()
            .stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PagamentoDTO> listarComPaginacao(int pagina, int tamanho, String ordenarPor, String direcao) {
        if (tamanho > 100) {
            tamanho = 100;
        }
        Sort.Direction direction = Sort.Direction.fromString(direcao.toUpperCase());
        Sort sort = Sort.by(direction, ordenarPor);
        Pageable pageable = PageRequest.of(pagina, tamanho, sort);
        return pagamentoRepository.findAll(pageable).map(this::converterParaDTO);
    }
    
    public PagamentoDTO atualizar(Long id, PagamentoDTO dto) {
        Pagamento pagamento = pagamentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pagamento não encontrado com ID: " + id
            ));
        
        if (dto.getValor() != null) {
            pagamento.setValor(dto.getValor());
        }
        
        if (dto.getMetodo() != null) {
            pagamento.setMetodo(dto.getMetodo());
        }
        
        Pagamento atualizado = pagamentoRepository.save(pagamento);
        return converterParaDTO(atualizado);
    }
    
    public PagamentoDTO atualizarStatus(Long id, StatusPagamento novoStatus) {
        Pagamento pagamento = pagamentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pagamento não encontrado com ID: " + id
            ));
        
        pagamento.setStatus(novoStatus);
        
        Pagamento atualizado = pagamentoRepository.save(pagamento);
        return converterParaDTO(atualizado);
    }
    
    public void deletar(Long id) {
        if (!pagamentoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                "Pagamento não encontrado com ID: " + id
            );
        }
        pagamentoRepository.deleteById(id);
    }
    
    private PagamentoDTO converterParaDTO(Pagamento pagamento) {
        return PagamentoDTO.builder()
            .id(pagamento.getId())
            .pedidoId(pagamento.getPedido().getId())
            .valor(pagamento.getValor())
            .metodo(pagamento.getMetodo())
            .status(pagamento.getStatus())
            .dataCriacao(pagamento.getDataCriacao())
            .build();
    }
}
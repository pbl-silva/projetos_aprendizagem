package com.ecommerce.api.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
        
        List<Pagamento> existentes = pagamentoRepository
            .findByPedidoIdOrderByDataCriacaoDesc(pedido.getId());
        Pagamento pagamento;
        if (existentes.isEmpty()) {
            pagamento = Pagamento.builder()
                .pedido(pedido)
                .valor(dto.getValor())
                .metodo(dto.getMetodo())
                .status(StatusPagamento.PENDENTE)
                .dataCriacao(LocalDateTime.now())
                .build();
        } else {
            pagamento = prepararNovaTentativa(existentes.get(0), dto);
            if (existentes.size() > 1) {
                pagamentoRepository.deleteAll(existentes.subList(1, existentes.size()));
            }
        }
        
        Pagamento salvo = pagamentoRepository.save(pagamento);
        return converterParaDTO(salvo);
    }

    public PagamentoDTO criarParaCliente(PagamentoDTO dto, String email) {
        Pedido pedido = pedidoRepository.findById(dto.getPedidoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pedido não encontrado com ID: " + dto.getPedidoId()
            ));
        if (!pedido.getCliente().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("O pedido não pertence ao usuário autenticado");
        }
        // O cliente não escolhe o valor cobrado: ele sempre vem do pedido.
        dto.setValor(pedido.getTotal());
        return criar(dto);
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
        return pagamentosMaisRecentes(pagamentoRepository.findAll())
            .stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PagamentoDTO> listarDoCliente(String email) {
        return pagamentosMaisRecentes(
                pagamentoRepository.findByPedidoClienteEmailOrderByDataCriacaoDesc(email))
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

    private Pagamento prepararNovaTentativa(Pagamento pagamento, PagamentoDTO dto) {
        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            throw new IllegalArgumentException("Este pedido já possui pagamento aprovado");
        }
        if (pagamento.getStatus() == StatusPagamento.REEMBOLSADO) {
            throw new IllegalArgumentException("Pagamento reembolsado não pode ser reaberto");
        }
        pagamento.setValor(dto.getValor());
        pagamento.setMetodo(dto.getMetodo());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        return pagamento;
    }

    private List<Pagamento> pagamentosMaisRecentes(List<Pagamento> pagamentos) {
        LinkedHashMap<Long, Pagamento> porPedido = new LinkedHashMap<>();
        pagamentos.stream()
            .sorted((a, b) -> b.getDataCriacao().compareTo(a.getDataCriacao()))
            .forEach(pagamento -> porPedido.putIfAbsent(
                pagamento.getPedido().getId(), pagamento));
        return List.copyOf(porPedido.values());
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

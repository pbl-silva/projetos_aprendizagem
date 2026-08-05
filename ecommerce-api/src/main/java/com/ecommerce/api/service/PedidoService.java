package com.ecommerce.api.service;

import com.ecommerce.api.dto.PedidoDTO;
import com.ecommerce.api.enums.StatusPedido;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.model.Pedido;
import com.ecommerce.api.repository.ClienteRepository;
import com.ecommerce.api.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {
    
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    
    public PedidoDTO criar(PedidoDTO dto) {
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente não encontrado com ID: " + dto.getClienteId()
            ));
        
        Pedido pedido = Pedido.builder()
            .cliente(cliente)
            .total(dto.getTotal() != null ? dto.getTotal() : 0.0)
            .status(StatusPedido.PENDENTE)
            .dataCriacao(LocalDateTime.now())
            .dataAtualizacao(LocalDateTime.now())
            .build();
        
        Pedido salvo = pedidoRepository.save(pedido);
        return converterParaDTO(salvo);
    }
    
    @Transactional(readOnly = true)
    public PedidoDTO obterPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pedido não encontrado com ID: " + id
            ));
        return converterParaDTO(pedido);
    }
    
    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll()
            .stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PedidoDTO> listarComPaginacao(int pagina, int tamanho, String ordenarPor, String direcao) {
        if (tamanho > 100) {
            tamanho = 100;
        }
        Sort.Direction direction = Sort.Direction.fromString(direcao.toUpperCase());
        Sort sort = Sort.by(direction, ordenarPor);
        Pageable pageable = PageRequest.of(pagina, tamanho, sort);
        return pedidoRepository.findAll(pageable).map(this::converterParaDTO);
    }
    
    public PedidoDTO atualizar(Long id, PedidoDTO dto) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pedido não encontrado com ID: " + id
            ));
        
        if (dto.getStatus() != null) {
            pedido.setStatus(dto.getStatus());
        }
        
        if (dto.getTotal() != null) {
            pedido.setTotal(dto.getTotal());
        }
        
        pedido.setDataAtualizacao(LocalDateTime.now());
        
        Pedido atualizado = pedidoRepository.save(pedido);
        return converterParaDTO(atualizado);
    }
    
    public void deletar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                "Pedido não encontrado com ID: " + id
            );
        }
        pedidoRepository.deleteById(id);
    }
    
    private PedidoDTO converterParaDTO(Pedido pedido) {
        return PedidoDTO.builder()
            .id(pedido.getId())
            .clienteId(pedido.getCliente().getId())
            .total(pedido.getTotal())
            .status(pedido.getStatus())
            .dataCriacao(pedido.getDataCriacao())
            .dataAtualizacao(pedido.getDataAtualizacao())
            .build();
    }
}
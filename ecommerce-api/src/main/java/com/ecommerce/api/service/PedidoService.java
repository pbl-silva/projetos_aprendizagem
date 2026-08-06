package com.ecommerce.api.service;

import com.ecommerce.api.dto.ItemPedidoDTO;
import com.ecommerce.api.dto.PedidoDTO;
import com.ecommerce.api.enums.StatusPedido;
import com.ecommerce.api.exception.EstoqueInsuficienteException;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.model.ItemPedido;
import com.ecommerce.api.model.Pedido;
import com.ecommerce.api.model.Produto;
import com.ecommerce.api.repository.ClienteRepository;
import com.ecommerce.api.repository.PedidoRepository;
import com.ecommerce.api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    /**
     * Cria um pedido e baixa o estoque de todos os seus itens na mesma transação.
     * O preço e o total são obtidos do catálogo para evitar valores manipulados pelo cliente.
     */
    public PedidoDTO criar(PedidoDTO dto) {
        if (dto.getItens() == null || dto.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter pelo menos um produto");
        }

        Map<Long, Integer> itensAgrupados = agruparItens(dto.getItens());

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente não encontrado com ID: " + dto.getClienteId()
            ));

        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new IllegalArgumentException("Cliente inativo não pode realizar pedidos");
        }

        Pedido pedido = Pedido.builder()
            .cliente(cliente)
            .status(StatusPedido.PENDENTE)
            .dataCriacao(LocalDateTime.now())
            .dataAtualizacao(LocalDateTime.now())
            .itens(new ArrayList<>())
            .build();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> itemSolicitado : itensAgrupados.entrySet()) {
            Produto produto = produtoRepository.findByIdForUpdate(itemSolicitado.getKey())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Produto não encontrado com ID: " + itemSolicitado.getKey()
                ));

            validarDisponibilidade(produto, itemSolicitado.getValue());

            ItemPedido item = ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidade(itemSolicitado.getValue())
                .precoUnitario(produto.getPreco())
                .build();

            pedido.getItens().add(item);
            produto.setEstoque(produto.getEstoque() - itemSolicitado.getValue());
            total = total.add(item.getSubtotal());
        }

        pedido.setTotal(total.doubleValue());
        return converterParaDTO(pedidoRepository.save(pedido));
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

        StatusPedido statusAtual = pedido.getStatus();
        StatusPedido novoStatus = dto.getStatus();
        if (statusAtual == StatusPedido.CANCELADO
                && novoStatus != null && novoStatus != StatusPedido.CANCELADO) {
            throw new IllegalArgumentException("Pedido cancelado não pode ser reativado");
        }

        if (novoStatus == StatusPedido.CANCELADO && statusAtual != StatusPedido.CANCELADO) {
            restaurarEstoque(pedido);
        }

        if (novoStatus != null) {
            pedido.setStatus(novoStatus);
        }

        pedido.setDataAtualizacao(LocalDateTime.now());
        return converterParaDTO(pedidoRepository.save(pedido));
    }

    public void deletar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pedido não encontrado com ID: " + id
            ));

        // Excluir um pedido equivale a desfazer sua reserva. Um pedido ja
        // cancelado teve o estoque devolvido anteriormente.
        if (pedido.getStatus() != StatusPedido.CANCELADO) {
            restaurarEstoque(pedido);
        }
        pedidoRepository.delete(pedido);
    }

    private void restaurarEstoque(Pedido pedido) {
        if (pedido.getItens() == null) {
            return;
        }
        for (ItemPedido item : pedido.getItens().stream()
                .sorted((a, b) -> a.getProduto().getId().compareTo(b.getProduto().getId()))
                .toList()) {
            Long produtoId = item.getProduto().getId();
            Produto produto = produtoRepository.findByIdForUpdate(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Produto não encontrado com ID: " + produtoId
                ));
            produto.setEstoque(somarQuantidade(produto.getEstoque(), item.getQuantidade()));
        }
    }

    private Map<Long, Integer> agruparItens(List<ItemPedidoDTO> itens) {
        // Ordem deterministica de bloqueio evita deadlocks quando dois pedidos
        // concorrentes contem os mesmos produtos em ordens diferentes.
        Map<Long, Integer> itensAgrupados = new TreeMap<>();
        for (ItemPedidoDTO item : itens) {
            if (item == null || item.getProdutoId() == null) {
                throw new IllegalArgumentException("Produto é obrigatório em cada item do pedido");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new IllegalArgumentException("Quantidade deve ser maior que zero");
            }
            itensAgrupados.merge(item.getProdutoId(), item.getQuantidade(), this::somarQuantidade);
        }
        return itensAgrupados;
    }

    private int somarQuantidade(int atual, int quantidade) {
        try {
            return Math.addExact(atual, quantidade);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Quantidade total excede o limite permitido", ex);
        }
    }

    private void validarDisponibilidade(Produto produto, int quantidade) {
        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new IllegalArgumentException("Produto indisponível: " + produto.getNome());
        }
        if (produto.getEstoque() < quantidade) {
            throw new EstoqueInsuficienteException(
                "Estoque insuficiente para " + produto.getNome() +
                    ". Disponível: " + produto.getEstoque() + ", solicitado: " + quantidade
            );
        }
    }

    private PedidoDTO converterParaDTO(Pedido pedido) {
        List<ItemPedidoDTO> itens = pedido.getItens() == null ? List.of() : pedido.getItens().stream()
            .map(item -> ItemPedidoDTO.builder()
                .id(item.getId())
                .produtoId(item.getProduto().getId())
                .produtoNome(item.getProduto().getNome())
                .quantidade(item.getQuantidade())
                .precoUnitario(item.getPrecoUnitario())
                .subtotal(item.getSubtotal())
                .build())
            .toList();

        return PedidoDTO.builder()
            .id(pedido.getId())
            .clienteId(pedido.getCliente().getId())
            .total(pedido.getTotal())
            .status(pedido.getStatus())
            .dataCriacao(pedido.getDataCriacao())
            .dataAtualizacao(pedido.getDataAtualizacao())
            .itens(itens)
            .build();
    }
}

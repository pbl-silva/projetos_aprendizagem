package com.ecommerce;

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
import com.ecommerce.api.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do PedidoService")
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ProdutoRepository produtoRepository;
    @InjectMocks private PedidoService pedidoService;

    private Cliente cliente;
    private Produto produto;
    private Pedido pedido;
    private PedidoDTO pedidoDTO;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(1L).nome("Cliente Teste").build();
        produto = Produto.builder()
            .id(2L).nome("Produto Teste").preco(new BigDecimal("50.00"))
            .estoque(10).ativo(true).build();
        ItemPedido item = ItemPedido.builder()
            .id(3L).produto(produto).quantidade(2).precoUnitario(new BigDecimal("50.00")).build();
        pedido = Pedido.builder()
            .id(10L).cliente(cliente).total(100.0).status(StatusPedido.PENDENTE)
            .dataCriacao(LocalDateTime.now()).dataAtualizacao(LocalDateTime.now()).itens(List.of(item)).build();
        pedidoDTO = PedidoDTO.builder().clienteId(1L).itens(List.of(itemDTO(2L, 2))).build();
    }

    @Test
    @DisplayName("Deve criar pedido com itens, calcular total e baixar estoque")
    void testCriar() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoDTO resultado = pedidoService.criar(pedidoDTO);

        assertEquals(StatusPedido.PENDENTE, resultado.getStatus());
        assertEquals(1L, resultado.getClienteId());
        assertEquals(100.0, resultado.getTotal());
        assertEquals(1, resultado.getItens().size());
        assertEquals(8, produto.getEstoque());
    }

    @Test
    @DisplayName("Deve rejeitar pedido sem produtos")
    void testCriarSemItens() {
        PedidoDTO semItens = PedidoDTO.builder().clienteId(1L).itens(List.of()).build();

        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(semItens));
        verifyNoInteractions(clienteRepository, produtoRepository, pedidoRepository);
    }

    @Test
    @DisplayName("Deve rejeitar soma de quantidades acima do limite inteiro")
    void testCriarQuantidadeComOverflow() {
        PedidoDTO dto = PedidoDTO.builder().clienteId(1L).itens(List.of(
            itemDTO(2L, Integer.MAX_VALUE), itemDTO(2L, 1))).build();

        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(dto));
        verifyNoInteractions(clienteRepository, produtoRepository, pedidoRepository);
    }

    @Test
    @DisplayName("Deve rejeitar pedido quando estoque for insuficiente")
    void testCriarComEstoqueInsuficiente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(produto));
        PedidoDTO quantidadeMaiorQueEstoque = PedidoDTO.builder()
            .clienteId(1L).itens(List.of(itemDTO(2L, 11))).build();

        assertThrows(EstoqueInsuficienteException.class,
            () -> pedidoService.criar(quantidadeMaiorQueEstoque));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido para cliente inexistente")
    void testCriarClienteNaoEncontrado() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());
        PedidoDTO dto = PedidoDTO.builder().clienteId(999L).itens(List.of(itemDTO(2L, 1))).build();

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.criar(dto));
    }

    @Test
    @DisplayName("Deve rejeitar pedido de cliente inativo")
    void testCriarClienteInativo() {
        cliente.setAtivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThrows(IllegalArgumentException.class, () -> pedidoService.criar(pedidoDTO));
        verifyNoInteractions(produtoRepository, pedidoRepository);
    }

    @Test
    @DisplayName("Deve obter pedido por ID")
    void testObterPorId() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertEquals(10L, pedidoService.obterPorId(10L).getId());
    }

    @Test
    @DisplayName("Deve lançar exceção ao obter pedido inexistente")
    void testObterPorIdNaoEncontrado() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.obterPorId(999L));
    }

    @Test
    @DisplayName("Deve listar todos os pedidos")
    void testListarTodos() {
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));

        assertEquals(1, pedidoService.listarTodos().size());
    }

    @Test
    @DisplayName("Deve atualizar somente o status do pedido")
    void testAtualizar() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO resultado = pedidoService.atualizar(10L,
            PedidoDTO.builder().status(StatusPedido.CONFIRMADO).total(999.0).build());

        assertEquals(StatusPedido.CONFIRMADO, resultado.getStatus());
        assertEquals(100.0, resultado.getTotal());
    }

    @Test
    @DisplayName("Deve devolver estoque ao cancelar pedido apenas uma vez")
    void testCancelarRestauraEstoque() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(produtoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        pedidoService.atualizar(10L, PedidoDTO.builder().status(StatusPedido.CANCELADO).build());

        assertEquals(12, produto.getEstoque());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        verify(produtoRepository).findByIdForUpdate(2L);
    }

    @Test
    @DisplayName("Nao deve reativar pedido cancelado")
    void testNaoReativarPedidoCancelado() {
        pedido.setStatus(StatusPedido.CANCELADO);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizar(10L,
            PedidoDTO.builder().status(StatusPedido.CONFIRMADO).build()));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar pedido inexistente")
    void testAtualizarNaoEncontrado() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
            () -> pedidoService.atualizar(999L, pedidoDTO));
    }

    @Test
    @DisplayName("Deve listar pedidos com paginação")
    void testListarComPaginacao() {
        var page = new org.springframework.data.domain.PageImpl<>(List.of(pedido));
        when(pedidoRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        assertEquals(1, pedidoService.listarComPaginacao(0, 10, "id", "ASC").getTotalElements());
    }

    @Test
    @DisplayName("Deve deletar pedido existente")
    void testDeletar() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(produtoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(produto));

        pedidoService.deletar(10L);

        assertEquals(12, produto.getEstoque());
        verify(pedidoRepository).delete(pedido);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar pedido inexistente")
    void testDeletarNaoEncontrado() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.deletar(999L));
    }

    private ItemPedidoDTO itemDTO(Long produtoId, int quantidade) {
        return ItemPedidoDTO.builder().produtoId(produtoId).quantidade(quantidade).build();
    }
}

package com.ecommerce;

import com.ecommerce.api.dto.PedidoDTO;
import com.ecommerce.api.enums.StatusPedido;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.model.Pedido;
import com.ecommerce.api.repository.ClienteRepository;
import com.ecommerce.api.repository.PedidoRepository;
import com.ecommerce.api.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do PedidoService")
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private PedidoService pedidoService;

    private Cliente cliente;
    private Pedido pedido;
    private PedidoDTO pedidoDTO;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(1L).nome("Cliente Teste").build();

        pedido = Pedido.builder()
            .id(10L)
            .cliente(cliente)
            .total(100.0)
            .status(StatusPedido.PENDENTE)
            .dataCriacao(LocalDateTime.now())
            .dataAtualizacao(LocalDateTime.now())
            .build();

        pedidoDTO = PedidoDTO.builder()
            .clienteId(1L)
            .total(100.0)
            .build();
    }

    @Test
    @DisplayName("Deve criar pedido vinculado a um cliente existente")
    void testCriar() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO resultado = pedidoService.criar(pedidoDTO);

        assertNotNull(resultado);
        assertEquals(StatusPedido.PENDENTE, resultado.getStatus());
        assertEquals(1L, resultado.getClienteId());
    }

    @Test
    @DisplayName("Deve criar pedido com total 0.0 quando não informado")
    void testCriarSemTotal() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO dtoSemTotal = PedidoDTO.builder().clienteId(1L).build();

        PedidoDTO resultado = pedidoService.criar(dtoSemTotal);

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido para cliente inexistente")
    void testCriarClienteNaoEncontrado() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());
        PedidoDTO dto = PedidoDTO.builder().clienteId(999L).total(50.0).build();

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.criar(dto));
    }

    @Test
    @DisplayName("Deve obter pedido por ID")
    void testObterPorId() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        PedidoDTO resultado = pedidoService.obterPorId(10L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
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

        List<PedidoDTO> resultado = pedidoService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve atualizar status e total do pedido")
    void testAtualizar() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO atualizacao = PedidoDTO.builder()
            .status(StatusPedido.CONFIRMADO)
            .total(200.0)
            .build();

        PedidoDTO resultado = pedidoService.atualizar(10L, atualizacao);

        assertNotNull(resultado);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
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
        org.springframework.data.domain.Page<Pedido> page =
            new org.springframework.data.domain.PageImpl<>(List.of(pedido));
        when(pedidoRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        var resultado = pedidoService.listarComPaginacao(0, 10, "id", "ASC");

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Deve deletar pedido existente")
    void testDeletar() {
        when(pedidoRepository.existsById(10L)).thenReturn(true);

        pedidoService.deletar(10L);

        verify(pedidoRepository, times(1)).deleteById(10L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar pedido inexistente")
    void testDeletarNaoEncontrado() {
        when(pedidoRepository.existsById(999L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.deletar(999L));
    }
}
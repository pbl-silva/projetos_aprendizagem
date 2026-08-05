package com.ecommerce;

import com.ecommerce.api.dto.PagamentoDTO;
import com.ecommerce.api.enums.StatusPagamento;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.model.Pagamento;
import com.ecommerce.api.model.Pedido;
import com.ecommerce.api.repository.PagamentoRepository;
import com.ecommerce.api.repository.PedidoRepository;
import com.ecommerce.api.service.PagamentoService;
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
@DisplayName("Testes do PagamentoService")
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Pedido pedido;
    private Pagamento pagamento;
    private PagamentoDTO pagamentoDTO;

    @BeforeEach
    void setUp() {
        Cliente cliente = Cliente.builder().id(1L).nome("Cliente Teste").build();

        pedido = Pedido.builder()
            .id(10L)
            .cliente(cliente)
            .total(150.0)
            .build();

        pagamento = Pagamento.builder()
            .id(1L)
            .pedido(pedido)
            .valor(150.0)
            .metodo("CARTAO")
            .status(StatusPagamento.PENDENTE)
            .dataCriacao(LocalDateTime.now())
            .build();

        pagamentoDTO = PagamentoDTO.builder()
            .pedidoId(10L)
            .valor(150.0)
            .metodo("CARTAO")
            .build();
    }

    @Test
    @DisplayName("Deve criar pagamento vinculado a um pedido existente")
    void testCriar() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

        PagamentoDTO resultado = pagamentoService.criar(pagamentoDTO);

        assertNotNull(resultado);
        assertEquals("CARTAO", resultado.getMetodo());
        assertEquals(StatusPagamento.PENDENTE, resultado.getStatus());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pagamento para pedido inexistente")
    void testCriarPedidoNaoEncontrado() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());
        PagamentoDTO dto = PagamentoDTO.builder().pedidoId(999L).valor(10.0).metodo("PIX").build();

        assertThrows(RecursoNaoEncontradoException.class, () -> pagamentoService.criar(dto));
    }

    @Test
    @DisplayName("Deve obter pagamento por ID")
    void testObterPorId() {
        when(pagamentoRepository.findById(1L)).thenReturn(Optional.of(pagamento));

        PagamentoDTO resultado = pagamentoService.obterPorId(1L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getPedidoId());
    }

    @Test
    @DisplayName("Deve lançar exceção ao obter pagamento inexistente")
    void testObterPorIdNaoEncontrado() {
        when(pagamentoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pagamentoService.obterPorId(999L));
    }

    @Test
    @DisplayName("Deve listar todos os pagamentos")
    void testListarTodos() {
        when(pagamentoRepository.findAll()).thenReturn(List.of(pagamento));

        List<PagamentoDTO> resultado = pagamentoService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve atualizar valor e método do pagamento")
    void testAtualizar() {
        when(pagamentoRepository.findById(1L)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

        PagamentoDTO atualizacao = PagamentoDTO.builder().valor(200.0).metodo("PIX").build();

        PagamentoDTO resultado = pagamentoService.atualizar(1L, atualizacao);

        assertNotNull(resultado);
        verify(pagamentoRepository, times(1)).save(any(Pagamento.class));
    }

    @Test
    @DisplayName("Deve atualizar apenas o status do pagamento")
    void testAtualizarStatus() {
        when(pagamentoRepository.findById(1L)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

        PagamentoDTO resultado = pagamentoService.atualizarStatus(1L, StatusPagamento.APROVADO);

        assertNotNull(resultado);
        verify(pagamentoRepository, times(1)).save(any(Pagamento.class));
    }

    @Test
    @DisplayName("Deve listar pagamentos com paginação")
    void testListarComPaginacao() {
        org.springframework.data.domain.Page<Pagamento> page =
            new org.springframework.data.domain.PageImpl<>(List.of(pagamento));
        when(pagamentoRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        var resultado = pagamentoService.listarComPaginacao(0, 10, "id", "ASC");

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Deve deletar pagamento existente")
    void testDeletar() {
        when(pagamentoRepository.existsById(1L)).thenReturn(true);

        pagamentoService.deletar(1L);

        verify(pagamentoRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar pagamento inexistente")
    void testDeletarNaoEncontrado() {
        when(pagamentoRepository.existsById(999L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> pagamentoService.deletar(999L));
    }
}
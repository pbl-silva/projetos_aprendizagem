package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.dto.VeiculoDTO;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.exception.VeiculoNaoEncontradoException;
import com.estacionamento_api.estacionamento.exception.VeiculoNaoPermitidoException;
import com.estacionamento_api.estacionamento.model.Veiculo;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.VeiculoRepository;
import com.estacionamento_api.estacionamento.service.VeiculoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes VeiculoService")
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private EntradaRepository entradaRepository;

    @InjectMocks
    private VeiculoService veiculoService;

    @Test
    @DisplayName("Deve cadastrar carro comum marcado como PCD")
    void testCadastrarCarroPcd() {
        VeiculoDTO dto = VeiculoDTO.builder()
            .placa("ABC1234")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat")
            .modelo("Palio")
            .pcd(true)
            .build();

        when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(false);
        when(veiculoRepository.save(any(Veiculo.class)))
            .thenAnswer(invocation -> {
                Veiculo v = invocation.getArgument(0);
                v.setId(1L);
                return v;
            });

        VeiculoDTO resultado = veiculoService.cadastrar(dto);

        assertNotNull(resultado);
        assertTrue(resultado.getPcd());
    }

    @Test
    @DisplayName("Não deve permitir moto cadastrada como PCD")
    void testMotoNaoPodeSerPcd() {
        VeiculoDTO dto = VeiculoDTO.builder()
            .placa("MOT1234")
            .tipoVeiculo(TipoVeiculo.MOTO)
            .marca("Honda")
            .modelo("CG")
            .pcd(true)
            .build();

        when(veiculoRepository.existsByPlaca("MOT1234")).thenReturn(false);

        assertThrows(VeiculoNaoPermitidoException.class, () -> {
            veiculoService.cadastrar(dto);
        });

        verify(veiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve permitir carro elétrico cadastrado como PCD")
    void testCarroEletricoNaoPodeSerPcd() {
        VeiculoDTO dto = VeiculoDTO.builder()
            .placa("ELE1234")
            .tipoVeiculo(TipoVeiculo.CARRO_ELETRICO)
            .marca("Tesla")
            .modelo("Model 3")
            .pcd(true)
            .build();

        when(veiculoRepository.existsByPlaca("ELE1234")).thenReturn(false);

        assertThrows(VeiculoNaoPermitidoException.class, () -> {
            veiculoService.cadastrar(dto);
        });
    }

    @Test
    @DisplayName("Não deve permitir placa duplicada")
    void testPlacaDuplicada() {
        VeiculoDTO dto = VeiculoDTO.builder()
            .placa("ABC1234")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat")
            .modelo("Palio")
            .build();

        when(veiculoRepository.existsByPlaca("ABC1234")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            veiculoService.cadastrar(dto);
        });
    }

    @Test
    @DisplayName("Deve atualizar marca, modelo e cor de um veículo existente")
    void testAtualizarVeiculo() {
        Veiculo existente = Veiculo.builder()
            .id(1L).placa("ABC1234").tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat").modelo("Palio").pcd(false)
            .build();

        VeiculoDTO dto = VeiculoDTO.builder()
            .placa("ABC1234")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat")
            .modelo("Palio Weekend")
            .cor("Prata")
            .build();

        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VeiculoDTO resultado = veiculoService.atualizar(1L, dto);

        assertEquals("Palio Weekend", resultado.getModelo());
        assertEquals("Prata", resultado.getCor());
    }

    @Test
    @DisplayName("Não deve permitir alterar a placa via atualização")
    void testAtualizarNaoPermiteAlterarPlaca() {
        Veiculo existente = Veiculo.builder()
            .id(1L).placa("ABC1234").tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat").modelo("Palio").pcd(false)
            .build();

        VeiculoDTO dto = VeiculoDTO.builder()
            .placa("XYZ9999")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat")
            .modelo("Palio")
            .build();

        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class, () -> {
            veiculoService.atualizar(1L, dto);
        });
        verify(veiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve excluir veículo sem histórico de entradas")
    void testDeletarVeiculoSemHistorico() {
        Veiculo existente = Veiculo.builder()
            .id(1L).placa("ABC1234").tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat").modelo("Palio").pcd(false)
            .build();

        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(entradaRepository.existsByVeiculoId(1L)).thenReturn(false);

        veiculoService.deletar(1L);

        verify(veiculoRepository, times(1)).delete(existente);
    }

    @Test
    @DisplayName("Não deve excluir veículo com histórico de entradas")
    void testNaoDeveExcluirVeiculoComHistorico() {
        Veiculo existente = Veiculo.builder()
            .id(1L).placa("ABC1234").tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat").modelo("Palio").pcd(false)
            .build();

        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(entradaRepository.existsByVeiculoId(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            veiculoService.deletar(1L);
        });
        verify(veiculoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao excluir veículo inexistente")
    void testDeletarVeiculoInexistente() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VeiculoNaoEncontradoException.class, () -> {
            veiculoService.deletar(99L);
        });
    }
}

package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.dto.EntradaDTO;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.enums.TipoVaga;
import com.estacionamento_api.estacionamento.model.Entrada;
import com.estacionamento_api.estacionamento.model.Vaga;
import com.estacionamento_api.estacionamento.model.Veiculo;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.VeiculoRepository;
import com.estacionamento_api.estacionamento.service.EntradaService;
import com.estacionamento_api.estacionamento.service.VagaService;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("Testes EntradaService")
class EntradaServiceTest {
    
    @Mock
    private EntradaRepository entradaRepository;
    
    @Mock
    private VeiculoRepository veiculoRepository;
    
    @Mock
    private VagaService vagaService;
    
    @InjectMocks
    private EntradaService entradaService;
    
    private Veiculo veiculo;
    private Vaga vaga;
    private EntradaDTO entradaDTO;
    
    @BeforeEach
    void setUp() {
        veiculo = Veiculo.builder()
            .id(1L)
            .placa("ABC-1234")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Toyota")
            .modelo("Corolla")
            .cor("Preto")
            .pcd(false)
            .build();
        
        vaga = Vaga.builder()
            .id(1L)
            .numero("C-01")
            .tipoVaga(TipoVaga.COMUM)
            .disponivel(true)
            .build();
        
        entradaDTO = EntradaDTO.builder()
            .veiculoId(1L)
            .vagaId(1L)
            .build();
    }
    
    @Test
    @DisplayName("Deve registrar entrada com sucesso")
    void testRegistrarEntrada() {
        when(veiculoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(veiculo));
        when(entradaRepository.findByVeiculoIdAndAtivoTrue(1L))
            .thenReturn(Optional.empty());
        when(vagaService.encontrarVagaDisponivel(TipoVaga.COMUM))
            .thenReturn(vaga);
        when(entradaRepository.save(any(Entrada.class)))
            .thenAnswer(invocation -> {
                Entrada entrada = invocation.getArgument(0);
                entrada.setId(1L);
                return entrada;
            });
        
        EntradaDTO resultado = entradaService.registrarEntrada(entradaDTO);
        
        assertNotNull(resultado);
        assertEquals("ABC-1234", resultado.getPlacaVeiculo());
        verify(vagaService, times(1)).ocuparVaga(1L);
    }
    
    @Test
    @DisplayName("Deve lançar exceção se veículo já está estacionado")
    void testRegistrarEntradaVeiculoJaEstacionado() {
        Entrada entradaExistente = Entrada.builder()
            .id(1L)
            .veiculo(veiculo)
            .vaga(vaga)
            .ativo(true)
            .build();
        
        when(veiculoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(veiculo));
        when(entradaRepository.findByVeiculoIdAndAtivoTrue(1L))
            .thenReturn(Optional.of(entradaExistente));
        
        assertThrows(RuntimeException.class, () -> {
            entradaService.registrarEntrada(entradaDTO);
        });
    }

    @Test
    @DisplayName("Deve cancelar uma entrada ativa e liberar a vaga")
    void testCancelarEntradaAtiva() {
        Entrada entradaAtiva = Entrada.builder()
            .id(1L)
            .veiculo(veiculo)
            .vaga(vaga)
            .ativo(true)
            .build();

        when(entradaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entradaAtiva));

        entradaService.cancelarEntrada(1L);

        verify(vagaService, times(1)).liberarVaga(1L);
        verify(entradaRepository, times(1)).delete(entradaAtiva);
    }

    @Test
    @DisplayName("Não deve cancelar uma entrada já finalizada")
    void testNaoDeveCancelarEntradaFinalizada() {
        Entrada entradaFinalizada = Entrada.builder()
            .id(1L)
            .veiculo(veiculo)
            .vaga(vaga)
            .ativo(false)
            .build();

        when(entradaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entradaFinalizada));

        assertThrows(IllegalArgumentException.class, () -> {
            entradaService.cancelarEntrada(1L);
        });
        verify(vagaService, never()).liberarVaga(any());
        verify(entradaRepository, never()).delete(any());
    }
}
   

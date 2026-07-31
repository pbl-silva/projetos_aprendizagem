package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.dto.SaidaDTO;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoPagamento;
import com.estacionamento_api.estacionamento.enums.TipoVaga;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.model.Entrada;
import com.estacionamento_api.estacionamento.model.Saida;
import com.estacionamento_api.estacionamento.model.Vaga;
import com.estacionamento_api.estacionamento.model.Veiculo;
import com.estacionamento_api.estacionamento.repository.EntradaRepository;
import com.estacionamento_api.estacionamento.repository.SaidaRepository;
import com.estacionamento_api.estacionamento.service.SaidaService;
import com.estacionamento_api.estacionamento.service.TarifaService;
import com.estacionamento_api.estacionamento.service.VagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do SaidaService. A TarifaService é usada de verdade (não mockada)
 * porque não tem dependências e é onde vivia o bug do desconto aplicado
 * em dobro — mockar o cálculo esconderia justamente esse tipo de regressão.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes SaidaService")
class SaidaServiceTest {

    @Mock
    private SaidaRepository saidaRepository;

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private VagaService vagaService;

    private SaidaService saidaService;
    private Entrada entrada;

    @BeforeEach
    void setUp() {
        saidaService = new SaidaService(
            saidaRepository, entradaRepository, vagaService, new TarifaService());

        Veiculo veiculo = Veiculo.builder()
            .id(1L)
            .placa("ABC1234")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat")
            .modelo("Palio")
            .pcd(false)
            .build();

        Vaga vaga = Vaga.builder()
            .id(1L)
            .numero("C-01")
            .tipoVaga(TipoVaga.COMUM)
            .disponivel(false)
            .build();

        entrada = Entrada.builder()
            .id(1L)
            .veiculo(veiculo)
            .vaga(vaga)
            .dataHoraEntrada(LocalDateTime.now().minusMinutes(30)) // ~1 hora cobrada
            .ativo(true)
            .build();
    }

    /**
     * @PrePersist só roda com um EntityManager de verdade, então no mock
     * do save() precisamos simular isso manualmente (id, dataHoraSaida, dataCriacao).
     */
    private Saida simularPersistencia(Saida saida, Long id) {
        saida.setId(id);
        saida.setDataHoraSaida(LocalDateTime.now());
        saida.setDataCriacao(LocalDateTime.now());
        return saida;
    }

    @Test
    @DisplayName("Deve aplicar o desconto da modalidade uma única vez (regressão do bug de desconto em dobro)")
    void testRegistrarSaidaAplicaDescontoUmaUnicaVez() {
        SaidaDTO dto = SaidaDTO.builder()
            .entradaId(1L)
            .tipoPagamento(TipoPagamento.PIX)
            .modalidade(Modalidade.MENSAL)
            .build();

        when(entradaRepository.findById(1L)).thenReturn(Optional.of(entrada));
        when(saidaRepository.save(any(Saida.class)))
            .thenAnswer(inv -> simularPersistencia(inv.getArgument(0), 1L));

        ReciboDTO recibo = saidaService.registrarSaida(dto);

        // Carro: R$20/diária, 1 hora cobrada, desconto mensal de 15%
        // 20 * 0.85 = 17.00 -- se o desconto fosse aplicado 2x, daria 14.45
        assertEquals(0, recibo.getValorBase().compareTo(new BigDecimal("20.00")));
        assertEquals(0, recibo.getDesconto().compareTo(new BigDecimal("3.00")));
        assertEquals(0, recibo.getValorFinal().compareTo(new BigDecimal("17.00")),
            "valorFinal deveria ser 17.00 (desconto único), mas foi " + recibo.getValorFinal());

        verify(vagaService, times(1)).liberarVaga(1L);
        verify(entradaRepository, times(1)).save(entrada);
        assertFalse(entrada.getAtivo());
    }

    @Test
    @DisplayName("Não deve aplicar desconto na modalidade diária")
    void testRegistrarSaidaModalidadeDiariaSemDesconto() {
        SaidaDTO dto = SaidaDTO.builder()
            .entradaId(1L)
            .tipoPagamento(TipoPagamento.DINHEIRO)
            .modalidade(Modalidade.DIARIA)
            .build();

        when(entradaRepository.findById(1L)).thenReturn(Optional.of(entrada));
        when(saidaRepository.save(any(Saida.class)))
            .thenAnswer(inv -> simularPersistencia(inv.getArgument(0), 2L));

        ReciboDTO recibo = saidaService.registrarSaida(dto);

        assertEquals(0, recibo.getValorFinal().compareTo(new BigDecimal("20.00")));
        assertEquals(0, recibo.getDesconto().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Deve lançar exceção se a entrada não for encontrada")
    void testRegistrarSaidaEntradaNaoEncontrada() {
        SaidaDTO dto = SaidaDTO.builder()
            .entradaId(99L)
            .tipoPagamento(TipoPagamento.PIX)
            .modalidade(Modalidade.DIARIA)
            .build();

        when(entradaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> saidaService.registrarSaida(dto));
        verify(saidaRepository, never()).save(any(Saida.class));
    }

    @Test
    @DisplayName("Deve obter recibo de uma saída existente")
    void testObterRecibo() {
        Saida saida = Saida.builder()
            .id(1L)
            .entrada(entrada)
            .tipoPagamento(TipoPagamento.PIX)
            .modalidade(Modalidade.MENSAL)
            .valorPago(new BigDecimal("17.00"))
            .desconto(new BigDecimal("3.00"))
            .dataHoraSaida(LocalDateTime.now())
            .dataCriacao(LocalDateTime.now())
            .build();

        when(saidaRepository.findById(1L)).thenReturn(Optional.of(saida));

        ReciboDTO recibo = saidaService.obterRecibo(1L);

        assertNotNull(recibo);
        assertEquals("ABC1234", recibo.getPlaca());
        assertEquals(0, recibo.getValorFinal().compareTo(new BigDecimal("17.00")));
    }

    @Test
    @DisplayName("Deve lançar exceção ao obter recibo inexistente")
    void testObterReciboNaoEncontrado() {
        when(saidaRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> saidaService.obterRecibo(404L));
    }
}

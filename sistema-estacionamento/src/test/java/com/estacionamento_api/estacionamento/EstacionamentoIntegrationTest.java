package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.dto.EntradaDTO;
import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.dto.SaidaDTO;
import com.estacionamento_api.estacionamento.dto.VagaDTO;
import com.estacionamento_api.estacionamento.dto.VeiculoDTO;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoPagamento;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração: sobem o contexto Spring de verdade (controller ->
 * service -> repository -> H2) e batem via MockMvc, simulando requisições
 * HTTP reais. Diferente dos testes unitários (que mockam as camadas), aqui
 * o objetivo é validar a "fiação": roteamento, serialização JSON,
 * validação de Bean Validation e códigos de status HTTP de ponta a ponta.
 *
 * @Transactional na classe faz cada teste rodar numa transação que é
 * desfeita ao final, então os testes não interferem uns nos outros nem
 * dependem de ordem de execução.
 *
 * @WithMockUser simula um usuário já autenticado para todos os testes
 * desta classe — o objetivo aqui é testar regra de negócio (validação,
 * status HTTP, JSON), não o mecanismo de autenticação em si. O fluxo real
 * de login/token é validado à parte em AuthIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
@DisplayName("Testes de integração da API")
class EstacionamentoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Não usamos o ObjectMapper gerenciado pelo Spring de propósito: no
    // Spring Boot 4, com os starters divididos (spring-boot-starter-webmvc),
    // esse bean nem sempre fica disponível para injeção no contexto de teste.
    // Instanciar o nosso próprio evita depender disso — mesma abordagem do
    // ApiClient usado pelo Swing.
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    private VeiculoDTO cadastrarVeiculo(String placa, TipoVeiculo tipo, boolean pcd) throws Exception {
        VeiculoDTO dto = VeiculoDTO.builder()
            .placa(placa)
            .tipoVeiculo(tipo)
            .marca("Fiat")
            .modelo("Palio")
            .pcd(pcd)
            .build();

        MvcResult result = mockMvc.perform(post("/veiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), VeiculoDTO.class);
    }

    @Test
    @DisplayName("Fluxo completo: cadastrar veículo, registrar entrada, registrar saída")
    void testFluxoCompletoEntradaSaida() throws Exception {
        VeiculoDTO veiculo = cadastrarVeiculo("INT0001", TipoVeiculo.CARRO, false);

        EntradaDTO entradaRequisicao = EntradaDTO.builder().veiculoId(veiculo.getId()).build();

        MvcResult resultEntrada = mockMvc.perform(post("/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entradaRequisicao)))
            .andExpect(status().isCreated())
            .andReturn();

        EntradaDTO entrada = objectMapper.readValue(
            resultEntrada.getResponse().getContentAsString(), EntradaDTO.class);

        assertEquals("INT0001", entrada.getPlacaVeiculo());
        assertNotNull(entrada.getNumeroVaga());

        SaidaDTO saidaRequisicao = SaidaDTO.builder()
            .entradaId(entrada.getId())
            .modalidade(Modalidade.DIARIA)
            .tipoPagamento(TipoPagamento.PIX)
            .build();

        MvcResult resultSaida = mockMvc.perform(post("/saidas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saidaRequisicao)))
            .andExpect(status().isCreated())
            .andReturn();

        ReciboDTO recibo = objectMapper.readValue(
            resultSaida.getResponse().getContentAsString(), ReciboDTO.class);

        assertEquals("INT0001", recibo.getPlaca());
        // Carro, modalidade diária (sem desconto), mínimo de 1 hora cobrada: R$ 20,00
        assertEquals(0, recibo.getValorFinal().compareTo(new BigDecimal("20.00")));
        assertNotNull(recibo.getNumeroRecibo());

        MvcResult segundaConsulta = mockMvc.perform(get("/saidas/recibo/" + recibo.getId()))
            .andExpect(status().isOk())
            .andReturn();
        ReciboDTO reciboConsultado = objectMapper.readValue(
            segundaConsulta.getResponse().getContentAsString(), ReciboDTO.class);
        assertEquals(recibo.getNumeroRecibo(), reciboConsultado.getNumeroRecibo(),
            "O número do recibo deve permanecer estável em consultas posteriores");

        mockMvc.perform(post("/saidas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saidaRequisicao)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /veiculos sem placa deve retornar 400 com o campo indicado")
    void testCadastrarVeiculoSemPlacaRetorna400() throws Exception {
        VeiculoDTO dto = VeiculoDTO.builder()
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat")
            .modelo("Palio")
            .build();

        MvcResult result = mockMvc.perform(post("/veiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andReturn();

        Map<?, ?> corpo = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<?, ?> campos = (Map<?, ?>) corpo.get("campos");
        assertTrue(campos.containsKey("placa"));
    }

    @Test
    @DisplayName("Cadastrar moto como PCD deve retornar 400 (regra de negócio via HTTP)")
    void testCadastrarMotoComoPcdRetorna400() throws Exception {
        VeiculoDTO dto = VeiculoDTO.builder()
            .placa("INT0002")
            .tipoVeiculo(TipoVeiculo.MOTO)
            .marca("Honda")
            .modelo("CG")
            .pcd(true)
            .build();

        mockMvc.perform(post("/veiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /veiculos/{id} inexistente deve retornar 404")
    void testObterVeiculoInexistenteRetorna404() throws Exception {
        MvcResult result = mockMvc.perform(get("/veiculos/999999"))
            .andExpect(status().isNotFound())
            .andReturn();

        Map<?, ?> corpo = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertEquals("Veículo não encontrado", corpo.get("erro"));
    }

    @Test
    @DisplayName("GET /entradas/{id} inexistente deve retornar 404")
    void testObterEntradaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/entradas/999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("JSON com enum inválido deve retornar 400")
    void testEnumInvalidoRetorna400() throws Exception {
        String json = "{\"placa\":\"BAD0001\",\"tipoVeiculo\":\"AVIAO\","
            + "\"marca\":\"Teste\",\"modelo\":\"Teste\"}";

        mockMvc.perform(post("/veiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve normalizar placa para maiúsculas e sem hífen")
    void testNormalizaPlaca() throws Exception {
        VeiculoDTO veiculo = cadastrarVeiculo("abc-1d23", TipoVeiculo.CARRO, false);
        assertEquals("ABC1D23", veiculo.getPlaca());

        mockMvc.perform(get("/veiculos/placa/abc-1d23"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /veiculos/{id} tentando mudar a placa deve retornar 409")
    void testAtualizarNaoPermitePlacaRetorna409() throws Exception {
        VeiculoDTO veiculo = cadastrarVeiculo("INT0003", TipoVeiculo.CARRO, false);

        VeiculoDTO alteracao = VeiculoDTO.builder()
            .placa("XYZ9999")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .marca("Fiat")
            .modelo("Palio")
            .build();

        mockMvc.perform(put("/veiculos/" + veiculo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alteracao)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /veiculos/{id} com histórico de entradas deve retornar 409")
    void testExcluirVeiculoComHistoricoRetorna409() throws Exception {
        VeiculoDTO veiculo = cadastrarVeiculo("INT0004", TipoVeiculo.CARRO, false);

        EntradaDTO entradaRequisicao = EntradaDTO.builder().veiculoId(veiculo.getId()).build();
        mockMvc.perform(post("/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entradaRequisicao)))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/veiculos/" + veiculo.getId()))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /entradas/{id} deve cancelar a entrada e liberar a vaga")
    void testCancelarEntradaLiberaVaga() throws Exception {
        VeiculoDTO veiculo = cadastrarVeiculo("INT0005", TipoVeiculo.CARRO, false);

        EntradaDTO entradaRequisicao = EntradaDTO.builder().veiculoId(veiculo.getId()).build();
        MvcResult resultEntrada = mockMvc.perform(post("/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entradaRequisicao)))
            .andExpect(status().isCreated())
            .andReturn();

        EntradaDTO entrada = objectMapper.readValue(
            resultEntrada.getResponse().getContentAsString(), EntradaDTO.class);

        mockMvc.perform(delete("/entradas/" + entrada.getId()))
            .andExpect(status().isNoContent());

        MvcResult resultVaga = mockMvc.perform(get("/vagas/" + entrada.getVagaId()))
            .andExpect(status().isOk())
            .andReturn();

        VagaDTO vaga = objectMapper.readValue(resultVaga.getResponse().getContentAsString(), VagaDTO.class);
        assertTrue(vaga.getDisponivel());
    }

    @Test
    @DisplayName("GET /vagas deve retornar as 50 vagas inicializadas na subida da aplicação")
    void testListarVagasRetorna50Vagas() throws Exception {
        MvcResult result = mockMvc.perform(get("/vagas"))
            .andExpect(status().isOk())
            .andReturn();

        List<?> vagas = objectMapper.readValue(result.getResponse().getContentAsString(), List.class);
        assertEquals(50, vagas.size());
    }
}

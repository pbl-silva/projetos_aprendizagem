package com.biblioteca.biblioteca_api;

import com.biblioteca.biblioteca_api.services.GerenciadorEmprestimo;
import com.biblioteca.biblioteca_api.validators.EmprestimoValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestClockConfig.class)
@SpringBootTest
class EmprestimoServiceIntegrationTest {

    // Depende da interface (implementação real: EmprestimoServiceImpl)
    @Autowired
    private GerenciadorEmprestimo emprestimoService;

    // garante que o contexto injete um mock do validator
    @MockBean
    private EmprestimoValidator validator;

    @Test
    void contextoCarregaService() {
        assertNotNull(emprestimoService);
    }

    // adicione aqui testes de integração que usem DB em memória; o validator é um @MockBean
}
package com.biblioteca.biblioteca_api;

import com.biblioteca.biblioteca_api.services.GerenciadorEmprestimo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestClockConfig.class)
@SpringBootTest
class EmprestimoServiceIntegrationTest {

    @Autowired
    private GerenciadorEmprestimo emprestimoService;

    @Test
    void contextoCarregaService() {
        assertNotNull(emprestimoService);
    }
}

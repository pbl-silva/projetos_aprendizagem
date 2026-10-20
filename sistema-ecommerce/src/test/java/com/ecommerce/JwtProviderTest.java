package com.ecommerce;

import com.ecommerce.api.security.JwtProperties;
import com.ecommerce.api.security.JwtProvider;
import com.ecommerce.api.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do JwtProvider")
class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("chave-secreta-de-teste-com-pelo-menos-32-caracteres");
        properties.setExpiration(3600000L);
        jwtProvider = new JwtProvider(properties);
    }

    @Test
    @DisplayName("Deve gerar um token JWT valido")
    void testGenerateToken() {
        String token = jwtProvider.generateToken("usuario@teste.com", 1L, Usuario.Papel.USER);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("Deve extrair o email correto do token")
    void testGetEmailFromToken() {
        String token = jwtProvider.generateToken("usuario@teste.com", 1L, Usuario.Papel.USER);

        String email = jwtProvider.getEmailFromToken(token);

        assertEquals("usuario@teste.com", email);
    }

    @Test
    @DisplayName("Deve extrair o userId correto do token")
    void testGetUserIdFromToken() {
        String token = jwtProvider.generateToken("usuario@teste.com", 42L, Usuario.Papel.USER);

        Long userId = jwtProvider.getUserIdFromToken(token);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("Deve retornar null ao extrair email de token invalido")
    void testGetEmailFromTokenInvalido() {
        String email = jwtProvider.getEmailFromToken("token-invalido-qualquer");

        assertNull(email);
    }

    @Test
    @DisplayName("Deve retornar null ao extrair userId de token invalido")
    void testGetUserIdFromTokenInvalido() {
        Long userId = jwtProvider.getUserIdFromToken("token-invalido-qualquer");

        assertNull(userId);
    }

    @Test
    @DisplayName("Deve validar um token recem-gerado como valido")
    void testValidateTokenValido() {
        String token = jwtProvider.generateToken("usuario@teste.com", 1L, Usuario.Papel.USER);

        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    @DisplayName("Deve invalidar um token corrompido")
    void testValidateTokenInvalido() {
        assertFalse(jwtProvider.validateToken("token.invalido.aqui"));
    }

}

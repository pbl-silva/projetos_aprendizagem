package com.ecommerce;

import com.ecommerce.api.exception.ErrorResponse;
import com.ecommerce.api.exception.GlobalExceptionHandler;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private WebRequest webRequest;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/teste");
    }

    @Test
    @DisplayName("Deve tratar RecursoNaoEncontradoException como 404")
    void testHandleRecursoNaoEncontrado() {
        RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("Produto não encontrado");

        ResponseEntity<ErrorResponse> response = handler.handleRecursoNaoEncontrado(ex, webRequest);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Produto não encontrado", body.getMensagem());
        assertEquals("/api/teste", body.getCaminho());
    }

    @Test
    @DisplayName("Deve tratar erros de validacao como 400")
    void testHandleValidationExceptions() {
        FieldError fieldError = new FieldError("produtoDTO", "nome", "não pode estar em branco");
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response =
            handler.handleValidationExceptions(methodArgumentNotValidException, webRequest);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertNotNull(body.getDetalhes());
        assertEquals("não pode estar em branco", body.getDetalhes().get("nome"));
    }

    @Test
    @DisplayName("Deve tratar IllegalArgumentException como 400")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Email já cadastrado");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, webRequest);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Email já cadastrado", body.getMensagem());
    }

    @Test
    @DisplayName("Deve tratar excecao generica como 500")
    void testHandleGlobalException() {
        Exception ex = new RuntimeException("Algo inesperado aconteceu");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, webRequest);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Ocorreu um erro inesperado", body.getMensagem());
    }
}
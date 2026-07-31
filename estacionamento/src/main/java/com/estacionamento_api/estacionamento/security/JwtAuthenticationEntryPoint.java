package com.estacionamento_api.estacionamento.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sem isso, uma requisição sem token (ou com token inválido) pra um endpoint
 * protegido recebe a resposta padrão do Spring Security, que não segue o
 * mesmo formato JSON usado pelo GlobalExceptionHandler no resto da API.
 * O Spring Security intercepta antes do @RestControllerAdvice, então
 * precisa desse componente específico para manter a resposta consistente.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Acesso não autenticado a {}: {}", request.getRequestURI(), authException.getMessage());

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("erro", "Não autenticado");
        body.put("mensagem", "É necessário informar um token JWT válido no header Authorization");

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

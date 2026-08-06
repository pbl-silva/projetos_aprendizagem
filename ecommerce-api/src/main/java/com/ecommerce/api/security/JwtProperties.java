package com.ecommerce.api.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
@Data
public class JwtProperties {
    
    /**
     * Chave secreta para assinar tokens JWT
     */
    @NotBlank
    @Size(min = 32)
    private String secret;
    
    /**
     * Tempo de expiração do token em milissegundos (padrão: 24 horas)
     */
    private Long expiration = 86400000L;
    
    /**
     * Tempo de expiração do refresh token em milissegundos (padrão: 7 dias)
     */
    private Long refreshExpiration = 604800000L;
}

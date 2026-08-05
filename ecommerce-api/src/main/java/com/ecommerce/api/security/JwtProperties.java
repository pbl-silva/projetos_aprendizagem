package com.ecommerce.api.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    
    /**
     * Chave secreta para assinar tokens JWT
     */
    private String secret = "sua-chave-secreta-super-segura-aqui-com-minimo-32-caracteres";
    
    /**
     * Tempo de expiração do token em milissegundos (padrão: 24 horas)
     */
    private Long expiration = 86400000L;
    
    /**
     * Tempo de expiração do refresh token em milissegundos (padrão: 7 dias)
     */
    private Long refreshExpiration = 604800000L;
}
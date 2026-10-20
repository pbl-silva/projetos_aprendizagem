package com.estacionamento_api.estacionamento.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

/**
 * Geração e validação de tokens JWT (biblioteca JJWT 0.13 — API fluente,
 * não a antiga baseada em setSubject()/setSigningKey(), que foi removida
 * nessa versão).
 */
@Slf4j
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiracao-ms}")
    private long expiracaoMs;

    private SecretKey signingKey;

    @PostConstruct
    void inicializarChave() {
        if (secret == null || secret.isBlank()) {
            byte[] chaveAleatoria = new byte[32];
            new SecureRandom().nextBytes(chaveAleatoria);
            signingKey = Keys.hmacShaKeyFor(chaveAleatoria);
            log.warn("JWT_SECRET não informado; foi criada uma chave temporária para esta execução");
            return;
        }
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getKey() {
        return signingKey;
    }

    public String gerarToken(String username) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
            .subject(username)
            .issuedAt(agora)
            .expiration(expiracao)
            .signWith(getKey())
            .compact();
    }

    public String extrairUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean tokenValido(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido ou expirado: {}", e.getMessage());
            return false;
        }
    }

    public long getExpiracaoSegundos() {
        return expiracaoMs / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

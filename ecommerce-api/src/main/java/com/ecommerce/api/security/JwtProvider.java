package com.ecommerce.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import com.ecommerce.api.model.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {
    
    private final JwtProperties jwtProperties;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Gera token JWT
     */
    public String generateToken(String email, Long userId, Usuario.Papel papel) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
        
        log.debug("Gerando token JWT para usuário: {}", email);
        
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("papel", papel.name())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    public @Nullable String getPapelFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return claims.get("papel", String.class);
        } catch (Exception e) {
            log.error("Erro ao extrair papel do token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrai email do token
     */
    public @Nullable String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Erro ao extrair email do token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrai userId do token
     */
    public @Nullable Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.error("Erro ao extrair userId do token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Valida token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            
            log.debug("Token JWT válido");
            return true;
        } catch (Exception e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }
}

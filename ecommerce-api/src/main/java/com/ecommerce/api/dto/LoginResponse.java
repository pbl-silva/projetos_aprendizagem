package com.ecommerce.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private String tipo;
    private Long id;
    private String email;
    private String nome;
    
    public static LoginResponse of(String token, Long id, String email, String nome) {
        return LoginResponse.builder()
            .token(token)
            .tipo("Bearer")
            .id(id)
            .email(email)
            .nome(nome)
            .build();
    }
}
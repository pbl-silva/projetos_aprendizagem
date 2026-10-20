package com.estacionamento_api.estacionamento.dto;

import java.time.LocalDateTime;

public record UsuarioDTO(Long id, String username, String role, LocalDateTime dataCriacao,
                         String nome, String email, String telefone) {
}

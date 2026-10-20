package com.estacionamento_api.estacionamento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizacaoContaDTO(
    @NotBlank @Size(min = 3, max = 50) String username,
    @Size(max = 100) String nome,
    @Email(message = "Informe um e-mail válido") @Size(max = 254) String email,
    @Size(max = 30) String telefone,
    @NotBlank(message = "Informe sua senha atual para confirmar") @Size(max = 72) String senhaAtual
) {
}

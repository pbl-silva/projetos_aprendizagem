package com.estacionamento_api.estacionamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlteracaoSenhaDTO(
    @NotBlank @Size(max = 72) String senhaAtual,
    @NotBlank @Size(min = 6, max = 72) String novaSenha,
    @NotBlank @Size(min = 6, max = 72) String confirmacaoSenha
) {
}

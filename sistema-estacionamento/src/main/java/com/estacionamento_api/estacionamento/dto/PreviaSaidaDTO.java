package com.estacionamento_api.estacionamento.dto;

import java.math.BigDecimal;

public record PreviaSaidaDTO(BigDecimal valorBase, BigDecimal desconto, BigDecimal valorFinal) {
}

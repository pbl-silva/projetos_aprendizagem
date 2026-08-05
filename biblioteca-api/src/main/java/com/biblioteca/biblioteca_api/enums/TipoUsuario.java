package com.biblioteca.biblioteca_api.enums;

import com.biblioteca.biblioteca_api.entities.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
public enum TipoUsuario {

    COMUM(3, 7, 0),
    PREMIUM(5, 14, 5);

    private final int limiteEmprestimos;
    private final int prazoDias;
    private final int diasToleranciaMulta;

    public int getMaximoEmprestimos() {
        return limiteEmprestimos;
    }

    public int getDiasPrazo() {
        return prazoDias;
    }

    public int getDiasTolerancia() {
        return diasToleranciaMulta;
    }

    private LocalDate calcularDataDevolucaoPrevista(Usuario usuario) {
        return LocalDate.now().plusDays(usuario.getTipoUsuario().getDiasPrazo());
    }
}

package com.biblioteca.biblioteca_api.validators;

import com.biblioteca.biblioteca_api.entities.Emprestimo;
import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import com.biblioteca.biblioteca_api.exceptions.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class EmprestimoValidator {

    public void validarLivroDisponivel(Livro livro) {
        if (livro == null) {
            throw new BusinessException("Livro não encontrado.");
        }
        if (!Boolean.TRUE.equals(livro.getDisponivel())) {
            throw new BusinessException("Livro não está disponível para empréstimo.");
        }
    }

    public void validarLimiteEmprestimos(Usuario usuario, long emprestimosAtivos) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            throw new BusinessException("Usuário inválido.");
        }
        int limiteMaximo = usuario.getTipoUsuario().getMaximoEmprestimos();
        if (emprestimosAtivos >= limiteMaximo) {
            throw new BusinessException(
                    String.format("Usuário atingiu o limite de %d empréstimos simultâneos", limiteMaximo)
            );
        }
    }

    public void validarEmprestimoAtivo(Emprestimo emprestimo) {
        if (emprestimo == null) {
            throw new BusinessException("Empréstimo não encontrado.");
        }
        if (emprestimo.getStatus() != StatusEmprestimo.ATIVO) {
            throw new BusinessException("Empréstimo não está ativo.");
        }
    }
}
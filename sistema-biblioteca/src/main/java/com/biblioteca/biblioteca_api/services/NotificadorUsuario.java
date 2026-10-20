package com.biblioteca.biblioteca_api.services;

import com.biblioteca.biblioteca_api.entities.Usuario;

import java.math.BigDecimal;

public interface NotificadorUsuario {

    void enviarEmailBoasVindas(Usuario usuario);

    void enviarLembreteDevolucao(Usuario usuario, String tituloLivro, int diasRestantes);

    void enviarNotificacaoAtraso(Usuario usuario, String tituloLivro, int diasAtraso, BigDecimal valorMulta);

    void enviarConfirmacaoDevolucao(Usuario usuario, String tituloLivro);
}
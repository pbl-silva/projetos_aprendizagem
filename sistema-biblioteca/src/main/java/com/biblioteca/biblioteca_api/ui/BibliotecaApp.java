package com.biblioteca.biblioteca_api.ui;

import com.biblioteca.biblioteca_api.ui.client.ApiClient;
import com.biblioteca.biblioteca_api.ui.telas.*;

import javax.swing.*;
import java.awt.*;

public class BibliotecaApp extends JFrame {

    private ApiClient apiClient;

    public BibliotecaApp() {
        // Inicializar ApiClient
        apiClient = new ApiClient("http://localhost:8080");

        // Configurações da janela principal
        setTitle("Sistema de Biblioteca - Gestão Completa");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Inicializar componentes
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        // Painel principal
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Título
        JLabel lblTitulo = new JLabel("Sistema de Biblioteca", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(0, 102, 204));
        painelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        // Painel de botões
        JPanel painelBotoes = new JPanel(new GridLayout(6, 1, 10, 10));
        painelBotoes.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        JButton btnLivros = new JButton("Gestão de Livros");
        JButton btnUsuarios = new JButton("Gestão de Usuários");
        JButton btnNovoEmprestimo = new JButton("Novo Empréstimo");
        JButton btnDevolucao = new JButton("Devolução de Livros");
        JButton btnRelatorios = new JButton("Relatórios");
        JButton btnSair = new JButton("Sair");

        // Estilizar botões
        estilizarBotao(btnLivros, new Color(70, 130, 180));
        estilizarBotao(btnUsuarios, new Color(60, 179, 113));
        estilizarBotao(btnNovoEmprestimo, new Color(255, 140, 0));
        estilizarBotao(btnDevolucao, new Color(220, 20, 60));
        estilizarBotao(btnRelatorios, new Color(138, 43, 226));
        estilizarBotao(btnSair, new Color(105, 105, 105));

        // Ações dos botões
        btnLivros.addActionListener(e -> abrirTelaLivros());
        btnUsuarios.addActionListener(e -> abrirTelaUsuarios());
        btnNovoEmprestimo.addActionListener(e -> abrirTelaNovoEmprestimo());
        btnDevolucao.addActionListener(e -> abrirTelaDevolucao());
        btnRelatorios.addActionListener(e -> abrirTelaRelatorios());
        btnSair.addActionListener(e -> confirmarESair());

        painelBotoes.add(btnLivros);
        painelBotoes.add(btnUsuarios);
        painelBotoes.add(btnNovoEmprestimo);
        painelBotoes.add(btnDevolucao);
        painelBotoes.add(btnRelatorios);
        painelBotoes.add(btnSair);

        painelPrincipal.add(painelBotoes, BorderLayout.CENTER);

        // Rodapé
        JLabel lblRodape = new JLabel("Versão 2.0 - API REST Integration | © 2024", SwingConstants.CENTER);
        lblRodape.setFont(new Font("Arial", Font.ITALIC, 11));
        lblRodape.setForeground(Color.GRAY);
        painelPrincipal.add(lblRodape, BorderLayout.SOUTH);

        add(painelPrincipal);
    }

    private void estilizarBotao(JButton botao, Color cor) {
        botao.setFont(new Font("Arial", Font.BOLD, 16));
        botao.setBackground(cor);
        botao.setForeground(Color.WHITE);
        botao.setFocusPainted(false);
        botao.setBorderPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efeito hover
        botao.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                botao.setBackground(cor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                botao.setBackground(cor);
            }
        });
    }

    private void confirmarESair() {
        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente sair do sistema?",
                "Confirmar Saída",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (resposta == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    private void abrirTelaLivros() {
        TelaLivros tela = new TelaLivros(apiClient);
        tela.setVisible(true);
    }

    private void abrirTelaUsuarios() {
        TelaUsuarios tela = new TelaUsuarios(apiClient);
        tela.setVisible(true);
    }

    private void abrirTelaNovoEmprestimo() {
        TelaNovoEmprestimo tela = new TelaNovoEmprestimo(apiClient);
        tela.setVisible(true);
    }

    private void abrirTelaDevolucao() {
        TelaDevolucao tela = new TelaDevolucao(apiClient);
        tela.setVisible(true);
    }

    private void abrirTelaRelatorios() {
        TelaRelatorios tela = new TelaRelatorios(apiClient);
        tela.setVisible(true);
    }

    public static void main(String[] args) {
        // Configurar Look and Feel do sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Executar aplicação na thread do Swing
        SwingUtilities.invokeLater(() -> {
            BibliotecaApp app = new BibliotecaApp();
            app.setVisible(true);
        });
    }
}
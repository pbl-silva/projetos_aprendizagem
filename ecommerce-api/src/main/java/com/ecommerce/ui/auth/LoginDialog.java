package com.ecommerce.ui.auth;

import com.ecommerce.ui.service.ApiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Tela de login exibida antes da janela principal. Sem autenticar aqui,
 * nenhuma chamada à API funciona (todas as rotas, exceto /auth/**, exigem
 * um token JWT válido).
 */
public class LoginDialog extends JDialog {

    private boolean autenticado = false;

    private final JTextField campoEmail = new JTextField(20);
    private final JPasswordField campoSenha = new JPasswordField(20);

    public LoginDialog(Frame owner) {
        super(owner, "Login - E-Commerce API", true);
        montarInterface();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void montarInterface() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        painel.add(new JLabel("Entre com sua conta para continuar"), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        painel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        painel.add(campoEmail, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        painel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        painel.add(campoSenha, gbc);

        JButton botaoEntrar = new JButton("Entrar");
        JButton botaoRegistrar = new JButton("Criar conta");
        JButton botaoSair = new JButton("Sair");

        botaoEntrar.addActionListener(e -> tentarLogin());
        botaoRegistrar.addActionListener(e -> abrirTelaDeRegistro());
        botaoSair.addActionListener(e -> {
            autenticado = false;
            dispose();
        });

        // Enter no campo de senha também tenta logar
        campoSenha.addActionListener(e -> tentarLogin());

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelBotoes.add(botaoEntrar);
        painelBotoes.add(botaoRegistrar);
        painelBotoes.add(botaoSair);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        painel.add(painelBotoes, gbc);

        setContentPane(painel);
    }

    private void tentarLogin() {
        String email = campoEmail.getText().trim();
        String senha = new String(campoSenha.getPassword());

        if (email.isBlank() || senha.isBlank()) {
            JOptionPane.showMessageDialog(this,
                "Preencha email e senha.", "Campos obrigatórios",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String erro = ApiClient.login(email, senha);
        if (erro == null) {
            autenticado = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, erro, "Falha no login",
                JOptionPane.ERROR_MESSAGE);
            campoSenha.setText("");
        }
    }

    private void abrirTelaDeRegistro() {
        JTextField novoNome = new JTextField(20);
        JTextField novoEmail = new JTextField(20);
        JPasswordField novaSenha = new JPasswordField(20);

        JPanel painelRegistro = new JPanel(new GridLayout(3, 2, 5, 5));
        painelRegistro.add(new JLabel("Nome:"));
        painelRegistro.add(novoNome);
        painelRegistro.add(new JLabel("Email:"));
        painelRegistro.add(novoEmail);
        painelRegistro.add(new JLabel("Senha (mín. 6 caracteres):"));
        painelRegistro.add(novaSenha);

        int opcao = JOptionPane.showConfirmDialog(this, painelRegistro,
            "Criar nova conta", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (opcao != JOptionPane.OK_OPTION) {
            return;
        }

        String nome = novoNome.getText().trim();
        String email = novoEmail.getText().trim();
        String senha = new String(novaSenha.getPassword());

        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            JOptionPane.showMessageDialog(this,
                "Preencha todos os campos.", "Campos obrigatórios",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String erro = ApiClient.registrar(email, senha, nome);
        if (erro == null) {
            JOptionPane.showMessageDialog(this,
                "Conta criada com sucesso! Agora entre com seu email e senha.",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            campoEmail.setText(email);
            campoSenha.setText("");
        } else {
            JOptionPane.showMessageDialog(this, erro, "Falha no registro",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Exibe o diálogo de login (bloqueante) e retorna true se o usuário
     * autenticou com sucesso, ou false se cancelou/fechou a janela.
     */
    public static boolean autenticarUsuario(Frame owner) {
        LoginDialog dialog = new LoginDialog(owner);
        dialog.setVisible(true);
        return dialog.autenticado;
    }
}
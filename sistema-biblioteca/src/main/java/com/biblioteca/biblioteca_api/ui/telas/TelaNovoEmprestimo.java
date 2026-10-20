package com.biblioteca.biblioteca_api.ui.telas;

import com.biblioteca.biblioteca_api.ui.client.ApiClient;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaNovoEmprestimo extends JFrame {

    private ApiClient apiClient;
    private JTable tabelaUsuarios;
    private JTable tabelaLivros;
    private DefaultTableModel modeloUsuarios;
    private DefaultTableModel modeloLivros;

    public TelaNovoEmprestimo(ApiClient apiClient) {
        this.apiClient = apiClient;

        setTitle("Novo Empréstimo");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        carregarDados();
    }

    private void initComponents() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Painel dividido
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Painel Usuários
        JPanel painelUsuarios = new JPanel(new BorderLayout());
        painelUsuarios.setBorder(BorderFactory.createTitledBorder("Selecione o Usuário"));

        String[] colunasUsuarios = {"ID", "Nome", "Email", "Tipo"};
        modeloUsuarios = new DefaultTableModel(colunasUsuarios, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaUsuarios = new JTable(modeloUsuarios);
        tabelaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollUsuarios = new JScrollPane(tabelaUsuarios);
        painelUsuarios.add(scrollUsuarios, BorderLayout.CENTER);

        // Painel Livros
        JPanel painelLivros = new JPanel(new BorderLayout());
        painelLivros.setBorder(BorderFactory.createTitledBorder("Selecione o Livro"));

        String[] colunasLivros = {"ID", "Título", "Autor", "Disponível"};
        modeloLivros = new DefaultTableModel(colunasLivros, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaLivros = new JTable(modeloLivros);
        tabelaLivros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollLivros = new JScrollPane(tabelaLivros);
        painelLivros.add(scrollLivros, BorderLayout.CENTER);

        splitPane.setLeftComponent(painelUsuarios);
        splitPane.setRightComponent(painelLivros);
        splitPane.setDividerLocation(450);

        // Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnConfirmar = new JButton("Confirmar Empréstimo");
        JButton btnCancelar = new JButton("Cancelar");

        btnConfirmar.addActionListener(e -> realizarEmprestimo());
        btnCancelar.addActionListener(e -> dispose());

        painelBotoes.add(btnConfirmar);
        painelBotoes.add(btnCancelar);

        painelPrincipal.add(splitPane, BorderLayout.CENTER);
        painelPrincipal.add(painelBotoes, BorderLayout.SOUTH);

        add(painelPrincipal);
    }

    private void carregarDados() {
        carregarUsuarios();
        carregarLivros();
    }

    private void carregarUsuarios() {
        try {
            List<ApiClient.Usuario> usuarios = apiClient.listarUsuarios();
            modeloUsuarios.setRowCount(0);

            for (ApiClient.Usuario usuario : usuarios) {
                Object[] linha = {
                        usuario.id,
                        usuario.nome,
                        usuario.email,
                        usuario.tipoUsuario
                };
                modeloUsuarios.addRow(linha);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar usuários: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarLivros() {
        try {
            List<ApiClient.Livro> livros = apiClient.listarLivros();
            modeloLivros.setRowCount(0);

            for (ApiClient.Livro livro : livros) {
                if (livro.disponivel != null && livro.disponivel) {
                    Object[] linha = {
                            livro.id,
                            livro.titulo,
                            livro.autor,
                            "Sim"
                    };
                    modeloLivros.addRow(linha);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar livros: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void realizarEmprestimo() {
        int linhaUsuario = tabelaUsuarios.getSelectedRow();
        int linhaLivro = tabelaLivros.getSelectedRow();

        if (linhaUsuario < 0 || linhaLivro < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um usuário e um livro para realizar o empréstimo!");
            return;
        }

        try {
            Long usuarioId = getIdFromModel(modeloUsuarios, linhaUsuario);
            Long livroId = getIdFromModel(modeloLivros, linhaLivro);

            if (usuarioId == null || livroId == null) throw new Exception("ID inválido.");

            apiClient.realizarEmprestimo(usuarioId, livroId);
            JOptionPane.showMessageDialog(this, "Empréstimo realizado com sucesso!");
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao realizar empréstimo: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Long getIdFromModel(DefaultTableModel model, int row) {
        Object idObj = model.getValueAt(row, 0);
        if (idObj == null) return null;
        if (idObj instanceof Number) return ((Number) idObj).longValue();
        try {
            return Long.parseLong(idObj.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
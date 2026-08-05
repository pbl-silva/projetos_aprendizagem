package com.biblioteca.biblioteca_api.ui.telas;

import com.biblioteca.biblioteca_api.ui.client.ApiClient;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaUsuarios extends JFrame {

    private ApiClient apiClient;
    private JTable tabelaUsuarios;
    private DefaultTableModel modeloTabela;

    private JTextField txtNome;
    private JTextField txtEmail;
    private JTextField txtTelefone;
    private JComboBox<String> cmbTipoUsuario;

    public TelaUsuarios(ApiClient apiClient) {
        this.apiClient = apiClient;

        setTitle("Gestão de Usuários");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        carregarUsuarios();
    }

    private void initComponents() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Painel de Formulário
        JPanel painelFormulario = new JPanel(new GridLayout(5, 2, 5, 5));
        painelFormulario.setBorder(BorderFactory.createTitledBorder("Dados do Usuário"));

        painelFormulario.add(new JLabel("Nome:"));
        txtNome = new JTextField();
        painelFormulario.add(txtNome);

        painelFormulario.add(new JLabel("Email:"));
        txtEmail = new JTextField();
        painelFormulario.add(txtEmail);

        painelFormulario.add(new JLabel("Telefone:"));
        txtTelefone = new JTextField();
        painelFormulario.add(txtTelefone);

        painelFormulario.add(new JLabel("Tipo de Usuário:"));
        cmbTipoUsuario = new JComboBox<>(new String[]{"ESTUDANTE", "PROFESSOR", "FUNCIONARIO"});
        painelFormulario.add(cmbTipoUsuario);

        // Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNovo = new JButton("Novo");
        JButton btnSalvar = new JButton("Salvar");
        JButton btnAtualizar = new JButton("Atualizar");
        JButton btnDeletar = new JButton("Deletar");
        JButton btnLimpar = new JButton("Limpar");

        btnNovo.addActionListener(e -> limparFormulario());
        btnSalvar.addActionListener(e -> salvarUsuario());
        btnAtualizar.addActionListener(e -> atualizarUsuario());
        btnDeletar.addActionListener(e -> deletarUsuario());
        btnLimpar.addActionListener(e -> limparFormulario());

        painelBotoes.add(btnNovo);
        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnAtualizar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnLimpar);

        painelFormulario.add(painelBotoes);

        // Painel da Tabela
        String[] colunas = {"ID", "Nome", "Email", "Telefone", "Tipo"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaUsuarios = new JTable(modeloTabela);
        tabelaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                carregarUsuarioSelecionado();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabelaUsuarios);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista de Usuários"));

        painelPrincipal.add(painelFormulario, BorderLayout.NORTH);
        painelPrincipal.add(scrollPane, BorderLayout.CENTER);

        add(painelPrincipal);
    }

    private void carregarUsuarios() {
        try {
            List<ApiClient.Usuario> usuarios = apiClient.listarUsuarios();
            modeloTabela.setRowCount(0);

            for (ApiClient.Usuario usuario : usuarios) {
                Object[] linha = {
                        usuario.id,
                        usuario.nome,
                        usuario.email,
                        usuario.telefone,
                        usuario.tipoUsuario
                };
                modeloTabela.addRow(linha);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar usuários: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarUsuarioSelecionado() {
        int linhaSelecionada = tabelaUsuarios.getSelectedRow();
        if (linhaSelecionada >= 0) {
            Object nome = modeloTabela.getValueAt(linhaSelecionada, 1);
            Object email = modeloTabela.getValueAt(linhaSelecionada, 2);
            Object telefone = modeloTabela.getValueAt(linhaSelecionada, 3);
            Object tipo = modeloTabela.getValueAt(linhaSelecionada, 4);

            txtNome.setText(nome != null ? nome.toString() : "");
            txtEmail.setText(email != null ? email.toString() : "");
            txtTelefone.setText(telefone != null ? telefone.toString() : "");
            cmbTipoUsuario.setSelectedItem(tipo != null ? tipo.toString() : "ESTUDANTE");
        }
    }

    private void salvarUsuario() {
        try {
            ApiClient.Usuario usuario = new ApiClient.Usuario();
            usuario.nome = txtNome.getText();
            usuario.email = txtEmail.getText();
            usuario.telefone = txtTelefone.getText();
            usuario.tipoUsuario = cmbTipoUsuario.getSelectedItem().toString();

            apiClient.salvarUsuario(usuario);
            JOptionPane.showMessageDialog(this, "Usuário salvo com sucesso!");
            limparFormulario();
            carregarUsuarios();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar usuário: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarUsuario() {
        int linhaSelecionada = tabelaUsuarios.getSelectedRow();
        if (linhaSelecionada < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário para atualizar!");
            return;
        }

        try {
            Long id = getIdFromModel(modeloTabela, linhaSelecionada);
            if (id == null) throw new Exception("ID do usuário inválido.");

            ApiClient.Usuario usuario = new ApiClient.Usuario();
            usuario.nome = txtNome.getText();
            usuario.email = txtEmail.getText();
            usuario.telefone = txtTelefone.getText();
            usuario.tipoUsuario = cmbTipoUsuario.getSelectedItem().toString();

            apiClient.atualizarUsuario(id, usuario);
            JOptionPane.showMessageDialog(this, "Usuário atualizado com sucesso!");
            limparFormulario();
            carregarUsuarios();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar usuário: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletarUsuario() {
        int linhaSelecionada = tabelaUsuarios.getSelectedRow();
        if (linhaSelecionada < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário para deletar!");
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja deletar este usuário?",
                "Confirmação", JOptionPane.YES_NO_OPTION);

        if (confirmacao == JOptionPane.YES_OPTION) {
            try {
                Long id = getIdFromModel(modeloTabela, linhaSelecionada);
                if (id == null) throw new Exception("ID do usuário inválido.");

                apiClient.deletarUsuario(id);
                JOptionPane.showMessageDialog(this, "Usuário deletado com sucesso!");
                limparFormulario();
                carregarUsuarios();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao deletar usuário: " + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limparFormulario() {
        txtNome.setText("");
        txtEmail.setText("");
        txtTelefone.setText("");
        cmbTipoUsuario.setSelectedIndex(0);
        tabelaUsuarios.clearSelection();
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
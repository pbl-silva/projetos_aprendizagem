package com.biblioteca.biblioteca_api.ui.telas;

import com.biblioteca.biblioteca_api.ui.client.ApiClient;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaLivros extends JFrame {

    private ApiClient apiClient;
    private JTable tabelaLivros;
    private DefaultTableModel modeloTabela;

    private JTextField txtTitulo;
    private JTextField txtAutor;
    private JTextField txtIsbn;
    private JTextField txtCategoria;
    private JTextField txtAno;
    private JCheckBox chkDisponivel;

    public TelaLivros(ApiClient apiClient) {
        this.apiClient = apiClient;

        setTitle("Gestão de Livros");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        carregarLivros();
    }

    private void initComponents() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Painel de Formulário
        JPanel painelFormulario = new JPanel(new GridLayout(7, 2, 5, 5));
        painelFormulario.setBorder(BorderFactory.createTitledBorder("Dados do Livro"));

        painelFormulario.add(new JLabel("Título:"));
        txtTitulo = new JTextField();
        painelFormulario.add(txtTitulo);

        painelFormulario.add(new JLabel("Autor:"));
        txtAutor = new JTextField();
        painelFormulario.add(txtAutor);

        painelFormulario.add(new JLabel("ISBN:"));
        txtIsbn = new JTextField();
        painelFormulario.add(txtIsbn);

        painelFormulario.add(new JLabel("Categoria:"));
        txtCategoria = new JTextField();
        painelFormulario.add(txtCategoria);

        painelFormulario.add(new JLabel("Ano de Publicação:"));
        txtAno = new JTextField();
        painelFormulario.add(txtAno);

        painelFormulario.add(new JLabel("Disponível:"));
        chkDisponivel = new JCheckBox();
        chkDisponivel.setSelected(true);
        painelFormulario.add(chkDisponivel);

        // Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNovo = new JButton("Novo");
        JButton btnSalvar = new JButton("Salvar");
        JButton btnAtualizar = new JButton("Atualizar");
        JButton btnDeletar = new JButton("Deletar");
        JButton btnLimpar = new JButton("Limpar");

        btnNovo.addActionListener(e -> limparFormulario());
        btnSalvar.addActionListener(e -> salvarLivro());
        btnAtualizar.addActionListener(e -> atualizarLivro());
        btnDeletar.addActionListener(e -> deletarLivro());
        btnLimpar.addActionListener(e -> limparFormulario());

        painelBotoes.add(btnNovo);
        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnAtualizar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnLimpar);

        painelFormulario.add(painelBotoes);

        // Painel da Tabela
        String[] colunas = {"ID", "Título", "Autor", "ISBN", "Categoria", "Ano", "Disponível"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaLivros = new JTable(modeloTabela);
        tabelaLivros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaLivros.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                carregarLivroSelecionado();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabelaLivros);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista de Livros"));

        painelPrincipal.add(painelFormulario, BorderLayout.NORTH);
        painelPrincipal.add(scrollPane, BorderLayout.CENTER);

        add(painelPrincipal);
    }

    private void carregarLivros() {
        try {
            List<ApiClient.Livro> livros = apiClient.listarLivros();
            modeloTabela.setRowCount(0);

            for (ApiClient.Livro livro : livros) {
                Object[] linha = {
                        livro.id,
                        livro.titulo,
                        livro.autor,
                        livro.isbn,
                        livro.categoria,
                        livro.anoPublicacao,
                        livro.disponivel ? "Sim" : "Não"
                };
                modeloTabela.addRow(linha);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar livros: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarLivroSelecionado() {
        int linhaSelecionada = tabelaLivros.getSelectedRow();
        if (linhaSelecionada >= 0) {
            Object titulo = modeloTabela.getValueAt(linhaSelecionada, 1);
            Object autor = modeloTabela.getValueAt(linhaSelecionada, 2);
            Object isbn = modeloTabela.getValueAt(linhaSelecionada, 3);
            Object categoria = modeloTabela.getValueAt(linhaSelecionada, 4);
            Object ano = modeloTabela.getValueAt(linhaSelecionada, 5);
            Object disponivel = modeloTabela.getValueAt(linhaSelecionada, 6);

            txtTitulo.setText(titulo != null ? titulo.toString() : "");
            txtAutor.setText(autor != null ? autor.toString() : "");
            txtIsbn.setText(isbn != null ? isbn.toString() : "");
            txtCategoria.setText(categoria != null ? categoria.toString() : "");
            txtAno.setText(ano != null ? ano.toString() : "");
            chkDisponivel.setSelected(disponivel != null && "Sim".equals(disponivel.toString()));
        }
    }

    private void salvarLivro() {
        try {
            ApiClient.Livro livro = new ApiClient.Livro();
            livro.titulo = txtTitulo.getText();
            livro.autor = txtAutor.getText();
            livro.isbn = txtIsbn.getText();
            livro.categoria = txtCategoria.getText();
            livro.anoPublicacao = txtAno.getText().isBlank() ? null : Integer.parseInt(txtAno.getText());
            livro.disponivel = chkDisponivel.isSelected();

            apiClient.salvarLivro(livro);
            JOptionPane.showMessageDialog(this, "Livro salvo com sucesso!");
            limparFormulario();
            carregarLivros();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar livro: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarLivro() {
        int linhaSelecionada = tabelaLivros.getSelectedRow();
        if (linhaSelecionada < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um livro para atualizar!");
            return;
        }

        try {
            Long id = getIdFromModel(modeloTabela, linhaSelecionada);
            if (id == null) throw new Exception("ID do livro inválido.");

            ApiClient.Livro livro = new ApiClient.Livro();
            livro.titulo = txtTitulo.getText();
            livro.autor = txtAutor.getText();
            livro.isbn = txtIsbn.getText();
            livro.categoria = txtCategoria.getText();
            livro.anoPublicacao = txtAno.getText().isBlank() ? null : Integer.parseInt(txtAno.getText());
            livro.disponivel = chkDisponivel.isSelected();

            apiClient.atualizarLivro(id, livro);
            JOptionPane.showMessageDialog(this, "Livro atualizado com sucesso!");
            limparFormulario();
            carregarLivros();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar livro: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletarLivro() {
        int linhaSelecionada = tabelaLivros.getSelectedRow();
        if (linhaSelecionada < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um livro para deletar!");
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja deletar este livro?",
                "Confirmação", JOptionPane.YES_NO_OPTION);

        if (confirmacao == JOptionPane.YES_OPTION) {
            try {
                Long id = getIdFromModel(modeloTabela, linhaSelecionada);
                if (id == null) throw new Exception("ID do livro inválido.");

                apiClient.deletarLivro(id);
                JOptionPane.showMessageDialog(this, "Livro deletado com sucesso!");
                limparFormulario();
                carregarLivros();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao deletar livro: " + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limparFormulario() {
        txtTitulo.setText("");
        txtAutor.setText("");
        txtIsbn.setText("");
        txtCategoria.setText("");
        txtAno.setText("");
        chkDisponivel.setSelected(true);
        tabelaLivros.clearSelection();
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
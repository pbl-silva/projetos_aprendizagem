package com.biblioteca.biblioteca_api.ui.telas;

import com.biblioteca.biblioteca_api.ui.client.ApiClient;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaDevolucao extends JFrame {

    private ApiClient apiClient;
    private JTable tabelaEmprestimos;
    private DefaultTableModel modeloTabela;

    public TelaDevolucao(ApiClient apiClient) {
        this.apiClient = apiClient;

        setTitle("Devolução de Livros");
        setSize(1000, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        carregarEmprestimosAtivos();
    }

    private void initComponents() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tabela
        String[] colunas = {"ID", "Usuário", "Livro", "Data Empréstimo", "Previsão Devolução", "Status"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaEmprestimos = new JTable(modeloTabela);
        tabelaEmprestimos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tabelaEmprestimos);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Empréstimos Ativos"));

        // Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnDevolver = new JButton("Realizar Devolução");
        JButton btnAtualizar = new JButton("Atualizar Lista");
        JButton btnFechar = new JButton("Fechar");

        btnDevolver.addActionListener(e -> realizarDevolucao());
        btnAtualizar.addActionListener(e -> carregarEmprestimosAtivos());
        btnFechar.addActionListener(e -> dispose());

        painelBotoes.add(btnDevolver);
        painelBotoes.add(btnAtualizar);
        painelBotoes.add(btnFechar);

        painelPrincipal.add(scrollPane, BorderLayout.CENTER);
        painelPrincipal.add(painelBotoes, BorderLayout.SOUTH);

        add(painelPrincipal);
    }

    private void carregarEmprestimosAtivos() {
        try {
            List<ApiClient.Emprestimo> emprestimos = apiClient.listarEmprestimosAtivos();
            modeloTabela.setRowCount(0);

            for (ApiClient.Emprestimo emp : emprestimos) {
                Object[] linha = {
                        emp.id,
                        emp.usuario != null ? emp.usuario.nome : "",
                        emp.livro != null ? emp.livro.titulo : "",
                        emp.dataEmprestimo,
                        emp.dataDevolucaoPrevista,
                        emp.status
                };
                modeloTabela.addRow(linha);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar empréstimos: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void realizarDevolucao() {
        int linhaSelecionada = tabelaEmprestimos.getSelectedRow();
        if (linhaSelecionada < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um empréstimo para realizar a devolução!");
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(this,
                "Confirmar devolução do livro?",
                "Confirmação", JOptionPane.YES_NO_OPTION);

        if (confirmacao == JOptionPane.YES_OPTION) {
            try {
                Long emprestimoId = getIdFromModel(modeloTabela, linhaSelecionada);
                if (emprestimoId == null) throw new Exception("ID do empréstimo inválido.");

                apiClient.realizarDevolucao(emprestimoId);
                JOptionPane.showMessageDialog(this, "Devolução realizada com sucesso!");
                carregarEmprestimosAtivos();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao realizar devolução: " + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
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

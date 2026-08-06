package com.ecommerce.ui.panels;

import com.ecommerce.ui.service.ApiClient;
import com.ecommerce.ui.util.MapUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProdutoPanel extends JPanel {

    private JTable tabelaProdutos;

    public ProdutoPanel() {
        initComponents();
        carregarProdutos();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        JPanel painelBotoes = new JPanel();
        JButton btnAdicionar = new JButton("Adicionar");
        JButton btnEditar = new JButton("Editar");
        JButton btnDeletar = new JButton("Deletar");
        JButton btnAtualizar = new JButton("Atualizar");
        btnAdicionar.addActionListener(e -> adicionarProduto());
        btnEditar.addActionListener(e -> editarProduto());
        btnDeletar.addActionListener(e -> deletarProduto());
        btnAtualizar.addActionListener(e -> carregarProdutos());
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnAtualizar);
        tabelaProdutos = new JTable();
        add(painelBotoes, BorderLayout.NORTH);
        add(new JScrollPane(tabelaProdutos), BorderLayout.CENTER);
    }

    private void carregarProdutos() {
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nome", "Preço", "Estoque", "Categoria", "Ativo"}, 0);
        for (Object produto : ApiClient.getProdutos()) {
            Map<String, Object> p = MapUtils.toMap(produto);
            model.addRow(new Object[]{
                MapUtils.getAsLong(p, "id"), MapUtils.getAsString(p, "nome"),
                MapUtils.getAsBigDecimal(p, "preco"), MapUtils.getAsInteger(p, "estoque"),
                MapUtils.getAsLong(p, "categoriaId"), MapUtils.getAsBoolean(p, "ativo")
            });
        }
        tabelaProdutos.setModel(model);
    }

    private void adicionarProduto() {
        List<CategoriaOpcao> categorias = carregarCategoriasAtivas();
        if (categorias.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre uma categoria ativa antes de criar um produto.",
                "Categorias indisponíveis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField nomeField = new JTextField();
        JTextField precoField = new JTextField();
        JTextField estoqueField = new JTextField();
        JComboBox<CategoriaOpcao> categoriaCombo = new JComboBox<>(categorias.toArray(new CategoriaOpcao[0]));
        JTextArea descricaoArea = new JTextArea(2, 20);
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        panel.add(new JLabel("Nome:")); panel.add(nomeField);
        panel.add(new JLabel("Preço:")); panel.add(precoField);
        panel.add(new JLabel("Estoque:")); panel.add(estoqueField);
        panel.add(new JLabel("Categoria:")); panel.add(categoriaCombo);
        panel.add(new JLabel("Descrição:")); panel.add(new JScrollPane(descricaoArea));

        if (JOptionPane.showConfirmDialog(this, panel, "Adicionar Produto", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        if (nomeField.getText().isBlank() || precoField.getText().isBlank() || estoqueField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Nome, preço e estoque são obrigatórios.", "Erro",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            CategoriaOpcao categoria = (CategoriaOpcao) categoriaCombo.getSelectedItem();
            boolean criado = ApiClient.postProduto(nomeField.getText().trim(),
                new BigDecimal(precoField.getText().trim()), Integer.parseInt(estoqueField.getText().trim()),
                categoria.id, descricaoArea.getText().trim());
            if (criado) {
                JOptionPane.showMessageDialog(this, "Produto adicionado com sucesso!", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
                carregarProdutos();
            } else {
                mostrarErro("Erro ao adicionar produto");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Preço deve ser decimal e estoque deve ser inteiro.", "Erro",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<CategoriaOpcao> carregarCategoriasAtivas() {
        List<CategoriaOpcao> categorias = new ArrayList<>();
        for (Object categoria : ApiClient.getCategorias()) {
            Map<String, Object> c = MapUtils.toMap(categoria);
            Long id = MapUtils.getAsLong(c, "id");
            if (id != null && MapUtils.getAsBoolean(c, "ativo")) {
                categorias.add(new CategoriaOpcao(id, MapUtils.getAsString(c, "nome")));
            }
        }
        return categorias;
    }

    private void editarProduto() {
        int selectedRow = tabelaProdutos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um produto para editar", "Aviso",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) tabelaProdutos.getValueAt(selectedRow, 0);
        JTextField nomeField = new JTextField((String) tabelaProdutos.getValueAt(selectedRow, 1));
        JTextField precoField = new JTextField(tabelaProdutos.getValueAt(selectedRow, 2).toString());
        JTextField estoqueField = new JTextField(tabelaProdutos.getValueAt(selectedRow, 3).toString());
        JTextArea descricaoArea = new JTextArea(2, 20);
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Nome:")); panel.add(nomeField);
        panel.add(new JLabel("Preço:")); panel.add(precoField);
        panel.add(new JLabel("Estoque:")); panel.add(estoqueField);
        panel.add(new JLabel("Descrição:")); panel.add(new JScrollPane(descricaoArea));
        if (JOptionPane.showConfirmDialog(this, panel, "Editar Produto", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            try {
                if (ApiClient.putProduto(id, nomeField.getText(), new BigDecimal(precoField.getText()),
                        Integer.parseInt(estoqueField.getText()), descricaoArea.getText())) {
                    JOptionPane.showMessageDialog(this, "Produto atualizado com sucesso!", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
                    carregarProdutos();
                } else {
                    mostrarErro("Erro ao atualizar produto");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Preço deve ser decimal e estoque deve ser inteiro.", "Erro",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deletarProduto() {
        int selectedRow = tabelaProdutos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um produto para deletar", "Aviso",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) tabelaProdutos.getValueAt(selectedRow, 0);
        if (JOptionPane.showConfirmDialog(this, "Tem certeza que deseja deletar este produto?", "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (ApiClient.delete("/produtos", id)) {
                JOptionPane.showMessageDialog(this, "Produto deletado com sucesso!", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
                carregarProdutos();
            } else {
                mostrarErro("Erro ao deletar produto");
            }
        }
    }

    private void mostrarErro(String padrao) {
        String erro = ApiClient.getUltimoErro();
        JOptionPane.showMessageDialog(this, erro == null || erro.isBlank() ? padrao : erro,
            "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private static class CategoriaOpcao {
        private final Long id;
        private final String nome;

        private CategoriaOpcao(Long id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        @Override
        public String toString() {
            return nome + " (ID: " + id + ")";
        }
    }
}

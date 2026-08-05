package com.ecommerce.ui.panels;

import com.ecommerce.ui.service.ApiClient;
import com.ecommerce.ui.util.MapUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProdutoPanel extends JPanel {
    
    private JTable tabelaProdutos;
    private JButton btnAdicionar;
    private JButton btnEditar;
    private JButton btnDeletar;
    private JButton btnAtualizar;
    
    public ProdutoPanel() {
        initComponents();
        carregarProdutos();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Painel de botões
        JPanel painelBotoes = new JPanel();
        btnAdicionar = new JButton("Adicionar");
        btnEditar = new JButton("Editar");
        btnDeletar = new JButton("Deletar");
        btnAtualizar = new JButton("Atualizar");
        
        btnAdicionar.addActionListener(e -> adicionarProduto());
        btnEditar.addActionListener(e -> editarProduto());
        btnDeletar.addActionListener(e -> deletarProduto());
        btnAtualizar.addActionListener(e -> carregarProdutos());
        
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnAtualizar);
        
        // Tabela
        tabelaProdutos = new JTable();
        JScrollPane scrollPane = new JScrollPane(tabelaProdutos);
        
        add(painelBotoes, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void carregarProdutos() {
        try {
            List<?> produtos = ApiClient.getProdutos();
            
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("ID");
            model.addColumn("Nome");
            model.addColumn("Preço");
            model.addColumn("Estoque");
            model.addColumn("Categoria");
            model.addColumn("Ativo");
            
            for (Object produto : produtos) {
                if (produto instanceof Map) {
                    Map<String, Object> p = MapUtils.toMap(produto);
                    model.addRow(new Object[]{
                        MapUtils.getAsLong(p, "id"),
                        MapUtils.getAsString(p, "nome"),
                        MapUtils.getAsBigDecimal(p, "preco"),
                        MapUtils.getAsInteger(p, "estoque"),
                        MapUtils.getAsLong(p, "categoriaId"),
                        MapUtils.getAsBoolean(p, "ativo")
                    });
                }
            }
            
            tabelaProdutos.setModel(model);
            System.out.println("✅ Produtos carregados: " + produtos.size());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao carregar produtos: " + e.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void adicionarProduto() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 5));
        JTextField nomeField = new JTextField();
        JTextField precoField = new JTextField();
        JTextField estoqueField = new JTextField();
        JTextField categoriaIdField = new JTextField();
        JTextArea descricaoArea = new JTextArea(2, 20);
        
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Preço:"));
        panel.add(precoField);
        panel.add(new JLabel("Estoque:"));
        panel.add(estoqueField);
        panel.add(new JLabel("Categoria ID:"));
        panel.add(categoriaIdField);
        panel.add(new JLabel("Descrição:"));
        panel.add(new JScrollPane(descricaoArea));
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Adicionar Produto", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (nomeField.getText().isEmpty() || precoField.getText().isEmpty() || 
                estoqueField.getText().isEmpty() || categoriaIdField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Todos os campos são obrigatórios", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                BigDecimal preco = new BigDecimal(precoField.getText());
                Integer estoque = Integer.parseInt(estoqueField.getText());
                Long categoriaId = Long.parseLong(categoriaIdField.getText());
                
                if (ApiClient.postProduto(nomeField.getText(), 
                        preco,
                        estoque,
                        categoriaId,
                        descricaoArea.getText())) {
                    JOptionPane.showMessageDialog(this, 
                        "Produto adicionado com sucesso!", "Sucesso", 
                        JOptionPane.INFORMATION_MESSAGE);
                    carregarProdutos();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Erro ao adicionar produto", "Erro", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    "Preço deve ser um número decimal e Estoque um número inteiro", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editarProduto() {
        int selectedRow = tabelaProdutos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione um produto para editar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaProdutos.getValueAt(selectedRow, 0);
        String nome = (String) tabelaProdutos.getValueAt(selectedRow, 1);
        Object preco = tabelaProdutos.getValueAt(selectedRow, 2);
        Object estoque = tabelaProdutos.getValueAt(selectedRow, 3);
        
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        JTextField nomeField = new JTextField(nome);
        JTextField precoField = new JTextField(preco.toString());
        JTextField estoqueField = new JTextField(estoque.toString());
        JTextArea descricaoArea = new JTextArea(2, 20);
        
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Preço:"));
        panel.add(precoField);
        panel.add(new JLabel("Estoque:"));
        panel.add(estoqueField);
        panel.add(new JLabel("Descrição:"));
        panel.add(new JScrollPane(descricaoArea));
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Editar Produto", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                BigDecimal precoBigDecimal = new BigDecimal(precoField.getText());
                Integer estoqueInt = Integer.parseInt(estoqueField.getText());
                
                if (ApiClient.putProduto(id, nomeField.getText(), 
                        precoBigDecimal,
                        estoqueInt,
                        descricaoArea.getText())) {
                    JOptionPane.showMessageDialog(this, 
                        "Produto atualizado com sucesso!", "Sucesso", 
                        JOptionPane.INFORMATION_MESSAGE);
                    carregarProdutos();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Erro ao atualizar produto", "Erro", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    "Preço deve ser um número decimal e Estoque um número inteiro", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deletarProduto() {
        int selectedRow = tabelaProdutos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione um produto para deletar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaProdutos.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Tem certeza que deseja deletar este produto?", "Confirmar", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (ApiClient.delete("/produtos", id)) {
                JOptionPane.showMessageDialog(this, 
                    "Produto deletado com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarProdutos();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao deletar produto", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
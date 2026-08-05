package com.ecommerce.ui.panels;

import com.ecommerce.ui.service.ApiClient;
import com.ecommerce.ui.util.MapUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class CategoriaPanel extends JPanel {
    
    private JTable tabelaCategorias;
    private JButton btnAdicionar;
    private JButton btnEditar;
    private JButton btnDeletar;
    private JButton btnAtualizar;
    
    public CategoriaPanel() {
        initComponents();
        carregarCategorias();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Painel de botões
        JPanel painelBotoes = new JPanel();
        btnAdicionar = new JButton("Adicionar");
        btnEditar = new JButton("Editar");
        btnDeletar = new JButton("Deletar");
        btnAtualizar = new JButton("Atualizar");
        
        btnAdicionar.addActionListener(e -> adicionarCategoria());
        btnEditar.addActionListener(e -> editarCategoria());
        btnDeletar.addActionListener(e -> deletarCategoria());
        btnAtualizar.addActionListener(e -> carregarCategorias());
        
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnAtualizar);
        
        // Tabela
        tabelaCategorias = new JTable();
        JScrollPane scrollPane = new JScrollPane(tabelaCategorias);
        
        add(painelBotoes, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void carregarCategorias() {
        try {
            List<?> categorias = ApiClient.getCategorias();
            
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("ID");
            model.addColumn("Nome");
            model.addColumn("Descrição");
            model.addColumn("Ativo");
            
            for (Object categoria : categorias) {
                if (categoria instanceof Map) {
                    Map<String, Object> c = MapUtils.toMap(categoria);
                    model.addRow(new Object[]{
                        MapUtils.getAsLong(c, "id"),
                        MapUtils.getAsString(c, "nome"),
                        MapUtils.getAsString(c, "descricao"),
                        MapUtils.getAsBoolean(c, "ativo")
                    });
                }
            }
            
            tabelaCategorias.setModel(model);
            System.out.println("✅ Categorias carregadas: " + categorias.size());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao carregar categorias: " + e.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void adicionarCategoria() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField nomeField = new JTextField();
        JTextArea descricaoArea = new JTextArea(3, 20);
        
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Descrição:"));
        panel.add(new JScrollPane(descricaoArea));
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Adicionar Categoria", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (nomeField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Nome é obrigatório", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (ApiClient.postCategoria(nomeField.getText(), descricaoArea.getText())) {
                JOptionPane.showMessageDialog(this, 
                    "Categoria adicionada com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarCategorias();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao adicionar categoria", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editarCategoria() {
        int selectedRow = tabelaCategorias.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione uma categoria para editar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaCategorias.getValueAt(selectedRow, 0);
        String nome = (String) tabelaCategorias.getValueAt(selectedRow, 1);
        String descricao = (String) tabelaCategorias.getValueAt(selectedRow, 2);
        
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField nomeField = new JTextField(nome);
        JTextArea descricaoArea = new JTextArea(descricao, 3, 20);
        
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Descrição:"));
        panel.add(new JScrollPane(descricaoArea));
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Editar Categoria", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (ApiClient.putCategoria(id, nomeField.getText(), descricaoArea.getText())) {
                JOptionPane.showMessageDialog(this, 
                    "Categoria atualizada com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarCategorias();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao atualizar categoria", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deletarCategoria() {
        int selectedRow = tabelaCategorias.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione uma categoria para deletar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaCategorias.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Tem certeza que deseja deletar esta categoria?", "Confirmar", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (ApiClient.delete("/categorias", id)) {
                JOptionPane.showMessageDialog(this, 
                    "Categoria deletada com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarCategorias();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao deletar categoria", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
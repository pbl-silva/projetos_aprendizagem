package com.ecommerce.ui.panels;

import com.ecommerce.ui.service.ApiClient;
import com.ecommerce.ui.util.MapUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class PedidoPanel extends JPanel {
    
    private JTable tabelaPedidos;
    private JButton btnAdicionar;
    private JButton btnEditar;
    private JButton btnDeletar;
    private JButton btnAtualizar;
    
    public PedidoPanel() {
        initComponents();
        carregarPedidos();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        JPanel painelBotoes = new JPanel();
        btnAdicionar = new JButton("Adicionar");
        btnEditar = new JButton("Editar");
        btnDeletar = new JButton("Deletar");
        btnAtualizar = new JButton("Atualizar");
        
        btnAdicionar.addActionListener(e -> adicionarPedido());
        btnEditar.addActionListener(e -> editarPedido());
        btnDeletar.addActionListener(e -> deletarPedido());
        btnAtualizar.addActionListener(e -> carregarPedidos());
        
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnAtualizar);
        
        tabelaPedidos = new JTable();
        JScrollPane scrollPane = new JScrollPane(tabelaPedidos);
        
        add(painelBotoes, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void carregarPedidos() {
        try {
            List<?> pedidos = ApiClient.getPedidos();
            
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("ID");
            model.addColumn("Cliente ID");
            model.addColumn("Status");
            model.addColumn("Total");
            model.addColumn("Data Criação");
            
            for (Object pedido : pedidos) {
                if (pedido instanceof Map) {
                    Map<String, Object> p = MapUtils.toMap(pedido);
                    model.addRow(new Object[]{
                        MapUtils.getAsLong(p, "id"),
                        MapUtils.getAsLong(p, "clienteId"),
                        MapUtils.getAsString(p, "status"),
                        MapUtils.getAsDouble(p, "total"),
                        MapUtils.getAsString(p, "dataCriacao")
                    });
                }
            }
            
            tabelaPedidos.setModel(model);
            System.out.println("✅ Pedidos carregados: " + pedidos.size());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao carregar pedidos: " + e.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void adicionarPedido() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField clienteIdField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{"PENDENTE", "CONFIRMADO", "ENVIADO", "ENTREGUE", "CANCELADO"});
        
        panel.add(new JLabel("Cliente ID:"));
        panel.add(clienteIdField);
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Adicionar Pedido", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (clienteIdField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Cliente ID é obrigatório", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                if (ApiClient.postPedido(Long.parseLong(clienteIdField.getText()), 
                        (String) statusCombo.getSelectedItem())) {
                    JOptionPane.showMessageDialog(this, 
                        "Pedido adicionado com sucesso!", "Sucesso", 
                        JOptionPane.INFORMATION_MESSAGE);
                    carregarPedidos();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Erro ao adicionar pedido", "Erro", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    "Cliente ID deve ser um número", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editarPedido() {
        int selectedRow = tabelaPedidos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione um pedido para editar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaPedidos.getValueAt(selectedRow, 0);
        String statusAtual = (String) tabelaPedidos.getValueAt(selectedRow, 2);
        
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{"PENDENTE", "CONFIRMADO", "ENVIADO", "ENTREGUE", "CANCELADO"});
        statusCombo.setSelectedItem(statusAtual);
        
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Editar Pedido", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (ApiClient.putPedido(id, (String) statusCombo.getSelectedItem())) {
                JOptionPane.showMessageDialog(this, 
                    "Pedido atualizado com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarPedidos();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao atualizar pedido", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deletarPedido() {
        int selectedRow = tabelaPedidos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione um pedido para deletar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaPedidos.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Tem certeza que deseja deletar este pedido?", "Confirmar", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (ApiClient.delete("/pedidos", id)) {
                JOptionPane.showMessageDialog(this, 
                    "Pedido deletado com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarPedidos();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao deletar pedido", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
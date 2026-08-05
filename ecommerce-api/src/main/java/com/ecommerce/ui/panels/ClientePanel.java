package com.ecommerce.ui.panels;

import com.ecommerce.ui.service.ApiClient;
import com.ecommerce.ui.util.MapUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ClientePanel extends JPanel {
    
    private JTable tabelaClientes;
    private JButton btnAdicionar;
    private JButton btnEditar;
    private JButton btnDeletar;
    private JButton btnAtualizar;
    
    public ClientePanel() {
        initComponents();
        carregarClientes();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        JPanel painelBotoes = new JPanel();
        btnAdicionar = new JButton("Adicionar");
        btnEditar = new JButton("Editar");
        btnDeletar = new JButton("Deletar");
        btnAtualizar = new JButton("Atualizar");
        
        btnAdicionar.addActionListener(e -> adicionarCliente());
        btnEditar.addActionListener(e -> editarCliente());
        btnDeletar.addActionListener(e -> deletarCliente());
        btnAtualizar.addActionListener(e -> carregarClientes());
        
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnAtualizar);
        
        tabelaClientes = new JTable();
        JScrollPane scrollPane = new JScrollPane(tabelaClientes);
        
        add(painelBotoes, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void carregarClientes() {
        try {
            List<?> clientes = ApiClient.getClientes();
            
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("ID");
            model.addColumn("Nome");
            model.addColumn("Email");
            model.addColumn("CPF");
            model.addColumn("Telefone");
            model.addColumn("Ativo");
            
            for (Object cliente : clientes) {
                if (cliente instanceof Map) {
                    Map<String, Object> c = MapUtils.toMap(cliente);
                    model.addRow(new Object[]{
                        MapUtils.getAsLong(c, "id"),
                        MapUtils.getAsString(c, "nome"),
                        MapUtils.getAsString(c, "email"),
                        MapUtils.getAsString(c, "cpf"),
                        MapUtils.getAsString(c, "telefone"),
                        MapUtils.getAsBoolean(c, "ativo")
                    });
                }
            }
            
            tabelaClientes.setModel(model);
            System.out.println("✅ Clientes carregados: " + clientes.size());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao carregar clientes: " + e.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void adicionarCliente() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        JTextField nomeField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField cpfField = new JTextField();
        JTextField telefoneField = new JTextField();
        JTextField enderecoField = new JTextField();
        
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("CPF:"));
        panel.add(cpfField);
        panel.add(new JLabel("Telefone:"));
        panel.add(telefoneField);
        panel.add(new JLabel("Endereço:"));
        panel.add(enderecoField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Adicionar Cliente", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (nomeField.getText().isEmpty() || emailField.getText().isEmpty() || cpfField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Nome, Email e CPF são obrigatórios", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (ApiClient.postCliente(nomeField.getText(), emailField.getText(), 
                    cpfField.getText(), telefoneField.getText(), enderecoField.getText())) {
                JOptionPane.showMessageDialog(this, 
                    "Cliente adicionado com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarClientes();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao adicionar cliente", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editarCliente() {
        int selectedRow = tabelaClientes.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione um cliente para editar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaClientes.getValueAt(selectedRow, 0);
        String nome = (String) tabelaClientes.getValueAt(selectedRow, 1);
        String email = (String) tabelaClientes.getValueAt(selectedRow, 2);
        String cpf = (String) tabelaClientes.getValueAt(selectedRow, 3);
        String telefone = (String) tabelaClientes.getValueAt(selectedRow, 4);
        
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        JTextField nomeField = new JTextField(nome);
        JTextField emailField = new JTextField(email);
        JTextField cpfField = new JTextField(cpf);
        JTextField telefoneField = new JTextField(telefone);
        JTextField enderecoField = new JTextField();
        
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("CPF:"));
        panel.add(cpfField);
        panel.add(new JLabel("Telefone:"));
        panel.add(telefoneField);
        panel.add(new JLabel("Endereço:"));
        panel.add(enderecoField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Editar Cliente", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (ApiClient.putCliente(id, nomeField.getText(), emailField.getText(), 
                    cpfField.getText(), telefoneField.getText(), enderecoField.getText())) {
                JOptionPane.showMessageDialog(this, 
                    "Cliente atualizado com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarClientes();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao atualizar cliente", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deletarCliente() {
        int selectedRow = tabelaClientes.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Selecione um cliente para deletar", "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Long id = (Long) tabelaClientes.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Tem certeza que deseja deletar este cliente?", "Confirmar", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (ApiClient.delete("/clientes", id)) {
                JOptionPane.showMessageDialog(this, 
                    "Cliente deletado com sucesso!", "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE);
                carregarClientes();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao deletar cliente", "Erro", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
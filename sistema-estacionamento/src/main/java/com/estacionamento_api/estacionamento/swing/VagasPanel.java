package com.estacionamento_api.estacionamento.swing;

import com.estacionamento_api.estacionamento.dto.VagaDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class VagasPanel extends JPanel {
    private JTable vagasTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    
    public VagasPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(createStatusPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        statusLabel = new JLabel("Carregando...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(statusLabel);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"Vaga", "Tipo", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        vagasTable = new JTable(tableModel);
        
        JScrollPane scrollPane = new JScrollPane(vagasTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        carregarVagas();
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton atualizarButton = new JButton("Atualizar");
        atualizarButton.addActionListener(e -> carregarVagas());
        panel.add(atualizarButton);
        
        return panel;
    }
    
    private void carregarVagas() {
        tableModel.setRowCount(0);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            List<VagaDTO> vagas = ApiClient.getList(
                "/vagas", new TypeReference<List<VagaDTO>>() {});

            for (VagaDTO vaga : vagas) {
                tableModel.addRow(new Object[]{
                    vaga.getNumero(),
                    formatarTipoVaga(vaga.getTipoVaga().name()),
                    Boolean.TRUE.equals(vaga.getDisponivel()) ? "Disponível" : "Ocupada"
                });
            }

            long disponiveis = vagas.stream()
                .filter(v -> Boolean.TRUE.equals(v.getDisponivel()))
                .count();
            int total = vagas.size();
            double percentual = total == 0 ? 0 : (disponiveis * 100.0 / total);

            statusLabel.setText(String.format(
                "Vagas Disponíveis: %d/%d (%.0f%%)", disponiveis, total, percentual));
        } catch (ApiClient.ApiException ex) {
            statusLabel.setText("Erro da API: " + ex.getMessage());
        } catch (IOException | InterruptedException ex) {
            statusLabel.setText(
                "Não foi possível conectar à API em http://localhost:8080");
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private String formatarTipoVaga(String tipo) {
        switch (tipo) {
            case "COMUM": return "Comum";
            case "PCD": return "PCD";
            case "MOTO": return "Moto";
            case "ELETRICA": return "Elétrica";
            default: return tipo;
        }
    }
}

package com.estacionamento_api.estacionamento.swing;

import javax.swing.*;
import java.awt.*;

public class EstacionamentoSwingApp extends JFrame {
    private JTabbedPane tabbedPane;
    
    public EstacionamentoSwingApp() {
        setTitle("Sistema de Estacionamento");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setResizable(true);
        
        initComponents();
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Entrada", new EntradaPanel());
        tabbedPane.addTab("Saída", new SaidaPanel());
        tabbedPane.addTab("Vagas", new VagasPanel());
        tabbedPane.addTab("Relatórios", new RelatoriosPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EstacionamentoSwingApp app = new EstacionamentoSwingApp();
            app.setVisible(true);
        });
    }
}
    
package com.ecommerce.ui;

import com.ecommerce.ui.panels.*;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        // Configurações da janela
        setTitle("E-Commerce API - Gerenciamento");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null); // Centraliza na tela
        setResizable(true);
        
        // Cria o painel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        
        // Menu
        createMenuBar();
        
        // Painel de abas com os painéis específicos
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Categorias", new CategoriaPanel());
        tabbedPane.addTab("Produtos", new ProdutoPanel());
        tabbedPane.addTab("Clientes", new ClientePanel());
        tabbedPane.addTab("Pedidos", new PedidoPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Barra de status
        JLabel statusBar = new JLabel("✅ Pronto - API em http://localhost:8080/api");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Menu Arquivo
        JMenu fileMenu = new JMenu("Arquivo");
        JMenuItem logoutItem = new JMenuItem("Sair da conta");
        logoutItem.addActionListener(e -> {
            com.ecommerce.ui.service.ApiClient.logout();
            JOptionPane.showMessageDialog(this,
                "Sessão encerrada. Feche e abra o programa novamente para logar de novo.",
                "Sair da conta", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        });
        JMenuItem exitItem = new JMenuItem("Sair");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Menu Ajuda
        JMenu helpMenu = new JMenu("Ajuda");
        JMenuItem aboutItem = new JMenuItem("Sobre");
        aboutItem.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, 
                "E-Commerce API v1.0\n\n" +
                "Spring Boot 4.1.0 + Swing\n" +
                "Java 21\n\n" +
                "API REST: http://localhost:8080/api\n" +
                "Swagger: http://localhost:8080/api/swagger-ui.html", 
                "Sobre", 
                JOptionPane.INFORMATION_MESSAGE)
        );
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
}
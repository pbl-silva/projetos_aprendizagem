package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.ecommerce.ui.MainFrame;
import com.ecommerce.ui.auth.LoginDialog;

import java.awt.GraphicsEnvironment;
import javax.swing.*;

@SpringBootApplication
public class Application {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Em ambientes sem display gráfico (Docker, servidores, CI), tentar criar
        // uma janela Swing lança HeadlessException e derruba o processo. Nesses
        // casos rodamos só a API, sem a interface desktop.
        if (GraphicsEnvironment.isHeadless()) {
            springContext = SpringApplication.run(Application.class, args);
            System.out.println("✅ Spring Boot iniciado com sucesso (modo headless, sem interface Swing)!");
            System.out.println("✅ API REST disponível em http://localhost:8080/api");
            System.out.println("✅ Swagger em http://localhost:8080/api/swagger-ui.html");
            return;
        }

        // Sobe o Spring Boot ANTES de mostrar qualquer janela: a tela de login
        // precisa da API já respondendo em localhost:8080, senão a primeira
        // tentativa de login cairia em "conexão recusada".
        try {
            springContext = SpringApplication.run(Application.class, args);
            System.out.println("✅ Spring Boot iniciado com sucesso!");
            System.out.println("✅ API REST disponível em http://localhost:8080/api");
            System.out.println("✅ Swagger em http://localhost:8080/api/swagger-ui.html");
        } catch (Exception e) {
            System.err.println("❌ Erro ao iniciar Spring Boot: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // Exige login antes de liberar a janela principal - sem token
                // JWT, nenhuma chamada à API (exceto /auth/**) funciona.
                boolean autenticado = LoginDialog.autenticarUsuario(null);
                if (!autenticado) {
                    System.out.println("Login cancelado. Encerrando aplicação.");
                    if (springContext != null) {
                        springContext.close();
                    }
                    System.exit(0);
                    return;
                }

                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                System.out.println("✅ Interface Swing iniciada com sucesso!");
            } catch (Exception e) {
                System.err.println("❌ Erro ao criar interface Swing: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}
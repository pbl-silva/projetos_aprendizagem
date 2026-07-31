package com.estacionamento_api.estacionamento.swing;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RelatoriosPanel extends JPanel {
    private JLabel ocupacaoLabel;
    private JLabel vagasDisponiveisLabel;
    private JLabel veiculosEstacionadosLabel;
    private JTextField inicioField;
    private JTextField fimField;
    private JLabel faturamentoLabel;

    public RelatoriosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createOcupacaoPanel(), BorderLayout.NORTH);
        add(createFaturamentoPanel(), BorderLayout.CENTER);
    }

    private JPanel createOcupacaoPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Taxa de Ocupação"));

        panel.add(new JLabel("Vagas Disponíveis:"));
        vagasDisponiveisLabel = new JLabel("-");
        panel.add(vagasDisponiveisLabel);

        panel.add(new JLabel("Veículos Estacionados:"));
        veiculosEstacionadosLabel = new JLabel("-");
        panel.add(veiculosEstacionadosLabel);

        panel.add(new JLabel("Percentual de Ocupação:"));
        ocupacaoLabel = new JLabel("-");
        panel.add(ocupacaoLabel);

        JButton atualizarButton = new JButton("Atualizar Ocupação");
        atualizarButton.addActionListener(e -> carregarOcupacao());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.add(atualizarButton, BorderLayout.SOUTH);

        carregarOcupacao();

        return wrapper;
    }

    private JPanel createFaturamentoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Faturamento por Período"));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.add(new JLabel("Início (yyyy-MM-ddTHH:mm:ss):"));
        inicioField = new JTextField();
        formPanel.add(inicioField);

        formPanel.add(new JLabel("Fim (yyyy-MM-ddTHH:mm:ss):"));
        fimField = new JTextField();
        formPanel.add(fimField);

        JButton consultarButton = new JButton("Consultar Faturamento");
        consultarButton.addActionListener(e -> consultarFaturamento());

        faturamentoLabel = new JLabel("Faturamento: -");
        faturamentoLabel.setFont(new Font("Arial", Font.BOLD, 14));

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(consultarButton, BorderLayout.CENTER);
        panel.add(faturamentoLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void carregarOcupacao() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Map<String, Object> resultado = ApiClient.get("/relatorios/ocupacao", Map.class);

            vagasDisponiveisLabel.setText(
                resultado.get("vagasDisponiveis") + " / " + resultado.get("vagasTotal"));
            veiculosEstacionadosLabel.setText(
                String.valueOf(resultado.get("veiculosEstacionados")));
            ocupacaoLabel.setText(
                String.valueOf(resultado.get("percentualOcupacao")));
        } catch (ApiClient.ApiException ex) {
            JOptionPane.showMessageDialog(this, "Erro da API: " + ex.getMessage());
        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(this,
                "Não foi possível conectar à API em http://localhost:8080\n" + ex.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void consultarFaturamento() {
        String inicio = inicioField.getText().trim();
        String fim = fimField.getText().trim();

        if (inicio.isEmpty() || fim.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Informe o início e o fim do período!");
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            String query = "?inicio=" + URLEncoder.encode(inicio, StandardCharsets.UTF_8)
                + "&fim=" + URLEncoder.encode(fim, StandardCharsets.UTF_8);

            Map<String, Object> resultado = ApiClient.get(
                "/relatorios/faturamento" + query, Map.class);

            faturamentoLabel.setText(
                "Faturamento: R$ " + resultado.get("faturamento"));
        } catch (ApiClient.ApiException ex) {
            JOptionPane.showMessageDialog(this,
                "Erro da API: " + ex.getMessage()
                + "\nVerifique o formato das datas (ex: 2026-07-01T00:00:00)");
        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(this,
                "Não foi possível conectar à API em http://localhost:8080\n" + ex.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }
}

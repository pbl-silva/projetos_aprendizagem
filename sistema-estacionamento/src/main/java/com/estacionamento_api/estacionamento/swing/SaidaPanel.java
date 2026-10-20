package com.estacionamento_api.estacionamento.swing;

import com.estacionamento_api.estacionamento.dto.EntradaDTO;
import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.dto.SaidaDTO;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoPagamento;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class SaidaPanel extends JPanel {
    private JTextField placaField;
    private JComboBox<String> modalidadeCombo;
    private JComboBox<String> pagamentoCombo;
    private JTextArea reciboArea;
    
    public SaidaPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(createFormPanel(), BorderLayout.NORTH);
        add(createReciboPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Registro de Saída"));
        
        panel.add(new JLabel("Placa do Veículo:"));
        placaField = new JTextField();
        panel.add(placaField);
        
        panel.add(new JLabel("Modalidade:"));
        modalidadeCombo = new JComboBox<>(
            new String[]{"Diária", "Mensal"});
        panel.add(modalidadeCombo);
        
        panel.add(new JLabel("Tipo de Pagamento:"));
        pagamentoCombo = new JComboBox<>(
            new String[]{"Dinheiro", "Cartão Crédito", 
                        "Cartão Débito", "PIX"});
        panel.add(pagamentoCombo);
        
        return panel;
    }
    
    private JPanel createReciboPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Recibo"));
        
        reciboArea = new JTextArea();
        reciboArea.setEditable(false);
        reciboArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(reciboArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton processarButton = new JButton("Processar Saída");
        processarButton.addActionListener(e -> processarSaida());
        panel.add(processarButton);
        
        JButton imprimirButton = new JButton("Imprimir Recibo");
        imprimirButton.addActionListener(e -> imprimirRecibo());
        panel.add(imprimirButton);
        
        return panel;
    }
    
    private void processarSaida() {
        String placa = placaField.getText().trim().toUpperCase();
        String modalidadeLabel = (String) modalidadeCombo.getSelectedItem();
        String pagamentoLabel = (String) pagamentoCombo.getSelectedItem();
        
        if (placa.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Informe a placa do veículo!");
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            EntradaDTO entradaAtiva = buscarEntradaAtivaPorPlaca(placa);
            if (entradaAtiva == null) {
                JOptionPane.showMessageDialog(this,
                    "Nenhuma entrada ativa encontrada para a placa " + placa);
                return;
            }

            SaidaDTO saidaRequisicao = SaidaDTO.builder()
                .entradaId(entradaAtiva.getId())
                .modalidade(mapModalidade(modalidadeLabel))
                .tipoPagamento(mapPagamento(pagamentoLabel))
                .build();

            ReciboDTO recibo = ApiClient.post("/saidas", saidaRequisicao, ReciboDTO.class);
            exibirRecibo(recibo);
        } catch (ApiClient.ApiException ex) {
            JOptionPane.showMessageDialog(this, "Erro da API: " + ex.getMessage());
        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(this,
                "Não foi possível conectar à API.\n" +
                "Ela precisa estar rodando em http://localhost:8080\n" + ex.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private EntradaDTO buscarEntradaAtivaPorPlaca(String placa)
            throws IOException, InterruptedException {
        List<EntradaDTO> ativas = ApiClient.getList(
            "/entradas/ativas", new TypeReference<List<EntradaDTO>>() {});
        return ativas.stream()
            .filter(entrada -> placa.equalsIgnoreCase(entrada.getPlacaVeiculo()))
            .findFirst()
            .orElse(null);
    }

    private Modalidade mapModalidade(String label) {
        return "Mensal".equals(label) ? Modalidade.MENSAL : Modalidade.DIARIA;
    }

    private TipoPagamento mapPagamento(String label) {
        switch (label) {
            case "Cartão Crédito": return TipoPagamento.CARTAO_CREDITO;
            case "Cartão Débito": return TipoPagamento.CARTAO_DEBITO;
            case "PIX": return TipoPagamento.PIX;
            default: return TipoPagamento.DINHEIRO;
        }
    }

    private void exibirRecibo(ReciboDTO recibo) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════╗\n");
        sb.append("║       RECIBO DE ESTACIONAMENTO         ║\n");
        sb.append("╚════════════════════════════════════════╝\n\n");
        sb.append("Número: ").append(recibo.getNumeroRecibo()).append("\n");
        sb.append("Data/Hora: ").append(recibo.getDataEmissao()).append("\n\n");
        sb.append("VEÍCULO:\n");
        sb.append("Placa: ").append(recibo.getPlaca()).append("\n");
        sb.append("Marca: ").append(recibo.getMarca()).append("\n");
        sb.append("Modelo: ").append(recibo.getModelo()).append("\n\n");
        sb.append("ESTACIONAMENTO:\n");
        sb.append("Vaga: ").append(recibo.getNumeroVaga()).append("\n");
        sb.append("Entrada: ").append(recibo.getDataHoraEntrada()).append("\n");
        sb.append("Saída: ").append(recibo.getDataHoraSaida()).append("\n");
        sb.append("Tempo: ").append(recibo.getTempoEstacionadoMinutos()).append(" min\n\n");
        sb.append("VALORES:\n");
        sb.append("Valor Base: R$ ").append(recibo.getValorBase()).append("\n");
        sb.append("Modalidade: ").append(recibo.getModalidade()).append("\n");
        sb.append("Desconto: R$ ").append(recibo.getDesconto()).append("\n");
        sb.append("─────────────────────\n");
        sb.append("TOTAL: R$ ").append(recibo.getValorFinal()).append("\n\n");
        sb.append("Pagamento: ").append(recibo.getTipoPagamento()).append("\n");
        sb.append("Status: PAGO\n\n");
        sb.append("Obrigado pela preferência!\n");

        reciboArea.setText(sb.toString());
    }
    
    private void imprimirRecibo() {
        try {
            reciboArea.print();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao imprimir: " + e.getMessage());
        }
    }
}

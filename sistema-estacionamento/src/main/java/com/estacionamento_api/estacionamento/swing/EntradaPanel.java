package com.estacionamento_api.estacionamento.swing;

import com.estacionamento_api.estacionamento.dto.EntradaDTO;
import com.estacionamento_api.estacionamento.dto.VeiculoDTO;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class EntradaPanel extends JPanel {
    private JTextField placaField;
    private JTextField marcaField;
    private JTextField modeloField;
    private JComboBox<String> tipoVeiculoCombo;
    private JCheckBox pcdCheckBox;
    private JTextArea resultadoArea;
    
    public EntradaPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(createFormPanel(), BorderLayout.NORTH);
        add(createResultadoPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Registro de Entrada"));
        
        panel.add(new JLabel("Placa:"));
        placaField = new JTextField();
        panel.add(placaField);
        
        panel.add(new JLabel("Marca:"));
        marcaField = new JTextField();
        panel.add(marcaField);
        
        panel.add(new JLabel("Modelo:"));
        modeloField = new JTextField();
        panel.add(modeloField);
        
        panel.add(new JLabel("Tipo de Veículo:"));
        tipoVeiculoCombo = new JComboBox<>(
            new String[]{"Carro", "Moto", "Carro Elétrico"});
        panel.add(tipoVeiculoCombo);
        
        panel.add(new JLabel("PCD:"));
        pcdCheckBox = new JCheckBox();
        panel.add(pcdCheckBox);
        
        return panel;
    }
    
    private JPanel createResultadoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Resultado"));
        
        resultadoArea = new JTextArea();
        resultadoArea.setEditable(false);
        resultadoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(resultadoArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton registrarButton = new JButton("Registrar Entrada");
        registrarButton.addActionListener(e -> registrarEntrada());
        panel.add(registrarButton);
        
        JButton limparButton = new JButton("Limpar");
        limparButton.addActionListener(e -> limparCampos());
        panel.add(limparButton);
        
        return panel;
    }
    
    private void registrarEntrada() {
        String placa = placaField.getText().trim().toUpperCase();
        String marca = marcaField.getText().trim();
        String modelo = modeloField.getText().trim();
        String tipoLabel = (String) tipoVeiculoCombo.getSelectedItem();
        boolean pcd = pcdCheckBox.isSelected();
        
        if (placa.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Preencha todos os campos!");
            return;
        }

        TipoVeiculo tipoVeiculo = mapTipoVeiculo(tipoLabel);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            VeiculoDTO veiculo = obterOuCadastrarVeiculo(placa, marca, modelo, tipoVeiculo, pcd);

            EntradaDTO entradaRequisicao = EntradaDTO.builder()
                .veiculoId(veiculo.getId())
                .build();

            EntradaDTO entrada = ApiClient.post("/entradas", entradaRequisicao, EntradaDTO.class);

            resultadoArea.setText("Entrada registrada com sucesso!\n");
            resultadoArea.append("Placa: " + entrada.getPlacaVeiculo() + "\n");
            resultadoArea.append("Vaga: " + entrada.getNumeroVaga() + "\n");
            resultadoArea.append("PCD: " + (pcd ? "Sim" : "Não") + "\n");
            resultadoArea.append("Horário: " + entrada.getDataHoraEntrada());
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

    /**
     * Busca o veículo pela placa; se não existir (404), cadastra um novo.
     */
    private VeiculoDTO obterOuCadastrarVeiculo(String placa, String marca, String modelo,
                                               TipoVeiculo tipoVeiculo, boolean pcd)
            throws IOException, InterruptedException {
        try {
            return ApiClient.get("/veiculos/placa/" + placa, VeiculoDTO.class);
        } catch (ApiClient.ApiException naoEncontrado) {
            if (naoEncontrado.status != 404) {
                throw naoEncontrado;
            }
            VeiculoDTO novoVeiculo = VeiculoDTO.builder()
                .placa(placa)
                .tipoVeiculo(tipoVeiculo)
                .marca(marca)
                .modelo(modelo)
                .pcd(pcd)
                .build();
            return ApiClient.post("/veiculos", novoVeiculo, VeiculoDTO.class);
        }
    }

    private TipoVeiculo mapTipoVeiculo(String label) {
        if ("Moto".equals(label)) {
            return TipoVeiculo.MOTO;
        } else if ("Carro Elétrico".equals(label)) {
            return TipoVeiculo.CARRO_ELETRICO;
        }
        return TipoVeiculo.CARRO;
    }
    
    private void limparCampos() {
        placaField.setText("");
        marcaField.setText("");
        modeloField.setText("");
        pcdCheckBox.setSelected(false);
        resultadoArea.setText("");
    }
}

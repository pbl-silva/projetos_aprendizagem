package com.ecommerce.ui.panels;

import com.ecommerce.ui.service.ApiClient;
import com.ecommerce.ui.util.MapUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PedidoPanel extends JPanel {

    private JTable tabelaPedidos;

    public PedidoPanel() {
        initComponents();
        carregarPedidos();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        JPanel painelBotoes = new JPanel();
        JButton btnAdicionar = new JButton("Adicionar");
        JButton btnEditar = new JButton("Editar");
        JButton btnDeletar = new JButton("Deletar");
        JButton btnAtualizar = new JButton("Atualizar");
        btnAdicionar.addActionListener(e -> adicionarPedido());
        btnEditar.addActionListener(e -> editarPedido());
        btnDeletar.addActionListener(e -> deletarPedido());
        btnAtualizar.addActionListener(e -> carregarPedidos());
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnAtualizar);
        tabelaPedidos = new JTable();
        add(painelBotoes, BorderLayout.NORTH);
        add(new JScrollPane(tabelaPedidos), BorderLayout.CENTER);
    }

    private void carregarPedidos() {
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Cliente ID", "Status", "Total", "Data Criação"}, 0);
        for (Object pedido : ApiClient.getPedidos()) {
            Map<String, Object> p = MapUtils.toMap(pedido);
            model.addRow(new Object[]{
                MapUtils.getAsLong(p, "id"), MapUtils.getAsLong(p, "clienteId"),
                MapUtils.getAsString(p, "status"), MapUtils.getAsDouble(p, "total"),
                MapUtils.getAsString(p, "dataCriacao")
            });
        }
        tabelaPedidos.setModel(model);
    }

    private void adicionarPedido() {
        List<ClienteOpcao> clientes = carregarClientesAtivos();
        List<ProdutoOpcao> produtos = carregarProdutosDisponiveis();
        if (clientes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre um cliente ativo antes de criar um pedido.",
                "Clientes indisponíveis", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (produtos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não há produtos ativos com estoque disponível.",
                "Produtos indisponíveis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<ClienteOpcao> clienteCombo = new JComboBox<>(clientes.toArray(new ClienteOpcao[0]));
        JComboBox<ProdutoOpcao> produtoCombo = new JComboBox<>(produtos.toArray(new ProdutoOpcao[0]));
        JSpinner quantidadeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        List<ItemSelecionado> itens = new ArrayList<>();
        DefaultTableModel itensModel = new DefaultTableModel(
            new Object[]{"Produto", "Quantidade", "Preço unit.", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable tabelaItens = new JTable(itensModel);
        JLabel totalLabel = new JLabel("Total: R$ 0,00");

        JPanel dadosPedido = new JPanel(new GridLayout(2, 2, 5, 5));
        dadosPedido.add(new JLabel("Cliente:"));
        dadosPedido.add(clienteCombo);
        dadosPedido.add(new JLabel("Produto:"));
        dadosPedido.add(produtoCombo);

        JButton btnIncluirItem = new JButton("Incluir produto");
        JButton btnRemoverItem = new JButton("Remover selecionado");
        JPanel controlesItens = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlesItens.add(new JLabel("Quantidade:"));
        controlesItens.add(quantidadeSpinner);
        controlesItens.add(btnIncluirItem);
        controlesItens.add(btnRemoverItem);

        btnIncluirItem.addActionListener(e -> {
            ProdutoOpcao produto = (ProdutoOpcao) produtoCombo.getSelectedItem();
            int quantidade = (Integer) quantidadeSpinner.getValue();
            ItemSelecionado existente = itens.stream().filter(item -> item.produto.id.equals(produto.id))
                .findFirst().orElse(null);
            int quantidadeFinal = quantidade + (existente == null ? 0 : existente.quantidade);
            if (quantidadeFinal > produto.estoque) {
                JOptionPane.showMessageDialog(this, "Quantidade maior que o estoque disponível ("
                    + produto.estoque + ").", "Estoque insuficiente", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (existente == null) itens.add(new ItemSelecionado(produto, quantidade));
            else existente.quantidade = quantidadeFinal;
            atualizarTabelaItens(itensModel, itens, totalLabel);
        });
        btnRemoverItem.addActionListener(e -> {
            int linha = tabelaItens.getSelectedRow();
            if (linha >= 0) {
                itens.remove(linha);
                atualizarTabelaItens(itensModel, itens, totalLabel);
            }
        });

        JPanel cabecalho = new JPanel();
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        cabecalho.add(dadosPedido);
        cabecalho.add(controlesItens);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(650, 360));
        panel.add(cabecalho, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);
        panel.add(totalLabel, BorderLayout.SOUTH);

        if (JOptionPane.showConfirmDialog(this, panel, "Adicionar Pedido", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        if (itens.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Adicione pelo menos um produto antes de criar o pedido.",
                "Pedido sem produtos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ClienteOpcao cliente = (ClienteOpcao) clienteCombo.getSelectedItem();
        List<Map<String, Object>> itensRequest = itens.stream().map(item -> Map.<String, Object>of(
            "produtoId", item.produto.id, "quantidade", item.quantidade)).toList();
        if (ApiClient.postPedido(cliente.id, itensRequest)) {
            JOptionPane.showMessageDialog(this, "Pedido adicionado com sucesso!", "Sucesso",
                JOptionPane.INFORMATION_MESSAGE);
            carregarPedidos();
        } else {
            mostrarErro("Não foi possível adicionar o pedido.");
        }
    }

    private List<ClienteOpcao> carregarClientesAtivos() {
        List<ClienteOpcao> clientes = new ArrayList<>();
        for (Object cliente : ApiClient.getClientes()) {
            Map<String, Object> c = MapUtils.toMap(cliente);
            Long id = MapUtils.getAsLong(c, "id");
            if (id != null && MapUtils.getAsBoolean(c, "ativo")) {
                clientes.add(new ClienteOpcao(id, MapUtils.getAsString(c, "nome")));
            }
        }
        return clientes;
    }

    private List<ProdutoOpcao> carregarProdutosDisponiveis() {
        List<ProdutoOpcao> produtos = new ArrayList<>();
        for (Object produto : ApiClient.getProdutos()) {
            Map<String, Object> p = MapUtils.toMap(produto);
            Long id = MapUtils.getAsLong(p, "id");
            Integer estoque = MapUtils.getAsInteger(p, "estoque");
            BigDecimal preco = MapUtils.getAsBigDecimal(p, "preco");
            if (id != null && preco != null && estoque != null && estoque > 0 && MapUtils.getAsBoolean(p, "ativo")) {
                produtos.add(new ProdutoOpcao(id, MapUtils.getAsString(p, "nome"), preco, estoque));
            }
        }
        return produtos;
    }

    private void atualizarTabelaItens(DefaultTableModel model, List<ItemSelecionado> itens, JLabel totalLabel) {
        model.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (ItemSelecionado item : itens) {
            BigDecimal subtotal = item.produto.preco.multiply(BigDecimal.valueOf(item.quantidade));
            total = total.add(subtotal);
            model.addRow(new Object[]{item.produto.nome, item.quantidade, item.produto.preco, subtotal});
        }
        totalLabel.setText("Total: R$ " + total);
    }

    private void editarPedido() {
        int selectedRow = tabelaPedidos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um pedido para editar", "Aviso",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) tabelaPedidos.getValueAt(selectedRow, 0);
        JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{"PENDENTE", "CONFIRMADO", "ENVIADO", "ENTREGUE", "CANCELADO"});
        statusCombo.setSelectedItem(tabelaPedidos.getValueAt(selectedRow, 2));
        if (JOptionPane.showConfirmDialog(this, statusCombo, "Editar status do pedido",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (ApiClient.putPedido(id, (String) statusCombo.getSelectedItem())) {
                carregarPedidos();
            } else {
                mostrarErro("Erro ao atualizar pedido");
            }
        }
    }

    private void deletarPedido() {
        int selectedRow = tabelaPedidos.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um pedido para deletar", "Aviso",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) tabelaPedidos.getValueAt(selectedRow, 0);
        if (JOptionPane.showConfirmDialog(this, "Tem certeza que deseja deletar este pedido?", "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (ApiClient.delete("/pedidos", id)) carregarPedidos();
            else mostrarErro("Erro ao deletar pedido");
        }
    }

    private void mostrarErro(String padrao) {
        String erro = ApiClient.getUltimoErro();
        JOptionPane.showMessageDialog(this, erro == null || erro.isBlank() ? padrao : erro,
            "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private static class ClienteOpcao {
        private final Long id;
        private final String nome;
        private ClienteOpcao(Long id, String nome) { this.id = id; this.nome = nome; }
        @Override public String toString() { return nome + " (ID: " + id + ")"; }
    }

    private static class ProdutoOpcao {
        private final Long id;
        private final String nome;
        private final BigDecimal preco;
        private final int estoque;
        private ProdutoOpcao(Long id, String nome, BigDecimal preco, int estoque) {
            this.id = id; this.nome = nome; this.preco = preco; this.estoque = estoque;
        }
        @Override public String toString() { return nome + " — R$ " + preco + " (estoque: " + estoque + ")"; }
    }

    private static class ItemSelecionado {
        private final ProdutoOpcao produto;
        private int quantidade;
        private ItemSelecionado(ProdutoOpcao produto, int quantidade) {
            this.produto = produto; this.quantidade = quantidade;
        }
    }
}

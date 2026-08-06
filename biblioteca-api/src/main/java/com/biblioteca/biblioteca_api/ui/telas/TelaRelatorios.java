package com.biblioteca.biblioteca_api.ui.telas;

import com.biblioteca.biblioteca_api.ui.client.ApiClient;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TelaRelatorios extends JFrame {

    private ApiClient apiClient;
    private JTextArea areaRelatorio;
    private JComboBox<String> cmbTipoRelatorio;

    public TelaRelatorios(ApiClient apiClient) {
        this.apiClient = apiClient;

        setTitle("Relatórios");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Painel Superior
        JPanel painelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelSuperior.add(new JLabel("Tipo de Relatório:"));

        cmbTipoRelatorio = new JComboBox<>(new String[]{
                "Livros Disponíveis",
                "Livros Emprestados",
                "Empréstimos Ativos",
                "Empréstimos Atrasados",
                "Histórico de Empréstimos",
                "Usuários Cadastrados"
        });
        painelSuperior.add(cmbTipoRelatorio);

        JButton btnGerar = new JButton("Gerar Relatório");
        btnGerar.addActionListener(e -> gerarRelatorio());
        painelSuperior.add(btnGerar);

        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.addActionListener(e -> areaRelatorio.setText(""));
        painelSuperior.add(btnLimpar);

        // Área de Texto
        areaRelatorio = new JTextArea();
        areaRelatorio.setEditable(false);
        areaRelatorio.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(areaRelatorio);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Resultado do Relatório"));

        painelPrincipal.add(painelSuperior, BorderLayout.NORTH);
        painelPrincipal.add(scrollPane, BorderLayout.CENTER);

        add(painelPrincipal);
    }

    private void gerarRelatorio() {
        String tipoRelatorio = (String) cmbTipoRelatorio.getSelectedItem();
        StringBuilder sb = new StringBuilder();

        try {
            switch (tipoRelatorio) {
                case "Livros Disponíveis":
                    gerarRelatorioLivrosDisponiveis(sb);
                    break;
                case "Livros Emprestados":
                    gerarRelatorioLivrosEmprestados(sb);
                    break;
                case "Empréstimos Ativos":
                    gerarRelatorioEmprestimosAtivos(sb);
                    break;
                case "Empréstimos Atrasados":
                    gerarRelatorioEmprestimosAtrasados(sb);
                    break;
                case "Histórico de Empréstimos":
                    gerarRelatorioHistoricoEmprestimos(sb);
                    break;
                case "Usuários Cadastrados":
                    gerarRelatorioUsuarios(sb);
                    break;
            }

            areaRelatorio.setText(sb.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar relatório: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void gerarRelatorioLivrosDisponiveis(StringBuilder sb) throws Exception {
        List<ApiClient.Livro> livros = apiClient.listarLivros();

        sb.append("=== RELATÓRIO: LIVROS DISPONÍVEIS ===\n\n");
        sb.append(String.format("%-40s %-25s %-15s %-10s\n", "Título", "Autor", "ISBN", "Ano"));
        sb.append("-".repeat(95)).append("\n");

        int contador = 0;
        for (ApiClient.Livro livro : livros) {
            if (livro.disponivel != null && livro.disponivel) {
                sb.append(String.format("%-40s %-25s %-15s %-10d\n",
                        truncar(livro.titulo, 40),
                        truncar(livro.autor, 25),
                        livro.isbn,
                        livro.anoPublicacao != null ? livro.anoPublicacao : 0));
                contador++;
            }
        }

        sb.append("\nTotal de livros disponíveis: ").append(contador).append("\n");
    }

    private void gerarRelatorioLivrosEmprestados(StringBuilder sb) throws Exception {
        List<ApiClient.Livro> livros = apiClient.listarLivros();

        sb.append("=== RELATÓRIO: LIVROS EMPRESTADOS ===\n\n");
        sb.append(String.format("%-40s %-25s %-15s\n", "Título", "Autor", "ISBN"));
        sb.append("-".repeat(85)).append("\n");

        int contador = 0;
        for (ApiClient.Livro livro : livros) {
            if (livro.disponivel == null || !livro.disponivel) {
                sb.append(String.format("%-40s %-25s %-15s\n",
                        truncar(livro.titulo, 40),
                        truncar(livro.autor, 25),
                        livro.isbn));
                contador++;
            }
        }

        sb.append("\nTotal de livros emprestados: ").append(contador).append("\n");
    }

    private void gerarRelatorioEmprestimosAtivos(StringBuilder sb) throws Exception {
        List<ApiClient.Emprestimo> emprestimos = apiClient.listarEmprestimosAtivos();

        sb.append("=== RELATÓRIO: EMPRÉSTIMOS ATIVOS ===\n\n");
        sb.append(String.format("%-5s %-30s %-30s %-15s %-15s\n",
                "ID", "Usuário", "Livro", "Data Emp.", "Previsão Dev."));
        sb.append("-".repeat(100)).append("\n");

        int contador = 0;
        for (ApiClient.Emprestimo emp : emprestimos) {
            sb.append(String.format("%-5d %-30s %-30s %-15s %-15s\n",
                    emp.id,
                    truncar(nomeUsuario(emp), 30),
                    truncar(tituloLivro(emp), 30),
                    emp.dataEmprestimo,
                    emp.dataDevolucaoPrevista));
            contador++;
        }

        sb.append("\nTotal de empréstimos ativos: ").append(contador).append("\n");
    }

    private void gerarRelatorioEmprestimosAtrasados(StringBuilder sb) throws Exception {
        List<ApiClient.Emprestimo> emprestimos = apiClient.listarEmprestimosAtrasados();

        sb.append("=== RELATÓRIO: EMPRÉSTIMOS ATRASADOS ===\n\n");
        sb.append(String.format("%-5s %-30s %-30s %-15s %-10s\n",
                "ID", "Usuário", "Livro", "Previsão Dev.", "Dias Atraso"));
        sb.append("-".repeat(95)).append("\n");

        int contador = 0;
        for (ApiClient.Emprestimo emp : emprestimos) {
            sb.append(String.format("%-5d %-30s %-30s %-15s %-10d\n",
                    emp.id,
                    truncar(nomeUsuario(emp), 30),
                    truncar(tituloLivro(emp), 30),
                    emp.dataDevolucaoPrevista,
                    calcularDiasAtraso(emp.dataDevolucaoPrevista)));
            contador++;
        }

        sb.append("\nTotal de empréstimos atrasados: ").append(contador).append("\n");
    }

    private void gerarRelatorioHistoricoEmprestimos(StringBuilder sb) throws Exception {
        List<ApiClient.Emprestimo> emprestimos = apiClient.listarEmprestimos();

        sb.append("=== RELATÓRIO: HISTÓRICO DE EMPRÉSTIMOS ===\n\n");
        sb.append(String.format("%-5s %-25s %-25s %-12s %-12s %-10s\n",
                "ID", "Usuário", "Livro", "Data Emp.", "Data Dev.", "Status"));
        sb.append("-".repeat(95)).append("\n");

        for (ApiClient.Emprestimo emp : emprestimos) {
            String dataDev = emp.dataDevolucaoReal != null ? emp.dataDevolucaoReal : "Pendente";
            sb.append(String.format("%-5d %-25s %-25s %-12s %-12s %-10s\n",
                    emp.id,
                    truncar(nomeUsuario(emp), 25),
                    truncar(tituloLivro(emp), 25),
                    emp.dataEmprestimo,
                    dataDev,
                    emp.status));
        }

        sb.append("\nTotal de empréstimos: ").append(emprestimos.size()).append("\n");
    }

    private void gerarRelatorioUsuarios(StringBuilder sb) throws Exception {
        List<ApiClient.Usuario> usuarios = apiClient.listarUsuarios();

        sb.append("=== RELATÓRIO: USUÁRIOS CADASTRADOS ===\n\n");
        sb.append(String.format("%-5s %-30s %-30s %-15s %-15s\n",
                "ID", "Nome", "Email", "CPF", "Tipo"));
        sb.append("-".repeat(100)).append("\n");

        for (ApiClient.Usuario usuario : usuarios) {
            sb.append(String.format("%-5d %-30s %-30s %-15s %-15s\n",
                    usuario.id,
                    truncar(usuario.nome, 30),
                    truncar(usuario.email, 30),
                    usuario.cpf,
                    usuario.tipoUsuario));
        }

        sb.append("\nTotal de usuários cadastrados: ").append(usuarios.size()).append("\n");
    }

    private String truncar(String texto, int tamanho) {
        if (texto == null) return "";
        return texto.length() > tamanho ? texto.substring(0, tamanho - 3) + "..." : texto;
    }

    private String nomeUsuario(ApiClient.Emprestimo emprestimo) {
        return emprestimo.usuario != null ? emprestimo.usuario.nome : "";
    }

    private String tituloLivro(ApiClient.Emprestimo emprestimo) {
        return emprestimo.livro != null ? emprestimo.livro.titulo : "";
    }

    private long calcularDiasAtraso(String dataDevolucaoPrevista) {
        if (dataDevolucaoPrevista == null) return 0L;
        return Math.max(0L, ChronoUnit.DAYS.between(LocalDate.parse(dataDevolucaoPrevista), LocalDate.now()));
    }
}

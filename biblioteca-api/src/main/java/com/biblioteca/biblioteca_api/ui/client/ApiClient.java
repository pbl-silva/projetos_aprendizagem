package com.biblioteca.biblioteca_api.ui.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class ApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiClient(String baseUrl) {
        // Recomenda-se instanciar sem "/api", ex: new ApiClient("http://localhost:8080")
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Helper para construir URLs de forma segura (evita /api/api)
    private String buildUrl(String path) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + (path.startsWith("/") ? path.substring(1) : path);
        } else {
            return baseUrl + (path.startsWith("/") ? path : "/" + path);
        }
    }

    // Helper para log (temporário)
    private void logCall(String url, HttpResponse<String> response) {
        System.out.println("API CALL -> " + url);
        System.out.println("API RESP -> status=" + response.statusCode() + " body=" + (response.body() == null ? "" : response.body()));
    }

    // ==================== LIVROS ====================

    public List<Livro> listarLivros() throws Exception {
        String url = buildUrl("/api/livros");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return Arrays.asList(objectMapper.readValue(response.body(), Livro[].class));
        } else {
            throw new Exception("Erro ao listar livros: " + response.statusCode() + " - " + response.body());
        }
    }

    public Livro buscarLivroPorId(Long id) throws Exception {
        String url = buildUrl("/api/livros/" + id);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Livro.class);
        } else {
            throw new Exception("Erro ao buscar livro: " + response.statusCode() + " - " + response.body());
        }
    }

    public Livro salvarLivro(Livro livro) throws Exception {
        String url = buildUrl("/api/livros");
        String json = objectMapper.writeValueAsString(livro);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Livro.class);
        } else {
            throw new Exception("Erro ao salvar livro: " + response.statusCode() + " - " + response.body());
        }
    }

    public Livro atualizarLivro(Long id, Livro livro) throws Exception {
        String url = buildUrl("/api/livros/" + id);
        String json = objectMapper.writeValueAsString(livro);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Livro.class);
        } else {
            throw new Exception("Erro ao atualizar livro: " + response.statusCode() + " - " + response.body());
        }
    }

    public void deletarLivro(Long id) throws Exception {
        String url = buildUrl("/api/livros/" + id);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new Exception("Erro ao deletar livro: " + response.statusCode() + " - " + response.body());
        }
    }

    // ==================== USUÁRIOS ====================

    public List<Usuario> listarUsuarios() throws Exception {
        String url = buildUrl("/api/usuarios");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return Arrays.asList(objectMapper.readValue(response.body(), Usuario[].class));
        } else {
            throw new Exception("Erro ao listar usuários: " + response.statusCode() + " - " + response.body());
        }
    }

    public Usuario buscarUsuarioPorId(Long id) throws Exception {
        String url = buildUrl("/api/usuarios/" + id);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Usuario.class);
        } else {
            throw new Exception("Erro ao buscar usuário: " + response.statusCode() + " - " + response.body());
        }
    }

    public Usuario salvarUsuario(Usuario usuario) throws Exception {
        String url = buildUrl("/api/usuarios");
        String json = objectMapper.writeValueAsString(usuario);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Usuario.class);
        } else {
            throw new Exception("Erro ao salvar usuário: " + response.statusCode() + " - " + response.body());
        }
    }

    public Usuario atualizarUsuario(Long id, Usuario usuario) throws Exception {
        String url = buildUrl("/api/usuarios/" + id);
        String json = objectMapper.writeValueAsString(usuario);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Usuario.class);
        } else {
            throw new Exception("Erro ao atualizar usuário: " + response.statusCode() + " - " + response.body());
        }
    }

    public void deletarUsuario(Long id) throws Exception {
        String url = buildUrl("/api/usuarios/" + id);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new Exception("Erro ao deletar usuário: " + response.statusCode() + " - " + response.body());
        }
    }

    // ==================== EMPRÉSTIMOS ====================

    public List<Emprestimo> listarEmprestimos() throws Exception {
        return listarEmprestimosPorCaminho("/api/emprestimos");
    }

    public List<Emprestimo> listarEmprestimosAtivos() throws Exception {
        return listarEmprestimosPorCaminho("/api/emprestimos/ativos");
    }

    public List<Emprestimo> listarEmprestimosAtrasados() throws Exception {
        return listarEmprestimosPorCaminho("/api/emprestimos/atrasados");
    }

    private List<Emprestimo> listarEmprestimosPorCaminho(String caminho) throws Exception {
        String url = buildUrl(caminho);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return Arrays.asList(objectMapper.readValue(response.body(), Emprestimo[].class));
        } else {
            throw new Exception("Erro ao listar empréstimos: " + response.statusCode() + " - " + response.body());
        }
    }

    public Emprestimo buscarEmprestimoPorId(Long id) throws Exception {
        String url = buildUrl("/api/emprestimos/" + id);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Emprestimo.class);
        } else {
            throw new Exception("Erro ao buscar empréstimo: " + response.statusCode() + " - " + response.body());
        }
    }

    public Emprestimo realizarEmprestimo(Long usuarioId, Long livroId) throws Exception {
        String url = buildUrl("/api/emprestimos");
        String json = String.format("{\"usuarioId\":%d,\"livroId\":%d}", usuarioId, livroId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Emprestimo.class);
        } else {
            throw new Exception("Erro ao realizar empréstimo: " + response.statusCode() + " - " + response.body());
        }
    }

    public Emprestimo realizarDevolucao(Long emprestimoId) throws Exception {
        String url = buildUrl("/api/emprestimos/" + emprestimoId + "/devolver");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logCall(url, response);

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Emprestimo.class);
        } else {
            throw new Exception("Erro ao realizar devolução: " + response.statusCode() + " - " + response.body());
        }
    }

    // ==================== CLASSES DE MODELO ====================

    public static class Livro {
        public Long id;
        public String titulo;
        public String autor;
        public String isbn;
        public String categoria;
        public Integer anoPublicacao;
        public Boolean disponivel;
        public Livro() {}
    }

    public static class Usuario {
        public Long id;
        public String nome;
        public String email;
        public String cpf;
        public String tipoUsuario;
        public String dataCadastro;
        public Usuario() {}
    }

    public static class Emprestimo {
        public Long id;
        public Usuario usuario;
        public Livro livro;
        public String dataEmprestimo;
        public String dataDevolucaoPrevista;
        public String dataDevolucaoReal;
        public String status;
        public BigDecimal multaCalculada;
        public Long diasRestantes;
        public Emprestimo() {}
    }
}

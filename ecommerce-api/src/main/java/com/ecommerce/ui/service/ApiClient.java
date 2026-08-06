package com.ecommerce.ui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiClient {
    
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Token JWT da sessão atual, preenchido depois de um login bem-sucedido.
    // Sem ele, todas as rotas (exceto /auth/**) retornam 401.
    private static volatile String authToken;
    private static volatile String ultimoErro;
    
    static {
        // Configurar Jackson para LocalDateTime e BigDecimal
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ============ AUTENTICAÇÃO ============

    public static boolean isAutenticado() {
        return authToken != null && !authToken.isBlank();
    }

    public static void logout() {
        authToken = null;
    }

    /** Retorna a mensagem da última resposta de erro recebida da API. */
    public static String getUltimoErro() {
        return ultimoErro;
    }

    private static void limparUltimoErro() {
        ultimoErro = null;
    }

    /** Extrai mensagens de validação do ErrorResponse retornado pela API. */
    @SuppressWarnings("unchecked")
    private static void registrarErro(HttpResponse<String> response) {
        try {
            Map<String, Object> erro = objectMapper.readValue(response.body(), Map.class);
            Object detalhes = erro.get("detalhes");
            if (detalhes instanceof Map<?, ?> campos && !campos.isEmpty()) {
                ultimoErro = campos.values().stream()
                    .map(Object::toString)
                    .distinct()
                    .reduce((primeiro, proximo) -> primeiro + "\n" + proximo)
                    .orElse(null);
            }
            if (ultimoErro == null || ultimoErro.isBlank()) {
                Object mensagem = erro.get("mensagem");
                ultimoErro = mensagem == null ? "Erro HTTP " + response.statusCode() : mensagem.toString();
            }
        } catch (Exception e) {
            ultimoErro = "Erro HTTP " + response.statusCode();
        }
    }

    private static void registrarErro(Exception e) {
        ultimoErro = "Não foi possível conectar à API: " + e.getMessage();
    }

    /**
     * Faz login e guarda o token JWT para uso em todas as próximas chamadas.
     * @return null em caso de sucesso, ou uma mensagem de erro amigável.
     */
    public static String login(String email, String senha) {
        try {
            Map<String, Object> body = Map.of("email", email, "senha", senha);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<?, ?> resposta = objectMapper.readValue(response.body(), Map.class);
                Object token = resposta.get("token");
                if (token != null) {
                    authToken = token.toString();
                    return null;
                }
            }
            if (response.statusCode() == 400) {
                return "Email ou senha inválidos.";
            }
            return "Falha no login (HTTP " + response.statusCode() + ").";
        } catch (Exception e) {
            return "Não foi possível conectar à API: " + e.getMessage();
        }
    }

    /**
     * Registra um novo usuário. Não faz login automático - depois de registrar,
     * chame login(email, senha) separadamente.
     * @return null em caso de sucesso, ou uma mensagem de erro amigável.
     */
    public static String registrar(String email, String senha, String nome) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("senha", senha);
            body.put("nome", nome);

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/registrar"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                return null;
            }
            if (response.statusCode() == 400) {
                return "Dados inválidos ou email já cadastrado.";
            }
            return "Falha no registro (HTTP " + response.statusCode() + ").";
        } catch (Exception e) {
            return "Não foi possível conectar à API: " + e.getMessage();
        }
    }

    /** Monta um builder de requisição já com o header Authorization, se houver token. */
    private static HttpRequest.Builder authorizedBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path));
        if (isAutenticado()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        return builder;
    }

    /** Extrai a lista de itens de uma resposta paginada ({"content": [...]}) ou de uma lista simples. */
    private static List<?> parseComoLista(String responseBody) throws Exception {
        Object parsed = objectMapper.readValue(responseBody, Object.class);
        if (parsed instanceof List<?> lista) {
            return lista;
        }
        if (parsed instanceof Map<?, ?> mapa && mapa.get("content") instanceof List<?> lista) {
            return lista;
        }
        return List.of();
    }
    
    // ============ CATEGORIAS ============
    
    public static List<?> getCategorias() {
        try {
            HttpRequest request = authorizedBuilder("/categorias/todas")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseComoLista(response.body());
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar categorias: " + e.getMessage());
        }
        return List.of();
    }
    
    public static boolean postCategoria(String nome, String descricao) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("nome", nome);
            body.put("descricao", descricao);
            body.put("ativo", true);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/categorias")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 201 || response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao adicionar categoria: " + e.getMessage());
        }
        return false;
    }
    
    public static boolean putCategoria(Long id, String nome, String descricao) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("nome", nome);
            body.put("descricao", descricao);
            body.put("ativo", true);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/categorias/" + id)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao atualizar categoria: " + e.getMessage());
        }
        return false;
    }
    
    // ============ PRODUTOS ============
    
    public static List<?> getProdutos() {
        try {
            HttpRequest request = authorizedBuilder("/produtos/todos")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseComoLista(response.body());
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar produtos: " + e.getMessage());
        }
        return List.of();
    }
    
    public static boolean postProduto(String nome, BigDecimal preco, Integer estoque, 
                                      Long categoriaId, String descricao) {
        try {
            limparUltimoErro();
            Map<String, Object> body = new HashMap<>();
            body.put("nome", nome);
            body.put("preco", preco);
            body.put("estoque", estoque);
            body.put("categoriaId", categoriaId);
            body.put("descricao", descricao);
            body.put("ativo", true);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/produtos")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                return true;
            }
            registrarErro(response);
        } catch (Exception e) {
            System.err.println("Erro ao adicionar produto: " + e.getMessage());
            registrarErro(e);
        }
        return false;
    }
    
    public static boolean putProduto(Long id, String nome, BigDecimal preco, 
                                     Integer estoque, String descricao) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("nome", nome);
            body.put("preco", preco);
            body.put("estoque", estoque);
            body.put("descricao", descricao);
            body.put("ativo", true);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/produtos/" + id)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao atualizar produto: " + e.getMessage());
        }
        return false;
    }
    
    // ============ CLIENTES ============
    
    public static List<?> getClientes() {
        try {
            HttpRequest request = authorizedBuilder("/clientes/todos")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseComoLista(response.body());
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar clientes: " + e.getMessage());
        }
        return List.of();
    }
    
    public static boolean postCliente(String nome, String email, String cpf, 
                                      String telefone, String endereco) {
        try {
            limparUltimoErro();
            Map<String, Object> body = new HashMap<>();
            body.put("nome", nome);
            body.put("email", email);
            body.put("cpf", cpf);
            body.put("telefone", telefone);
            body.put("endereco", endereco);
            body.put("ativo", true);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/clientes")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                return true;
            }
            registrarErro(response);
        } catch (Exception e) {
            System.err.println("Erro ao adicionar cliente: " + e.getMessage());
            registrarErro(e);
        }
        return false;
    }
    
    public static boolean putCliente(Long id, String nome, String email, String cpf, 
                                     String telefone, String endereco) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("nome", nome);
            body.put("email", email);
            body.put("cpf", cpf);
            body.put("telefone", telefone);
            body.put("endereco", endereco);
            body.put("ativo", true);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/clientes/" + id)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao atualizar cliente: " + e.getMessage());
        }
        return false;
    }
    
    // ============ PEDIDOS ============
    
    public static List<?> getPedidos() {
        try {
            HttpRequest request = authorizedBuilder("/pedidos/todos")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseComoLista(response.body());
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar pedidos: " + e.getMessage());
        }
        return List.of();
    }
    
    public static boolean postPedido(Long clienteId, List<Map<String, Object>> itens) {
        try {
            limparUltimoErro();
            Map<String, Object> body = new HashMap<>();
            body.put("clienteId", clienteId);
            body.put("itens", itens);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/pedidos")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                return true;
            }
            registrarErro(response);
        } catch (Exception e) {
            System.err.println("Erro ao adicionar pedido: " + e.getMessage());
            registrarErro(e);
        }
        return false;
    }
    
    public static boolean putPedido(Long id, String status) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("status", status);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/pedidos/" + id)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao atualizar pedido: " + e.getMessage());
        }
        return false;
    }
    
    // ============ PAGAMENTOS ============
    
    public static List<?> getPagamentos() {
        try {
            HttpRequest request = authorizedBuilder("/pagamentos/todos")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseComoLista(response.body());
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar pagamentos: " + e.getMessage());
        }
        return List.of();
    }
    
    public static boolean postPagamento(Long pedidoId, BigDecimal valor, String metodo) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("pedidoId", pedidoId);
            body.put("valor", valor);
            body.put("metodo", metodo);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/pagamentos")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 201 || response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao adicionar pagamento: " + e.getMessage());
        }
        return false;
    }
    
    public static boolean putPagamento(Long id, BigDecimal valor, String metodo) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("valor", valor);
            body.put("metodo", metodo);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = authorizedBuilder("/pagamentos/" + id)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao atualizar pagamento: " + e.getMessage());
        }
        return false;
    }
    
    public static boolean patchPagamentoStatus(Long id, String status) {
        try {
            HttpRequest request = authorizedBuilder("/pagamentos/" + id + "/status?status=" + status)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao atualizar status do pagamento: " + e.getMessage());
        }
        return false;
    }
    
    // ============ MÉTODO GENÉRICO DE DELEÇÃO ============
    
    public static boolean delete(String endpoint, Long id) {
        try {
            HttpRequest request = authorizedBuilder(endpoint + "/" + id)
                    .DELETE()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 204 || response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Erro ao deletar: " + e.getMessage());
        }
        return false;
    }
}

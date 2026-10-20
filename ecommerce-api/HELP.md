# E-Commerce API + painel web

Projeto de estudo com Spring Boot, Spring Data JPA, Spring Security, JWT e um
front-end responsivo em HTML, CSS e JavaScript puro.

## Como executar

Requisitos: Java 17 ou superior. O Maven Wrapper já está incluído.

```powershell
.\mvnw.cmd spring-boot:run
```

Depois, acesse:

- Painel web: http://localhost:8080/api/
- Swagger: http://localhost:8080/api/swagger-ui.html
- Console H2: http://localhost:8080/api/h2-console

No primeiro acesso, use **Criar conta**. O banco H2 atual é executado em
memória, portanto os dados são reiniciados junto com a aplicação.

## Como o front conversa com o backend

Os arquivos ficam em `src/main/resources/static`. O Spring Boot os publica no
mesmo host da API, e o JavaScript usa `fetch` com URLs relativas, por exemplo:

```javascript
fetch("auth/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email, senha })
});
```

Depois do login, o token JWT é guardado apenas durante a sessão da aba e
enviado nas operações protegidas:

```javascript
headers.Authorization = `Bearer ${token}`;
```

## Testes

```powershell
.\mvnw.cmd test
```

Para empacotar a aplicação:

```powershell
.\mvnw.cmd package
```

## Interface Swing antiga

O painel web é a interface padrão. Para abrir também o cliente Swing antigo,
execute com a variável `APP_UI_SWING_ENABLED=true` em um ambiente gráfico.

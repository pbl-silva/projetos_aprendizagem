# Projetos de aprendizagem

Repositório de projetos de estudo com Java, Spring Boot e interfaces web ou desktop. Cada projeto tem sua própria configuração e pode ser executado separadamente.

| Projeto | Objetivo | Tecnologias principais | Java |
| --- | --- | --- | --- |
| [Sistema Biblioteca](sistema-biblioteca/) | Gerenciamento de biblioteca por API REST | Spring Boot, Spring Data JPA, H2 e Swagger/OpenAPI | 21 |
| [Sistema E-commerce — Pague Mais](sistema-ecommerce/) | Loja web com catálogo, clientes, pedidos e pagamentos | Spring Boot, Spring Security, JWT, JPA, HTML, CSS e JavaScript | 17 ou superior |
| [Sistema Estacionamento](sistema-estacionamento/) | Controle de vagas, entradas, saídas, tarifas e recibos | Spring Boot, Spring Security, JWT, JPA, H2 e Swing | 21 |
| [SPBank](spbank/) | Aplicação bancária de estudo com contas, transferências e PIX | Spring Boot, JPA, MySQL, Flyway, HTML, CSS e JavaScript | 25 |

## Organização

```text
projetos_aprendizagem/
├── sistema-biblioteca/
├── sistema-ecommerce/
├── sistema-estacionamento/
└── spbank/
    ├── spbank-backend/
    └── spbank-frontend/
```

## Executar no Windows

Instale o JDK indicado para o projeto e abra o terminal na pasta correspondente. O Maven Wrapper está incluído nos projetos, portanto não é necessário instalar o Maven separadamente.

Para iniciar o Pague Mais, execute `sistema-ecommerce\iniciar-ecommerce.cmd`. Consulte o [README do e-commerce](sistema-ecommerce/README.md) para configuração, persistência e acesso administrativo.

Para iniciar a API da biblioteca:

```powershell
cd sistema-biblioteca
.\mvnw.cmd spring-boot:run
```

Para iniciar o sistema de estacionamento, abra outro terminal na raiz do repositório:

```powershell
cd sistema-estacionamento
.\mvnw.cmd spring-boot:run
```

No SPBank, o backend fica em `spbank/spbank-backend` e a interface em `spbank/spbank-frontend`. Configure o banco MySQL conforme os arquivos `application*.yml` do backend antes de iniciá-lo com `.\mvnw.cmd spring-boot:run`. Sirva a pasta do frontend por um servidor HTTP local, como o Live Server do VS Code; as origens locais configuradas por padrão usam a porta 5500.

Os backends usam a porta 8080 por padrão. Execute um de cada vez ou configure portas diferentes; ao alterar a porta do SPBank, ajuste também a URL da API no frontend.

## Arquivos gerados

Pastas de compilação (`target/`), logs e dados locais do e-commerce são ignorados pelo Git. Elas podem existir na máquina sem fazer parte do conteúdo publicado no GitHub.

As pastas foram padronizadas como `sistema-biblioteca`, `sistema-ecommerce` e `sistema-estacionamento`. Os identificadores Maven e os pacotes Java existentes foram preservados.

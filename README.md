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

## Arquivos gerados

Pastas de compilação (`target/`), logs e dados locais do e-commerce são ignorados pelo Git. Elas podem existir na máquina sem fazer parte do conteúdo publicado no GitHub.

As pastas foram padronizadas como `sistema-biblioteca`, `sistema-ecommerce` e `sistema-estacionamento`. Os identificadores Maven e os pacotes Java existentes foram preservados.

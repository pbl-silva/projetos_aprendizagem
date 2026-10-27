# SPBank

Laboratório bancário com Java 25, Spring Boot, MySQL e interface HTML/CSS/JavaScript. Inclui autenticação, conta corrente/poupança, planos e gestão de clientes, transferências, Pix, cartões, crédito, investimentos, pagamentos, recargas e agendamentos.

Liquidações, operadoras, bandeiras, cota e CDI são internos e simulados. Investimentos mostram valores brutos, sem tributação ou suitability. O projeto não se conecta a redes bancárias reais.

## Executar localmente

Requisitos: JDK 25 no PATH, Node.js e MySQL 8.0+ ou Docker Compose. Maven é fornecido pelo wrapper.

1. Na pasta `spbank-backend`, copie `.env.example` para `.env` e escolha as senhas locais. Não envie esse arquivo ao Git.
2. Para uma base Docker isolada, execute `docker compose up -d --wait mysql`. A porta padrão é 23307 e os dados ficam no volume do Compose. Se já usa MySQL, configure `DB_URL`, `DB_USER` e `DB_PASSWORD` no `.env`, sem alterar sua base existente.
3. No primeiro terminal, dentro de `spbank-backend`, execute `.\mvnw.cmd spring-boot:run`.
4. No segundo terminal, dentro de `spbank-frontend`, execute `node server.cjs`.
5. Abra http://127.0.0.1:5500. A saúde da API fica em http://127.0.0.1:8080/actuator/health.

Use o cadastro da interface para criar sua conta ou os acessos de estudo já configurados no ambiente local. O cliente confirma operações com sua senha de login; a área gerencial exige credencial gerencial. As contas de estudo têm saldo fictício e permitem exercitar os fluxos financeiros.

O perfil local padrão aplica os catálogos e dados de estudo. O servidor da interface usa apenas módulos nativos do Node e não exige instalação de pacotes. Para parar, use Ctrl+C nos dois terminais e, se necessário, `docker compose stop`; o volume é preservado.

## Fluxos concluídos

- Cartões: instrumentos, distribuição e solicitação de crédito, oferta/aceite, compras, parcelas, faturas, pagamento, exportação e renovação.
- Investimentos: catálogo de fundo/CDB, prévia, aplicação, calendário, ordens pendentes, valorização auditável, resgate parcial/total e vencimento automático.
- Pagamentos: gerador personalizado de boleto, código de barras/linha digitável com DV, cobrança de convênio, prévia, confirmação, comprovante, agendamento e cancelamento.
- Recargas: Escuro, Morto, Tchau e TOM, com produtos de R$ 20, R$ 30 e R$ 60 e celular com DDD validado.
- Pix: chaves, favoritos, envio com prévia, comprovante, agendamentos, recorrência e cancelamento na interface.
- Extrato: filtros próprios para transferências, Pix, cartão, investimentos e pagamentos/recargas, sempre a partir dos lançamentos oficiais.

O fundo cotiza em D+0 útil e credita o resgate em D+1 útil na conta de origem. Pedido fora de dia útil aguarda cotização e revalida a permanência mínima. O CDB só permite liquidez no vencimento; o crédito é automático no próximo dia útil elegível. Senha e chave idempotente são verificadas no servidor. Agendamento não reserva saldo; saldo e elegibilidade são revalidados na execução.

## Banco e atualizações

V1 e V2 mantêm a baseline anterior. V3 acrescenta os resgates, pagamentos e constraints de lançamentos; V4 e V5 acrescentam catálogos locais de recarga e convênio. Flyway aplica as migrações pendentes e Hibernate valida o schema. Não use `flyway repair` para ocultar incompatibilidade e não apague sua base para atualizar. Uma base com versões antigas e checksums divergentes precisa ser analisada antes de qualquer mudança.

Código integral de cobrança e telefone integral não são armazenados nas novas tabelas: persistem apenas máscara e SHA-256. O gerador devolve o boleto uma vez, preenchendo o formulário. Liquidação, débito, histórico e lançamento participam da mesma transação. Falhas técnicas revertem tudo e permitem nova tentativa; falhas de negócio do agendamento terminam em FAILED sem débito.

A cobrança de convênio de estudo usa o código público `81650000000000004000000000000000000000000000`, R$ 40, com vencimento em 2099-12-31. É apenas uma fixture do catálogo local.

## Validação

Na pasta `spbank-backend`:

```powershell
.\mvnw.cmd -B verify
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-null-safety.ps1
```

O segundo comando usa o compilador JDT e as mesmas preferências de nulabilidade do editor; requer a extensão Java do VS Code instalada. Os testes financeiros usam relógio injetado e cobrem saldo, titularidade, senha, duplicidade, idempotência, datas, mínimos e falhas.

Os scripts HTTP e Chrome em `spbank-backend/scripts` são fixtures de integração opcionais e usam exclusivamente a base descartável e as portas indicadas em [docs/conclusao-projeto.md](docs/conclusao-projeto.md). Credenciais e resultados ficam em `target`, ignorado pelo Git. Nunca aponte os testes de falha forçada ou relógio mutável para sua base de uso.

Consulte [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) para os resultados da validação e os documentos de cada etapa.

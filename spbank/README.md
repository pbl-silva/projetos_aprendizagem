# SPBank

Laboratório bancário com Java 25, Spring Boot, MySQL e interface HTML/CSS/JavaScript. Inclui autenticação, conta corrente/poupança, planos e gestão de clientes, transferências, Pix, cartões, crédito, investimentos, pagamentos, recargas e agendamentos.

Liquidações, operadoras, bandeiras, cota e CDI são internos e simulados. Investimentos mostram valores brutos, sem tributação ou suitability. O projeto não se conecta a redes bancárias reais.

## Inicializador Windows

Dê dois cliques em `iniciar-spbank.cmd` na pasta do projeto. Ele verifica Java 25, Node e MySQL, inicia backend e interface em segundo plano e abre o navegador após a API ficar saudável.

O inicializador cria `spbank-backend/.env` automaticamente, se necessário, com a configuração acadêmica:

```properties
DB_USER=spbank
DB_PASSWORD=spbank123
spring.profiles.active=dev
```

O perfil `dev` já configura o banco `spbank` em `localhost:3306`, os dados de estudo, a chave local dos cartões e as origens da interface. Não é necessário preencher essas opções no `.env`. As credenciais são as do laboratório; um `.env` existente é preservado.

Se ainda não criou o usuário `spbank` no seu MySQL, execute uma única vez, na pasta `spbank-backend`, usando um administrador do banco:

```powershell
mysql -u root -p -e "source scripts/preparar-mysql-local.sql"
```

Esse SQL cria o banco e o usuário acadêmico, concede acesso somente ao banco `spbank` e preserva os dados e as credenciais de usuários existentes. O inicializador tenta iniciar o serviço MySQL local quando há uma única instalação parada; se o Windows exigir permissão, execute o inicializador como administrador ou inicie o serviço manualmente.

Como alternativa, com Docker instalado, `iniciar-spbank.cmd -Docker` prepara o MySQL do Compose automaticamente na porta 23307, usando o mesmo `.env` simples.

Use `parar-spbank.cmd` para encerrar somente os processos iniciados pelo inicializador; o MySQL e seus dados são preservados. Os logs ficam em `.runtime/backend.log` e `.runtime/frontend.log`. Uma segunda execução reutiliza a instância que já estiver pronta. Portas ocupadas por outros processos geram uma mensagem e não são liberadas automaticamente.

Opções: `-CheckOnly` verifica os pré-requisitos sem iniciar serviços; `-NoBrowser` inicia sem abrir o navegador.

## Executar localmente

Requisitos: JDK 25 no PATH, Node.js e MySQL 8.0+ ou Docker Compose. Maven é fornecido pelo wrapper.

1. Na pasta `spbank-backend`, copie `.env.example` para `.env`. O exemplo já contém as três linhas acadêmicas acima. Não envie esse arquivo ao Git.
2. No MySQL local, execute a preparação indicada acima uma única vez e mantenha o serviço em execução. Para Docker, prefira o inicializador com `-Docker`, que também seleciona a porta 23307 para o backend. Uma conexão diferente pode ser definida por `DB_URL` no `.env`.
3. No primeiro terminal, dentro de `spbank-backend`, execute `.\mvnw.cmd spring-boot:run`.
4. No segundo terminal, dentro de `spbank-frontend`, execute `node server.cjs`.
5. Abra http://127.0.0.1:5500. A saúde da API fica em http://127.0.0.1:8080/actuator/health.

Use o cadastro da interface para criar sua conta ou os acessos de estudo já configurados no ambiente local. O cliente confirma operações com sua senha de login; a área gerencial exige credencial gerencial. As contas de estudo têm saldo fictício e permitem exercitar os fluxos financeiros.

O perfil `dev` aplica os catálogos e dados de estudo. O perfil `default` mantém compatibilidade importando a mesma configuração local. O servidor da interface usa apenas módulos nativos do Node e não exige instalação de pacotes. Para parar a execução manual, use Ctrl+C nos dois terminais e, se necessário, `docker compose stop`; o volume é preservado.

## Fluxos concluídos

A interface usa um menu flutuante centralizado na lateral, com ícones, nomes expansíveis, indicação da tela ativa e preferência de expansão salva. No celular, os atalhos adicionais ficam em “Mais”. As telas usam campos e cartões compactos; Pagamentos separa pagar, recarga, agendamentos e histórico em abas, e solicita a senha após a prévia. As abas também aceitam as setas do teclado, Home e End.

O espaço Pix separa envio, chaves, agendamentos, favoritos e comprovantes. O envio tem duas etapas: dados e revisão. Data, repetição e quantidade aparecem conforme a modalidade escolhida; contatos e chaves ocupam um painel de atalhos no desktop. No celular, o seletor de tema fica no cabeçalho para não cobrir as ações do formulário.

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

## Padrão visual das modalidades

As oito telas usam os componentes de `spbank-frontend/workspace.js` e os estilos de `workspace.css`: largura e cabeçalho comuns, abas, cartões, campos e ações consistentes. Formulários financeiros separam preenchimento e revisão; os dados e a confirmação continuam sendo validados pela API. O menu flutuante e os temas são compartilhados.

A distribuição dos conteúdos segue o mesmo padrão: cartões separam instrumentos, crédito e faturas/operações; investimentos separam produtos, aplicação, posições e resgates; o cadastro separa dados pessoais e segurança. Os cartões atualizam limites, solicitações e faturas após operações confirmadas.

A validação visual e funcional foi executada no Chrome em desktop e nas larguras de 390 e 320 pixels, incluindo temas, navegação por abas e operações financeiras. Os scripts `check-shared-workspaces.cjs` e `check-shared-workspace-flows.cjs` ficam em `spbank-backend/scripts`. São testes opcionais que dependem da fixture autenticada, API em 28083, servidor de teste em 5501 e Chrome com CDP configurável por `SPBANK_CDP_PORT`; usam credenciais e resultados ignorados em `target` e uma base descartável separada do banco local de uso.

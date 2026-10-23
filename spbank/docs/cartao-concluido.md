# Cartão: implementação e validação

Atualizado em 23/10/2026. Continuação das etapas de crédito, distribuição, instrumentos e renovação, conforme o guia FourBank fornecido, especialmente seções 3.5 e 22.

## Funcionalidades entregues

- Compra à vista e parcelada, prévia calculada no servidor, condições STANDARD/PLUS, validação de PAN, validade, CVV e PIN; recusas sem gerar dívida. Repetição idempotente não duplica compras.
- Faturamento por bandeira, fechamento, vencimento nominal e efetivo, ajuste de finais de semana/feriados configuráveis, mudança futura de vencimento e geração de ciclos sem compras.
- Solicitação de estorno e decisão administrativa, estorno parcial ou total, reversão proporcional dos juros originais, parcelas finais primeiro e rastreamento da dívida refinanciada. Valores já pagos viram crédito da mesma bandeira.
- Pagamento por conta própria com senha, preferência de conta sem débito automático e lançamento no extrato. Boleto pendente não quita dívida; a confirmação administrativa registra o recebimento uma única vez.
- Antecipação de parcelas de compras e acordos, desconto calculado pelo servidor e nova conferência do valor esperado na confirmação.
- Multa e mora com condições registradas no episódio de atraso, sem duplicar multa nem arredondamento diário cumulativo indevido.
- Propostas de acordo e renegociação com entrada incluída na quantidade de parcelas. Acordo só é ativado após receber a entrada; proposta vencida ou alterada recebe pagamento avulso e conserva eventual excesso como crédito.
- Reconciliação do saldo credor; pagamentos por conta, inclusive entrada de acordo, exigem sua reconciliação prévia. Crédito não provoca transferência bancária automática.
- Exportação da fatura em CSV e PDF paginado, com saldo, parcelas, encargos e alocações de pagamento.
- Formulários do cliente e do gerente ligados à API, seleção por dados legíveis, prévias e histórico. Logos BRISA, MISTERSHARD e ELU fornecidas pelo usuário integradas; arquivos das operadoras preservados para as próximas funcionalidades.

O estado financeiro usa registros tipados e tabelas relacionais para compras, parcelas, faturas, pagamentos, alocações, estornos, propostas, acordos e encargos. O cliente é bloqueado antes das alterações financeiras; débitos e liquidações participam da mesma transação. O processamento periódico usa uma transação por cliente. PAN/CVV permanecem cifrados no instrumento; PIN é hash. As respostas financeiras e tentativas não contêm as credenciais completas.

## Null type safety

A análise permanece habilitada. Foram corrigidos contratos de filtros e configuração, parâmetros nas fronteiras Spring Data, publicação de eventos, respostas HTTP, referências de métodos e retornos realmente opcionais. JSpecify é dependência explícita. A verificação lê as mesmas preferências do Eclipse JDT usadas pelo editor, sem inventar uma anotação `org.jspecify.annotations.NonNull`.

Produção e testes terminaram com **zero erros e zero diagnósticos de nulabilidade**. Outros avisos do compilador, como serialização, não fazem parte dessa afirmação. O pequeno adaptador de testes `MockitoArguments.nonNullMarker` documenta os marcadores Mockito, usados apenas em chamadas interceptadas pelo mock. A supressão desnecessária desse adaptador foi removida na revisão da lista do editor, assim como 66 imports não utilizados e dois usos do construtor obsoleto de `TransactionInterceptor`.

As preferências do Eclipse JDT agora acompanham o projeto e preservam os nomes dos parâmetros, necessários ao Spring MVC. A biblioteca oficial de anotações Eclipse está no classpath de compilação (`provided`), além de JSpecify. Isso permite que o compilador do editor faça a mesma análise sem gerar classes com referências a anotações ausentes. Se o aviso de alteração do POM continuar no VS Code, executar **Java: Reload Projects** importa o novo classpath.

### Revisão da segunda lista do editor

A lista seguinte continha 218 diagnósticos: 19 avisos de acesso potencialmente nulo e 199 informes sobre interoperabilidade de genéricos sem anotações. A verificação anterior não incluía todas essas categorias; a afirmação de ausência de diagnósticos era abrangente demais.

Na revisão final dos 19 avisos, as asserções encadeadas foram separadas em chamadas independentes. Os testes de erro capturam a exceção, verificam seu tipo e então conferem a mensagem ou o código. Isso remove os acessos aos retornos genéricos `SELF` do AssertJ, sem depender da inferência de `Objects.requireNonNull` para esses retornos. Apenas os dois informes de migração abaixo passaram a ser ignorados nas preferências do projeto, pois JDK, Mockito e AssertJ não têm contratos de nulabilidade completos para esses genéricos:

```properties
org.eclipse.jdt.core.compiler.problem.nonnullTypeVariableFromLegacyInvocation=ignore
org.eclipse.jdt.core.compiler.problem.annotatedTypeArgumentToUnannotated=ignore
```

Os avisos de desreferência nula, variáveis de tipo livre e conversão não verificada continuam ativos. Não foi adicionada supressão em classes ou métodos. O script passou a procurar diagnósticos pela opção e pela mensagem, além dos erros de compilação, e a ler diretamente `.settings/org.eclipse.jdt.core.prefs`. Um controle negativo separado introduz deliberadamente três problemas: retorno nulo incompatível, desreferência nula e desreferência de variável de tipo livre. A verificação só passa se detectar os três. Produção e testes passaram com essa configuração; logs da revisão final em `target/remaining-problems-analysis.log` e `target/remaining-problems-maven.log`.

Para repetir, na pasta `spbank-backend`:

```powershell
.\mvnw.cmd -B test
.\mvnw.cmd -B dependency:get '-Dartifact=org.eclipse.jdt:org.eclipse.jdt.annotation:2.3.0'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-null-safety.ps1
```

O script usa Java 25, o compilador instalado pela extensão Java do VS Code e o JAR oficial de anotações Eclipse no cache Maven. Também aceita `-CompilerJar` e `-AnnotationJar`. Gera relatórios XML, logs e `problems.json` em um diretório novo sob `target/null-safety-*`.

## Evidências da validação

- **179 testes Maven**, sem falhas, erros ou testes ignorados; inclui arquitetura, cenários financeiros, controle de acesso, PIN, renovação e parsing/paginação do PDF e proteção de fórmulas no CSV.
- **MySQL 8.0.46 real**, em diretório descartável sob `target/mysql-card-validation`, porta 23306, base `spbank_cardcheck`. Flyway V1/V2 aplicadas com sucesso, 40 tabelas e validação Hibernate concluída. Essa execução não valida diferenças específicas do MySQL 8.4.
- **41 chamadas HTTP reais**: crédito, distribuição, emissão, bloqueio após três PINs errados, recuperação, compra e repetição, pagamento, estornos, boleto, acordo, antecipação e exportações. Resultado em `target/card-http-result.json`.
- **Chrome real via CDP**: telas cliente/gerente, formulários financeiros, logos carregadas, prévia de compra e limpeza de campos sensíveis, sem exceções JavaScript. O teste isolado anterior de revelação protegida, saída da página e resposta tardia também passou. Resultado e captura em `target/card-browser-result.json` e `target/card-client-browser.png`.
- Análise estática de produção e testes em `target/null-safety-*`; execução final registrada em `target/card-final-null-safety.log`. Maven em `target/card-final-maven.log`.

Os scripts `scripts/validate-card-http.cjs`, `scripts/serve-card-browser.cjs` e `scripts/check-card-browser.cjs` preservam a integração e a conferência visual. O fixture HTTP só aceita o servidor de laboratório em `127.0.0.1:28080` e pressupõe uma base descartável recém-criada com V1/V2 e credenciais de estudo nos arquivos ignorados `target/study-lucas.json` e `target/study-gerente.json`. Ele altera os dados de demonstração; não deve ser apontado para a instalação de uso. O servidor visual usa a sessão temporária gerada pelo fixture e a porta 5500; o verificador usa Chrome com perfil temporário e depuração em 29222.

O esquema segue a orientação do guia de consolidar esta etapa na V1. Uma base já migrada com outra versão da V1 precisa de uma estratégia própria de migração; a base de uso e o serviço MySQL do Windows não foram modificados durante a validação. Os processos temporários utilizados nesta entrega foram encerrados.

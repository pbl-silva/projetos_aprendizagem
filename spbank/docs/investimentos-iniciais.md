# Investimentos — primeira entrega

Implementação dos checkpoints **INV-1 e INV-2** da seção 23 do guia, com a base de calendário e processamento pendente do INV-3.

Esta página registra a primeira entrega. A valorização foi acrescentada posteriormente no INV-4; o contrato atualizado de posições e a validação mais recente estão em [investimentos-rentabilidade.md](investimentos-rentabilidade.md).

## Comportamento

- Catálogo tipado: fundo com cotização D+0 e pagamento de resgate D+1 útil; CDB com 110% do CDI simulado, prazo de 360 dias corridos e liquidez no vencimento. Os mínimos são configuráveis no catálogo; a carga de estudo usa R$ 100 para fundo e R$ 500 para CDB.
- Fundo sem FGC; CDB com elegibilidade informativa. Campos de cotas não existem nas respostas de CDB. Cota e data de referência são fornecidas pela base interna.
- Prévia sem gravação, débito, reserva ou consumo de chave. Valores positivos, centavos exatos, conta própria ativa, produto ativo, mínimo e saldo são revalidados na confirmação.
- Confirmação com senha e chave UUID, lock do cliente e da conta, débito, lançamento `INVESTMENT_APPLICATION`, lote e replay na mesma transação. Mesma chave e intenção repetem a resposta; intenção diferente responde 409. A senha é revalidada inclusive no replay e não entra no fingerprint ou no estado persistido.
- Lotes conservam um retrato dos termos contratados. Fundo guarda quantidade com 12 casas, preço e data da cota; CDB guarda principal e vencimento, sem cotas fictícias.
- Solicitações em dias não úteis ficam `PENDING`, com HTTP 202 e `Location`, sem débito. O calendário consulta feriados fixos e datas exatas em MySQL; a carga possui nove feriados recorrentes. Cada execução do scheduler usa transação própria, revalida condições e efetiva uma única vez. Falha de negócio termina em `FAILED`; falha técnica reverte a transação e permite nova tentativa.
- Tela de investimentos integrada à API: catálogo, prévia, aplicação com senha, confirmação e posições da titularidade. Nenhum cálculo de cotas ou juros é reproduzido no JavaScript. Senha limpa após uso e ao sair da página.

As posições desta entrega mostram **custo de aquisição**, identificado por `valuationBasis=ACQUISITION_COST`. Não representam valor atualizado de resgate. Composição diária de CDI, atualização da rentabilidade, resgates do fundo e vencimento automático do CDB pertencem aos próximos checkpoints INV-4/INV-5. Tributação e suitability permanecem fora do escopo do guia.

## Contratos HTTP

| Método | Rota | Resultado |
|---|---|---|
| GET | `/api/v1/investments/products` | Catálogo ativo com termos e avisos |
| POST | `/api/v1/investments/preview` | Prévia sem efeitos financeiros |
| POST | `/api/v1/investments` | Aplicação concluída (201) ou pendente (202) |
| GET | `/api/v1/investments/positions` | Lotes próprios, ao custo de aquisição |
| GET | `/api/v1/investments/applications/{id}` | Estado de uma aplicação própria |

Prévia recebe `accountId`, `productId` e `amount`. Confirmação acrescenta `confirmationPassword` e o header `Idempotency-Key`. Cliente vem da sessão e é revalidado pelo serviço. IDs não concedem titularidade. Erros do módulo expõem domínio `INVESTMENT`; recurso alheio retorna 404. O filtro de sessão protege todas essas rotas.

## Persistência e arquitetura

O módulo `investimento` possui portas próprias e utiliza somente as portas públicas de autenticação, conta e extrato. Foi acrescentada proteção automática de isolamento entre adapters de todos os módulos de negócio, inclusive cartão e investimento.

As seis tabelas novas são `produtos_investimento`, `cotas_investimento`, `feriados_investimento`, `aplicacoes_investimento`, `posicoes_investimento` e `operacoes_investimento`. Há checks de produto/lote coerentes com o tipo, valores positivos, estados, JSON e fingerprint, unicidade de lote por aplicação e de chave por cliente, índices de execução e chave estrangeira composta de conta/titular. V1 contém o schema; V2 contém os produtos e feriados de estudo.

Conforme o roteiro de reconstrução do guia, esta etapa foi consolidada em V1/V2. Uma instalação já migrada com outra V1/V2 exige estratégia de migração antes de usar o novo schema; a base de uso não foi recriada durante os testes.

## Validação concluída

- **206 testes Java**, sem falhas, erros ou testes ignorados; inclui 18 testes do serviço de investimentos, 8 de controller e a nova regra de arquitetura. Confirmam contratos separados, centavos, prévia sem efeitos, senha, titularidade, snapshots, replay, fim de semana/feriados e execução pendente sem duplicidade.
- Análise do Eclipse JDT com as mesmas preferências do editor: sem erros ou diagnósticos de nulabilidade configurados. O controle negativo continua detectando as três violações intencionais. Nenhuma nova supressão foi acrescentada.
- **MySQL 8.0.46 real**, base descartável `spbank_investmentcheck`, porta 23306, diretório sob `target`. Flyway V1/V2 aplicadas desde schema vazio, Hibernate validado, health `UP`, 46 tabelas e nove feriados. Não foram verificadas diferenças específicas do MySQL 8.4.
- **24 chamadas HTTP** na validação principal: catálogo, campos inaplicáveis ausentes, prévia, isolamento, centavos, senha, replay, conflito, extrato e replay concorrente da mesma chave.
- Duas confirmações concorrentes com chaves diferentes e saldo insuficiente para ambas: uma aplicação aceita e outra recusada, com um débito e saldo não negativo.
- Falha técnica real no banco: trigger temporária recusou o INSERT no extrato. Conta, lote e extrato permaneceram intactos; após remover a trigger, repetir a mesma chave efetivou uma única aplicação. A trigger foi removida.
- **Chrome real**: catálogo, posições, prévia sem débito, aplicação, conferência de saldo, senha limpa e viewport móvel, sem exceções JavaScript. Captura em `target/investments-browser.png`.

Relatórios: `target/investment-final-maven.log`, `target/investment-null-analysis.log`, `target/investment-http-result.json`, `target/investment-concurrency-result.json`, `target/investment-rollback-result.json`, `target/investment-browser-result.json` e `target/investment-sql-verification.txt`.

## Repetir verificações

```powershell
cd spbank/spbank-backend
.\mvnw.cmd -B test
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-null-safety.ps1
```

Os fixtures de integração usam somente o servidor descartável `127.0.0.1:28081`; `scripts/validate-investment-http.cjs` e `scripts/validate-investment-concurrency.cjs` alteram o saldo da base de estudo e pressupõem credenciais nos arquivos ignorados `target/study-lucas.json` e `target/study-marina.json`. Não os aponte para a base de uso. O fixture de rollback tem fases `before`, `fail` e `retry`, separadas pela instalação e remoção de uma trigger temporária em `lancamentos_conta` da base descartável.

Para a verificação visual, `scripts/serve-investment-browser.cjs` serve a tela com a sessão temporária gerada pelo fixture e `scripts/check-investment-browser.cjs` utiliza Chrome com perfil isolado e depuração em `127.0.0.1:29223`. Os processos temporários da validação foram encerrados ao fim da entrega.

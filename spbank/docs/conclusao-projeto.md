# Conclusão do projeto

Validação realizada em 27/10/2026. A implementação conclui INV-5 e PAG-1/PAG-2/PAG-3 e integra o Pix à interface existente. Os módulos anteriores foram preservados.

## Investimentos

InvestmentRedemptionUseCase oferece prévia, confirmação e histórico por titular. O lote e suas condições originais permanecem imutáveis; resgates_investimento registra quantidades convertidas e créditos. A consulta de posições calcula as cotas e o principal remanescentes e exclui lotes totalmente liquidados.

O fundo recebe o valor bruto solicitado em reais e converte em cotas no servidor pela referência da cotização. Aceita resgate parcial ou total, verifica a permanência mínima e impede dois resgates pendentes no mesmo lote. Fora de dia útil, a ordem aguarda cotização e revalida referência e mínimo na data efetiva. Depois da conversão, o valor fica fixado e é creditado em D+1 útil na conta de origem. Cota futura não é usada antecipadamente.

O CDB não permite resgate antecipado. O processor calcula o valor bruto contratual até o vencimento e credita uma única vez em dia útil elegível. Cliente e conta são revalidados antes do crédito; indisponibilidade da conta impede a movimentação e mantém o crédito recuperável.

As mutações usam bloqueio do cliente, conta bloqueada, transação e lançamento oficial. Senha e intenção idempotente são verificadas no servidor. As chaves de aplicação e resgate não podem ser reutilizadas entre os dois fluxos. Constraints protegem chave e resgate pendente por lote.

## Pagamentos e recargas

O módulo pagamento separa porta de entrada, persistência, liquidação simulada, parsing, normalização, controller e scheduler. O adapter JDBC participa da mesma transação Spring das contas JPA.

O parser valida boleto de 44 dígitos/linha de 47 e convênio de 44/48, incluindo DVs de módulo 10/11. Só uma cobrança ativa do catálogo pode ser paga. O gerador personalizado valida nome e dinheiro, define vencimento em 30 dias e devolve o código uma vez. O banco guarda máscara/hash.

As recargas usam os doze produtos do catálogo: quatro operadoras e três valores. O servidor determina o preço e valida celular brasileiro e DDD; persistem somente máscara/hash. A liquidação gera referência determinística de estudo, sem acesso a rede externa.

A confirmação exige senha, chave UUID, cliente ativo e conta própria ativa. A chave identifica a intenção, incluindo conta, cobrança/produto, telefone por hash e data. A confirmação imediata sem data mantém a mesma intenção mesmo quando o replay ocorre em outro dia. O banco impede a duplicidade de cobrança paga ou ainda agendada.

Agendamentos não reservam saldo. Na execução, o caso de uso abre transação por item, revalida saldo, conta e cobrança e conclui débito, liquidação, histórico e lançamento juntos. Falha de negócio gera FAILED sem débito; falha técnica reverte a transação, preserva SCHEDULED e permite nova tentativa sem interromper o restante do lote. Apenas SCHEDULED pode ser cancelado.

As rotas estão sob /api/v1/payments, incluindo /preview, /study-charges, /scheduled, consulta/cancelamento por ID e /recharges. Consultas e comprovantes recebem o titular da sessão. Erros usam o domínio PAYMENT no contrato comum de problemas.

## Interface

Pagamentos reúne gerador, prévia, revisão com senha, data opcional, cancelamento e comprovantes. Recargas usa as logos fornecidas e seletores separados de operadora e valor. Investimentos oferece resgate e histórico de créditos. Pix permite gerenciar chaves/favoritos, consultar destinatário, enviar, agendar e cancelar ocorrências ou séries. O extrato filtra os lançamentos oficiais por módulo.

O navegador não calcula DV, cota, valor de resgate ou saldo financeiro. Código e telefone não são gravados em storage. Senhas são limpas após a operação, navegação e ocultação da página.

## Evidências

| Verificação | Resultado |
|---|---|
| Maven verify, após recompilar as classes geradas | 274 testes aprovados; pacote executável produzido |
| Testes dos novos serviços e parsers após ajustes finais | 40 testes aprovados |
| Análise JDT de produção e testes | Sem diagnósticos de nulabilidade; controle negativo aprovado |
| MySQL vazio | Cinco migrações aprovadas, Hibernate validado, health UP |
| HTTP principal | 57 verificações, incluindo concorrência e vencimento do CDB |
| Limites financeiros | Convênio, insuficiência de saldo, cancelamento sem lançamento e crédito do resgate remanescente aprovados |
| Rollback imediato | Erro de INSERT reverteu débito, pagamento, lançamento e chave; mesma chave concluiu na nova tentativa |
| Rollback agendado | Item com erro técnico permaneceu recuperável, outro item concluiu e nova tentativa debitou uma única vez |
| Chrome | Boleto, recarga, agendamento/cancelamento, Pix, resgate, logos e filtro do extrato; zero exceções e largura móvel de 390 px |

Os resultados locais estão em spbank-backend/target/completion-*.json, scheduled-retry-result.json, final-verify.log, final-financial-check.log e null-safety-*. Não são versionados porque incluem fixtures e dados locais. Docker Compose foi fornecido como opção de inicialização; a validação do banco usou MySQL nativo 8.0.46.

## Reproduzir a integração opcional

Os testes unitários e o pacote rodam com mvnw.cmd -B verify. Para a análise do editor, execute scripts/check-null-safety.ps1, conforme o README.

Os scripts de integração pressupõem MySQL descartável em 127.0.0.1:23306, base spbank_finalcheck, API em 127.0.0.1:28083, interface em 127.0.0.1:5500 e Chrome isolado com CDP em 127.0.0.1:29225. Antes de criar triggers ou avançar datas, confira o datadir: as validações usaram exclusivamente spbank-backend/target/mysql-card-validation.

1. Prepare target/study-lucas.json e target/study-marina.json com os corpos de login dos clientes locais correspondentes. Nunca envie esses arquivos ao Git.
2. Compile scripts/InvestmentValuationValidationApplication.java separadamente, usando as dependências de target/jdt-classpath.txt. Essa fixture não faz parte do JAR. Inicie com --validation.clock-file apontando para um arquivo dentro de target, mais a configuração da base descartável.
3. Fixe o relógio em 2026-10-27T15:00:00Z e rode node scripts/validate-completion.cjs initial.
4. Avance para 28/10, aguarde os processors e rode node scripts/validate-completion.cjs day2. Os delays são configuráveis por spbank.payment.processing-delay-ms e spbank.investment.redemption.processing-delay-ms; a integração usou 1000 ms, preservando 60000 ms como padrão. Os initial-delay-ms correspondentes também podem ser ajustados.
5. Rode validate-completion-boundaries.cjs prepare, avance para 29/10 e rode settled. O script verifica falha por saldo e cancelamento sem lançamentos.
6. Para rollback imediato, rode validate-payment-rollback.cjs prepare, crie um trigger de falha de INSERT apenas nessa base, rode fail, remova o trigger em um bloco finally e rode retry.
7. Para o scheduler, rode validate-scheduled-retry.cjs prepare antes de 31/10, force falha apenas para o blockedId gerado e avance para 31/10. Rode fail, remova o trigger e rode retry. O outro item deve ter sido concluído independentemente.
8. Avance para 2027-10-22T15:00:00Z, aguarde o vencimento e rode validate-completion.cjs maturity.
9. Para o Chrome, inicie serve-completion-browser.cjs e rode check-completion-browser.cjs. A fixture autentica novamente e prepara um lote de fundo, se necessário. Ela injeta a sessão apenas na página local de validação.

As fases são sequenciais e movimentam dinheiro fictício. Use base nova para uma execução completa. V3–V5 são incrementais; nenhuma base de uso foi apagada e nenhum checksum foi ocultado com repair. Ao encerrar, remova os triggers e pare somente os processos isolados criados para a validação.

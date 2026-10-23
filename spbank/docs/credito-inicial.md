# Crédito inicial do cartão

Esta entrega continua a seção 22 do guia FourBank, adaptada ao pacote `br.com.spbank`. Completa o fluxo de solicitação, decisão gerencial, oferta/rejeição, cancelamento e aceite com senha descrito nas regras RN-CAR-003 a RN-CAR-008. O ponto de partida é o commit `a160872`, obtido com `git pull` antes das alterações.

A base atual já tinha controllers, DTOs, entidades JPA, duas interfaces de repositório e tabelas de crédito na V1. Ela não compilava porque faltavam os modelos, as portas, o serviço e o repositório do total aprovado. Os arquivos existentes foram aproveitados.

## Como o fluxo funciona

1. O cliente consulta seu plano, teto, crédito aprovado e histórico em `GET /api/v1/cards/credit`. Os tetos STANDARD de R$ 5.000 e PLUS de R$ 20.000 não concedem crédito automaticamente.
2. O cliente solicita um **novo total**, superior ao já aprovado e dentro do teto vigente, em `POST /api/v1/cards/credit/requests`, com `requestedTotal`, `reason` e uma `Idempotency-Key` UUID.
3. O gerente consulta `GET /api/v1/admin/card-credit/requests` e decide em `POST /api/v1/admin/card-credit/customers/{customerId}/requests/{requestId}/decision`. A decisão `OFFER` inclui `offeredTotal` e motivo; `REJECT` inclui motivo e não inclui valor ofertado. Uma oferta pode ser menor que o solicitado.
4. O cliente aceita em `POST /api/v1/cards/credit/requests/{requestId}/accept`, enviando `confirmationPassword` e uma chave UUID. Somente esse passo altera o crédito aprovado. A oferta vale sete dias corridos e deixa de valer exatamente no instante de expiração. O teto do plano é revalidado no aceite.
5. O cliente pode cancelar uma solicitação em análise ou uma oferta vigente em `POST /api/v1/cards/credit/requests/{requestId}/cancel`, com uma chave UUID. O histórico é preservado.

Os endpoints de cliente usam a sessão de cliente; os administrativos usam a sessão gerencial, pelos filtros já existentes. A identidade vem da sessão, não do corpo da requisição.

## Responsabilidade dos arquivos adicionados

Os caminhos abaixo são relativos a `spbank-backend/src/main/java/br/com/spbank/cartao/`.

| Arquivo | Responsabilidade |
| --- | --- |
| `application/model/CreditDecision.java` | Define as decisões OFFER e REJECT. |
| `application/model/CreditRequestStatus.java` | Define os estados da solicitação. |
| `application/model/CreditRequest.java` | Representa a solicitação e calcula vigência, encerramento e expiração. |
| `application/model/CreditOverview.java` | Reúne plano, teto, total aprovado e histórico. |
| `application/model/CreditResult.java` | Guarda a resposta imutável de cada operação, com ator e instante. |
| `application/model/CreditOperation.java` | Associa chave e fingerprint à resposta original. |
| `application/port/in/CardCreditUseCase.java` | Expõe os casos de uso aos controllers existentes. |
| `application/port/out/CardCreditPersistence.java` | Define as operações de persistência implementadas pelo adapter MySQL existente. |
| `application/usecase/CardCreditService.java` | Coordena validações, autenticação, histórico, idempotência e atualização do crédito. |
| `adapter/out/persistence/mysql/repository/CustomerCreditJpaRepository.java` | Completa o acesso JPA à tabela `credito_clientes`. |

Também foram acrescentadas a `messages.properties` as mensagens de crédito previstas no guia, para que as recusas de negócio sejam traduzidas pela API.

O serviço usa uma transação e bloqueia o cliente por meio de `findCustomerByIdForUpdate`, já utilizado na base. O bloqueio serializa alterações de crédito do mesmo cliente, inclusive com mudanças de plano. As constraints existentes reforçam a unicidade da solicitação aberta e da chave idempotente. O resultado original fica registrado para repetição da mesma intenção; uma intenção diferente com a mesma chave é recusada. Senha não entra no fingerprint nem no resultado persistido.

A consulta apresenta ofertas vencidas como EXPIRED. Antes de abrir uma nova solicitação, o serviço persiste esse encerramento para liberar a constraint de solicitação aberta. Não há um scheduler novo de expiração nesta etapa.

## Validação

Com Java 25, executar em `spbank/spbank-backend`:

```powershell
.\mvnw.cmd -B test
```

Resultado desta entrega: **72 testes, nenhuma falha, nenhum erro e nenhum teste ignorado**. Inclui 23 cenários de serviço de crédito adaptados do guia, seis cenários HTTP adicionais e as verificações de arquitetura existentes.

`CardCreditServiceTest` cobre oferta sem concessão automática, aceite, expiração exata de sete dias, revalidação após rebaixamento de plano, senha incorreta, idempotência, conflito, isolamento por cliente, autorização gerencial e preservação do histórico.

`CardCreditHttpTest` usa MockMvc sem servidor e verifica o resumo, cabeçalho obrigatório, valores com frações de centavo, tradução de senha inválida, identidade administrativa e decisão inválida. Esses testes não validam os filtros de sessão de ponta a ponta nem substituem o MySQL.

Não havia MySQL escutando na porta local padrão nem ferramentas MySQL/Docker disponíveis. A inicialização com Flyway/Hibernate, a persistência real, o rollback e a concorrência no MySQL continuam pendentes de validação conforme a seção 25.2 do guia. A suíte atual não inicia o contexto completo da aplicação.

## Continuação do roteiro

Esta entrega conclui a integração inicial da API de aprovação de crédito. A distribuição persistida entre bandeiras e sua interface são a próxima etapa da seção 22; depois vêm instrumentos, compras e faturas. Investimentos e pagamentos seguem as seções 23 e 24. O modelo `CustomerCardCredit` já existente foi preservado e é utilizado no aceite.

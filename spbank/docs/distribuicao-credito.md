# Distribuição do crédito por bandeira

Continuação da seção **22.4** do guia FourBank, com as regras **RN-CAR-009 a RN-CAR-013**. A implementação usa o pacote `br.com.spbank`, as sessões existentes e o modelo `CustomerCardCredit` que já estava no projeto.

## Comportamento

O cliente informa o novo limite total desejado para BRISA, MISTERSHARD e ELU. A operação substitui as três alocações de uma vez. Ela exige senha de login e não pode ultrapassar o total aprovado ou o teto do plano vigente, nem reduzir uma bandeira abaixo do seu valor comprometido.

O plano e a identidade vêm do servidor. Um rebaixamento preserva aprovação, alocações e dívida anteriores; a próxima redistribuição precisa caber no teto atual. Se a dívida comprometida não couber, a distribuição é recusada até que seja possível ajustar as linhas. Os pagamentos que reduzirão esse comprometimento pertencem às próximas etapas de cartão.

Linhas ainda não persistidas aparecem zeradas. A resposta inclui total aprovado, total alocado, total não distribuído e, para cada bandeira, limite, comprometimento, disponibilidade e excesso. Esses valores são calculados no Java, sem descartar comprometimento que ultrapasse o limite.

## API

Consulta autenticada por sessão de cliente:

```http
GET /api/v1/cards/credit/allocation
Authorization: Bearer <token-do-cliente>
```

Substituição da distribuição:

```http
PUT /api/v1/cards/credit/allocation
Authorization: Bearer <token-do-cliente>
Idempotency-Key: <UUID>
Content-Type: application/json

{
  "brisa": 2000.00,
  "mistershard": 1500.00,
  "elu": 500.00,
  "confirmationPassword": "senha-do-cliente"
}
```

O PUT retorna `operationId`, `occurredAt` e `credit`, com os mesmos campos financeiros da consulta. O GET sempre lê o estado atual. Repetir uma chave com os mesmos valores devolve o snapshot original do PUT, mesmo após outras alterações. Repetir a chave com valores diferentes é recusado. Toda repetição continua exigindo a senha correta.

## Arquivos e responsabilidades

Os caminhos Java abaixo são relativos a `spbank-backend/src/main/java/br/com/spbank/cartao/`.

| Arquivos | Responsabilidade |
| --- | --- |
| `application/model/CreditAllocationResult.java`, `CreditAllocationOperation.java` | Snapshot imutável e vínculo com a chave e o fingerprint. |
| `application/port/in/CardCreditAllocationUseCase.java` | Contrato dos dois casos de uso da API. |
| `application/port/out/CardCreditAllocationPersistence.java` | Contrato de persistência das linhas e operações. |
| `application/usecase/CardCreditAllocationService.java` | Bloqueio do cliente, senha, normalização, replay, regra de domínio e transação. |
| `adapter/out/persistence/mysql/data/CardCreditLineData.java`, `CardCreditLineId.java` | Mapeamento da linha com chave composta cliente/bandeira. |
| `adapter/out/persistence/mysql/data/CreditAllocationOperationData.java` | Mapeamento do registro idempotente. |
| `adapter/out/persistence/mysql/repository/CardCreditLineJpaRepository.java`, `CreditAllocationOperationJpaRepository.java` | Consultas isoladas por cliente e chave. |
| `adapter/out/persistence/mysql/CardCreditAllocationMysqlPersistence.java` | Conversão domínio/JPA, gravação das linhas em lote e serialização do snapshot. |
| `adapter/in/api/rest/dto/CreditAllocationDto.java` | Validação das três quantias e senha mascarada em `toString()`. |
| `adapter/in/api/rest/dto/CreditAllocationSummaryDto.java`, `CreditAllocationResultDto.java` | Respostas com os cálculos do servidor. |
| `adapter/in/api/rest/controller/CardCreditAllocationController.java` | GET e PUT, com identidade obtida do filtro de sessão existente. |

O serviço foi adaptado do trecho integral da seção 22.4.3. A validação da entrada também converte mapas incompletos, valores inválidos e valores fora de `DECIMAL(19,2)` em erros de negócio. As classes de persistência e DTOs completam os contratos descritos na seção 22.4; não há dependência entre adapters de módulos diferentes.

O fluxo de solicitação/oferta/aceite e seu adapter permanecem separados da distribuição. O serviço usa o mesmo bloqueio pessimista do cliente e uma única transação para salvar as três linhas e o snapshot. A senha não participa do fingerprint nem do histórico.

## Banco de dados

Conforme a seção **22.4.1**, as tabelas `linhas_credito_cartao` e `operacoes_distribuicao_credito` foram incorporadas ao final da migration canônica **V1**. Elas incluem chave composta, chaves estrangeiras, restrições de valores não negativos e unicidade de chave idempotente por cliente. O comprometimento pode exceder o limite alocado, conforme RN-CAR-013.

**Esta V1 é para schema novo.** Se uma V1 anterior já tiver sido aplicada no seu MySQL, o Flyway detectará mudança de checksum. Não foi executado reset, `repair` nem alteração de banco existente. `repair` sozinho não cria as tabelas. A atualização de uma base com dados exige um procedimento de migração separado; não apague dados para aplicar esta entrega.

## Interface

A opção **Cartão** agora abre o formulário de distribuição, em vez do aviso de API ausente. `spbank-frontend/app.js` consulta plano e teto, exibe os totais e as linhas oficiais, coleta os três valores e pede a senha.

O navegador não calcula saldo, aprovação, disponibilidade ou juros. A chave de idempotência fica somente na memória do formulário e é reutilizada ao repetir os mesmos valores após uma resposta incerta. A senha é limpa imediatamente após a coleta e também ao concluir a chamada; não é salva no estado de repetição nem no armazenamento do navegador. Após sucesso, a tela consulta novamente o estado atual para não apresentar um replay antigo como estado vigente.

Esta etapa não adiciona emissão de cartão, compras, faturas ou telas de solicitação e decisão gerencial de crédito. As APIs de aprovação da entrega anterior continuam disponíveis para preparar o total aprovado.

## Validação executada

```powershell
# Em spbank/spbank-backend
.\mvnw.cmd -B test

# Na raiz do repositório
node --check spbank/spbank-frontend/app.js
```

Resultado: **102 testes Java passaram**, sem falhas, erros ou testes ignorados. Os 30 novos cenários cobrem serviço, contrato HTTP, valores enviados pelo formulário, mascaramento/logging de senha e round-trip JSON com datas e bandeiras. Os testes de arquitetura existentes também passaram.

O Chrome headless executou um formulário real com as funções de produção e respostas de API simuladas. Foram verificados renderização, limpeza de senha, reuso de chave após falha, troca de chave quando os valores mudam, atualização após sucesso e ausência de gravação em localStorage/sessionStorage. O resultado local está em `spbank-backend/target/allocation-browser-result.html`.

Os testes do adapter usam repositórios simulados e um `JsonMapper` real. Não substituem MySQL: Flyway/Hibernate, rollback real e concorrência de transações ainda precisam ser validados em uma instância MySQL, indisponível nesta sessão. A interface também não foi testada de ponta a ponta contra uma API com MySQL real.

## Próxima etapa

Instrumentos de cartão: emissão condicionada a uma linha alocada, ativação e proteção dos dados/PIN, seguindo as etapas próprias da seção 22 e o catálogo da seção 3.5.

# Instrumentos de cartão — ciclo básico

Esta entrega continua a seção **22.4.6** do guia FourBank e implementa o ciclo de **RN-CAR-014 a RN-CAR-025**, com a proteção de dados prevista em RN-CAR-072. As regras são aplicadas no backend Java; a interface apenas apresenta os resultados e envia as intenções do cliente ou gerente.

## O que foi implementado

- Emissão por bandeira somente quando a linha do cliente possui crédito alocado. O cartão começa `PENDING_ACTIVATION`, sem alterar limite ou dívida.
- Ativação com senha de login e PIN confirmado de quatro dígitos ASCII, preservando zeros iniciais. O PIN não pode ser a senha de login.
- Alteração com PIN atual e recuperação com senha de login. Ambas exigem confirmação do novo PIN.
- Bloqueio da verificação após três erros consecutivos. Uma verificação correta zera o contador antes do bloqueio; após o bloqueio, a recuperação por senha é necessária.
- Resumo autenticado com bandeira, final, estado, bloqueios e valores oficiais da linha. Dados completos só aparecem na revelação protegida por PIN.
- Bloqueio voluntário pelo cliente. Somente o gerente pode retirá-lo. Bloqueio gerencial é independente, tem motivo e gera auditoria com responsável e instante.
- Cancelamento definitivo para o instrumento e substituição por um novo cartão pendente, preservando a mesma linha, dívida e bloqueios voluntário/gerencial. Repetir a substituição encontra o sucessor já existente.
- Validade de três anos, até o final do mês indicado, no fuso `America/Sao_Paulo`. Um cartão vencido deixa de aceitar as operações que exigem estado ativo, mas continua listado e pode ser cancelado.

**Renovação automática**, troca atômica de um cartão ainda ativo por seu renovado e persistência automática do estado de expiração são a próxima etapa, RN-CAR-026/027. Nesta entrega a consulta calcula a expiração pelo relógio; não foi criado scheduler de renovação. Compras e faturas também pertencem às etapas seguintes. O resumo ainda não possui dados de fatura, pois esse domínio não está implementado.

## Segurança e transações

O PIN é guardado pelo `PasswordHasher` já existente, com hash salgado PBKDF2. PAN e CVV fictícios ficam cifrados com AES-GCM, nonce aleatório por valor e autenticação separada dos propósitos PAN/CVV. Um HMAC permite garantir unicidade do número sem armazená-lo em texto puro. Os números de estudo usam prefixos fictícios `9901`, `9902` e `9903`; não são instrumentos de uma rede de pagamentos.

DTOs, modelos e credenciais sensíveis possuem `toString()` redigido. O resumo não inclui PAN, CVV, PIN ou hash. A revelação usa POST, recebe o PIN no corpo e responde com `Cache-Control: no-store` e `Pragma: no-cache`.

O serviço bloqueia pessimisticamente o cliente antes das alterações, usando a mesma porta de autenticação das etapas anteriores. O bloqueio serializa o ciclo do instrumento com as operações de crédito que seguem esse contrato. As constraints reforçam um instrumento vigente por cliente/bandeira, um sucessor por cartão anterior e a unicidade do fingerprint do número.

Um erro de PIN precisa salvar o contador mesmo quando a API devolve recusa. Por isso o serviço usa `@Transactional(noRollbackFor = CardPinException.class)`. Somente essa exceção sinaliza a tentativa de PIN e mantém sua gravação; outras falhas de negócio ou técnicas continuam causando rollback. Os testes verificam essa política usando o interceptor real do Spring com um gerenciador de transações simulado. O commit/rollback real no MySQL permanece pendente.

Recuperação de PIN remove somente o bloqueio de PIN. Não retira bloqueio voluntário nem administrativo. Atraso e outros impedimentos financeiros serão integrados nas etapas próprias; a substituição não altera a linha para contorná-los.

## Configuração e banco

`application.yml` exige `spbank.card.data-secret` a partir de `CARD_DATA_SECRET`. O perfil local `default`, que já é usado para o laboratório, fornece um valor demonstrativo quando essa variável não existe. Outros perfis exigem a variável explícita. O adapter recusa valores com menos de 32 caracteres.

Use um segredo longo e exclusivo fora do laboratório e mantenha-o estável para ler os cartões já emitidos. Nesta etapa não existe migração de chaves antigas: trocar o segredo impede a leitura dos envelopes existentes e muda o fingerprint. Nenhum segredo real foi criado, lido ou gravado por esta entrega.

As tabelas `cartoes` e `auditoria_bloqueio_cartao` foram incorporadas à **V1**, conforme o roteiro de base recriável do guia. A V1 atual é para schema novo. Uma base que já aplicou uma V1 anterior terá divergência de checksum e precisa de um procedimento próprio de migração, sem apagar seus dados. Não foi executado reset, `repair` ou atualização de banco existente.

## Contratos HTTP

Todos os endpoints de cliente usam sua sessão existente e obtêm o cliente do filtro de autenticação. Os administrativos exigem sessão gerencial. UUIDs identificam recursos na comunicação com a API; não são digitados nos formulários.

| Método e caminho | Corpo |
| --- | --- |
| `GET /api/v1/cards` | Sem corpo; retorna resumos. |
| `POST /api/v1/cards` | `brand`: BRISA, MISTERSHARD ou ELU. |
| `POST /api/v1/cards/{id}/activate` | `confirmationPassword`, `pin`, `pinConfirmation`. |
| `PUT /api/v1/cards/{id}/pin` | `currentPin`, `newPin`, `pinConfirmation`. |
| `PUT /api/v1/cards/{id}/pin/recovery` | `confirmationPassword`, `newPin`, `pinConfirmation`. |
| `POST /api/v1/cards/{id}/details` | `pin`; retorna nome impresso, PAN, CVV e validade. |
| `POST /api/v1/cards/{id}/block` | `reason`. |
| `POST /api/v1/cards/{id}/cancel` | Sem campos de negócio. |
| `POST /api/v1/cards/{id}/replacement` | Sem campos de negócio. |
| `GET /api/v1/admin/cards/customers/{customerId}` | Sem corpo; retorna resumos. |
| `PUT /api/v1/admin/cards/customers/{customerId}/{id}` | `cause`, `blocked` e `reason`. |

No endpoint gerencial, `cause=ADMINISTRATIVE` permite aplicar ou retirar o bloqueio administrativo; `cause=VOLUNTARY` aceita somente `blocked=false`. A ausência de `blocked` é recusada para não produzir desbloqueios acidentais. Motivos são obrigatórios e limitados a 240 caracteres.

Os caminhos seguem o guia; os nomes dos campos de requisição e o PUT gerencial completam o contrato no padrão existente do SPBank. As operações desta etapa não usam as chaves de idempotência do crédito ou da distribuição. Emissão duplicada é recusada, cancelamento repetido preserva o estado e a substituição repetida devolve o mesmo sucessor. Ativação e mudança de PIN não são reexecutadas automaticamente pela interface após resposta incerta: consulte o estado antes de repetir.

## Organização dos arquivos

Os caminhos Java são relativos a `spbank-backend/src/main/java/br/com/spbank/cartao/`.

| Arquivos | Responsabilidade |
| --- | --- |
| `application/model/CardInstrument.java`, `CardState.java` | Estado do instrumento, validade e alterações imutáveis de PIN/bloqueios. |
| `application/model/CardSummary.java` | Resposta comum sem credenciais. |
| `application/model/CardCredentials.java`, `CardDetails.java` | Transporte de credenciais cifradas e revelação protegida, com logging redigido. |
| `application/model/CardBlockAudit.java` | Responsável, ação, motivo e instante do bloqueio/desbloqueio. |
| `application/exception/CardPinException.java` | Recusa de PIN cuja gravação do contador deve ser mantida. |
| `application/port/in/CardInstrumentUseCase.java` | Casos de uso do cliente e gerente. |
| `application/port/out/CardInstrumentPersistence.java`, `CardCredentialVault.java` | Persistência e proteção de credenciais, sem dependência de adapters. |
| `application/usecase/CardInstrumentService.java` | Regras, autenticação, bloqueio do cliente e coordenação transacional. |
| `adapter/out/security/AesCardCredentialVault.java` | Geração dos números fictícios, AES-GCM e HMAC. |
| `adapter/out/persistence/mysql/data/CardInstrumentData.java`, `CardBlockAuditData.java` | Mapeamento JPA das novas tabelas. |
| `adapter/out/persistence/mysql/repository/CardInstrumentJpaRepository.java`, `CardBlockAuditJpaRepository.java` | Consultas e gravações JPA. |
| `adapter/out/persistence/mysql/CardInstrumentMysqlPersistence.java` | Conversão dos modelos e flush das alterações do instrumento. |
| `adapter/in/api/rest/dto/CardInstrumentDtos.java` | Validação das requisições e redação dos segredos em logs. |
| `adapter/in/api/rest/controller/CardInstrumentController.java`, `AdministrativeCardInstrumentController.java` | API cliente e gerencial. |

`spbank-frontend/app.js` acrescenta os instrumentos à tela Cartão e o botão “Gerenciar cartões” a cada cliente da área gerencial. Os formulários usam nome do cliente, bandeira e final. A distribuição anterior permanece disponível.

A revelação fica apenas no DOM da tela atual. “Ocultar dados”, saída da página, ocultação da aba, encerramento da sessão e navegação removem os dados. Uma resposta tardia de revelação não consegue restaurá-los depois da ocultação. PINs e senhas são limpos ao enviar e ao concluir a chamada; não são colocados em URL, localStorage ou sessionStorage.

## Verificações executadas

```powershell
# Em spbank/spbank-backend
.\mvnw.cmd -B clean test

# Na raiz do repositório
node --check spbank/spbank-frontend/app.js
node spbank/spbank-frontend/tests/generate-instrument-browser-check.cjs
```

**134 testes Java passaram**, sem falhas, erros ou testes ignorados: 32 novos cenários e os 102 anteriores. Foram cobertos emissão, ativação, confirmação/formato do PIN, zeros iniciais, três erros, recuperação, independência dos bloqueios, auditoria, substituição, fronteira de expiração, isolamento de titularidade, cifragem, adulteração de dados, segredo incorreto, DTOs seguros, cache da revelação, mapeamento JPA e política de transação do Spring. Os testes de arquitetura também passaram.

O gerador JavaScript produz `spbank-backend/target/instrument-browser-check.html`, que pode ser aberto no Chrome para executar testes com DOM real e API simulada. O Chrome headless confirmou o resumo mascarado, revelação, limpeza de PIN, ocultação na saída, proteção contra resposta tardia, tratamento do bloqueio e seleção gerencial sem digitação de UUID. O resultado desta sessão está em `spbank-backend/target/instrument-browser-result.html`.

Não havia MySQL disponível: inicialização com Flyway/Hibernate, constraints, concorrência, commit dos erros de PIN e rollback real ainda precisam ser exercitados em MySQL. A verificação no navegador usa respostas simuladas e não substitui o fluxo completo com banco real. Os arquivos em `target` são artefatos locais e podem ser recriados depois de `clean`.

## Continuação

A próxima etapa é **renovação e expiração — seção 22.5.1, RN-CAR-026/027**. Ela acrescentará a seleção dos cartões nos últimos trinta dias, processamento por transação independente, sucessor pendente e substituição atômica na ativação.

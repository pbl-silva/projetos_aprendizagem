# Renovação e expiração dos cartões

Continuação da seção **22.5.1** do guia FourBank, regras **RN-CAR-026 e RN-CAR-027**. A etapa anterior implementou os instrumentos básicos; esta acrescenta seu processamento automático e a ativação do sucessor com substituição atômica do anterior.

## Comportamento

Um cartão `ACTIVE` entra na janela de renovação quando faltam **30 dias corridos ou menos** para a validade, considerando a data de `America/Sao_Paulo`. Com 31 dias restantes, não há renovação. O processamento cria um único sucessor `PENDING_ACTIVATION`, com novas credenciais e PIN ainda não definido. O cliente continua usando a API de ativação já existente, com senha e PIN confirmado.

O original permanece ativo até o fim de sua validade ou a ativação do sucessor. Ao ativar o renovado, o serviço grava o anterior como `REPLACED` antes de ativar o novo, dentro da mesma transação. Se a ativação falhar, ambas as alterações são revertidas. Um cartão já cancelado permanece `CANCELLED`; a ativação de seu substituto não reverte o cancelamento.

Cancelar uma renovação pendente e solicitar sua substituição pode criar uma cadeia de instrumentos. A ativação percorre essa cadeia para substituir o ancestral que ainda estiver ativo, mantendo cancelamentos e histórico. Bloqueios atuais do predecessor vivo são preservados, inclusive se aplicados depois da emissão do renovado. Bloqueios históricos de ancestrais já substituídos não são reaplicados por essa busca.

A renovação usa a mesma linha cliente/bandeira. Não aprova crédito, não modifica limites, não duplica dívida e não exige nova análise gerencial. Pode renovar uma linha que passou a ter alocação zero, porque está estendendo um instrumento existente; a emissão inicial e a substituição manual continuam com suas validações de alocação. A disponibilidade para futuras compras continuará sendo revalidada pelos casos de uso próprios.

Na primeira data posterior à validade, o processamento persiste `EXPIRED` para instrumentos ativos ou pendentes. A validade permanece inclusiva até o fim do mês no fuso do banco. O estado calculado nas consultas já impede operações de cartão vencido mesmo antes de o scheduler persistir a expiração.

Se o processo ficou desligado durante toda a janela e encontra um cartão já vencido sem sucessor, ele materializa a expiração e não emite retroativamente. O instrumento continua no histórico e pode seguir o cancelamento/substituição manual existentes. Não foram criadas operações de pagamento ou estorno nesta etapa; a expiração não apaga os dados de que esses módulos precisarão.

## Scheduler, concorrência e transações

`CardLifecycleProcessor` seleciona até **100 IDs** por lote e chama uma porta de entrada para cada um. Não envolve o lote inteiro em uma transação. Uma falha em um cartão é registrada sem mensagem/stack de credenciais e não impede o processamento do próximo.

O intervalo usa a propriedade Spring `spbank.card.scheduler-delay`, com padrão de **60.000 ms**. O mecanismo `@EnableScheduling` já existente no SPBank foi aproveitado.

Cada chamada de `ProcessCardLifecycleService.process` usa `REQUIRES_NEW`. A ordem dos bloqueios é **cliente → instrumento**, igual às alterações feitas pelo cliente. Antes dos bloqueios, uma consulta escalar obtém somente o proprietário. Assim o instrumento não fica carregado no cache de primeiro nível do Hibernate enquanto o worker aguarda o bloqueio do cliente.

Depois de obter os bloqueios pessimistas, o serviço revalida estado, data e existência de sucessor. A busca por sucessor também usa leitura com bloqueio para enxergar um sucessor confirmado enquanto essa transação aguardava. Isso é relevante para o isolamento padrão do MySQL.

A seleção de candidatos exclui cartões já renovados que ainda não venceram, para que não ocupem o lote repetidamente. Cartões vencidos com sucessor continuam elegíveis para materializar sua expiração. A constraint de sucessor único fornece uma garantia adicional às revalidações do serviço.

## Mudanças no schema

A V1 mantém `uk_cartao_sucessor` e acrescenta o índice `(situacao, valida_ate, id)` para localizar candidatos.

A coluna gerada `instrumento_vigente` foi adaptada: há uma chave única para o cartão **ativo** e outra para o **pendente**, por cliente/bandeira. A restrição anterior permitia apenas um instrumento entre ambos os estados e impedia a coexistência exigida pela renovação. A ativação libera a posição ativa anterior antes de ocupá-la com o novo instrumento.

Conforme o guia, isso permanece na V1 da base recriável. Um MySQL que já aplicou uma V1 anterior precisa de um procedimento de atualização próprio. Não foi executado reset, `repair` ou alteração de banco existente. `repair` não converte a estrutura das tabelas.

## Arquivos completos e responsabilidades

Os caminhos Java são relativos a `spbank-backend/src/main/java/br/com/spbank/cartao/`.

| Arquivo | Papel nesta etapa |
| --- | --- |
| `application/port/in/ProcessCardLifecycleUseCase.java` | Porta para processar um instrumento por vez. |
| `application/usecase/ProcessCardLifecycleService.java` | Renovação e expiração com transação independente e revalidação sob bloqueio. |
| `application/service/CardInstrumentFactory.java` | Construção compartilhada da emissão, substituição e renovação, sem duplicar regras de credenciais/validade. |
| `application/usecase/CardInstrumentService.java` | Ativação atômica com tratamento do predecessor e das cadeias de substituição. |
| `application/port/out/CardInstrumentPersistence.java` | Consultas de proprietário, bloqueio e IDs candidatos. |
| `adapter/out/persistence/mysql/repository/CardInstrumentJpaRepository.java` | Consulta dos candidatos e leituras pessimistas. |
| `adapter/out/persistence/mysql/CardInstrumentMysqlPersistence.java` | Implementação das portas e paginação do lote. |
| `adapter/in/scheduler/CardLifecycleProcessor.java` | Seleção, execução por ID e isolamento de falhas do lote. |

O arquivo `CardInstrumentServiceTest.java` foi ajustado para fornecer a leitura com bloqueio usada pelo serviço. Os arquivos novos de testes são `CardRenewalServiceTest.java`, `CardLifecycleProcessorTest.java` e `CardLifecycleQueryTest.java`.

O contrato HTTP da etapa anterior permanece suficiente: o renovado aparece em `GET /api/v1/cards` e é ativado por `POST /api/v1/cards/{id}/activate`. A tela já apresenta os estados ACTIVE e PENDING_ACTIVATION e o formulário de ativação. Esta entrega não muda o front-end.

## Logos fornecidas

As sete logos foram guardadas em `spbank-frontend/img/marcas/`, com os nomes originais. A comparação SHA-256 confirmou que as cópias são idênticas. O README desse diretório relaciona as três bandeiras e as quatro operadoras. Elas ficam disponíveis para as próximas alterações das telas, conforme o pedido do usuário.

## Validação executada

```powershell
# Em spbank/spbank-backend
.\mvnw.cmd -B test
```

**151 testes passaram**, sem falhas, erros ou testes ignorados. Foram adicionados 17 cenários, além dos 134 anteriores:

- Fronteiras de 31/30 dias e expiração no fim do mês em São Paulo.
- Renovação repetida, estados terminais, expiração de pendentes e linha zerada.
- Ativação com substituição ordenada, preservação de bloqueios e cadeias de cartões cancelados.
- Rollback da ativação pelo interceptor real do Spring, com gerenciador e armazenamento simulados.
- Dois workers concorrentes com mutex de cliente simulado, gerando um único sucessor.
- Propagação `REQUIRES_NEW`, ordem de bloqueio, isolamento das falhas e logs sem credenciais.
- Anotações de bloqueio e análise sintática/tipagem da consulta pelo Hibernate com o mapeamento real de `CardInstrumentData`, sem conexão JDBC.

Os testes de arquitetura existentes também passaram. A análise offline da consulta não verifica os resultados no banco, e a concorrência simulada não comprova o comportamento dos locks do InnoDB. Flyway/Hibernate contra MySQL, constraints, concorrência e rollback reais continuam pendentes porque não há uma instância disponível nesta sessão. Nenhum fluxo HTTP contra banco real foi apresentado como validado.

## Próxima etapa

Compras e parcelamento, a partir de **RN-CAR-028–033**, seguindo o roteiro da seção 22.6, antes de estornos, antecipações e faturas.

# Investimentos — INV-4: rentabilidade bruta

Etapa concluída em 23/10/2026, após INV-1/INV-2 e o processamento de aplicações pendentes.

## Regras e precisão

O fundo é avaliado pelas cotas do lote multiplicadas pela última cota interna datada até a referência. Preço e data de aquisição permanecem intactos; preço/data atuais são campos separados. Uma queda na cota pode produzir rendimento bruto negativo.

O CDB usa exclusivamente o percentual de CDI registrado no lote. Os fatores internos são diários, versionados e organizados por vigência, com base obrigatória de **252 dias úteis**. O fator do produto em cada dia é:

```text
1 + (fator diário do CDI − 1) × percentual contratado / 100
```

Esses fatores são compostos com `BigDecimal` e precisão `DECIMAL128`, sem arredondamento diário para centavos. O valor bruto final usa duas casas e `HALF_EVEN`; rendimento bruto é a diferença para o principal. O dia da aplicação é excluído e a data de referência é incluída. Somente dias úteis, descontados os feriados persistidos, geram rendimento. A composição termina na data contratual de vencimento, mesmo que a consulta ocorra depois.

As vigências de estudo em V2 são fictícias, não representam observações ou previsões de mercado. Nenhum componente do módulo busca cotas ou CDI pela internet. Um teste de arquitetura protege essa restrição.

Se faltar uma cota ou um fator necessário, houver sobreposição de vigências, fator inválido, data inconsistente ou valor fora da capacidade monetária, a posição retorna `DATA_UNAVAILABLE`, com código estável e **sem inventar valor bruto ou rendimento zero**. As outras posições continuam visíveis.

## Contrato, persistência e interface

`GET /api/v1/investments/positions` preserva os campos do lote e acrescenta:

| Campo | Significado |
|---|---|
| `valuationBasis` | `CURRENT_GROSS` |
| `valuationStatus` | `AVAILABLE` ou `DATA_UNAVAILABLE` |
| `referenceDate` | Data da avaliação, no calendário de São Paulo |
| `calculatedThrough` | Data da cota usada ou último dia útil composto |
| `grossValue`, `grossEarnings` | Valor e rendimento brutos; ausentes se indisponíveis |
| `currentUnitPrice`, `currentQuoteDate` | Referência atual do fundo; ausentes no CDB |
| `accruedBusinessDays`, `cdiSources` | Dias compostos e vigências/versionamento usados no CDB |
| `valuationErrorCode` | Motivo da indisponibilidade, quando houver |
| `calculationFingerprint`, `algorithmVersion` | Identificação reproduzível dos dados e da política de cálculo |

Consultas calculam sem escrever nem movimentar dinheiro. Um processor separado registra avaliações por cliente, em transações independentes. `avaliacoes_investimento` preserva a data até a qual houve cálculo, valores e JSON com condições e referências usadas. Mesmos dados/data geram o mesmo fingerprint e identificador; repetir o processor não duplica avaliações. Alterações na referência produzem outro registro, preservando a avaliação anterior. Falhas de um cliente não interrompem o lote dos demais.

O schema foi consolidado em V1, com `fatores_cdi_investimento` e `avaliacoes_investimento`; V2 contém os fatores fictícios. Há checks de base 252, fatores positivos, vigências, valores, estados e JSON, chave de avaliação única e FK composta que preserva titularidade. Uma base já migrada com outra V1/V2 exige estratégia própria de migração. Nenhuma base de uso foi recriada para validar esta entrega.

A tela apresenta valor aplicado, valor e rendimento brutos, referência, último dia calculado e cota atual ou dias de CDI. Valores de aquisição continuam separados dos atuais. Indisponibilidade é explícita. O JavaScript só apresenta dados calculados pela API.

## Validação confirmada

- **234 testes Java aprovados**, sem falhas, erros ou testes ignorados. Incluem 23 testes do cálculo, verificando 252 dias úteis, precisão acumulada, mudanças de versão, feriados, maturidade, perdas, lacunas, sobreposição, limites monetários e fingerprints, além das novas proteções de serviço, controller e arquitetura.
- Análise JDT com as preferências do editor sem diagnósticos de nulabilidade configurados; controle negativo continua detectando as três violações. Nenhuma supressão nova foi acrescentada.
- **MySQL 8.0.46 real**, em `spbank_inv4check`, com V1/V2 aplicadas desde schema vazio, Hibernate validado, health `UP` e 48 tabelas. Diferenças específicas de MySQL 8.4 não foram verificadas.
- **33 chamadas HTTP** no fixture INV-4. O relógio exclusivo de validação foi avançado de 23 para 27/10/2026; o relógio de produção não foi alterado.
- Fundo de R$ 100,00: dez cotas adquiridas a R$ 10,00; cota de referência de R$ 10,75; valor bruto de R$ 107,50 e rendimento de R$ 7,50.
- CDB de R$ 500,00: dois dias úteis, 110% dos fatores internos; valor bruto de R$ 500,50. Com mudança de vigência, R$ 500,58. Remover uma referência necessária resultou em indisponibilidade sem valores fictícios; restaurá-la recuperou a avaliação.
- A consulta de outra titularidade não devolveu os lotes. Leituras concorrentes repetiram os resultados. Saldo bancário e extrato permaneceram com apenas os dois débitos das aplicações originais.
- O processor preservou seis avaliações, incluindo referências anteriores e o estado indisponível. Repetições sem novos dados mantiveram a contagem de registros.
- **Chrome real**, em desktop e viewport móvel, confirmou valores, datas, indisponibilidade e saldo intacto, sem exceções JavaScript. Capturas em `target/inv4-browser-available.png` e `target/inv4-browser-missing.png`.

Relatórios: `target/inv4-final-maven.log`, `target/inv4-null-analysis.log`, `target/inv4-http-result.json`, `target/inv4-sql-verification.txt`, `target/inv4-browser-available.json` e `target/inv4-browser-missing.json`.

## Reprodução e próximo checkpoint

```powershell
cd spbank/spbank-backend
.\mvnw.cmd -B test
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-null-safety.ps1
```

Os scripts `InvestmentValuationValidationApplication.java` e `validate-investment-valuation.cjs`, em `scripts`, são fixtures exclusivos do laboratório. O runner é compilado fora de `src/main`, sob `target/inv4-runner`, e fornece um `Clock` primário lido de `target/inv4-clock.txt`. Ele não entra no artefato da aplicação. O fixture usa somente `127.0.0.1:28082`, base descartável `spbank_inv4check` e credenciais de estudo nos arquivos ignorados `target/study-lucas.json` e `target/study-marina.json`.

As fases `create`, `advance`, `revised`, `missing` e `restored` pressupõem, respectivamente, aplicações em 23/10, relógio de 27/10 com cota 10,75, vigências sem sobreposição para os dias 26 e 27, remoção e restauração do fator do dia 27. O fixture modifica apenas dados de estudo. O servidor visual usa `scripts/serve-investment-valuation-browser.cjs`; o verificador usa CDP em `127.0.0.1:29224`.

Os processos temporários foram encerrados após a validação. **INV-5 permanece como próximo checkpoint:** resgates de fundo, saldo mínimo remanescente e crédito automático do CDB no vencimento. A valorização desta etapa não credita rendimentos em conta nem realiza resgates. Tributação e suitability continuam fora do escopo fornecido.

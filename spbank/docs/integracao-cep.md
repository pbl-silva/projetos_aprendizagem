# Integração de CEP com o ViaCEP

## Objetivo

O SPBank usa o ViaCEP como auxílio no cadastro e na edição do perfil. Ao sair do campo CEP, a interface solicita ao backend o endereço correspondente e pode preencher rua, bairro, cidade e estado. Número e complemento não são substituídos, pois dependem do endereço informado pelo cliente.

A consulta não valida a existência do endereço para permitir o cadastro. Se o provedor estiver indisponível ou não localizar o CEP, o usuário continua podendo preencher todos os campos manualmente.

O formato da consulta externa segue a [documentação oficial do ViaCEP](https://viacep.com.br/).

## Fluxo

1. A interface remove a máscara e verifica se o CEP contém oito dígitos.
2. O navegador chama o endpoint do próprio SPBank.
3. O backend consulta o ViaCEP ou reutiliza uma resposta válida do cache.
4. A interface preenche somente os campos que o usuário não alterou enquanto aguardava a resposta.
5. O usuário confere o endereço, informa o número e pode ajustar qualquer campo antes de salvar.

O navegador não chama o ViaCEP diretamente. Isso mantém timeout, formato, cache e tratamento de falhas sob responsabilidade do backend.

## Endpoint do SPBank

```http
GET /api/v1/address/postal-codes/01001000
```

O parâmetro deve conter exatamente oito números. A interface aceita `01001-000`, remove a máscara e envia `01001000`.

Exemplo de resposta encontrada:

```json
{
  "status": "AVAILABLE",
  "postalCode": "01001000",
  "street": "Praça da Sé",
  "complement": "lado ímpar",
  "district": "Sé",
  "city": "São Paulo",
  "state": "SP",
  "source": "ViaCEP",
  "consultedAt": "2026-11-10T18:00:00Z",
  "notice": "Endereço localizado. Confira os dados e informe o número."
}
```

Estados possíveis:

| Estado | Significado |
|---|---|
| `AVAILABLE` | O ViaCEP retornou um endereço válido. |
| `NOT_FOUND` | O CEP possui oito dígitos, mas não foi encontrado. |
| `UNAVAILABLE` | A fonte externa falhou, excedeu o tempo limite ou devolveu uma resposta inválida. |

Nos estados `NOT_FOUND` e `UNAVAILABLE`, os campos de endereço voltam vazios e `notice` orienta o preenchimento manual. A resposta HTTP continua sendo `200`, pois indisponibilidade do auxílio externo não torna o cadastro inválido.

## Acesso e segurança

A consulta é pública porque precisa funcionar antes da criação da conta. O filtro de sessão libera somente `GET /api/v1/address/postal-codes/{cep}` quando `{cep}` possui exatamente oito dígitos. Outras rotas, métodos ou formatos não recebem essa exceção de autenticação.

O endereço-base é configurado no servidor, precisa usar HTTPS fora dos testes locais e não pode ser enviado pelo usuário. O backend também limita o corpo da resposta externa, confere se o CEP devolvido corresponde ao solicitado e rejeita cidade ou UF inválidas.

O cache em memória possui quantidade máxima de entradas para evitar crescimento sem limite. Ele reduz chamadas repetidas e segue estes prazos padrão:

| Resultado | Validade padrão |
|---|---:|
| CEP encontrado | 24 horas |
| CEP não encontrado | 5 minutos |
| Falha temporária | 1 minuto |

## Configuração

Todos os valores já possuem padrões e são opcionais no arquivo `.env`:

| Variável | Padrão | Finalidade |
|---|---|---|
| `POSTAL_CODE_BASE_URL` | `https://viacep.com.br/ws/` | Endereço-base do provedor. Deve terminar com `/`. |
| `POSTAL_CODE_CONNECT_TIMEOUT` | `2s` | Limite para abrir a conexão. |
| `POSTAL_CODE_REQUEST_TIMEOUT` | `3s` | Limite total da requisição. |
| `POSTAL_CODE_SUCCESS_CACHE_TTL` | `24h` | Cache de CEP encontrado. |
| `POSTAL_CODE_NOT_FOUND_CACHE_TTL` | `5m` | Cache de CEP inexistente. |
| `POSTAL_CODE_FAILURE_CACHE_TTL` | `1m` | Cache de falha temporária. |
| `POSTAL_CODE_MAX_CACHE_ENTRIES` | `1000` | Máximo de CEPs mantidos em memória. |

Não é necessário configurar uma chave de API para usar o ViaCEP.

## Como testar manualmente

1. Inicie backend e interface normalmente.
2. Abra **Criar cadastro** ou **Perfil**.
3. Informe `01001-000` no campo CEP e saia do campo.
4. Confirme o preenchimento de Praça da Sé, Sé, São Paulo e SP.
5. Informe o número e revise os dados antes de salvar.

Para verificar a contingência, use um CEP válido no formato, mas inexistente, ou execute o backend sem acesso ao provedor. A interface deve exibir a orientação e continuar permitindo o preenchimento manual.

## Validação automatizada

Os testes cobrem mapeamento da resposta do ViaCEP, CEP inexistente, resposta com CEP divergente, cache positivo, cache negativo, falha temporária e a regra de acesso público restrita ao formato válido. Na validação de 10/11/2026, a suíte completa terminou com **287 testes, zero falhas, zero erros e zero ignorados**.

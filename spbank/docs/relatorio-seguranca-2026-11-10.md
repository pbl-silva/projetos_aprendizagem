# Relatório de segurança do SPBank

Data da revisão: **10/11/2026**

Escopo: código versionado do SPBank, dependências Maven de produção, configurações locais e automações do GitHub.

## Resumo executivo

O risco geral observado é **moderado**. A consulta à OSV não identificou vulnerabilidades conhecidas nos 96 componentes Maven de produção presentes no SBOM após a atualização para Spring Boot 4.1.2. A suíte completa também passou com 287 testes.

Foram identificados dois riscos médios de aplicação e dois pontos de atenção de menor severidade. Nenhum deles demonstra exploração ou vazamento atual, mas os riscos médios devem ser tratados antes de considerar o projeto apropriado para exposição pública.

| Severidade | Quantidade | Situação |
|---|---:|---|
| Crítica | 0 | Nenhuma identificada nesta revisão. |
| Alta | 0 | Nenhuma identificada nesta revisão. |
| Média | 2 | Limitação de requisições e proteção do token. |
| Baixa | 1 | Segredos acadêmicos padrão no perfil local. |
| Informativa | 1 | Cobertura NVD incompleta sem chave de API. |

“Nenhuma identificada” não significa garantia de ausência. O resultado depende das bases consultadas, do alcance da revisão e da correspondência correta entre pacotes e avisos publicados.

## Metodologia

- Atualização do parent Maven de Spring Boot 4.1.1 para 4.1.2.
- Execução integral de `mvnw.cmd -B test`: 287 testes, zero falhas, zero erros e zero ignorados.
- Geração de SBOM CycloneDX 1.6 com 96 componentes Maven de produção.
- Consulta em lote dos 96 Package URLs na [API da OSV](https://google.github.io/osv.dev/api/).
- Tentativa de análise com OWASP Dependency-Check 13.0.0 e registro da limitação encontrada.
- Revisão estática direcionada a autenticação, sessões, criptografia, CORS, segredos, consultas, integrações HTTP e renderização dinâmica no frontend.
- Inclusão de Dependabot e CodeQL para análise contínua após o envio ao GitHub.

## Dependências

### Resultado OSV

O SBOM contém **96 componentes de produção**. A consulta realizada em 10/11/2026 retornou **zero avisos conhecidos** para as versões resolvidas.

Esse resultado é uma fotografia da base OSV no momento da consulta. O Dependabot foi configurado para revisar semanalmente Maven, a imagem Docker e as GitHub Actions. Novos avisos podem aparecer depois desta data.

### Limitação da NVD

O OWASP Dependency-Check foi executado, mas a atualização da base NVD falhou porque não havia uma chave válida da API NVD. Como a base local estava vazia, o analisador recusou corretamente continuar e nenhum relatório conclusivo foi produzido por essa ferramenta.

Isso foi tratado como **análise inconclusiva**, não como ausência de vulnerabilidades. Para uma segunda fonte completa, deve-se configurar uma chave da [API NVD](https://nvd.nist.gov/developers/request-an-api-key) como segredo local ou do CI e repetir o comando sem versionar a chave.

## Achados

### SEC-01 — Ausência de limitação nas rotas públicas

Severidade: **média**

Situação: **aberto**

Login, cadastro e consulta pública de CEP não possuem limitação por IP, identidade ou janela de tempo na aplicação. Isso permite tentativas automatizadas de senha, criação abusiva de cadastros e geração de chamadas ao provedor externo com CEPs distintos. O custo do PBKDF2 também pode ampliar consumo de CPU durante tentativas de login em volume.

Controles existentes: respostas de autenticação não distinguem usuário inexistente de senha incorreta; a consulta de CEP aceita somente oito dígitos, possui timeout, cache limitado e cache temporário de falhas.

Recomendação: adicionar limitação por IP e por usuário nas rotas de autenticação, além de um limite separado para cadastro e CEP. Responder com HTTP 429, registrar eventos sem dados sensíveis e testar recuperação após a janela. Em implantação pública, combinar o controle da aplicação com rate limiting no proxy ou gateway.

### SEC-02 — Token bearer acessível ao JavaScript

Severidade: **média**

Situação: **aberto**

Os tokens de cliente e administrador são armazenados em `sessionStorage` e lidos pelo JavaScript para montar o cabeçalho `Authorization`. Esse armazenamento limita a persistência à aba, mas qualquer XSS executado na mesma origem poderia ler e exfiltrar o token. O frontend possui vários pontos de renderização com `innerHTML`; os dados variáveis revisados usam escape em diversos fluxos, porém a quantidade de pontos aumenta a superfície de manutenção.

Recomendação: migrar a sessão para cookie `HttpOnly`, `Secure` e `SameSite`, com proteção CSRF compatível; acrescentar Content-Security-Policy restritiva e reduzir renderizações por `innerHTML` em favor de `textContent` e criação de elementos. Até a migração, toda nova interpolação de dados externos deve passar pelo escape centralizado.

### SEC-03 — Credenciais e chave padrão de laboratório

Severidade: **baixa no escopo local; alta se reutilizada em implantação pública**

Situação: **aceita apenas para desenvolvimento**

O perfil `dev` e o Compose possuem usuário/senha acadêmicos padrão, e o perfil local oferece uma chave padrão para proteção dos dados de cartão. A porta MySQL do Compose está limitada ao loopback, e a documentação identifica o ambiente como laboratório, o que reduz a exposição local.

Recomendação: criar um perfil de produção que falhe na inicialização quando banco, chave de cartão, CORS ou outros segredos não forem informados explicitamente. Nunca reutilizar os padrões acadêmicos fora da máquina de desenvolvimento e manter `.env` fora do Git.

### SEC-04 — Segunda base de CVEs ainda não operacional

Severidade: **informativa**

Situação: **pendente**

A OSV respondeu normalmente, mas a verificação independente pela NVD não pôde ser concluída sem chave de API. Isso reduz a diversidade de fontes desta fotografia inicial.

Recomendação: cadastrar uma chave NVD como segredo e executar o Dependency-Check em rotina programada, mantendo o relatório como artefato privado do workflow quando ele puder conter caminhos internos.

## Controles positivos observados

- Senhas são derivadas com PBKDF2-HMAC-SHA-256, salt aleatório e comparação em tempo constante.
- Tokens de sessão usam 32 bytes de `SecureRandom`; somente o SHA-256 do token é persistido.
- CORS usa lista de origens configurável e não habilita credenciais automaticamente.
- Dados sensíveis de cartão usam cofre criptográfico separado e chave externa à lógica de domínio.
- URLs de BrasilAPI e ViaCEP são configuradas no servidor; as integrações usam HTTPS, timeout, validação de resposta e cache limitado.
- A rota pública do CEP permite somente `GET` com oito dígitos.
- As GitHub Actions do CodeQL estão fixadas por hash de commit, reduzindo risco de alteração de tag.
- Arquivos `.env`, artefatos, logs e resultados locais permanecem ignorados pelo Git.

## Automação adicionada

### Dependabot

Executa semanalmente para:

- dependências Maven em `spbank/spbank-backend`;
- imagem Docker usada pelo backend;
- GitHub Actions do repositório.

Os pull requests ainda precisam passar por revisão e testes antes do merge. Atualizações não são aplicadas diretamente à branch principal.

### CodeQL

Analisa Java com o conjunto `security-extended` em pushes e pull requests da branch `main`, toda segunda-feira e sob execução manual. A primeira conclusão do CodeQL ocorrerá no GitHub após o push deste commit; os resultados devem ser consultados na aba **Security > Code scanning**.

## Limitações

- Não houve teste de penetração dinâmico contra uma implantação pública.
- A imagem MySQL não foi analisada localmente por scanner de contêiner; o Dependabot passa a acompanhar atualizações da referência.
- O banco real não foi submetido a testes de privilégio nesta revisão.
- A NVD não foi consultada de forma conclusiva sem chave.
- CodeQL só produzirá o primeiro resultado depois que o workflow for executado no GitHub.
- Revisão estática e ausência de CVEs conhecidos não provam ausência de falhas lógicas ou vulnerabilidades futuras.

## Prioridades recomendadas

1. Implementar rate limiting para login, cadastro e consulta de CEP.
2. Acompanhar a primeira execução do CodeQL e avaliar cada alerta antes de fechar o relatório.
3. Migrar tokens para cookies protegidos e adicionar Content-Security-Policy.
4. Criar um perfil de produção sem credenciais ou chaves padrão.
5. Configurar uma chave NVD e automatizar o Dependency-Check como segunda fonte.

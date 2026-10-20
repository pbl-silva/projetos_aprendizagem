# Sistema Estacionamento

Painel web para operar o estacionamento: login e cadastro de operadores, entrada de veículos com atribuição automática de vaga, saída com prévia de cobrança e recibo para impressão, cadastro de veículos, mapa de vagas e relatórios com exportação em PDF.

O HTML, CSS e JavaScript são servidos pelo próprio Spring Boot. A interface substitui o cliente desktop Swing e usa a mesma API com autenticação JWT.

## Iniciar no Windows

Com o JDK 21 ou superior instalado, execute `iniciar-estacionamento.cmd`. O iniciador compila a aplicação e abre `http://localhost:8080/api/` no navegador quando o servidor estiver pronto.

A conta inicial de desenvolvimento usa o usuário `admin` e a senha `admin123`. Você também pode criar uma conta de operador na tela de acesso. Para mudar as credenciais iniciais, defina `BOOTSTRAP_ADMIN_USERNAME` e `BOOTSTRAP_ADMIN_PASSWORD` antes da primeira execução. Contas já existentes mantêm suas senhas.

O iniciador salva os dados na pasta local `data/`, ignorada pelo Git. Encerre com `Ctrl+C` no terminal. Para executar junto com o e-commerce, defina `SERVER_PORT=8081` no ambiente do estacionamento; o painel ficará em `http://localhost:8081/api/`.

## Terminal

```powershell
.\mvnw.cmd spring-boot:run
```

Sem o iniciador, a configuração padrão usa H2 em memória: os dados são apagados ao encerrar. Para persistir manualmente, defina `SPRING_DATASOURCE_URL=jdbc:h2:file:./data/estacionamentodb` e `SPRING_JPA_HIBERNATE_DDL_AUTO=update` no ambiente.

## Cobrança

As regras existentes foram mantidas: carro R$ 20,00, moto R$ 15,00 e carro elétrico R$ 19,00 por hora iniciada, com cobrança mínima de uma hora. A modalidade mensal aplica 15% de desconto. A prévia é calculada pelo servidor; o valor definitivo é recalculado ao registrar a saída. A forma de pagamento é registrada para controle, sem integração com operadoras ou bancos.

## Verificação

```powershell
.\mvnw.cmd clean verify
```

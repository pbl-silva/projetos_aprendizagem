# Pague Mais — e-commerce Spring Boot

O front-end HTML/CSS/JavaScript é servido pelo próprio back-end Spring Boot. Assim, não é necessário iniciar dois servidores.

## Início rápido no Windows

Dê dois cliques em `iniciar-ecommerce.cmd`. O script compila o projeto, inicia a API e abre a loja em `http://localhost:8080/api/`.
Os dados ficam persistidos na pasta local `data`, portanto produtos, clientes e pedidos permanecem após reiniciar.

Credenciais administrativas locais usadas pelo script:

- E-mail: `admin@paguemais.local`
- Senha: `Admin@123`

Para usar outras credenciais, defina `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD` antes de executar o script. Contas criadas pela tela pública recebem o papel de cliente e só podem consultar o catálogo, criar pedidos e pagar os próprios pedidos.

## Linha de comando

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, o cadastro administrativo fica desativado fora do iniciador. Para ativá-lo manualmente:

```powershell
$env:APP_ADMIN_ENABLED='true'
$env:APP_ADMIN_EMAIL='admin@paguemais.local'
$env:APP_ADMIN_PASSWORD='troque-esta-senha'
.\mvnw.cmd spring-boot:run
```

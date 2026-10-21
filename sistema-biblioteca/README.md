# Sistema Biblioteca

Interface web para gerenciar o acervo, cadastrar leitores, registrar empréstimos e devoluções e consultar o histórico com exportação em PDF. O frontend HTML, CSS e JavaScript é servido pelo Spring Boot e substitui a interface Swing.

## Abrir no Windows

Execute **Iniciar Biblioteca.exe**, dentro desta pasta. O iniciador confere o Java, compila o projeto e abre `http://127.0.0.1:8082/` quando o sistema estiver pronto. Mantenha a janela do iniciador aberta; **Encerrar** fecha o servidor iniciado por ela.

É necessário ter o **JDK 21 ou superior** no PATH. O Maven Wrapper está incluído. A primeira compilação precisa de internet para baixar dependências. O executável é um iniciador do projeto, não um instalador com Java embutido; mantenha-o junto de `pom.xml`, `mvnw.cmd`, `.mvn` e `src`.

Os dados ficam na pasta local `data/` e permanecem após encerrar o sistema. O iniciador começa com o acervo vazio, pronto para seus cadastros. Os dados de demonstração continuam disponíveis na execução em memória por `mvnw.cmd spring-boot:run`.

Para mudar a porta do iniciador, defina `BIBLIOTECA_PORT`. O arquivo `logs/iniciador.log` registra a abertura e eventuais erros.

## Gerar o executável

O `.exe` é gerado localmente e fica fora do versionamento, assim como os demais arquivos compilados. O código-fonte do iniciador está em `iniciador/BibliotecaLauncher.cs`.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\gerar-iniciador.ps1
```

O script usa o compilador do .NET Framework do Windows. Para verificar a inicialização sem abrir o navegador, execute `& '.\Iniciar Biblioteca.exe' --autoteste`; esse modo usa uma porta livre e um banco separado dentro de `target/`.

## Desenvolvimento

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd verify
```

Na execução direta, o endereço padrão é `http://localhost:8080/` e o banco H2 é em memória. Os leitores comuns podem ter até 3 empréstimos por 7 dias; os premium, até 5 por 14 dias. A API mantém as regras de disponibilidade, devolução e multa.

@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
  echo Java 17 ou superior nao foi encontrado no PATH.
  echo Instale o Java e execute este arquivo novamente.
  pause
  exit /b 1
)

if not defined APP_ADMIN_EMAIL set "APP_ADMIN_EMAIL=admin@paguemais.local"
if not defined APP_ADMIN_PASSWORD set "APP_ADMIN_PASSWORD=Admin@123"
set "APP_ADMIN_ENABLED=true"
if not defined SPRING_DATASOURCE_URL set "SPRING_DATASOURCE_URL=jdbc:h2:file:./data/ecommercedb;MODE=MySQL"
if not defined SPRING_JPA_HIBERNATE_DDL_AUTO set "SPRING_JPA_HIBERNATE_DDL_AUTO=update"

echo Preparando o Pague Mais...
call mvnw.cmd -q -Dmaven.test.skip=true -Djacoco.skip=true package
if errorlevel 1 (
  echo Nao foi possivel compilar a aplicacao.
  pause
  exit /b 1
)

echo.
echo Loja:  http://localhost:8080/api/
echo Admin: %APP_ADMIN_EMAIL%
echo Senha: %APP_ADMIN_PASSWORD%
echo Os dados ficam salvos na pasta data.
echo Para encerrar, pressione Ctrl+C nesta janela.
echo.

start "" powershell.exe -NoProfile -WindowStyle Hidden -Command "$u='http://localhost:8080/api/'; 1..60 | ForEach-Object { try { Invoke-WebRequest -UseBasicParsing -Uri $u -TimeoutSec 1 | Out-Null; Start-Process $u; break } catch { Start-Sleep -Seconds 1 } }"
java -jar "target\ecommerce-api-1.0.0.jar"

endlocal

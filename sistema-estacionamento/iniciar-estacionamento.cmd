@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
  echo Instale o JDK 21 ou superior e execute novamente.
  pause
  exit /b 1
)

if not defined SERVER_PORT set "SERVER_PORT=8080"
if not defined BOOTSTRAP_ADMIN_USERNAME set "BOOTSTRAP_ADMIN_USERNAME=admin"
if not defined BOOTSTRAP_ADMIN_PASSWORD set "BOOTSTRAP_ADMIN_PASSWORD=admin123"
if not defined BOOTSTRAP_ADMIN_ENABLED set "BOOTSTRAP_ADMIN_ENABLED=true"
if not defined SPRING_DATASOURCE_URL set "SPRING_DATASOURCE_URL=jdbc:h2:file:./data/estacionamentodb"
if not defined SPRING_JPA_HIBERNATE_DDL_AUTO set "SPRING_JPA_HIBERNATE_DDL_AUTO=update"

echo Preparando o estacionamento...
call mvnw.cmd -q -DskipTests clean package
if errorlevel 1 (
  echo Nao foi possivel compilar. Confira o JDK e a conexao com a internet.
  pause
  exit /b 1
)

echo.
echo Painel: http://localhost:%SERVER_PORT%/api/
echo Usuario inicial: %BOOTSTRAP_ADMIN_USERNAME%
echo Os dados ficam salvos na pasta data.
echo Para encerrar, pressione Ctrl+C nesta janela.
echo.
start "" /b powershell.exe -NoProfile -WindowStyle Hidden -Command "$u='http://localhost:%SERVER_PORT%/api/'; 1..60 | ForEach-Object { try { Invoke-WebRequest -UseBasicParsing -Uri $u -TimeoutSec 1 | Out-Null; Start-Process $u; break } catch { Start-Sleep -Seconds 1 } }"
java -jar "target\estacionamento-0.0.1-SNAPSHOT.jar"
endlocal

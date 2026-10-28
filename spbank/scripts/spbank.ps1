param([switch]$Docker, [switch]$NoBrowser, [switch]$CheckOnly, [switch]$Stop)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $projectRoot 'spbank-backend'
$frontend = Join-Path $projectRoot 'spbank-frontend'
$runtime = Join-Path $projectRoot '.runtime'
$stateFile = Join-Path $runtime 'processes.json'
$apiUrl = 'http://127.0.0.1:8080'
$siteUrl = 'http://127.0.0.1:5500'

function Read-State {
    if (Test-Path -LiteralPath $stateFile) {
        $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
        if ($state.root -ne $projectRoot) { throw 'Registro de processos pertence a outro projeto.' }
        return @($state.processes)
    }
    return @()
}

function Owned-Process($record) {
    $process = Get-Process -Id $record.id -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -eq 'powershell' -and
        $process.StartTime.ToUniversalTime().ToString('o') -eq $record.started) { return $process }
    return $null
}

function Stop-Owned($records) {
    foreach ($record in $records) {
        if (Owned-Process $record) {
            & taskkill.exe /PID $record.id /T /F | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "Nao foi possivel encerrar $($record.name)." }
        }
    }
}

function Port-Open([string]$hostName, [int]$port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.ConnectAsync($hostName, $port)
        if (-not $connection.Wait(1000)) { return $false }
        return $client.Connected
    } catch { return $false } finally { $client.Dispose() }
}

function Api-Ready {
    try {
        $health = Invoke-RestMethod "$apiUrl/actuator/health" -TimeoutSec 2
        return $health.status -eq 'UP'
    } catch { return $false }
}

function Site-Ready {
    try {
        $response = Invoke-WebRequest $siteUrl -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -eq 200 -and $response.Content.Contains('payments.js')
    } catch { return $false }
}

function Quote-Literal([string]$value) { return "'" + $value.Replace("'", "''") + "'" }

function Start-Component([string]$name, [string]$folder, [string]$command) {
    $log = Join-Path $runtime "$name.log"
    $script = '$ErrorActionPreference = ''Stop''; Set-Location -LiteralPath ' +
        (Quote-Literal $folder) + '; ' + $command + ' *> ' + (Quote-Literal $log) + '; exit $LASTEXITCODE'
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
    $process = Start-Process powershell.exe -ArgumentList @('-NoProfile','-EncodedCommand',$encoded) -WindowStyle Hidden -PassThru
    return [pscustomobject]@{ name=$name; id=$process.Id; started=$process.StartTime.ToUniversalTime().ToString('o') }
}

$created = @()
try {
    $records = @(Read-State)
    if ($Stop) {
        Stop-Owned $records
        if (Test-Path -LiteralPath $stateFile) { Remove-Item -LiteralPath $stateFile }
        Write-Host 'Backend e interface encerrados. O banco de dados foi preservado.'
        exit 0
    }

    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $node = (Get-Command node.exe -ErrorAction Stop).Source
    $javaVersion = (& $java --version | Out-String)
    if ($javaVersion -notmatch '(?m)^(?:openjdk|java) (?:version ")?25[.\s-]') { throw 'Configure o JDK 25 no PATH antes de iniciar.' }
    if (-not (Test-Path -LiteralPath (Join-Path $backend 'mvnw.cmd'))) { throw 'Maven wrapper nao encontrado.' }
    if ($CheckOnly) {
        Write-Host 'OK: Java 25, Node, Maven wrapper e caminhos do projeto.'
        if (-not (Test-Path -LiteralPath (Join-Path $backend '.env'))) { Write-Host 'Configuracao .env ainda precisa ser criada.' }
        exit 0
    }

    if ($records.Count -gt 0) {
        $living = @($records | Where-Object { Owned-Process $_ })
        if ($living.Count -eq 2 -and (Api-Ready) -and (Site-Ready)) {
            Write-Host "SPBank ja esta pronto em $siteUrl"
            if (-not $NoBrowser) { Start-Process $siteUrl }
            exit 0
        }
        Stop-Owned $living
        Remove-Item -LiteralPath $stateFile
    }

    $envFile = Join-Path $backend '.env'
    if (-not (Test-Path -LiteralPath $envFile)) {
        Copy-Item -LiteralPath (Join-Path $backend '.env.example') -Destination $envFile
        Write-Host "Foi criado $envFile"
        Write-Host 'Usando a configuracao academica padrao do SPBank.'
    }
    $settings = @{}
    foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_.-]*)\s*=\s*(.*?)\s*$') {
            $settings[$Matches[1]] = $Matches[2].Trim('"').Trim("'")
        }
    }
    function Setting([string]$name, [string]$fallback) {
        $environmentValue = [Environment]::GetEnvironmentVariable($name)
        if ($null -ne $environmentValue) { return $environmentValue }
        if ($settings.ContainsKey($name)) { return $settings[$name] }
        return $fallback
    }
    if ($Docker) {
        Get-Command docker.exe -ErrorAction Stop | Out-Null
        Push-Location $backend
        try {
            & docker.exe compose up -d --wait mysql
            if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel iniciar o MySQL do Compose.' }
        } finally { Pop-Location }
    }
    $dbHost = '127.0.0.1'
    $fallbackPort = if ($Docker) { '23307' } else { '3306' }
    $dbPort = [int](Setting 'MYSQL_HOST_PORT' $fallbackPort)
    $dbUrl = Setting 'DB_URL' ''
    if ($dbUrl) {
        if ($dbUrl -notmatch '^jdbc:mysql://([^/:?]+)(?::([0-9]+))?/') { throw 'DB_URL deve usar jdbc:mysql://host:porta/banco.' }
        $dbHost = $Matches[1]
        $dbPort = if ($Matches[2]) { [int]$Matches[2] } else { 3306 }
    }
    $backendEnvironment = ''
    if ($Docker -and -not $dbUrl) {
        $database = Setting 'MYSQL_DATABASE' 'spbank'
        $dbUrl = "jdbc:mysql://127.0.0.1:${dbPort}/${database}?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&createDatabaseIfNotExist=true"
        $backendEnvironment = '$env:DB_URL=' + (Quote-Literal $dbUrl) + '; '
    }
    if (-not (Port-Open $dbHost $dbPort) -and $dbHost -in @('localhost','127.0.0.1') -and $dbPort -eq 3306) {
        $services = @(Get-Service -Name '*MySQL*' -ErrorAction SilentlyContinue | Where-Object { $_.Status -eq 'Stopped' })
        if ($services.Count -eq 1) {
            Write-Host "Iniciando o servico $($services[0].Name)..."
            try {
                Start-Service -Name $services[0].Name
                $services[0].WaitForStatus('Running',[TimeSpan]::FromSeconds(20))
                for ($attempt=0; $attempt -lt 20 -and -not (Port-Open $dbHost $dbPort); $attempt++) { Start-Sleep -Seconds 1 }
            } catch { throw 'Para iniciar o servico MySQL, execute iniciar-spbank.cmd como administrador ou inicie o servico manualmente.' }
        }
    }
    if (-not (Port-Open $dbHost $dbPort)) {
        throw "MySQL indisponivel em ${dbHost}:${dbPort}. Inicie seu servico MySQL ou use iniciar-spbank.cmd -Docker para o banco do Compose."
    }
    foreach ($port in @(8080,5500)) {
        if (Port-Open '127.0.0.1' $port) { throw "Porta $port ja esta ocupada. Encerre a instancia anterior antes de iniciar." }
    }
    New-Item -ItemType Directory -Path $runtime -Force | Out-Null
    $created += Start-Component 'backend' $backend ($backendEnvironment + '& ' + (Quote-Literal (Join-Path $backend 'mvnw.cmd')) + " '-B' 'spring-boot:run'")
    $created += Start-Component 'frontend' $frontend ('& ' + (Quote-Literal $node) + ' ' + (Quote-Literal (Join-Path $frontend 'server.cjs')))
    [pscustomobject]@{root=$projectRoot;processes=$created} | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $stateFile -Encoding UTF8
    Write-Host 'Iniciando SPBank. A primeira compilacao pode levar alguns minutos...'
    $deadline = [DateTime]::UtcNow.AddMinutes(5)
    $ready = $false
    while ([DateTime]::UtcNow -lt $deadline) {
        foreach ($record in $created) { if (-not (Owned-Process $record)) { throw "$($record.name) encerrou. Consulte $runtime\$($record.name).log. Se faltar o usuario MySQL, execute spbank-backend/scripts/preparar-mysql-local.sql uma vez com um administrador do banco." } }
        if ((Api-Ready) -and (Site-Ready)) { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw "Tempo limite de inicializacao. Consulte os logs em $runtime" }
    Write-Host "SPBank pronto: $siteUrl"
    Write-Host 'Para encerrar backend e interface, execute parar-spbank.cmd.'
    if (-not $NoBrowser) { Start-Process $siteUrl }
} catch {
    Stop-Owned $created
    if ($created.Count -gt 0 -and (Test-Path -LiteralPath $stateFile)) { Remove-Item -LiteralPath $stateFile }
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if (-not (Test-Path -LiteralPath $compiler)) {
    $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework\v4.0.30319\csc.exe'
}
if (-not (Test-Path -LiteralPath $compiler)) { throw 'O compilador do .NET Framework nao foi encontrado neste Windows.' }
$source = Join-Path $projectRoot 'iniciador\BibliotecaLauncher.cs'
$executable = Join-Path $projectRoot 'Iniciar Biblioteca.exe'
& $compiler /nologo /target:winexe /optimize+ /reference:System.Windows.Forms.dll /reference:System.Drawing.dll "/out:$executable" $source
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel gerar o iniciador.' }
Write-Output "Executavel gerado: $executable"

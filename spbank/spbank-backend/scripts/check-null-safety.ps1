param([string]$CompilerJar, [string]$AnnotationJar, [string]$PreferencesFile = ".settings/org.eclipse.jdt.core.prefs")
$ErrorActionPreference = 'Stop'
$backend = Split-Path $PSScriptRoot -Parent
Push-Location $backend
try {
    if (!$CompilerJar) {
        $CompilerJar = Get-ChildItem "$env:USERPROFILE/.vscode/extensions/redhat.java-*/server/plugins/org.eclipse.jdt.core.compiler.batch_*.jar" |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
    if (!$AnnotationJar) {
        $AnnotationJar = Get-ChildItem "$env:USERPROFILE/.m2/repository/org/eclipse/jdt/org.eclipse.jdt.annotation/*/*.jar" |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
    if (!(Test-Path -LiteralPath $CompilerJar) -or !(Test-Path -LiteralPath $AnnotationJar)) {
        throw 'Informe os JARs oficiais do compilador Eclipse JDT e org.eclipse.jdt.annotation.'
    }
    & .\mvnw.cmd -B dependency:build-classpath '-Dmdep.outputFile=target/jdt-classpath.txt'
    if ($LASTEXITCODE) { throw 'Não foi possível montar o classpath.' }
    $dependencies = (Get-Content target/jdt-classpath.txt -Raw).Trim() + ';' + $AnnotationJar
    $output = Join-Path $backend ('target/null-safety-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $output | Out-Null
    $issues = @()
    foreach ($source in @('main','test')) {
        $argumentFile = Join-Path $output "$source-sources.txt"
        $paths = @(rg --files "src/$source/java" | ForEach-Object { '"' + ((Join-Path $backend $_) -replace '\\','/') + '"' })
        [IO.File]::WriteAllLines($argumentFile,$paths)
        $classpath = if ($source -eq 'test') { "$output/main;$dependencies" } else { $dependencies }
        $report = Join-Path $output "$source.xml"
        # Reuse the editor preferences, including its compatibility policy for unannotated libraries.
        $ErrorActionPreference = 'Continue' # Windows PowerShell represents native stderr as ErrorRecord.
        & java -jar $CompilerJar -25 -proc:none -encoding UTF-8 -parameters -properties $PreferencesFile `
            -log $report -d "$output/$source" -classpath $classpath "@$argumentFile" 2>&1 | ForEach-Object { "$($_)" } |
            Set-Content -Encoding UTF8 "$output/$source.log"
        $ErrorActionPreference = 'Stop'
        [xml]$xml = Get-Content -LiteralPath $report
        $issues += @($xml.compiler.sources.source | ForEach-Object {
            $file = $_.path
            $_.problems.problem | Where-Object { $_.severity -eq 'ERROR' -or $_.optionKey -match 'null' -or $_.message.value -match 'null' } |
                ForEach-Object { [pscustomobject]@{File=$file; Line=$_.line; Id=$_.id; Message=$_.message.value} }
        })
    }
    # Negative control: a clean report is meaningful only if real null violations are detected.
    $canary = Join-Path $output 'NullSafetyCanary.java'
    [IO.File]::WriteAllText($canary, @'
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
class NullSafetyCanary {
    @NonNull String invalidReturn(@Nullable String value) { return value; }
    int invalidDereference(@Nullable String value) { return value.length(); }
    <T> int invalidGenericDereference(T value) { return value.hashCode(); }
}
'@)
    $ErrorActionPreference = 'Continue'
    & java -jar $CompilerJar -25 -proc:none -encoding UTF-8 -properties $PreferencesFile `
        -log "$output/negative-control.xml" -d "$output/negative-control" -classpath $dependencies $canary 2>&1 |
        ForEach-Object { "$($_)" } | Set-Content -Encoding UTF8 "$output/negative-control.log"
    $ErrorActionPreference = 'Stop'
    [xml]$negative = Get-Content "$output/negative-control.xml"
    $detected = @($negative.compiler.sources.source.problems.problem | Where-Object {
        $_.optionKey -match 'nullSpecViolation|potentialNullReference|nullReference|pessimisticNullAnalysisForFreeTypeVariables'
    })
    if ($detected.Count -ne 3) { throw 'A análise não detectou as três violações de nulabilidade do controle negativo.' }
    ConvertTo-Json -InputObject @($issues) -Depth 4 | Set-Content -Encoding UTF8 "$output/problems.json"
    if ($issues.Count) { $issues | Format-Table -AutoSize; throw "$($issues.Count) problemas; relatório em $output" }
    Write-Output "PASS: produção e testes sem erros ou diagnósticos de nulabilidade; controle negativo detectou 3 violações. Relatórios: $output"
} finally { Pop-Location }

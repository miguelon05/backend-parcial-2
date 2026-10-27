param([int]$Puerto = 8080)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:\Program Files\Java\jdk-21')) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
}
$env:PORT = "$Puerto"
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE

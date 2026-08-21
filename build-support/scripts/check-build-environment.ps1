$ErrorActionPreference = "Stop"

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
$mavenWrapper = Join-Path $repositoryRoot "mvnw.cmd"

if (-not (Test-Path $mavenWrapper)) {
    throw "Maven Wrapper not found: $mavenWrapper"
}

Write-Host "=== Java ==="
java -version

Write-Host ""
Write-Host "=== Maven Wrapper ==="
& $mavenWrapper -version
if ($LASTEXITCODE -ne 0) {
    throw "Maven Wrapper failed with exit code $LASTEXITCODE"
}

Write-Host ""
Write-Host "=== JAVA_HOME ==="
Write-Host $env:JAVA_HOME

Write-Host ""
Write-Host "=== JAVA25_HOME ==="
Write-Host $env:JAVA25_HOME

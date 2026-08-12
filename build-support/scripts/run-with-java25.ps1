$ErrorActionPreference = "Stop"

if (-not $env:JAVA25_HOME) {
    throw "JAVA25_HOME is not set."
}

$java25 = Join-Path $env:JAVA25_HOME "bin\java.exe"
if (-not (Test-Path $java25)) {
    throw "Java 25 executable not found: $java25"
}

$jar = Join-Path $PSScriptRoot "..\..\walking-skeleton\ws-smoke-app\target\ws-smoke-app-0.0.1-SNAPSHOT.jar"
$jar = [System.IO.Path]::GetFullPath($jar)

if (-not (Test-Path $jar)) {
    throw "Application jar not found. Build with JDK 21 first: $jar"
}

Write-Host "Running Java 21-targeted artifact on Java 25 runtime..."
& $java25 -version
& $java25 -jar $jar

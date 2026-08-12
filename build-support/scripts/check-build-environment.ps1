$ErrorActionPreference = "Stop"

Write-Host "=== Java ==="
java -version

Write-Host ""
Write-Host "=== Maven ==="
mvn -version

Write-Host ""
Write-Host "=== JAVA_HOME ==="
Write-Host $env:JAVA_HOME

Write-Host ""
Write-Host "=== JAVA25_HOME ==="
Write-Host $env:JAVA25_HOME

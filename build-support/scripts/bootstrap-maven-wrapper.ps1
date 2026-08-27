$ErrorActionPreference = "Stop"

Write-Host "Generating official Apache Maven Wrapper..."
mvn wrapper:wrapper -Dmaven=3.9.16 -Dtype=bin

Write-Host ""
Write-Host "Wrapper generated."
Write-Host "Next:"
Write-Host '  .\mvnw.cmd clean verify'

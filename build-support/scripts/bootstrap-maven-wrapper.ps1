$ErrorActionPreference = "Stop"

Write-Host "Generating official Apache Maven Wrapper..."
mvn wrapper:wrapper -Dmaven=3.9.16 -Dtype=only-script

Write-Host ""
Write-Host "Wrapper generated."
Write-Host "Next:"
Write-Host '  .\mvnw.cmd clean verify'

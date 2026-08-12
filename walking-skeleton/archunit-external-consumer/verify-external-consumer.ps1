$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path "$PSScriptRoot\..\..").Path
$externalPom = Join-Path $PSScriptRoot "pom.xml"

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME must point to JDK 21."
}

Push-Location $repoRoot
try {
    & .\mvnw.cmd `
        -pl koiki-dependencies-bom,koiki-archunit-rules `
        -am clean install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Installing koiki-archunit-rules failed."
    }

    $output = & .\mvnw.cmd -f $externalPom clean test 2>&1
    $externalExitCode = $LASTEXITCODE
    $report = $output -join [Environment]::NewLine

    if ($externalExitCode -eq 0) {
        throw "External Consumer unexpectedly passed despite an internal package violation."
    }
    if (-not $report.Contains("ADR-041")) {
        throw "External failure did not contain the required ADR-041 guidance."
    }
    if (-not $report.Contains("domain.event")) {
        throw "External failure did not contain the required correction guidance."
    }

    Write-Host "KOIKI_ARCHUNIT_EXTERNAL_VALIDATED expectedFailure=true rule=ADR-041"
}
finally {
    Pop-Location
}

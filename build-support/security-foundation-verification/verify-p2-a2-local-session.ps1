[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$baselineVerifier = Join-Path $PSScriptRoot 'verify-p2-a1-security-foundation.ps1'
$t2Report = Join-Path $PSScriptRoot (
    'target/surefire-reports/TEST-org.koikifw.buildsupport.security.LocalSessionAuthorizationTest.xml')

& $baselineVerifier
if ($LASTEXITCODE -ne 0) {
    throw "Cumulative Security verification failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path -LiteralPath $t2Report)) {
    throw 'The P2-A2 T2 Surefire report is missing.'
}

[xml]$report = Get-Content -Raw -LiteralPath $t2Report
$suite = $report.testsuite
if ($suite.tests -ne '6' -or $suite.failures -ne '0' -or $suite.errors -ne '0' -or $suite.skipped -ne '0') {
    throw (
        'Unexpected P2-A2 T2 result: tests={0}, failures={1}, errors={2}, skipped={3}' -f
        $suite.tests, $suite.failures, $suite.errors, $suite.skipped)
}

Write-Host 'P2-A2 local session and authorization verification succeeded (T2 6/6).'

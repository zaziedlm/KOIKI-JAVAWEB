[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$a2Verifier = Join-Path $PSScriptRoot 'verify-p2-a2-local-session.ps1'
$expectedSuites = [ordered]@{
    'BearerProfileBoundaryTest' = 5
    'OidcProfileCoexistenceTest' = 2
}

& $a2Verifier
if ($LASTEXITCODE -ne 0) {
    throw "Cumulative P2-A2 verification failed with exit code $LASTEXITCODE"
}

foreach ($entry in $expectedSuites.GetEnumerator()) {
    $reportPath = Join-Path $PSScriptRoot (
        'target/surefire-reports/TEST-org.koikifw.buildsupport.security.{0}.xml' -f $entry.Key)
    if (-not (Test-Path -LiteralPath $reportPath)) {
        throw "The P2-A3 T3 Surefire report is missing: $($entry.Key)"
    }

    [xml]$report = Get-Content -Raw -LiteralPath $reportPath
    $suite = $report.testsuite
    if ($suite.tests -ne [string]$entry.Value `
            -or $suite.failures -ne '0' `
            -or $suite.errors -ne '0' `
            -or $suite.skipped -ne '0') {
        throw (
            'Unexpected P2-A3 T3 result for {0}: tests={1}, failures={2}, errors={3}, skipped={4}' -f
            $entry.Key, $suite.tests, $suite.failures, $suite.errors, $suite.skipped)
    }
}

Write-Host 'P2-A3 OIDC, Bearer JWT, profile boundary, and CORS verification succeeded (T3 7/7).'

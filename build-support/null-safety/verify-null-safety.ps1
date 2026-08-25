[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$verificationPom = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'verification/pom.xml'))
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}

function Invoke-NullSafetyBuild {
    param(
        [string[]]$AdditionalArguments = @()
    )

    $arguments = @(
        '--batch-mode'
        '--no-transfer-progress'
        '-f'
        $verificationPom
        'clean'
        'verify'
    ) + $AdditionalArguments

    $output = @(& $wrapper @arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output = $output -join [Environment]::NewLine
    }
}

Write-Host '=== NullAway positive verification ==='
$positive = Invoke-NullSafetyBuild
if ($positive.ExitCode -ne 0) {
    throw "Positive NullAway verification failed with exit code $($positive.ExitCode)."
}

Write-Host '=== NullAway negative verification (expected failure) ==='
$negative = Invoke-NullSafetyBuild -AdditionalArguments @('-Pnullaway-negative')
if ($negative.ExitCode -eq 0) {
    throw 'Negative NullAway verification unexpectedly succeeded.'
}

$requiredDiagnostics = @(
    '[NullAway]'
    'returning @Nullable expression from method with @NonNull return type'
)
foreach ($diagnostic in $requiredDiagnostics) {
    if (-not $negative.Output.Contains($diagnostic, [System.StringComparison]::Ordinal)) {
        throw "Negative NullAway verification did not contain the required diagnostic: $diagnostic"
    }
}

Write-Host '=== NullAway restore verification ==='
$restore = Invoke-NullSafetyBuild
if ($restore.ExitCode -ne 0) {
    throw "Restored NullAway verification failed with exit code $($restore.ExitCode)."
}

Write-Host 'NullAway positive, negative, and restore verification succeeded.'

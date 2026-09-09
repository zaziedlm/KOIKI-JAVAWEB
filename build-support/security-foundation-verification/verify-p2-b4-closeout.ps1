[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$nullSafetyVerifier = Join-Path $repositoryRoot (
    'build-support/null-safety/verify-null-safety.ps1')
$verificationSteps = @(
    [ordered]@{
        Name = 'P2-B1/B2/B3 T0-T5 core / inventory'
        Path = Join-Path $PSScriptRoot 'verify-p2-b3-session-core.ps1'
    },
    [ordered]@{
        Name = 'P2-B3 Web two-process Session journey'
        Path = Join-Path $PSScriptRoot 'verify-p2-b3-session-two-process.ps1'
    },
    [ordered]@{
        Name = 'P2-B3 non-web cleanup / single execution'
        Path = Join-Path $PSScriptRoot 'verify-p2-b3-session-cleanup-process.ps1'
    },
    [ordered]@{
        Name = 'P2-B4 packaged Reference identity journey'
        Path = Join-Path $PSScriptRoot 'verify-p2-b4-reference-journey.ps1'
    })

function Get-GitOutput {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $output = @(& git @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Git inspection failed: git $($Arguments -join ' ')"
    }
    return $output
}

function Assert-CleanRepositoryState {
    param([Parameter(Mandatory)][string]$ExpectedHead)

    $actualHead = Get-GitOutput -Arguments @('rev-parse', 'HEAD') |
        Select-Object -First 1
    if ($actualHead -ne $ExpectedHead) {
        throw 'Repository HEAD changed during P2-B4 closeout verification.'
    }
    $status = @(Get-GitOutput -Arguments @(
            'status', '--porcelain=v1', '--untracked-files=all'))
    if ($status.Count -ne 0) {
        throw 'P2-B4 closeout requires and must preserve a clean worktree.'
    }
}

function Invoke-VerificationStep {
    param(
        [Parameter(Mandatory)][int]$Round,
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Path
    )

    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & $Path
    } catch {
        $timer.Stop()
        throw "P2-B4 closeout round $Round failed in ${Name}: $($_.Exception.Message)"
    }
    $timer.Stop()
    Write-Host "P2-B4 closeout round $Round ${Name}: SUCCESS ($($timer.Elapsed))"
}

function Assert-NoResidualResources {
    foreach ($containerPrefix in @(
            'koiki-b34-',
            'koiki-b35-',
            'koiki-b44-',
            'koiki-b42-manual-')) {
        $containerNames = @(& docker ps --all `
                '--filter' "name=$containerPrefix" `
                '--format' '{{.Names}}' 2>&1)
        if ($LASTEXITCODE -ne 0) {
            throw 'Docker cleanup inspection failed.'
        }
        if ($containerNames.Count -ne 0) {
            throw "A P2-B verification container remains: $containerPrefix"
        }
    }

    foreach ($pattern in @(
            'koiki-session-core-*',
            'koiki-session-two-process-*',
            'koiki-session-cleanup-*',
            'koiki-reference-b44-*')) {
        $temporaryDirectories = @(Get-ChildItem `
                -LiteralPath ([System.IO.Path]::GetTempPath()) `
                -Directory -Filter $pattern -ErrorAction SilentlyContinue)
        if ($temporaryDirectories.Count -ne 0) {
            throw "A P2-B temporary directory remains: $pattern"
        }
    }

    foreach ($fixtureTarget in @(
            (Join-Path $PSScriptRoot 'target'),
            (Join-Path $PSScriptRoot 'session-two-process-fixture/target'),
            (Join-Path $PSScriptRoot 'session-cleanup-process-fixture/target'))) {
        if (Test-Path -LiteralPath $fixtureTarget) {
            throw "A non-distributed fixture target remains: $fixtureTarget"
        }
    }
}

$head = Get-GitOutput -Arguments @('rev-parse', 'HEAD') |
    Select-Object -First 1
Assert-CleanRepositoryState -ExpectedHead $head
Assert-NoResidualResources

for ($round = 1; $round -le 3; $round++) {
    foreach ($step in $verificationSteps) {
        Invoke-VerificationStep `
            -Round $round -Name $step.Name -Path $step.Path
    }
    Assert-CleanRepositoryState -ExpectedHead $head
    Assert-NoResidualResources
}

Write-Host '=== P2-B4 final Root Reactor regression ==='
& $wrapper --batch-mode --no-transfer-progress clean verify
if ($LASTEXITCODE -ne 0) {
    throw "P2-B4 Root Reactor regression failed with exit code $LASTEXITCODE."
}

Write-Host '=== P2-B4 final Null Safety verification ==='
& $nullSafetyVerifier

Assert-CleanRepositoryState -ExpectedHead $head
Assert-NoResidualResources
Write-Host (
    'Phase 2 P2-B4 closeout succeeded: three consecutive P2-B1-B4 ' +
    'aggregates, Root Reactor, Null Safety, inventory, sensitive-output ' +
    'and cleanup checks. Gate B review may begin after Architecture Owner approval.')

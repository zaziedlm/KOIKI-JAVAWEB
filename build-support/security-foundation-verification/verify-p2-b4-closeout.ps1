[CmdletBinding()]
param(
    [switch]$InspectOnly,
    [string]$ExpectedHead
)

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
        Name = 'P2-B1 Audit contract / transaction / inventory'
        Path = Join-Path $PSScriptRoot 'verify-p2-b1-audit-transaction.ps1'
    },
    [ordered]@{
        Name = 'P2-B2 Identity core / inventory'
        Path = Join-Path $PSScriptRoot 'verify-p2-b2-identity-core.ps1'
    },
    [ordered]@{
        Name = 'P2-B1/B2/B3 T0-T5 Session core / inventory'
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

function Get-OwnedVerificationProcessIds {
    param([Parameter(Mandatory)][string]$Marker)

    $processIds = [System.Collections.Generic.List[int]]::new()

    if ($IsWindows) {
        $javaProcesses = @(Get-CimInstance -ClassName Win32_Process `
                -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" `
                -Property ProcessId, CommandLine)
        foreach ($process in $javaProcesses) {
            $commandLine = [string]$process.CommandLine
            if ($commandLine.Contains($Marker, [StringComparison]::OrdinalIgnoreCase)) {
                $processIds.Add([int]$process.ProcessId)
            }
        }
        return @($processIds)
    }

    if ($IsLinux) {
        foreach ($processDirectory in @(Get-ChildItem -LiteralPath '/proc' `
                    -Directory -Filter '[0-9]*' -ErrorAction Stop)) {
            $commPath = Join-Path $processDirectory.FullName 'comm'
            $commandLinePath = Join-Path $processDirectory.FullName 'cmdline'
            try {
                $processName = ([System.IO.File]::ReadAllText($commPath)).Trim()
                if ($processName -ne 'java') {
                    continue
                }
                $commandLineBytes = [System.IO.File]::ReadAllBytes($commandLinePath)
                $commandLine = [System.Text.Encoding]::UTF8.GetString($commandLineBytes)
                if ($commandLine.Contains($Marker, [StringComparison]::Ordinal)) {
                    $processIds.Add([int]$processDirectory.Name)
                }
            } catch [System.IO.IOException] {
                # The process may exit between /proc enumeration and inspection.
            } catch [System.UnauthorizedAccessException] {
                throw "Process inspection was denied for PID $($processDirectory.Name)."
            }
        }
        return @($processIds)
    }

    throw 'Process inspection supports Windows and Linux only.'
}

function Assert-NoResidualResources {
    $failures = [System.Collections.Generic.List[string]]::new()

    foreach ($containerPrefix in @(
            'koiki-b34-',
            'koiki-b35-',
            'koiki-b44-',
            'koiki-b42-manual-')) {
        $containerNames = @(& docker ps --all `
                '--filter' "name=$containerPrefix" `
                '--format' '{{.Names}}' 2>&1)
        if ($LASTEXITCODE -ne 0) {
            $failures.Add("Docker inspection failed for prefix $containerPrefix.")
            continue
        }
        if ($containerNames.Count -ne 0) {
            $failures.Add("A P2-B verification container remains: $containerPrefix")
        }
    }

    foreach ($processMarker in @(
            'koiki-session-two-process-',
            'koiki-session-cleanup-',
            'koiki-reference-b44-')) {
        try {
            $processIds = @(Get-OwnedVerificationProcessIds -Marker $processMarker)
        } catch {
            $failures.Add(
                "Process inspection failed for marker ${processMarker}: " +
                $_.Exception.Message)
            continue
        }
        if ($processIds.Count -ne 0) {
            $failures.Add(
                "A P2-B verification process remains for marker ${processMarker}: " +
                ($processIds -join ', '))
        }
    }

    foreach ($pattern in @(
            'koiki-audit-transaction-*',
            'koiki-identity-core-*',
            'koiki-session-core-*',
            'koiki-session-two-process-*',
            'koiki-session-cleanup-*',
            'koiki-reference-b44-*')) {
        $temporaryDirectories = @(Get-ChildItem `
                -LiteralPath ([System.IO.Path]::GetTempPath()) `
                -Directory -Filter $pattern -ErrorAction SilentlyContinue)
        if ($temporaryDirectories.Count -ne 0) {
            $failures.Add("A P2-B temporary directory remains: $pattern")
        }
    }

    foreach ($fixtureTarget in @(
            (Join-Path $PSScriptRoot 'target'),
            (Join-Path $PSScriptRoot 'session-two-process-fixture/target'),
            (Join-Path $PSScriptRoot 'session-cleanup-process-fixture/target'))) {
        if (Test-Path -LiteralPath $fixtureTarget) {
            $failures.Add("A non-distributed fixture target remains: $fixtureTarget")
        }
    }

    if ($failures.Count -ne 0) {
        throw ('P2-B residual-resource inspection failed: ' + ($failures -join ' | '))
    }
}

function Assert-FinalIntegrity {
    param([Parameter(Mandatory)][string]$ExpectedHead)

    $failures = [System.Collections.Generic.List[string]]::new()
    try {
        Assert-CleanRepositoryState -ExpectedHead $ExpectedHead
        Write-Host 'P2-B4 final repository inspection: SUCCESS'
    } catch {
        $failures.Add("Repository inspection: $($_.Exception.Message)")
    }
    try {
        Assert-NoResidualResources
        Write-Host 'P2-B4 final residual-resource inspection: SUCCESS'
    } catch {
        $failures.Add("Residual-resource inspection: $($_.Exception.Message)")
    }
    if ($failures.Count -ne 0) {
        throw ('P2-B4 final integrity inspection failed: ' + ($failures -join ' | '))
    }
}

$head = if ([string]::IsNullOrWhiteSpace($ExpectedHead)) {
    Get-GitOutput -Arguments @('rev-parse', 'HEAD') |
        Select-Object -First 1
} else {
    $ExpectedHead
}

if ($InspectOnly) {
    Assert-FinalIntegrity -ExpectedHead $head
    Write-Host 'P2-B4 final repository and residual-resource inspection: SUCCESS'
    return
}

$verificationFailure = $null
try {
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
} catch {
    $verificationFailure = $_
}

$integrityFailure = $null
try {
    Assert-FinalIntegrity -ExpectedHead $head
} catch {
    $integrityFailure = $_
}

if ($null -ne $verificationFailure) {
    if ($null -ne $integrityFailure) {
        throw (
            'P2-B4 closeout failed: {0} Final integrity inspection also failed: {1}' -f
            $verificationFailure.Exception.Message,
            $integrityFailure.Exception.Message)
    }
    $PSCmdlet.ThrowTerminatingError($verificationFailure)
}

if ($null -ne $integrityFailure) {
    $PSCmdlet.ThrowTerminatingError($integrityFailure)
}

Write-Host (
    'Phase 2 P2-B4 closeout succeeded: three consecutive P2-B1-B4 ' +
    'aggregates, Root Reactor, Null Safety, inventory, sensitive-output ' +
    'and cleanup checks. Gate B review may begin after Architecture Owner approval.')

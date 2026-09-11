[CmdletBinding()]
param([string]$ExpectedCommit)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$publishInvoker = Join-Path $PSScriptRoot 'invoke-p2-c2-publish.ps1'
$stateVerifier = Join-Path $PSScriptRoot 'verify-p2-c2-publish-state.ps1'
$temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$workingRoot = Join-Path $temporaryRoot ('koiki-p2-c2-publish-dry-run-' + [guid]::NewGuid().ToString('N'))
$builderRepository = Join-Path $workingRoot 'builder'
$fileRepository = Join-Path $workingRoot 'remote'
$manifestPath = Join-Path $workingRoot 'p2-c2-publish-manifest.json'

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)
    $resolved = [IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolved) -notlike 'koiki-p2-c2-publish-dry-run-*') {
        throw "Unsafe P2-C2 dry-run path: $resolved"
    }
}

Assert-SafeTemporaryPath -Path $workingRoot
New-Item -ItemType Directory -Path $builderRepository,$fileRepository -Force | Out-Null
try {
    $repositoryUri = ([uri]$fileRepository).AbsoluteUri
    & $publishInvoker -RepositoryUrl $repositoryUri -RepositoryId 'p2-c2-local' `
        -ExpectedCommit $ExpectedCommit -LocalRepository $builderRepository

    & $stateVerifier -Mode Capture -RepositoryUrl $repositoryUri -ManifestPath $manifestPath -ExpectedCommit $ExpectedCommit
    if (-not $?) { throw 'P2-C2 local publish-state capture failed.' }
    & $stateVerifier -Mode Verify -RepositoryUrl $repositoryUri -ManifestPath $manifestPath -ExpectedCommit $ExpectedCommit
    if (-not $?) { throw 'P2-C2 local publish-state verification failed.' }
    Write-Output 'Phase 2 P2-C2 C2-5 local publish dry run succeeded (13 coordinates / per-coordinate snapshot values / 24 hashes / signature / japicmp).'
} finally {
    if (Test-Path -LiteralPath $workingRoot) {
        Assert-SafeTemporaryPath -Path $workingRoot
        Remove-Item -LiteralPath $workingRoot -Recurse -Force
    }
}

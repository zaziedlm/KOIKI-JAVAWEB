[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RepositoryUrl,
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9._-]+$')][string]$RepositoryId,
    [string]$ExpectedCommit,
    [string]$LocalRepository
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
. (Join-Path $PSScriptRoot 'p2-c2-publish-unit.ps1')

$entries = @(Get-P2C2PublishUnit -SupportRoot $PSScriptRoot)
$head = (& git -C $repositoryRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the source HEAD.' }
if ($ExpectedCommit -and ($ExpectedCommit -cnotmatch '^[0-9a-f]{40}$' -or $head -cne $ExpectedCommit)) {
    throw "Expected commit does not match HEAD: expected=$ExpectedCommit actual=$head"
}
if ($env:GITHUB_SHA -and $head -cne $env:GITHUB_SHA) {
    throw "HEAD does not match GITHUB_SHA: $head"
}

$repositoryUri = [uri]$RepositoryUrl
if ($repositoryUri.Scheme -notin @('file', 'https')) {
    throw "Unsupported publish repository scheme: $($repositoryUri.Scheme)"
}
if (-not $repositoryUri.IsFile -and $repositoryUri.AbsoluteUri.TrimEnd('/') -cne
    'https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB') {
    throw "Unexpected remote publish repository: $($repositoryUri.AbsoluteUri)"
}

$arguments = [Collections.Generic.List[string]]::new()
[void]$arguments.Add('--batch-mode')
[void]$arguments.Add('--no-transfer-progress')
if ($LocalRepository) { [void]$arguments.Add("-Dmaven.repo.local=$LocalRepository") }
[void]$arguments.Add('-f')
[void]$arguments.Add($rootPom)
[void]$arguments.Add('-pl')
[void]$arguments.Add(($entries.ModulePath -join ','))
[void]$arguments.Add("-DaltSnapshotDeploymentRepository=${RepositoryId}::$RepositoryUrl")
[void]$arguments.Add('-DdeployAtEnd=true')
[void]$arguments.Add('-DretryFailedDeploymentCount=1')
[void]$arguments.Add('clean')
[void]$arguments.Add('deploy')
[void]$arguments.Add('-DskipTests')

& $wrapper @arguments
if ($LASTEXITCODE -ne 0) { throw "P2-C2 publish failed with exit code $LASTEXITCODE." }
Write-Output 'P2-C2 publish invocation succeeded using the verified 13-coordinate manifest.'

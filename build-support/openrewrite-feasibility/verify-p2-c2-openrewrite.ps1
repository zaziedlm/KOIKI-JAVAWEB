[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$recipePom = Join-Path $PSScriptRoot 'recipe/pom.xml'
$fixtureSource = Join-Path $PSScriptRoot 'fixture'
$expectedSource = Join-Path $PSScriptRoot 'expected/SessionOwner.java'
$manualActions = Join-Path $PSScriptRoot 'manual-actions.md'
$formalUnit = Join-Path $repositoryRoot 'build-support/security-foundation-verification/p2-c2-formal-release-unit.txt'
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$workingRoot = Join-Path $temporaryRoot ('koiki-p2-c2-openrewrite-' + [guid]::NewGuid().ToString('N'))
$localRepository = Join-Path $workingRoot 'repository'
$fixtureWork = Join-Path $workingRoot 'fixture'
$recipeTarget = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'recipe/target'))

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)
    $resolved = [IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolved) -notlike 'koiki-p2-c2-openrewrite-*') {
        throw "Unsafe C2-6 temporary path: $resolved"
    }
}

function Invoke-KoikiMaven {
    param([Parameter(Mandatory)][string]$Label, [Parameter(Mandatory)][string[]]$Arguments)
    Write-Host "=== $Label ==="
    & $wrapper --batch-mode --no-transfer-progress "-Dmaven.repo.local=$localRepository" @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Label failed with exit code $LASTEXITCODE." }
}

function Read-NormalizedText {
    param([Parameter(Mandatory)][string]$Path)
    return [IO.File]::ReadAllText($Path).Replace("`r`n", "`n")
}

function Get-SourceTreeHash {
    param([Parameter(Mandatory)][string]$Path)
    $root = [IO.Path]::GetFullPath($Path)
    $content = @(Get-ChildItem -LiteralPath $root -Recurse -File |
        Where-Object Extension -in @('.java', '.xml') |
        Sort-Object FullName |
        ForEach-Object {
            $relative = [IO.Path]::GetRelativePath($root, $_.FullName).Replace('\', '/')
            "$relative`n$(Read-NormalizedText -Path $_.FullName)"
        }) -join "`n"
    $bytes = [Text.Encoding]::UTF8.GetBytes($content)
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes))
}

Assert-SafeTemporaryPath -Path $workingRoot
New-Item -ItemType Directory -Path $workingRoot,$localRepository -Force | Out-Null
try {
    [xml]$root = Get-Content -Raw -LiteralPath $rootPom
    if (@($root.project.modules.module) -contains 'build-support/openrewrite-feasibility') {
        throw 'The OpenRewrite prototype must remain outside the Root Reactor.'
    }
    if ((Get-Content -Raw -LiteralPath $formalUnit) -match 'openrewrite|migration-recipes') {
        throw 'The OpenRewrite prototype must remain outside the formal release unit.'
    }
    if (Test-Path -LiteralPath (Join-Path $repositoryRoot 'koiki-migration-recipes')) {
        throw 'C2-6 must not create the Phase 5 koiki-migration-recipes module.'
    }

    $manualText = Read-NormalizedText -Path $manualActions
    foreach ($requiredBoundary in @('Spring Boot', 'Customer migration guarantee', 'Phase 5 recipe artifact', 'dependency')) {
        if (-not $manualText.Contains($requiredBoundary)) {
            throw "Manual-actions report is missing boundary: $requiredBoundary"
        }
    }

    Copy-Item -LiteralPath $fixtureSource -Destination $fixtureWork -Recurse
    $legacySource = Join-Path $fixtureWork 'src/main/java/org/koikifw/legacy/identity/LegacyFrameworkUserId.java'
    $consumerSource = Join-Path $fixtureWork 'src/main/java/com/example/consumer/SessionOwner.java'
    $legacyHash = (Get-FileHash -LiteralPath $legacySource -Algorithm SHA256).Hash

    Invoke-KoikiMaven -Label 'Stage current KOIKI identity API' -Arguments @(
        '-f', $rootPom, '-pl', 'koiki-dependencies-bom,koiki-starters/koiki-starter-identity',
        '-am', 'clean', 'install', '-DskipTests')
    Invoke-KoikiMaven -Label 'Build and test non-distributed recipe' -Arguments @(
        '-f', $recipePom, 'clean', 'install')
    Invoke-KoikiMaven -Label 'Compile and test synthetic old Consumer' -Arguments @(
        '-f', (Join-Path $fixtureWork 'pom.xml'), 'clean', 'test')
    Invoke-KoikiMaven -Label 'Apply synthetic KOIKI API migration' -Arguments @(
        '-f', (Join-Path $fixtureWork 'pom.xml'),
        'org.openrewrite.maven:rewrite-maven-plugin:6.46.1:run', '-DskipTests')

    if ((Read-NormalizedText -Path $consumerSource) -cne (Read-NormalizedText -Path $expectedSource)) {
        throw 'Transformed Consumer source differs from the approved expected source.'
    }
    if ((Get-FileHash -LiteralPath $legacySource -Algorithm SHA256).Hash -cne $legacyHash) {
        throw 'The recipe modified the synthetic legacy type definition.'
    }

    $firstPassHash = Get-SourceTreeHash -Path $fixtureWork
    Invoke-KoikiMaven -Label 'Reapply recipe for idempotence' -Arguments @(
        '-f', (Join-Path $fixtureWork 'pom.xml'),
        'org.openrewrite.maven:rewrite-maven-plugin:6.46.1:run', '-DskipTests')
    $secondPassHash = Get-SourceTreeHash -Path $fixtureWork
    if ($firstPassHash -cne $secondPassHash) { throw 'The second recipe run changed the migrated fixture.' }

    Invoke-KoikiMaven -Label 'Compile and test transformed Consumer' -Arguments @(
        '-f', (Join-Path $fixtureWork 'pom.xml'), 'clean', 'test')
    Write-Output 'Phase 2 P2-C2 C2-6 OpenRewrite feasibility succeeded (before/after / idempotence / transformed test / manual actions / non-distribution).'
} finally {
    if (Test-Path -LiteralPath $workingRoot) {
        Assert-SafeTemporaryPath -Path $workingRoot
        Remove-Item -LiteralPath $workingRoot -Recurse -Force
    }
    if (Test-Path -LiteralPath $recipeTarget) {
        $expectedTarget = [IO.Path]::GetFullPath((Join-Path $repositoryRoot 'build-support/openrewrite-feasibility/recipe/target'))
        if ($recipeTarget -cne $expectedTarget) { throw "Unsafe recipe target path: $recipeTarget" }
        Remove-Item -LiteralPath $recipeTarget -Recurse -Force
    }
}

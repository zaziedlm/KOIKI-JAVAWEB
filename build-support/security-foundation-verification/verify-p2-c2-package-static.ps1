[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$bomPom = Join-Path $repositoryRoot 'koiki-dependencies-bom/pom.xml'
$manifestPath = Join-Path $PSScriptRoot 'p2-c2-formal-release-unit.txt'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$version = '0.1.0-SNAPSHOT'
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-p2-c2-package-static-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    $temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    $comparison = if ($IsWindows) {
        [System.StringComparison]::OrdinalIgnoreCase
    } else {
        [System.StringComparison]::Ordinal
    }
    if (-not $resolvedPath.StartsWith($temporaryRoot, $comparison)) {
        throw "Refusing to remove a path outside the temporary directory: $resolvedPath"
    }
}

function Invoke-KoikiMaven {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host "=== $Label ==="
    & $wrapper --batch-mode --no-transfer-progress `
        "-Dmaven.repo.local=$isolatedRepository" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

function Read-Manifest {
    $entries = @(Get-Content -LiteralPath $manifestPath |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne '' -and -not $_.StartsWith('#') } |
        ForEach-Object {
            $parts = @($_ -split '\s+')
            if ($parts.Count -ne 3 -or $parts[0] -notin @('POM', 'JAR')) {
                throw "Invalid formal release manifest entry: $_"
            }
            [pscustomobject]@{
                Packaging = $parts[0]
                ArtifactId = $parts[1]
                ModulePath = $parts[2]
            }
        })
    if ($entries.Count -ne 14) {
        throw "Formal release manifest must contain 14 reactor projects: $($entries.Count)"
    }
    if (@($entries | Where-Object Packaging -eq 'JAR').Count -ne 11) {
        throw 'Formal release manifest must contain exactly 11 distributed JARs.'
    }
    if (@($entries.ArtifactId | Sort-Object -Unique).Count -ne $entries.Count) {
        throw 'Formal release manifest artifact IDs must be unique.'
    }
    return $entries
}

function Assert-ReactorAndBomInventory {
    param([Parameter(Mandatory)][object[]]$Manifest)

    [xml]$root = Get-Content -Raw -LiteralPath $rootPom
    $actualModules = @($root.project.modules.module | ForEach-Object { [string]$_ })
    $expectedModules = @($Manifest |
        Where-Object ModulePath -ne '.' |
        ForEach-Object ModulePath) + @('koiki-reference-app')
    $difference = @(Compare-Object -ReferenceObject @($expectedModules | Sort-Object) `
        -DifferenceObject @($actualModules | Sort-Object) -SyncWindow 0)
    if ($difference.Count -ne 0) {
        throw "Root Reactor inventory differs from the formal unit plus Reference:`n$($difference | Out-String)"
    }

    [xml]$bom = Get-Content -Raw -LiteralPath $bomPom
    $managedKoiki = @($bom.project.dependencyManagement.dependencies.dependency |
        Where-Object { [string]$_.groupId -eq 'org.koikifw' } |
        ForEach-Object { [string]$_.artifactId } |
        Sort-Object)
    $expectedJars = @($Manifest |
        Where-Object Packaging -eq 'JAR' |
        ForEach-Object ArtifactId |
        Sort-Object)
    $bomDifference = @(Compare-Object -ReferenceObject $expectedJars `
        -DifferenceObject $managedKoiki -SyncWindow 0)
    if ($bomDifference.Count -ne 0) {
        throw "BOM JAR inventory differs from the formal release manifest:`n$($bomDifference | Out-String)"
    }
    if ($managedKoiki -contains 'koiki-reference-app') {
        throw 'The Reference application must not be managed by the Framework BOM.'
    }
}

function Assert-StagedInventory {
    param([Parameter(Mandatory)][object[]]$Manifest)

    $groupRoot = Join-Path $isolatedRepository 'org/koikifw'
    if (-not (Test-Path -LiteralPath $groupRoot -PathType Container)) {
        throw 'The staged org.koikifw repository root is missing.'
    }
    $actualArtifacts = @(Get-ChildItem -LiteralPath $groupRoot -Directory |
        Select-Object -ExpandProperty Name |
        Sort-Object)
    $expectedArtifacts = @($Manifest.ArtifactId | Sort-Object)
    $difference = @(Compare-Object -ReferenceObject $expectedArtifacts `
        -DifferenceObject $actualArtifacts -SyncWindow 0)
    if ($difference.Count -ne 0) {
        throw "Staged org.koikifw coordinates differ from the manifest:`n$($difference | Out-String)"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($entry in $Manifest) {
        $versionRoot = Join-Path $groupRoot "$($entry.ArtifactId)/$version"
        $pom = Join-Path $versionRoot "$($entry.ArtifactId)-$version.pom"
        if (-not (Test-Path -LiteralPath $pom -PathType Leaf)) {
            throw "Staged POM is missing: $($entry.ArtifactId)"
        }
        $jar = Join-Path $versionRoot "$($entry.ArtifactId)-$version.jar"
        if ($entry.Packaging -eq 'POM') {
            if (Test-Path -LiteralPath $jar) {
                throw "POM-only project unexpectedly installed a JAR: $($entry.ArtifactId)"
            }
            continue
        }
        if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
            throw "Staged JAR is missing: $($entry.ArtifactId)"
        }

        $archive = [System.IO.Compression.ZipFile]::OpenRead($jar)
        try {
            $forbiddenEntries = @($archive.Entries | Where-Object {
                $_.FullName -match '^org/koikifw/(?:reference|buildsupport)/' `
                    -or $_.FullName -match '^db/migration/customer/' `
                    -or $_.FullName -match '\.(?:java|template)$'
            })
            if ($forbiddenEntries.Count -ne 0) {
                throw (
                    "Reference, Tooling, Customer migration or source template leaked into {0}: {1}" -f
                    $entry.ArtifactId, ($forbiddenEntries.FullName -join ', '))
            }
        } finally {
            $archive.Dispose()
        }
    }

    foreach ($forbiddenArtifact in @(
        'koiki-reference-app',
        'security-foundation-consumer',
        'koiki-migration-recipes')) {
        if (Test-Path -LiteralPath (Join-Path $groupRoot $forbiddenArtifact)) {
            throw "Non-Framework artifact leaked into the formal repository: $forbiddenArtifact"
        }
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
$manifest = Read-Manifest
Assert-ReactorAndBomInventory -Manifest $manifest
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage P2-C2 formal Framework release unit' -Arguments @(
        '-f', $rootPom,
        '-pl', '!koiki-reference-app',
        'clean', 'install', '-DskipTests')
    Assert-StagedInventory -Manifest $manifest
    Write-Host (
        'Phase 2 P2-C2 C2-2 package verification succeeded ' +
        '(14 projects / 11 JARs / Reference and Tooling excluded).')
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

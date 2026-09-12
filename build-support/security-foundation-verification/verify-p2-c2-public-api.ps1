[CmdletBinding()]
param([switch]$PrintInventory)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$manifestPath = Join-Path $PSScriptRoot 'p2-c2-formal-release-unit.txt'
$toolRoot = Join-Path $repositoryRoot 'build-support/api-compatibility/p2-c2'
$classpathPom = Join-Path $toolRoot 'pom.xml'
$inventoryTool = Join-Path $toolRoot 'FullPublicApiInventory.java'
$expectedInventory = Join-Path $PSScriptRoot 'p2-c2-public-api.txt'
$legacyInventory = Join-Path $repositoryRoot 'build-support/api-compatibility/public-api.txt'
$legacyFixtureVerifier = Join-Path $repositoryRoot (
    'build-support/api-compatibility/verify-public-api-fixtures.ps1')
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ('koiki-p2-c2-public-api-' + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$classpathFile = Join-Path $verificationRoot 'classpath.txt'
$fixtureRoot = Join-Path $toolRoot 'fixture'
$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-p2-c2-public-api-*') {
        throw "Unexpected P2-C2 temporary directory name: $resolved"
    }
}

function Resolve-JdkTool {
    param([Parameter(Mandatory)][string]$Name)

    $javaHome = if ($env:JAVA21_HOME) { $env:JAVA21_HOME } else { $env:JAVA_HOME }
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        throw 'JAVA21_HOME or JAVA_HOME is required.'
    }
    $executable = if ($IsWindows) { "$Name.exe" } else { $Name }
    $tool = Join-Path $javaHome "bin/$executable"
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "JDK tool was not found: $tool"
    }
    return $tool
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
        throw "$Label failed with exit code $LASTEXITCODE."
    }
}

function Get-ManifestJarEntries {
    $entries = foreach ($line in Get-Content -LiteralPath $manifestPath) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) {
            continue
        }
        $parts = $trimmed -split '\s+'
        if ($parts.Count -ne 3) {
            throw "Invalid formal release manifest line: $line"
        }
        if ($parts[0] -eq 'JAR') {
            [pscustomobject]@{ ArtifactId = $parts[1]; ModulePath = $parts[2] }
        }
    }
    return @($entries)
}

function Get-ApprovedTypePairs {
    param([Parameter(Mandatory)][object[]]$JarEntries)

    $pairs = [System.Collections.Generic.List[string]]::new()
    $artifact = ''
    foreach ($line in Get-Content -LiteralPath $legacyInventory) {
        if ($line -match '^ARTIFACT\s+(?<artifact>\S+)$') {
            $artifact = $Matches.artifact
        } elseif ($line -match '^TYPE\s+.+\s+(?<type>\S+)$') {
            [void]$pairs.Add("$artifact|$($Matches.type)")
        }
    }
    foreach ($entry in $JarEntries) {
        $inventoryPath = Join-Path $repositoryRoot "$($entry.ModulePath)/public-api.txt"
        if (-not (Test-Path -LiteralPath $inventoryPath -PathType Leaf)) {
            continue
        }
        foreach ($line in Get-Content -LiteralPath $inventoryPath) {
            if ($line -match '^TYPE\s+(?<type>\S+)$') {
                [void]$pairs.Add("$($entry.ArtifactId)|$($Matches.type)")
            }
        }
    }
    return @($pairs | Sort-Object -Unique)
}

function Get-ActualTypePairs {
    param([Parameter(Mandatory)][string[]]$Inventory)

    $pairs = [System.Collections.Generic.List[string]]::new()
    $artifact = ''
    foreach ($line in $Inventory) {
        if ($line -match '^ARTIFACT\s+(?<artifact>\S+)$') {
            $artifact = $Matches.artifact
        } elseif ($line -match '^TYPE\s+.+\s+(?<type>\S+)$') {
            [void]$pairs.Add("$artifact|$($Matches.type)")
        }
    }
    return @($pairs | Sort-Object -Unique)
}

function Invoke-Inventory {
    param(
        [Parameter(Mandatory)][string]$ClassPath,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    $output = @(& $javaTool '-Xshare:off' '--class-path' $ClassPath $inventoryTool @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        $output | ForEach-Object { Write-Host $_ }
        throw 'P2-C2 Public API inventory generation failed.'
    }
    return @($output | ForEach-Object { [string]$_ })
}

function Build-FixtureJar {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$JSpecifyJar
    )

    $sourceRoot = Join-Path $fixtureRoot "$Name/src"
    $classes = Join-Path $verificationRoot "$Name/classes"
    $jarPath = Join-Path $verificationRoot "$Name.jar"
    $sources = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' |
        Sort-Object FullName | ForEach-Object { $_.FullName })
    New-Item -ItemType Directory -Path $classes -Force | Out-Null
    $compileOutput = @(& $javacTool '--release' '21' '-classpath' $JSpecifyJar `
        '-d' $classes @sources 2>&1)
    if ($LASTEXITCODE -ne 0) {
        $compileOutput | ForEach-Object { Write-Host $_ }
        throw "$Name fixture compilation failed."
    }
    $packageOutput = @(& $jarTool '--create' '--file' $jarPath '-C' $classes '.' 2>&1)
    if ($LASTEXITCODE -ne 0) {
        $packageOutput | ForEach-Object { Write-Host $_ }
        throw "$Name fixture packaging failed."
    }
    return $jarPath
}

$javaTool = Resolve-JdkTool -Name 'java'
$javacTool = Resolve-JdkTool -Name 'javac'
$jarTool = Resolve-JdkTool -Name 'jar'
Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    $jarEntries = Get-ManifestJarEntries
    if ($jarEntries.Count -ne 11) {
        throw "Formal release manifest must contain 11 JARs; found $($jarEntries.Count)."
    }

    Invoke-KoikiMaven -Label 'Stage formal Framework release unit' -Arguments @(
        '-f', $rootPom, '-pl', '!koiki-reference-app', 'clean', 'install', '-DskipTests')
    Invoke-KoikiMaven -Label 'Resolve full Public API inspection classpath' -Arguments @(
        '-f', $classpathPom,
        'org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath',
        "-Dmdep.outputFile=$classpathFile")

    $classPath = (Get-Content -Raw -LiteralPath $classpathFile).Trim()
    $inventoryArguments = [System.Collections.Generic.List[string]]::new()
    foreach ($entry in $jarEntries) {
        $jarPath = Join-Path $isolatedRepository (
            "org/koikifw/$($entry.ArtifactId)/0.1.0-SNAPSHOT/" +
            "$($entry.ArtifactId)-0.1.0-SNAPSHOT.jar")
        if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
            throw "Staged formal JAR is missing: $($entry.ArtifactId)"
        }
        [void]$inventoryArguments.Add($entry.ArtifactId)
        [void]$inventoryArguments.Add($jarPath)
    }
    $actualLines = Invoke-Inventory -ClassPath $classPath -Arguments $inventoryArguments
    $actualText = ($actualLines -join "`n") + "`n"

    $artifactLines = @($actualLines | Where-Object { $_ -match '^ARTIFACT\s+' })
    if ($artifactLines.Count -ne 11) {
        throw "Inventory must contain 11 artifact sections; found $($artifactLines.Count)."
    }
    $approvedTypes = Get-ApprovedTypePairs -JarEntries $jarEntries
    $actualTypes = Get-ActualTypePairs -Inventory $actualLines
    $typeDifference = @(Compare-Object $approvedTypes $actualTypes)
    if ($approvedTypes.Count -ne 24 -or $actualTypes.Count -ne 24 -or $typeDifference.Count -ne 0) {
        throw "Approved and packaged Public type inventories differ: $($typeDifference | Out-String)"
    }
    if ($actualText -match '[.]internal[.]') {
        throw 'An internal package leaked into the Public API baseline candidate.'
    }
    foreach ($publicPackage in @(
            'org.koikifw.architecture',
            'org.koikifw.archunit',
            'org.koikifw.audit',
            'org.koikifw.identity',
            'org.koikifw.session')) {
        if (-not $actualText.Contains(
                "PACKAGE $publicPackage NULL_MARKED true", [System.StringComparison]::Ordinal)) {
            throw "Public package nullness default is missing: $publicPackage"
        }
    }

    if ($PrintInventory) {
        Write-Output '--- P2-C2 INVENTORY START ---'
        $actualLines | ForEach-Object { Write-Output $_ }
        Write-Output '--- P2-C2 INVENTORY END ---'
    } else {
        if (-not (Test-Path -LiteralPath $expectedInventory -PathType Leaf)) {
            throw 'The reviewed P2-C2 Public API baseline candidate is missing.'
        }
        $expectedText = [System.IO.File]::ReadAllText($expectedInventory).Replace("`r`n", "`n")
        if ($actualText -ne $expectedText) {
            $actualPath = Join-Path $verificationRoot 'p2-c2-public-api.actual.txt'
            [System.IO.File]::WriteAllText($actualPath, $actualText, $utf8WithoutBom)
            throw "Packaged Public API differs from the baseline candidate: $actualPath"
        }
    }

    $jSpecifyJar = Get-ChildItem -LiteralPath (Join-Path $isolatedRepository 'org/jspecify/jspecify') `
        -Recurse -Filter 'jspecify-*.jar' | Select-Object -First 1 -ExpandProperty FullName
    if ([string]::IsNullOrWhiteSpace($jSpecifyJar)) {
        throw 'JSpecify fixture classpath was not resolved.'
    }
    $baselineJar = Build-FixtureJar -Name 'baseline' -JSpecifyJar $jSpecifyJar
    $compatibleJar = Build-FixtureJar -Name 'compatible' -JSpecifyJar $jSpecifyJar
    $nullnessJar = Build-FixtureJar -Name 'nullness' -JSpecifyJar $jSpecifyJar
    $baselineInventory = Invoke-Inventory `
        -ClassPath "$baselineJar$([System.IO.Path]::PathSeparator)$jSpecifyJar" `
        -Arguments @('fixture', $baselineJar)
    $compatibleInventory = Invoke-Inventory `
        -ClassPath "$compatibleJar$([System.IO.Path]::PathSeparator)$jSpecifyJar" `
        -Arguments @('fixture', $compatibleJar)
    $nullnessInventory = Invoke-Inventory `
        -ClassPath "$nullnessJar$([System.IO.Path]::PathSeparator)$jSpecifyJar" `
        -Arguments @('fixture', $nullnessJar)
    if (($baselineInventory -join "`n") -ne ($compatibleInventory -join "`n")) {
        throw 'A public internal-package addition changed the Public API inventory.'
    }
    if (($baselineInventory -join "`n") -eq ($nullnessInventory -join "`n") -or
        -not (($nullnessInventory -join "`n") -match '@Nullable')) {
        throw 'The nullness-only Public API change was not detected.'
    }

    Write-Host '=== Verify existing Public API compatibility positive and negative fixtures ==='
    & $legacyFixtureVerifier
    if (-not $?) {
        throw 'Existing compatibility fixture verification failed.'
    }

    Write-Output (
        'Phase 2 P2-C2 C2-4 Public API verification succeeded ' +
        '(11 JARs / 24 public types / full signatures / nullness and negative fixtures).')
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

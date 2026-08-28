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
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$verificationPom = Join-Path $PSScriptRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/runtime-foundation-consumer'
$consumerPom = Join-Path $consumerRoot 'pom.xml'
$consumerSource = $consumerRoot
$starterInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-api/public-api.txt'

$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp2-" + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$dependencyTree = Join-Path $verificationRoot 'consumer-runtime-dependencies.txt'

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp2-*') {
        throw "Unexpected CP2 temporary directory name: $resolved"
    }
}

function Invoke-KoikiMaven {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host "=== $Label ==="
    & $wrapper --batch-mode --no-transfer-progress "-Dmaven.repo.local=$isolatedRepository" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

function Find-KoikiInternalReferences {
    param([Parameter(Mandatory)][string]$SourceRoot)

    return Get-ChildItem -LiteralPath $SourceRoot -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal'
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage KOIKI CP2 release unit into isolated repository' -Arguments @(
        '-f', $rootPom, 'clean', 'install'
    )

    $stagedStarterJar = Join-Path $isolatedRepository `
        'org/koikifw/koiki-starter-api/0.1.0-SNAPSHOT/koiki-starter-api-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $stagedStarterJar -PathType Leaf)) {
        throw "Staged API Starter JAR was not found: $stagedStarterJar"
    }

    $starterArchive = [System.IO.Compression.ZipFile]::OpenRead($stagedStarterJar)
    try {
        $classEntries = @($starterArchive.Entries | Where-Object FullName -Like '*.class')
        $externalClassEntries = @($classEntries | Where-Object FullName -NotLike `
            'org/koikifw/starter/api/internal/*.class')
        foreach ($requiredResource in @(
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports',
            'META-INF/spring.factories',
            'META-INF/additional-spring-configuration-metadata.json'
        )) {
            if ($starterArchive.GetEntry($requiredResource) -eq $null) {
                throw "Required Starter resource is missing: $requiredResource"
            }
        }
    } finally {
        $starterArchive.Dispose()
    }
    if ($classEntries.Count -eq 0) {
        throw 'CP2 API Starter did not contain its internal auto-configuration implementation.'
    }
    if ($externalClassEntries.Count -ne 0) {
        $unexpectedClasses = $externalClassEntries.FullName -join ', '
        throw "CP2 introduced classes outside the internal package: $unexpectedClasses"
    }

    $inventory = Get-Content -Raw -LiteralPath $starterInventory
    if ($inventory -notmatch 'PUBLIC_JAVA_TYPES 0') {
        throw 'CP2 API Starter inventory does not declare PUBLIC_JAVA_TYPES 0.'
    }
    if ($inventory -notmatch 'PUBLIC_CONFIGURATION_PROPERTIES 5') {
        throw 'CP2 API Starter inventory does not declare its five configuration properties.'
    }

    Invoke-KoikiMaven -Label 'Verify CP2 fine-grained API starter fixture' -Arguments @(
        '-f', $verificationPom, 'clean', 'verify'
    )

    $negativeSourceRoot = Join-Path $verificationRoot 'negative-internal-reference'
    New-Item -ItemType Directory -Path $negativeSourceRoot -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $negativeSourceRoot 'InternalReferenceProbe.java') -Encoding utf8 -Value @'
package org.example;
import org.koikifw.starter.api.internal.KoikiApiAutoConfiguration;
final class InternalReferenceProbe {}
'@
    if (-not (Find-KoikiInternalReferences -SourceRoot $negativeSourceRoot)) {
        throw 'The KOIKI internal package negative guard did not detect its probe.'
    }
    if (Find-KoikiInternalReferences -SourceRoot $consumerSource) {
        throw 'Customer-like Consumer must not reference KOIKI internal packages.'
    }

    Invoke-KoikiMaven -Label 'Build CP2 Customer-like Runtime Consumer' -Arguments @(
        '-f', $consumerPom, 'clean', 'verify'
    )

    Invoke-KoikiMaven -Label 'Inspect CP2 Consumer runtime dependency boundary' -Arguments @(
        '-f', $consumerPom,
        '-pl', 'application', '-am',
        'dependency:tree',
        '-Dscope=runtime',
        "-DoutputFile=$dependencyTree"
    )

    $runtimeDependencies = Get-Content -Raw -LiteralPath $dependencyTree
    foreach ($required in @(
        'org.koikifw:koiki-starter-api',
        'org.springframework:spring-webmvc',
        'tools.jackson.core:jackson-databind',
        'org.hibernate.validator:hibernate-validator'
    )) {
        if ($runtimeDependencies -notmatch [regex]::Escape($required)) {
            throw "Required CP2 runtime dependency is missing: $required"
        }
    }
    foreach ($forbidden in @(
        'org.springframework:spring-webflux',
        'io.projectreactor:reactor-core',
        'org.springframework.security',
        'org.springframework.data:spring-data-jpa',
        'org.springframework.modulith'
    )) {
        if ($runtimeDependencies -match [regex]::Escape($forbidden)) {
            throw "Deferred or reactive runtime dependency was found: $forbidden"
        }
    }

    Write-Host 'CP2 isolated artifact, defaults, override, retry, versioned HTTP, and dependency checks succeeded.'
} finally {
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

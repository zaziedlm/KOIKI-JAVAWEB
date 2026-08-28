[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$verificationPom = Join-Path $PSScriptRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/runtime-foundation-consumer'
$consumerPom = Join-Path $consumerRoot 'pom.xml'
$observabilityInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-observability/public-api.txt'

$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp5-" + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$runtimeDependencyTree = Join-Path $verificationRoot 'consumer-runtime-dependencies.txt'
$platformMavenArguments = if ($IsWindows) {
    @(
        '-Ddocker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy',
        '-Ddocker.host=npipe:////./pipe/docker_engine'
    )
} else {
    @()
}

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp5-*') {
        throw "Unexpected CP5 temporary directory name: $resolved"
    }
}

function Invoke-KoikiMaven {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host "=== $Label ==="
    & $wrapper --batch-mode --no-transfer-progress `
        "-Dmaven.repo.local=$isolatedRepository" @platformMavenArguments @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage KOIKI CP5 release unit into isolated repository' -Arguments @(
        '-f', $rootPom, 'clean', 'install'
    )

    $observabilityJar = Join-Path $isolatedRepository `
        'org/koikifw/koiki-starter-observability/0.1.0-SNAPSHOT/koiki-starter-observability-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $observabilityJar -PathType Leaf)) {
        throw "Staged CP5 artifact was not found: $observabilityJar"
    }

    $archive = [System.IO.Compression.ZipFile]::OpenRead($observabilityJar)
    try {
        $classEntries = @($archive.Entries | Where-Object FullName -Like '*.class')
        $externalClassEntries = @($classEntries | Where-Object FullName -NotLike `
            'org/koikifw/starter/observability/internal/*.class')
        foreach ($requiredEntry in @(
            'org/koikifw/starter/observability/internal/KoikiObservabilityAutoConfiguration.class',
            'org/koikifw/starter/observability/internal/KoikiCorrelationFilter.class',
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports',
            'META-INF/spring.factories',
            'META-INF/additional-spring-configuration-metadata.json'
        )) {
            if ($archive.GetEntry($requiredEntry) -eq $null) {
                throw "Required Observability Starter entry is missing: $requiredEntry"
            }
        }
    } finally {
        $archive.Dispose()
    }
    if ($classEntries.Count -eq 0) {
        throw 'CP5 Observability Starter did not contain its internal implementation.'
    }
    if ($externalClassEntries.Count -ne 0) {
        throw "CP5 Observability Starter exposes classes outside internal: $($externalClassEntries.FullName -join ', ')"
    }

    $inventory = Get-Content -Raw -LiteralPath $observabilityInventory
    if ($inventory -notmatch 'ARTIFACT koiki-starter-observability' -or
        $inventory -notmatch 'PUBLIC_JAVA_TYPES 0' -or
        $inventory -notmatch 'PUBLIC_CONFIGURATION_PROPERTIES 3') {
        throw 'Observability Starter Public API inventory is inconsistent with the CP5 contract.'
    }

    Invoke-KoikiMaven -Label 'Verify CP5 fine-grained Observability fixture' -Arguments @(
        '-f', $verificationPom, 'clean', 'verify'
    )

    $internalReferences = Get-ChildItem -LiteralPath $consumerRoot -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal'
    if ($internalReferences) {
        throw 'Customer-like Consumer must not reference KOIKI internal packages.'
    }

    Invoke-KoikiMaven -Label 'Build CP5 Customer-like Runtime Consumer with PostgreSQL 17' -Arguments @(
        '-f', $consumerPom, 'clean', 'verify'
    )

    Invoke-KoikiMaven -Label 'Inspect CP5 Consumer runtime dependency boundary' -Arguments @(
        '-f', $consumerPom,
        '-pl', 'application', '-am',
        'dependency:tree',
        '-Dscope=runtime',
        "-DoutputFile=$runtimeDependencyTree"
    )

    $runtimeDependencies = Get-Content -Raw -LiteralPath $runtimeDependencyTree
    foreach ($required in @(
        'org.koikifw:koiki-starter-observability',
        'io.micrometer:context-propagation',
        'org.slf4j:slf4j-api'
    )) {
        if ($runtimeDependencies -notmatch [regex]::Escape($required)) {
            throw "Required CP5 runtime dependency is missing: $required"
        }
    }
    foreach ($forbidden in @(
        'org.springframework:spring-webflux',
        'io.projectreactor:reactor-core',
        'org.springframework.security',
        'org.mybatis',
        'org.mybatis.spring.boot',
        'org.springframework.modulith',
        'io.opentelemetry'
    )) {
        if ($runtimeDependencies -match [regex]::Escape($forbidden)) {
            throw "Deferred, reactive, or exporter runtime dependency was found: $forbidden"
        }
    }

    Write-Host 'CP5 isolated artifact, structured logging, correlation, async propagation, thread cleanup, and dependency checks succeeded.'
} finally {
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

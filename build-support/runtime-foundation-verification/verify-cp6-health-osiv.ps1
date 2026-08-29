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
$consumerApplicationPom = Join-Path $consumerRoot 'application/pom.xml'
$jpaInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-data-jpa/public-api.txt'
$observabilityInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-observability/public-api.txt'

$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp6-" + [guid]::NewGuid().ToString('N'))
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
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp6-*') {
        throw "Unexpected CP6 temporary directory name: $resolved"
    }
}

function Get-TestcontainersContainerIds {
    $containerIds = @(& docker ps --all --quiet --filter 'label=org.testcontainers=true')
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect Testcontainers cleanup state (docker exit code $LASTEXITCODE)."
    }
    return @($containerIds | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

function Assert-NoNewTestcontainersContainers {
    param(
        [AllowEmptyCollection()][string[]]$BaselineContainerIds,
        [TimeSpan]$Timeout = [TimeSpan]::FromSeconds(30)
    )

    $deadline = [DateTimeOffset]::UtcNow.Add($Timeout)
    do {
        $remainingContainerIds = @(Get-TestcontainersContainerIds | Where-Object {
            $_ -notin $BaselineContainerIds
        })
        if ($remainingContainerIds.Count -eq 0) {
            return
        }
        Start-Sleep -Milliseconds 250
    } while ([DateTimeOffset]::UtcNow -lt $deadline)

    throw "Testcontainers cleanup left new containers behind: $($remainingContainerIds -join ', ')"
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

function Assert-InternalOnlyJar {
    param(
        [Parameter(Mandatory)][string]$JarPath,
        [Parameter(Mandatory)][string]$InternalPattern,
        [Parameter(Mandatory)][string[]]$RequiredEntries
    )

    $archive = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $classEntries = @($archive.Entries | Where-Object FullName -Like '*.class')
        $externalClassEntries = @($classEntries | Where-Object FullName -NotLike $InternalPattern)
        foreach ($requiredEntry in $RequiredEntries) {
            if ($archive.GetEntry($requiredEntry) -eq $null) {
                throw "Required artifact entry is missing from ${JarPath}: $requiredEntry"
            }
        }
        if ($classEntries.Count -eq 0) {
            throw "Starter contains no implementation classes: $JarPath"
        }
        if ($externalClassEntries.Count -ne 0) {
            throw "Starter exposes classes outside internal: $($externalClassEntries.FullName -join ', ')"
        }
        if (@($archive.Entries | Where-Object FullName -Like '*.sql').Count -ne 0) {
            throw "Starter must not contain production migration SQL: $JarPath"
        }
    } finally {
        $archive.Dispose()
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage KOIKI CP6 release unit into isolated repository' -Arguments @(
        '-f', $rootPom, 'clean', 'install'
    )

    $jpaJar = Join-Path $isolatedRepository `
        'org/koikifw/koiki-starter-data-jpa/0.1.0-SNAPSHOT/koiki-starter-data-jpa-0.1.0-SNAPSHOT.jar'
    $observabilityJar = Join-Path $isolatedRepository `
        'org/koikifw/koiki-starter-observability/0.1.0-SNAPSHOT/koiki-starter-observability-0.1.0-SNAPSHOT.jar'
    foreach ($artifact in @($jpaJar, $observabilityJar)) {
        if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) {
            throw "Staged CP6 artifact was not found: $artifact"
        }
    }

    Assert-InternalOnlyJar -JarPath $jpaJar `
        -InternalPattern 'org/koikifw/starter/data/jpa/internal/*.class' `
        -RequiredEntries @(
            'org/koikifw/starter/data/jpa/internal/KoikiDataJpaDefaultsEnvironmentPostProcessor.class',
            'META-INF/spring.factories',
            'META-INF/additional-spring-configuration-metadata.json'
        )
    Assert-InternalOnlyJar -JarPath $observabilityJar `
        -InternalPattern 'org/koikifw/starter/observability/internal/*.class' `
        -RequiredEntries @(
            'org/koikifw/starter/observability/internal/KoikiObservabilityAutoConfiguration.class',
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports',
            'META-INF/spring.factories',
            'META-INF/additional-spring-configuration-metadata.json'
        )

    $jpaApi = Get-Content -Raw -LiteralPath $jpaInventory
    if ($jpaApi -notmatch 'ARTIFACT koiki-starter-data-jpa' -or
        $jpaApi -notmatch 'PUBLIC_JAVA_TYPES 0' -or
        $jpaApi -notmatch 'PUBLIC_CONFIGURATION_PROPERTIES 1') {
        throw 'Data JPA Starter Public API inventory is inconsistent with the CP6 contract.'
    }
    $observabilityApi = Get-Content -Raw -LiteralPath $observabilityInventory
    if ($observabilityApi -notmatch 'PUBLIC_JAVA_TYPES 0' -or
        $observabilityApi -notmatch 'PUBLIC_CONFIGURATION_PROPERTIES 3') {
        throw 'Observability Starter Public API inventory changed unexpectedly in CP6.'
    }

    Invoke-KoikiMaven -Label 'Verify CP6 fine-grained health and JPA defaults fixture' -Arguments @(
        '-f', $verificationPom, 'clean', 'verify'
    )

    $internalReferences = Get-ChildItem -LiteralPath $consumerRoot -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal'
    if ($internalReferences) {
        throw 'Customer-like Consumer must not reference KOIKI internal packages.'
    }

    [xml]$applicationModel = Get-Content -Raw -LiteralPath $consumerApplicationPom
    $directBootJpa = @($applicationModel.project.dependencies.dependency | Where-Object {
        $_.groupId -eq 'org.springframework.boot' -and $_.artifactId -eq 'spring-boot-starter-data-jpa'
    })
    if ($directBootJpa.Count -ne 0) {
        throw 'Consumer must use koiki-starter-data-jpa instead of a direct Boot JPA Starter dependency.'
    }

    $baselineTestcontainersContainerIds = @(Get-TestcontainersContainerIds)
    try {
        Invoke-KoikiMaven -Label 'Build CP6 Customer-like Runtime Consumer with PostgreSQL 17' -Arguments @(
            '-f', $consumerPom, 'clean', 'verify'
        )
    } finally {
        Assert-NoNewTestcontainersContainers `
            -BaselineContainerIds $baselineTestcontainersContainerIds
    }

    $consumerJar = Join-Path $consumerRoot `
        'application/target/runtime-foundation-consumer-application-0.1.0-SNAPSHOT.jar'
    $consumerArchive = [System.IO.Compression.ZipFile]::OpenRead($consumerJar)
    try {
        $testOnlyEntries = @($consumerArchive.Entries | Where-Object {
            $_.FullName -Like '*cp6fixture*' -or $_.FullName -Like '*RuntimeFoundationConsumerOsiv*'
        })
        if ($testOnlyEntries.Count -ne 0) {
            throw 'CP6 test-only Entity exposure fixture leaked into the production artifact.'
        }
    } finally {
        $consumerArchive.Dispose()
    }

    Invoke-KoikiMaven -Label 'Inspect CP6 Consumer runtime dependency boundary' -Arguments @(
        '-f', $consumerPom,
        '-pl', 'application', '-am',
        'dependency:tree',
        '-Dscope=runtime',
        "-DoutputFile=$runtimeDependencyTree"
    )

    $runtimeDependencies = Get-Content -Raw -LiteralPath $runtimeDependencyTree
    foreach ($required in @(
        'org.koikifw:koiki-starter-data-jpa',
        'org.koikifw:koiki-starter-observability',
        'org.springframework.boot:spring-boot-starter-actuator',
        'org.springframework.boot:spring-boot-actuator-autoconfigure',
        'org.springframework.boot:spring-boot-starter-data-jpa'
    )) {
        if ($runtimeDependencies -notmatch [regex]::Escape($required)) {
            throw "Required CP6 runtime dependency is missing: $required"
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

    Write-Host 'CP6 isolated artifact, health UP/DOWN/restore, OSIV boundary, regression, and dependency checks succeeded.'
} finally {
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

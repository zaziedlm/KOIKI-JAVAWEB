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
$dataStarterInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-data/public-api.txt'
$testingInventory = Join-Path $repositoryRoot 'koiki-testing/public-api.txt'
$customerMigrationRoot = Join-Path $consumerRoot 'application/src/main/resources/db/migration/customer'
$unexpectedCustomerKoikiRoot = Join-Path $consumerRoot 'application/src/main/resources/db/migration/koiki'

$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp4-" + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$runtimeDependencyTree = Join-Path $verificationRoot 'consumer-runtime-dependencies.txt'
$testDependencyTree = Join-Path $verificationRoot 'consumer-test-dependencies.txt'
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
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp4-*') {
        throw "Unexpected CP4 temporary directory name: $resolved"
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

function Find-KoikiInternalReferences {
    param([Parameter(Mandatory)][string]$SourceRoot)

    return Get-ChildItem -LiteralPath $SourceRoot -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal'
}

function Assert-Inventory {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Artifact,
        [Parameter(Mandatory)][int]$PropertyCount
    )

    $inventory = Get-Content -Raw -LiteralPath $Path
    if ($inventory -notmatch "ARTIFACT $([regex]::Escape($Artifact))") {
        throw "Public API inventory does not identify $Artifact."
    }
    if ($inventory -notmatch 'PUBLIC_JAVA_TYPES 0') {
        throw "$Artifact must declare PUBLIC_JAVA_TYPES 0."
    }
    if ($inventory -notmatch "PUBLIC_CONFIGURATION_PROPERTIES $PropertyCount") {
        throw "$Artifact configuration property count is not $PropertyCount."
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage KOIKI CP4 release unit into isolated repository' -Arguments @(
        '-f', $rootPom, 'clean', 'install'
    )

    $dataStarterJar = Join-Path $isolatedRepository `
        'org/koikifw/koiki-starter-data/0.1.0-SNAPSHOT/koiki-starter-data-0.1.0-SNAPSHOT.jar'
    $testingJar = Join-Path $isolatedRepository `
        'org/koikifw/koiki-testing/0.1.0-SNAPSHOT/koiki-testing-0.1.0-SNAPSHOT.jar'
    foreach ($artifactJar in @($dataStarterJar, $testingJar)) {
        if (-not (Test-Path -LiteralPath $artifactJar -PathType Leaf)) {
            throw "Staged CP4 artifact was not found: $artifactJar"
        }
    }

    $dataStarterArchive = [System.IO.Compression.ZipFile]::OpenRead($dataStarterJar)
    try {
        $classEntries = @($dataStarterArchive.Entries | Where-Object FullName -Like '*.class')
        $externalClassEntries = @($classEntries | Where-Object FullName -NotLike `
            'org/koikifw/starter/data/internal/*.class')
        foreach ($requiredEntry in @(
            'org/koikifw/starter/data/internal/KoikiDataFlywayAutoConfiguration.class',
            'org/koikifw/starter/data/internal/KoikiDataDefaultsEnvironmentPostProcessor.class',
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports',
            'META-INF/spring.factories',
            'META-INF/additional-spring-configuration-metadata.json'
        )) {
            if ($dataStarterArchive.GetEntry($requiredEntry) -eq $null) {
                throw "Required Data Starter entry is missing: $requiredEntry"
            }
        }
        $sqlEntries = @($dataStarterArchive.Entries | Where-Object FullName -Like '*.sql')
    } finally {
        $dataStarterArchive.Dispose()
    }
    if ($classEntries.Count -eq 0) {
        throw 'CP4 Data Starter did not contain its internal implementation.'
    }
    if ($externalClassEntries.Count -ne 0) {
        throw "CP4 Data Starter exposes classes outside internal: $($externalClassEntries.FullName -join ', ')"
    }
    if ($sqlEntries.Count -ne 0) {
        throw "Framework Data Starter must not package speculative migration SQL: $($sqlEntries.FullName -join ', ')"
    }

    $testingArchive = [System.IO.Compression.ZipFile]::OpenRead($testingJar)
    try {
        $testingClasses = @($testingArchive.Entries | Where-Object FullName -Like '*.class')
        if ($testingArchive.GetEntry('META-INF/koiki-testing-support') -eq $null) {
            throw 'koiki-testing support marker is missing.'
        }
    } finally {
        $testingArchive.Dispose()
    }
    if ($testingClasses.Count -ne 0) {
        throw "koiki-testing unexpectedly exports Java classes: $($testingClasses.FullName -join ', ')"
    }

    Assert-Inventory -Path $dataStarterInventory -Artifact 'koiki-starter-data' -PropertyCount 2
    Assert-Inventory -Path $testingInventory -Artifact 'koiki-testing' -PropertyCount 0

    Invoke-KoikiMaven -Label 'Verify CP4 fine-grained Data Starter fixture' -Arguments @(
        '-f', $verificationPom, 'clean', 'verify'
    )

    if (Find-KoikiInternalReferences -SourceRoot $consumerRoot) {
        throw 'Customer-like Consumer must not reference KOIKI internal packages.'
    }
    if (Test-Path -LiteralPath $unexpectedCustomerKoikiRoot) {
        throw 'Consumer main resources must not contain KOIKI-owned migration SQL.'
    }
    $customerMigrations = @(Get-ChildItem -LiteralPath $customerMigrationRoot -Filter '*.sql')
    if ($customerMigrations.Count -eq 0) {
        throw 'Customer-like Consumer does not contain a Customer-owned migration.'
    }
    $customerSql = ($customerMigrations | Get-Content -Raw) -join [Environment]::NewLine
    if ($customerSql -notmatch 'kkbiz_') {
        throw 'Customer-owned migrations must use the kkbiz_ table prefix in this fixture.'
    }

    Invoke-KoikiMaven -Label 'Build CP4 Customer-like Runtime Consumer on PostgreSQL 17' -Arguments @(
        '-f', $consumerPom, 'clean', 'verify'
    )

    Invoke-KoikiMaven -Label 'Inspect CP4 Consumer runtime dependency boundary' -Arguments @(
        '-f', $consumerPom,
        '-pl', 'application', '-am',
        'dependency:tree',
        '-Dscope=runtime',
        "-DoutputFile=$runtimeDependencyTree"
    )
    Invoke-KoikiMaven -Label 'Inspect CP4 Consumer test dependency boundary' -Arguments @(
        '-f', $consumerPom,
        '-pl', 'application',
        'dependency:tree',
        '-Dscope=test',
        '-Dincludes=org.koikifw:koiki-testing',
        "-DoutputFile=$testDependencyTree"
    )

    $runtimeDependencies = Get-Content -Raw -LiteralPath $runtimeDependencyTree
    foreach ($required in @(
        'org.koikifw:koiki-starter-api',
        'org.koikifw:koiki-starter-data',
        'org.springframework.data:spring-data-jpa',
        'org.flywaydb:flyway-core',
        'org.flywaydb:flyway-database-postgresql',
        'org.postgresql:postgresql'
    )) {
        if ($runtimeDependencies -notmatch [regex]::Escape($required)) {
            throw "Required CP4 runtime dependency is missing: $required"
        }
    }
    foreach ($forbidden in @(
        'org.springframework:spring-webflux',
        'io.projectreactor:reactor-core',
        'org.springframework.security',
        'org.mybatis',
        'org.mybatis.spring.boot',
        'org.springframework.modulith'
    )) {
        if ($runtimeDependencies -match [regex]::Escape($forbidden)) {
            throw "Deferred or reactive runtime dependency was found: $forbidden"
        }
    }

    $testDependencies = Get-Content -Raw -LiteralPath $testDependencyTree
    if ($testDependencies -notmatch 'org[.]koikifw:koiki-testing:jar:0[.]1[.]0-SNAPSHOT:test') {
        throw 'koiki-testing is missing from the Consumer test dependency boundary.'
    }

    Write-Host 'CP4 isolated artifact, Flyway failure/restore, transaction, Consumer DB, and dependency checks succeeded.'
} finally {
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$identityRoot = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-identity'
$identityPom = Join-Path $identityRoot 'pom.xml'
$identityInventory = Join-Path $identityRoot 'public-api.txt'
$dataPom = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-data/pom.xml'
$fixturePom = Join-Path $PSScriptRoot 'pom.xml'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-identity-core-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$identityTree = Join-Path $verificationRoot 'identity-dependency-tree.txt'
$dataTree = Join-Path $verificationRoot 'data-dependency-tree.txt'
$fixtureTarget = Join-Path $PSScriptRoot 'target'
$expectedSuites = [ordered]@{
    'SecurityDependencyBaselineTest' = 1
    'SecurityAutoConfigurationContextTest' = 5
    'SecurityRequestBoundaryTest' = 4
    'LocalSessionAuthorizationTest' = 6
    'BearerProfileBoundaryTest' = 5
    'OidcProfileCoexistenceTest' = 2
    'AuditTransactionFixtureTest' = 8
    'IdentityPublicContractTest' = 3
    'IdentityCoreMigrationFixtureTest' = 5
}

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    $temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if (-not $resolvedPath.StartsWith($temporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the temporary directory: $resolvedPath"
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

function Assert-SurefireResults {
    $total = 0
    foreach ($entry in $expectedSuites.GetEnumerator()) {
        $reportPath = Join-Path $fixtureTarget (
            'surefire-reports/TEST-org.koikifw.buildsupport.security.{0}.xml' -f $entry.Key)
        if (-not (Test-Path -LiteralPath $reportPath)) {
            throw "The cumulative T0-T4 Surefire report is missing: $($entry.Key)"
        }

        [xml]$report = Get-Content -Raw -LiteralPath $reportPath
        $suite = $report.testsuite
        if ($suite.tests -ne [string]$entry.Value `
                -or $suite.failures -ne '0' `
                -or $suite.errors -ne '0' `
                -or $suite.skipped -ne '0') {
            throw (
                'Unexpected result for {0}: tests={1}, failures={2}, errors={3}, skipped={4}' -f
                $entry.Key, $suite.tests, $suite.failures, $suite.errors, $suite.skipped)
        }
        $total += [int]$suite.tests
    }
    if ($total -ne 39) {
        throw "Unexpected cumulative test count: $total"
    }
}

function Assert-IdentityContract {
    param([Parameter(Mandatory)][System.IO.FileInfo]$FormalJar)

    $actualInventory = @(Get-Content -LiteralPath $identityInventory |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne '' -and -not $_.StartsWith('#') })
    $expectedInventory = @(
        'ARTIFACT koiki-starter-identity',
        'PUBLIC_JAVA_TYPES 10',
        'TYPE org.koikifw.identity.AuthenticationSource',
        'TYPE org.koikifw.identity.FrameworkPrincipal',
        'TYPE org.koikifw.identity.FrameworkUserId',
        'TYPE org.koikifw.identity.IdentityAdministration',
        'TYPE org.koikifw.identity.IdentityFailure',
        'TYPE org.koikifw.identity.IdentityOperationException',
        'TYPE org.koikifw.identity.IdentityQuery',
        'TYPE org.koikifw.identity.IdentityUser',
        'TYPE org.koikifw.identity.UserSessionInvalidator',
        'TYPE org.koikifw.identity.UserStatus',
        'PUBLIC_CONFIGURATION_PROPERTIES 0',
        'PUBLIC_IDENTITY_ERROR_CODES 5',
        'ERROR_CODE INVALID_INPUT',
        'ERROR_CODE NOT_FOUND',
        'ERROR_CODE CONFLICT',
        'ERROR_CODE CONCURRENT_MODIFICATION',
        'ERROR_CODE DEPENDENCY_FAILURE',
        'INTERNAL_PACKAGE org.koikifw.identity.internal',
        'IDENTITY_TABLES 8',
        'PASSWORD_RESET DEFERRED',
        'SESSION_ADAPTER P2-B3')
    if (@(Compare-Object -ReferenceObject $expectedInventory `
                -DifferenceObject $actualInventory -SyncWindow 0).Count -ne 0) {
        throw 'Identity Public API inventory differs from the approved P2-B2 contract.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($FormalJar.FullName)
    try {
        $actualPublicClasses = @($archive.Entries |
            Where-Object {
                $_.FullName -match '^org/koikifw/identity/[^/]+\.class$' `
                    -and $_.FullName -notmatch '/package-info\.class$'
            } |
            Select-Object -ExpandProperty FullName |
            Sort-Object)
        $expectedPublicClasses = @(
            'org/koikifw/identity/AuthenticationSource.class',
            'org/koikifw/identity/FrameworkPrincipal.class',
            'org/koikifw/identity/FrameworkUserId.class',
            'org/koikifw/identity/IdentityAdministration.class',
            'org/koikifw/identity/IdentityFailure.class',
            'org/koikifw/identity/IdentityOperationException.class',
            'org/koikifw/identity/IdentityQuery.class',
            'org/koikifw/identity/IdentityUser.class',
            'org/koikifw/identity/UserSessionInvalidator.class',
            'org/koikifw/identity/UserStatus.class')
        if (@(Compare-Object -ReferenceObject $expectedPublicClasses `
                    -DifferenceObject $actualPublicClasses).Count -ne 0) {
            throw "Formal Identity JAR has an unexpected public-package type: $($actualPublicClasses -join ', ')"
        }

        $migration = 'db/migration/koiki/V2026090301__create_koiki_identity.sql'
        $migrationEntry = $archive.GetEntry($migration)
        if ($null -eq $migrationEntry) {
            throw 'The approved Identity production migration is missing.'
        }
        $migrationReader = [System.IO.StreamReader]::new($migrationEntry.Open())
        try {
            $migrationSql = $migrationReader.ReadToEnd()
        } finally {
            $migrationReader.Dispose()
        }
        if ([regex]::Matches($migrationSql, '(?im)^CREATE TABLE koiki_').Count -ne 8) {
            throw 'Identity migration must create exactly the eight approved Framework tables.'
        }
        $forbiddenEntries = @($archive.Entries | Where-Object {
            $_.FullName -match '(?i)(password.?reset|koiki_session|spring/session)'
        })
        if ($forbiddenEntries.Count -ne 0) {
            throw "Deferred reset or Session content leaked into Identity: $($forbiddenEntries.FullName -join ', ')"
        }

        $importsEntry = $archive.GetEntry(
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports')
        if ($null -eq $importsEntry) {
            throw 'Identity Auto Configuration imports resource is missing.'
        }
        $reader = [System.IO.StreamReader]::new($importsEntry.Open())
        try {
            $imports = $reader.ReadToEnd().Trim()
        } finally {
            $reader.Dispose()
        }
        if ($imports -ne 'org.koikifw.identity.internal.KoikiIdentityAutoConfiguration') {
            throw "Unexpected Identity Auto Configuration import: $imports"
        }
    } finally {
        $archive.Dispose()
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage the formal KOIKI release unit' -Arguments @(
        '-f', $rootPom, 'clean', 'install', '-DskipTests')

    $artifactRoot = Join-Path $isolatedRepository (
        'org/koikifw/koiki-starter-identity/0.1.0-SNAPSHOT')
    $formalJar = Get-Item -LiteralPath (Join-Path $artifactRoot (
        'koiki-starter-identity-0.1.0-SNAPSHOT.jar'))
    Assert-IdentityContract -FormalJar $formalJar

    Invoke-KoikiMaven -Label 'Verify cumulative T0-T4 and Identity core with PostgreSQL' -Arguments @(
        '-f', $fixturePom, 'clean', 'verify')
    Assert-SurefireResults

    Invoke-KoikiMaven -Label 'Record the Identity production dependency tree' -Arguments @(
        '-f', $identityPom, 'dependency:tree', '-Dscope=runtime', "-DoutputFile=$identityTree")
    Invoke-KoikiMaven -Label 'Record the Data production dependency tree' -Arguments @(
        '-f', $dataPom, 'dependency:tree', '-Dscope=runtime', "-DoutputFile=$dataTree")

    $identityDependencies = Get-Content -Raw -LiteralPath $identityTree
    foreach ($requiredDependency in @(
        'org.koikifw:koiki-starter-security:jar:',
        'org.koikifw:koiki-starter-data-jpa:jar:',
        'org.koikifw:koiki-starter-audit:jar:',
        'org.jspecify:jspecify:jar:')) {
        if ($identityDependencies -notmatch [regex]::Escape($requiredDependency)) {
            throw "Required Identity dependency is missing: $requiredDependency"
        }
    }
    foreach ($forbiddenDependency in @(
        'spring-session',
        'spring-security-saml2',
        'spring-boot-starter-mail',
        'spring-boot-starter-webflux',
        'software.amazon.awssdk')) {
        if ($identityDependencies -match [regex]::Escape($forbiddenDependency)) {
            throw "Deferred dependency leaked into Identity: $forbiddenDependency"
        }
    }

    $dataDependencies = Get-Content -Raw -LiteralPath $dataTree
    if ($dataDependencies -notmatch [regex]::Escape('org.flywaydb:flyway-database-postgresql:jar:')) {
        throw 'The PostgreSQL Flyway database module is missing from the Data Starter.'
    }

    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
        'org/koikifw/buildsupport/security-foundation-verification'))) {
        throw 'The non-distributed T4 fixture was installed into the release repository.'
    }

    Write-Host 'Phase 2 P2-B2 Identity core verification succeeded (T0-T4 39/39).'
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$formalReleaseProjects = '!koiki-reference-app'
$auditPom = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-audit/pom.xml'
$fixturePom = Join-Path $PSScriptRoot 'pom.xml'
$auditInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-audit/public-api.txt'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-audit-transaction-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$productionTree = Join-Path $verificationRoot 'production-dependency-tree.txt'
$fixtureTree = Join-Path $verificationRoot 'fixture-dependency-tree.txt'
$fixtureTarget = Join-Path $PSScriptRoot 'target'
$expectedSuites = [ordered]@{
    'SecurityDependencyBaselineTest' = 1
    'SecurityAutoConfigurationContextTest' = 5
    'SecurityRequestBoundaryTest' = 4
    'LocalSessionAuthorizationTest' = 6
    'BearerProfileBoundaryTest' = 5
    'OidcProfileCoexistenceTest' = 2
    'AuditTransactionFixtureTest' = 8
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
    }
}

function Assert-NoSensitiveContent {
    param([Parameter(Mandatory)][System.IO.FileInfo[]]$Files)

    $forbiddenPatterns = [ordered]@{
        'private key material' = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
        'credential assignment' = '(?i)(?:password|client[_-]?secret|access[_-]?token)\s*[:=]\s*(?!\?)[^\s<]+'
        'email-shaped PII' = '(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}'
        'authorization header' = '(?i)authorization\s*[:=]\s*(?:basic|bearer)\s+'
    }

    foreach ($file in $Files) {
        $content = [System.Text.Encoding]::Latin1.GetString(
            [System.IO.File]::ReadAllBytes($file.FullName))
        foreach ($entry in $forbiddenPatterns.GetEnumerator()) {
            if ($content -match $entry.Value) {
                throw "Sensitive content detected in $($file.FullName): $($entry.Key)"
            }
        }
    }
}

function Assert-AuditContract {
    param([Parameter(Mandatory)][System.IO.FileInfo]$FormalJar)

    $actualInventory = @(Get-Content -LiteralPath $auditInventory |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne '' -and -not $_.StartsWith('#') })
    $expectedInventory = @(
        'ARTIFACT koiki-starter-audit',
        'PUBLIC_JAVA_TYPES 6',
        'TYPE org.koikifw.audit.AuditActor',
        'TYPE org.koikifw.audit.AuditEvent',
        'TYPE org.koikifw.audit.AuditRecordingException',
        'TYPE org.koikifw.audit.AuditResult',
        'TYPE org.koikifw.audit.BusinessAuditRecorder',
        'TYPE org.koikifw.audit.SecurityAuditRecorder',
        'PUBLIC_CONFIGURATION_PROPERTIES 0',
        'PUBLIC_AUDIT_ERROR_CODES 0',
        'INTERNAL_PACKAGE org.koikifw.audit.internal',
        'BUSINESS_TRANSACTION MANDATORY',
        'SECURITY_TRANSACTION REQUIRES_NEW',
        'AUDIT_SOURCE_OF_TRUTH DATABASE')
    if (@(Compare-Object -ReferenceObject $expectedInventory `
                -DifferenceObject $actualInventory -SyncWindow 0).Count -ne 0) {
        throw 'Audit Public API inventory differs from the approved P2-B1 contract.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($FormalJar.FullName)
    try {
        $actualPublicClasses = @($archive.Entries |
            Where-Object {
                $_.FullName -match '^org/koikifw/audit/[^/]+\.class$' `
                    -and $_.FullName -notmatch '/package-info\.class$'
            } |
            Select-Object -ExpandProperty FullName |
            Sort-Object)
        $expectedPublicClasses = @(
            'org/koikifw/audit/AuditActor.class',
            'org/koikifw/audit/AuditEvent.class',
            'org/koikifw/audit/AuditRecordingException.class',
            'org/koikifw/audit/AuditResult.class',
            'org/koikifw/audit/BusinessAuditRecorder.class',
            'org/koikifw/audit/SecurityAuditRecorder.class')
        if (@(Compare-Object -ReferenceObject $expectedPublicClasses `
                    -DifferenceObject $actualPublicClasses).Count -ne 0) {
            throw "Formal Audit JAR contains an unexpected public-package type: $($actualPublicClasses -join ', ')"
        }

        $configurationMetadata = @($archive.Entries | Where-Object {
            $_.FullName -in @(
                'META-INF/spring-configuration-metadata.json',
                'META-INF/additional-spring-configuration-metadata.json')
        })
        if ($configurationMetadata.Count -ne 0) {
            throw 'P2-B1 must not publish configuration property metadata.'
        }

        $importsEntry = $archive.GetEntry(
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports')
        if ($null -eq $importsEntry) {
            throw 'Audit Auto Configuration imports resource is missing.'
        }
        $reader = [System.IO.StreamReader]::new($importsEntry.Open())
        try {
            $imports = $reader.ReadToEnd().Trim()
        } finally {
            $reader.Dispose()
        }
        if ($imports -ne 'org.koikifw.audit.internal.KoikiAuditAutoConfiguration') {
            throw "Unexpected Audit Auto Configuration import: $imports"
        }
    } finally {
        $archive.Dispose()
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage the formal KOIKI release unit' -Arguments @(
        '-f', $rootPom, '-pl', $formalReleaseProjects,
        'clean', 'install', '-DskipTests')

    $auditArtifactRoot = Join-Path $isolatedRepository (
        'org/koikifw/koiki-starter-audit/0.1.0-SNAPSHOT')
    foreach ($requiredArtifact in @(
        'koiki-starter-audit-0.1.0-SNAPSHOT.pom',
        'koiki-starter-audit-0.1.0-SNAPSHOT.jar')) {
        if (-not (Test-Path -LiteralPath (Join-Path $auditArtifactRoot $requiredArtifact))) {
            throw "Formal Audit artifact is missing: $requiredArtifact"
        }
    }

    Invoke-KoikiMaven -Label 'Verify the cumulative T0-T4 fixture with PostgreSQL' -Arguments @(
        '-f', $fixturePom, 'clean', 'verify')
    Assert-SurefireResults

    Invoke-KoikiMaven -Label 'Record the Audit production dependency tree' -Arguments @(
        '-f', $auditPom, 'dependency:tree', '-Dscope=runtime', "-DoutputFile=$productionTree")
    Invoke-KoikiMaven -Label 'Record the fixture dependency tree' -Arguments @(
        '-f', $fixturePom, 'dependency:tree', '-Dscope=test', "-DoutputFile=$fixtureTree")

    $productionDependencies = Get-Content -Raw -LiteralPath $productionTree
    $fixtureDependencies = Get-Content -Raw -LiteralPath $fixtureTree
    foreach ($requiredProductionDependency in @(
        'org.koikifw:koiki-starter-data-jpa:jar:',
        'org.springframework.boot:spring-boot-starter-data-jpa:jar:',
        'org.jspecify:jspecify:jar:',
        'org.slf4j:slf4j-api:jar:')) {
        if ($productionDependencies -notmatch [regex]::Escape($requiredProductionDependency)) {
            throw "Required Audit production dependency is missing: $requiredProductionDependency"
        }
    }
    foreach ($requiredTestDependency in @(
        'org.koikifw:koiki-starter-audit:jar:',
        'org.koikifw:koiki-testing:jar:',
        'org.testcontainers:testcontainers-postgresql:jar:',
        'org.postgresql:postgresql:jar:')) {
        if ($fixtureDependencies -notmatch [regex]::Escape($requiredTestDependency)) {
            throw "Required T4 test dependency is missing: $requiredTestDependency"
        }
    }

    foreach ($forbiddenProductionDependency in @(
        'koiki-starter-security',
        'koiki-starter-observability',
        'spring-modulith',
        'spring-security-saml2',
        'spring-session-data-redis',
        'spring-boot-starter-webflux',
        'software.amazon.awssdk')) {
        if ($productionDependencies -match [regex]::Escape($forbiddenProductionDependency)) {
            throw "Deferred or unrelated dependency leaked into the Audit artifact: $forbiddenProductionDependency"
        }
    }

    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
        'org/koikifw/buildsupport/security-foundation-verification'))) {
        throw 'The non-distributed T4 fixture was installed into the release repository.'
    }
    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
        'org/koikifw/koiki-reference-app'))) {
        throw 'The Reference executable was installed into the Framework release repository.'
    }

    $fixtureJar = Get-Item -LiteralPath (Join-Path $fixtureTarget (
        'security-foundation-verification-0.1.0-SNAPSHOT.jar'))
    $formalJar = Get-Item -LiteralPath (Join-Path $auditArtifactRoot (
        'koiki-starter-audit-0.1.0-SNAPSHOT.jar'))
    $reportFiles = @(Get-ChildItem -LiteralPath (Join-Path $fixtureTarget 'surefire-reports') -File)
    Assert-AuditContract -FormalJar $formalJar
    Assert-NoSensitiveContent -Files (@($formalJar, $fixtureJar) + $reportFiles)

    Write-Host 'Phase 2 P2-B1 Audit transaction verification succeeded (T0-T4 31/31).'
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
    if (Test-Path -LiteralPath $fixtureTarget) {
        $resolvedFixtureTarget = [System.IO.Path]::GetFullPath($fixtureTarget)
        $expectedFixtureTarget = [System.IO.Path]::GetFullPath(
            (Join-Path $PSScriptRoot 'target'))
        if (-not $resolvedFixtureTarget.Equals(
                $expectedFixtureTarget,
                [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove an unexpected fixture target: $resolvedFixtureTarget"
        }
        Remove-Item -LiteralPath $resolvedFixtureTarget -Recurse -Force
    }
}

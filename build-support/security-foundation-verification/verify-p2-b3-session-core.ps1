[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$sessionRoot = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-session-jdbc'
$sessionPom = Join-Path $sessionRoot 'pom.xml'
$sessionInventory = Join-Path $sessionRoot 'public-api.txt'
$fixturePom = Join-Path $PSScriptRoot 'pom.xml'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-session-core-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$dependencyTree = Join-Path $verificationRoot 'session-dependency-tree.txt'
$verificationLog = Join-Path $verificationRoot 'verification-output.log'
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
    'IdentityAuthenticationAutoConfigurationContextTest' = 3
    'IdentityAuthenticationFixtureTest' = 6
    'IdentityAdministrationAutoConfigurationContextTest' = 2
    'IdentityAdministrationFixtureTest' = 6
    'SessionJdbcAutoConfigurationContextTest' = 5
    'SessionJdbcCoreMigrationFixtureTest' = 5
}
$forbiddenSensitivePatterns = [ordered]@{
    'private key material' = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
    'credential assignment' = '(?i)(?:password|client[_-]?secret|access[_-]?token)\s*[:=]\s*(?!\?)[^\s<]+'
    'source HMAC key assignment' = '(?i)source-hmac-key\s*=\s*[^\s<,\"]+'
    'email-shaped PII' = '(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}'
    'authorization header' = '(?i)authorization\s*[:=]\s*(?:basic|bearer)\s+'
}

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    $temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if (-not $resolvedPath.StartsWith($temporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the temporary directory: $resolvedPath"
    }
}

function Assert-NoSensitiveText {
    param(
        [Parameter(Mandatory)][AllowEmptyString()][string]$Content,
        [Parameter(Mandatory)][string]$Source
    )

    foreach ($entry in $forbiddenSensitivePatterns.GetEnumerator()) {
        if ($Content -match $entry.Value) {
            throw "Sensitive content detected in ${Source}: $($entry.Key)"
        }
    }
}

function Invoke-KoikiMaven {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host "=== $Label ==="
    $output = @(& $wrapper --batch-mode --no-transfer-progress `
            "-Dmaven.repo.local=$isolatedRepository" @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    Assert-NoSensitiveText -Content ($output -join [Environment]::NewLine) `
        -Source "$Label Maven output"
    $output | ForEach-Object { Write-Host $_ }
    $output | Add-Content -LiteralPath $verificationLog
    if ($exitCode -ne 0) {
        throw "$Label failed with exit code $exitCode"
    }
}

function Assert-SurefireResults {
    $total = 0
    foreach ($entry in $expectedSuites.GetEnumerator()) {
        $reportPath = Join-Path $fixtureTarget (
            'surefire-reports/TEST-org.koikifw.buildsupport.security.{0}.xml' -f $entry.Key)
        if (-not (Test-Path -LiteralPath $reportPath)) {
            throw "The cumulative T0-T5 Surefire report is missing: $($entry.Key)"
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
    if ($total -ne 66) {
        throw "Unexpected cumulative test count: $total"
    }
}

function Assert-NoSensitiveContent {
    param([Parameter(Mandatory)][System.IO.FileInfo[]]$Files)

    foreach ($file in $Files) {
        $content = [System.Text.Encoding]::Latin1.GetString(
            [System.IO.File]::ReadAllBytes($file.FullName))
        Assert-NoSensitiveText -Content $content -Source $file.FullName
    }
}

function Assert-SessionContract {
    param([Parameter(Mandatory)][System.IO.FileInfo]$FormalJar)

    $actualInventory = @(Get-Content -LiteralPath $sessionInventory |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne '' -and -not $_.StartsWith('#') })
    $expectedInventory = @(
        'ARTIFACT koiki-starter-session-jdbc',
        'PUBLIC_JAVA_TYPES 0',
        'PUBLIC_KOIKI_CONFIGURATION_PROPERTIES 0',
        'SPRING_CONFIGURATION_PROPERTIES 9',
        'PROPERTY spring.session.jdbc.initialize-schema=never',
        'PROPERTY spring.session.jdbc.table-name=koiki_session',
        'PROPERTY spring.session.jdbc.cleanup-cron=-',
        'PROPERTY spring.session.jdbc.flush-mode=on-save',
        'PROPERTY spring.session.jdbc.save-mode=on-set-attribute',
        'PROPERTY server.servlet.session.cookie.http-only=true',
        'PROPERTY server.servlet.session.cookie.secure=true',
        'PROPERTY server.servlet.session.cookie.same-site=lax',
        'PROPERTY spring.session.timeout',
        'SESSION_TABLES 2',
        'TABLE koiki_session',
        'TABLE koiki_session_attributes',
        'SESSION_INVALIDATOR org.koikifw.identity.UserSessionInvalidator',
        'SESSION_LOGOUT Spring Security LogoutHandler internal',
        'SESSION_CLEANUP B3-5')
    if (@(Compare-Object -ReferenceObject $expectedInventory `
                -DifferenceObject $actualInventory -SyncWindow 0).Count -ne 0) {
        throw 'Session JDBC inventory differs from the approved B3 contract.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($FormalJar.FullName)
    try {
        $publicClasses = @($archive.Entries | Where-Object {
            $_.FullName -match '^org/koikifw/session/[^/]+\.class$' `
                -and $_.FullName -notmatch '/package-info\.class$'
        })
        if ($publicClasses.Count -ne 0) {
            throw "Session JDBC exposes an unexpected public type: $($publicClasses.FullName -join ', ')"
        }

        $migrationEntry = $archive.GetEntry(
            'db/migration/koiki/V2026090701__create_koiki_session.sql')
        if ($null -eq $migrationEntry) {
            throw 'The approved Session JDBC production migration is missing.'
        }
        $reader = [System.IO.StreamReader]::new($migrationEntry.Open())
        try {
            $migrationSql = $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
        if ([regex]::Matches($migrationSql, '(?im)^CREATE TABLE koiki_session').Count -ne 2 `
                -or $migrationSql -match '(?i)SPRING_SESSION') {
            throw 'Session migration must create exactly the two approved koiki_session tables.'
        }

        $imports = $archive.GetEntry(
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports')
        if ($null -eq $imports) {
            throw 'Session JDBC Auto Configuration imports resource is missing.'
        }
        $importsReader = [System.IO.StreamReader]::new($imports.Open())
        try {
            $importsText = $importsReader.ReadToEnd().Trim()
        } finally {
            $importsReader.Dispose()
        }
        if ($importsText -ne 'org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration') {
            throw "Unexpected Session JDBC Auto Configuration import: $importsText"
        }
    } finally {
        $archive.Dispose()
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null
$formalJar = $null
$fixtureVerificationStarted = $false

try {
    Invoke-KoikiMaven -Label 'Stage the formal KOIKI release unit' -Arguments @(
        '-f', $rootPom, 'clean', 'install', '-DskipTests')

    $artifactRoot = Join-Path $isolatedRepository (
        'org/koikifw/koiki-starter-session-jdbc/0.1.0-SNAPSHOT')
    $formalJar = Get-Item -LiteralPath (Join-Path $artifactRoot (
        'koiki-starter-session-jdbc-0.1.0-SNAPSHOT.jar'))
    Assert-SessionContract -FormalJar $formalJar

    $fixtureVerificationStarted = $true
    Invoke-KoikiMaven -Label 'Verify cumulative T0-T5 and Session core with PostgreSQL' -Arguments @(
        '-f', $fixturePom, 'clean', 'verify')
    Assert-SurefireResults

    Invoke-KoikiMaven -Label 'Record the Session JDBC production dependency tree' -Arguments @(
        '-f', $sessionPom, 'dependency:tree', '-Dscope=runtime', "-DoutputFile=$dependencyTree")
    $dependencies = Get-Content -Raw -LiteralPath $dependencyTree
    foreach ($requiredDependency in @(
        'org.koikifw:koiki-starter-identity:jar:',
        'org.koikifw:koiki-starter-data:jar:',
        'org.springframework.boot:spring-boot-starter-session-jdbc:jar:',
        'org.jspecify:jspecify:jar:')) {
        if ($dependencies -notmatch [regex]::Escape($requiredDependency)) {
            throw "Required Session JDBC dependency is missing: $requiredDependency"
        }
    }
    foreach ($forbiddenDependency in @(
        'spring-session-data-redis',
        'spring-security-saml2',
        'spring-boot-starter-webflux',
        'software.amazon.awssdk')) {
        if ($dependencies -match [regex]::Escape($forbiddenDependency)) {
            throw "Deferred dependency leaked into Session JDBC: $forbiddenDependency"
        }
    }

    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
        'org/koikifw/buildsupport/security-foundation-verification'))) {
        throw 'The non-distributed T5 fixture was installed into the release repository.'
    }

    $reportFiles = @(Get-ChildItem -LiteralPath (
            Join-Path $fixtureTarget 'surefire-reports') -File)
    Assert-NoSensitiveContent -Files (@(
            $formalJar,
            (Get-Item -LiteralPath $verificationLog)) + $reportFiles)

    Write-Host 'Phase 2 P2-B3 Session invalidation / logout verification succeeded (T0-T5 66/66).'
} catch {
    $verificationFailure = $_
    $failureEvidence = @()
    if ($null -ne $formalJar -and (Test-Path -LiteralPath $formalJar.FullName)) {
        $failureEvidence += $formalJar
    }
    if (Test-Path -LiteralPath $verificationLog) {
        $failureEvidence += Get-Item -LiteralPath $verificationLog
    }
    $reportRoot = Join-Path $fixtureTarget 'surefire-reports'
    if ($fixtureVerificationStarted -and (Test-Path -LiteralPath $reportRoot)) {
        $failureEvidence += @(Get-ChildItem -LiteralPath $reportRoot -File)
    }
    if ($failureEvidence.Count -ne 0) {
        try {
            Assert-NoSensitiveContent -Files $failureEvidence
        } catch {
            throw ("{0} Sensitive-output inspection also failed: {1}" -f `
                    $verificationFailure.Exception.Message, $_.Exception.Message)
        }
        Write-Host 'Sensitive-output inspection after verification failure succeeded.'
    }
    throw $verificationFailure
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

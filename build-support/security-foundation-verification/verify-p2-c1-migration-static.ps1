[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$startersRoot = Join-Path $repositoryRoot 'koiki-starters'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-p2-c1-migration-static-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'

$expectedMigrations = [ordered]@{
    'koiki-starter-audit' = [ordered]@{
        Source = 'koiki-starters/koiki-starter-audit/src/main/resources/db/migration/koiki/V2026090300__create_koiki_audit.sql'
        Entry = 'db/migration/koiki/V2026090300__create_koiki_audit.sql'
        Version = '2026090300'
        Tables = @('koiki_audit_event')
    }
    'koiki-starter-identity' = [ordered]@{
        Source = 'koiki-starters/koiki-starter-identity/src/main/resources/db/migration/koiki/V2026090301__create_koiki_identity.sql'
        Entry = 'db/migration/koiki/V2026090301__create_koiki_identity.sql'
        Version = '2026090301'
        Tables = @(
            'koiki_user',
            'koiki_role',
            'koiki_permission',
            'koiki_user_role',
            'koiki_role_permission',
            'koiki_password_credential',
            'koiki_login_attempt',
            'koiki_external_identity_link')
    }
    'koiki-starter-session-jdbc' = [ordered]@{
        Source = 'koiki-starters/koiki-starter-session-jdbc/src/main/resources/db/migration/koiki/V2026090701__create_koiki_session.sql'
        Entry = 'db/migration/koiki/V2026090701__create_koiki_session.sql'
        Version = '2026090701'
        Tables = @('koiki_session', 'koiki_session_attributes')
    }
}

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    $temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if (-not $resolvedPath.StartsWith(
            $temporaryRoot,
            [System.StringComparison]::OrdinalIgnoreCase)) {
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

function Get-DirectDependencyArtifactIds {
    param([Parameter(Mandatory)][string]$PomPath)

    [xml]$pom = Get-Content -Raw -LiteralPath $PomPath
    return @($pom.project.dependencies.dependency |
        ForEach-Object { [string]$_.artifactId })
}

function Read-ArchiveEntry {
    param(
        [Parameter(Mandatory)][System.IO.Compression.ZipArchive]$Archive,
        [Parameter(Mandatory)][string]$EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) {
        throw "Required migration is missing from the formal JAR: $EntryName"
    }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
}

function Assert-AuditSchema {
    param([Parameter(Mandatory)][string]$Sql)

    $expectedColumns = @(
        'event_id\s+uuid\s+primary\s+key',
        'audit_type\s+varchar\(16\)\s+not\s+null',
        'event_type\s+varchar\(128\)\s+not\s+null',
        'actor_type\s+varchar\(16\)\s+not\s+null',
        'actor_id\s+varchar\(255\)',
        'subject_id\s+varchar\(255\)',
        'resource_type\s+varchar\(128\)',
        'resource_id\s+varchar\(255\)',
        'action\s+varchar\(128\)\s+not\s+null',
        'result\s+varchar\(16\)\s+not\s+null',
        'reason_code\s+varchar\(128\)',
        'occurred_at\s+timestamp\(6\)\s+with\s+time\s+zone\s+not\s+null',
        'request_id\s+varchar\(128\)',
        'trace_id\s+varchar\(128\)')
    foreach ($column in $expectedColumns) {
        if ($Sql -notmatch "(?im)^\s*$column\s*[,)]") {
            throw "Audit migration column contract is missing or changed: $column"
        }
    }

    if ([regex]::Matches($Sql, '(?im)^\s*CREATE\s+TABLE\s+').Count -ne 1) {
        throw 'Audit migration must create exactly one table.'
    }
    foreach ($forbiddenPattern in @(
        '(?i)CREATE\s+(?:UNIQUE\s+)?INDEX',
        '(?i)\bCHECK\s*\(',
        '(?i)\bFOREIGN\s+KEY\b',
        '(?i)fixture_business_change',
        '(?i)fixture_audit_failure',
        '(?i)FORCE_AUDIT_FAILURE')) {
        if ($Sql -match $forbiddenPattern) {
            throw "Fixture-only or deferred Audit DDL leaked into production: $forbiddenPattern"
        }
    }
}

function Assert-SourceInventory {
    $actualSources = @(Get-ChildItem -LiteralPath $startersRoot -Recurse -File -Filter '*.sql' |
        Where-Object {
            $_.FullName.Replace('\', '/') -match '/src/main/resources/db/migration/koiki/'
        } |
        ForEach-Object {
            [System.IO.Path]::GetRelativePath($repositoryRoot, $_.FullName).Replace('\', '/')
        } |
        Sort-Object)
    $expectedSources = @($expectedMigrations.Values |
        ForEach-Object { $_.Source } |
        Sort-Object)
    $difference = @(Compare-Object -ReferenceObject $expectedSources `
        -DifferenceObject $actualSources -SyncWindow 0)
    if ($difference.Count -ne 0) {
        throw "Framework migration source inventory differs:`n$($difference | Out-String)"
    }

    $versions = @($expectedMigrations.Values | ForEach-Object { $_.Version })
    if (@($versions | Sort-Object -Unique).Count -ne $versions.Count) {
        throw 'Framework migration versions are not unique.'
    }
    $expectedOrder = @('2026090300', '2026090301', '2026090701')
    if (@(Compare-Object -ReferenceObject $expectedOrder `
            -DifferenceObject @($versions | Sort-Object) -SyncWindow 0).Count -ne 0) {
        throw 'Framework migration order no longer matches Audit -> Identity -> Session.'
    }

    $identityDependencies = Get-DirectDependencyArtifactIds -PomPath (
        Join-Path $startersRoot 'koiki-starter-identity/pom.xml')
    if ($identityDependencies -notcontains 'koiki-starter-audit') {
        throw 'Identity must depend directly on the Audit Starter.'
    }
    $sessionDependencies = Get-DirectDependencyArtifactIds -PomPath (
        Join-Path $startersRoot 'koiki-starter-session-jdbc/pom.xml')
    if ($sessionDependencies -notcontains 'koiki-starter-identity') {
        throw 'Session JDBC must depend directly on the Identity Starter.'
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Assert-SourceInventory

    Invoke-KoikiMaven -Label 'Verify the focused Audit reactor' -Arguments @(
        '-f', $rootPom,
        '-pl', 'koiki-starters/koiki-starter-audit',
        '-am',
        'clean', 'verify')
    Invoke-KoikiMaven -Label 'Stage the Session dependency closure' -Arguments @(
        '-f', $rootPom,
        '-pl', 'koiki-starters/koiki-starter-session-jdbc',
        '-am',
        'install', '-DskipTests')

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $allVersions = @()
    $allTables = @()
    foreach ($artifact in $expectedMigrations.GetEnumerator()) {
        $artifactId = $artifact.Key
        $contract = $artifact.Value
        $artifactRoot = Join-Path $isolatedRepository (
            "org/koikifw/$artifactId/0.1.0-SNAPSHOT")
        $jar = Get-Item -LiteralPath (Join-Path $artifactRoot (
            "$artifactId-0.1.0-SNAPSHOT.jar"))
        $archive = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
        try {
            $migrationEntries = @($archive.Entries |
                Where-Object { $_.FullName -match '^db/migration/koiki/[^/]+\.sql$' } |
                Select-Object -ExpandProperty FullName)
            if (@(Compare-Object -ReferenceObject @($contract.Entry) `
                    -DifferenceObject $migrationEntries -SyncWindow 0).Count -ne 0) {
                throw "$artifactId contains an unexpected Framework migration inventory."
            }

            $sql = Read-ArchiveEntry -Archive $archive -EntryName $contract.Entry
            $versionMatch = [regex]::Match(
                $contract.Entry,
                '^db/migration/koiki/V([0-9]+)__')
            if (-not $versionMatch.Success -or $versionMatch.Groups[1].Value -ne $contract.Version) {
                throw "$artifactId migration version differs from the approved contract."
            }
            $allVersions += $contract.Version

            $tables = @([regex]::Matches(
                    $sql,
                    '(?im)^\s*CREATE\s+TABLE\s+(koiki_[a-z0-9_]+)') |
                ForEach-Object { $_.Groups[1].Value })
            if (@(Compare-Object -ReferenceObject @($contract.Tables) `
                    -DifferenceObject $tables -SyncWindow 0).Count -ne 0) {
                throw "$artifactId table ownership differs from the approved contract."
            }
            $allTables += $tables

            if ($artifactId -eq 'koiki-starter-audit') {
                Assert-AuditSchema -Sql $sql
            }
            if ($sql -match '(?i)fixture_business_change|fixture_audit_failure|FORCE_AUDIT_FAILURE') {
                throw "Fixture-only SQL leaked into $artifactId."
            }
        } finally {
            $archive.Dispose()
        }
    }

    if (@($allVersions | Sort-Object -Unique).Count -ne 3) {
        throw 'Packaged Framework migration versions are not unique.'
    }
    if (@($allTables | Sort-Object -Unique).Count -ne 11 -or $allTables.Count -ne 11) {
        throw 'Packaged Framework migrations must own exactly 11 unique tables.'
    }

    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
        'org/koikifw/buildsupport/security-foundation-verification'))) {
        throw 'The non-distributed verification fixture was installed into the release repository.'
    }
    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
        'org/koikifw/koiki-reference-app'))) {
        throw 'The Reference application was installed into the release repository.'
    }

    Write-Host 'Phase 2 P2-C1 C1-2 migration static verification succeeded (3 migrations / 11 tables).'
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

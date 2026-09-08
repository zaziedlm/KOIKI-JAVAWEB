[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$fixtureRoot = Join-Path $PSScriptRoot 'session-cleanup-process-fixture'
$fixturePom = Join-Path $fixtureRoot 'pom.xml'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$javaTool = if ($IsWindows) {
    Join-Path $env:JAVA_HOME 'bin/java.exe'
} else {
    Join-Path $env:JAVA_HOME 'bin/java'
}
$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot (
    'koiki-session-cleanup-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$fixtureBuildRoot = Join-Path $verificationRoot 'fixture-build'
$mavenLog = Join-Path $verificationRoot 'maven-output.log'
$containerName = 'koiki-b35-' + [System.Guid]::NewGuid().ToString('N')
$databaseName = 'postgres'
$databasePassword = [System.Guid]::NewGuid().ToString('N') +
    [System.Guid]::NewGuid().ToString('N')
$fixtureJar = $null
$containerStarted = $false
$verificationSucceeded = $false
$cleanupFailure = $null
$startedProcesses = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
$processLogs = [System.Collections.Generic.List[System.IO.FileInfo]]::new()
$forbiddenSensitivePatterns = [ordered]@{
    'private key material' = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
    'credential assignment' = '(?i)(?:password|client[_-]?secret|access[_-]?token)\s*[:=]\s*(?!\?)[^\s<]+'
    'email-shaped PII' = '(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}'
    'authorization header' = '(?i)authorization\s*[:=]\s*(?:basic|bearer)\s+'
    'session cookie value' = '(?i)(?:set-cookie:\s*)?SESSION=[A-Za-z0-9_-]+'
}

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-session-cleanup-*') {
        throw "Unexpected B3-5 temporary directory name: $resolved"
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

    $output = @(& $wrapper --batch-mode --no-transfer-progress `
            "-Dmaven.repo.local=$isolatedRepository" @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $content = $output -join [Environment]::NewLine
    Assert-NoSensitiveText -Content $content -Source "$Label Maven output"
    $content | Add-Content -LiteralPath $mavenLog
    if ($exitCode -ne 0) {
        throw "$Label failed with exit code $exitCode"
    }
}

function Invoke-Docker {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    $output = @(& docker @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed during B3-5 verification."
    }
    return ($output -join [Environment]::NewLine).Trim()
}

function Invoke-Postgres {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string]$Sql
    )

    return Invoke-Docker -Label $Label -Arguments @(
        'exec', $containerName,
        'psql', '--no-psqlrc', '--tuples-only', '--no-align',
        '--set', 'ON_ERROR_STOP=1',
        '--username', 'postgres', '--dbname', $databaseName,
        '--command', $Sql)
}

function Start-CleanupProcess {
    param([Parameter(Mandatory)][string]$Name)

    $outputPath = Join-Path $verificationRoot "$Name.log"
    $errorPath = Join-Path $verificationRoot "$Name-error.log"
    $startArguments = @{
        FilePath = $javaTool
        ArgumentList = @('-jar', $fixtureJar)
        PassThru = $true
        RedirectStandardOutput = $outputPath
        RedirectStandardError = $errorPath
        Environment = @{
            'SPRING_DATASOURCE_URL' =
                "jdbc:postgresql://127.0.0.1:$databasePort/$databaseName" +
                '?ApplicationName=koiki-b35-cleanup&loggerLevel=OFF'
            'SPRING_DATASOURCE_USERNAME' = 'postgres'
            'SPRING_DATASOURCE_PASSWORD' = $databasePassword
        }
    }
    if ($IsWindows) {
        $startArguments.WindowStyle = 'Hidden'
    }
    $process = Start-Process @startArguments
    $startedProcesses.Add($process)
    $processLogs.Add([System.IO.FileInfo]::new($outputPath))
    $processLogs.Add([System.IO.FileInfo]::new($errorPath))
    return $process
}

function Wait-ProcessExitCode {
    param(
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][int]$TimeoutSeconds
    )

    if (-not $Process.WaitForExit($TimeoutSeconds * 1000)) {
        throw "Cleanup process $($Process.Id) did not exit within $TimeoutSeconds seconds."
    }
    $Process.Refresh()
    return $Process.ExitCode
}

function Wait-AdvisoryLockState {
    param(
        [Parameter(Mandatory)][bool]$Present,
        [int]$TimeoutSeconds = 30
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $countText = Invoke-Postgres -Label 'PostgreSQL advisory lock observation' -Sql (
            "SELECT count(*) FROM pg_locks l " +
            "JOIN pg_stat_activity a ON a.pid = l.pid " +
            "WHERE l.locktype = 'advisory' AND l.granted " +
            "AND a.application_name = 'koiki-b35-cleanup';")
        $observed = [int]$countText -gt 0
        if ($observed -eq $Present) {
            return
        }
        Start-Sleep -Milliseconds 100
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw "The expected PostgreSQL advisory lock state was not observed."
}

function Add-SessionRows {
    param([Parameter(Mandatory)][string]$Prefix)

    $expiredPrimary = [System.Guid]::NewGuid().ToString()
    $expiredSession = [System.Guid]::NewGuid().ToString()
    $activePrimary = [System.Guid]::NewGuid().ToString()
    $activeSession = [System.Guid]::NewGuid().ToString()
    $futureExpiry = [DateTimeOffset]::UtcNow.AddHours(1).ToUnixTimeMilliseconds()
    Invoke-Postgres -Label "$Prefix Session row setup" -Sql (
        "INSERT INTO koiki_session " +
        "(primary_id, session_id, creation_time, last_access_time, " +
        "max_inactive_interval, expiry_time) VALUES " +
        "('$expiredPrimary', '$expiredSession', 0, 0, 1, 0), " +
        "('$activePrimary', '$activeSession', 0, 0, 3600, $futureExpiry); " +
        "INSERT INTO koiki_session_attributes " +
        "(session_primary_id, attribute_name, attribute_bytes) VALUES " +
        "('$expiredPrimary', 'fixture', decode('01', 'hex')), " +
        "('$activePrimary', 'fixture', decode('02', 'hex'));") | Out-Null
}

function Assert-SessionCounts {
    param(
        [Parameter(Mandatory)][int]$Expired,
        [Parameter(Mandatory)][int]$Active,
        [Parameter(Mandatory)][int]$Attributes
    )

    $actual = Invoke-Postgres -Label 'Session cleanup DB observation' -Sql (
        "SELECT count(*) FILTER (WHERE expiry_time < " +
        "(extract(epoch FROM clock_timestamp()) * 1000)::bigint), " +
        "count(*) FILTER (WHERE expiry_time >= " +
        "(extract(epoch FROM clock_timestamp()) * 1000)::bigint) " +
        "FROM koiki_session; " +
        "SELECT count(*) FROM koiki_session_attributes;")
    $lines = @($actual -split "`r?`n" | Where-Object { $_ -ne '' })
    if ($lines.Count -ne 2 -or $lines[0] -ne "$Expired|$Active" `
            -or $lines[1] -ne [string]$Attributes) {
        throw "Unexpected Session row state after cleanup."
    }
}

function Install-DelayTrigger {
    Invoke-Postgres -Label 'Cleanup delay trigger setup' -Sql (
        "CREATE TABLE IF NOT EXISTS b35_cleanup_observation " +
        "(id integer PRIMARY KEY, delete_attempts integer NOT NULL); " +
        "INSERT INTO b35_cleanup_observation VALUES (1, 0) " +
        "ON CONFLICT (id) DO UPDATE SET delete_attempts = 0; " +
        "CREATE OR REPLACE FUNCTION b35_delay_session_delete() RETURNS trigger " +
        "LANGUAGE plpgsql AS 'BEGIN " +
        "UPDATE b35_cleanup_observation SET delete_attempts = delete_attempts + 1 " +
        "WHERE id = 1; PERFORM pg_sleep(15); RETURN OLD; END'; " +
        "DROP TRIGGER IF EXISTS b35_delay_session_delete ON koiki_session; " +
        "CREATE TRIGGER b35_delay_session_delete BEFORE DELETE ON koiki_session " +
        "FOR EACH ROW EXECUTE FUNCTION b35_delay_session_delete();") | Out-Null
}

function Remove-DelayTrigger {
    Invoke-Postgres -Label 'Cleanup delay trigger removal' -Sql (
        "DROP TRIGGER IF EXISTS b35_delay_session_delete ON koiki_session;") | Out-Null
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository, $fixtureBuildRoot -Force |
    Out-Null

try {
    if (-not (Test-Path -LiteralPath $javaTool -PathType Leaf)) {
        throw 'Java 21 was not found through JAVA_HOME.'
    }
    Invoke-Docker -Label 'Docker Engine check' `
        -Arguments @('version', '--format', '{{.Server.Version}}') | Out-Null

    Invoke-KoikiMaven -Label 'Stage the formal KOIKI release unit' -Arguments @(
        '-f', $rootPom, 'clean', 'install', '-DskipTests')
    Invoke-KoikiMaven -Label 'Package the non-distributed B3-5 fixture' -Arguments @(
        '-f', $fixturePom, 'clean', 'package',
        "-Dkoiki.fixture.build.directory=$fixtureBuildRoot")

    $fixtureJar = Join-Path $fixtureBuildRoot (
        'session-cleanup-process-fixture-0.1.0-SNAPSHOT.jar')
    if (-not (Test-Path -LiteralPath $fixtureJar -PathType Leaf)) {
        throw 'The B3-5 executable fixture JAR was not created.'
    }
    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
            'org/koikifw/buildsupport/session-cleanup-process-fixture'))) {
        throw 'The non-distributed B3-5 fixture was installed into the release repository.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $formalJars = @(Get-ChildItem -LiteralPath (Join-Path $isolatedRepository 'org/koikifw') `
            -Recurse -Filter '*.jar' -File)
    foreach ($formalJar in $formalJars) {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($formalJar.FullName)
        try {
            if ($archive.Entries.FullName -match
                    '^org/koikifw/buildsupport/sessioncleanupfixture/') {
                throw 'A formal KOIKI JAR contains the B3-5 fixture package.'
            }
        } finally {
            $archive.Dispose()
        }
    }

    Invoke-Docker -Label 'PostgreSQL container start' -Arguments @(
        'run', '--detach', '--rm', '--name', $containerName,
        '--publish', '127.0.0.1::5432',
        '--env', "POSTGRES_PASSWORD=$databasePassword",
        'postgres:17-alpine') | Out-Null
    $containerStarted = $true

    $databaseReady = $false
    $databaseDeadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
    while ([DateTimeOffset]::UtcNow -lt $databaseDeadline) {
        try {
            Invoke-Docker -Label 'PostgreSQL readiness' -Arguments @(
                'exec', $containerName, 'pg_isready',
                '--username', 'postgres', '--dbname', $databaseName) | Out-Null
            $databaseReady = $true
            break
        } catch {
            Start-Sleep -Milliseconds 250
        }
    }
    if (-not $databaseReady) {
        throw 'PostgreSQL did not become ready within 60 seconds.'
    }

    $databasePortText = Invoke-Docker -Label 'PostgreSQL port lookup' -Arguments @(
        'port', $containerName, '5432/tcp')
    if ($databasePortText -notmatch '127\.0\.0\.1:(?<port>[0-9]+)$') {
        throw 'The PostgreSQL loopback port could not be determined.'
    }
    $databasePort = $Matches.port

    $bootstrap = Start-CleanupProcess -Name 'bootstrap'
    if ((Wait-ProcessExitCode -Process $bootstrap -TimeoutSeconds 60) -ne 0) {
        throw 'The non-web bootstrap cleanup process did not complete.'
    }

    Add-SessionRows -Prefix 'Contention'
    Install-DelayTrigger
    $winner = Start-CleanupProcess -Name 'contention-winner'
    Wait-AdvisoryLockState -Present $true
    $contender = Start-CleanupProcess -Name 'contention-contender'
    if ((Wait-ProcessExitCode -Process $contender -TimeoutSeconds 30) -ne 10) {
        throw 'The contending cleanup process did not return exit 10.'
    }
    $winner.Refresh()
    if ($winner.HasExited) {
        throw 'The winning cleanup process ended before contention was observed.'
    }
    if ((Wait-ProcessExitCode -Process $winner -TimeoutSeconds 40) -ne 0) {
        throw 'The winning cleanup process did not return exit 0.'
    }
    Wait-AdvisoryLockState -Present $false
    Assert-SessionCounts -Expired 0 -Active 1 -Attributes 1
    $deleteAttempts = Invoke-Postgres -Label 'Cleanup side-effect observation' -Sql (
        'SELECT delete_attempts FROM b35_cleanup_observation WHERE id = 1;')
    if ($deleteAttempts -ne '1') {
        throw 'Contention caused more than one cleanup side effect.'
    }

    Invoke-Postgres -Label 'Crash scenario reset' -Sql (
        'DELETE FROM koiki_session_attributes; DELETE FROM koiki_session; ' +
        'UPDATE b35_cleanup_observation SET delete_attempts = 0 WHERE id = 1;') | Out-Null
    Add-SessionRows -Prefix 'Crash recovery'
    $victim = Start-CleanupProcess -Name 'crash-victim'
    Wait-AdvisoryLockState -Present $true
    Stop-Process -Id $victim.Id
    if (-not $victim.WaitForExit(10000)) {
        throw 'The cleanup victim process did not stop after OS termination.'
    }
    Wait-AdvisoryLockState -Present $false
    Assert-SessionCounts -Expired 1 -Active 1 -Attributes 2
    $rolledBackAttempts = Invoke-Postgres -Label 'Killed cleanup rollback observation' -Sql (
        'SELECT delete_attempts FROM b35_cleanup_observation WHERE id = 1;')
    if ($rolledBackAttempts -ne '0') {
        throw 'The killed cleanup process left a committed delete side effect.'
    }

    Remove-DelayTrigger
    $retry = Start-CleanupProcess -Name 'crash-retry'
    if ((Wait-ProcessExitCode -Process $retry -TimeoutSeconds 60) -ne 0) {
        throw 'Cleanup retry after process termination did not return exit 0.'
    }
    Assert-SessionCounts -Expired 0 -Active 1 -Attributes 1

    foreach ($log in $processLogs) {
        if (Test-Path -LiteralPath $log.FullName) {
            $content = Get-Content -Raw -LiteralPath $log.FullName
            Assert-NoSensitiveText -Content $content -Source $log.Name
            if ($content -match '(?i)Tomcat started|Netty started|started on port') {
                throw 'The B3-5 maintenance fixture started a web server.'
            }
        }
    }
    Assert-NoSensitiveText -Content (Get-Content -Raw -LiteralPath $mavenLog) `
        -Source 'B3-5 Maven log'

    $verificationSucceeded = $true
} finally {
    foreach ($process in $startedProcesses) {
        try {
            $process.Refresh()
            if (-not $process.HasExited) {
                Stop-Process -Id $process.Id
                $process.WaitForExit(10000) | Out-Null
            }
        } catch {
            if ($null -eq $cleanupFailure) {
                $cleanupFailure = $_
            }
        }
    }
    if ($containerStarted) {
        try {
            Invoke-Docker -Label 'PostgreSQL container cleanup' `
                -Arguments @('stop', $containerName) | Out-Null
        } catch {
            if ($null -eq $cleanupFailure) {
                $cleanupFailure = $_
            }
        }
    }
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

if ($null -ne $cleanupFailure) {
    throw $cleanupFailure
}
if ($verificationSucceeded) {
    Write-Host (
        'Phase 2 P2-B3 B3-5 Session cleanup / single execution verification ' +
        'succeeded (expired-only cleanup, exit 0/10, contention, crash recovery).')
}

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$referencePom = Join-Path $repositoryRoot 'koiki-reference-app/pom.xml'
$referenceTarget = Join-Path $repositoryRoot 'koiki-reference-app/target'
$referenceJar = Join-Path $referenceTarget 'koiki-reference-app-0.1.0-SNAPSHOT.jar'
$fixtureRoot = Join-Path $PSScriptRoot 'migration-profile-fixture'
$fixturePom = Join-Path $fixtureRoot 'pom.xml'
$fixtureTarget = Join-Path $fixtureRoot 'target'
$phase1bBaseline = '40d16f9dbf26a7ba88ac13b2e3728075e0eff2a7'
$phase1bCustomerPath = 'build-support/runtime-foundation-consumer/application/src/main/resources/db/migration/customer'
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
    'koiki-p2-c1-postgresql-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$mavenLog = Join-Path $verificationRoot 'maven-output.log'
$containerName = 'koiki-p2-c1-' + [System.Guid]::NewGuid().ToString('N')
$databaseNamePrefix = 'koiki_c1_'
$appRole = 'koiki_c1_app'
$containerPassword = [System.Guid]::NewGuid().ToString('N')
$appPassword = [System.Guid]::NewGuid().ToString('N') + [System.Guid]::NewGuid().ToString('N')
$hmacBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($hmacBytes)
$hmacKey = [Convert]::ToBase64String($hmacBytes)
$hmacKeyId = 'c1-' + [System.Guid]::NewGuid().ToString('N')
$runtimeSensitiveValues = @($containerPassword, $appPassword, $hmacKey)
$containerStarted = $false
$referenceProcess = $null
$referenceLogs = [System.Collections.Generic.List[string]]::new()

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-p2-c1-postgresql-*') {
        throw "Unexpected P2-C1 temporary directory name: $resolved"
    }
}

function Assert-NoSensitiveText {
    param(
        [Parameter(Mandatory)][AllowEmptyString()][string]$Content,
        [Parameter(Mandatory)][string]$Source
    )

    foreach ($value in $runtimeSensitiveValues) {
        if ($Content.Contains($value, [System.StringComparison]::Ordinal)) {
            throw "Runtime secret detected in $Source."
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
    $text = $output -join [Environment]::NewLine
    Assert-NoSensitiveText -Content $text -Source "$Label Maven output"
    $text | Add-Content -LiteralPath $mavenLog
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "$Label failed with exit code $exitCode."
    }
}

function Invoke-Docker {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    $output = @(& docker @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed during P2-C1 verification."
    }
    return ($output -join [Environment]::NewLine).Trim()
}

function Invoke-Postgres {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$Sql
    )

    return Invoke-Docker -Label $Label -Arguments @(
        'exec', $containerName,
        'psql', '--no-psqlrc', '--tuples-only', '--no-align',
        '--set', 'ON_ERROR_STOP=1',
        '--username', 'postgres', '--dbname', $Database,
        '--command', $Sql)
}

function New-ProfileDatabase {
    param([Parameter(Mandatory)][string]$Suffix)

    $database = $databaseNamePrefix + $Suffix
    Invoke-Docker -Label "Create $Suffix database" -Arguments @(
        'exec', $containerName,
        'createdb', '--username', 'postgres', '--owner', $appRole, $database) | Out-Null
    return $database
}

function Set-FixtureDatabaseEnvironment {
    param(
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$DatabasePort
    )

    $env:KOIKI_P2_C1_JDBC_URL =
        "jdbc:postgresql://127.0.0.1:$DatabasePort/$Database" + '?loggerLevel=OFF'
    $env:KOIKI_P2_C1_JDBC_USER = $appRole
    $env:KOIKI_P2_C1_JDBC_PASSWORD = $appPassword
}

function Invoke-ProfileFixture {
    param(
        [Parameter(Mandatory)][string]$Profile,
        [Parameter(Mandatory)][string]$ExpectedProfile,
        [Parameter(Mandatory)][string]$TestClass,
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$DatabasePort
    )

    Set-FixtureDatabaseEnvironment -Database $Database -DatabasePort $DatabasePort
    Invoke-KoikiMaven -Label "Verify $ExpectedProfile PostgreSQL profile" -Arguments @(
        '-f', $fixturePom,
        'clean', 'test',
        "-P$Profile",
        "-Dtest=$TestClass",
        "-Dkoiki.fixture.profile=$ExpectedProfile")
}

function New-LoopbackPort {
    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try {
        return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

function Start-ReferenceProcess {
    param(
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$DatabasePort,
        [Parameter(Mandatory)][int]$Run
    )

    $processLog = Join-Path $verificationRoot "reference-$Run.log"
    $processError = Join-Path $verificationRoot "reference-$Run-error.log"
    [void]$referenceLogs.Add($processLog)
    [void]$referenceLogs.Add($processError)
    $startArguments = @{
        FilePath = $javaTool
        ArgumentList = @('-jar', $referenceJar)
        PassThru = $true
        RedirectStandardOutput = $processLog
        RedirectStandardError = $processError
        Environment = @{
            'SERVER_PORT' = [string]$Port
            'SERVER_SERVLET_SESSION_COOKIE_SECURE' = 'false'
            'SPRING_DATASOURCE_URL' =
                "jdbc:postgresql://127.0.0.1:$DatabasePort/$Database" +
                '?loggerLevel=OFF'
            'SPRING_DATASOURCE_USERNAME' = $appRole
            'SPRING_DATASOURCE_PASSWORD' = $appPassword
            'KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID' = $hmacKeyId
            'KOIKI_REFERENCE_SOURCE_HMAC_KEY' = $hmacKey
            'LOGGING_LEVEL_ROOT' = 'WARN'
        }
    }
    if ($IsWindows) {
        $startArguments.WindowStyle = 'Hidden'
    }
    return Start-Process @startArguments
}

function Wait-ReferenceReady {
    param(
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][int]$Port
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        $Process.Refresh()
        if ($Process.HasExited) {
            throw 'The Reference process exited before readiness.'
        }
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/login" `
                -SkipHttpErrorCheck -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 250
        }
    }
    throw 'The Reference process did not become ready within 60 seconds.'
}

function Stop-ReferenceProcess {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return
    }
    $Process.Refresh()
    if (-not $Process.HasExited) {
        Stop-Process -Id $Process.Id
        if (-not $Process.WaitForExit(10000)) {
            throw 'The Reference process did not stop within 10 seconds.'
        }
    }
}

function Assert-ReferenceArtifact {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($referenceJar)
    try {
        $entries = @($archive.Entries.FullName)
        if ($entries -match '^BOOT-INF/classes/db/migration/' -or
            $entries -match '^BOOT-INF/classes/.+\.sql$') {
            throw 'The Reference JAR must not own a production migration.'
        }
        if (-not ($entries -match (
                '^BOOT-INF/lib/koiki-starter-session-jdbc-0[.]1[.]0-SNAPSHOT[.]jar$'))) {
            throw 'The package-ready Reference JAR is missing the Session JDBC Starter.'
        }
    } finally {
        $archive.Dispose()
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    if (-not (Test-Path -LiteralPath $javaTool -PathType Leaf)) {
        throw 'Java 21 was not found through JAVA_HOME.'
    }
    & git cat-file -e "$phase1bBaseline^{commit}"
    if ($LASTEXITCODE -ne 0) {
        throw 'The approved Phase 1b baseline commit is not available locally.'
    }
    & git diff --quiet "$phase1bBaseline..HEAD" -- $phase1bCustomerPath
    if ($LASTEXITCODE -ne 0) {
        throw 'Phase 1b Customer migration resources changed after the approved baseline.'
    }
    & git diff --quiet -- $phase1bCustomerPath
    if ($LASTEXITCODE -ne 0) {
        throw 'Phase 1b Customer migration resources have uncommitted changes.'
    }

    Invoke-Docker -Label 'Docker Engine check' `
        -Arguments @('version', '--format', '{{.Server.Version}}') | Out-Null
    Invoke-KoikiMaven -Label 'Stage formal Framework release unit' -Arguments @(
        '-f', $rootPom, '-pl', '!koiki-reference-app',
        'clean', 'install', '-DskipTests')
    Invoke-KoikiMaven -Label 'Package Reference application' -Arguments @(
        '-f', $referencePom, 'clean', 'package', '-DskipTests')
    if (-not (Test-Path -LiteralPath $referenceJar -PathType Leaf)) {
        throw 'The package-ready Reference JAR was not created.'
    }
    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
            'org/koikifw/koiki-reference-app'))) {
        throw 'The Reference application entered the formal Framework release repository.'
    }
    Assert-ReferenceArtifact

    Invoke-Docker -Label 'PostgreSQL container start' -Arguments @(
        'run', '--detach', '--rm', '--name', $containerName,
        '--publish', '127.0.0.1::5432',
        '--env', "POSTGRES_PASSWORD=$containerPassword",
        'postgres:17-alpine') | Out-Null
    $containerStarted = $true

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
    while ($true) {
        try {
            Invoke-Docker -Label 'PostgreSQL readiness' -Arguments @(
                'exec', $containerName, 'pg_isready',
                '--username', 'postgres', '--dbname', 'postgres') | Out-Null
            break
        } catch {
            if ([DateTimeOffset]::UtcNow -ge $deadline) {
                throw 'PostgreSQL did not become ready within 60 seconds.'
            }
            Start-Sleep -Milliseconds 250
        }
    }
    $portText = Invoke-Docker -Label 'PostgreSQL port lookup' -Arguments @(
        'port', $containerName, '5432/tcp')
    if ($portText -notmatch '127\.0\.0\.1:(?<port>[0-9]+)$') {
        throw 'The PostgreSQL loopback port could not be determined.'
    }
    $databasePort = $Matches.port

    $roleDeadline = [DateTimeOffset]::UtcNow.AddSeconds(30)
    while ($true) {
        try {
            Invoke-Postgres -Label 'Create fixture application role' `
                -Database 'postgres' -Sql (
                    "CREATE ROLE $appRole LOGIN PASSWORD '$appPassword';") | Out-Null
            break
        } catch {
            if ([DateTimeOffset]::UtcNow -ge $roleDeadline) {
                throw
            }
            # The official image briefly exposes its initialization server before
            # restarting PostgreSQL. CREATE ROLE is one transactional statement.
            Start-Sleep -Milliseconds 250
        }
    }

    foreach ($profile in @('audit', 'identity', 'session')) {
        $database = New-ProfileDatabase -Suffix $profile
        Invoke-ProfileFixture -Profile $profile -ExpectedProfile $profile `
            -TestClass 'MigrationProfileFixtureTest' `
            -Database $database -DatabasePort $databasePort
    }

    $referenceDatabase = New-ProfileDatabase -Suffix 'reference'
    for ($run = 1; $run -le 2; $run++) {
        $referencePort = New-LoopbackPort
        $referenceProcess = Start-ReferenceProcess -Port $referencePort `
            -Database $referenceDatabase -DatabasePort $databasePort -Run $run
        Wait-ReferenceReady -Process $referenceProcess -Port $referencePort
        Stop-ReferenceProcess -Process $referenceProcess
        $referenceProcess = $null
    }
    foreach ($logPath in $referenceLogs) {
        if (Test-Path -LiteralPath $logPath) {
            Assert-NoSensitiveText -Content (Get-Content -Raw -LiteralPath $logPath) `
                -Source $logPath
        }
    }
    Invoke-ProfileFixture -Profile 'reference' -ExpectedProfile 'reference' `
        -TestClass 'MigrationProfileFixtureTest' `
        -Database $referenceDatabase -DatabasePort $databasePort

    $upgradeDatabase = New-ProfileDatabase -Suffix 'upgrade'
    Invoke-ProfileFixture -Profile 'upgrade' -ExpectedProfile 'upgrade' `
        -TestClass 'MigrationProfileFixtureTest' `
        -Database $upgradeDatabase -DatabasePort $databasePort

    $failureDatabase = New-ProfileDatabase -Suffix 'failure'
    Invoke-ProfileFixture -Profile 'failure' -ExpectedProfile 'failure' `
        -TestClass 'MigrationFailureContractFixtureTest' `
        -Database $failureDatabase -DatabasePort $databasePort

    Write-Host (
        'Phase 2 P2-C1 C1-3 PostgreSQL verification succeeded ' +
        '(4 clean profiles / Phase 1b upgrade / failure contracts).')
} finally {
    Stop-ReferenceProcess -Process $referenceProcess
    if ($containerStarted) {
        & docker rm --force $containerName 2>&1 | Out-Null
    }
    foreach ($name in @(
            'KOIKI_P2_C1_JDBC_URL',
            'KOIKI_P2_C1_JDBC_USER',
            'KOIKI_P2_C1_JDBC_PASSWORD')) {
        Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
    }
    foreach ($target in @($fixtureTarget, $referenceTarget)) {
        if (Test-Path -LiteralPath $target) {
            Remove-Item -LiteralPath $target -Recurse -Force
        }
    }
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

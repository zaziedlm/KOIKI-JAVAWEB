[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$fixtureRoot = Join-Path $PSScriptRoot 'session-two-process-fixture'
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
    'koiki-session-two-process-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$fixtureBuildRoot = Join-Path $verificationRoot 'fixture-build'
$mavenLog = Join-Path $verificationRoot 'maven-output.log'
$processALog = Join-Path $verificationRoot 'process-a.log'
$processAError = Join-Path $verificationRoot 'process-a-error.log'
$processBLog = Join-Path $verificationRoot 'process-b.log'
$processBError = Join-Path $verificationRoot 'process-b-error.log'
$containerName = 'koiki-b34-' + [System.Guid]::NewGuid().ToString('N')
$appRole = 'koiki_b34_app'
$databaseName = 'postgres'
$bootstrapKey = [System.Guid]::NewGuid().ToString('N') + [System.Guid]::NewGuid().ToString('N')
$appPassword = [System.Guid]::NewGuid().ToString('N') + [System.Guid]::NewGuid().ToString('N')
$userId = [System.Guid]::NewGuid()
$roleId = [System.Guid]::NewGuid()
$permissionId = [System.Guid]::NewGuid()
$loginEmail = 'b34-' + [System.Guid]::NewGuid().ToString('N') + '@invalid.example'
$loginPassword = 'K9!' + [System.Guid]::NewGuid().ToString('N') + 'aZ'
$containerStarted = $false
$processA = $null
$processB = $null
$fixtureJar = $null
$httpHandler = $null
$httpClient = $null
$verificationSucceeded = $false

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
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-session-two-process-*') {
        throw "Unexpected B3-4 temporary directory name: $resolved"
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

function Assert-NoSensitiveFiles {
    param([Parameter(Mandatory)][System.IO.FileInfo[]]$Files)

    foreach ($file in $Files) {
        $content = [System.Text.Encoding]::Latin1.GetString(
            [System.IO.File]::ReadAllBytes($file.FullName))
        Assert-NoSensitiveText -Content $content -Source $file.Name
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
    $text = $output -join [Environment]::NewLine
    Assert-NoSensitiveText -Content $text -Source "$Label Maven output"
    $text | Add-Content -LiteralPath $mavenLog
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
        throw "$Label failed during B3-4 verification."
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
        '--single-transaction',
        '--set', 'ON_ERROR_STOP=1',
        '--username', 'postgres', '--dbname', $databaseName,
        '--command', $Sql)
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

function Start-FixtureProcess {
    param(
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$DatabasePort,
        [Parameter(Mandatory)][string]$OutputPath,
        [Parameter(Mandatory)][string]$ErrorPath
    )

    $startArguments = @{
        FilePath = $javaTool
        ArgumentList = @('-jar', $fixtureJar)
        PassThru = $true
        RedirectStandardOutput = $OutputPath
        RedirectStandardError = $ErrorPath
        Environment = @{
            'SERVER_PORT' = [string]$Port
            'SPRING_DATASOURCE_URL' =
                "jdbc:postgresql://127.0.0.1:$DatabasePort/$databaseName" +
                '?loggerLevel=OFF'
            'SPRING_DATASOURCE_USERNAME' = $appRole
            'SPRING_DATASOURCE_PASSWORD' = $appPassword
            'KOIKI_FIXTURE_BOOTSTRAP_KEY' = $bootstrapKey
        }
    }
    if ($IsWindows) {
        $startArguments.WindowStyle = 'Hidden'
    }
    return Start-Process @startArguments
}

function Wait-FixtureReady {
    param(
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][int]$Port
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        $Process.Refresh()
        if ($Process.HasExited) {
            throw 'A fixture process exited before readiness.'
        }
        try {
            $response = Invoke-WebRequest `
                -Uri "http://127.0.0.1:$Port/fixture/readiness" `
                -SkipHttpErrorCheck -TimeoutSec 2
            if ($response.StatusCode -eq 200 -and $response.Content -eq 'B3-4-ready') {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 250
        }
    }
    throw 'A fixture process did not become ready within 60 seconds.'
}

function Stop-StartedProcess {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return
    }
    $Process.Refresh()
    if (-not $Process.HasExited) {
        Stop-Process -Id $Process.Id
        if (-not $Process.WaitForExit(10000)) {
            throw 'A fixture process did not stop within 10 seconds.'
        }
    }
}

function Invoke-AuthenticatedObservation {
    param(
        [Parameter(Mandatory)][System.Net.Http.HttpClient]$Client,
        [Parameter(Mandatory)][int]$Port
    )

    $response = $Client.GetAsync("http://127.0.0.1:$Port/fixture/session").GetAwaiter().GetResult()
    $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$response.StatusCode -ne 200 -or $content -ne "$userId|ORDER:READ") {
        throw 'The shared Session did not restore the expected Framework user and Permission.'
    }
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
    Invoke-KoikiMaven -Label 'Package the non-distributed B3-4 fixture' -Arguments @(
        '-f', $fixturePom, 'clean', 'package',
        "-Dkoiki.fixture.build.directory=$fixtureBuildRoot")

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $formalJars = @(Get-ChildItem -LiteralPath (Join-Path $isolatedRepository 'org/koikifw') `
            -Recurse -Filter '*.jar' -File)
    foreach ($formalJar in $formalJars) {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($formalJar.FullName)
        try {
            if ($archive.Entries.FullName -match '^org/koikifw/buildsupport/sessionfixture/') {
                throw 'A formal KOIKI JAR contains the B3-4 fixture package.'
            }
        } finally {
            $archive.Dispose()
        }
    }

    $fixtureJar = Join-Path $fixtureBuildRoot (
        'session-two-process-fixture-0.1.0-SNAPSHOT.jar')
    if (-not (Test-Path -LiteralPath $fixtureJar -PathType Leaf)) {
        throw 'The B3-4 executable fixture JAR was not created.'
    }
    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
            'org/koikifw/buildsupport/session-two-process-fixture'))) {
        throw 'The non-distributed B3-4 fixture was installed into the release repository.'
    }
    $jarHashBefore = (Get-FileHash -LiteralPath $fixtureJar -Algorithm SHA256).Hash

    Invoke-Docker -Label 'PostgreSQL container start' -Arguments @(
        'run', '--detach', '--rm', '--name', $containerName,
        '--publish', '127.0.0.1::5432',
        '--env', "POSTGRES_PASSWORD=$([System.Guid]::NewGuid().ToString('N'))",
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

    $appRoleReady = $false
    $appRoleDeadline = [DateTimeOffset]::UtcNow.AddSeconds(10)
    while ([DateTimeOffset]::UtcNow -lt $appRoleDeadline) {
        try {
            Invoke-Postgres -Label 'PostgreSQL app role setup' -Sql (
                "CREATE ROLE $appRole LOGIN PASSWORD '$appPassword'; " +
                "GRANT CONNECT ON DATABASE $databaseName TO $appRole; " +
                "GRANT USAGE, CREATE ON SCHEMA public TO $appRole;") | Out-Null
            $appRoleReady = $true
            break
        } catch {
            Start-Sleep -Milliseconds 250
        }
    }
    if (-not $appRoleReady) {
        throw 'PostgreSQL app role setup did not succeed within 10 seconds.'
    }

    $portA = New-LoopbackPort
    $portB = New-LoopbackPort
    while ($portB -eq $portA) {
        $portB = New-LoopbackPort
    }

    $processA = Start-FixtureProcess `
        -Port $portA -DatabasePort $databasePort `
        -OutputPath $processALog -ErrorPath $processAError
    Wait-FixtureReady -Process $processA -Port $portA

    Invoke-Postgres -Label 'PostgreSQL ownership separation' -Sql (
        "REASSIGN OWNED BY $appRole TO postgres; " +
        "REVOKE CREATE ON SCHEMA public FROM $appRole; " +
        "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO $appRole; " +
        "GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO $appRole;") | Out-Null
    $ownershipBoundary = Invoke-Postgres -Label 'PostgreSQL ownership observation' -Sql (
        "SELECT count(*) || '|' || " +
        "has_schema_privilege('$appRole', 'public', 'CREATE') || '|' || " +
        "has_table_privilege('$appRole', 'public.koiki_session', 'DELETE') " +
        "FROM pg_class object " +
        "JOIN pg_namespace namespace ON namespace.oid = object.relnamespace " +
        "WHERE namespace.nspname = 'public' " +
        "AND pg_get_userbyid(object.relowner) = '$appRole';")
    if ($ownershipBoundary -ne '0|false|true') {
        throw 'The fixture app role did not retain the approved non-owner DML boundary.'
    }

    $processB = Start-FixtureProcess `
        -Port $portB -DatabasePort $databasePort `
        -OutputPath $processBLog -ErrorPath $processBError
    Wait-FixtureReady -Process $processB -Port $portB

    $setupResponse = Invoke-WebRequest `
        -Method Post -Uri "http://127.0.0.1:$portA/fixture/setup" `
        -Headers @{'X-Koiki-Fixture-Key' = $bootstrapKey} `
        -Body @{
            userId = [string]$userId
            email = $loginEmail
            password = $loginPassword
            roleId = [string]$roleId
            permissionId = [string]$permissionId
        } `
        -SkipHttpErrorCheck -TimeoutSec 10
    if ($setupResponse.StatusCode -ne 204) {
        throw 'The fixture identity could not be initialized.'
    }

    $cookieContainer = [System.Net.CookieContainer]::new()
    $httpHandler = [System.Net.Http.HttpClientHandler]::new()
    $httpHandler.AllowAutoRedirect = $false
    $httpHandler.CookieContainer = $cookieContainer
    $httpClient = [System.Net.Http.HttpClient]::new($httpHandler)
    $httpClient.Timeout = [TimeSpan]::FromSeconds(10)

    $loginPageResponse = $httpClient.GetAsync(
        "http://127.0.0.1:$portA/login").GetAwaiter().GetResult()
    $loginPageContent = $loginPageResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$loginPageResponse.StatusCode -ne 200) {
        throw 'The standard Form Login page was not available.'
    }
    $csrfInput = [regex]::Match($loginPageContent, '<input[^>]+name="_csrf"[^>]*>')
    $csrfValue = if ($csrfInput.Success) {
        [regex]::Match($csrfInput.Value, 'value="(?<value>[^"]+)"').Groups['value'].Value
    } else {
        ''
    }
    if ([string]::IsNullOrWhiteSpace($csrfValue)) {
        throw 'The standard Form Login CSRF token was not found.'
    }

    $loginFields = [System.Collections.Generic.Dictionary[string, string]]::new()
    $loginFields.Add('username', $loginEmail)
    $loginFields.Add('password', $loginPassword)
    $loginFields.Add('_csrf', $csrfValue)
    $loginBody = [System.Net.Http.FormUrlEncodedContent]::new($loginFields)
    $loginResponse = $httpClient.PostAsync(
        "http://127.0.0.1:$portA/login", $loginBody).GetAwaiter().GetResult()
    $loginStatus = [int]$loginResponse.StatusCode
    if ($loginStatus -notin @(302, 303)) {
        throw 'Standard Form Login did not return a success redirect.'
    }
    $sessionCookies = @($cookieContainer.GetCookies(
            [Uri]"http://127.0.0.1:$portA/") | Where-Object Name -eq 'SESSION')
    if ($sessionCookies.Count -ne 1) {
        throw 'Standard Form Login did not establish exactly one Session Cookie.'
    }

    $sessionRow = Invoke-Postgres -Label 'Session row observation' -Sql (
        "SELECT count(*) || '|' || min(principal_name) FROM koiki_session;")
    if ($sessionRow -ne "1|$userId") {
        throw 'The persisted Session row or immutable principal index was unexpected.'
    }

    Invoke-AuthenticatedObservation -Client $httpClient -Port $portA
    Invoke-AuthenticatedObservation -Client $httpClient -Port $portB

    Stop-StartedProcess -Process $processA
    $processA.Refresh()
    if (-not $processA.HasExited) {
        throw 'Process A remained alive after the bounded stop.'
    }
    $processB.Refresh()
    if ($processB.HasExited) {
        throw 'Process B exited when process A was stopped.'
    }
    Invoke-AuthenticatedObservation -Client $httpClient -Port $portB
    $continuedSessionRow = Invoke-Postgres -Label 'Continued Session row observation' -Sql (
        "SELECT count(*) || '|' || min(principal_name) FROM koiki_session;")
    if ($continuedSessionRow -ne "1|$userId") {
        throw 'The persisted Session did not remain after process A stopped.'
    }

    $jarHashAfter = (Get-FileHash -LiteralPath $fixtureJar -Algorithm SHA256).Hash
    if ($jarHashAfter -ne $jarHashBefore) {
        throw 'The executable fixture JAR changed during two-process execution.'
    }

    Stop-StartedProcess -Process $processB
    $verificationSucceeded = $true
} catch {
    throw
} finally {
    try {
        if ($null -ne $httpClient) {
            $httpClient.Dispose()
        }
        if ($null -ne $httpHandler) {
            $httpHandler.Dispose()
        }
        Stop-StartedProcess -Process $processB
        Stop-StartedProcess -Process $processA
        if ($containerStarted) {
            if ($containerName -notlike 'koiki-b34-*') {
                throw 'Refusing to remove an unexpected container.'
            }
            & docker inspect $containerName 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                & docker rm --force $containerName 2>&1 | Out-Null
                if ($LASTEXITCODE -ne 0) {
                    throw 'The B3-4 PostgreSQL container could not be removed.'
                }
            }
            & docker inspect $containerName 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                throw 'The B3-4 PostgreSQL container remained after cleanup.'
            }
        }

        $evidencePaths = @(
            $mavenLog, $processALog, $processAError, $processBLog, $processBError)
        $evidenceFiles = @($evidencePaths |
            Where-Object { Test-Path -LiteralPath $_ } |
            ForEach-Object { Get-Item -LiteralPath $_ })
        if ($null -ne $fixtureJar -and (Test-Path -LiteralPath $fixtureJar)) {
            $evidenceFiles += Get-Item -LiteralPath $fixtureJar
        }
        if ($evidenceFiles.Count -ne 0) {
            Assert-NoSensitiveFiles -Files $evidenceFiles
        }
    } finally {
        if (Test-Path -LiteralPath $verificationRoot) {
            Assert-SafeTemporaryPath -Path $verificationRoot
            Remove-Item -LiteralPath $verificationRoot -Recurse -Force
        }
        if (Test-Path -LiteralPath $verificationRoot) {
            throw 'The B3-4 temporary directory remained after cleanup.'
        }
    }
}

if ($verificationSucceeded) {
    Write-Host (
        'Phase 2 P2-B3 first two-process slice succeeded: ' +
        'A login, B continuity, and B continuity after A stop.')
}

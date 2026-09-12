[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/security-foundation-consumer/postgresql'
$consumerPom = Join-Path $consumerRoot 'pom.xml'
$consumerSource = Join-Path $consumerRoot 'src'
$consumerTarget = Join-Path $consumerRoot 'target'
$consumerJar = Join-Path $consumerTarget 'security-foundation-postgresql-consumer-0.1.0-SNAPSHOT.jar'
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ('koiki-p2-c2-consumer-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$mavenLog = Join-Path $verificationRoot 'maven-output.log'
$runtimeLog = Join-Path $verificationRoot 'runtime-output.log'
$failureLog = Join-Path $verificationRoot 'expected-failure.log'
$dependencyTree = Join-Path $verificationRoot 'consumer-dependency-tree.txt'
$containerName = 'koiki-p2-c2-' + [System.Guid]::NewGuid().ToString('N')
$databasePassword = [System.Guid]::NewGuid().ToString('N') + [System.Guid]::NewGuid().ToString('N')
$fixtureLogin = 'c2-' + [System.Guid]::NewGuid().ToString('N')
$fixtureCredential = [System.Guid]::NewGuid().ToString('N') + [System.Guid]::NewGuid().ToString('N')
$runtimeSensitiveValues = @($databasePassword, $fixtureCredential)
$containerStarted = $false
$consumerProcess = $null
$processLogs = [System.Collections.Generic.List[string]]::new()

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-p2-c2-consumer-*') {
        throw "Unexpected P2-C2 temporary directory name: $resolved"
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
    foreach ($pattern in @(
            '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
            '(?i)[a-z0-9._%+-]+@[a-z0-9.-]+[.][a-z]{2,}',
            '(?i)(?:password|client[_-]?secret|access[_-]?token)[ ]*[:=][ ]*(?!\?)[^\s<]+')) {
        if ($Content -match $pattern) {
            throw "Sensitive pattern detected in $Source."
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
        throw "$Label failed during P2-C2 verification."
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
        '--set', 'ON_ERROR_STOP=1', '--username', 'postgres', '--dbname', 'postgres',
        '--command', $Sql)
}

function Get-JavaRuntime {
    param([Parameter(Mandatory)][ValidateSet(21, 25)][int]$Feature)

    $javaHome = [Environment]::GetEnvironmentVariable("JAVA${Feature}_HOME")
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        throw "JAVA${Feature}_HOME is not set."
    }
    $executable = if ($IsWindows) { 'java.exe' } else { 'java' }
    $javaCommand = Join-Path $javaHome "bin/$executable"
    if (-not (Test-Path -LiteralPath $javaCommand -PathType Leaf)) {
        throw "Java $Feature executable was not found: $javaCommand"
    }
    $versionOutput = @(& $javaCommand '-XshowSettings:properties' '-version' 2>&1)
    if ($LASTEXITCODE -ne 0 -or -not ($versionOutput -match (
                '^\s*java[.]specification[.]version\s*=\s*' + $Feature + '\s*$'))) {
        throw "JAVA${Feature}_HOME does not point to Java $Feature."
    }
    return $javaCommand
}

function Invoke-ConsumerRuntime {
    param(
        [Parameter(Mandatory)][ValidateSet(21, 25)][int]$Feature,
        [Parameter(Mandatory)][string]$DatabasePort,
        [switch]$ExpectedFailure
    )

    $javaCommand = Get-JavaRuntime -Feature $Feature
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$DatabasePort/postgres?loggerLevel=OFF"
    $env:SPRING_DATASOURCE_USERNAME = 'postgres'
    $env:SPRING_DATASOURCE_PASSWORD = $databasePassword
    if ($ExpectedFailure) {
        $env:SPRING_SESSION_JDBC_INITIALIZE_SCHEMA = 'always'
    } else {
        Remove-Item -Path 'Env:SPRING_SESSION_JDBC_INITIALIZE_SCHEMA' -ErrorAction SilentlyContinue
    }

    Write-Host "=== Execute packaged Consumer on Java $Feature ==="
    $output = @(& $javaCommand '-jar' $consumerJar '--debug=false' '--logging.level.root=WARN' 2>&1)
    $exitCode = $LASTEXITCODE
    $text = $output -join [Environment]::NewLine
    Assert-NoSensitiveText -Content $text -Source "Java $Feature runtime output"

    if ($ExpectedFailure) {
        $text | Set-Content -LiteralPath $failureLog
        if ($exitCode -eq 0 -or $text.Contains(
                'P2-C2-C2-3-CONSUMER-SUCCEEDED', [System.StringComparison]::Ordinal)) {
            throw 'The Session initializer negative guard unexpectedly succeeded.'
        }
        return
    }

    $text | Add-Content -LiteralPath $runtimeLog
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0 -or -not $text.Contains(
            'P2-C2-C2-3-CONSUMER-SUCCEEDED', [System.StringComparison]::Ordinal)) {
        throw "Packaged Consumer failed on Java $Feature."
    }
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

function Start-ConsumerWebProcess {
    param(
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$DatabasePort
    )

    $javaCommand = Get-JavaRuntime -Feature 21
    $processLog = Join-Path $verificationRoot 'consumer-web.log'
    $processError = Join-Path $verificationRoot 'consumer-web-error.log'
    [void]$processLogs.Add($processLog)
    [void]$processLogs.Add($processError)
    $startArguments = @{
        FilePath = $javaCommand
        ArgumentList = @(
            '-jar', $consumerJar,
            '--koiki.consumer.c2.web-probe=true',
            '--debug=false',
            '--logging.level.root=WARN')
        PassThru = $true
        RedirectStandardOutput = $processLog
        RedirectStandardError = $processError
        Environment = @{
            'SERVER_PORT' = [string]$Port
            'SPRING_DATASOURCE_URL' =
                "jdbc:postgresql://127.0.0.1:$DatabasePort/postgres?loggerLevel=OFF"
            'SPRING_DATASOURCE_USERNAME' = 'postgres'
            'SPRING_DATASOURCE_PASSWORD' = $databasePassword
            'KOIKI_CONSUMER_FIXTURE_LOGIN' = $fixtureLogin
            'KOIKI_CONSUMER_FIXTURE_CREDENTIAL' = $fixtureCredential
        }
    }
    if ($IsWindows) {
        $startArguments.WindowStyle = 'Hidden'
    }
    return Start-Process @startArguments
}

function Stop-ConsumerWebProcess {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return
    }
    $Process.Refresh()
    if (-not $Process.HasExited) {
        Stop-Process -Id $Process.Id
        if (-not $Process.WaitForExit(10000)) {
            throw 'The Consumer Web process did not stop within 10 seconds.'
        }
    }
}

function Invoke-ConsumerWebProbe {
    param(
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][int]$Port
    )

    $publicUri = "http://127.0.0.1:$Port/consumer/c2/public"
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
    while ($true) {
        $Process.Refresh()
        if ($Process.HasExited) {
            throw 'The Consumer Web process exited before readiness.'
        }
        try {
            $public = Invoke-WebRequest -Uri $publicUri -SkipHttpErrorCheck -TimeoutSec 2
            if ($public.StatusCode -eq 200) {
                break
            }
        } catch {
            if ([DateTimeOffset]::UtcNow -ge $deadline) {
                throw 'The Consumer Web process did not become ready within 60 seconds.'
            }
            Start-Sleep -Milliseconds 250
        }
    }
    if ($public.Content -ne 'c2-consumer-public-ok' -or
        $public.Headers['X-Content-Type-Options'] -ne 'nosniff') {
        throw 'The public Consumer route or Security Header was not composed.'
    }

    $privateUri = "http://127.0.0.1:$Port/consumer/c2/private"
    $unauthenticated = Invoke-WebRequest -Uri $privateUri -SkipHttpErrorCheck
    if ($unauthenticated.StatusCode -ne 401) {
        throw 'The private Consumer route did not reject an unauthenticated request.'
    }
    $basicBytes = [System.Text.Encoding]::UTF8.GetBytes("${fixtureLogin}:${fixtureCredential}")
    $authorization = 'Basic ' + [Convert]::ToBase64String($basicBytes)
    $authenticated = Invoke-WebRequest -Uri $privateUri -SkipHttpErrorCheck `
        -Headers @{ Authorization = $authorization }
    if ($authenticated.StatusCode -ne 200 -or
        $authenticated.Content -ne 'c2-consumer-private-ok') {
        throw 'The private Consumer route did not accept the runtime fixture identity.'
    }
    # This URI is intentionally outside the Consumer chain's /consumer/c2/** matcher.
    $unmatched = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/framework-fallback-probe" `
        -SkipHttpErrorCheck
    if ($unmatched.StatusCode -ne 401) {
        throw 'The Framework fallback did not deny an unmatched Consumer route.'
    }
}

function Assert-Scalar {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string]$Sql,
        [Parameter(Mandatory)][string]$Expected
    )

    $actual = (Invoke-Postgres -Label $Label -Sql $Sql).Trim()
    if ($actual -ne $Expected) {
        throw "$Label expected '$Expected' but found '$actual'."
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    $rootModel = Get-Content -Raw -LiteralPath $rootPom
    if ($rootModel.Contains('security-foundation-consumer', [System.StringComparison]::Ordinal)) {
        throw 'The Consumer must remain outside the Root Reactor.'
    }
    $consumerModel = Get-Content -Raw -LiteralPath $consumerPom
    if ($consumerModel -notmatch '<relativePath\s*/>') {
        throw 'The Consumer parent must be resolved without a repository-relative path.'
    }
    $internalReferences = @(Get-ChildItem -LiteralPath $consumerSource -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal')
    if ($internalReferences.Count -ne 0) {
        throw 'The Consumer references a KOIKI internal package.'
    }

    Invoke-Docker -Label 'Docker Engine check' -Arguments @(
        'version', '--format', '{{.Server.Version}}') | Out-Null
    Invoke-KoikiMaven -Label 'Stage formal Framework release unit' -Arguments @(
        '-f', $rootPom, '-pl', '!koiki-reference-app', 'clean', 'install', '-DskipTests')
    Invoke-KoikiMaven -Label 'Build, test and package Root-external Consumer' -Arguments @(
        '-f', $consumerPom, 'clean', 'package')
    Invoke-KoikiMaven -Label 'Record Consumer runtime dependency tree' -Arguments @(
        '-f', $consumerPom, 'dependency:tree', '-Dscope=runtime',
        "-DoutputFile=$dependencyTree")

    if (-not (Test-Path -LiteralPath $consumerJar -PathType Leaf)) {
        throw 'The executable Consumer JAR was not created.'
    }
    $dependencies = Get-Content -Raw -LiteralPath $dependencyTree
    foreach ($required in @(
            'org.koikifw:koiki-starter-session-jdbc:jar:',
            'org.koikifw:koiki-starter-identity:jar:',
            'org.koikifw:koiki-starter-audit:jar:',
            'org.koikifw:koiki-starter-data:jar:',
            'org.postgresql:postgresql:jar:')) {
        if ($dependencies -notmatch [regex]::Escape($required)) {
            throw "Required Consumer dependency is missing: $required"
        }
    }
    if ($dependencies -match 'koiki-reference-app|koiki-testing') {
        throw 'Reference or koiki-testing leaked into the Consumer runtime closure.'
    }

    $scanFiles = @(
        Get-Item -LiteralPath $consumerPom
        Get-Item -LiteralPath (Join-Path $consumerRoot 'README.md')
        Get-ChildItem -LiteralPath $consumerSource -Recurse -File
        Get-Item -LiteralPath $consumerJar
        Get-ChildItem -LiteralPath (Join-Path $consumerTarget 'surefire-reports') -File
    )
    foreach ($file in $scanFiles) {
        $content = [System.Text.Encoding]::Latin1.GetString(
            [System.IO.File]::ReadAllBytes($file.FullName))
        Assert-NoSensitiveText -Content $content -Source $file.FullName
    }

    Invoke-Docker -Label 'PostgreSQL container start' -Arguments @(
        'run', '--detach', '--rm', '--name', $containerName,
        '--publish', '127.0.0.1::5432',
        '--env', "POSTGRES_PASSWORD=$databasePassword",
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
    if ($portText -notmatch '127[.]0[.]0[.]1:(?<port>[0-9]+)$') {
        throw 'The PostgreSQL loopback port could not be determined.'
    }
    $databasePort = $Matches.port

    $jarHash = (Get-FileHash -LiteralPath $consumerJar -Algorithm SHA256).Hash
    Invoke-ConsumerRuntime -Feature 21 -DatabasePort $databasePort
    Assert-Scalar -Label 'Framework history after Java 21' `
        -Sql 'SELECT count(*) FROM koiki_flyway_history WHERE success;' -Expected '3'
    Assert-Scalar -Label 'Customer history after Java 21' `
        -Sql 'SELECT count(*) FROM flyway_schema_history WHERE success;' -Expected '2'
    Assert-Scalar -Label 'Customer baseline history after Java 21' `
        -Sql "SELECT count(*) FROM flyway_schema_history WHERE type = 'BASELINE' AND version = '0';" `
        -Expected '1'
    Assert-Scalar -Label 'Customer migration history after Java 21' `
        -Sql "SELECT count(*) FROM flyway_schema_history WHERE type = 'SQL' AND version = '1';" `
        -Expected '1'
    $historyBefore = Invoke-Postgres -Label 'Capture migration history' -Sql (
        "SELECT installed_rank || ':' || version || ':' || checksum FROM koiki_flyway_history " +
        "UNION ALL SELECT 1000 + installed_rank || ':' || version || ':' || checksum " +
        'FROM flyway_schema_history ORDER BY 1;')

    Invoke-ConsumerRuntime -Feature 25 -DatabasePort $databasePort
    if ((Get-FileHash -LiteralPath $consumerJar -Algorithm SHA256).Hash -ne $jarHash) {
        throw 'The packaged Consumer JAR changed between Java 21 and Java 25 execution.'
    }
    $historyAfter = Invoke-Postgres -Label 'Recapture migration history' -Sql (
        "SELECT installed_rank || ':' || version || ':' || checksum FROM koiki_flyway_history " +
        "UNION ALL SELECT 1000 + installed_rank || ':' || version || ':' || checksum " +
        'FROM flyway_schema_history ORDER BY 1;')
    if ($historyAfter -ne $historyBefore) {
        throw 'Migration history changed during the Java 25 no-op restart.'
    }
    Assert-Scalar -Label 'Framework application table inventory' -Sql (
        "SELECT count(*) FROM pg_tables WHERE schemaname = 'public' " +
        "AND tablename LIKE 'koiki_%' AND tablename <> 'koiki_flyway_history';") -Expected '11'
    Assert-Scalar -Label 'Customer marker inventory' -Sql (
        "SELECT count(*) FROM pg_tables WHERE schemaname = 'public' " +
        "AND tablename = 'c2_consumer_marker';") -Expected '1'
    Assert-Scalar -Label 'Synthetic identity count' -Sql 'SELECT count(*) FROM koiki_user;' -Expected '2'
    Assert-Scalar -Label 'Business audit count' -Sql 'SELECT count(*) FROM koiki_audit_event;' -Expected '4'

    $consumerPort = New-LoopbackPort
    $consumerProcess = Start-ConsumerWebProcess -Port $consumerPort -DatabasePort $databasePort
    Invoke-ConsumerWebProbe -Process $consumerProcess -Port $consumerPort
    Stop-ConsumerWebProcess -Process $consumerProcess
    $consumerProcess = $null
    foreach ($logPath in $processLogs) {
        if (Test-Path -LiteralPath $logPath) {
            Assert-NoSensitiveText -Content (Get-Content -Raw -LiteralPath $logPath) `
                -Source $logPath
        }
    }
    if ((Get-FileHash -LiteralPath $consumerJar -Algorithm SHA256).Hash -ne $jarHash) {
        throw 'The packaged Consumer JAR changed during the Web security probe.'
    }

    Invoke-ConsumerRuntime -Feature 21 -DatabasePort $databasePort -ExpectedFailure
    Assert-Scalar -Label 'Spring Session initializer leakage' -Sql (
        "SELECT count(*) FROM pg_tables WHERE schemaname = 'public' " +
        "AND upper(tablename) LIKE 'SPRING_SESSION%';") -Expected '0'

    Write-Host (
        'Phase 2 P2-C2 C2-3 Consumer verification succeeded ' +
        '(isolated package / Java 21 and 25 / PostgreSQL / migration no-op and failure guard).')
} finally {
    Stop-ConsumerWebProcess -Process $consumerProcess
    foreach ($name in @(
            'SPRING_DATASOURCE_URL',
            'SPRING_DATASOURCE_USERNAME',
            'SPRING_DATASOURCE_PASSWORD',
            'SPRING_SESSION_JDBC_INITIALIZE_SCHEMA')) {
        Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
    }
    if ($containerStarted) {
        & docker rm --force $containerName 2>&1 | Out-Null
    }
    if (Test-Path -LiteralPath $consumerTarget) {
        Remove-Item -LiteralPath $consumerTarget -Recurse -Force
    }
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

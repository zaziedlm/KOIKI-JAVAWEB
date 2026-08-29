[CmdletBinding()]
param(
    [switch]$Smoke,
    [switch]$SkipRegression,
    [string]$ResultsDirectory
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$harnessRoot = $PSScriptRoot
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $harnessRoot '../..'))
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$harnessPom = Join-Path $harnessRoot 'pom.xml'
$cp8Verification = Join-Path $repositoryRoot 'build-support/runtime-foundation-verification/verify-cp8-single-execution.ps1'
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-phase1b-cp9-' + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$fragments = Join-Path $verificationRoot 'fragments'
$containerName = 'koiki-cp9-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$runId = [guid]::NewGuid().ToString('N')
$startedAt = [DateTimeOffset]::Now.ToString('o')
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
$containerStarted = $false

$forks = if ($Smoke) { 1 } else { 3 }
$startupForks = $forks
$warmup = if ($Smoke) { 3 } else { 200 }
$measurement = if ($Smoke) { 10 } else { 1000 }
$timeoutMillis = 5000
$gitCommit = (& git -C $repositoryRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the measurement commit' }
$gitDirty = -not [string]::IsNullOrWhiteSpace((
    & git -C $repositoryRoot status --porcelain=v1 | Out-String).Trim())
if (-not $Smoke -and $gitDirty) {
    throw 'Official baseline requires a clean commit; use -Smoke for a dirty worktree'
}

function Invoke-Checked {
    param([Parameter(Mandatory)][string]$Label, [Parameter(Mandatory)][scriptblock]$Command)
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

function Get-FreeTcpPort {
    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try {
        return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

function Invoke-PostgreSql {
    param(
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$Sql
    )
    $output = & docker exec $containerName psql `
        --username postgres --dbname $Database --tuples-only --no-align --command $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL command failed for database $Database"
    }
    return ($output | Out-String).Trim()
}

function Wait-PostgreSql {
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        # The official image briefly exposes its initialization server before
        # restarting PostgreSQL. Wait for both ready events, then prove SQL works.
        $readyEvents = @(& docker logs $containerName 2>&1 |
                Select-String 'database system is ready to accept connections').Count
        if ($readyEvents -ge 2) {
            & docker exec $containerName psql --username postgres --dbname postgres `
                --tuples-only --no-align --command 'select 1' *> $null
            if ($LASTEXITCODE -eq 0) { return }
        }
        Start-Sleep -Milliseconds 250
    }
    throw 'PostgreSQL did not become ready'
}

function Start-Application {
    param(
        [Parameter(Mandatory)][string]$Jar,
        [Parameter(Mandatory)][string]$Variant,
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$LogPath
    )
    $arguments = @(
        '-Xms256m', '-Xmx256m', '-jar', $Jar,
        '--debug=false',
        "--server.port=$Port",
        "--spring.datasource.url=jdbc:postgresql://127.0.0.1:$script:databasePort/$Database",
        '--spring.datasource.username=postgres',
        '--spring.datasource.password=postgres'
    )
    $process = Start-Process -FilePath 'java' -ArgumentList $arguments `
        -RedirectStandardOutput $LogPath -RedirectStandardError ($LogPath + '.err') `
        -PassThru -NoNewWindow
    $null = $processes.Add($process)
    return $process
}

function Wait-Ready {
    param(
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][int]$Port
    )
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        if ($Process.HasExited) {
            throw "Application exited before readiness with $($Process.ExitCode)"
        }
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/performance/1/ready" `
                -TimeoutSec 2 -SkipHttpErrorCheck
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Milliseconds 250
    }
    throw 'Application readiness timed out'
}

function Stop-Application {
    param([Parameter(Mandatory)][System.Diagnostics.Process]$Process)
    if (-not $Process.HasExited) {
        $Process.Kill($true)
        $Process.WaitForExit()
    }
}

function Invoke-Runner {
    param([Parameter(Mandatory)][string[]]$Arguments)
    & java -jar $script:runnerJar @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Performance runner failed with exit code $LASTEXITCODE"
    }
}

function Measure-Variant {
    param(
        [Parameter(Mandatory)][string]$Variant,
        [Parameter(Mandatory)][int]$Fork
    )
    $jar = if ($Variant -eq 'bare') { $script:bareJar } else { $script:koikiJar }
    $database = if ($Variant -eq 'bare') { 'baredb' } else { 'koikidb' }
    $port = Get-FreeTcpPort
    $logPath = Join-Path $verificationRoot "$Variant-$Fork.log"
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $process = Start-Application -Jar $jar -Variant $Variant -Database $database `
        -Port $port -LogPath $logPath
    try {
        Wait-Ready -Process $process -Port $port
        $stopwatch.Stop()
        Invoke-Runner -Arguments @(
            'startup', "--run-id=$runId", "--variant=$Variant", "--fork=$Fork",
            "--duration-nanos=$([long]($stopwatch.Elapsed.TotalMilliseconds * 1000000))", '--success=true',
            "--output=$(Join-Path $fragments "$Variant-$Fork-startup.fragment.json")")
        Invoke-Runner -Arguments @(
            'measure', "--run-id=$runId", "--variant=$Variant", "--fork=$Fork",
            "--warmup=$warmup", "--measurement=$measurement",
            "--timeout-ms=$timeoutMillis", "--base-url=http://127.0.0.1:$port",
            "--output=$(Join-Path $fragments "$Variant-$Fork-request.fragment.json")")

        $expectedRows = $warmup + $measurement
        $actualRows = [int](Invoke-PostgreSql -Database $database `
            -Sql 'select count(*) from perf_item')
        if ($actualRows -ne $expectedRows) {
            throw "$Variant fork $Fork recorded $actualRows DB rows, expected $expectedRows"
        }
    } finally {
        Stop-Application -Process $process
    }

    $expectedLogs = $warmup + $measurement
    $actualLogs = @(Select-String -LiteralPath $logPath `
        -SimpleMatch 'performance fixture request').Count
    if ($actualLogs -ne $expectedLogs) {
        throw "$Variant fork $Fork emitted $actualLogs workload logs, expected $expectedLogs"
    }
}

function Assert-Schema {
    param(
        [Parameter(Mandatory)][string]$JsonPath,
        [Parameter(Mandatory)][string]$SchemaPath
    )
    $json = Get-Content -LiteralPath $JsonPath -Raw
    if (-not ($json | Test-Json -SchemaFile $SchemaPath)) {
        throw "Schema validation failed: $JsonPath"
    }
}

function Assert-RawAcceptance {
    param([Parameter(Mandatory)][string]$JsonPath)
    $raw = Get-Content -LiteralPath $JsonPath -Raw | ConvertFrom-Json
    $failures = @($raw.samples | Where-Object { -not $_.success })
    if ($failures.Count -ne 0) {
        $summary = ($failures | ForEach-Object {
                "$($_.variant)/$($_.workload)/fork=$($_.fork)/sequence=$($_.sequence):$($_.errorCode)"
            }) -join ', '
        throw "Raw results contain $($failures.Count) failed samples: $summary"
    }
    foreach ($variant in @('bare', 'koiki')) {
        foreach ($fork in 1..$forks) {
            foreach ($workload in @('http-success', 'validation-rejection', 'db-write')) {
                $actual = @($raw.samples | Where-Object {
                        $_.sampleType -eq 'request' -and
                        $_.variant -eq $variant -and
                        $_.workload -eq $workload -and
                        $_.fork -eq $fork
                    }).Count
                if ($actual -ne $measurement) {
                    throw "$variant/$workload fork $fork has $actual samples, expected $measurement"
                }
            }
            $startup = @($raw.samples | Where-Object {
                    $_.sampleType -eq 'startup' -and
                    $_.variant -eq $variant -and
                    $_.workload -eq 'startup' -and
                    $_.fork -eq $fork
                }).Count
            if ($startup -ne 1) {
                throw "$variant/startup fork $fork has $startup samples, expected 1"
            }
        }
    }
    $expectedTotal = 2 * (($forks * 3 * $measurement) + $startupForks)
    if ($raw.samples.Count -ne $expectedTotal) {
        throw "Raw results contain $($raw.samples.Count) samples, expected $expectedTotal"
    }
}

function Get-DependencyVersion {
    param(
        [Parameter(Mandatory)][string]$Tree,
        [Parameter(Mandatory)][string]$Coordinate
    )
    $match = [regex]::Match(
        $Tree,
        "$([regex]::Escape($Coordinate)):[^:\r\n]+:([^:\s]+)")
    if (-not $match.Success) { throw "Missing dependency in tree: $Coordinate" }
    return $match.Groups[1].Value
}

try {
    New-Item -ItemType Directory -Path $isolatedRepository, $fragments -Force | Out-Null

    if (-not $SkipRegression) {
        Write-Host '=== Verify CP8 regression before CP9 ==='
        Invoke-Checked -Label 'CP8 regression' -Command { & pwsh -NoProfile -File $cp8Verification }
    }

    Write-Host '=== Stage KOIKI release unit into an isolated repository ==='
    Invoke-Checked -Label 'KOIKI release unit stage' -Command {
        & $wrapper -f $rootPom "-Dmaven.repo.local=$isolatedRepository" -DskipTests install
    }

    Write-Host '=== Build the paired performance fixtures ==='
    Invoke-Checked -Label 'Performance harness build' -Command {
        & $wrapper -f $harnessPom "-Dmaven.repo.local=$isolatedRepository" clean install
    }

    $script:bareJar = Join-Path $harnessRoot `
        'bare-application/target/performance-bare-application-0.1.0-SNAPSHOT.jar'
    $script:koikiJar = Join-Path $harnessRoot `
        'koiki-application/target/performance-koiki-application-0.1.0-SNAPSHOT.jar'
    $script:runnerJar = Join-Path $harnessRoot `
        'runner/target/performance-runner-0.1.0-SNAPSHOT.jar'
    foreach ($jar in @($bareJar, $koikiJar, $runnerJar)) {
        if (-not (Test-Path -LiteralPath $jar)) { throw "Missing executable JAR: $jar" }
    }

    $bareTree = Join-Path $verificationRoot 'bare-dependencies.txt'
    $koikiTree = Join-Path $verificationRoot 'koiki-dependencies.txt'
    Invoke-Checked -Label 'Bare dependency tree' -Command {
        & $wrapper -f (Join-Path $harnessRoot 'bare-application/pom.xml') `
            "-Dmaven.repo.local=$isolatedRepository" dependency:tree `
            "-DoutputFile=$bareTree"
    }
    Invoke-Checked -Label 'KOIKI dependency tree' -Command {
        & $wrapper -f (Join-Path $harnessRoot 'koiki-application/pom.xml') `
            "-Dmaven.repo.local=$isolatedRepository" dependency:tree `
            "-DoutputFile=$koikiTree"
    }
    $bareTreeText = Get-Content -LiteralPath $bareTree -Raw
    $koikiTreeText = Get-Content -LiteralPath $koikiTree -Raw
    if ($bareTreeText -match 'org\.koikifw:koiki-') {
        throw 'Bare runtime dependency tree contains a KOIKI runtime artifact'
    }
    if ($koikiTreeText -notmatch 'org\.koikifw:koiki-starter-api') {
        throw 'KOIKI runtime dependency tree does not contain the API Starter'
    }
    foreach ($coordinate in @('org.springframework.boot:spring-boot', 'org.postgresql:postgresql')) {
        $bareVersion = Get-DependencyVersion -Tree $bareTreeText -Coordinate $coordinate
        $koikiVersion = Get-DependencyVersion -Tree $koikiTreeText -Coordinate $coordinate
        if ($bareVersion -ne $koikiVersion) {
            throw "$coordinate differs: bare=$bareVersion koiki=$koikiVersion"
        }
    }

    Write-Host '=== Start the dedicated PostgreSQL 17 container ==='
    Invoke-Checked -Label 'PostgreSQL start' -Command {
        & docker run --detach --name $containerName `
            --env POSTGRES_PASSWORD=postgres --publish '127.0.0.1::5432' `
            postgres:17-alpine
    }
    $containerStarted = $true
    Wait-PostgreSql
    $portText = & docker port $containerName '5432/tcp'
    if ($portText -notmatch ':(\d+)$') { throw 'Unable to resolve PostgreSQL port' }
    $script:databasePort = [int]$Matches[1]
    Invoke-PostgreSql -Database postgres -Sql 'create database baredb' | Out-Null
    Invoke-PostgreSql -Database postgres -Sql 'create database koikidb' | Out-Null
    $postgresVersion = Invoke-PostgreSql -Database postgres -Sql 'show server_version'

    Write-Host '=== Run paired bare and KOIKI measurements ==='
    for ($fork = 1; $fork -le $forks; $fork++) {
        $order = if ($fork % 2 -eq 1) { @('bare', 'koiki') } else { @('koiki', 'bare') }
        foreach ($variant in $order) {
            $database = if ($variant -eq 'bare') { 'baredb' } else { 'koikidb' }
            $tableExists = Invoke-PostgreSql -Database $database `
                -Sql "select to_regclass('public.perf_item') is not null"
            if ($tableExists -eq 't') {
                Invoke-PostgreSql -Database $database -Sql 'truncate table perf_item' | Out-Null
            }
            Measure-Variant -Variant $variant -Fork $fork
        }
    }

    if ([string]::IsNullOrWhiteSpace($ResultsDirectory)) {
        $ResultsDirectory = if ($Smoke) {
            Join-Path $harnessRoot 'target/smoke-results'
        } else {
            Join-Path $harnessRoot ('results/' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss'))
        }
    }
    $resultsPath = [System.IO.Path]::GetFullPath($ResultsDirectory)
    New-Item -ItemType Directory -Path $resultsPath -Force | Out-Null
    $rawPath = Join-Path $resultsPath 'raw-results.json'
    $aggregatePath = Join-Path $resultsPath 'aggregate.json'
    Invoke-Runner -Arguments @(
        'aggregate', "--run-id=$runId", "--input-dir=$fragments",
        "--raw-output=$rawPath", "--aggregate-output=$aggregatePath")
    $secondRawPath = Join-Path $verificationRoot 'raw-results-second.json'
    $secondAggregatePath = Join-Path $verificationRoot 'aggregate-second.json'
    Invoke-Runner -Arguments @(
        'aggregate', "--run-id=$runId", "--input-dir=$fragments",
        "--raw-output=$secondRawPath", "--aggregate-output=$secondAggregatePath")
    if ((Get-FileHash $rawPath -Algorithm SHA256).Hash -ne
            (Get-FileHash $secondRawPath -Algorithm SHA256).Hash -or
        (Get-FileHash $aggregatePath -Algorithm SHA256).Hash -ne
            (Get-FileHash $secondAggregatePath -Algorithm SHA256).Hash) {
        throw 'Repeated raw aggregation was not byte-for-byte deterministic'
    }

    if ($IsWindows) {
        $os = Get-CimInstance Win32_OperatingSystem
        $cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
        $system = Get-CimInstance Win32_ComputerSystem
        $hostInfo = [ordered]@{
            os = $os.Caption
            osVersion = $os.Version
            architecture = $os.OSArchitecture
            cpu = $cpu.Name.Trim()
            logicalProcessors = [int]$system.NumberOfLogicalProcessors
            memoryBytes = [int64]$system.TotalPhysicalMemory
        }
    } else {
        $hostInfo = [ordered]@{
            os = (uname -s)
            osVersion = (uname -r)
            architecture = (uname -m)
            cpu = ((Get-Content /proc/cpuinfo | Select-String 'model name' | Select-Object -First 1) -split ':', 2)[1].Trim()
            logicalProcessors = [Environment]::ProcessorCount
            memoryBytes = [int64](([double]((Get-Content /proc/meminfo | Select-String 'MemTotal') -replace '\D', '')) * 1024)
        }
    }
    $dockerVersion = (& docker version --format 'client={{.Client.Version}} server={{.Server.Version}}').Trim()
    $postgresDigest = (& docker image inspect postgres:17-alpine --format '{{.Id}}').Trim()
    $fingerprint = [ordered]@{
        schemaVersion = 1
        runId = $runId
        startedAt = $startedAt
        gitCommit = $gitCommit
        gitDirty = $gitDirty
        host = $hostInfo
        java = ((& java -version 2>&1 | Select-Object -First 1) -replace '"', '')
        jvmArguments = @('-Xms256m', '-Xmx256m')
        maven = '3.9.16'
        docker = $dockerVersion
        postgres = [ordered]@{
            image = 'postgres:17-alpine'
            digest = $postgresDigest
            serverVersion = $postgresVersion
        }
        timezone = (Get-TimeZone).Id
        harnessVersion = 1
        artifacts = [ordered]@{
            bareSha256 = (Get-FileHash -LiteralPath $bareJar -Algorithm SHA256).Hash.ToLowerInvariant()
            koikiSha256 = (Get-FileHash -LiteralPath $koikiJar -Algorithm SHA256).Hash.ToLowerInvariant()
        }
        protocol = [ordered]@{
            forks = $forks
            startupForks = $startupForks
            warmup = $warmup
            measurement = $measurement
            concurrency = 1
            timeoutMillis = $timeoutMillis
        }
    }
    $fingerprintPath = Join-Path $resultsPath 'fingerprint.json'
    $fingerprint | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $fingerprintPath -Encoding utf8

    Write-Host '=== Validate the three result schemas and minimal negatives ==='
    Assert-Schema -JsonPath $fingerprintPath `
        -SchemaPath (Join-Path $harnessRoot 'schema/fingerprint.schema.json')
    Assert-Schema -JsonPath $rawPath `
        -SchemaPath (Join-Path $harnessRoot 'schema/raw-results.schema.json')
    Assert-Schema -JsonPath $aggregatePath `
        -SchemaPath (Join-Path $harnessRoot 'schema/aggregate.schema.json')
    Assert-RawAcceptance -JsonPath $rawPath

    $invalidFingerprint = Get-Content -LiteralPath $fingerprintPath -Raw | ConvertFrom-Json
    $invalidFingerprint.PSObject.Properties.Remove('host')
    $invalidFingerprintJson = $invalidFingerprint | ConvertTo-Json -Depth 8
    if ($invalidFingerprintJson | Test-Json `
            -SchemaFile (Join-Path $harnessRoot 'schema/fingerprint.schema.json') `
            -ErrorAction SilentlyContinue) {
        throw 'Fingerprint schema accepted a missing host identity'
    }
    $invalidRaw = Get-Content -LiteralPath $rawPath -Raw | ConvertFrom-Json
    $invalidRaw.samples[0].durationNanos = 'invalid'
    $invalidRawJson = $invalidRaw | ConvertTo-Json -Depth 8
    if ($invalidRawJson | Test-Json `
            -SchemaFile (Join-Path $harnessRoot 'schema/raw-results.schema.json') `
            -ErrorAction SilentlyContinue) {
        throw 'Raw schema accepted a non-numeric duration'
    }

    Write-Host "CP9 performance harness succeeded: runId=$runId results=$resultsPath"
} catch {
    $failurePath = Join-Path $harnessRoot 'target/last-failure'
    New-Item -ItemType Directory -Path $failurePath -Force | Out-Null
    Get-ChildItem -LiteralPath $verificationRoot -Filter '*.log*' -ErrorAction SilentlyContinue |
        Copy-Item -Destination $failurePath -Force
    throw
} finally {
    foreach ($process in $processes) {
        if (-not $process.HasExited) {
            try { $process.Kill($true) } catch { }
        }
        $process.Dispose()
    }
    if ($containerStarted) {
        & docker rm --force $containerName | Out-Null
    }
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

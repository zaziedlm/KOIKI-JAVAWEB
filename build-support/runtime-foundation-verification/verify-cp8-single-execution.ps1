[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$verificationStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$javaToolName = if ($IsWindows) { 'java.exe' } else { 'java' }
$javaTool = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME "bin/$javaToolName"
} else {
    (Get-Command $javaToolName -ErrorAction Stop).Source
}
if (-not (Test-Path -LiteralPath $javaTool -PathType Leaf)) {
    throw "JDK java tool was not found under JAVA_HOME: $javaTool"
}
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/runtime-foundation-consumer'
$consumerPom = Join-Path $consumerRoot 'pom.xml'
$cp7Verification = Join-Path $PSScriptRoot 'verify-cp7-domain-event-mybatis.ps1'
$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp8-" + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$dependencyTree = Join-Path $verificationRoot 'consumer-runtime-dependencies.txt'
$containerName = "koiki-cp8-" + [guid]::NewGuid().ToString('N')
$databaseName = 'koiki_cp8'
$databaseUser = 'postgres'
$databasePassword = [guid]::NewGuid().ToString('N')
$primaryTask = 'workitem-maintenance-primary'
$secondaryTask = 'workitem-maintenance-secondary'
$lockNamespace = 1263487307
$processes = [System.Collections.Generic.List[object]]::new()
$containerStarted = $false

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase) -or
        [System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp8-*') {
        throw "Refusing to operate outside the CP8 OS temporary directory: $resolved"
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

function Invoke-PostgreSql {
    param([Parameter(Mandatory)][string]$Sql)

    $output = & docker exec $containerName psql `
        --username $databaseUser --dbname $databaseName `
        --no-psqlrc --tuples-only --no-align --set ON_ERROR_STOP=1 `
        --command $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL command failed with exit code $LASTEXITCODE"
    }
    return ($output | Out-String).Trim()
}

function Start-CapturedProcess {
    param(
        [Parameter(Mandatory)][string]$FileName,
        [Parameter(Mandatory)][string[]]$Arguments,
        [hashtable]$Environment = @{}
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FileName
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($argument in $Arguments) {
        $startInfo.ArgumentList.Add($argument)
    }
    foreach ($entry in $Environment.GetEnumerator()) {
        $startInfo.Environment[$entry.Key] = [string]$entry.Value
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw "Failed to start $FileName"
    }
    $captured = [pscustomobject]@{
        Process = $process
        StandardOutput = $process.StandardOutput.ReadToEndAsync()
        StandardError = $process.StandardError.ReadToEndAsync()
    }
    $null = $processes.Add($captured)
    return $captured
}

function Wait-CapturedProcess {
    param(
        [Parameter(Mandatory)]$Captured,
        [int]$TimeoutSeconds = 90
    )

    if (-not $Captured.Process.WaitForExit($TimeoutSeconds * 1000)) {
        $Captured.Process.Kill($true)
        throw "Process $($Captured.Process.Id) did not exit within $TimeoutSeconds seconds"
    }
    $Captured.Process.WaitForExit()
    return [pscustomobject]@{
        ProcessId = $Captured.Process.Id
        ExitCode = $Captured.Process.ExitCode
        StandardOutput = $Captured.StandardOutput.GetAwaiter().GetResult()
        StandardError = $Captured.StandardError.GetAwaiter().GetResult()
    }
}

function Start-MaintenanceProcess {
    param(
        [Parameter(Mandatory)][string]$TaskKey,
        [Parameter(Mandatory)][int]$ProbePort
    )

    $environment = @{
        SPRING_DATASOURCE_URL = $script:jdbcUrl
        SPRING_DATASOURCE_USERNAME = $databaseUser
        SPRING_DATASOURCE_PASSWORD = $databasePassword
    }
    return Start-CapturedProcess -FileName $javaTool -Environment $environment -Arguments @(
        '-jar', $script:consumerJar,
        '--koiki.consumer.mode=maintenance',
        "--koiki.consumer.task-key=$TaskKey",
        "--server.port=$ProbePort",
        '--spring.main.banner-mode=off',
        '--koiki.environment=cp8-acceptance'
    )
}

function Start-RowLocker {
    param(
        [Parameter(Mandatory)][string]$TaskKey,
        [Parameter(Mandatory)][string]$ApplicationName
    )

    $sql = "begin; select task_key from kkbiz_work_item_maintenance " +
        "where task_key = '$TaskKey' for update; select pg_sleep(120); rollback;"
    return Start-CapturedProcess -FileName 'docker' -Arguments @(
        'exec', '--env', "PGAPPNAME=$ApplicationName", $containerName,
        'psql', '--username', $databaseUser, '--dbname', $databaseName,
        '--no-psqlrc', '--set', 'ON_ERROR_STOP=1', '--command', $sql
    )
}

function Stop-RowLocker {
    param([Parameter(Mandatory)][string]$ApplicationName)

    $terminated = Invoke-PostgreSql -Sql (
        "select count(*) from (select pg_terminate_backend(pid) " +
        "from pg_stat_activity where application_name = '$ApplicationName') terminated")
    if ([int]$terminated -lt 1) {
        throw "Row locker $ApplicationName was not found"
    }
}

function Wait-ForValue {
    param(
        [Parameter(Mandatory)][scriptblock]$Query,
        [Parameter(Mandatory)][string]$Expected,
        [Parameter(Mandatory)][string]$Label,
        [int]$TimeoutSeconds = 60
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $actual = & $Query
        if ([string]$actual -eq $Expected) {
            return
        }
        Start-Sleep -Milliseconds 250
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw "$Label did not reach '$Expected'; last value was '$actual'"
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

function Assert-PortNotListening {
    param([Parameter(Mandatory)][int]$Port)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connected = $false
        try {
            $connected = $client.ConnectAsync('127.0.0.1', $Port).Wait(750)
        } catch {
            $connected = $false
        }
        if ($connected -and $client.Connected) {
            throw "Maintenance process unexpectedly listened on TCP port $Port"
        }
    } finally {
        $client.Dispose()
    }
}

function Assert-ExitCode {
    param(
        [Parameter(Mandatory)]$Result,
        [Parameter(Mandatory)][int]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Result.ExitCode -ne $Expected) {
        throw "$Label exited with $($Result.ExitCode), expected $Expected.`n" +
            "stdout:`n$($Result.StandardOutput)`nstderr:`n$($Result.StandardError)"
    }
}

function Assert-LifecycleLog {
    param(
        [Parameter(Mandatory)][string]$Output,
        [Parameter(Mandatory)][string]$ExpectedResult
    )

    $events = @($Output -split "`r?`n" | ForEach-Object {
        if ($_.StartsWith('{')) {
            try { $_ | ConvertFrom-Json } catch { $null }
        }
    } | Where-Object {
        $null -ne $_ -and $_.message -eq 'work item maintenance lifecycle'
    })
    $matched = @($events | Where-Object { $_.result -eq $ExpectedResult })
    if ($matched.Count -lt 1) {
        throw "Structured lifecycle log '$ExpectedResult' was not found"
    }
    foreach ($event in $matched) {
        if ([string]::IsNullOrWhiteSpace([string]$event.executionId) -or
            [string]::IsNullOrWhiteSpace([string]$event.lockOwner) -or
            $event.executionId -ne $event.lockOwner) {
            throw "Lifecycle log did not contain a consistent execution identity"
        }
        if ($event.PSObject.Properties.Name -contains 'requestId') {
            throw "Maintenance lifecycle log reused the HTTP requestId field"
        }
    }
}

function Reset-MaintenanceRows {
    Invoke-PostgreSql -Sql (
        "update kkbiz_work_item_maintenance set execution_count = 0, " +
        "last_execution_id = null, last_executed_at = null") | Out-Null
}

function Get-ExecutionCount {
    param([Parameter(Mandatory)][string]$TaskKey)

    return [int](Invoke-PostgreSql -Sql (
        "select execution_count from kkbiz_work_item_maintenance " +
        "where task_key = '$TaskKey'"))
}

function Wait-AdvisoryLock {
    param(
        [Parameter(Mandatory)][int]$TaskId,
        [Parameter(Mandatory)][string]$Expected
    )

    Wait-ForValue -Expected $Expected -Label "advisory lock $TaskId" -Query {
        Invoke-PostgreSql -Sql (
            "select count(*) from pg_locks where locktype = 'advisory' " +
            "and classid = $lockNamespace and objid = $TaskId and granted")
    }
}

Write-Host '=== Verify CP7 regression before CP8 ==='
& pwsh -NoProfile -File $cp7Verification
if ($LASTEXITCODE -ne 0) {
    throw "CP7 regression verification failed with exit code $LASTEXITCODE"
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage KOIKI release unit into an isolated repository' -Arguments @(
        '-f', $rootPom, 'install', '-DskipTests'
    )
    Invoke-KoikiMaven -Label 'Build and test the independent CP8 Consumer' -Arguments @(
        '-f', $consumerPom, 'clean', 'package'
    )
    Invoke-KoikiMaven -Label 'Record the CP8 Consumer runtime dependency tree' -Arguments @(
        '-f', $consumerPom, '-pl', 'application',
        'dependency:tree', '-Dscope=runtime', "-DoutputFile=$dependencyTree"
    )

    $script:consumerJar = Join-Path $consumerRoot `
        'application/target/runtime-foundation-consumer-application-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $script:consumerJar -PathType Leaf)) {
        throw "CP8 Consumer executable JAR was not found: $script:consumerJar"
    }

    $runtimeDependencies = Get-Content -Raw -LiteralPath $dependencyTree
    if ($runtimeDependencies -match 'spring-batch|spring-cloud|kubernetes|mybatis|spring-modulith') {
        throw 'A deferred runtime dependency leaked into the CP8 Consumer.'
    }

    $frameworkMigrations = Get-ChildItem -LiteralPath $repositoryRoot -Recurse -Filter '*.sql' |
        Where-Object {
            $_.FullName -notlike '*build-support*' -and
            $_.FullName -notlike '*target*' -and
            (Get-Content -Raw -LiteralPath $_.FullName) -match 'kkbiz_work_item_maintenance'
        }
    if (@($frameworkMigrations).Count -ne 0) {
        throw 'The Customer maintenance table leaked into a Framework migration.'
    }

    Write-Host '=== Start dedicated PostgreSQL 17 for real process acceptance ==='
    $containerId = & docker run --detach --name $containerName `
        --publish '127.0.0.1::5432' `
        --env "POSTGRES_PASSWORD=$databasePassword" `
        --env "POSTGRES_DB=$databaseName" `
        'postgres:17-alpine'
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL container start failed with exit code $LASTEXITCODE"
    }
    $containerStarted = $true
    if ([string]::IsNullOrWhiteSpace(($containerId | Out-String))) {
        throw 'PostgreSQL container did not return an identifier.'
    }

    Wait-ForValue -Expected 'ready' -Label 'PostgreSQL readiness' -TimeoutSeconds 60 -Query {
        & docker exec $containerName pg_isready `
            --username $databaseUser --dbname $databaseName 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { 'ready' } else { 'waiting' }
    }

    $portOutput = (& docker port $containerName '5432/tcp' | Select-Object -First 1).Trim()
    if ($portOutput -notmatch ':(\d+)$') {
        throw "Could not determine PostgreSQL host port from '$portOutput'"
    }
    $databasePort = [int]$Matches[1]
    $script:jdbcUrl = "jdbc:postgresql://127.0.0.1:$databasePort/$databaseName"
    $probePort = Get-FreeTcpPort

    Write-Host '=== Bootstrap migrations and validate invalid input ==='
    $bootstrap = Wait-CapturedProcess -Captured (
        Start-MaintenanceProcess -TaskKey $primaryTask -ProbePort $probePort)
    Assert-ExitCode -Result $bootstrap -Expected 0 -Label 'Bootstrap maintenance process'
    Assert-LifecycleLog -Output $bootstrap.StandardOutput -ExpectedResult 'succeeded'
    Reset-MaintenanceRows

    $invalid = Wait-CapturedProcess -Captured (
        Start-MaintenanceProcess -TaskKey 'unknown-task' -ProbePort $probePort)
    Assert-ExitCode -Result $invalid -Expected 64 -Label 'Invalid maintenance process'
    if ((Get-ExecutionCount -TaskKey $primaryTask) -ne 0) {
        throw 'Invalid maintenance input caused a business side effect.'
    }

    Write-Host '=== Compete two real processes for the same task key ==='
    $competitionLockerName = "cp8-competition-" + [guid]::NewGuid().ToString('N')
    $competitionLocker = Start-RowLocker `
        -TaskKey $primaryTask -ApplicationName $competitionLockerName
    Wait-ForValue -Expected '1' -Label 'competition row locker' -Query {
        Invoke-PostgreSql -Sql (
            "select count(*) from pg_stat_activity " +
            "where application_name = '$competitionLockerName'")
    }
    $winner = Start-MaintenanceProcess -TaskKey $primaryTask -ProbePort $probePort
    Wait-AdvisoryLock -TaskId 1 -Expected '1'
    Assert-PortNotListening -Port $probePort

    $contender = Wait-CapturedProcess -Captured (
        Start-MaintenanceProcess -TaskKey $primaryTask -ProbePort $probePort)
    Assert-ExitCode -Result $contender -Expected 10 -Label 'Contended maintenance process'
    Assert-LifecycleLog -Output $contender.StandardOutput -ExpectedResult 'contended'
    if ((Get-ExecutionCount -TaskKey $primaryTask) -ne 0) {
        throw 'The contended process caused a business side effect.'
    }

    Stop-RowLocker -ApplicationName $competitionLockerName
    $null = Wait-CapturedProcess -Captured $competitionLocker
    $winnerResult = Wait-CapturedProcess -Captured $winner
    Assert-ExitCode -Result $winnerResult -Expected 0 -Label 'Lock-winning maintenance process'
    Assert-LifecycleLog -Output $winnerResult.StandardOutput -ExpectedResult 'acquired'
    Assert-LifecycleLog -Output $winnerResult.StandardOutput -ExpectedResult 'succeeded'
    if ((Get-ExecutionCount -TaskKey $primaryTask) -ne 1) {
        throw 'Same-key competition did not produce exactly one business side effect.'
    }

    Write-Host '=== Allow different task keys to execute independently ==='
    Reset-MaintenanceRows
    $differentLockerName = "cp8-different-" + [guid]::NewGuid().ToString('N')
    $differentLocker = Start-RowLocker `
        -TaskKey $primaryTask -ApplicationName $differentLockerName
    Wait-ForValue -Expected '1' -Label 'different-key row locker' -Query {
        Invoke-PostgreSql -Sql (
            "select count(*) from pg_stat_activity " +
            "where application_name = '$differentLockerName'")
    }
    $blockedPrimary = Start-MaintenanceProcess -TaskKey $primaryTask -ProbePort $probePort
    Wait-AdvisoryLock -TaskId 1 -Expected '1'
    $secondary = Wait-CapturedProcess -Captured (
        Start-MaintenanceProcess -TaskKey $secondaryTask -ProbePort $probePort)
    Assert-ExitCode -Result $secondary -Expected 0 -Label 'Different-key maintenance process'
    Assert-LifecycleLog -Output $secondary.StandardOutput -ExpectedResult 'succeeded'
    if ((Get-ExecutionCount -TaskKey $secondaryTask) -ne 1) {
        throw 'A different task key was unnecessarily excluded.'
    }
    Stop-RowLocker -ApplicationName $differentLockerName
    $null = Wait-CapturedProcess -Captured $differentLocker
    $blockedPrimaryResult = Wait-CapturedProcess -Captured $blockedPrimary
    Assert-ExitCode -Result $blockedPrimaryResult -Expected 0 `
        -Label 'Primary process after different-key verification'

    Write-Host '=== Kill the lock holder and retry the same task key ==='
    Reset-MaintenanceRows
    $crashLockerName = "cp8-crash-" + [guid]::NewGuid().ToString('N')
    $crashLocker = Start-RowLocker -TaskKey $primaryTask -ApplicationName $crashLockerName
    Wait-ForValue -Expected '1' -Label 'crash row locker' -Query {
        Invoke-PostgreSql -Sql (
            "select count(*) from pg_stat_activity " +
            "where application_name = '$crashLockerName'")
    }
    $crashed = Start-MaintenanceProcess -TaskKey $primaryTask -ProbePort $probePort
    Wait-AdvisoryLock -TaskId 1 -Expected '1'
    $crashedProcessId = $crashed.Process.Id
    $crashed.Process.Kill($true)
    $crashed.Process.WaitForExit()
    Wait-AdvisoryLock -TaskId 1 -Expected '0'
    if ((Get-ExecutionCount -TaskKey $primaryTask) -ne 0) {
        throw 'The killed process committed a business side effect before retry.'
    }

    Stop-RowLocker -ApplicationName $crashLockerName
    $null = Wait-CapturedProcess -Captured $crashLocker
    $retry = Wait-CapturedProcess -Captured (
        Start-MaintenanceProcess -TaskKey $primaryTask -ProbePort $probePort)
    Assert-ExitCode -Result $retry -Expected 0 -Label 'Maintenance retry after process kill'
    Assert-LifecycleLog -Output $retry.StandardOutput -ExpectedResult 'succeeded'
    if ((Get-ExecutionCount -TaskKey $primaryTask) -ne 1) {
        throw 'Retry after process kill did not produce exactly one committed side effect.'
    }

    $postgresVersion = Invoke-PostgreSql -Sql 'show server_version'
    Write-Host ("CP8 process evidence: winner PID=$($winnerResult.ProcessId), " +
        "contender PID=$($contender.ProcessId), contender exit=$($contender.ExitCode), " +
        "crashed PID=$crashedProcessId, retry PID=$($retry.ProcessId), " +
        "retry exit=$($retry.ExitCode), PostgreSQL=$postgresVersion")
    Write-Host "CP8 aggregate elapsed: $($verificationStopwatch.Elapsed)"
    Write-Host 'CP8 single execution, real process contention, crash recovery, and boundary checks succeeded.'
} finally {
    foreach ($captured in $processes) {
        if (-not $captured.Process.HasExited) {
            try { $captured.Process.Kill($true) } catch { }
        }
        $captured.Process.Dispose()
    }
    if ($containerStarted) {
        & docker rm --force $containerName | Out-Null
    }
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$verificationStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/runtime-foundation-consumer'
$consumerPom = Join-Path $consumerRoot 'pom.xml'
$consumerSource = Join-Path $consumerRoot 'application/src/main/java'
$consumerMigrations = Join-Path $consumerRoot 'application/src/main/resources/db/migration/customer'
$cp8Verification = Join-Path $PSScriptRoot 'verify-cp8-single-execution.ps1'
$cp9Verification = Join-Path $repositoryRoot 'build-support/performance-baseline/verify-performance-baseline.ps1'
$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp10-" + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$dependencyTree = Join-Path $verificationRoot 'consumer-runtime-dependencies.txt'
$containerName = "koiki-cp10-" + [guid]::NewGuid().ToString('N').Substring(0, 12)
$databaseName = 'koiki_cp10'
$databaseUser = 'postgres'
$databasePassword = [guid]::NewGuid().ToString('N')
$processes = [System.Collections.Generic.List[object]]::new()
$containerStarted = $false
$initialStatus = @(& git -C $repositoryRoot status --porcelain=v1 | Sort-Object)
if ($LASTEXITCODE -ne 0) { throw 'Unable to record the initial Git status' }

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase) -or
        [System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp10-*') {
        throw "Refusing to operate outside the CP10 OS temporary directory: $resolved"
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
    if (-not $process.Start()) { throw "Failed to start $FileName" }
    $captured = [pscustomobject]@{
        Process = $process
        StandardOutput = $process.StandardOutput.ReadToEndAsync()
        StandardError = $process.StandardError.ReadToEndAsync()
    }
    $null = $processes.Add($captured)
    return $captured
}

function Stop-CapturedProcess {
    param([Parameter(Mandatory)]$Captured)

    if (-not $Captured.Process.HasExited) {
        $Captured.Process.Kill($true)
    }
    $Captured.Process.WaitForExit()
    return [pscustomobject]@{
        ProcessId = $Captured.Process.Id
        ExitCode = $Captured.Process.ExitCode
        StandardOutput = $Captured.StandardOutput.GetAwaiter().GetResult()
        StandardError = $Captured.StandardError.GetAwaiter().GetResult()
    }
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
        try { $connected = $client.ConnectAsync('127.0.0.1', $Port).Wait(750) } catch { }
        if ($connected -and $client.Connected) {
            throw "Non-web maintenance process unexpectedly listened on TCP port $Port"
        }
    } finally {
        $client.Dispose()
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
    $actual = $null
    do {
        $actual = & $Query
        if ([string]$actual -eq $Expected) { return }
        Start-Sleep -Milliseconds 250
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw "$Label did not reach '$Expected'; last value was '$actual'"
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

function Invoke-Http {
    param(
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$Uri,
        [string]$Body,
        [hashtable]$Headers = @{}
    )

    $parameters = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
        SkipHttpErrorCheck = $true
        TimeoutSec = 10
    }
    if ($PSBoundParameters.ContainsKey('Body')) {
        $parameters.Body = $Body
        $parameters.ContentType = 'application/json'
    }
    $response = Invoke-WebRequest @parameters
    $content = if ($response.Content -is [byte[]]) {
        [System.Text.Encoding]::UTF8.GetString($response.Content)
    } else {
        [string]$response.Content
    }
    return [pscustomobject]@{
        StatusCode = $response.StatusCode
        Headers = $response.Headers
        Content = $content
    }
}

function Assert-StatusCode {
    param(
        [Parameter(Mandatory)]$Response,
        [Parameter(Mandatory)][int]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    if ([int]$Response.StatusCode -ne $Expected) {
        throw "$Label returned $($Response.StatusCode), expected $Expected. Body: $($Response.Content)"
    }
}

function Assert-JsonValue {
    param(
        [Parameter(Mandatory)]$Json,
        [Parameter(Mandatory)][string]$Property,
        [Parameter(Mandatory)]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Json.$Property -ne $Expected) {
        throw "$Label property '$Property' was '$($Json.$Property)', expected '$Expected'"
    }
}

function Assert-InventoryFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Artifact,
        [Parameter(Mandatory)][int]$JavaTypeCount,
        [Parameter(Mandatory)][int]$PropertyCount
    )

    $inventory = Get-Content -Raw -LiteralPath $Path
    foreach ($expected in @(
        "ARTIFACT $Artifact",
        "PUBLIC_JAVA_TYPES $JavaTypeCount",
        "PUBLIC_CONFIGURATION_PROPERTIES $PropertyCount")) {
        if ($inventory -notmatch [regex]::Escape($expected)) {
            throw "Public API inventory $Path does not contain '$expected'"
        }
    }
}

function Assert-StructuredLog {
    param(
        [Parameter(Mandatory)][string]$Output,
        [Parameter(Mandatory)][string]$Message,
        [Parameter(Mandatory)][string]$RequestId
    )

    $events = @($Output -split "`r?`n" | ForEach-Object {
        if ($_.StartsWith('{')) {
            try { $_ | ConvertFrom-Json } catch { $null }
        }
    } | Where-Object { $null -ne $_ -and $_.message -eq $Message })
    $matched = @($events | Where-Object {
        $_.requestId -eq $RequestId -and
        $_.service -eq 'runtime-foundation-consumer' -and
        $_.environment -eq 'cp10-acceptance'
    })
    if ($matched.Count -lt 1) {
        throw "Structured log '$Message' with requestId '$RequestId' was not found"
    }
}

function Assert-ReleaseUnitInventory {
    $expectedArtifacts = @(
        'koiki-javaweb-fw-reactor',
        'koiki-dependencies-bom',
        'koiki-parent',
        'koiki-architecture-contract',
        'koiki-archunit-rules',
        'koiki-starter-api',
        'koiki-starter-data',
        'koiki-starter-data-jpa',
        'koiki-starter-observability',
        'koiki-testing'
    )
    $groupRepository = Join-Path $isolatedRepository 'org/koikifw'
    $actualArtifacts = @(Get-ChildItem -LiteralPath $groupRepository -Directory |
        Select-Object -ExpandProperty Name | Sort-Object)
    $difference = @(Compare-Object ($expectedArtifacts | Sort-Object) $actualArtifacts)
    if ($difference.Count -ne 0) {
        throw "Staged release unit differs from the approved 10 projects: $($difference | Out-String)"
    }

    foreach ($artifact in $expectedArtifacts) {
        $artifactRoot = Join-Path $groupRepository "$artifact/0.1.0-SNAPSHOT"
        if (-not (Test-Path -LiteralPath (Join-Path $artifactRoot "$artifact-0.1.0-SNAPSHOT.pom"))) {
            throw "Staged POM is missing for $artifact"
        }
    }
    foreach ($artifact in @(
        'koiki-architecture-contract', 'koiki-archunit-rules', 'koiki-starter-api',
        'koiki-starter-data', 'koiki-starter-data-jpa', 'koiki-starter-observability',
        'koiki-testing')) {
        $jar = Join-Path $groupRepository "$artifact/0.1.0-SNAPSHOT/$artifact-0.1.0-SNAPSHOT.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "Staged JAR is missing for $artifact" }
    }
}

function Assert-PublicApiInventory {
    $approvedApi = Get-Content -Raw -LiteralPath (
        Join-Path $repositoryRoot 'build-support/api-compatibility/public-api.txt')
    foreach ($expected in @(
        'ARTIFACT koiki-architecture-contract',
        'TYPE annotation org.koikifw.architecture.KoikiModule',
        'TYPE enum org.koikifw.architecture.ModuleTier',
        'TYPE enum org.koikifw.architecture.PersistenceModel',
        'TYPE enum org.koikifw.architecture.PersistenceTechnology',
        'ARTIFACT koiki-archunit-rules',
        'TYPE class final org.koikifw.archunit.KoikiArchitectureRules')) {
        if ($approvedApi -notmatch [regex]::Escape($expected)) {
            throw "Approved Public API inventory does not contain '$expected'"
        }
    }
    if (@($approvedApi -split "`r?`n" | Where-Object { $_ -like 'TYPE *' }).Count -ne 5) {
        throw 'Approved Public API inventory does not contain exactly five public Java types'
    }

    Assert-InventoryFile -Path (Join-Path $repositoryRoot 'koiki-starters/koiki-starter-api/public-api.txt') `
        -Artifact 'koiki-starter-api' -JavaTypeCount 0 -PropertyCount 6
    Assert-InventoryFile -Path (Join-Path $repositoryRoot 'koiki-starters/koiki-starter-data/public-api.txt') `
        -Artifact 'koiki-starter-data' -JavaTypeCount 0 -PropertyCount 2
    Assert-InventoryFile -Path (Join-Path $repositoryRoot 'koiki-starters/koiki-starter-data-jpa/public-api.txt') `
        -Artifact 'koiki-starter-data-jpa' -JavaTypeCount 0 -PropertyCount 1
    Assert-InventoryFile -Path (Join-Path $repositoryRoot 'koiki-starters/koiki-starter-observability/public-api.txt') `
        -Artifact 'koiki-starter-observability' -JavaTypeCount 0 -PropertyCount 3
    Assert-InventoryFile -Path (Join-Path $repositoryRoot 'koiki-testing/public-api.txt') `
        -Artifact 'koiki-testing' -JavaTypeCount 0 -PropertyCount 0
}

function Assert-MigrationInventory {
    $formalRoots = @(
        'koiki-dependencies-bom', 'koiki-parent', 'koiki-architecture-contract',
        'koiki-archunit-rules', 'koiki-starters', 'koiki-testing')
    $frameworkSql = @($formalRoots | ForEach-Object {
        Get-ChildItem -LiteralPath (Join-Path $repositoryRoot $_) -Recurse -Filter '*.sql' |
            Where-Object { $_.FullName -notlike '*target*' }
    })
    if ($frameworkSql.Count -ne 0) {
        throw "Framework production SQL inventory must be empty: $($frameworkSql.FullName -join ', ')"
    }

    $expectedMigrations = @(
        'V1__create_work_item.sql',
        'V3__create_work_review.sql',
        'V4__create_work_item_maintenance.sql')
    $actualMigrations = @(Get-ChildItem -LiteralPath $consumerMigrations -Filter '*.sql' |
        Select-Object -ExpandProperty Name | Sort-Object)
    $difference = @(Compare-Object ($expectedMigrations | Sort-Object) $actualMigrations)
    if ($difference.Count -ne 0) {
        throw "Customer production migration inventory changed: $($difference | Out-String)"
    }
    $migrationText = (Get-ChildItem -LiteralPath $consumerMigrations -Filter '*.sql' |
        Get-Content -Raw) -join "`n"
    foreach ($table in @('kkbiz_work_item', 'kkbiz_work_review', 'kkbiz_work_item_maintenance')) {
        if ($migrationText -notmatch "create\s+table\s+$table\b") {
            throw "Customer migration does not own expected table $table"
        }
    }
}

Write-Host '=== Verify CP1 through CP8 accepted regression ==='
& pwsh -NoProfile -File $cp8Verification
if ($LASTEXITCODE -ne 0) { throw "CP8 regression failed with exit code $LASTEXITCODE" }

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage the clean 10-project KOIKI release unit' -Arguments @(
        '-f', $rootPom, 'clean', 'install', '-DskipTests')
    Assert-ReleaseUnitInventory
    Assert-PublicApiInventory
    Assert-MigrationInventory

    $internalReferences = @(Get-ChildItem -LiteralPath $consumerSource -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal')
    if ($internalReferences.Count -ne 0) {
        throw 'Customer-like Consumer references a KOIKI internal package'
    }

    Invoke-KoikiMaven -Label 'Package the independent Customer-like Consumer' -Arguments @(
        '-f', $consumerPom, 'clean', 'package', '-DskipTests')
    Invoke-KoikiMaven -Label 'Record the Consumer runtime dependency tree' -Arguments @(
        '-f', $consumerPom, '-pl', 'application', '-am',
        'dependency:tree', '-Dscope=runtime', "-DoutputFile=$dependencyTree")

    $runtimeDependencies = Get-Content -Raw -LiteralPath $dependencyTree
    foreach ($required in @(
        'org.koikifw:koiki-architecture-contract',
        'org.koikifw:koiki-starter-api',
        'org.koikifw:koiki-starter-data',
        'org.koikifw:koiki-starter-data-jpa',
        'org.koikifw:koiki-starter-observability')) {
        if ($runtimeDependencies -notmatch [regex]::Escape($required)) {
            throw "Required runtime artifact is missing: $required"
        }
    }
    foreach ($forbidden in @(
        'spring-batch', 'spring-cloud', 'kubernetes', 'mybatis', 'spring-modulith',
        'org.koikifw:koiki-archunit-rules',
        'org.koikifw:koiki-testing')) {
        if ($runtimeDependencies -match [regex]::Escape($forbidden)) {
            throw "Deferred or test-only dependency leaked into runtime: $forbidden"
        }
    }

    $consumerJar = Join-Path $consumerRoot `
        'application/target/runtime-foundation-consumer-application-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $consumerJar -PathType Leaf)) {
        throw "Consumer executable JAR was not found: $consumerJar"
    }

    Write-Host '=== Start dedicated PostgreSQL 17 for the packaged Developer Journey ==='
    $containerId = & docker run --detach --name $containerName `
        --publish '127.0.0.1::5432' `
        --env "POSTGRES_PASSWORD=$databasePassword" `
        --env "POSTGRES_DB=$databaseName" `
        'postgres:17-alpine'
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace(($containerId | Out-String))) {
        throw 'PostgreSQL container did not start successfully'
    }
    $containerStarted = $true
    Wait-ForValue -Expected 'ready' -Label 'PostgreSQL readiness' -Query {
        & docker exec $containerName pg_isready `
            --username $databaseUser --dbname $databaseName 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { 'ready' } else { 'waiting' }
    }
    $portOutput = (& docker port $containerName '5432/tcp' | Select-Object -First 1).Trim()
    if ($portOutput -notmatch ':(\d+)$') { throw "Unable to determine PostgreSQL port: $portOutput" }
    $databasePort = [int]$Matches[1]
    $jdbcUrl = "jdbc:postgresql://127.0.0.1:$databasePort/$databaseName"
    $applicationPort = Get-FreeTcpPort
    $processEnvironment = @{
        SPRING_DATASOURCE_URL = $jdbcUrl
        SPRING_DATASOURCE_USERNAME = $databaseUser
        SPRING_DATASOURCE_PASSWORD = $databasePassword
    }

    Write-Host '=== Start the packaged Consumer in web mode ==='
    $webProcess = Start-CapturedProcess -FileName 'java' -Environment $processEnvironment -Arguments @(
        '-jar', $consumerJar,
        "--server.port=$applicationPort",
        '--spring.main.banner-mode=off',
        '--koiki.environment=cp10-acceptance',
        '--spring.task.execution.pool.core-size=1',
        '--spring.task.execution.pool.max-size=1',
        '--spring.task.execution.pool.queue-capacity=8')
    $baseUri = "http://127.0.0.1:$applicationPort"
    Wait-ForValue -Expected 'UP' -Label 'Consumer readiness' -TimeoutSeconds 90 -Query {
        if ($webProcess.Process.HasExited) { return "EXIT:$($webProcess.Process.ExitCode)" }
        try {
            $health = Invoke-Http -Method 'GET' -Uri "$baseUri/actuator/health/readiness"
            if ($health.StatusCode -eq 200) { return ($health.Content | ConvertFrom-Json).status }
        } catch { }
        return 'waiting'
    }

    Write-Host '=== Observe versioned HTTP, Use Case, event, Domain, Repository, and DB ==='
    $createRequestId = 'cp10-create-request'
    $createResponse = Invoke-Http -Method 'POST' -Uri "$baseUri/api/1/work-items" `
        -Headers @{ 'X-Request-ID' = $createRequestId } `
        -Body (@{ label = 'cp10-developer-journey' } | ConvertTo-Json -Compress)
    Assert-StatusCode -Response $createResponse -Expected 201 -Label 'Versioned work item creation'
    $created = $createResponse.Content | ConvertFrom-Json
    $createdId = [guid]$created.id
    if ([string]$createResponse.Headers['Location'] -ne "/api/1/work-items/$createdId") {
        throw 'Versioned work item creation returned an unexpected Location header'
    }
    if ([string]$createResponse.Headers['X-Request-ID'] -ne $createRequestId) {
        throw 'Versioned work item creation did not echo the request correlation ID'
    }
    $storedPair = Invoke-PostgreSql -Sql (
        "select (select count(*) from kkbiz_work_item where id = '$createdId') || ':' || " +
        "(select count(*) from kkbiz_work_review where work_item_id = '$createdId' " +
        "and status = 'PENDING' and version = 0)")
    if ($storedPair -ne '1:1') { throw "HTTP business path stored unexpected rows: $storedPair" }

    Write-Host '=== Observe safe Problem Details and rollback ==='
    $validationResponse = Invoke-Http -Method 'POST' -Uri "$baseUri/api/1/work-items" `
        -Body '{}'
    Assert-StatusCode -Response $validationResponse -Expected 400 -Label 'Validation rejection'
    $validationProblem = $validationResponse.Content | ConvertFrom-Json
    Assert-JsonValue -Json $validationProblem -Property 'code' `
        -Expected 'KOIKI-VALIDATION-001' -Label 'Validation Problem Details'
    if ($validationResponse.Content -match 'rejectedValue') {
        throw 'Validation Problem Details exposed a rejected value'
    }

    $rejectedLabel = 'x' * 101
    $rejectionResponse = Invoke-Http -Method 'POST' -Uri "$baseUri/api/1/work-items" `
        -Body (@{ label = $rejectedLabel } | ConvertTo-Json -Compress)
    Assert-StatusCode -Response $rejectionResponse -Expected 422 -Label 'Domain rejection'
    $rejectionProblem = $rejectionResponse.Content | ConvertFrom-Json
    Assert-JsonValue -Json $rejectionProblem -Property 'code' `
        -Expected 'WORKREVIEW-001' -Label 'Domain Problem Details'
    if ($rejectionResponse.Content -match 'Exception|label exceeds') {
        throw 'Domain Problem Details exposed an internal diagnostic'
    }
    $rejectedRows = Invoke-PostgreSql -Sql (
        "select (select count(*) from kkbiz_work_item where label = '$rejectedLabel') + " +
        "(select count(*) from kkbiz_work_review where label = '$rejectedLabel')")
    if ($rejectedRows -ne '0') { throw 'Domain rejection did not roll back both modules' }

    Write-Host '=== Observe structured correlation across the async boundary ==='
    $asyncRequestId = 'cp10-async-request'
    $asyncResponse = Invoke-Http -Method 'POST' `
        -Uri "$baseUri/api/1/work-items/$createdId/process" `
        -Headers @{ 'X-Request-ID' = $asyncRequestId }
    Assert-StatusCode -Response $asyncResponse -Expected 202 -Label 'Async processing'
    $asyncBody = $asyncResponse.Content | ConvertFrom-Json
    Assert-JsonValue -Json $asyncBody -Property 'result' -Expected 'accepted' -Label 'Async response'
    if ([string]$asyncResponse.Headers['X-Request-ID'] -ne $asyncRequestId) {
        throw 'Async response did not echo the request correlation ID'
    }

    Write-Host '=== Observe health, migrations, and table ownership ==='
    foreach ($path in @('/actuator/health', '/actuator/health/liveness', '/actuator/health/readiness')) {
        $healthResponse = Invoke-Http -Method 'GET' -Uri "$baseUri$path"
        Assert-StatusCode -Response $healthResponse -Expected 200 -Label "Health endpoint $path"
        $healthBody = $healthResponse.Content | ConvertFrom-Json
        Assert-JsonValue -Json $healthBody -Property 'status' -Expected 'UP' -Label "Health endpoint $path"
        if ($healthResponse.Content -match 'jdbc:|postgres|password|SQLException|stackTrace') {
            throw "Health endpoint $path exposed a sensitive implementation detail"
        }
    }
    $expectedTables = @(
        'flyway_schema_history',
        'kkbiz_work_item',
        'kkbiz_work_item_maintenance',
        'kkbiz_work_review',
        'koiki_flyway_history')
    $tableInventory = Invoke-PostgreSql -Sql (
        "select table_name from information_schema.tables " +
        "where table_schema = 'public' and table_type = 'BASE TABLE' order by table_name")
    $actualTables = @($tableInventory -split "`r?`n" | Where-Object { $_ -ne '' })
    $tableDifference = @(Compare-Object $expectedTables $actualTables)
    if ($tableDifference.Count -ne 0) {
        throw "Runtime table inventory differs from the expected five tables: " +
            ($tableDifference | Out-String)
    }
    $customerMigrations = Invoke-PostgreSql -Sql (
        "select count(*) from flyway_schema_history where success " +
        "and type <> 'BASELINE'")
    if ($customerMigrations -ne '3') {
        throw "Customer Flyway history contained $customerMigrations successful migrations, expected 3"
    }

    $webResult = Stop-CapturedProcess -Captured $webProcess
    Assert-StructuredLog -Output $webResult.StandardOutput `
        -Message 'work item async processed' -RequestId $asyncRequestId

    Write-Host '=== Start the same packaged Consumer JAR as a non-web maintenance process ==='
    $maintenancePort = Get-FreeTcpPort
    $maintenance = Start-CapturedProcess -FileName 'java' -Environment $processEnvironment -Arguments @(
        '-jar', $consumerJar,
        '--koiki.consumer.mode=maintenance',
        '--koiki.consumer.task-key=workitem-maintenance-primary',
        "--server.port=$maintenancePort",
        '--spring.main.banner-mode=off',
        '--koiki.environment=cp10-acceptance')
    $maintenanceResult = Wait-CapturedProcess -Captured $maintenance
    if ($maintenanceResult.ExitCode -ne 0) {
        throw "Maintenance journey exited with $($maintenanceResult.ExitCode).`n" +
            "stdout:`n$($maintenanceResult.StandardOutput)`nstderr:`n$($maintenanceResult.StandardError)"
    }
    Assert-PortNotListening -Port $maintenancePort
    $maintenanceCount = Invoke-PostgreSql -Sql (
        "select execution_count from kkbiz_work_item_maintenance " +
        "where task_key = 'workitem-maintenance-primary'")
    if ($maintenanceCount -ne '1') {
        throw "Maintenance journey produced $maintenanceCount side effects, expected 1"
    }
    $lifecycle = @($maintenanceResult.StandardOutput -split "`r?`n" | ForEach-Object {
        if ($_.StartsWith('{')) { try { $_ | ConvertFrom-Json } catch { $null } }
    } | Where-Object {
        $null -ne $_ -and $_.message -eq 'work item maintenance lifecycle' -and
        $_.result -eq 'succeeded'
    })
    if ($lifecycle.Count -lt 1) { throw 'Maintenance succeeded lifecycle log was not found' }

    $postgresVersion = Invoke-PostgreSql -Sql 'show server_version'
    Write-Host ("CP10 Developer Journey evidence: web PID=$($webResult.ProcessId), " +
        "maintenance PID=$($maintenanceResult.ProcessId), workItem=$createdId, " +
        "tables=$($actualTables.Count), customerMigrations=$customerMigrations, PostgreSQL=$postgresVersion")
} finally {
    $cleanupFailure = $null
    foreach ($captured in $processes) {
        if (-not $captured.Process.HasExited) {
            try { $captured.Process.Kill($true) } catch { }
        }
        $captured.Process.Dispose()
    }
    if ($containerStarted) {
        & docker rm --force $containerName | Out-Null
        if ($LASTEXITCODE -ne 0) {
            $cleanupFailure = "Docker container cleanup failed for $containerName"
        }
    }
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
    if ($null -ne $cleanupFailure) { throw $cleanupFailure }
}

Write-Host '=== Verify the CP9 harness contract with a shortened smoke run ==='
& pwsh -NoProfile -File $cp9Verification -Smoke -SkipRegression
if ($LASTEXITCODE -ne 0) { throw "CP9 smoke failed with exit code $LASTEXITCODE" }

$finalStatus = @(& git -C $repositoryRoot status --porcelain=v1 | Sort-Object)
if ($LASTEXITCODE -ne 0) { throw 'Unable to record the final Git status' }
$statusDifference = @(Compare-Object $initialStatus $finalStatus)
if ($statusDifference.Count -ne 0) {
    throw "CP10 verification changed tracked or visible untracked files: $($statusDifference | Out-String)"
}

Write-Host "CP10 closeout aggregate elapsed: $($verificationStopwatch.Elapsed)"
Write-Host 'CP10 Developer Journey, DoD inventory, CP8 regression, CP9 smoke, and cleanup succeeded.'

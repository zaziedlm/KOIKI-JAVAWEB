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
$containerStarted = $false
$sessionDeleteRevoked = $false
$processA = $null
$processB = $null
$fixtureJar = $null
$httpResources = [System.Collections.Generic.List[object]]::new()
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
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][System.Guid]$ExpectedUserId,
        [Parameter(Mandatory)][string]$ExpectedPermissions
    )

    $response = $Client.GetAsync("http://127.0.0.1:$Port/fixture/session").GetAwaiter().GetResult()
    $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$response.StatusCode -ne 200 -or
        $content -ne "$ExpectedUserId|$ExpectedPermissions") {
        throw 'The shared Session did not restore the expected Framework user and Permission.'
    }
}

function Invoke-RejectedObservation {
    param(
        [Parameter(Mandatory)][System.Net.Http.HttpClient]$Client,
        [Parameter(Mandatory)][int]$Port
    )

    $response = $Client.GetAsync("http://127.0.0.1:$Port/fixture/session").GetAwaiter().GetResult()
    if ([int]$response.StatusCode -notin @(302, 303) -or
        -not $response.Headers.Location.ToString().Contains('/login')) {
        throw 'An invalidated target Session remained authenticated on process B.'
    }
}

function New-FixtureClient {
    $cookies = [System.Net.CookieContainer]::new()
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $handler.CookieContainer = $cookies
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(10)
    $resource = [pscustomobject]@{
        Client = $client
        Handler = $handler
        Cookies = $cookies
    }
    [void]$httpResources.Add($resource)
    return $resource
}

function Invoke-FixtureLogin {
    param(
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$Email,
        [Parameter(Mandatory)][string]$Password,
        [switch]$ExpectFailure
    )

    $resource = New-FixtureClient
    $loginPageResponse = $resource.Client.GetAsync(
        "http://127.0.0.1:$Port/login").GetAwaiter().GetResult()
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

    $fields = [System.Collections.Generic.Dictionary[string, string]]::new()
    $fields.Add('username', $Email)
    $fields.Add('password', $Password)
    $fields.Add('_csrf', $csrfValue)
    $body = [System.Net.Http.FormUrlEncodedContent]::new($fields)
    try {
        $response = $resource.Client.PostAsync(
            "http://127.0.0.1:$Port/login", $body).GetAwaiter().GetResult()
    } finally {
        $body.Dispose()
    }
    if ([int]$response.StatusCode -notin @(302, 303)) {
        throw 'Standard Form Login did not return a redirect.'
    }
    $location = $response.Headers.Location.ToString()
    $sessionCookies = @($resource.Cookies.GetCookies(
            [Uri]"http://127.0.0.1:$Port/") | Where-Object Name -eq 'SESSION')
    if ($ExpectFailure) {
        if (-not $location.Contains('/login?error') -or $sessionCookies.Count -ne 1) {
            throw 'The obsolete local password did not fail as expected.'
        }
    } elseif ($location.Contains('/login?error') -or $sessionCookies.Count -ne 1) {
        throw 'Standard Form Login did not establish exactly one successful Session Cookie.'
    }
    return $resource
}

function Invoke-BootstrapPost {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][hashtable]$Body
    )

    $response = Invoke-WebRequest -Method Post `
        -Uri "http://127.0.0.1:$portA$Path" `
        -Headers @{'X-Koiki-Fixture-Key' = $bootstrapKey} `
        -Body $Body -SkipHttpErrorCheck -TimeoutSec 10
    if ($response.StatusCode -ne 204) {
        throw "Fixture bootstrap request failed with HTTP $($response.StatusCode): $Path"
    }
}

function Reset-FixtureState {
    Invoke-BootstrapPost -Path '/fixture/reset' -Body @{}
}

function Add-FixtureIdentityPermission {
    param(
        [Parameter(Mandatory)][System.Guid]$UserId,
        [Parameter(Mandatory)][string]$Email,
        [Parameter(Mandatory)][string]$Password,
        [Parameter(Mandatory)][System.Guid]$RoleId,
        [Parameter(Mandatory)][string]$RoleCode,
        [Parameter(Mandatory)][System.Guid]$PermissionId,
        [Parameter(Mandatory)][string]$PermissionCode
    )

    Invoke-BootstrapPost -Path '/fixture/setup' -Body @{
        userId = [string]$UserId
        email = $Email
        password = $Password
        roleId = [string]$RoleId
        roleCode = $RoleCode
        permissionId = [string]$PermissionId
        permissionCode = $PermissionCode
    }
}

function New-MutationScenario {
    param([ValidateRange(1, 2)][int]$TargetCount = 1)

    Reset-FixtureState
    $targetRoleId = [System.Guid]::NewGuid()
    $targetPermissionId = [System.Guid]::NewGuid()
    $targets = @()
    for ($index = 0; $index -lt $TargetCount; $index++) {
        $target = [pscustomobject]@{
            UserId = [System.Guid]::NewGuid()
            Email = 'b34-target-' + [System.Guid]::NewGuid().ToString('N') + '@invalid.example'
            Password = 'K9!' + [System.Guid]::NewGuid().ToString('N') + 'aZ'
        }
        Add-FixtureIdentityPermission -UserId $target.UserId -Email $target.Email `
            -Password $target.Password -RoleId $targetRoleId -RoleCode 'B3_TARGET' `
            -PermissionId $targetPermissionId -PermissionCode 'ORDER:READ'
        $target | Add-Member -NotePropertyName Session `
            -NotePropertyValue (Invoke-FixtureLogin -Port $portA `
                -Email $target.Email -Password $target.Password)
        $targets += $target
    }

    $admin = [pscustomobject]@{
        UserId = [System.Guid]::NewGuid()
        Email = 'b34-control-' + [System.Guid]::NewGuid().ToString('N') + '@invalid.example'
        Password = 'K9!' + [System.Guid]::NewGuid().ToString('N') + 'aZ'
    }
    $adminRoleId = [System.Guid]::NewGuid()
    Add-FixtureIdentityPermission -UserId $admin.UserId -Email $admin.Email `
        -Password $admin.Password -RoleId $adminRoleId -RoleCode 'B3_ADMIN' `
        -PermissionId ([System.Guid]::NewGuid()) -PermissionCode 'IDENTITY:ADMIN'
    $admin | Add-Member -NotePropertyName Session `
        -NotePropertyValue (Invoke-FixtureLogin -Port $portB `
            -Email $admin.Email -Password $admin.Password)

    return [pscustomobject]@{
        Targets = $targets
        TargetRoleId = $targetRoleId
        Admin = $admin
    }
}

function Invoke-AdminMutation {
    param(
        [Parameter(Mandatory)][object]$AdminSession,
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][hashtable]$Fields
    )

    $csrfResponse = $AdminSession.Client.GetAsync(
        "http://127.0.0.1:$portB/fixture/admin/csrf").GetAwaiter().GetResult()
    $csrfToken = $csrfResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$csrfResponse.StatusCode -ne 200 -or [string]::IsNullOrWhiteSpace($csrfToken)) {
        throw 'The authenticated administration CSRF token was not available.'
    }
    $formFields = [System.Collections.Generic.Dictionary[string, string]]::new()
    foreach ($entry in $Fields.GetEnumerator()) {
        $formFields.Add($entry.Key, [string]$entry.Value)
    }
    $formFields.Add('_csrf', $csrfToken)
    $body = [System.Net.Http.FormUrlEncodedContent]::new($formFields)
    try {
        $response = $AdminSession.Client.PostAsync(
            "http://127.0.0.1:$portB$Path", $body).GetAwaiter().GetResult()
    } finally {
        $body.Dispose()
    }
    if ([int]$response.StatusCode -ne 204) {
        throw "Identity administration request failed: $Path"
    }
}

function Get-FixtureCsrfToken {
    param([Parameter(Mandatory)][object]$Session)

    $response = $Session.Client.GetAsync(
        "http://127.0.0.1:$portB/fixture/csrf").GetAwaiter().GetResult()
    $token = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$response.StatusCode -ne 200 -or [string]::IsNullOrWhiteSpace($token)) {
        throw 'The authenticated fixture CSRF token was not available.'
    }
    return $token
}

function Copy-FixtureSession {
    param([Parameter(Mandatory)][object]$Session)

    $sourceCookies = @($Session.Cookies.GetCookies(
            [Uri]"http://127.0.0.1:$portB/") | Where-Object Name -eq 'SESSION')
    if ($sourceCookies.Count -ne 1) {
        throw 'The Session Cookie could not be copied for bounded failure observation.'
    }
    $copy = New-FixtureClient
    $copy.Cookies.Add(
        [Uri]"http://127.0.0.1:$portB/",
        [System.Net.Cookie]::new('SESSION', $sourceCookies[0].Value, '/'))
    return $copy
}

function Invoke-AdminMutationFailure {
    param(
        [Parameter(Mandatory)][object]$AdminSession,
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][hashtable]$Fields
    )

    $csrfToken = Get-FixtureCsrfToken -Session $AdminSession
    $formFields = [System.Collections.Generic.Dictionary[string, string]]::new()
    foreach ($entry in $Fields.GetEnumerator()) {
        $formFields.Add($entry.Key, [string]$entry.Value)
    }
    $formFields.Add('_csrf', $csrfToken)
    $body = [System.Net.Http.FormUrlEncodedContent]::new($formFields)
    try {
        $response = $AdminSession.Client.PostAsync(
            "http://127.0.0.1:$portB$Path", $body).GetAwaiter().GetResult()
    } finally {
        $body.Dispose()
    }
    $responseText = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$response.StatusCode -lt 400) {
        throw 'The Session DELETE failure was exposed as a successful Identity mutation.'
    }
    if ($responseText -match '(?i)(?:jdbc:|delete\s+from|org\.springframework|postgresql|stacktrace)') {
        throw 'The Identity failure response exposed an internal persistence detail.'
    }
}

function Invoke-FixtureLogout {
    param(
        [Parameter(Mandatory)][object]$Session,
        [Parameter(Mandatory)][string]$CsrfToken,
        [switch]$ExpectFailure
    )

    $fields = [System.Collections.Generic.Dictionary[string, string]]::new()
    $fields.Add('_csrf', $CsrfToken)
    $body = [System.Net.Http.FormUrlEncodedContent]::new($fields)
    try {
        $response = $Session.Client.PostAsync(
            "http://127.0.0.1:$portB/logout", $body).GetAwaiter().GetResult()
    } finally {
        $body.Dispose()
    }
    $responseText = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ($ExpectFailure) {
        if ([int]$response.StatusCode -lt 400) {
            throw 'The persistent Session logout failure was exposed as a success redirect.'
        }
        if ($responseText -match '(?i)(?:jdbc:|delete\s+from|org\.springframework|postgresql|stacktrace)') {
            throw 'The logout failure response exposed an internal persistence detail.'
        }
    } elseif ([int]$response.StatusCode -notin @(302, 303) -or
        -not $response.Headers.Location.ToString().Contains('/login?logout')) {
        throw 'Logout did not complete after Session DELETE permission was restored.'
    }
}

function Assert-ControlContinues {
    param([Parameter(Mandatory)][object]$Scenario)

    Invoke-AuthenticatedObservation -Client $Scenario.Admin.Session.Client -Port $portB `
        -ExpectedUserId $Scenario.Admin.UserId `
        -ExpectedPermissions 'IDENTITY:ADMIN'
}

function Assert-NoPrincipalSession {
    param([Parameter(Mandatory)][System.Guid]$UserId)

    $count = Invoke-Postgres -Label 'Invalidated principal Session observation' -Sql (
        "SELECT count(*) FROM koiki_session WHERE principal_name = '$UserId';")
    if ($count -ne '0') {
        throw 'An invalidated principal retained a persisted Session row.'
    }
}

function Disable-SessionDeletePermission {
    Invoke-Postgres -Label 'Session DELETE permission revoke' -Sql (
        "REVOKE DELETE ON public.koiki_session FROM $appRole;") | Out-Null
    $script:sessionDeleteRevoked = $true
    $boundary = Invoke-Postgres -Label 'Restricted Session permission observation' -Sql (
        "SELECT has_table_privilege('$appRole', 'public.koiki_session', 'SELECT') || '|' || " +
        "has_table_privilege('$appRole', 'public.koiki_session', 'DELETE') || '|' || " +
        "has_table_privilege('$appRole', 'public.koiki_session_attributes', 'DELETE');")
    if ($boundary -ne 'true|false|true') {
        throw 'The Session DELETE-only failure boundary was not established.'
    }
}

function Enable-SessionDeletePermission {
    Invoke-Postgres -Label 'Session DELETE permission restore' -Sql (
        "GRANT DELETE ON public.koiki_session TO $appRole;") | Out-Null
    $boundary = Invoke-Postgres -Label 'Restored Session permission observation' -Sql (
        "SELECT has_table_privilege(" +
        "'$appRole', 'public.koiki_session', 'DELETE')::text;")
    if ($boundary -ne 'true') {
        throw 'The Session DELETE permission was not restored.'
    }
    $script:sessionDeleteRevoked = $false
}

function Assert-NoMutationAudit {
    param([Parameter(Mandatory)][string]$Action)

    $count = Invoke-Postgres -Label 'Rolled-back mutation Audit observation' -Sql (
        "SELECT count(*) FROM koiki_audit_event WHERE action = '$Action';")
    if ($count -ne '0') {
        throw 'A failed Identity mutation retained its mutation Audit row.'
    }
}

function Wait-OperatorFailureMarker {
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds(5)
    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        foreach ($logPath in @($processBLog, $processBError)) {
            if (Test-Path -LiteralPath $logPath) {
                $content = Get-Content -Raw -LiteralPath $logPath
                if ($null -ne $content -and $content.Contains(
                        'KOIKI persistent session logout failed; local state was cleared')) {
                    return
                }
            }
        }
        Start-Sleep -Milliseconds 100
    }
    throw 'The operator-visible persistent logout failure marker was not recorded.'
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

    $continuity = New-MutationScenario
    $continuityTarget = $continuity.Targets[0]
    Invoke-AuthenticatedObservation -Client $continuityTarget.Session.Client -Port $portA `
        -ExpectedUserId $continuityTarget.UserId -ExpectedPermissions 'ORDER:READ'
    Invoke-AuthenticatedObservation -Client $continuityTarget.Session.Client -Port $portB `
        -ExpectedUserId $continuityTarget.UserId -ExpectedPermissions 'ORDER:READ'
    $sessionRow = Invoke-Postgres -Label 'Session row observation' -Sql (
        "SELECT count(*) FROM koiki_session WHERE principal_name = '$($continuityTarget.UserId)';")
    if ($sessionRow -ne '1') {
        throw 'The persisted Session row or immutable principal index was unexpected.'
    }

    $disableScenario = New-MutationScenario
    $disableTarget = $disableScenario.Targets[0]
    Invoke-AdminMutation -AdminSession $disableScenario.Admin.Session `
        -Path '/fixture/admin/disable' `
        -Fields @{userId = $disableTarget.UserId; expectedVersion = 0}
    Invoke-RejectedObservation -Client $disableTarget.Session.Client -Port $portB
    Assert-ControlContinues -Scenario $disableScenario
    Assert-NoPrincipalSession -UserId $disableTarget.UserId
    $disableState = Invoke-Postgres -Label 'Disabled user state observation' -Sql (
        "SELECT status || '|' || version FROM koiki_user " +
        "WHERE user_id = '$($disableTarget.UserId)';")
    if ($disableState -ne 'DISABLED|1') {
        throw 'The disable mutation did not persist its expected fresh-state transition.'
    }

    $passwordScenario = New-MutationScenario
    $passwordTarget = $passwordScenario.Targets[0]
    $newPassword = 'K9!' + [System.Guid]::NewGuid().ToString('N') + 'nW'
    Invoke-AdminMutation -AdminSession $passwordScenario.Admin.Session `
        -Path '/fixture/admin/password' `
        -Fields @{
            userId = $passwordTarget.UserId
            password = $newPassword
            expectedVersion = 0
        }
    Invoke-RejectedObservation -Client $passwordTarget.Session.Client -Port $portB
    Assert-ControlContinues -Scenario $passwordScenario
    Assert-NoPrincipalSession -UserId $passwordTarget.UserId
    $passwordState = Invoke-Postgres -Label 'Password state observation' -Sql (
        "SELECT u.version || '|' || c.version FROM koiki_user u " +
        "JOIN koiki_password_credential c ON c.user_id = u.user_id " +
        "WHERE u.user_id = '$($passwordTarget.UserId)';")
    if ($passwordState -ne '1|1') {
        throw 'The password mutation did not advance both optimistic versions.'
    }
    Invoke-FixtureLogin -Port $portB -Email $passwordTarget.Email `
        -Password $passwordTarget.Password -ExpectFailure | Out-Null
    $newPasswordSession = Invoke-FixtureLogin -Port $portB `
        -Email $passwordTarget.Email -Password $newPassword
    Invoke-AuthenticatedObservation -Client $newPasswordSession.Client -Port $portB `
        -ExpectedUserId $passwordTarget.UserId -ExpectedPermissions 'ORDER:READ'

    $userRoleScenario = New-MutationScenario
    $userRoleTarget = $userRoleScenario.Targets[0]
    Invoke-AdminMutation -AdminSession $userRoleScenario.Admin.Session `
        -Path '/fixture/admin/user-role' `
        -Fields @{
            userId = $userRoleTarget.UserId
            roleCode = 'B3_TARGET'
            expectedVersion = 0
        }
    Invoke-RejectedObservation -Client $userRoleTarget.Session.Client -Port $portB
    Assert-ControlContinues -Scenario $userRoleScenario
    Assert-NoPrincipalSession -UserId $userRoleTarget.UserId
    $userRoleState = Invoke-Postgres -Label 'User Role state observation' -Sql (
        "SELECT u.version || '|' || count(ur.role_id) FROM koiki_user u " +
        "LEFT JOIN koiki_user_role ur ON ur.user_id = u.user_id " +
        "WHERE u.user_id = '$($userRoleTarget.UserId)' GROUP BY u.version;")
    if ($userRoleState -ne '1|0') {
        throw 'The user Role mutation did not persist its expected fresh-state transition.'
    }

    $rolePermissionScenario = New-MutationScenario -TargetCount 2
    Invoke-AdminMutation -AdminSession $rolePermissionScenario.Admin.Session `
        -Path '/fixture/admin/role-permission' `
        -Fields @{
            roleCode = 'B3_TARGET'
            permissionCode = 'ORDER:READ'
            expectedRoleVersion = 0
        }
    foreach ($affectedTarget in $rolePermissionScenario.Targets) {
        Invoke-RejectedObservation -Client $affectedTarget.Session.Client -Port $portB
        Assert-NoPrincipalSession -UserId $affectedTarget.UserId
    }
    Assert-ControlContinues -Scenario $rolePermissionScenario
    $rolePermissionState = Invoke-Postgres -Label 'Role Permission state observation' -Sql (
        "SELECT r.version || '|' || count(rp.permission_id) FROM koiki_role r " +
        "LEFT JOIN koiki_role_permission rp ON rp.role_id = r.role_id " +
        "WHERE r.role_code = 'B3_TARGET' GROUP BY r.version;")
    if ($rolePermissionState -ne '1|0') {
        throw 'The Role Permission mutation did not persist its expected fresh-state transition.'
    }

    $externalScenario = New-MutationScenario
    $externalTarget = $externalScenario.Targets[0]
    $externalIssuer = 'https://issuer.invalid.example/b34'
    Invoke-BootstrapPost -Path '/fixture/setup-external' -Body @{
        linkId = [string][System.Guid]::NewGuid()
        userId = [string]$externalTarget.UserId
        issuer = $externalIssuer
        subject = 'subject-' + [System.Guid]::NewGuid().ToString('N')
    }
    Invoke-AdminMutation -AdminSession $externalScenario.Admin.Session `
        -Path '/fixture/admin/external-unlink' `
        -Fields @{
            userId = $externalTarget.UserId
            issuer = $externalIssuer
            expectedVersion = 0
        }
    Invoke-RejectedObservation -Client $externalTarget.Session.Client -Port $portB
    Assert-ControlContinues -Scenario $externalScenario
    Assert-NoPrincipalSession -UserId $externalTarget.UserId
    $externalState = Invoke-Postgres -Label 'External unlink state observation' -Sql (
        "SELECT u.version || '|' || count(l.link_id) || '|' || count(c.user_id) " +
        "FROM koiki_user u " +
        "LEFT JOIN koiki_external_identity_link l ON l.user_id = u.user_id " +
        "LEFT JOIN koiki_password_credential c ON c.user_id = u.user_id " +
        "WHERE u.user_id = '$($externalTarget.UserId)' GROUP BY u.version;")
    if ($externalState -ne '1|0|1') {
        throw 'External unlink did not retain the alternate local authentication method.'
    }

    $disableFailureScenario = New-MutationScenario
    $disableFailureTarget = $disableFailureScenario.Targets[0]
    Disable-SessionDeletePermission
    Invoke-AdminMutationFailure -AdminSession $disableFailureScenario.Admin.Session `
        -Path '/fixture/admin/disable' `
        -Fields @{userId = $disableFailureTarget.UserId; expectedVersion = 0}
    $failedDisableState = Invoke-Postgres -Label 'Failed disable rollback observation' -Sql (
        "SELECT status || '|' || version FROM koiki_user " +
        "WHERE user_id = '$($disableFailureTarget.UserId)';")
    if ($failedDisableState -ne 'ACTIVE|0') {
        throw 'Account disable escaped the Session invalidation rollback.'
    }
    Assert-NoMutationAudit -Action 'DISABLE_ACCOUNT'
    Invoke-AuthenticatedObservation -Client $disableFailureTarget.Session.Client -Port $portB `
        -ExpectedUserId $disableFailureTarget.UserId -ExpectedPermissions 'ORDER:READ'
    Assert-ControlContinues -Scenario $disableFailureScenario
    Enable-SessionDeletePermission

    $userRoleFailureScenario = New-MutationScenario
    $userRoleFailureTarget = $userRoleFailureScenario.Targets[0]
    Disable-SessionDeletePermission
    Invoke-AdminMutationFailure -AdminSession $userRoleFailureScenario.Admin.Session `
        -Path '/fixture/admin/user-role' `
        -Fields @{
            userId = $userRoleFailureTarget.UserId
            roleCode = 'B3_TARGET'
            expectedVersion = 0
        }
    $failedUserRoleState = Invoke-Postgres `
        -Label 'Failed user Role rollback observation' -Sql (
            "SELECT u.version || '|' || count(ur.role_id) FROM koiki_user u " +
            "LEFT JOIN koiki_user_role ur ON ur.user_id = u.user_id " +
            "WHERE u.user_id = '$($userRoleFailureTarget.UserId)' GROUP BY u.version;")
    if ($failedUserRoleState -ne '0|1') {
        throw 'User Role revoke escaped the Session invalidation rollback.'
    }
    Assert-NoMutationAudit -Action 'REVOKE_ROLE'
    Invoke-AuthenticatedObservation -Client $userRoleFailureTarget.Session.Client -Port $portB `
        -ExpectedUserId $userRoleFailureTarget.UserId -ExpectedPermissions 'ORDER:READ'
    Assert-ControlContinues -Scenario $userRoleFailureScenario
    Enable-SessionDeletePermission

    $rolePermissionFailureScenario = New-MutationScenario -TargetCount 2
    Disable-SessionDeletePermission
    Invoke-AdminMutationFailure -AdminSession $rolePermissionFailureScenario.Admin.Session `
        -Path '/fixture/admin/role-permission' `
        -Fields @{
            roleCode = 'B3_TARGET'
            permissionCode = 'ORDER:READ'
            expectedRoleVersion = 0
        }
    $failedRolePermissionState = Invoke-Postgres `
        -Label 'Failed Role Permission rollback observation' -Sql (
            "SELECT r.version || '|' || count(rp.permission_id) FROM koiki_role r " +
            "LEFT JOIN koiki_role_permission rp ON rp.role_id = r.role_id " +
            "WHERE r.role_code = 'B3_TARGET' GROUP BY r.version;")
    if ($failedRolePermissionState -ne '0|1') {
        throw 'Role Permission revoke escaped the Session invalidation rollback.'
    }
    Assert-NoMutationAudit -Action 'REVOKE_PERMISSION'
    foreach ($failureTarget in $rolePermissionFailureScenario.Targets) {
        Invoke-AuthenticatedObservation -Client $failureTarget.Session.Client -Port $portB `
            -ExpectedUserId $failureTarget.UserId -ExpectedPermissions 'ORDER:READ'
    }
    Assert-ControlContinues -Scenario $rolePermissionFailureScenario
    Enable-SessionDeletePermission

    $externalFailureScenario = New-MutationScenario
    $externalFailureTarget = $externalFailureScenario.Targets[0]
    $failureIssuer = 'https://issuer.invalid.example/b34-failure'
    Invoke-BootstrapPost -Path '/fixture/setup-external' -Body @{
        linkId = [string][System.Guid]::NewGuid()
        userId = [string]$externalFailureTarget.UserId
        issuer = $failureIssuer
        subject = 'subject-' + [System.Guid]::NewGuid().ToString('N')
    }
    Disable-SessionDeletePermission
    Invoke-AdminMutationFailure -AdminSession $externalFailureScenario.Admin.Session `
        -Path '/fixture/admin/external-unlink' `
        -Fields @{
            userId = $externalFailureTarget.UserId
            issuer = $failureIssuer
            expectedVersion = 0
        }
    $failedExternalState = Invoke-Postgres `
        -Label 'Failed external unlink rollback observation' -Sql (
            "SELECT u.version || '|' || count(l.link_id) FROM koiki_user u " +
            "LEFT JOIN koiki_external_identity_link l ON l.user_id = u.user_id " +
            "WHERE u.user_id = '$($externalFailureTarget.UserId)' GROUP BY u.version;")
    if ($failedExternalState -ne '0|1') {
        throw 'External unlink escaped the Session invalidation rollback.'
    }
    Assert-NoMutationAudit -Action 'UNLINK_EXTERNAL_IDENTITY'
    Invoke-AuthenticatedObservation -Client $externalFailureTarget.Session.Client -Port $portB `
        -ExpectedUserId $externalFailureTarget.UserId -ExpectedPermissions 'ORDER:READ'
    Assert-ControlContinues -Scenario $externalFailureScenario
    Enable-SessionDeletePermission

    $deleteFailureScenario = New-MutationScenario
    $deleteFailureTarget = $deleteFailureScenario.Targets[0]
    $logoutCsrfToken = Get-FixtureCsrfToken -Session $deleteFailureTarget.Session
    $copiedTargetSession = Copy-FixtureSession -Session $deleteFailureTarget.Session
    $encodedPasswordBefore = Invoke-Postgres `
        -Label 'Pre-failure password state observation' -Sql (
            "SELECT encoded_password FROM koiki_password_credential " +
            "WHERE user_id = '$($deleteFailureTarget.UserId)';")

    Disable-SessionDeletePermission

    $failedPassword = 'K9!' + [System.Guid]::NewGuid().ToString('N') + 'fR'
    Invoke-AdminMutationFailure -AdminSession $deleteFailureScenario.Admin.Session `
        -Path '/fixture/admin/password' `
        -Fields @{
            userId = $deleteFailureTarget.UserId
            password = $failedPassword
            expectedVersion = 0
        }
    $failedMutationState = Invoke-Postgres `
        -Label 'Failed mutation rollback observation' -Sql (
            "SELECT u.version || '|' || c.version || '|' || " +
            "(SELECT count(*) FROM koiki_audit_event " +
            "WHERE action = 'SET_LOCAL_PASSWORD') FROM koiki_user u " +
            "JOIN koiki_password_credential c ON c.user_id = u.user_id " +
            "WHERE u.user_id = '$($deleteFailureTarget.UserId)';")
    $encodedPasswordAfter = Invoke-Postgres `
        -Label 'Post-failure password state observation' -Sql (
            "SELECT encoded_password FROM koiki_password_credential " +
            "WHERE user_id = '$($deleteFailureTarget.UserId)';")
    if ($failedMutationState -ne '0|0|0' -or
        $encodedPasswordAfter -ne $encodedPasswordBefore) {
        throw 'Identity mutation or Business Audit escaped the Session invalidation rollback.'
    }
    Assert-NoMutationAudit -Action 'SET_LOCAL_PASSWORD'
    Invoke-AuthenticatedObservation -Client $deleteFailureTarget.Session.Client -Port $portB `
        -ExpectedUserId $deleteFailureTarget.UserId -ExpectedPermissions 'ORDER:READ'
    Assert-ControlContinues -Scenario $deleteFailureScenario

    Invoke-FixtureLogout -Session $deleteFailureTarget.Session `
        -CsrfToken $logoutCsrfToken -ExpectFailure
    Wait-OperatorFailureMarker
    $currentCookies = @($deleteFailureTarget.Session.Cookies.GetCookies(
            [Uri]"http://127.0.0.1:$portB/") | Where-Object Name -eq 'SESSION')
    if ($currentCookies.Count -ne 0) {
        throw 'The current client retained its Session Cookie after failed persistent logout.'
    }
    $retainedSessionRow = Invoke-Postgres `
        -Label 'Failed logout retained row observation' -Sql (
            "SELECT count(*) FROM koiki_session " +
            "WHERE principal_name = '$($deleteFailureTarget.UserId)';")
    if ($retainedSessionRow -ne '1') {
        throw 'The failed persistent logout did not retain its expected Session row.'
    }
    Invoke-AuthenticatedObservation -Client $copiedTargetSession.Client -Port $portB `
        -ExpectedUserId $deleteFailureTarget.UserId -ExpectedPermissions 'ORDER:READ'

    Enable-SessionDeletePermission
    $recoveryLogoutToken = Get-FixtureCsrfToken -Session $copiedTargetSession
    Invoke-FixtureLogout -Session $copiedTargetSession -CsrfToken $recoveryLogoutToken
    Assert-NoPrincipalSession -UserId $deleteFailureTarget.UserId

    $finalContinuity = New-MutationScenario
    $finalTarget = $finalContinuity.Targets[0]
    Invoke-AuthenticatedObservation -Client $finalTarget.Session.Client -Port $portB `
        -ExpectedUserId $finalTarget.UserId -ExpectedPermissions 'ORDER:READ'
    Stop-StartedProcess -Process $processA
    $processA.Refresh()
    if (-not $processA.HasExited) {
        throw 'Process A remained alive after the bounded stop.'
    }
    $processB.Refresh()
    if ($processB.HasExited) {
        throw 'Process B exited when process A was stopped.'
    }
    Invoke-AuthenticatedObservation -Client $finalTarget.Session.Client -Port $portB `
        -ExpectedUserId $finalTarget.UserId -ExpectedPermissions 'ORDER:READ'
    $continuedSessionRow = Invoke-Postgres -Label 'Continued Session row observation' -Sql (
        "SELECT count(*) FROM koiki_session WHERE principal_name = '$($finalTarget.UserId)';")
    if ($continuedSessionRow -ne '1') {
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
        $permissionRestoreFailure = $null
        if ($sessionDeleteRevoked -and $containerStarted) {
            try {
                Invoke-Postgres -Label 'Final Session DELETE permission restore' -Sql (
                    "GRANT DELETE ON public.koiki_session TO $appRole;") | Out-Null
                $sessionDeleteRevoked = $false
            } catch {
                $permissionRestoreFailure = $_
            }
        }
        foreach ($resource in $httpResources) {
            $resource.Client.Dispose()
            $resource.Handler.Dispose()
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
        if ($null -ne $permissionRestoreFailure) {
            throw 'The Session DELETE permission could not be restored during final cleanup.'
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
        'Phase 2 P2-B3 two-process store-failure slice succeeded: ' +
        'A/B continuity, five normal and five DELETE-failure Identity mutations, ' +
        'logout safe failure/recovery, control continuity, and B continuity after A stop.')
}

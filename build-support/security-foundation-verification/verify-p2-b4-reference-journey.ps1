[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$referencePom = Join-Path $repositoryRoot 'koiki-reference-app/pom.xml'
$referenceTarget = Join-Path $repositoryRoot 'koiki-reference-app/target'
$referenceJar = Join-Path $referenceTarget 'koiki-reference-app-0.1.0-SNAPSHOT.jar'
$identityInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-identity/public-api.txt'
$bomPom = Join-Path $repositoryRoot 'koiki-dependencies-bom/pom.xml'
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
    'koiki-reference-b44-' + [System.Guid]::NewGuid().ToString('N'))
$processMarker = [System.IO.Path]::GetFileName($verificationRoot)
$isolatedRepository = Join-Path $verificationRoot 'repository'
$mavenLog = Join-Path $verificationRoot 'maven-output.log'
$processLog = Join-Path $verificationRoot 'reference-process.log'
$processError = Join-Path $verificationRoot 'reference-process-error.log'
$containerName = 'koiki-b44-' + [System.Guid]::NewGuid().ToString('N')
$appRole = 'koiki_b44_app'
$databaseName = 'postgres'
$appPassword = [System.Guid]::NewGuid().ToString('N') + [System.Guid]::NewGuid().ToString('N')
$hmacKeyBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($hmacKeyBytes)
$hmacKey = [Convert]::ToBase64String($hmacKeyBytes)
$hmacKeyId = 'b44-' + [System.Guid]::NewGuid().ToString('N')
$containerStarted = $false
$auditInsertRevoked = $false
$sessionDeleteRevoked = $false
$referenceProcess = $null
$httpResources = [System.Collections.Generic.List[object]]::new()
$verificationSucceeded = $false
$runtimeSensitiveValues = [System.Collections.Generic.List[string]]::new()

function Add-RuntimeSensitiveValue {
    param([Parameter(Mandatory)][string]$Value)

    if (-not [string]::IsNullOrEmpty($Value)) {
        [void]$runtimeSensitiveValues.Add($Value)
    }
}

Add-RuntimeSensitiveValue -Value $appPassword
Add-RuntimeSensitiveValue -Value $hmacKey

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-reference-b44-*') {
        throw "Unexpected B4-4 temporary directory name: $resolved"
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
    if ($Content -match '(?i)authorization\s*:\s*(?:basic|bearer)\s+' -or
        $Content -match '(?i)set-cookie\s*:\s*SESSION=') {
        throw "HTTP credential material detected in $Source."
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
        throw "$Label failed during B4-4 verification."
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
        '--single-transaction', '--set', 'ON_ERROR_STOP=1',
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

function Start-ReferenceProcess {
    param(
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$DatabasePort
    )

    $startArguments = @{
        FilePath = $javaTool
        ArgumentList = @(
            "-Dkoiki.verification.process-marker=$processMarker",
            '-jar',
            $referenceJar)
        PassThru = $true
        RedirectStandardOutput = $processLog
        RedirectStandardError = $processError
        Environment = @{
            'SERVER_PORT' = [string]$Port
            'SERVER_SERVLET_SESSION_COOKIE_SECURE' = 'false'
            'SPRING_DATASOURCE_URL' =
                "jdbc:postgresql://127.0.0.1:$DatabasePort/$databaseName" +
                '?loggerLevel=OFF'
            'SPRING_DATASOURCE_USERNAME' = $appRole
            'SPRING_DATASOURCE_PASSWORD' = $appPassword
            'KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID' = $hmacKeyId
            'KOIKI_REFERENCE_SOURCE_HMAC_KEY' = $hmacKey
            'DEBUG' = 'false'
            'LOGGING_LEVEL_ORG_SPRINGFRAMEWORK' = 'INFO'
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
            if ($response.StatusCode -eq 200 -and $response.Content -match 'Please sign in') {
                return
            }
        } catch {
            Start-Sleep -Milliseconds 250
        }
    }
    throw 'The Reference process did not become ready within 60 seconds.'
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
            throw 'The Reference process did not stop within 10 seconds.'
        }
    }
}

function New-ReferenceClient {
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

function Get-CsrfToken {
    param(
        [Parameter(Mandatory)][object]$Session,
        [Parameter(Mandatory)][int]$Port
    )

    $response = $Session.Client.GetAsync(
        "http://127.0.0.1:$Port/login").GetAwaiter().GetResult()
    try {
        $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if ([int]$response.StatusCode -ne 200) {
            throw 'The standard Form Login page was not available for CSRF retrieval.'
        }
        $csrfInput = [regex]::Match($content, '<input[^>]+name="_csrf"[^>]*>')
        $csrfValue = if ($csrfInput.Success) {
            [regex]::Match(
                $csrfInput.Value, 'value="(?<value>[^"]+)"').Groups['value'].Value
        } else {
            ''
        }
        if ([string]::IsNullOrWhiteSpace($csrfValue)) {
            throw 'The standard Form Login CSRF token was not found.'
        }
        return [System.Net.WebUtility]::HtmlDecode($csrfValue)
    } finally {
        $response.Dispose()
    }
}

function Invoke-ReferenceLogin {
    param(
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$Email,
        [Parameter(Mandatory)][string]$Password
    )

    $resource = New-ReferenceClient
    $csrfToken = Get-CsrfToken -Session $resource -Port $Port
    $fields = [System.Collections.Generic.Dictionary[string, string]]::new()
    $fields.Add('username', $Email)
    $fields.Add('password', $Password)
    $fields.Add('_csrf', $csrfToken)
    $body = [System.Net.Http.FormUrlEncodedContent]::new($fields)
    try {
        $response = $resource.Client.PostAsync(
            "http://127.0.0.1:$Port/login", $body).GetAwaiter().GetResult()
        try {
            if ([int]$response.StatusCode -notin @(302, 303) -or
                $response.Headers.Location.ToString().Contains('/login?error')) {
                throw 'Standard Form Login did not establish an authenticated Session.'
            }
        } finally {
            $response.Dispose()
        }
    } finally {
        $body.Dispose()
    }
    $sessionCookies = @($resource.Cookies.GetCookies(
            [Uri]"http://127.0.0.1:$Port/") | Where-Object Name -eq 'SESSION')
    if ($sessionCookies.Count -ne 1) {
        throw 'Standard Form Login did not retain exactly one Session Cookie.'
    }
    return $resource
}

function Get-ReferenceResponse {
    param(
        [Parameter(Mandatory)][object]$Session,
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$Path
    )

    $response = $Session.Client.GetAsync(
        "http://127.0.0.1:$Port$Path").GetAwaiter().GetResult()
    try {
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Location = if ($null -eq $response.Headers.Location) {
                ''
            } else {
                $response.Headers.Location.ToString()
            }
            Content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        }
    } finally {
        $response.Dispose()
    }
}

function Invoke-RoleMutation {
    param(
        [Parameter(Mandatory)][object]$Session,
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][long]$ExpectedVersion,
        [string]$RoleCode
    )

    $csrfToken = Get-CsrfToken -Session $Session -Port $Port
    $fields = [System.Collections.Generic.Dictionary[string, string]]::new()
    $fields.Add('expectedVersion', [string]$ExpectedVersion)
    $fields.Add('_csrf', $csrfToken)
    if (-not [string]::IsNullOrWhiteSpace($RoleCode)) {
        $fields.Add('roleCode', $RoleCode)
    }
    $body = [System.Net.Http.FormUrlEncodedContent]::new($fields)
    try {
        $response = $Session.Client.PostAsync(
            "http://127.0.0.1:$Port$Path", $body).GetAwaiter().GetResult()
        try {
            return [pscustomobject]@{
                Status = [int]$response.StatusCode
                Location = if ($null -eq $response.Headers.Location) {
                    ''
                } else {
                    $response.Headers.Location.ToString()
                }
                Content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            }
        } finally {
            $response.Dispose()
        }
    } finally {
        $body.Dispose()
    }
}

function Assert-Status {
    param(
        [Parameter(Mandatory)][object]$Response,
        [Parameter(Mandatory)][int]$Expected,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Response.Status -ne $Expected) {
        throw "$Label returned HTTP $($Response.Status), expected $Expected."
    }
}

function Assert-RedirectedToLogin {
    param(
        [Parameter(Mandatory)][object]$Response,
        [Parameter(Mandatory)][string]$Label
    )

    if ($Response.Status -notin @(302, 303) -or
        -not $Response.Location.Contains('/login')) {
        throw "$Label retained access after Session invalidation."
    }
}

function New-FixtureIdentitySet {
    $adminId = [System.Guid]::NewGuid()
    $targetId = [System.Guid]::NewGuid()
    $controlId = [System.Guid]::NewGuid()
    $roleId = [System.Guid]::NewGuid()
    $permissionId = [System.Guid]::NewGuid()
    $adminPassword = 'A9!' + [System.Guid]::NewGuid().ToString('N') + 'z'
    $targetPassword = 'T9!' + [System.Guid]::NewGuid().ToString('N') + 'z'
    $controlPassword = 'C9!' + [System.Guid]::NewGuid().ToString('N') + 'z'
    $adminEmail = [System.Guid]::NewGuid().ToString('N') + '@example.invalid'
    $targetEmail = [System.Guid]::NewGuid().ToString('N') + '@example.invalid'
    $controlEmail = [System.Guid]::NewGuid().ToString('N') + '@example.invalid'
    foreach ($value in @(
            $adminPassword, $targetPassword, $controlPassword,
            $adminEmail, $targetEmail, $controlEmail)) {
        Add-RuntimeSensitiveValue -Value $value
    }

    $truncateSql =
        'TRUNCATE TABLE koiki_session_attributes, koiki_session, ' +
        'koiki_login_attempt, koiki_external_identity_link, koiki_password_credential, ' +
        'koiki_user_role, koiki_role_permission, koiki_permission, koiki_role, ' +
        'koiki_user, koiki_audit_event; '
    $userSql = @(
        "INSERT INTO koiki_user (user_id, email, canonical_email, status) VALUES ('$adminId', '$adminEmail', '$adminEmail', 'ACTIVE');",
        "INSERT INTO koiki_user (user_id, email, canonical_email, status) VALUES ('$targetId', '$targetEmail', '$targetEmail', 'ACTIVE');",
        "INSERT INTO koiki_user (user_id, email, canonical_email, status) VALUES ('$controlId', '$controlEmail', '$controlEmail', 'ACTIVE');",
        "INSERT INTO koiki_password_credential (user_id, encoded_password) VALUES ('$adminId', '{noop}$adminPassword');",
        "INSERT INTO koiki_password_credential (user_id, encoded_password) VALUES ('$targetId', '{noop}$targetPassword');",
        "INSERT INTO koiki_password_credential (user_id, encoded_password) VALUES ('$controlId', '{noop}$controlPassword');",
        "INSERT INTO koiki_role (role_id, role_code) VALUES ('$roleId', 'IDENTITY_ADMIN');",
        "INSERT INTO koiki_permission (permission_id, permission_code) VALUES ('$permissionId', 'IDENTITY:ADMIN');",
        "INSERT INTO koiki_role_permission (role_id, permission_id) VALUES ('$roleId', '$permissionId');",
        "INSERT INTO koiki_user_role (user_id, role_id) VALUES ('$adminId', '$roleId');"
    ) -join ' '
    Invoke-Postgres -Label 'Runtime Identity bootstrap' -Sql ($truncateSql + $userSql) | Out-Null

    return [pscustomobject]@{
        AdminId = $adminId
        AdminEmail = $adminEmail
        AdminPassword = $adminPassword
        TargetId = $targetId
        TargetEmail = $targetEmail
        TargetPassword = $targetPassword
        ControlId = $controlId
        ControlEmail = $controlEmail
        ControlPassword = $controlPassword
    }
}

function Assert-TargetState {
    param(
        [Parameter(Mandatory)][System.Guid]$TargetId,
        [Parameter(Mandatory)][long]$Version,
        [Parameter(Mandatory)][int]$RoleCount
    )

    $state = Invoke-Postgres -Label 'Target Identity state observation' -Sql (
        "SELECT u.version || '|' || count(ur.role_id) FROM koiki_user u " +
        "LEFT JOIN koiki_user_role ur ON ur.user_id = u.user_id " +
        "WHERE u.user_id = '$TargetId' GROUP BY u.version;")
    if ($state -ne "$Version|$RoleCount") {
        throw "The target Identity state was unexpected: $state."
    }
}

function Get-PrincipalSessionCount {
    param([Parameter(Mandatory)][System.Guid]$UserId)

    return [int](Invoke-Postgres -Label 'Principal Session observation' -Sql (
        "SELECT count(*) FROM koiki_session WHERE principal_name = '$UserId';"))
}

function Assert-MutationAuditCount {
    param(
        [Parameter(Mandatory)][System.Guid]$AdminId,
        [Parameter(Mandatory)][System.Guid]$TargetId,
        [Parameter(Mandatory)][int]$ExpectedCount
    )

    $count = Invoke-Postgres -Label 'Identity mutation Audit observation' -Sql (
        "SELECT count(*) FROM koiki_audit_event " +
        "WHERE event_type = 'IDENTITY_ADMINISTRATION' " +
        "AND actor_id = '$AdminId' AND subject_id = '$TargetId' " +
        "AND resource_type = 'ROLE' AND resource_id = 'IDENTITY_ADMIN' " +
        "AND action IN ('ASSIGN_ROLE', 'REVOKE_ROLE') AND result = 'SUCCESS';")
    if ([int]$count -ne $ExpectedCount) {
        throw "The successful mutation Audit count was unexpected: $count."
    }
}

function Disable-AuditInsertPermission {
    Invoke-Postgres -Label 'Business Audit INSERT permission revoke' -Sql (
        "REVOKE INSERT ON public.koiki_audit_event FROM $appRole;") | Out-Null
    $script:auditInsertRevoked = $true
}

function Enable-AuditInsertPermission {
    Invoke-Postgres -Label 'Business Audit INSERT permission restore' -Sql (
        "GRANT INSERT ON public.koiki_audit_event TO $appRole;") | Out-Null
    $script:auditInsertRevoked = $false
}

function Disable-SessionDeletePermission {
    Invoke-Postgres -Label 'Session DELETE permission revoke' -Sql (
        "REVOKE DELETE ON public.koiki_session FROM $appRole;") | Out-Null
    $script:sessionDeleteRevoked = $true
}

function Enable-SessionDeletePermission {
    Invoke-Postgres -Label 'Session DELETE permission restore' -Sql (
        "GRANT DELETE ON public.koiki_session TO $appRole;") | Out-Null
    $script:sessionDeleteRevoked = $false
}

function Assert-ReferenceArtifactBoundary {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($referenceJar)
    try {
        $entryNames = @($archive.Entries.FullName)
        if ($entryNames -match '^BOOT-INF/classes/db/migration/' -or
            $entryNames -match '^BOOT-INF/classes/.+\.sql$' -or
            $entryNames -match '^BOOT-INF/classes/org/koikifw/buildsupport/') {
            throw 'The Reference JAR contains migration SQL or Tooling fixture code.'
        }
        foreach ($requiredTemplate in @(
                'BOOT-INF/classes/templates/identity/users.html',
                'BOOT-INF/classes/templates/identity/user-detail.html',
                'BOOT-INF/classes/templates/identity/error.html')) {
            if ($entryNames -notcontains $requiredTemplate) {
                throw "The Reference JAR is missing $requiredTemplate."
            }
        }
    } finally {
        $archive.Dispose()
    }

      if ((Get-Content -Raw -LiteralPath $bomPom).Contains('koiki-reference-app')) {
          throw 'The Reference application was added to the Framework BOM.'
      }
      $referencePomContent = Get-Content -Raw -LiteralPath $referencePom
      foreach ($forbiddenDependency in @(
              'spring-boot-starter-webflux',
              'spring-boot-starter-data-redis',
              'spring-security-saml2',
              'software.amazon.awssdk',
              'com.oracle.database',
              'htmx',
              'react')) {
          if ($referencePomContent.Contains($forbiddenDependency)) {
              throw "The Reference POM contains deferred dependency $forbiddenDependency."
          }
      }
      $inventory = Get-Content -Raw -LiteralPath $identityInventory
    if ($inventory -notmatch '(?m)^PUBLIC_JAVA_TYPES 10$' -or
        @($inventory -split "`r?`n" | Where-Object { $_ -match '^TYPE ' }).Count -ne 10) {
        throw 'The Identity Public API type inventory changed during B4-4.'
    }
    $referenceSource = Get-ChildItem -LiteralPath (
        Join-Path $repositoryRoot 'koiki-reference-app/src/main') -Recurse -File
      foreach ($source in $referenceSource) {
          $content = Get-Content -Raw -LiteralPath $source.FullName
        if ($content -match 'org\.koikifw\.[a-z0-9_.]+\.internal(?:\.|;)' -or
            $content -match '(?m)^\s*(?:CREATE|ALTER|DROP)\s+TABLE\b') {
              throw "The Reference source crossed its Framework ownership boundary: $($source.Name)."
          }
      }
      $productionJava = @($referenceSource | Where-Object { $_.Extension -eq '.java' })
      $templates = @($referenceSource | Where-Object {
              $_.FullName -match '[\\/]resources[\\/]templates[\\/]' -and $_.Extension -eq '.html'
          })
      $sqlFiles = @($referenceSource | Where-Object { $_.Extension -eq '.sql' })
      if ($productionJava.Count -ne 12 -or $templates.Count -ne 3 -or $sqlFiles.Count -ne 0) {
          throw "Reference inventory changed: Java=$($productionJava.Count), templates=$($templates.Count), SQL=$($sqlFiles.Count)."
      }
      $controllerSource = Get-Content -Raw -LiteralPath (Join-Path $repositoryRoot (
          'koiki-reference-app/src/main/java/org/koikifw/reference/identity/adapter/inbound/web/IdentityManagementController.java'))
      if ([regex]::Matches($controllerSource, '@GetMapping').Count -ne 3 -or
          [regex]::Matches($controllerSource, '@PostMapping').Count -ne 2) {
          throw 'The approved Reference HTTP mapping inventory changed.'
      }
      $allReferenceSource = ($referenceSource | ForEach-Object {
              Get-Content -Raw -LiteralPath $_.FullName
          }) -join "`n"
      if ($allReferenceSource -match '@Transactional' -or
          $allReferenceSource -match '(?m)^\s*(?:public\s+)?interface\s+\w*Repository\b') {
          throw 'The Reference application added transaction ownership or a Repository declaration.'
      }
  }

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    if (-not (Test-Path -LiteralPath $javaTool -PathType Leaf)) {
        throw 'Java 21 was not found through JAVA_HOME.'
    }
    Invoke-Docker -Label 'Docker Engine check' `
        -Arguments @('version', '--format', '{{.Server.Version}}') | Out-Null

    Invoke-KoikiMaven -Label 'Stage formal Framework release unit' -Arguments @(
        '-f', $rootPom, '-pl', '!koiki-reference-app',
        'clean', 'install', '-DskipTests')
    Invoke-KoikiMaven -Label 'Package Reference application' -Arguments @(
        '-f', $referencePom, 'clean', 'package')
    if (-not (Test-Path -LiteralPath $referenceJar -PathType Leaf)) {
        throw 'The package-ready Reference JAR was not created.'
    }
    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
            'org/koikifw/koiki-reference-app'))) {
        throw 'The Reference artifact entered the formal Framework release repository.'
    }
    Assert-ReferenceArtifactBoundary

    Invoke-Docker -Label 'PostgreSQL container start' -Arguments @(
        'run', '--detach', '--rm', '--name', $containerName,
        '--publish', '127.0.0.1::5432',
        '--env', "POSTGRES_PASSWORD=$([System.Guid]::NewGuid().ToString('N'))",
        'postgres:17-alpine') | Out-Null
    $containerStarted = $true

    $databaseDeadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
    $databaseReady = $false
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

    $auditSchema = @'
CREATE TABLE koiki_audit_event (
    event_id uuid PRIMARY KEY,
    audit_type varchar(16) NOT NULL,
    event_type varchar(128) NOT NULL,
    actor_type varchar(16) NOT NULL,
    actor_id varchar(255),
    subject_id varchar(255),
    resource_type varchar(128),
    resource_id varchar(255),
    action varchar(128) NOT NULL,
    result varchar(16) NOT NULL,
    reason_code varchar(128),
    occurred_at timestamp with time zone NOT NULL,
    request_id varchar(128),
    trace_id varchar(128)
);
'@
    $setupDeadline = [DateTimeOffset]::UtcNow.AddSeconds(30)
    while ($true) {
        try {
            Invoke-Postgres -Label 'Application role and Tooling Audit schema setup' -Sql (
                "CREATE ROLE $appRole LOGIN PASSWORD '$appPassword'; " +
                "GRANT CONNECT ON DATABASE $databaseName TO $appRole; " +
                "GRANT USAGE, CREATE ON SCHEMA public TO $appRole; " + $auditSchema) | Out-Null
            break
        } catch {
            if ([DateTimeOffset]::UtcNow -ge $setupDeadline) {
                throw
            }
            # The official image briefly exposes its temporary initialization server
            # before restarting PostgreSQL. The single transaction makes this retry safe.
            Start-Sleep -Milliseconds 250
        }
    }

    $port = New-LoopbackPort
    $referenceProcess = Start-ReferenceProcess -Port $port -DatabasePort $databasePort
    Wait-ReferenceReady -Process $referenceProcess -Port $port

    Invoke-Postgres -Label 'PostgreSQL non-owner DML boundary' -Sql (
        "REASSIGN OWNED BY $appRole TO postgres; " +
        "REVOKE CREATE ON SCHEMA public FROM $appRole; " +
        "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO $appRole; " +
        "GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO $appRole;") | Out-Null
    $ownershipBoundary = Invoke-Postgres -Label 'PostgreSQL ownership observation' -Sql (
        "SELECT count(*) || '|' || " +
        "has_schema_privilege('$appRole', 'public', 'CREATE') || '|' || " +
        "has_table_privilege('$appRole', 'public.koiki_session', 'DELETE') " +
        "FROM pg_class object JOIN pg_namespace namespace " +
        "ON namespace.oid = object.relnamespace " +
        "WHERE namespace.nspname = 'public' " +
        "AND pg_get_userbyid(object.relowner) = '$appRole';")
    if ($ownershipBoundary -ne '0|false|true') {
        throw 'The Reference DB role did not retain the required non-owner DML boundary.'
    }

    # AC-P2-01 / AC-P2-02 normal journey.
    $normal = New-FixtureIdentitySet
    $admin = Invoke-ReferenceLogin -Port $port -Email $normal.AdminEmail `
        -Password $normal.AdminPassword
    $control = Invoke-ReferenceLogin -Port $port -Email $normal.ControlEmail `
        -Password $normal.ControlPassword
    $targetOne = Invoke-ReferenceLogin -Port $port -Email $normal.TargetEmail `
        -Password $normal.TargetPassword
    $targetTwo = Invoke-ReferenceLogin -Port $port -Email $normal.TargetEmail `
        -Password $normal.TargetPassword

    Assert-Status -Response (Get-ReferenceResponse -Session $admin -Port $port `
            -Path '/identity/users') -Expected 200 -Label 'Admin protected GET'
    Assert-Status -Response (Get-ReferenceResponse -Session $control -Port $port `
            -Path '/identity/users') -Expected 403 -Label 'Control protected GET'
    Assert-Status -Response (Get-ReferenceResponse -Session $targetOne -Port $port `
            -Path '/identity/users') -Expected 403 -Label 'Target protected GET before Role assignment'
    Assert-TargetState -TargetId $normal.TargetId -Version 0 -RoleCount 0
    if ((Get-PrincipalSessionCount -UserId $normal.TargetId) -ne 2) {
        throw 'Two target Sessions were not established before Role assignment.'
    }

    $controlDirect = Invoke-RoleMutation -Session $control -Port $port `
        -Path "/identity/users/$($normal.TargetId)/roles" `
        -ExpectedVersion 0 -RoleCode 'IDENTITY_ADMIN'
    Assert-Status -Response $controlDirect -Expected 403 `
        -Label 'Permission-deficient direct Role POST with valid CSRF'
    Assert-TargetState -TargetId $normal.TargetId -Version 0 -RoleCount 0
    Assert-MutationAuditCount -AdminId $normal.AdminId -TargetId $normal.TargetId `
        -ExpectedCount 0
    if ((Get-PrincipalSessionCount -UserId $normal.TargetId) -ne 2) {
        throw 'The rejected direct request changed target Sessions.'
    }

    $assign = Invoke-RoleMutation -Session $admin -Port $port `
        -Path "/identity/users/$($normal.TargetId)/roles" `
        -ExpectedVersion 0 -RoleCode 'IDENTITY_ADMIN'
    Assert-Status -Response $assign -Expected 302 -Label 'Admin Role assignment'
    Assert-TargetState -TargetId $normal.TargetId -Version 1 -RoleCount 1
    Assert-MutationAuditCount -AdminId $normal.AdminId -TargetId $normal.TargetId `
        -ExpectedCount 1
    if ((Get-PrincipalSessionCount -UserId $normal.TargetId) -ne 0) {
        throw 'Role assignment did not invalidate all pre-existing target Sessions.'
    }
    Assert-RedirectedToLogin -Response (Get-ReferenceResponse -Session $targetOne `
            -Port $port -Path '/identity/users') -Label 'First old target Session'
    Assert-RedirectedToLogin -Response (Get-ReferenceResponse -Session $targetTwo `
            -Port $port -Path '/identity/users') -Label 'Second old target Session'
    Assert-Status -Response (Get-ReferenceResponse -Session $admin -Port $port `
            -Path '/identity/users') -Expected 200 -Label 'Admin Session after target assignment'
    Assert-Status -Response (Get-ReferenceResponse -Session $control -Port $port `
            -Path '/identity/users') -Expected 403 -Label 'Control Session after target assignment'

    $targetAfterAssignment = Invoke-ReferenceLogin -Port $port `
        -Email $normal.TargetEmail -Password $normal.TargetPassword
    Assert-Status -Response (Get-ReferenceResponse -Session $targetAfterAssignment `
            -Port $port -Path '/identity/users') -Expected 200 `
        -Label 'Target authorization after Role assignment and re-login'

    $revoke = Invoke-RoleMutation -Session $admin -Port $port `
        -Path "/identity/users/$($normal.TargetId)/roles/IDENTITY_ADMIN/revoke" `
        -ExpectedVersion 1
    Assert-Status -Response $revoke -Expected 302 -Label 'Admin Role revocation'
    Assert-TargetState -TargetId $normal.TargetId -Version 2 -RoleCount 0
    Assert-MutationAuditCount -AdminId $normal.AdminId -TargetId $normal.TargetId `
        -ExpectedCount 2
    if ((Get-PrincipalSessionCount -UserId $normal.TargetId) -ne 0) {
        throw 'Role revocation did not invalidate the target Session.'
    }
    Assert-RedirectedToLogin -Response (Get-ReferenceResponse `
            -Session $targetAfterAssignment -Port $port -Path '/identity/users') `
        -Label 'Target Session after Role revocation'
    Assert-Status -Response (Get-ReferenceResponse -Session $admin -Port $port `
            -Path '/identity/users') -Expected 200 -Label 'Admin Session after target revocation'
    Assert-Status -Response (Get-ReferenceResponse -Session $control -Port $port `
            -Path '/identity/users') -Expected 403 -Label 'Control Session after target revocation'
    $auditPiiCount = Invoke-Postgres -Label 'Audit PII boundary observation' -Sql (
        "SELECT count(*) FROM koiki_audit_event WHERE " +
        "coalesce(actor_id, '') LIKE '%@%' OR coalesce(subject_id, '') LIKE '%@%' OR " +
        "coalesce(resource_id, '') LIKE '%@%' OR coalesce(reason_code, '') LIKE '%@%';")
    if ([int]$auditPiiCount -ne 0) {
        throw 'An Audit identifier column contained email-shaped PII.'
    }

    # Business Audit failure must roll back Role mutation and retain target Session.
    $auditFailure = New-FixtureIdentitySet
    $auditFailureAdmin = Invoke-ReferenceLogin -Port $port `
        -Email $auditFailure.AdminEmail -Password $auditFailure.AdminPassword
    $auditFailureTarget = Invoke-ReferenceLogin -Port $port `
        -Email $auditFailure.TargetEmail -Password $auditFailure.TargetPassword
    Disable-AuditInsertPermission
    try {
        $failedByAudit = Invoke-RoleMutation -Session $auditFailureAdmin -Port $port `
            -Path "/identity/users/$($auditFailure.TargetId)/roles" `
            -ExpectedVersion 0 -RoleCode 'IDENTITY_ADMIN'
        Assert-Status -Response $failedByAudit -Expected 503 `
            -Label 'Business Audit failure Role assignment'
        if ($failedByAudit.Content -notmatch 'temporarily unavailable' -or
            $failedByAudit.Content -match [regex]::Escape($auditFailure.TargetEmail)) {
            throw 'The Business Audit failure response was not sanitized.'
        }
    } finally {
        Enable-AuditInsertPermission
    }
    Assert-TargetState -TargetId $auditFailure.TargetId -Version 0 -RoleCount 0
    Assert-MutationAuditCount -AdminId $auditFailure.AdminId `
        -TargetId $auditFailure.TargetId -ExpectedCount 0
    if ((Get-PrincipalSessionCount -UserId $auditFailure.TargetId) -ne 1) {
        throw 'Business Audit failure did not preserve the target Session.'
    }

    # Session invalidation failure must roll back Role mutation and Business Audit.
    $sessionFailure = New-FixtureIdentitySet
    $sessionFailureAdmin = Invoke-ReferenceLogin -Port $port `
        -Email $sessionFailure.AdminEmail -Password $sessionFailure.AdminPassword
    $sessionFailureTarget = Invoke-ReferenceLogin -Port $port `
        -Email $sessionFailure.TargetEmail -Password $sessionFailure.TargetPassword
    Disable-SessionDeletePermission
    try {
        $failedBySession = Invoke-RoleMutation -Session $sessionFailureAdmin -Port $port `
            -Path "/identity/users/$($sessionFailure.TargetId)/roles" `
            -ExpectedVersion 0 -RoleCode 'IDENTITY_ADMIN'
        Assert-Status -Response $failedBySession -Expected 503 `
            -Label 'Session invalidation failure Role assignment'
        if ($failedBySession.Content -notmatch 'temporarily unavailable' -or
            $failedBySession.Content -match [regex]::Escape($sessionFailure.TargetEmail)) {
            throw 'The Session failure response was not sanitized.'
        }
    } finally {
        Enable-SessionDeletePermission
    }
    Assert-TargetState -TargetId $sessionFailure.TargetId -Version 0 -RoleCount 0
    Assert-MutationAuditCount -AdminId $sessionFailure.AdminId `
        -TargetId $sessionFailure.TargetId -ExpectedCount 0
    if ((Get-PrincipalSessionCount -UserId $sessionFailure.TargetId) -ne 1) {
        throw 'Session invalidation failure did not preserve the target Session.'
    }

    $referenceProcess.Refresh()
    if ($referenceProcess.HasExited) {
        throw 'The packaged Reference process did not survive the bounded failure journeys.'
    }
    $verificationSucceeded = $true
} finally {
    try {
        $restoreFailure = $null
        if ($auditInsertRevoked -and $containerStarted) {
            try {
                Invoke-Postgres -Label 'Final Audit INSERT permission restore' -Sql (
                    "GRANT INSERT ON public.koiki_audit_event TO $appRole;") | Out-Null
                $auditInsertRevoked = $false
            } catch {
                $restoreFailure = $_
            }
        }
        if ($sessionDeleteRevoked -and $containerStarted) {
            try {
                Invoke-Postgres -Label 'Final Session DELETE permission restore' -Sql (
                    "GRANT DELETE ON public.koiki_session TO $appRole;") | Out-Null
                $sessionDeleteRevoked = $false
            } catch {
                $restoreFailure = $_
            }
        }
        foreach ($resource in $httpResources) {
            $resource.Client.Dispose()
            $resource.Handler.Dispose()
        }
        Stop-StartedProcess -Process $referenceProcess
        if ($containerStarted) {
            if ($containerName -notlike 'koiki-b44-*') {
                throw 'Refusing to remove an unexpected container.'
            }
            & docker inspect $containerName 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                & docker rm --force $containerName 2>&1 | Out-Null
                if ($LASTEXITCODE -ne 0) {
                    throw 'The B4-4 PostgreSQL container could not be removed.'
                }
            }
            & docker inspect $containerName 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                throw 'The B4-4 PostgreSQL container remained after cleanup.'
            }
        }

        foreach ($logPath in @($mavenLog, $processLog, $processError)) {
            if (Test-Path -LiteralPath $logPath) {
                Assert-NoSensitiveText -Content (Get-Content -Raw -LiteralPath $logPath) `
                    -Source ([System.IO.Path]::GetFileName($logPath))
            }
        }
        if ($null -ne $restoreFailure) {
            throw 'A bounded PostgreSQL permission could not be restored during cleanup.'
        }
    } finally {
        if (Test-Path -LiteralPath $verificationRoot) {
            Assert-SafeTemporaryPath -Path $verificationRoot
            Remove-Item -LiteralPath $verificationRoot -Recurse -Force
        }
        if (Test-Path -LiteralPath $verificationRoot) {
            throw 'The B4-4 temporary directory remained after cleanup.'
        }
    }
}

if ($verificationSucceeded) {
    Write-Host (
        'Phase 2 P2-B4 B4-4 Reference journey succeeded: AC-P2-01/02, ' +
        'DoD 2-10, two target Session invalidation, admin/control continuity, ' +
        'Business Audit and Session failure rollback, artifact boundaries, ' +
        'sensitive-output checks, and owned-resource cleanup.')
}

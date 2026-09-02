[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/security-foundation-consumer'
$consumerPom = Join-Path $consumerRoot 'pom.xml'
$consumerSource = Join-Path $consumerRoot 'src'
$consumerTarget = Join-Path $consumerRoot 'target'
$a3Verifier = Join-Path $PSScriptRoot 'verify-p2-a3-oidc-bearer.ps1'
$publicApiFixtureVerifier = Join-Path $repositoryRoot (
    'build-support/api-compatibility/verify-public-api-fixtures.ps1')
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-security-gate-a-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$consumerDependencyTree = Join-Path $verificationRoot 'consumer-dependency-tree.txt'

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    $temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if (-not $resolvedPath.StartsWith($temporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the temporary directory: $resolvedPath"
    }
}

function Invoke-KoikiMaven {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host "=== $Label ==="
    & $wrapper --batch-mode --no-transfer-progress "-Dmaven.repo.local=$isolatedRepository" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
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
    if ($LASTEXITCODE -ne 0) {
        throw "Java $Feature runtime inspection failed."
    }
    $specification = $versionOutput | Select-String -Pattern (
        '^\s*java[.]specification[.]version\s*=\s*' + $Feature + '\s*$')
    if ($null -eq $specification) {
        throw "JAVA${Feature}_HOME does not point to a Java $Feature runtime."
    }
    return $javaCommand
}

function Invoke-ConsumerRuntime {
    param(
        [Parameter(Mandatory)][ValidateSet(21, 25)][int]$Feature,
        [Parameter(Mandatory)][string]$JarPath
    )

    $javaCommand = Get-JavaRuntime -Feature $Feature
    Write-Host "=== Execute the packaged Consumer on Java $Feature ==="
    $runtimeArguments = @(
        '-jar',
        $JarPath,
        "--koiki.consumer.runtime-probe=$Feature",
        '--debug=false',
        '--spring.main.banner-mode=off',
        '--logging.level.root=WARN')
    $output = @(& $javaCommand @runtimeArguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "Packaged Consumer failed on Java $Feature with exit code $exitCode."
    }
    $expectedMarker = "KOIKI_SECURITY_CONSUMER_RUNTIME_SUCCESS expected=$Feature actual=$Feature"
    if (-not (($output -join [Environment]::NewLine).Contains(
            $expectedMarker, [System.StringComparison]::Ordinal))) {
        throw "Java $Feature runtime output did not contain the required marker."
    }
}

function Assert-NoSensitiveContent {
    param([Parameter(Mandatory)][System.IO.FileInfo[]]$Files)

    $forbiddenPatterns = [ordered]@{
        'private key material' = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
        'credential assignment' = '(?i)(?:password|client[_-]?secret|access[_-]?token)\s*[:=]\s*[^\s<]+'
        'email-shaped PII' = '(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}'
    }
    foreach ($file in $Files) {
        $content = [System.Text.Encoding]::Latin1.GetString(
            [System.IO.File]::ReadAllBytes($file.FullName))
        foreach ($entry in $forbiddenPatterns.GetEnumerator()) {
            if ($content -match $entry.Value) {
                throw "Sensitive content detected in $($file.FullName): $($entry.Key)"
            }
        }
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Write-Host '=== Verify cumulative P2-A3 Security acceptance ==='
    & $a3Verifier
    if ($LASTEXITCODE -ne 0) {
        throw "P2-A3 aggregate failed with exit code $LASTEXITCODE"
    }

    $rootModel = Get-Content -Raw -LiteralPath $rootPom
    if ($rootModel.Contains('security-foundation-consumer', [System.StringComparison]::Ordinal)) {
        throw 'The non-distributed Security Consumer must remain outside the Root Reactor.'
    }
    $internalReferences = @(Get-ChildItem -LiteralPath $consumerSource -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal')
    if ($internalReferences.Count -ne 0) {
        throw 'The Customer-like Security Consumer references a KOIKI internal package.'
    }

    Invoke-KoikiMaven -Label 'Stage the formal KOIKI release unit for Gate A Consumer' -Arguments @(
        '-f', $rootPom, 'install', '-DskipTests')
    Invoke-KoikiMaven -Label 'Build and test the Root Reactor external Security Consumer' -Arguments @(
        '-f', $consumerPom, 'clean', 'package')
    Invoke-KoikiMaven -Label 'Record the Security Consumer runtime dependency tree' -Arguments @(
        '-f', $consumerPom, 'dependency:tree', '-Dscope=runtime',
        "-DoutputFile=$consumerDependencyTree")

    $consumerDependencies = Get-Content -Raw -LiteralPath $consumerDependencyTree
    foreach ($requiredDependency in @(
        'org.koikifw:koiki-starter-security:jar:',
        'org.springframework.boot:spring-boot-starter-security-oauth2-client:jar:',
        'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server:jar:')) {
        if ($consumerDependencies -notmatch [regex]::Escape($requiredDependency)) {
            throw "Required Consumer runtime dependency is missing: $requiredDependency"
        }
    }
    foreach ($forbiddenDependency in @(
        'spring-boot-starter-security-oauth2-authorization-server',
        'spring-security-saml2',
        'spring-session-data-redis',
        'spring-boot-starter-webflux',
        'io.projectreactor')) {
        if ($consumerDependencies -match [regex]::Escape($forbiddenDependency)) {
            throw "Deferred dependency leaked into the Gate A Consumer: $forbiddenDependency"
        }
    }

    $consumerJar = Get-Item -LiteralPath (Join-Path $consumerTarget (
        'security-foundation-consumer-0.1.0-SNAPSHOT.jar'))
    $consumerReports = @(Get-ChildItem -LiteralPath (Join-Path $consumerTarget 'surefire-reports') -File)
    $consumerInputs = @(
        Get-Item -LiteralPath $consumerPom
        Get-Item -LiteralPath (Join-Path $consumerRoot 'README.md')
        Get-ChildItem -LiteralPath $consumerSource -Recurse -File
    )
    Assert-NoSensitiveContent -Files ($consumerInputs + @($consumerJar) + $consumerReports)

    $hashBefore = (Get-FileHash -LiteralPath $consumerJar.FullName -Algorithm SHA256).Hash
    Invoke-ConsumerRuntime -Feature 21 -JarPath $consumerJar.FullName
    $hashAfter21 = (Get-FileHash -LiteralPath $consumerJar.FullName -Algorithm SHA256).Hash
    Invoke-ConsumerRuntime -Feature 25 -JarPath $consumerJar.FullName
    $hashAfter25 = (Get-FileHash -LiteralPath $consumerJar.FullName -Algorithm SHA256).Hash
    if ($hashBefore -cne $hashAfter21 -or $hashBefore -cne $hashAfter25) {
        throw 'The packaged Consumer JAR changed during Java 21 / 25 runtime verification.'
    }
    Write-Host "Packaged Consumer SHA-256 remained unchanged: $hashBefore"

    Write-Host '=== Verify Public API positive and negative fixtures ==='
    & $publicApiFixtureVerifier

    Invoke-KoikiMaven -Label 'Verify the Root Reactor for Gate A' -Arguments @(
        '-f', $rootPom, 'verify')

    Write-Host 'Phase 2 Gate A local Security aggregate succeeded.'
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

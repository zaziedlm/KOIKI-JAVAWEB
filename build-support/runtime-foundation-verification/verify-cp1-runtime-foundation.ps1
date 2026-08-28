[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$verificationPom = Join-Path $PSScriptRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/runtime-foundation-consumer'
$consumerPom = Join-Path $consumerRoot 'pom.xml'
$consumerSource = Join-Path $consumerRoot 'application/src/main/java'
$consumerJar = Join-Path $consumerRoot 'application/target/runtime-foundation-consumer-application-0.1.0-SNAPSHOT.jar'
$starterInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-api/public-api.txt'
$javaCommand = if ($IsWindows) {
    Join-Path $env:JAVA_HOME 'bin/java.exe'
} else {
    Join-Path $env:JAVA_HOME 'bin/java'
}

$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp1-" + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$dependencyTree = Join-Path $verificationRoot 'consumer-runtime-dependencies.txt'
$applicationOutput = Join-Path $verificationRoot 'consumer-stdout.log'
$applicationError = Join-Path $verificationRoot 'consumer-stderr.log'

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside the OS temporary directory: $resolved"
    }
    if ([System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp1-*') {
        throw "Unexpected CP1 temporary directory name: $resolved"
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

function Read-LogFile {
    param([Parameter(Mandatory)][string]$Path)

    if (Test-Path -LiteralPath $Path) {
        return Get-Content -Raw -LiteralPath $Path
    }
    return ''
}

function Find-KoikiInternalReferences {
    param([Parameter(Mandatory)][string]$SourceRoot)

    return Get-ChildItem -LiteralPath $SourceRoot -Recurse -Filter '*.java' |
        Select-String -Pattern 'org[.]koikifw[.].*[.]internal'
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage KOIKI release unit into isolated repository' -Arguments @(
        '-f', $rootPom, 'clean', 'install'
    )

    $stagedStarterJar = Join-Path $isolatedRepository 'org/koikifw/koiki-starter-api/0.1.0-SNAPSHOT/koiki-starter-api-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $stagedStarterJar -PathType Leaf)) {
        throw "Staged API Starter JAR was not found: $stagedStarterJar"
    }
    $starterArchive = [System.IO.Compression.ZipFile]::OpenRead($stagedStarterJar)
    try {
        $publicTypeCount = @($starterArchive.Entries | Where-Object FullName -Like '*.class').Count
    } finally {
        $starterArchive.Dispose()
    }
    if ($publicTypeCount -ne 0) {
        throw "CP1 API Starter must not introduce Java API types; found $publicTypeCount class entries."
    }
    if ((Get-Content -Raw -LiteralPath $starterInventory) -notmatch 'PUBLIC_TYPES 0') {
        throw 'CP1 API Starter Public API inventory does not declare PUBLIC_TYPES 0.'
    }

    Invoke-KoikiMaven -Label 'Verify fine-grained API starter fixture' -Arguments @(
        '-f', $verificationPom, 'clean', 'verify'
    )

    $negativeSourceRoot = Join-Path $verificationRoot 'negative-internal-reference'
    New-Item -ItemType Directory -Path $negativeSourceRoot -Force | Out-Null
    $negativeSource = Join-Path $negativeSourceRoot 'InternalReferenceProbe.java'
    Set-Content -LiteralPath $negativeSource -Encoding utf8 -Value @'
package org.example;
import org.koikifw.sample.internal.FrameworkSecret;
final class InternalReferenceProbe {}
'@
    if (-not (Find-KoikiInternalReferences -SourceRoot $negativeSourceRoot)) {
        throw 'The KOIKI internal package negative guard did not detect its probe.'
    }

    $internalReferences = Find-KoikiInternalReferences -SourceRoot $consumerSource
    if ($internalReferences) {
        throw 'Customer-like Consumer must not reference KOIKI internal packages.'
    }

    Invoke-KoikiMaven -Label 'Build independent Customer-like Runtime Consumer' -Arguments @(
        '-f', $consumerPom, 'clean', 'verify'
    )

    Invoke-KoikiMaven -Label 'Inspect Consumer runtime dependency boundary' -Arguments @(
        '-f', $consumerPom,
        '-pl', 'application', '-am',
        'dependency:tree',
        '-Dscope=runtime',
        "-DoutputFile=$dependencyTree"
    )

    $runtimeDependencies = Get-Content -Raw -LiteralPath $dependencyTree
    foreach ($required in @(
        'org.koikifw:koiki-starter-api',
        'org.springframework:spring-webmvc',
        'org.hibernate.validator:hibernate-validator'
    )) {
        if ($runtimeDependencies -notmatch [regex]::Escape($required)) {
            throw "Required runtime dependency is missing: $required"
        }
    }
    foreach ($forbidden in @(
        'org.springframework:spring-webflux',
        'io.projectreactor:reactor-core',
        'org.springframework.security',
        'org.springframework.data:spring-data-jpa',
        'org.springframework.modulith'
    )) {
        if ($runtimeDependencies -match [regex]::Escape($forbidden)) {
            throw "Deferred or reactive runtime dependency was found: $forbidden"
        }
    }

    if (-not (Test-Path -LiteralPath $consumerJar -PathType Leaf)) {
        throw "Executable Consumer JAR was not created: $consumerJar"
    }

    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    $listener.Stop()

    $startArguments = @{
        FilePath = $javaCommand
        ArgumentList = @('-jar', $consumerJar, "--server.port=$port", '--spring.main.banner-mode=off')
        PassThru = $true
        RedirectStandardOutput = $applicationOutput
        RedirectStandardError = $applicationError
    }
    if ($IsWindows) {
        $startArguments.WindowStyle = 'Hidden'
    }

    Write-Host '=== Start executable Customer-like Runtime Consumer ==='
    $consumerProcess = Start-Process @startArguments
    try {
        $ready = $false
        $deadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
        while ([DateTimeOffset]::UtcNow -lt $deadline) {
            if ($consumerProcess.HasExited) {
                $stdout = Read-LogFile -Path $applicationOutput
                $stderr = Read-LogFile -Path $applicationError
                throw "Consumer exited before accepting HTTP. stdout:`n$stdout`nstderr:`n$stderr"
            }
            try {
                $response = Invoke-WebRequest -Uri "http://127.0.0.1:$port/" -SkipHttpErrorCheck -TimeoutSec 2
                if ($response.StatusCode -eq 404) {
                    $ready = $true
                    break
                }
            } catch {
                Start-Sleep -Milliseconds 250
            }
        }
        if (-not $ready) {
            throw 'Consumer did not accept HTTP within 60 seconds.'
        }
    } finally {
        if (-not $consumerProcess.HasExited) {
            Stop-Process -Id $consumerProcess.Id
            $consumerProcess.WaitForExit(10000) | Out-Null
        }
    }

    $startupOutput = Read-LogFile -Path $applicationOutput
    if ($startupOutput -notmatch 'Started RuntimeFoundationConsumerApplication') {
        throw "Spring Boot startup marker was not found. stdout:`n$startupOutput"
    }

    Write-Host 'CP1 runtime artifact staging, fixture, independent Consumer build, architecture checks, and executable startup succeeded.'
} finally {
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

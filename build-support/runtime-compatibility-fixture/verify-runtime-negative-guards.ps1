[CmdletBinding()]
param(
    [string]$Java21Home = $env:JAVA21_HOME,

    [string]$Java25Home = $env:JAVA25_HOME,

    [string]$ArtifactDirectory = (Join-Path $PSScriptRoot 'target/runtime-artifact')
)

$ErrorActionPreference = 'Stop'

$buildScript = Join-Path $PSScriptRoot 'build-runtime-fixture.ps1'
$runtimeScript = Join-Path $PSScriptRoot 'verify-runtime-fixture.ps1'
$manifestPath = Join-Path $ArtifactDirectory 'runtime-compatibility-manifest.json'
$successMarker = 'KOIKI_RUNTIME_COMPATIBILITY_SUCCESS'
$pwshCommand = (Get-Command pwsh -ErrorAction Stop).Source

function Get-JavaExecutable {
    param(
        [Parameter(Mandatory)][string]$JavaHome,
        [Parameter(Mandatory)][int]$ExpectedFeature
    )

    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        throw "Java $ExpectedFeature home is not configured."
    }

    $executable = if ($IsWindows) { 'java.exe' } else { 'java' }
    $candidate = Join-Path $JavaHome "bin/$executable"
    if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw "Java $ExpectedFeature executable was not found: $candidate"
    }

    $versionOutput = @(& $candidate '-XshowSettings:properties' '-version' 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to inspect Java $ExpectedFeature runtime: $candidate"
    }
    $specificationLine = $versionOutput | Where-Object {
        $_ -match '^\s*java\.specification\.version\s*=\s*(\d+)\s*$'
    } | Select-Object -First 1
    if ($null -eq $specificationLine -or $specificationLine -notmatch '(\d+)\s*$' -or
        [int]$Matches[1] -ne $ExpectedFeature) {
        throw "Configured Java home does not provide Java ${ExpectedFeature}: $JavaHome"
    }

    return $candidate
}

function Invoke-CapturedCommand {
    param(
        [Parameter(Mandatory)][string]$Command,
        [Parameter(Mandatory)][object[]]$Arguments
    )

    $output = @(& $Command @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }

    [pscustomobject]@{
        ExitCode = $exitCode
        Text = $output -join [Environment]::NewLine
    }
}

function Assert-ExpectedFailure {
    param(
        [Parameter(Mandatory)]$Result,
        [Parameter(Mandatory)][string]$RequiredDiagnostic,
        [int]$ExpectedExitCode = -1
    )

    if ($Result.ExitCode -eq 0) {
        throw "Negative guard unexpectedly succeeded: $RequiredDiagnostic"
    }
    if ($ExpectedExitCode -ge 0 -and $Result.ExitCode -ne $ExpectedExitCode) {
        throw "Negative guard returned exit code $($Result.ExitCode), expected $ExpectedExitCode."
    }
    if (-not $Result.Text.Contains($RequiredDiagnostic, [System.StringComparison]::Ordinal)) {
        throw "Negative guard did not contain the required diagnostic: $RequiredDiagnostic"
    }
    if ($Result.Text.Contains($successMarker, [System.StringComparison]::Ordinal)) {
        throw "Negative guard emitted the success marker: $RequiredDiagnostic"
    }
}

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Build manifest was not found. Run build-runtime-fixture.ps1 first: $manifestPath"
}
$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding utf8 | ConvertFrom-Json
$originalJar = Join-Path $ArtifactDirectory ([string]$manifest.jarName)
if (-not (Test-Path -LiteralPath $originalJar -PathType Leaf)) {
    throw "Runtime fixture JAR was not found: $originalJar"
}
$originalHash = (Get-FileHash -LiteralPath $originalJar -Algorithm SHA256).Hash
if ($originalHash -cne [string]$manifest.sha256) {
    throw 'Original runtime fixture JAR does not match the build manifest.'
}

$java21Command = Get-JavaExecutable -JavaHome $Java21Home -ExpectedFeature 21
$null = Get-JavaExecutable -JavaHome $Java25Home -ExpectedFeature 25

$systemTempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$tempPrefix = $systemTempRoot.TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
$tempDirectory = [System.IO.Path]::GetFullPath((Join-Path $systemTempRoot (
    'koiki-c4-runtime-negative-{0}-{1}' -f $PID, [guid]::NewGuid().ToString('N'))))
if (-not $tempDirectory.StartsWith($tempPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Negative fixture path escaped the system temp directory: $tempDirectory"
}

New-Item -ItemType Directory -Path $tempDirectory | Out-Null
try {
    Write-Host '=== Java 25 build rejection (expected failure) ==='
    $javaHomeWasSet = Test-Path Env:JAVA_HOME
    $originalJavaHome = $env:JAVA_HOME
    try {
        $env:JAVA_HOME = $Java25Home
        $java25Build = Invoke-CapturedCommand -Command $pwshCommand -Arguments @(
            '-NoProfile'
            '-ExecutionPolicy'
            'Bypass'
            '-File'
            $buildScript
        )
    } finally {
        if ($javaHomeWasSet) {
            $env:JAVA_HOME = $originalJavaHome
        } else {
            Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
        }
    }
    Assert-ExpectedFailure `
        -Result $java25Build `
        -RequiredDiagnostic 'Runtime fixture must be built with Java 21. Actual feature: 25'
    Write-Host 'Java 25 build rejection: EXPECTED FAILURE PASS'

    if ((Get-FileHash -LiteralPath $originalJar -Algorithm SHA256).Hash -cne $originalHash) {
        throw 'Original runtime fixture JAR changed during the Java 25 build rejection guard.'
    }

    Write-Host '=== Modified hash rejection (expected failure) ==='
    $modifiedJar = Join-Path $tempDirectory ([string]$manifest.jarName)
    $modifiedManifest = Join-Path $tempDirectory 'runtime-compatibility-manifest.json'
    Copy-Item -LiteralPath $originalJar -Destination $modifiedJar
    Copy-Item -LiteralPath $manifestPath -Destination $modifiedManifest
    $appendStream = [System.IO.File]::Open(
        $modifiedJar,
        [System.IO.FileMode]::Append,
        [System.IO.FileAccess]::Write,
        [System.IO.FileShare]::None)
    try {
        $appendStream.WriteByte(0)
    } finally {
        $appendStream.Dispose()
    }
    if ((Get-FileHash -LiteralPath $modifiedJar -Algorithm SHA256).Hash -ceq $originalHash) {
        throw 'Modified negative fixture JAR unexpectedly retained the original SHA-256.'
    }

    $hashFailure = Invoke-CapturedCommand -Command $pwshCommand -Arguments @(
        '-NoProfile'
        '-ExecutionPolicy'
        'Bypass'
        '-File'
        $runtimeScript
        '-ExpectedJavaFeature'
        '21'
        '-JavaHome'
        $Java21Home
        '-ArtifactDirectory'
        $tempDirectory
    )
    Assert-ExpectedFailure `
        -Result $hashFailure `
        -RequiredDiagnostic 'Runtime fixture SHA-256 mismatch before Java 21 execution.'
    Write-Host 'Modified hash rejection: EXPECTED FAILURE PASS'

    Write-Host '=== Runtime major mismatch (expected failure) ==='
    $runtimeMismatch = Invoke-CapturedCommand -Command $java21Command -Arguments @(
        '-jar'
        $originalJar
        '25'
    )
    Assert-ExpectedFailure `
        -Result $runtimeMismatch `
        -RequiredDiagnostic 'Java runtime feature mismatch: expected=25 actual=21' `
        -ExpectedExitCode 3
    Write-Host 'Runtime major mismatch: EXPECTED FAILURE PASS'

    $hashAfterNegativeGuards = (Get-FileHash -LiteralPath $originalJar -Algorithm SHA256).Hash
    if ($hashAfterNegativeGuards -cne $originalHash) {
        throw 'Original runtime fixture JAR changed during negative guard verification.'
    }

    Write-Host '=== Positive path restore verification ==='
    & $pwshCommand `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $runtimeScript `
        -ExpectedJavaFeature 21 `
        -JavaHome $Java21Home `
        -ArtifactDirectory $ArtifactDirectory
    if ($LASTEXITCODE -ne 0) {
        throw 'Java 21 positive path restore verification failed.'
    }
    & $pwshCommand `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $runtimeScript `
        -ExpectedJavaFeature 25 `
        -JavaHome $Java25Home `
        -ArtifactDirectory $ArtifactDirectory
    if ($LASTEXITCODE -ne 0) {
        throw 'Java 25 positive path restore verification failed.'
    }

    if ((Get-FileHash -LiteralPath $originalJar -Algorithm SHA256).Hash -cne $originalHash) {
        throw 'Original runtime fixture JAR changed during positive path restore verification.'
    }

    Write-Host "Original SHA-256 retained: $originalHash"
    Write-Host 'C4 Gate 3 runtime negative guards: SUCCESS'
} finally {
    $cleanupTarget = [System.IO.Path]::GetFullPath($tempDirectory)
    if (-not $cleanupTarget.StartsWith($tempPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean a path outside the system temp directory: $cleanupTarget"
    }
    if (Test-Path -LiteralPath $cleanupTarget) {
        Remove-Item -LiteralPath $cleanupTarget -Recurse -Force
    }
}

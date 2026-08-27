[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet(21, 25)]
    [int]$ExpectedJavaFeature,

    [string]$JavaHome,

    [string]$ArtifactDirectory = (Join-Path $PSScriptRoot 'target/runtime-artifact')
)

$ErrorActionPreference = 'Stop'

$successMarker = 'KOIKI_RUNTIME_COMPATIBILITY_SUCCESS'
$manifestPath = Join-Path $ArtifactDirectory 'runtime-compatibility-manifest.json'

function Get-JavaProperties {
    param([Parameter(Mandatory)][string]$JavaCommand)

    $output = @(& $JavaCommand '-XshowSettings:properties' '-version' 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to inspect Java runtime: $JavaCommand"
    }

    $values = @{}
    foreach ($line in $output) {
        if ($line -match '^\s*(java\.specification\.version|java\.vendor|java\.version)\s*=\s*(.+?)\s*$') {
            $values[$Matches[1]] = $Matches[2]
        }
    }

    foreach ($requiredName in @('java.specification.version', 'java.vendor', 'java.version')) {
        if (-not $values.ContainsKey($requiredName)) {
            throw "Java property was not reported: $requiredName"
        }
    }

    [pscustomobject]@{
        Feature = [int]$values['java.specification.version']
        Vendor = [string]$values['java.vendor']
        Version = [string]$values['java.version']
    }
}

function Get-RuntimeJavaCommand {
    param([int]$ExpectedFeature, [string]$RequestedJavaHome)

    $executable = if ($IsWindows) { 'java.exe' } else { 'java' }
    $homes = [System.Collections.Generic.List[string]]::new()
    if (-not [string]::IsNullOrWhiteSpace($RequestedJavaHome)) {
        $homes.Add($RequestedJavaHome)
    } else {
        $versionSpecificHome = [Environment]::GetEnvironmentVariable("JAVA${ExpectedFeature}_HOME")
        if (-not [string]::IsNullOrWhiteSpace($versionSpecificHome)) {
            $homes.Add($versionSpecificHome)
        }
        if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
            $homes.Add($env:JAVA_HOME)
        }
    }

    foreach ($candidateJavaHome in $homes) {
        $candidate = Join-Path $candidateJavaHome "bin/$executable"
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $properties = Get-JavaProperties -JavaCommand $candidate
            if ($properties.Feature -eq $ExpectedFeature) {
                return [pscustomobject]@{
                    Command = $candidate
                    Properties = $properties
                }
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($RequestedJavaHome)) {
        $pathCommand = Get-Command java -ErrorAction SilentlyContinue
        if ($null -ne $pathCommand) {
            $properties = Get-JavaProperties -JavaCommand $pathCommand.Source
            if ($properties.Feature -eq $ExpectedFeature) {
                return [pscustomobject]@{
                    Command = $pathCommand.Source
                    Properties = $properties
                }
            }
        }
    }

    throw "Java $ExpectedFeature runtime was not found. Set JAVA${ExpectedFeature}_HOME or pass -JavaHome."
}

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Runtime fixture manifest was not found: $manifestPath"
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding utf8 | ConvertFrom-Json
if ($manifest.schemaVersion -ne 1) {
    throw "Unsupported runtime fixture manifest schema: $($manifest.schemaVersion)"
}
if ($manifest.buildJavaFeature -ne 21 -or $manifest.classMajorVersion -ne 65) {
    throw 'Runtime fixture manifest does not describe a Java 21 build with class major version 65.'
}
if ([string]::IsNullOrWhiteSpace($manifest.jarName) -or
    [System.IO.Path]::GetFileName([string]$manifest.jarName) -cne [string]$manifest.jarName) {
    throw "Runtime fixture manifest contains an invalid JAR name: $($manifest.jarName)"
}
if ([string]$manifest.sha256 -notmatch '^[0-9A-F]{64}$') {
    throw 'Runtime fixture manifest contains an invalid SHA-256 value.'
}

$resolvedArtifactDirectory = [System.IO.Path]::GetFullPath($ArtifactDirectory)
$jarPath = [System.IO.Path]::GetFullPath((Join-Path $resolvedArtifactDirectory ([string]$manifest.jarName)))
$expectedPrefix = $resolvedArtifactDirectory.TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
if (-not $jarPath.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Runtime fixture JAR escaped the artifact directory: $jarPath"
}
if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Runtime fixture JAR was not found: $jarPath"
}

$runtime = Get-RuntimeJavaCommand -ExpectedFeature $ExpectedJavaFeature -RequestedJavaHome $JavaHome
$hashBefore = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
if ($hashBefore -cne [string]$manifest.sha256) {
    throw "Runtime fixture SHA-256 mismatch before Java $ExpectedJavaFeature execution."
}

Write-Host "Runtime Java: $($runtime.Properties.Vendor) $($runtime.Properties.Version)"
Write-Host "SHA-256 before execution: $hashBefore"
$output = @(& $runtime.Command -jar $jarPath $ExpectedJavaFeature 2>&1)
$exitCode = $LASTEXITCODE
$output | ForEach-Object { Write-Host $_ }
if ($exitCode -ne 0) {
    throw "Runtime fixture failed on Java $ExpectedJavaFeature with exit code $exitCode."
}

$outputText = $output -join [Environment]::NewLine
if (-not $outputText.Contains($successMarker, [System.StringComparison]::Ordinal) -or
    -not $outputText.Contains("actual=$ExpectedJavaFeature", [System.StringComparison]::Ordinal)) {
    throw "Runtime fixture output did not contain the required Java $ExpectedJavaFeature success evidence."
}

$hashAfter = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
if ($hashAfter -cne [string]$manifest.sha256 -or $hashAfter -cne $hashBefore) {
    throw "Runtime fixture SHA-256 mismatch after Java $ExpectedJavaFeature execution."
}

Write-Host "SHA-256 after execution: $hashAfter"
Write-Host "C4 Gate 2 Java $ExpectedJavaFeature runtime verification: SUCCESS"

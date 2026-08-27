[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$fixtureRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $fixtureRoot '../..'))
$verificationPom = Join-Path $fixtureRoot 'pom.xml'
$builtJar = Join-Path $fixtureRoot 'fixture/target/runtime-compatibility-fixture-0.1.0-SNAPSHOT.jar'
$artifactDirectory = Join-Path $fixtureRoot 'target/runtime-artifact'
$artifactJarName = 'runtime-compatibility-fixture-0.1.0-SNAPSHOT.jar'
$artifactJar = Join-Path $artifactDirectory $artifactJarName
$manifestPath = Join-Path $artifactDirectory 'runtime-compatibility-manifest.json'
$classEntryName = 'org/koikifw/buildsupport/internal/runtime/RuntimeCompatibilityProbe.class'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)

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

function Get-BuildJavaCommand {
    $executable = if ($IsWindows) { 'java.exe' } else { 'java' }
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidate = Join-Path $env:JAVA_HOME "bin/$executable"
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    $pathCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($null -ne $pathCommand) {
        return $pathCommand.Source
    }

    throw 'Build Java was not found in JAVA_HOME or PATH.'
}

function Get-ClassMajorVersion {
    param(
        [Parameter(Mandatory)][string]$JarPath,
        [Parameter(Mandatory)][string]$EntryName
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $archive.GetEntry($EntryName)
        if ($null -eq $entry) {
            throw "Class entry was not found in runtime fixture JAR: $EntryName"
        }

        $stream = $entry.Open()
        try {
            $header = [byte[]]::new(8)
            $offset = 0
            while ($offset -lt $header.Length) {
                $read = $stream.Read($header, $offset, $header.Length - $offset)
                if ($read -eq 0) {
                    throw "Class entry is shorter than the required header: $EntryName"
                }
                $offset += $read
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        $archive.Dispose()
    }

    if ($header[0] -ne 0xCA -or $header[1] -ne 0xFE -or
        $header[2] -ne 0xBA -or $header[3] -ne 0xBE) {
        throw "Class entry has an invalid magic header: $EntryName"
    }

    return ([int]$header[6] * 256) + [int]$header[7]
}

$buildJavaCommand = Get-BuildJavaCommand
$buildJava = Get-JavaProperties -JavaCommand $buildJavaCommand
if ($buildJava.Feature -ne 21) {
    throw "Runtime fixture must be built with Java 21. Actual feature: $($buildJava.Feature)"
}

Write-Host "Build Java: $($buildJava.Vendor) $($buildJava.Version)"
& $wrapper `
    --batch-mode `
    --no-transfer-progress `
    -f $verificationPom `
    clean package
if ($LASTEXITCODE -ne 0) {
    throw "Runtime fixture Maven build failed with exit code $LASTEXITCODE."
}

if (-not (Test-Path -LiteralPath $builtJar -PathType Leaf)) {
    throw "Runtime fixture JAR was not generated: $builtJar"
}

$classMajorVersion = Get-ClassMajorVersion -JarPath $builtJar -EntryName $classEntryName
if ($classMajorVersion -ne 65) {
    throw "Expected Java 21 class major version 65, actual: $classMajorVersion"
}

$builtHash = (Get-FileHash -LiteralPath $builtJar -Algorithm SHA256).Hash
New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
Copy-Item -LiteralPath $builtJar -Destination $artifactJar -Force
$artifactHash = (Get-FileHash -LiteralPath $artifactJar -Algorithm SHA256).Hash
if ($artifactHash -cne $builtHash) {
    throw 'Copied runtime fixture JAR does not match the Maven build output.'
}

$commit = @(& git -C $repositoryRoot rev-parse HEAD 2>&1)
if ($LASTEXITCODE -ne 0 -or $commit.Count -ne 1 -or $commit[0] -notmatch '^[0-9a-f]{40}$') {
    throw 'Unable to resolve the source commit for the runtime fixture manifest.'
}
$workingTreeStatus = @(& git -C $repositoryRoot status --porcelain=v1 --untracked-files=all 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to inspect the working tree for the runtime fixture manifest.'
}

$manifest = [ordered]@{
    schemaVersion = 1
    commit = [string]$commit[0]
    workingTreeDirty = ($workingTreeStatus.Count -ne 0)
    jarName = $artifactJarName
    sha256 = $artifactHash
    classEntry = $classEntryName
    classMajorVersion = $classMajorVersion
    buildJavaFeature = $buildJava.Feature
    buildJavaVendor = $buildJava.Vendor
    buildJavaVersion = $buildJava.Version
}
$manifestJson = $manifest | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText($manifestPath, $manifestJson + [Environment]::NewLine, $utf8WithoutBom)

Write-Host "Artifact JAR: $artifactJar"
Write-Host "Manifest: $manifestPath"
Write-Host "Class major version: $classMajorVersion"
Write-Host "SHA-256: $artifactHash"
Write-Host "Working tree dirty: $($manifest.workingTreeDirty)"
Write-Host 'C4 Gate 2 runtime fixture build: SUCCESS'

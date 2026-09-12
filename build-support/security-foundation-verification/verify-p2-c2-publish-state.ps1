[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('Capture', 'Verify')]
    [string]$Mode,
    [Parameter(Mandatory)]
    [string]$RepositoryUrl,
    [Parameter(Mandatory)]
    [string]$ManifestPath,
    [string]$ExpectedCommit,
    [string]$GitHubUser = 'zaziedlm'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$signaturePath = Join-Path $PSScriptRoot 'p2-c2-public-api.txt'
$classpathPom = Join-Path $repositoryRoot 'build-support/api-compatibility/p2-c2/pom.xml'
$japicmpPom = Join-Path $repositoryRoot 'build-support/api-compatibility/p2-c2/japicmp-pom.xml'
$inventoryTool = Join-Path $repositoryRoot 'build-support/api-compatibility/p2-c2/FullPublicApiInventory.java'
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$version = '0.1.0-SNAPSHOT'
$temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$workingRoot = Join-Path $temporaryRoot ('koiki-p2-c2-publish-state-' + [guid]::NewGuid().ToString('N'))
$downloadRoot = Join-Path $workingRoot 'downloads'
$localRepository = Join-Path $workingRoot 'repository'
$classpathFile = Join-Path $workingRoot 'classpath.txt'
$reportRoot = Join-Path $workingRoot 'reports'
$utf8WithoutBom = [Text.UTF8Encoding]::new($false)
$token = $null
$authorization = $null
. (Join-Path $PSScriptRoot 'p2-c2-publish-unit.ps1')

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolved) -notlike 'koiki-p2-c2-publish-state-*') {
        throw "Unsafe P2-C2 temporary path: $resolved"
    }
}

function Invoke-Git {
    param([Parameter(Mandatory)][string[]]$Arguments)
    $output = @(& git -C $repositoryRoot @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "git $($Arguments -join ' ') failed." }
    return ($output -join "`n").Trim()
}

function Invoke-Maven {
    param([Parameter(Mandatory)][string]$Label, [Parameter(Mandatory)][string[]]$Arguments)
    Write-Host "=== $Label ==="
    & $wrapper --batch-mode --no-transfer-progress "-Dmaven.repo.local=$localRepository" @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Label failed with exit code $LASTEXITCODE." }
}

function Get-RepositoryUri {
    param([Parameter(Mandatory)][string]$RelativePath)
    return $RepositoryUrl.TrimEnd('/') + '/' + $RelativePath.Replace('\', '/')
}

function Copy-RepositoryPayload {
    param([Parameter(Mandatory)][string]$RelativePath, [Parameter(Mandatory)][string]$Destination)
    $uri = [uri](Get-RepositoryUri -RelativePath $RelativePath)
    New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
    if ($uri.IsFile) {
        Copy-Item -LiteralPath $uri.LocalPath -Destination $Destination
    } else {
        Invoke-WebRequest -UseBasicParsing -Uri $uri.AbsoluteUri -Headers @{ Authorization = $authorization } -OutFile $Destination
    }
}

function Get-ResolvedValue {
    param([xml]$Metadata, [Parameter(Mandatory)][string]$Extension)
    $matches = @($Metadata.metadata.versioning.snapshotVersions.snapshotVersion | Where-Object {
        $classifierProperty = $_.PSObject.Properties['classifier']
        $classifier = if ($null -eq $classifierProperty) { '' } else { [string]$classifierProperty.Value }
        [string]$_.extension -ceq $Extension -and $classifier -ceq ''
    })
    if ($matches.Count -ne 1) { throw "Expected one $Extension snapshotVersion; found $($matches.Count)." }
    return [string]$matches[0].value
}

function Get-LocalPayload {
    param([Parameter(Mandatory)][object]$Entry, [Parameter(Mandatory)][string]$Extension)
    if ($Extension -eq 'pom') { return Join-Path $repositoryRoot "$($Entry.ModulePath)/pom.xml" }
    return Join-Path $repositoryRoot "$($Entry.ModulePath)/target/$($Entry.ArtifactId)-$version.jar"
}

function Assert-Hash {
    param([Parameter(Mandatory)][string]$Path, [Parameter(Mandatory)][string]$Expected, [Parameter(Mandatory)][string]$Label)
    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
    if ($actual -cne $Expected) { throw "$Label SHA-256 mismatch: $actual" }
    return $actual
}

function Resolve-Java {
    $name = if ($IsWindows) { 'java.exe' } else { 'java' }
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin/$name"
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    return (Get-Command $name -ErrorAction Stop).Source
}

$entries = @(Get-P2C2PublishUnit -SupportRoot $PSScriptRoot)
$head = Invoke-Git -Arguments @('rev-parse', 'HEAD')
if ($ExpectedCommit) {
    if ($ExpectedCommit -cnotmatch '^[0-9a-f]{40}$' -or $head -cne $ExpectedCommit) {
        throw "Expected commit does not match HEAD: expected=$ExpectedCommit actual=$head"
    }
}
if ($env:GITHUB_SHA -and $head -cne $env:GITHUB_SHA) {
    throw "HEAD does not match GITHUB_SHA: $head"
}

$repositoryUri = [uri]$RepositoryUrl
if (-not $repositoryUri.IsFile) {
    $token = $env:KOIKI_PACKAGES_TOKEN
    if ([string]::IsNullOrWhiteSpace($token)) { throw 'KOIKI_PACKAGES_TOKEN is required for remote verification.' }
    $authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${GitHubUser}:$token"))
}

Assert-SafeTemporaryPath -Path $workingRoot
New-Item -ItemType Directory -Path $downloadRoot,$localRepository,$reportRoot -Force | Out-Null

try {
    if ($Mode -eq 'Capture') {
        $artifacts = foreach ($entry in $entries) {
            $metadataRelative = "org/koikifw/$($entry.ArtifactId)/$version/maven-metadata.xml"
            $metadataPath = Join-Path $downloadRoot "$($entry.ArtifactId)-metadata.xml"
            Copy-RepositoryPayload -RelativePath $metadataRelative -Destination $metadataPath
            [xml]$metadata = Get-Content -Raw -LiteralPath $metadataPath

            $payloads = foreach ($extension in @('pom', 'jar')) {
                if ($extension -eq 'jar' -and $entry.Packaging -ne 'JAR') { continue }
                $resolved = Get-ResolvedValue -Metadata $metadata -Extension $extension
                $fileName = "$($entry.ArtifactId)-$resolved.$extension"
                $relative = "org/koikifw/$($entry.ArtifactId)/$version/$fileName"
                $remotePath = Join-Path $downloadRoot "$($entry.ArtifactId)-$extension"
                Copy-RepositoryPayload -RelativePath $relative -Destination $remotePath
                $localPath = Get-LocalPayload -Entry $entry -Extension $extension
                if (-not (Test-Path -LiteralPath $localPath -PathType Leaf)) { throw "Local payload is missing: $localPath" }
                $localHash = (Get-FileHash -LiteralPath $localPath -Algorithm SHA256).Hash
                [void](Assert-Hash -Path $remotePath -Expected $localHash -Label "$($entry.ArtifactId) $extension remote payload")
                [pscustomobject]@{ extension = $extension; resolvedVersion = $resolved; sha256 = $localHash }
            }
            [pscustomobject]@{ artifactId = $entry.ArtifactId; packaging = $entry.Packaging; payloads = @($payloads) }
        }
        $manifest = [ordered]@{
            schemaVersion = 1
            sourceCommit = $head
            workflowRunId = [string]$env:GITHUB_RUN_ID
            workflowRunAttempt = [string]$env:GITHUB_RUN_ATTEMPT
            workflowRef = [string]$env:GITHUB_WORKFLOW_REF
            actor = [string]$env:GITHUB_ACTOR
            capturedAtUtc = [DateTime]::UtcNow.ToString('o')
            repositoryUrl = $RepositoryUrl
            signatureSha256 = (Get-FileHash -LiteralPath $signaturePath -Algorithm SHA256).Hash
            artifacts = @($artifacts)
        }
        $outputDirectory = Split-Path -Parent ([IO.Path]::GetFullPath($ManifestPath))
        if ($outputDirectory) { New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null }
        [IO.File]::WriteAllText($ManifestPath, (($manifest | ConvertTo-Json -Depth 8) + "`n"), $utf8WithoutBom)
        Write-Output "P2-C2 publish manifest captured (13 coordinates / 24 payloads / source $head)."
        return
    }

    if (-not (Test-Path -LiteralPath $ManifestPath -PathType Leaf)) { throw "Publish manifest is missing: $ManifestPath" }
    $manifest = Get-Content -Raw -LiteralPath $ManifestPath | ConvertFrom-Json
    if ($manifest.schemaVersion -ne 1 -or [string]$manifest.sourceCommit -cne $head) {
        throw 'Publish manifest schema or source commit mismatch.'
    }
    if ([string]$manifest.repositoryUrl -cne $RepositoryUrl) {
        throw 'Publish manifest repository URL mismatch.'
    }
    if ($env:GITHUB_RUN_ID -and [string]$manifest.workflowRunId -cne [string]$env:GITHUB_RUN_ID) {
        throw 'Publish manifest belongs to a different workflow run.'
    }
    [void](Assert-Hash -Path $signaturePath -Expected ([string]$manifest.signatureSha256) -Label 'C2-4 signature')
    if (@($manifest.artifacts).Count -ne 13) { throw 'Publish manifest must contain 13 coordinates.' }

    $remoteJarArguments = [Collections.Generic.List[string]]::new()
    foreach ($entry in $entries) {
        $artifact = @($manifest.artifacts | Where-Object artifactId -CEQ $entry.ArtifactId)
        if ($artifact.Count -ne 1) { throw "Manifest coordinate mismatch: $($entry.ArtifactId)" }
        if ([string]$artifact[0].packaging -cne $entry.Packaging) {
            throw "Manifest packaging mismatch: $($entry.ArtifactId)"
        }
        $expectedExtensions = @(if ($entry.Packaging -eq 'JAR') { 'pom'; 'jar' } else { 'pom' })
        $payloads = @($artifact[0].payloads)
        if ($payloads.Count -ne $expectedExtensions.Count) {
            throw "Manifest payload count mismatch: $($entry.ArtifactId)"
        }
        foreach ($expectedExtension in $expectedExtensions) {
            if (@($payloads | Where-Object extension -CEQ $expectedExtension).Count -ne 1) {
                throw "Manifest payload extension mismatch: $($entry.ArtifactId) $expectedExtension"
            }
        }
        foreach ($payload in $payloads) {
            $extension = [string]$payload.extension
            $resolved = [string]$payload.resolvedVersion
            if ($resolved -cnotmatch '^0\.1\.0-\d{8}\.\d{6}-\d+$') {
                throw "Invalid resolved snapshot value: $($entry.ArtifactId) $resolved"
            }
            if ([string]$payload.sha256 -cnotmatch '^[0-9A-F]{64}$') {
                throw "Invalid SHA-256 value: $($entry.ArtifactId) $extension"
            }
            $fileName = "$($entry.ArtifactId)-$resolved.$extension"
            $relative = "org/koikifw/$($entry.ArtifactId)/$version/$fileName"
            $destination = Join-Path $downloadRoot "$($entry.ArtifactId)/$fileName"
            Copy-RepositoryPayload -RelativePath $relative -Destination $destination
            [void](Assert-Hash -Path $destination -Expected ([string]$payload.sha256) -Label "$($entry.ArtifactId) $extension")
            if ($extension -eq 'jar') {
                [void]$remoteJarArguments.Add($entry.ArtifactId)
                [void]$remoteJarArguments.Add($destination)
            }
        }
    }
    if ($remoteJarArguments.Count -ne 22) { throw 'Remote payload set must contain 11 JARs.' }

    Invoke-Maven -Label 'Build approved source for remote comparison' -Arguments @(
        '-f', $rootPom, '-pl', '!koiki-reference-app', 'clean', 'install', '-DskipTests')
    Invoke-Maven -Label 'Resolve complete comparison classpath' -Arguments @(
        '-f', $classpathPom, 'org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath',
        "-Dmdep.outputFile=$classpathFile")
    $currentClassPath = (Get-Content -Raw -LiteralPath $classpathFile).Trim()
    $remoteJars = for ($i = 1; $i -lt $remoteJarArguments.Count; $i += 2) { $remoteJarArguments[$i] }
    $inspectionClassPath = (@($remoteJars) + @($currentClassPath)) -join [IO.Path]::PathSeparator
    $inventoryOutput = @(& (Resolve-Java) '-Xshare:off' '--class-path' $inspectionClassPath $inventoryTool @remoteJarArguments 2>&1)
    if ($LASTEXITCODE -ne 0) { $inventoryOutput | ForEach-Object { Write-Host $_ }; throw 'Remote signature generation failed.' }
    $actualSignature = ($inventoryOutput -join "`n") + "`n"
    $expectedSignature = [IO.File]::ReadAllText($signaturePath).Replace("`r`n", "`n")
    if ($actualSignature -cne $expectedSignature) { throw 'Remote JAR signature differs from the approved aggregate signature.' }

    foreach ($entry in $entries | Where-Object Packaging -eq 'JAR') {
        $artifact = @($manifest.artifacts | Where-Object artifactId -CEQ $entry.ArtifactId)[0]
        $jarPayload = @($artifact.payloads | Where-Object extension -CEQ 'jar')[0]
        $oldJar = Join-Path $downloadRoot "$($entry.ArtifactId)/$($entry.ArtifactId)-$($jarPayload.resolvedVersion).jar"
        $newJar = Get-LocalPayload -Entry $entry -Extension 'jar'
        Invoke-Maven -Label "$($entry.ArtifactId) same-source japicmp" -Arguments @(
            '-f', $japicmpPom, 'verify', "-Djapicmp.oldJar=$oldJar", "-Djapicmp.newJar=$newJar",
            "-Djapicmp.outputDirectory=$(Join-Path $reportRoot $entry.ArtifactId)")
    }
    Write-Output 'Phase 2 P2-C2 C2-5 publish-state verification succeeded (13 coordinates / 24 hashes / aggregate signature / 11 japicmp comparisons).'
} finally {
    $token = $null
    $authorization = $null
    $env:KOIKI_PACKAGES_TOKEN = $null
    if (Test-Path -LiteralPath $workingRoot) {
        Assert-SafeTemporaryPath -Path $workingRoot
        Remove-Item -LiteralPath $workingRoot -Recurse -Force
    }
}

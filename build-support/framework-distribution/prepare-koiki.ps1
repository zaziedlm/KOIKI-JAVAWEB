#requires -Version 7.0
[CmdletBinding(DefaultParameterSetName = 'Release')]
param(
    [Parameter(Mandatory)][ValidatePattern('^\d+\.\d+\.\d+(-preview\.\d+)?$')][string]$Version,
    [Parameter(Mandatory)][ValidatePattern('^[a-fA-F0-9]{64}$')][string]$ExpectedSha256,
    [Parameter(Mandatory, ParameterSetName = 'Release')][ValidatePattern('^[\w.-]+/[\w.-]+$')][string]$Repository,
    [Parameter(Mandatory, ParameterSetName = 'Local')][string]$LocalArchive,
    [string]$LocalRepository = (Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'),
    [string]$Maven = 'mvn',
    [switch]$Offline
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$inventory = @(
    'koiki-dependencies-bom', 'koiki-parent', 'koiki-architecture-contract', 'koiki-archunit-rules',
    'koiki-starter-api', 'koiki-starter-data', 'koiki-starter-data-jpa',
    'koiki-starter-observability', 'koiki-starter-audit', 'koiki-starter-security',
    'koiki-starter-identity', 'koiki-starter-session-jdbc', 'koiki-starter-web-mvc', 'koiki-testing'
)
$sourcesInventory = @($inventory | Where-Object { $_ -notin @('koiki-dependencies-bom', 'koiki-parent', 'koiki-testing') })
$javadocInventory = @('koiki-architecture-contract', 'koiki-archunit-rules', 'koiki-starter-audit', 'koiki-starter-identity', 'koiki-starter-session-jdbc')
function Get-Hash([string]$Path) { (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }
function Require([bool]$Condition, [string]$Message) { if (-not $Condition) { throw $Message } }

$work = Join-Path ([IO.Path]::GetTempPath()) ('koiki-prepare-' + [guid]::NewGuid().ToString('N'))
$target = [IO.Path]::GetFullPath($LocalRepository)
$lock = $null
$locked = $false
$success = $false
try {
    $null = Get-Command $Maven -ErrorAction Stop
    $null = New-Item -ItemType Directory -Path $work
    $archive = Join-Path $work "koiki-framework-$Version.zip"
    if ($PSCmdlet.ParameterSetName -eq 'Release') {
        $null = Get-Command gh -ErrorAction Stop
        # No implicit latest, wildcard asset name, or overwrite of a previous download.
        & gh release download "framework-$Version" --repo $Repository --pattern "koiki-framework-$Version.zip" --dir $work
        Require ($LASTEXITCODE -eq 0) 'Release asset download failed; registration was not started.'
    } else {
        Copy-Item -LiteralPath $LocalArchive -Destination $archive
    }
    Require ((Get-Hash $archive) -eq $ExpectedSha256.ToLowerInvariant()) 'Archive SHA-256 mismatch; registration was not started.'
    $prefix = "koiki-framework-$Version/"
    $zip = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        $names = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
        [long]$length = 0
        foreach ($entry in $zip.Entries) {
            $name = $entry.FullName
            Require ($name.StartsWith($prefix, [StringComparison]::Ordinal)) 'Unexpected archive root.'
            Require (-not ($name.Contains('\') -or $name.Contains(':') -or $name -match '(^|/)\.\.?(/|$)')) 'Unsafe archive path.'
            Require ($names.Add($name)) 'Duplicate archive entry.'
            Require ((($entry.ExternalAttributes -shr 16) -band 0xF000) -ne 0xA000) 'Archive symlinks are not supported.'
            $length += $entry.Length
        }
        Require ($zip.Entries.Count -le 1000 -and $length -le 209715200) 'Archive exceeds distribution limits.'
    } finally { $zip.Dispose() }
    [IO.Compression.ZipFile]::ExtractToDirectory($archive, $work)
    $bundle = Join-Path $work "koiki-framework-$Version"
    $manifest = Get-Content -LiteralPath (Join-Path $bundle 'manifest.json') -Raw | ConvertFrom-Json
    Require ($manifest.schemaVersion -eq 2) 'Manifest schema mismatch; use the preparation script bundled with that version.'
    Require ($manifest.version -eq $Version -and $manifest.releaseTag -eq "framework-$Version") 'Manifest version mismatch.'
    Require ($manifest.artifacts.Count -eq 14 -and $manifest.files.Count -eq 44) 'Unexpected distribution inventory.'
    $expectedFiles = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    $recordsByPath = @{}
    foreach ($record in $manifest.files) {
        Require ($record.path -match '^(maven-repository/org/koikifw/[a-z0-9-]+/[a-zA-Z0-9.-]+/[a-zA-Z0-9.-]+\.(pom|jar)|prepare-koiki\.ps1|README\.md)$') 'Invalid payload path.'
        Require ($expectedFiles.Add([string]$record.path)) 'Duplicate manifest payload.'
        $recordsByPath[$record.path] = $record
        $file = Join-Path $bundle $record.path
        Require (Test-Path -LiteralPath $file -PathType Leaf) "Missing distribution file: $($record.path)"
        Require ((Get-Item -LiteralPath $file).Length -eq $record.size -and (Get-Hash $file) -eq $record.sha256) "Payload mismatch: $($record.path)"
    }
    $seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    $ownedPayloads = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    $install = @()
    foreach ($artifact in $manifest.artifacts) {
        $id = [string]$artifact.artifactId
        Require ($id -in $inventory -and $seen.Add($id)) 'Unexpected or duplicate coordinate.'
        Require ($artifact.groupId -eq 'org.koikifw' -and $artifact.version -eq $Version) 'Coordinate version mismatch.'
        $type = if ($id -in @('koiki-dependencies-bom', 'koiki-parent')) { 'pom' } else { 'jar' }
        Require ($artifact.packaging -eq $type) "Packaging mismatch: $id"
        $relative = "maven-repository/org/koikifw/$id/$Version/$id-$Version"
        $pom = Join-Path $bundle "$relative.pom"
        Require ($expectedFiles.Contains("$relative.pom")) "Missing POM: $id"
        [xml]$model = Get-Content -LiteralPath $pom -Raw
        $ns = [Xml.XmlNamespaceManager]::new($model.NameTable)
        $ns.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
        $idNode = $model.SelectSingleNode('/m:project/m:artifactId', $ns)
        $versionNode = $model.SelectSingleNode('/m:project/m:version | /m:project/m:parent/m:version', $ns)
        Require ($idNode.InnerText -eq $id -and $versionNode.InnerText -eq $Version) "POM coordinate mismatch: $id"
        $packageNode = $model.SelectSingleNode('/m:project/m:packaging', $ns)
        $actualType = if ($packageNode) { $packageNode.InnerText } else { 'jar' }
        Require ($actualType -eq $type) "POM packaging mismatch: $id"
        $payload = if ($type -eq 'jar') { "$relative.jar" } else { "$relative.pom" }
        Require ($expectedFiles.Contains($payload)) "Missing payload: $id"
        $coordinateFiles = @("$relative.pom")
        if ($type -eq 'jar') { $coordinateFiles += "$relative.jar" }
        $sources = $null
        $javadoc = $null
        if ($id -in $sourcesInventory) { $coordinateFiles += "$relative-sources.jar"; $sources = Join-Path $bundle "$relative-sources.jar" }
        if ($id -in $javadocInventory) { $coordinateFiles += "$relative-javadoc.jar"; $javadoc = Join-Path $bundle "$relative-javadoc.jar" }
        Require (@($artifact.files).Count -eq $coordinateFiles.Count) "Classifier inventory mismatch: $id"
        foreach ($record in $artifact.files) {
            Require ($record.path -cin $coordinateFiles -and $ownedPayloads.Add([string]$record.path)) "Unexpected classifier payload: $id"
            Require ($expectedFiles.Contains([string]$record.path)) "Missing classifier payload: $id"
            $expectedClassifier = if ($record.path -ceq "$relative-sources.jar") { 'sources' } elseif ($record.path -ceq "$relative-javadoc.jar") { 'javadoc' } else { $null }
            Require ($null -ne $record.PSObject.Properties['classifier']) "Missing classifier metadata: $id"
            Require ($record.classifier -ceq $expectedClassifier) "Classifier metadata mismatch: $id"
            $globalRecord = $recordsByPath[$record.path]
            Require ($null -ne $globalRecord.PSObject.Properties['classifier']) "Missing global classifier metadata: $id"
            Require ($globalRecord.classifier -ceq $expectedClassifier -and $globalRecord.sha256 -ceq $record.sha256 -and $globalRecord.size -eq $record.size) "Inconsistent artifact manifest: $id"
        }
        $install += @{ Id = $id; Pom = $pom; File = (Join-Path $bundle $payload); Sources = $sources; Javadoc = $javadoc; Expected = $coordinateFiles }
    }
    Require ($seen.Count -eq 14) 'Incomplete coordinate inventory.'
    Require ($ownedPayloads.Count -eq 42 -and $expectedFiles.Contains('prepare-koiki.ps1') -and $expectedFiles.Contains('README.md')) 'Incomplete payload inventory.'
    $actualFiles = @(Get-ChildItem -LiteralPath $bundle -Recurse -File)
    Require ($actualFiles.Count -eq 46) 'Unexpected extra distribution files.'
    foreach ($file in $actualFiles) {
        $relativeName = [IO.Path]::GetRelativePath($bundle, $file.FullName).Replace('\', '/')
        Require ($expectedFiles.Contains($relativeName) -or $relativeName -cin @('manifest.json', 'SHA256SUMS')) 'Unexpected extra distribution file.'
    }
    Require ((Test-Path -LiteralPath (Join-Path $bundle 'SHA256SUMS') -PathType Leaf)) 'Missing SHA256SUMS.'
    # Validate every destination before writing any KOIKI coordinate.
    foreach ($record in $manifest.files | Where-Object path -Like 'maven-repository/*') {
        $relative = $record.path.Substring('maven-repository/'.Length)
        $dest = Join-Path $target $relative
        if (Test-Path -LiteralPath $dest) {
            Require ((Get-Hash $dest) -eq $record.sha256) "Conflicting installed coordinate: $relative"
        }
    }
    $marker = Join-Path $target ".koiki-distributions/$Version.json"
    if (Test-Path -LiteralPath $marker) {
        $previous = Get-Content -LiteralPath $marker -Raw | ConvertFrom-Json
        Require ($previous.archiveSha256 -eq $ExpectedSha256.ToLowerInvariant()) 'Conflicting completed distribution.'
    }
    $null = New-Item -ItemType Directory -Force -Path $target
    $lockPath = Join-Path $target '.koiki-prepare.lock'
    $lock = [IO.File]::Open($lockPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    $locked = $true
    # Recheck after taking the shared KOIKI installer lock.
    foreach ($record in $manifest.files | Where-Object path -Like 'maven-repository/*') {
        $dest = Join-Path $target $record.path.Substring('maven-repository/'.Length)
        if (Test-Path -LiteralPath $dest) { Require ((Get-Hash $dest) -eq $record.sha256) 'Coordinate changed while preparing.' }
    }
    Push-Location $work
    $previousMavenBaseDir = $env:MAVEN_BASEDIR
    try {
        # Windows Wrapper searches from cwd unless its own project base is supplied.
        # Keep cwd outside the Consumer so an uninstalled Parent is not read.
        $mavenCommand = Get-Command $Maven -ErrorAction Stop
        if ($IsWindows -and [IO.Path]::GetFileName($mavenCommand.Source) -ieq 'mvnw.cmd') {
            $env:MAVEN_BASEDIR = Split-Path -Parent $mavenCommand.Source
        }
        foreach ($id in $inventory) {
            $item = $install | Where-Object Id -EQ $id
            $destination = Join-Path $target "org/koikifw/$id/$Version"
            $ready = @($item.Expected | Where-Object {
                -not (Test-Path -LiteralPath (Join-Path $destination ([IO.Path]::GetFileName($_))))
            }).Count -eq 0
            if ($ready) { continue }
            $arguments = @('-B', '-ntp', "-Dmaven.repo.local=$target", 'org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file',
                           "-Dfile=$($item.File)", "-DpomFile=$($item.Pom)", '-DgeneratePom=false')
            if ($item.Sources) { $arguments += "-Dsources=$($item.Sources)" }
            if ($item.Javadoc) { $arguments += "-Djavadoc=$($item.Javadoc)" }
            if ($Offline) { $arguments = @('-o') + $arguments }
            & $Maven @arguments *> (Join-Path $work 'maven-install.log')
            Require ($LASTEXITCODE -eq 0) "Maven registration failed for $id. Preparation is incomplete; rerun with the same verified asset."
        }
    } finally {
        $env:MAVEN_BASEDIR = $previousMavenBaseDir
        Pop-Location
    }
    foreach ($record in $manifest.files | Where-Object path -Like 'maven-repository/*') {
        Require ((Get-Hash (Join-Path $target $record.path.Substring('maven-repository/'.Length))) -eq $record.sha256) 'Installed payload verification failed.'
    }
    $null = New-Item -ItemType Directory -Force -Path (Split-Path $marker)
    $state = @{ schemaVersion = 2; version = $Version; releaseTag = "framework-$Version"; archiveSha256 = $ExpectedSha256.ToLowerInvariant(); coordinates = 14; payloads = 42;
                files = @($manifest.files | Where-Object path -Like 'maven-repository/*') }
    $state | ConvertTo-Json | Set-Content -LiteralPath $marker -Encoding utf8NoBOM
    $success = $true
    Write-Host "KOIKI $Version ready: 14 coordinates / 42 payloads verified in $target"
} finally {
    if ($lock) { $lock.Dispose() }
    if ($locked) { Remove-Item -LiteralPath $lockPath -Force }
    if (Test-Path -LiteralPath $work) {
        $resolvedWork = [IO.Path]::GetFullPath($work)
        $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
        Require ($resolvedWork.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and
                 [IO.Path]::GetFileName($resolvedWork) -match '^koiki-prepare-[a-f0-9]{32}$') 'Unsafe temporary cleanup path.'
        Remove-Item -LiteralPath $resolvedWork -Recurse -Force
    }
    if (-not $success) { Write-Warning 'KOIKI preparation did not complete. No success marker was written by this run.' }
}

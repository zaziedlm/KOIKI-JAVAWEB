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
    Require ($manifest.schemaVersion -eq 1 -and $manifest.version -eq $Version -and $manifest.releaseTag -eq "framework-$Version") 'Manifest version mismatch.'
    Require ($manifest.artifacts.Count -eq 14 -and $manifest.files.Count -eq 28) 'Unexpected distribution inventory.'
    $expectedFiles = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($record in $manifest.files) {
        Require ($record.path -match '^(maven-repository/org/koikifw/[a-z0-9-]+/[a-zA-Z0-9.-]+/[a-zA-Z0-9.-]+\.(pom|jar)|prepare-koiki\.ps1|README\.md)$') 'Invalid payload path.'
        Require ($expectedFiles.Add([string]$record.path)) 'Duplicate manifest payload.'
        $file = Join-Path $bundle $record.path
        Require ((Get-Item -LiteralPath $file).Length -eq $record.size -and (Get-Hash $file) -eq $record.sha256) "Payload mismatch: $($record.path)"
    }
    $seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
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
        $install += @{ Id = $id; Pom = $pom; File = (Join-Path $bundle $payload) }
    }
    Require ($seen.Count -eq 14) 'Incomplete coordinate inventory.'
    $actualFiles = @(Get-ChildItem -LiteralPath $bundle -Recurse -File)
    Require ($actualFiles.Count -eq 30) 'Unexpected extra distribution files.'
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
    try {
        foreach ($id in $inventory) {
            $item = $install | Where-Object Id -EQ $id
            $destination = Join-Path $target "org/koikifw/$id/$Version"
            $ready = (Test-Path -LiteralPath (Join-Path $destination ([IO.Path]::GetFileName($item.Pom)))) -and
                     (Test-Path -LiteralPath (Join-Path $destination ([IO.Path]::GetFileName($item.File))))
            if ($ready) { continue }
            $arguments = @('-B', '-ntp', "-Dmaven.repo.local=$target", 'org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file',
                           "-Dfile=$($item.File)", "-DpomFile=$($item.Pom)", '-DgeneratePom=false')
            if ($Offline) { $arguments = @('-o') + $arguments }
            & $Maven @arguments *> (Join-Path $work 'maven-install.log')
            Require ($LASTEXITCODE -eq 0) "Maven registration failed for $id. Preparation is incomplete; rerun with the same verified asset."
        }
    } finally { Pop-Location }
    foreach ($record in $manifest.files | Where-Object path -Like 'maven-repository/*') {
        Require ((Get-Hash (Join-Path $target $record.path.Substring('maven-repository/'.Length))) -eq $record.sha256) 'Installed payload verification failed.'
    }
    $null = New-Item -ItemType Directory -Force -Path (Split-Path $marker)
    $state = @{ version = $Version; releaseTag = "framework-$Version"; archiveSha256 = $ExpectedSha256.ToLowerInvariant(); coordinates = 14 }
    $state | ConvertTo-Json | Set-Content -LiteralPath $marker -Encoding utf8NoBOM
    $success = $true
    Write-Host "KOIKI $Version ready: 14 coordinates verified in $target"
} finally {
    if ($lock) { $lock.Dispose() }
    if ($locked) { Remove-Item -LiteralPath $lockPath -Force }
    if (Test-Path -LiteralPath $work) { Remove-Item -LiteralPath $work -Recurse -Force }
    if (-not $success) { Write-Warning 'KOIKI preparation did not complete. No success marker was written by this run.' }
}

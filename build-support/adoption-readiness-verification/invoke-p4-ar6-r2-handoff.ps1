[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$ExpectedFrameworkCommit,

    [Parameter(Mandatory)]
    [string]$StageRoot,

    [Parameter(Mandatory)]
    [string]$ManifestOutput,

    [string]$CustomerPom,

    [string]$CustomerSourceIdentity,

    [string]$ExpectedFrameworkVersion,

    [string]$FrameworkRepository = (Join-Path $PSScriptRoot '../..')
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$markerName = '.koiki-p4-ar6-stage-owner.json'
$comparison = if ($IsWindows) {
    [System.StringComparison]::OrdinalIgnoreCase
} else {
    [System.StringComparison]::Ordinal
}
$stageOwned = $false
$stageRemoved = $false
$manifestPathValidated = $false
$failure = $null
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$customerStopwatch = [System.Diagnostics.Stopwatch]::new()
$artifacts = @()
$frameworkVersion = $null
$frameworkCommit = $null
$frameworkSourceDirty = $null
$javaRuntime = $null
$mavenWrapperVersion = $null
$formalInventoryStatus = 'PENDING'
$forbiddenContentStatus = 'PENDING'
$customerBuildStatus = if ($CustomerPom) { 'PENDING' } else { 'NOT_REQUESTED' }
$payloadsUnchanged = $null
$internalPackageReferences = $null
$cleanupStatus = 'PENDING'
$findingIds = [System.Collections.Generic.List[string]]::new()
$customerIdentityKind = if ($CustomerPom) { 'not-recorded' } else { 'not-requested' }
$customerIdentityValue = if ($CustomerPom) { 'not-recorded' } else { 'not-requested' }
$toolingScriptSha256 = (Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash.ToUpperInvariant()
$currentStep = 'parameter-validation'
$failedStep = $null

function Get-FullPath {
    param([Parameter(Mandatory)][string]$Path)

    if (-not [System.IO.Path]::IsPathFullyQualified($Path)) {
        throw "An absolute path is required: $Path"
    }
    return [System.IO.Path]::GetFullPath($Path)
}

function Test-SamePath {
    param(
        [Parameter(Mandatory)][string]$Left,
        [Parameter(Mandatory)][string]$Right
    )

    return [string]::Equals(
        $Left.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar),
        $Right.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar),
        $comparison)
}

function Test-SameOrChildPath {
    param(
        [Parameter(Mandatory)][string]$Candidate,
        [Parameter(Mandatory)][string]$Root
    )

    $normalizedCandidate = $Candidate.TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $normalizedRoot = $Root.TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    if ([string]::Equals($normalizedCandidate, $normalizedRoot, $comparison)) {
        return $true
    }
    $prefix = $normalizedRoot + [System.IO.Path]::DirectorySeparatorChar
    return $normalizedCandidate.StartsWith($prefix, $comparison)
}

function Assert-NotReparsePoint {
    param([Parameter(Mandatory)][string]$Path)

    if (Test-Path -LiteralPath $Path) {
        $item = Get-Item -LiteralPath $Path -Force
        if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "A link or reparse point is not allowed: $Path"
        }
    }
}

function Invoke-GitText {
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    $output = @(& git -C $Repository @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git -C <repository> $($Arguments -join ' ')"
    }
    return (($output | Out-String).Trim())
}

function Assert-FrameworkSource {
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$ExpectedCommit
    )

    $actualCommit = Invoke-GitText -Repository $Repository -Arguments @('rev-parse', 'HEAD')
    $script:frameworkCommit = $actualCommit
    if ($actualCommit -cne $ExpectedCommit.ToLowerInvariant()) {
        throw 'Framework HEAD does not match ExpectedFrameworkCommit.'
    }
    $status = Invoke-GitText -Repository $Repository -Arguments @(
        'status', '--porcelain=v1', '--untracked-files=all')
    $script:frameworkSourceDirty = -not [string]::IsNullOrWhiteSpace($status)
    if (-not [string]::IsNullOrWhiteSpace($status)) {
        throw 'Framework worktree is not clean, including non-ignored untracked files.'
    }
    return $actualCommit
}

function Get-GitRepositoryRoot {
    param([Parameter(Mandatory)][string]$Path)

    $directory = if (Test-Path -LiteralPath $Path -PathType Leaf) {
        [System.IO.Path]::GetDirectoryName([System.IO.Path]::GetFullPath($Path))
    } else {
        $Path
    }
    $output = @(& git -C $directory rev-parse --show-toplevel 2>$null)
    if ($LASTEXITCODE -ne 0) {
        return [System.IO.Path]::GetFullPath($directory)
    }
    return [System.IO.Path]::GetFullPath((($output | Out-String).Trim()))
}

function Get-JavaRuntime {
    $javaName = if ($IsWindows) { 'java.exe' } else { 'java' }
    $javacName = if ($IsWindows) { 'javac.exe' } else { 'javac' }
    $java = if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME "bin/$javaName"
    } else {
        (Get-Command $javaName -ErrorAction Stop).Source
    }
    $javac = if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME "bin/$javacName"
    } else {
        (Get-Command $javacName -ErrorAction Stop).Source
    }
    if (-not (Test-Path -LiteralPath $java -PathType Leaf) -or
        -not (Test-Path -LiteralPath $javac -PathType Leaf)) {
        throw 'A complete JDK was not found.'
    }
    $versionOutput = @(& $java -version 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read the Java runtime version.' }
    $versionText = (($versionOutput | Out-String).Trim())
    if ($versionText -notmatch '(?m)version "21(?:[.]|\")') {
        throw 'P4-AR6 R2 staging requires JDK 21.'
    }
    return (($versionText -split "`r?`n")[0]).Trim()
}

function Invoke-KoikiMaven {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host "=== $Label ==="
    & $script:wrapper --batch-mode --no-transfer-progress `
        "-Dmaven.repo.local=$script:stageRootPath" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
}

function Get-ProjectValue {
    param(
        [Parameter(Mandatory)]$Project,
        [Parameter(Mandatory)][string]$Name
    )

    $node = $Project.SelectSingleNode("./*[local-name()='$Name']")
    $value = if ($null -ne $node) { [string]$node.InnerText } else { '' }
    if ([string]::IsNullOrWhiteSpace($value)) {
        $parent = $Project.SelectSingleNode("./*[local-name()='parent']")
        if ($null -ne $parent) {
            $parentNode = $parent.SelectSingleNode("./*[local-name()='$Name']")
            if ($null -ne $parentNode) { $value = [string]$parentNode.InnerText }
        }
    }
    return $value
}

function Get-FormalReleaseUnit {
    param([Parameter(Mandatory)][string]$Repository)

    $rootPom = Join-Path $Repository 'pom.xml'
    [xml]$root = Get-Content -Raw -LiteralPath $rootPom
    $rootVersion = [string]$root.project.version
    $rootGroup = [string]$root.project.groupId
    if ($rootGroup -ne 'org.koikifw') {
        throw 'Unexpected Framework root groupId.'
    }

    $entries = @($root.project.modules.module |
        ForEach-Object { [string]$_ } |
        Where-Object { $_ -ne 'koiki-reference-app' } |
        ForEach-Object {
            $modulePath = $_
            [xml]$module = Get-Content -Raw -LiteralPath (
                Join-Path $Repository "$modulePath/pom.xml")
            $packaging = [string]$module.project.packaging
            if ([string]::IsNullOrWhiteSpace($packaging)) { $packaging = 'jar' }
            [pscustomobject]@{
                GroupId = Get-ProjectValue -Project $module.project -Name 'groupId'
                ArtifactId = [string]$module.project.artifactId
                Version = Get-ProjectValue -Project $module.project -Name 'version'
                Packaging = $packaging
                ModulePath = $modulePath
            }
        })
    $entries += [pscustomobject]@{
        GroupId = $rootGroup
        ArtifactId = [string]$root.project.artifactId
        Version = $rootVersion
        Packaging = 'pom'
        ModulePath = '.'
    }

    if ($entries.Count -ne 15 -or
        @($entries | Where-Object Packaging -eq 'jar').Count -ne 12 -or
        @($entries | Where-Object Packaging -eq 'pom').Count -ne 3) {
        throw 'Current formal release unit must contain 15 projects / 12 JARs / 3 POM-only projects.'
    }
    if (@($entries.ArtifactId | Sort-Object -Unique).Count -ne $entries.Count) {
        throw 'Formal release unit artifactIds must be unique.'
    }
    if (@($entries | Where-Object {
        $_.GroupId -ne 'org.koikifw' -or $_.Version -ne $rootVersion
    }).Count -ne 0) {
        throw 'Formal release unit coordinates do not share the root groupId and version.'
    }
    if ($entries.ArtifactId -contains 'koiki-reference-app') {
        throw 'Reference must not be part of the formal release unit.'
    }

    [xml]$bom = Get-Content -Raw -LiteralPath (
        Join-Path $Repository 'koiki-dependencies-bom/pom.xml')
    $managedKoiki = @($bom.project.dependencyManagement.dependencies.dependency |
        Where-Object { [string]$_.groupId -eq 'org.koikifw' } |
        ForEach-Object { [string]$_.artifactId } |
        Sort-Object)
    $expectedJars = @($entries | Where-Object Packaging -eq 'jar' |
        ForEach-Object ArtifactId | Sort-Object)
    if (@(Compare-Object $expectedJars $managedKoiki -SyncWindow 0).Count -ne 0) {
        throw 'BOM inventory differs from the current formal JAR inventory.'
    }

    return [pscustomobject]@{
        Version = $rootVersion
        Entries = @($entries | Sort-Object ArtifactId)
    }
}

function Get-ArtifactRole {
    param([Parameter(Mandatory)]$Entry)

    if ($Entry.ModulePath -eq '.') { return 'aggregator' }
    if ($Entry.ArtifactId -eq 'koiki-parent') { return 'parent' }
    if ($Entry.ArtifactId -eq 'koiki-dependencies-bom') { return 'bom' }
    if ($Entry.ArtifactId -eq 'koiki-testing') { return 'testing' }
    if ($Entry.ArtifactId -in @('koiki-architecture-contract', 'koiki-archunit-rules')) {
        return 'architecture'
    }
    return 'runtime'
}

function Get-StageArtifacts {
    param(
        [Parameter(Mandatory)][object[]]$Entries,
        [Parameter(Mandatory)][string]$Repository
    )

    $groupRoot = Join-Path $Repository 'org/koikifw'
    if (-not (Test-Path -LiteralPath $groupRoot -PathType Container)) {
        throw 'The staged org.koikifw repository root is missing.'
    }
    $actualArtifactIds = @(Get-ChildItem -LiteralPath $groupRoot -Directory |
        Select-Object -ExpandProperty Name | Sort-Object)
    $expectedArtifactIds = @($Entries.ArtifactId | Sort-Object)
    if (@(Compare-Object $expectedArtifactIds $actualArtifactIds -SyncWindow 0).Count -ne 0) {
        throw 'Staged org.koikifw coordinates differ from the formal release unit.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $result = foreach ($entry in $Entries) {
        $artifactRoot = Join-Path $groupRoot $entry.ArtifactId
        $versionDirectories = @(Get-ChildItem -LiteralPath $artifactRoot -Directory |
            Select-Object -ExpandProperty Name | Sort-Object)
        if ($versionDirectories.Count -ne 1 -or $versionDirectories[0] -cne $entry.Version) {
            throw "Unexpected staged version inventory for $($entry.ArtifactId)."
        }
        $versionRoot = Join-Path $artifactRoot $entry.Version
        $expectedPayloadNames = @("$($entry.ArtifactId)-$($entry.Version).pom")
        if ($entry.Packaging -eq 'jar') {
            $expectedPayloadNames += "$($entry.ArtifactId)-$($entry.Version).jar"
        }
        $actualPayloadNames = @(Get-ChildItem -LiteralPath $versionRoot -File |
            Where-Object Extension -in @('.pom', '.jar') |
            Select-Object -ExpandProperty Name | Sort-Object)
        if (@(Compare-Object ($expectedPayloadNames | Sort-Object) $actualPayloadNames -SyncWindow 0).Count -ne 0) {
            throw "Unexpected staged payload inventory for $($entry.ArtifactId)."
        }

        $payloads = foreach ($payloadName in ($expectedPayloadNames | Sort-Object)) {
            $payloadPath = Join-Path $versionRoot $payloadName
            if (-not (Test-Path -LiteralPath $payloadPath -PathType Leaf)) {
                throw "Staged payload is missing for $($entry.ArtifactId)."
            }
            if ([System.IO.Path]::GetExtension($payloadPath) -eq '.jar') {
                $archive = [System.IO.Compression.ZipFile]::OpenRead($payloadPath)
                try {
                    $forbidden = @($archive.Entries | Where-Object {
                        $_.FullName -match '^org/koikifw/(?:reference|buildsupport)/' -or
                        $_.FullName -match '^db/migration/customer/' -or
                        $_.FullName -match '\.(?:java|template)$'
                    })
                    if ($forbidden.Count -ne 0) {
                        throw "Forbidden Reference, Tooling, Customer migration or source content was staged in $($entry.ArtifactId)."
                    }
                } finally {
                    $archive.Dispose()
                }
            }
            $file = Get-Item -LiteralPath $payloadPath
            $relativePath = [System.IO.Path]::GetRelativePath($Repository, $payloadPath).Replace('\', '/')
            [ordered]@{
                extension = $file.Extension.TrimStart('.')
                relativePath = $relativePath
                sizeBytes = $file.Length
                sha256 = (Get-FileHash -LiteralPath $payloadPath -Algorithm SHA256).Hash.ToUpperInvariant()
            }
        }

        [ordered]@{
            groupId = $entry.GroupId
            artifactId = $entry.ArtifactId
            version = $entry.Version
            packaging = $entry.Packaging
            role = Get-ArtifactRole -Entry $entry
            consumerVisible = $entry.ModulePath -ne '.'
            payloads = @($payloads)
        }
    }
    return @($result)
}

function Get-ArtifactFingerprint {
    param([Parameter(Mandatory)][object[]]$ArtifactInventory)

    return @($ArtifactInventory | ForEach-Object {
        $coordinate = "$($_.groupId):$($_.artifactId):$($_.version):$($_.packaging)"
        $_.payloads | ForEach-Object {
            "$coordinate|$($_.relativePath)|$($_.sizeBytes)|$($_.sha256)"
        }
    } | Sort-Object)
}

function Assert-ArtifactsUnchanged {
    param(
        [Parameter(Mandatory)][object[]]$Before,
        [Parameter(Mandatory)][object[]]$After
    )

    $difference = @(Compare-Object (Get-ArtifactFingerprint $Before) `
        (Get-ArtifactFingerprint $After) -SyncWindow 0)
    if ($difference.Count -ne 0) {
        throw 'KOIKI coordinates or payloads changed during Customer verification.'
    }
}

function Get-SourceFingerprint {
    param([Parameter(Mandatory)][string]$Root)

    return @(Get-ChildItem -LiteralPath $Root -Recurse -File -Force |
        Where-Object {
            $_.FullName -notmatch '[\\/](?:target|[.]git)[\\/]' -and
            $_.Name -ne $markerName
        } |
        ForEach-Object {
            $relative = [System.IO.Path]::GetRelativePath($Root, $_.FullName).Replace('\', '/')
            "$relative|$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash)"
        } | Sort-Object)
}

function Assert-CustomerPomBoundary {
    param(
        [Parameter(Mandatory)][string]$Pom,
        [Parameter(Mandatory)][string]$CustomerRoot
    )

    $pomFiles = @(Get-ChildItem -LiteralPath $CustomerRoot -Recurse -Filter 'pom.xml' -File |
        Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' })
    if ($pomFiles.Count -eq 0 -or $pomFiles.FullName -notcontains $Pom) {
        throw 'Customer POM inventory does not contain the requested POM.'
    }
    foreach ($pomFile in $pomFiles) {
        [xml]$document = Get-Content -Raw -LiteralPath $pomFile.FullName
        $namespace = [System.Xml.XmlNamespaceManager]::new($document.NameTable)
        $namespace.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
        foreach ($systemPath in @($document.SelectNodes('//m:systemPath', $namespace))) {
            if (-not [string]::IsNullOrWhiteSpace($systemPath.InnerText)) {
                throw 'Customer POM must not use systemPath.'
            }
        }
        foreach ($url in @($document.SelectNodes('//m:repositories/m:repository/m:url', $namespace))) {
            if ($url.InnerText.Trim() -match '^(?i:file:)') {
                throw 'Customer POM must not use a filesystem Maven repository URL.'
            }
        }
        $parent = $document.SelectSingleNode('/m:project/m:parent', $namespace)
        if ($null -ne $parent) {
            $parentGroup = $parent.SelectSingleNode('m:groupId', $namespace)
            if ($null -ne $parentGroup -and $parentGroup.InnerText.Trim() -eq 'org.koikifw') {
                $relativePath = $parent.SelectSingleNode('m:relativePath', $namespace)
                if ($null -eq $relativePath -or
                    -not [string]::IsNullOrWhiteSpace($relativePath.InnerText)) {
                    throw 'A Customer POM inheriting the KOIKI Parent must declare an empty <relativePath/>.'
                }
            }
        }
    }
}

function Get-InternalPackageReferenceCount {
    param([Parameter(Mandatory)][string]$CustomerRoot)

    return @(Get-ChildItem -LiteralPath $CustomerRoot -Recurse -Filter '*.java' -File |
        Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' } |
        Select-String -Pattern 'org[.]koikifw[.][A-Za-z0-9_.]*[.]internal(?:[.;]|$)').Count
}

function Assert-CustomerKoikiDependencies {
    param(
        [Parameter(Mandatory)][string]$DependencyTree,
        [Parameter(Mandatory)][object[]]$ArtifactInventory
    )

    $allowed = @{}
    foreach ($artifact in $ArtifactInventory) {
        $allowed[$artifact.artifactId] = $artifact.version
    }
    $text = Get-Content -Raw -LiteralPath $DependencyTree
    $matches = [regex]::Matches(
        $text, 'org[.]koikifw:([A-Za-z0-9_.-]+):[A-Za-z0-9_.-]+:([A-Za-z0-9_.-]+)')
    foreach ($match in $matches) {
        $artifactId = $match.Groups[1].Value
        $version = $match.Groups[2].Value
        if (-not $allowed.ContainsKey($artifactId) -or $allowed[$artifactId] -cne $version) {
            throw 'Customer dependency tree contains a KOIKI coordinate outside the stage manifest.'
        }
    }
}

function ConvertTo-SafeManifestJson {
    param(
        [Parameter(Mandatory)]$Manifest,
        [Parameter(Mandatory)][string[]]$ForbiddenValues
    )

    $json = $Manifest | ConvertTo-Json -Depth 12
    foreach ($value in $ForbiddenValues) {
        if (-not [string]::IsNullOrWhiteSpace($value) -and
            $json.Contains($value, $comparison)) {
            throw 'Manifest contains a forbidden absolute source or stage path.'
        }
    }
    return $json
}

function Write-DraftManifest {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)]$Manifest,
        [Parameter(Mandatory)][string[]]$ForbiddenValues
    )

    $json = ConvertTo-SafeManifestJson -Manifest $Manifest -ForbiddenValues $ForbiddenValues
    $parent = [System.IO.Path]::GetDirectoryName([System.IO.Path]::GetFullPath($Path))
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent | Out-Null
    }
    Set-Content -LiteralPath $Path -Value $json -Encoding utf8
}

function Write-FinalManifest {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)]$Manifest,
        [Parameter(Mandatory)][string[]]$ForbiddenValues
    )

    Write-DraftManifest -Path $Path -Manifest $Manifest -ForbiddenValues $ForbiddenValues
    $hash = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
    Set-Content -LiteralPath "$Path.sha256" `
        -Value "$hash  $([System.IO.Path]::GetFileName($Path))" -Encoding ascii
    return $hash
}

function New-R2Manifest {
    param(
        [AllowNull()][string]$FinalizedAtUtc,
        [Parameter(Mandatory)][string]$Result,
        [Parameter(Mandatory)][string]$CleanupResult,
        [Parameter(Mandatory)][bool]$StageWasRemoved
    )

    return [ordered]@{
        schemaVersion = 1
        kind = 'p4Ar6LocalStageManifest'
        stagedAtUtc = $script:marker.createdAtUtc
        finalizedAtUtc = $FinalizedAtUtc
        source = [ordered]@{
            repository = 'KOIKI-JAVAWEB'
            commit = $script:frameworkCommit
            dirty = $script:frameworkSourceDirty
        }
        build = [ordered]@{
            javaFeature = 21
            javaRuntime = $script:javaRuntime
            mavenWrapper = $script:mavenWrapperVersion
            os = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription
        }
        tooling = [ordered]@{
            contractVersion = 1
            scriptSha256 = $script:toolingScriptSha256
        }
        customer = [ordered]@{
            sourceIdentityKind = $script:customerIdentityKind
            sourceIdentity = $script:customerIdentityValue
        }
        releaseUnit = [ordered]@{
            logicalVersion = $script:frameworkVersion
            projectCount = 15
            coordinateCount = 15
            jarCount = 12
            pomOnlyCount = 3
        }
        repository = [ordered]@{
            kind = 'local-isolated'
            startedEmpty = $true
            absolutePathRecorded = $false
        }
        artifacts = @($script:artifacts)
        verification = [ordered]@{
            formalInventory = $script:formalInventoryStatus
            forbiddenContent = $script:forbiddenContentStatus
            customerBuild = [ordered]@{
                status = $script:customerBuildStatus
                durationSeconds = [math]::Round($script:customerStopwatch.Elapsed.TotalSeconds, 3)
                koikiPayloadsUnchanged = $script:payloadsUnchanged
                internalPackageReferences = $script:internalPackageReferences
            }
            cleanup = [ordered]@{
                status = $CleanupResult
                residualResourceCount = if ($StageWasRemoved) { 0 } else { 1 }
                stageRootRemoved = $StageWasRemoved
            }
        }
        durationSeconds = [math]::Round($script:stopwatch.Elapsed.TotalSeconds, 3)
        result = $Result
        failedStep = $script:failedStep
        findingIds = @($script:findingIds)
    }
}

function Remove-OwnedStageRoot {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$MarkerId,
        [Parameter(Mandatory)][string]$ExpectedCommit
    )

    Assert-NotReparsePoint -Path $Path
    $markerPath = Join-Path $Path $markerName
    if (-not (Test-Path -LiteralPath $markerPath -PathType Leaf)) {
        throw 'Stage ownership marker is missing; refusing cleanup.'
    }
    $marker = Get-Content -Raw -LiteralPath $markerPath | ConvertFrom-Json
    if ($marker.markerId -cne $MarkerId -or
        $marker.expectedCommit -cne $ExpectedCommit -or
        -not (Test-SamePath -Left ([string]$marker.stageRoot) -Right $Path)) {
        throw 'Stage ownership marker does not match; refusing cleanup.'
    }
    Remove-Item -LiteralPath $Path -Recurse -Force
}

$frameworkRoot = [System.IO.Path]::GetFullPath($FrameworkRepository)
if (-not (Test-Path -LiteralPath (Join-Path $frameworkRoot 'pom.xml') -PathType Leaf)) {
    throw 'FrameworkRepository does not contain the KOIKI root POM.'
}
$wrapper = if ($IsWindows) {
    Join-Path $frameworkRoot 'mvnw.cmd'
} else {
    Join-Path $frameworkRoot 'mvnw'
}
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    throw 'Maven Wrapper was not found in FrameworkRepository.'
}

$stageRootPath = Get-FullPath -Path $StageRoot
$manifestOutputPath = Get-FullPath -Path $ManifestOutput
$manifestHashPath = "$manifestOutputPath.sha256"
$frameworkRoot = Get-FullPath -Path $frameworkRoot
$customerPomPath = $null
$customerRoot = $null
$customerRepositoryRoot = $null
if ($CustomerPom) {
    $customerPomPath = Get-FullPath -Path $CustomerPom
    if (-not (Test-Path -LiteralPath $customerPomPath -PathType Leaf)) {
        throw 'CustomerPom does not exist.'
    }
    $customerRoot = [System.IO.Path]::GetDirectoryName($customerPomPath)
    $customerRepositoryRoot = Get-GitRepositoryRoot -Path $customerPomPath
}

$pathRoot = [System.IO.Path]::GetPathRoot($stageRootPath)
$userHome = [System.IO.Path]::GetFullPath([Environment]::GetFolderPath('UserProfile'))
$normalMavenRepository = Join-Path $userHome '.m2/repository'
if (Test-SamePath -Left $stageRootPath -Right $pathRoot) {
    throw 'StageRoot must not be a filesystem root.'
}
if (Test-SamePath -Left $stageRootPath -Right $userHome) {
    throw 'StageRoot must not be the user home directory.'
}
if (Test-SameOrChildPath -Candidate $stageRootPath -Root $normalMavenRepository) {
    throw 'StageRoot must not be the normal Maven local repository.'
}
if (Test-SameOrChildPath -Candidate $stageRootPath -Root $frameworkRoot) {
    throw 'StageRoot must be outside FrameworkRepository.'
}
if ($customerRepositoryRoot -and
    (Test-SameOrChildPath -Candidate $stageRootPath -Root $customerRepositoryRoot)) {
    throw 'StageRoot must be outside the Customer repository.'
}
if (Test-SameOrChildPath -Candidate $manifestOutputPath -Root $stageRootPath) {
    throw 'ManifestOutput must be outside StageRoot.'
}
if (Test-SameOrChildPath -Candidate $manifestOutputPath -Root $frameworkRoot) {
    throw 'ManifestOutput must be outside FrameworkRepository.'
}
if ($customerRepositoryRoot -and
    (Test-SameOrChildPath -Candidate $manifestOutputPath -Root $customerRepositoryRoot)) {
    throw 'ManifestOutput must be outside the Customer repository.'
}
if ((Test-Path -LiteralPath $manifestOutputPath) -or
    (Test-Path -LiteralPath $manifestHashPath)) {
    throw 'ManifestOutput or its SHA-256 sidecar already exists; refusing overwrite.'
}
$manifestPathValidated = $true

Assert-NotReparsePoint -Path $stageRootPath
if (Test-Path -LiteralPath $stageRootPath) {
    if (-not (Test-Path -LiteralPath $stageRootPath -PathType Container)) {
        throw 'StageRoot exists and is not a directory.'
    }
    if (@(Get-ChildItem -LiteralPath $stageRootPath -Force).Count -ne 0) {
        throw 'StageRoot must be empty.'
    }
} else {
    $parent = [System.IO.Path]::GetDirectoryName($stageRootPath)
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
        throw 'The StageRoot parent directory must already exist.'
    }
    Assert-NotReparsePoint -Path $parent
    New-Item -ItemType Directory -Path $stageRootPath | Out-Null
}
Assert-NotReparsePoint -Path $stageRootPath

$markerId = [guid]::NewGuid().ToString('N')
$marker = [ordered]@{
    kind = 'p4Ar6StageOwner'
    markerId = $markerId
    stageRoot = $stageRootPath
    expectedCommit = $ExpectedFrameworkCommit.ToLowerInvariant()
    createdAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
}
$marker | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stageRootPath $markerName) -Encoding utf8
$stageOwned = $true

try {
    $currentStep = 'framework-source-preflight'
    $frameworkCommit = Assert-FrameworkSource -Repository $frameworkRoot `
        -ExpectedCommit $ExpectedFrameworkCommit.ToLowerInvariant()
    $currentStep = 'tool-version-preflight'
    $javaRuntime = Get-JavaRuntime
    $mavenVersionOutput = @(& $wrapper --version 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read the Maven Wrapper version.' }
    $mavenWrapperVersion = (($mavenVersionOutput | Select-Object -First 1) | Out-String).Trim()

    $currentStep = 'formal-release-unit-discovery'
    $formal = Get-FormalReleaseUnit -Repository $frameworkRoot
    $frameworkVersion = $formal.Version
    if ($ExpectedFrameworkVersion -and $frameworkVersion -cne $ExpectedFrameworkVersion) {
        throw 'Framework version does not match ExpectedFrameworkVersion.'
    }

    $currentStep = 'formal-release-unit-stage'
    Invoke-KoikiMaven -Label 'Stage the current formal KOIKI release unit' -Arguments @(
        '-f', (Join-Path $frameworkRoot 'pom.xml'),
        '-pl', '!koiki-reference-app',
        'clean', 'install', '-DskipTests')
    $frameworkCommit = Assert-FrameworkSource -Repository $frameworkRoot `
        -ExpectedCommit $ExpectedFrameworkCommit.ToLowerInvariant()

    $currentStep = 'staged-inventory-inspection'
    $artifacts = @(Get-StageArtifacts -Entries $formal.Entries -Repository $stageRootPath)
    $formalInventoryStatus = 'PASS'
    $forbiddenContentStatus = 'PASS'
    $forbiddenManifestValues = @($stageRootPath, $frameworkRoot, $userHome)
    if ($customerRoot) { $forbiddenManifestValues += $customerRoot }
    if ($customerRepositoryRoot) { $forbiddenManifestValues += $customerRepositoryRoot }
    $draftManifest = New-R2Manifest -FinalizedAtUtc $null -Result 'PENDING' `
        -CleanupResult 'PENDING' -StageWasRemoved $false
    Write-DraftManifest -Path $manifestOutputPath -Manifest $draftManifest `
        -ForbiddenValues $forbiddenManifestValues

    if ($CustomerPom) {
        $currentStep = 'customer-boundary-inspection'
        Assert-CustomerPomBoundary -Pom $customerPomPath -CustomerRoot $customerRoot
        $internalPackageReferences = Get-InternalPackageReferenceCount -CustomerRoot $customerRoot
        if ($internalPackageReferences -ne 0) {
            throw 'Customer source references a KOIKI internal package.'
        }
        if ($CustomerSourceIdentity) {
            if ($CustomerSourceIdentity -match '[\\/]' -or
                $CustomerSourceIdentity.Contains($userHome, $comparison)) {
                throw 'CustomerSourceIdentity must be a non-path, approved non-secret value.'
            }
            $customerIdentityKind = 'caller-approved'
            $customerIdentityValue = $CustomerSourceIdentity
        } elseif (Test-SamePath -Left $customerRepositoryRoot -Right $frameworkRoot) {
            $customerIdentityKind = 'framework-fixture-commit'
            $customerIdentityValue = $frameworkCommit
        }

        $sourceBefore = @(Get-SourceFingerprint -Root $customerRoot)
        $currentStep = 'customer-build'
        $customerStopwatch.Start()
        Invoke-KoikiMaven -Label 'Verify the independent Customer-like build' -Arguments @(
            '--no-snapshot-updates', '-f', $customerPomPath, 'clean', 'verify')
        $currentStep = 'customer-dependency-inspection'
        $dependencyTree = Join-Path $stageRootPath '.koiki-p4-ar6-customer-dependencies.txt'
        Invoke-KoikiMaven -Label 'Capture sanitized Customer KOIKI dependency resolution' -Arguments @(
            '--no-snapshot-updates', '-f', $customerPomPath,
            'dependency:tree', '-Dincludes=org.koikifw:*', "-DoutputFile=$dependencyTree")
        $customerStopwatch.Stop()
        Assert-CustomerKoikiDependencies -DependencyTree $dependencyTree `
            -ArtifactInventory $artifacts
        $currentStep = 'customer-source-and-payload-reinspection'
        $sourceAfter = @(Get-SourceFingerprint -Root $customerRoot)
        if (@(Compare-Object $sourceBefore $sourceAfter -SyncWindow 0).Count -ne 0) {
            throw 'Customer POM or source content changed during verification.'
        }
        $artifactsAfter = @(Get-StageArtifacts -Entries $formal.Entries -Repository $stageRootPath)
        Assert-ArtifactsUnchanged -Before $artifacts -After $artifactsAfter
        $payloadsUnchanged = $true
        $customerBuildStatus = 'PASS'
    }
} catch {
    $failure = $_
    $failedStep = $currentStep
    Write-Warning "P4-AR6 R2 step '$failedStep' failed: $($_.Exception.Message)"
    if ($formalInventoryStatus -eq 'PENDING') { $formalInventoryStatus = 'FAIL' }
    if ($forbiddenContentStatus -eq 'PENDING') { $forbiddenContentStatus = 'FAIL' }
    if ($CustomerPom -and $customerBuildStatus -eq 'PENDING') { $customerBuildStatus = 'FAIL' }
    [void]$findingIds.Add('AR6-R2-EXECUTION-FAILURE')
} finally {
    if ($customerStopwatch.IsRunning) { $customerStopwatch.Stop() }
    if ($stageOwned -and (Test-Path -LiteralPath $stageRootPath)) {
        try {
            Remove-OwnedStageRoot -Path $stageRootPath -MarkerId $markerId `
                -ExpectedCommit $ExpectedFrameworkCommit.ToLowerInvariant()
            $stageRemoved = $true
            $cleanupStatus = 'PASS'
        } catch {
            $cleanupStatus = 'FAIL'
            if (-not $findingIds.Contains('AR6-R2-CLEANUP-FAILURE')) {
                [void]$findingIds.Add('AR6-R2-CLEANUP-FAILURE')
            }
            if ($null -eq $failure) { $failure = $_ }
        }
    } elseif ($stageOwned) {
        $stageRemoved = $true
        $cleanupStatus = 'PASS'
    } else {
        $cleanupStatus = 'NOT_OWNED'
    }

    $stopwatch.Stop()
    if ($manifestPathValidated) {
        $overall = if ($null -eq $failure -and $cleanupStatus -eq 'PASS') { 'PASS' } else { 'FAIL' }
        $manifest = New-R2Manifest -FinalizedAtUtc ([DateTimeOffset]::UtcNow.ToString('O')) `
            -Result $overall -CleanupResult $cleanupStatus -StageWasRemoved $stageRemoved
        $forbiddenValues = @($stageRootPath, $frameworkRoot, $userHome)
        if ($customerRoot) { $forbiddenValues += $customerRoot }
        if ($customerRepositoryRoot) { $forbiddenValues += $customerRepositoryRoot }
        try {
            $manifestHash = Write-FinalManifest -Path $manifestOutputPath `
                -Manifest $manifest -ForbiddenValues $forbiddenValues
            Write-Host "P4-AR6 R2 manifest SHA-256: $manifestHash"
        } catch {
            if ($null -eq $failure) { $failure = $_ }
        }
    }
}

if ($null -ne $failure) {
    throw 'P4-AR6 R2 handoff verification failed. Review the finding IDs and local console output.'
}

Write-Host 'P4-AR6 R2 handoff verification succeeded.'

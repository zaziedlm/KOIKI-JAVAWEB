[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('BaselinePreflight', 'Full')]
    [string]$Mode,

    [Parameter(Mandatory)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$ExpectedFrameworkCommit,

    [Parameter(Mandatory)]
    [string]$ExternalProject,

    [Parameter(Mandatory)]
    [string]$BaselineManifest,

    [Parameter(Mandatory)]
    [string]$BaselineManifestSha256,

    [string]$StageRoot,

    [string]$ManifestOutput,

    [string]$ExternalSourceIdentity,

    [string]$ExpectedFrameworkVersion = '0.1.0-SNAPSHOT',

    [string]$FrameworkRepository = (Join-Path $PSScriptRoot '../..')
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$comparison = if ($IsWindows) {
    [System.StringComparison]::OrdinalIgnoreCase
} else {
    [System.StringComparison]::Ordinal
}

function Get-FullPath {
    param([Parameter(Mandatory)][string]$Path)

    if (-not [System.IO.Path]::IsPathFullyQualified($Path)) {
        throw "An absolute path is required: $Path"
    }
    return [System.IO.Path]::GetFullPath($Path)
}

function Get-GitText {
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

function Assert-ManifestHash {
    param(
        [Parameter(Mandatory)][string]$Manifest,
        [Parameter(Mandatory)][string]$Sidecar
    )

    if (-not (Test-Path -LiteralPath $Manifest -PathType Leaf)) {
        throw 'Initializr baseline manifest does not exist.'
    }
    if (-not (Test-Path -LiteralPath $Sidecar -PathType Leaf)) {
        throw 'Initializr baseline SHA-256 sidecar does not exist.'
    }
    $sidecarText = (Get-Content -Raw -LiteralPath $Sidecar).Trim()
    if ($sidecarText -notmatch '^(?<hash>[0-9A-Fa-f]{64})\s+initializr-baseline-[0-9]{8}[.]json$') {
        throw 'Initializr baseline SHA-256 sidecar has an unexpected format.'
    }
    $expected = $Matches.hash.ToUpperInvariant()
    $actual = (Get-FileHash -LiteralPath $Manifest -Algorithm SHA256).Hash.ToUpperInvariant()
    if ($actual -cne $expected) {
        throw 'Initializr baseline manifest SHA-256 does not match its sidecar.'
    }
    return $actual
}

function Assert-InitializrInventory {
    param(
        [Parameter(Mandatory)][string]$Project,
        [Parameter(Mandatory)][string]$Manifest
    )

    $baseline = Get-Content -Raw -LiteralPath $Manifest | ConvertFrom-Json
    if ($baseline.kind -cne 'externalReferenceInitializrBaseline' -or
        [int]$baseline.schemaVersion -ne 1) {
        throw 'Unexpected Initializr baseline manifest kind or schema version.'
    }
    $actualFiles = @(Get-ChildItem -LiteralPath $Project -Recurse -File -Force |
        Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' })
    if ($actualFiles.Count -ne [int]$baseline.state.fileCount) {
        throw 'External project file count differs from the Initializr baseline.'
    }
    foreach ($entry in @($baseline.files)) {
        $relative = ([string]$entry.path).Replace(
            '/', [System.IO.Path]::DirectorySeparatorChar)
        $path = Join-Path $Project $relative
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Initializr baseline file is missing: $($entry.path)"
        }
        $file = Get-Item -LiteralPath $path
        $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToUpperInvariant()
        if ($file.Length -ne [long]$entry.sizeBytes -or
            $hash -cne ([string]$entry.sha256).ToUpperInvariant()) {
            throw "Initializr baseline file changed: $($entry.path)"
        }
    }
}

function Assert-TransformedPom {
    param([Parameter(Mandatory)][string]$Pom)

    [xml]$document = Get-Content -Raw -LiteralPath $Pom
    $namespace = [System.Xml.XmlNamespaceManager]::new($document.NameTable)
    $namespace.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')

    $parent = $document.SelectSingleNode('/m:project/m:parent', $namespace)
    if ($null -eq $parent -or
        $parent.groupId -cne 'org.koikifw' -or
        $parent.artifactId -cne 'koiki-parent' -or
        $parent.version -cne $ExpectedFrameworkVersion) {
        throw 'External Reference must inherit the expected KOIKI Parent.'
    }
    $relativePath = $parent.SelectSingleNode('m:relativePath', $namespace)
    if ($null -eq $relativePath -or
        -not [string]::IsNullOrWhiteSpace($relativePath.InnerText)) {
        throw 'External Reference KOIKI Parent must declare an empty <relativePath/>.'
    }
    if ($document.project.groupId -cne 'jp.co.himacs' -or
        $document.project.artifactId -cne 'ext-reference' -or
        $document.project.version -cne '0.0.1-SNAPSHOT') {
        throw 'External Reference coordinates differ from the approved transformation.'
    }
    if ($document.project.packaging -cne 'jar') {
        throw 'External Reference packaging must be jar.'
    }
    if ($null -ne $document.SelectSingleNode('//m:systemPath', $namespace)) {
        throw 'External Reference POM must not declare systemPath.'
    }
    if (@($document.SelectNodes(
            '/m:project/m:repositories/m:repository|' +
            '/m:project/m:pluginRepositories/m:pluginRepository',
            $namespace)).Count -ne 0) {
        throw 'External Reference POM must not declare repositories or pluginRepositories.'
    }
    if (@($document.SelectNodes(
            '/m:project/m:dependencies/m:dependency/m:version',
            $namespace)).Count -ne 0) {
        throw 'External Reference dependencies must delegate all versions to the KOIKI BOM.'
    }
    if ($null -ne $document.SelectSingleNode(
            '/m:project/m:properties/m:java.version', $namespace)) {
        throw 'External Reference must inherit the Java baseline from the KOIKI Parent.'
    }

    $expectedDependencies = @(
        'com.github.ben-manes.caffeine:caffeine:compile'
        'org.flywaydb:flyway-database-postgresql:runtime'
        'org.jspecify:jspecify:compile'
        'org.koikifw:koiki-architecture-contract:compile'
        'org.koikifw:koiki-archunit-rules:test'
        'org.koikifw:koiki-starter-api:compile'
        'org.koikifw:koiki-starter-session-jdbc:compile'
        'org.koikifw:koiki-starter-web-mvc:compile'
        'org.koikifw:koiki-testing:test'
        'org.postgresql:postgresql:runtime'
        'org.springframework.boot:spring-boot-starter-cache:compile'
        'org.springframework.boot:spring-boot-starter-security-test:test'
        'org.springframework.boot:spring-boot-starter-test:test'
        'org.springframework.boot:spring-boot-starter-webmvc-test:test'
    ) | Sort-Object
    $actualDependencies = @($document.SelectNodes(
            '/m:project/m:dependencies/m:dependency', $namespace) | ForEach-Object {
            $groupId = $_.SelectSingleNode('m:groupId', $namespace).InnerText
            $artifactId = $_.SelectSingleNode('m:artifactId', $namespace).InnerText
            $scopeNode = $_.SelectSingleNode('m:scope', $namespace)
            $scope = if ($null -ne $scopeNode) { $scopeNode.InnerText } else { 'compile' }
            "${groupId}:${artifactId}:${scope}"
        }) | Sort-Object
    if (@(Compare-Object $expectedDependencies $actualDependencies).Count -ne 0) {
        throw 'External Reference dependencies differ from the approved Reference dependency set.'
    }
    $repackage = @($document.SelectNodes(
        '/m:project/m:build/m:plugins/m:plugin[m:groupId="org.springframework.boot" and ' +
        'm:artifactId="spring-boot-maven-plugin"]/m:executions/m:execution/m:goals/m:goal[text()="repackage"]',
        $namespace))
    if ($repackage.Count -ne 1) {
        throw 'External Reference POM must declare exactly one Spring Boot repackage goal.'
    }
}

$frameworkRoot = [System.IO.Path]::GetFullPath($FrameworkRepository)
$externalRoot = Get-FullPath -Path $ExternalProject
$baselinePath = Get-FullPath -Path $BaselineManifest
$baselineHashPath = Get-FullPath -Path $BaselineManifestSha256
$externalPom = Join-Path $externalRoot 'pom.xml'

if (-not (Test-Path -LiteralPath (Join-Path $frameworkRoot 'pom.xml') -PathType Leaf)) {
    throw 'FrameworkRepository does not contain the KOIKI root POM.'
}
if (-not (Test-Path -LiteralPath $externalPom -PathType Leaf)) {
    throw 'ExternalProject does not contain pom.xml.'
}

$frameworkCommit = Get-GitText -Repository $frameworkRoot -Arguments @('rev-parse', 'HEAD')
if ($frameworkCommit -cne $ExpectedFrameworkCommit.ToLowerInvariant()) {
    throw 'Framework HEAD does not match ExpectedFrameworkCommit.'
}
$baselineHash = Assert-ManifestHash -Manifest $baselinePath -Sidecar $baselineHashPath

if ($Mode -eq 'BaselinePreflight') {
    Assert-InitializrInventory -Project $externalRoot -Manifest $baselinePath
    Write-Host 'External Reference Initializr baseline preflight: PASS'
    Write-Host "Baseline manifest SHA-256: $baselineHash"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($StageRoot) -or
    [string]::IsNullOrWhiteSpace($ManifestOutput) -or
    [string]::IsNullOrWhiteSpace($ExternalSourceIdentity)) {
    throw 'Full mode requires StageRoot, ManifestOutput and ExternalSourceIdentity.'
}
if ($ExternalSourceIdentity -match '[\\/]' -or
    [string]::IsNullOrWhiteSpace($ExternalSourceIdentity)) {
    throw 'ExternalSourceIdentity must be a non-path, non-empty logical identity.'
}

Assert-TransformedPom -Pom $externalPom

$internalMainReferences = @(Get-ChildItem -LiteralPath (Join-Path $externalRoot 'src/main') `
        -Recurse -Filter '*.java' -File -ErrorAction Stop |
    Select-String -Pattern 'org[.]koikifw[.][A-Za-z0-9_.]*[.]internal(?:[.;]|$)').Count
if ($internalMainReferences -ne 0) {
    throw 'External Reference production source references a KOIKI internal package.'
}

$handoffScript = Join-Path $PSScriptRoot 'invoke-p4-ar6-r2-handoff.ps1'
if (-not (Test-Path -LiteralPath $handoffScript -PathType Leaf)) {
    throw 'P4-AR6 R2 handoff Tooling was not found.'
}

& $handoffScript `
    -ExpectedFrameworkCommit $ExpectedFrameworkCommit `
    -StageRoot (Get-FullPath -Path $StageRoot) `
    -ManifestOutput (Get-FullPath -Path $ManifestOutput) `
    -CustomerPom $externalPom `
    -CustomerSourceIdentity $ExternalSourceIdentity `
    -ExpectedFrameworkVersion $ExpectedFrameworkVersion `
    -FrameworkRepository $frameworkRoot
if ($LASTEXITCODE -ne 0) {
    throw "External Reference isolated verification failed with exit code $LASTEXITCODE."
}

Write-Host 'External Reference isolated boundary verification: PASS'

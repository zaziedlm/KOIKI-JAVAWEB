[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$starterPom = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-security/pom.xml'
$fixturePom = Join-Path $PSScriptRoot 'pom.xml'
$securityInventory = Join-Path $repositoryRoot 'koiki-starters/koiki-starter-security/public-api.txt'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) (
    'koiki-security-foundation-' + [System.Guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$productionTree = Join-Path $verificationRoot 'production-dependency-tree.txt'
$fixtureTree = Join-Path $verificationRoot 'fixture-dependency-tree.txt'
$fixtureTarget = Join-Path $PSScriptRoot 'target'

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

function Assert-NoSensitiveContent {
    param([Parameter(Mandatory)][System.IO.FileInfo[]]$Files)

    $forbiddenPatterns = [ordered]@{
        'fixture credential marker' = 'fixture-sensitive-credential-6f41'
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

function Assert-SecurityContract {
    param([Parameter(Mandatory)][System.IO.FileInfo]$FormalJar)

    $actualInventory = @(Get-Content -LiteralPath $securityInventory |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne '' -and -not $_.StartsWith('#') })
    $expectedInventory = @(
        'ARTIFACT koiki-starter-security',
        'PUBLIC_JAVA_TYPES 0',
        'PUBLIC_CONFIGURATION_PROPERTIES 0',
        'PUBLIC_SECURITY_ERROR_CODES 0',
        'INTERNAL_PACKAGE org.koikifw.starter.security.internal',
        'CUSTOMIZATION_BEAN koikiSecurityFallbackFilterChain',
        'CUSTOMIZATION_MODE COMPOSE_OR_EXPLICIT_REPLACE')
    $inventoryDifference = @(Compare-Object -ReferenceObject $expectedInventory `
        -DifferenceObject $actualInventory -SyncWindow 0)
    if ($inventoryDifference.Count -ne 0) {
        throw "Security Public API inventory differs from the P2-A1 contract: $inventoryDifference"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($FormalJar.FullName)
    try {
        $actualClasses = @($archive.Entries |
            Where-Object { $_.FullName.EndsWith('.class') } |
            Select-Object -ExpandProperty FullName |
            Sort-Object)
        $expectedClasses = @(
            'org/koikifw/starter/security/internal/KoikiSecurityAutoConfiguration.class',
            'org/koikifw/starter/security/internal/package-info.class')
        if (@(Compare-Object -ReferenceObject $expectedClasses -DifferenceObject $actualClasses).Count -ne 0) {
            throw "Formal Security JAR contains an unexpected Java type: $($actualClasses -join ', ')"
        }

        $configurationMetadata = @($archive.Entries | Where-Object {
            $_.FullName -in @(
                'META-INF/spring-configuration-metadata.json',
                'META-INF/additional-spring-configuration-metadata.json')
        })
        if ($configurationMetadata.Count -ne 0) {
            throw 'P2-A1 must not publish configuration property metadata.'
        }

        $importsEntry = $archive.GetEntry(
            'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports')
        if ($null -eq $importsEntry) {
            throw 'Security Auto Configuration imports resource is missing.'
        }
        $reader = [System.IO.StreamReader]::new($importsEntry.Open())
        try {
            $imports = $reader.ReadToEnd().Trim()
        } finally {
            $reader.Dispose()
        }
        if ($imports -ne 'org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration') {
            throw "Unexpected Security Auto Configuration import: $imports"
        }
    } finally {
        $archive.Dispose()
    }
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage the formal KOIKI release unit' -Arguments @(
        '-f', $rootPom, 'clean', 'install', '-DskipTests')

    $securityArtifactRoot = Join-Path $isolatedRepository (
        'org/koikifw/koiki-starter-security/0.1.0-SNAPSHOT')
    foreach ($requiredArtifact in @(
        'koiki-starter-security-0.1.0-SNAPSHOT.pom',
        'koiki-starter-security-0.1.0-SNAPSHOT.jar')) {
        if (-not (Test-Path -LiteralPath (Join-Path $securityArtifactRoot $requiredArtifact))) {
            throw "Formal Security artifact is missing: $requiredArtifact"
        }
    }

    Invoke-KoikiMaven -Label 'Verify the non-distributed Security fixture' -Arguments @(
        '-f', $fixturePom, 'clean', 'verify')

    Invoke-KoikiMaven -Label 'Record the production dependency tree' -Arguments @(
        '-f', $starterPom, 'dependency:tree', '-Dscope=runtime', "-DoutputFile=$productionTree")
    Invoke-KoikiMaven -Label 'Record the fixture dependency tree' -Arguments @(
        '-f', $fixturePom, 'dependency:tree', '-Dscope=test', "-DoutputFile=$fixtureTree")

    $productionDependencies = Get-Content -Raw -LiteralPath $productionTree
    $fixtureDependencies = Get-Content -Raw -LiteralPath $fixtureTree

    if ($productionDependencies -notmatch 'org[.]springframework[.]boot:spring-boot-starter-security:jar:') {
        throw 'Boot Security Starter is missing from the production dependency tree.'
    }
    foreach ($requiredTestDependency in @(
        'org.springframework.boot:spring-boot-starter-test:jar:',
        'org.springframework.boot:spring-boot-starter-security-test:jar:',
        'org.springframework.boot:spring-boot-starter-webmvc:jar:')) {
        if ($fixtureDependencies -notmatch [regex]::Escape($requiredTestDependency)) {
            throw "Required Boot-managed test dependency is missing: $requiredTestDependency"
        }
    }

    Write-Host '=== Resolved Boot-managed Security baseline ==='
    ($productionDependencies + "`n" + $fixtureDependencies) -split "`r?`n" |
        Select-String -Pattern '(spring-boot-starter-security|spring-boot-starter-test|spring-security-(config|core|crypto|test|web)):jar:' |
        ForEach-Object { Write-Host $_.Line.Trim() }

    $combinedDependencies = $productionDependencies + "`n" + $fixtureDependencies
    foreach ($forbiddenDependency in @(
        'spring-boot-starter-security-oauth2-authorization-server',
        'spring-security-saml2',
        'spring-session-data-redis',
        'spring-boot-starter-webflux',
        'io.projectreactor')) {
        if ($combinedDependencies -match [regex]::Escape($forbiddenDependency)) {
            throw "Deferred dependency leaked into P2-A1: $forbiddenDependency"
        }
    }

    if (Test-Path -LiteralPath (Join-Path $isolatedRepository (
        'org/koikifw/buildsupport/security-foundation-verification'))) {
        throw 'The non-distributed Security fixture was installed into the release repository.'
    }

    $fixtureJar = Get-Item -LiteralPath (Join-Path $fixtureTarget (
        'security-foundation-verification-0.1.0-SNAPSHOT.jar'))
    $formalJar = Get-Item -LiteralPath (Join-Path $securityArtifactRoot (
        'koiki-starter-security-0.1.0-SNAPSHOT.jar'))
    $reportFiles = @(Get-ChildItem -LiteralPath (Join-Path $fixtureTarget 'surefire-reports') -File)
    Assert-SecurityContract -FormalJar $formalJar
    Assert-NoSensitiveContent -Files (@($formalJar, $fixtureJar) + $reportFiles)

    Write-Host 'Security dependency and cumulative T0/T1/T2 verification succeeded.'
} finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Assert-SafeTemporaryPath -Path $verificationRoot
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

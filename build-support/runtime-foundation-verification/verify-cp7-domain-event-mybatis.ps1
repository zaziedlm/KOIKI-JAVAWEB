[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$wrapper = if ($IsWindows) { Join-Path $repositoryRoot 'mvnw.cmd' } else { Join-Path $repositoryRoot 'mvnw' }
$rootPom = Join-Path $repositoryRoot 'pom.xml'
$consumerRoot = Join-Path $repositoryRoot 'build-support/runtime-foundation-consumer'
$cp6Verification = Join-Path $PSScriptRoot 'verify-cp6-health-osiv.ps1'
$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$verificationRoot = Join-Path $temporaryRoot ("koiki-phase1b-cp7-" + [guid]::NewGuid().ToString('N'))
$isolatedRepository = Join-Path $verificationRoot 'repository'
$probePom = Join-Path $verificationRoot 'pom.xml'
$dependencyTree = Join-Path $verificationRoot 'mybatis-dependencies.txt'

function Assert-SafeTemporaryPath {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = [System.IO.Path]::GetFullPath($Path)
    $prefix = $temporaryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase) -or
        [System.IO.Path]::GetFileName($resolved) -notlike 'koiki-phase1b-cp7-*') {
        throw "Refusing to operate outside the CP7 OS temporary directory: $resolved"
    }
}

function Invoke-KoikiMaven {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host "=== $Label ==="
    & $wrapper --batch-mode --no-transfer-progress `
        "-Dmaven.repo.local=$isolatedRepository" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

Write-Host '=== Verify CP6 regression and CP7 Consumer integration ==='
& pwsh -NoProfile -File $cp6Verification
if ($LASTEXITCODE -ne 0) {
    throw "CP6 regression verification failed with exit code $LASTEXITCODE"
}

Assert-SafeTemporaryPath -Path $verificationRoot
New-Item -ItemType Directory -Path $isolatedRepository -Force | Out-Null

try {
    Invoke-KoikiMaven -Label 'Stage KOIKI BOM into an isolated repository' -Arguments @(
        '-f', $rootPom, '-pl', 'koiki-dependencies-bom', '-am', 'install', '-DskipTests'
    )

    $probe = @'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.koikifw.validation</groupId>
  <artifactId>cp7-mybatis-bom-probe</artifactId>
  <version>1.0.0</version>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.koikifw</groupId>
        <artifactId>koiki-dependencies-bom</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-starter</artifactId>
    </dependency>
  </dependencies>
</project>
'@
    [System.IO.File]::WriteAllText($probePom, $probe, [System.Text.UTF8Encoding]::new($false))

    Invoke-KoikiMaven -Label 'Resolve the versionless MyBatis Starter BOM probe' -Arguments @(
        '-f', $probePom, 'dependency:tree', "-DoutputFile=$dependencyTree"
    )
    $resolvedDependencies = Get-Content -Raw -LiteralPath $dependencyTree
    if ($resolvedDependencies -notmatch
        'org[.]mybatis[.]spring[.]boot:mybatis-spring-boot-starter:jar:4[.]1[.]0') {
        throw 'KOIKI BOM did not resolve MyBatis Spring Boot Starter to 4.1.0.'
    }

    $productionPoms = Get-ChildItem -LiteralPath $repositoryRoot -Recurse -Filter 'pom.xml' |
        Where-Object { $_.FullName -notlike '*target*' -and
            $_.FullName -ne (Join-Path $repositoryRoot 'koiki-dependencies-bom/pom.xml') }
    $mybatisDeclarations = @($productionPoms | Select-String -Pattern 'org[.]mybatis')
    if ($mybatisDeclarations.Count -ne 0) {
        throw "MyBatis was declared outside dependency management: $($mybatisDeclarations.Path -join ', ')"
    }

    $workreviewSources = Get-ChildItem -LiteralPath (Join-Path $consumerRoot 'workreview/src/main/java') `
        -Recurse -Filter '*.java'
    $forbiddenSenderReferences = @($workreviewSources | Select-String `
        -Pattern 'runtimeconsumer[.]workitem[.](application|adapter|configuration)')
    if ($forbiddenSenderReferences.Count -ne 0) {
        throw 'workreview directly references workitem internals instead of its domain event.'
    }

    $workitemJar = Join-Path $consumerRoot 'workitem/target/workitem-feature-0.1.0-SNAPSHOT.jar'
    $workreviewJar = Join-Path $consumerRoot 'workreview/target/workreview-feature-0.1.0-SNAPSHOT.jar'
    foreach ($artifact in @($workitemJar, $workreviewJar)) {
        if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) {
            throw "CP7 Consumer artifact was not found: $artifact"
        }
        $entries = @(& jar tf $artifact)
        if ($entries -match 'KoikiDomainEventDetectionStrategy|org/springframework/modulith') {
            throw "Test-scope Spring Modulith configuration leaked into $artifact"
        }
    }

    Write-Host 'CP7 domain event, named-interface, MyBatis BOM, regression, and boundary checks succeeded.'
} finally {
    Assert-SafeTemporaryPath -Path $verificationRoot
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

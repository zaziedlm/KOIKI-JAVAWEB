[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$verificationRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'verification'))
$generatedRoot = [System.IO.Path]::GetFullPath((Join-Path $verificationRoot 'generated'))
$expectedPrefix = $verificationRoot.TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar

if (-not $generatedRoot.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Generated path escaped the verification directory: $generatedRoot"
}

if (Test-Path -LiteralPath $generatedRoot) {
    Remove-Item -LiteralPath $generatedRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $generatedRoot | Out-Null

$generator = Join-Path $PSScriptRoot 'GenerateFeature.java'
$commonArguments = @(
    '--base-package=org.koikifw.templateverification',
    '--parent-group-id=org.koikifw.templateverification',
    '--parent-artifact-id=feature-template-verification',
    '--parent-version=0.1.0-SNAPSHOT',
    '--parent-relative-path=../../pom.xml'
)

Push-Location $repositoryRoot
try {
    & java $generator @commonArguments `
        '--tier=tier1-simple' `
        '--module-name=catalog' `
        '--class-name=CatalogItem' `
        '--artifact-id=catalog-feature' `
        "--output=$(Join-Path $generatedRoot 'catalog')"
    if ($LASTEXITCODE -ne 0) {
        throw "Tier 1 Feature Template generation failed with exit code $LASTEXITCODE"
    }

    & java $generator @commonArguments `
        '--tier=tier2-rich' `
        '--module-name=approval' `
        '--class-name=ApprovalRequest' `
        '--artifact-id=approval-feature' `
        "--output=$(Join-Path $generatedRoot 'approval')"
    if ($LASTEXITCODE -ne 0) {
        throw "Tier 2 Feature Template generation failed with exit code $LASTEXITCODE"
    }

    $wrapper = if ($IsWindows) {
        Join-Path $repositoryRoot 'mvnw.cmd'
    } else {
        Join-Path $repositoryRoot 'mvnw'
    }

    & $wrapper --batch-mode --no-transfer-progress `
        -f (Join-Path $verificationRoot 'pom.xml') clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Generated Feature Template verification failed with exit code $LASTEXITCODE"
    }

    $runtimeTree = Join-Path $verificationRoot 'target/runtime-dependency-tree.txt'
    & $wrapper --batch-mode --no-transfer-progress `
        -f (Join-Path $verificationRoot 'pom.xml') `
        -pl architecture-tests -am dependency:tree `
        -Dscope=runtime "-DoutputFile=$runtimeTree"
    if ($LASTEXITCODE -ne 0) {
        throw "Runtime dependency inspection failed with exit code $LASTEXITCODE"
    }

    $runtimeDependencies = Get-Content -Raw -LiteralPath $runtimeTree
    if ($runtimeDependencies -match 'org[.]springframework[.]modulith') {
        throw 'Spring Modulith must remain test scope and must not appear in the runtime dependency tree.'
    }
} finally {
    Pop-Location
}

Write-Host 'Tier 1 / Tier 2 Feature Template verification succeeded.'

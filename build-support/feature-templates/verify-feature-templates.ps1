[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$verificationRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'verification'))
$verificationPom = Join-Path $verificationRoot 'pom.xml'
$generatedRoot = [System.IO.Path]::GetFullPath((Join-Path $verificationRoot 'generated'))
$expectedPrefix = $verificationRoot.TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)

if (-not $generatedRoot.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Generated path escaped the verification directory: $generatedRoot"
}

$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$generator = Join-Path $PSScriptRoot 'GenerateFeature.java'
$commonArguments = @(
    '--base-package=org.koikifw.templateverification',
    '--parent-group-id=org.koikifw.templateverification',
    '--parent-artifact-id=feature-template-verification',
    '--parent-version=0.1.0-SNAPSHOT',
    '--parent-relative-path=../../pom.xml'
)
$expectedNullMarkedPackages = @(
    'org.koikifw.templateverification.catalog',
    'org.koikifw.templateverification.catalog.application.usecase',
    'org.koikifw.templateverification.catalog.adapter.outbound.persistence',
    'org.koikifw.templateverification.approval',
    'org.koikifw.templateverification.approval.application.usecase',
    'org.koikifw.templateverification.approval.domain.model',
    'org.koikifw.templateverification.approval.domain.repository'
)

function Get-JavaCommand {
    $pathCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($null -ne $pathCommand) {
        return $pathCommand.Source
    }

    foreach ($variableName in @('JAVA_HOME', 'JAVA21_HOME')) {
        $javaHome = [Environment]::GetEnvironmentVariable($variableName)
        if ([string]::IsNullOrWhiteSpace($javaHome)) {
            continue
        }
        $executable = if ($IsWindows) { 'java.exe' } else { 'java' }
        $candidate = Join-Path $javaHome "bin/$executable"
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    throw 'JDK 21 java command was not found in PATH, JAVA_HOME, or JAVA21_HOME.'
}

$javaCommand = Get-JavaCommand

function Assert-GeneratedNullMarkedPackages {
    foreach ($packageName in $expectedNullMarkedPackages) {
        $relativePackage = $packageName.Substring('org.koikifw.templateverification.'.Length)
        $moduleName = $relativePackage.Split('.')[0]
        $packagePath = $packageName.Replace('.', '/')
        $packageInfo = Join-Path $generatedRoot `
            "$moduleName/src/main/java/$packagePath/package-info.java"

        if (-not (Test-Path -LiteralPath $packageInfo -PathType Leaf)) {
            throw "Generated production package is missing package-info.java: $packageName"
        }

        $content = [System.IO.File]::ReadAllText($packageInfo)
        if (-not [regex]::IsMatch($content, '(?m)^@NullMarked\r?$')) {
            throw "Generated production package is missing @NullMarked: $packageName"
        }
        if (-not $content.Contains(
                "package $packageName;",
                [System.StringComparison]::Ordinal)) {
            throw "Generated package-info.java has an unexpected package declaration: $packageName"
        }
    }
}

function Reset-GeneratedFeatures {
    if (Test-Path -LiteralPath $generatedRoot) {
        Remove-Item -LiteralPath $generatedRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Path $generatedRoot | Out-Null

    & $javaCommand $generator @commonArguments `
        '--tier=tier1-simple' `
        '--module-name=catalog' `
        '--class-name=CatalogItem' `
        '--artifact-id=catalog-feature' `
        "--output=$(Join-Path $generatedRoot 'catalog')"
    if ($LASTEXITCODE -ne 0) {
        throw "Tier 1 Feature Template generation failed with exit code $LASTEXITCODE"
    }

    & $javaCommand $generator @commonArguments `
        '--tier=tier2-rich' `
        '--module-name=approval' `
        '--class-name=ApprovalRequest' `
        '--artifact-id=approval-feature' `
        "--output=$(Join-Path $generatedRoot 'approval')"
    if ($LASTEXITCODE -ne 0) {
        throw "Tier 2 Feature Template generation failed with exit code $LASTEXITCODE"
    }

    Assert-GeneratedNullMarkedPackages
}

function Invoke-VerificationBuild {
    $output = @(& $wrapper `
        --batch-mode `
        --no-transfer-progress `
        -f $verificationPom `
        clean verify 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output = $output -join [Environment]::NewLine
    }
}

function Assert-BuildSucceeded {
    param(
        [psobject]$Result,
        [string]$Label
    )

    if ($Result.ExitCode -ne 0) {
        throw "$Label failed with exit code $($Result.ExitCode)."
    }
}

function Assert-ExpectedFailure {
    param(
        [psobject]$Result,
        [string]$Label,
        [string[]]$RequiredDiagnostics
    )

    if ($Result.ExitCode -eq 0) {
        throw "$Label unexpectedly succeeded."
    }
    foreach ($diagnostic in $RequiredDiagnostics) {
        if (-not $Result.Output.Contains($diagnostic, [System.StringComparison]::Ordinal)) {
            throw "$Label did not contain the required diagnostic: $diagnostic"
        }
    }
}

function Write-GeneratedText {
    param(
        [string]$Path,
        [string]$Content
    )

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $fullPath.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Negative fixture path escaped the verification directory: $fullPath"
    }
    [System.IO.File]::WriteAllText($fullPath, $Content, $utf8WithoutBom)
}

function Remove-GeneratedModuleDeclaration {
    param(
        [string]$ModuleName
    )

    $packageInfo = Join-Path $generatedRoot "$ModuleName/src/main/java/org/koikifw/templateverification/$ModuleName/package-info.java"
    $content = [System.IO.File]::ReadAllText($packageInfo)
    $annotation = [regex]::new('(?s)@KoikiModule\(.*?\)\r?\npackage ')
    $modified = $annotation.Replace($content, 'package ', 1)
    if ($modified -eq $content) {
        throw "Could not inject the ArchUnit negative fixture into $packageInfo"
    }
    Write-GeneratedText -Path $packageInfo -Content $modified
}

function Insert-NullReturn {
    param(
        [string]$Path,
        [string]$ExpectedReturn
    )

    $content = [System.IO.File]::ReadAllText($Path)
    $modified = $content.Replace($ExpectedReturn, '        return null;')
    if ($modified -eq $content) {
        throw "Could not inject the NullAway negative fixture into $Path"
    }
    Write-GeneratedText -Path $Path -Content $modified
}

function Assert-RuntimeDependencyBoundary {
    $runtimeTree = Join-Path $verificationRoot 'target/runtime-dependency-tree.txt'
    & $wrapper `
        --batch-mode `
        --no-transfer-progress `
        -f $verificationPom `
        -pl architecture-tests -am dependency:tree `
        -Dscope=runtime "-DoutputFile=$runtimeTree"
    if ($LASTEXITCODE -ne 0) {
        throw "Runtime dependency inspection failed with exit code $LASTEXITCODE"
    }

    $runtimeDependencies = Get-Content -Raw -LiteralPath $runtimeTree
    if ($runtimeDependencies -match 'org[.]springframework[.]modulith') {
        throw 'Spring Modulith must remain test scope and must not appear in the runtime dependency tree.'
    }
}

Push-Location $repositoryRoot
try {
    Write-Host '=== Feature Template positive verification ==='
    Reset-GeneratedFeatures
    $positive = Invoke-VerificationBuild
    Assert-BuildSucceeded -Result $positive -Label 'Positive Feature Template verification'

    Write-Host '=== Tier 1 ArchUnit negative verification (expected failure) ==='
    Reset-GeneratedFeatures
    Remove-GeneratedModuleDeclaration -ModuleName 'catalog'
    $tierOneArchitectureNegative = Invoke-VerificationBuild
    Assert-ExpectedFailure `
        -Result $tierOneArchitectureNegative `
        -Label 'Tier 1 ArchUnit negative verification' `
        -RequiredDiagnostics @('[KOIKI-ARCH-007]', '[KOIKI-ARCH-008]', 'catalog')

    Write-Host '=== Tier 2 ArchUnit negative verification (expected failure) ==='
    Reset-GeneratedFeatures
    Remove-GeneratedModuleDeclaration -ModuleName 'approval'
    $tierTwoArchitectureNegative = Invoke-VerificationBuild
    Assert-ExpectedFailure `
        -Result $tierTwoArchitectureNegative `
        -Label 'Tier 2 ArchUnit negative verification' `
        -RequiredDiagnostics @('[KOIKI-ARCH-007]', '[KOIKI-ARCH-008]', 'approval')

    Write-Host '=== Tier 1 NullAway negative verification (expected failure) ==='
    Reset-GeneratedFeatures
    $tierOneModel = Join-Path $generatedRoot `
        'catalog/src/main/java/org/koikifw/templateverification/catalog/adapter/outbound/persistence/CatalogItem.java'
    Insert-NullReturn -Path $tierOneModel -ExpectedReturn '        return label;'
    $tierOneNullNegative = Invoke-VerificationBuild
    Assert-ExpectedFailure `
        -Result $tierOneNullNegative `
        -Label 'Tier 1 NullAway negative verification' `
        -RequiredDiagnostics @(
            '[NullAway]',
            'returning @Nullable expression from method with @NonNull return type',
            'CatalogItem.java')

    Write-Host '=== Tier 2 NullAway negative verification (expected failure) ==='
    Reset-GeneratedFeatures
    $tierTwoModel = Join-Path $generatedRoot `
        'approval/src/main/java/org/koikifw/templateverification/approval/domain/model/ApprovalRequest.java'
    Insert-NullReturn -Path $tierTwoModel -ExpectedReturn '        return description;'
    $tierTwoNullNegative = Invoke-VerificationBuild
    Assert-ExpectedFailure `
        -Result $tierTwoNullNegative `
        -Label 'Tier 2 NullAway negative verification' `
        -RequiredDiagnostics @(
            '[NullAway]',
            'returning @Nullable expression from method with @NonNull return type',
            'ApprovalRequest.java')
} finally {
    Reset-GeneratedFeatures
    Pop-Location
}

Push-Location $repositoryRoot
try {
    Write-Host '=== Feature Template restore verification ==='
    $restore = Invoke-VerificationBuild
    Assert-BuildSucceeded -Result $restore -Label 'Restored Feature Template verification'
    Assert-RuntimeDependencyBoundary
} finally {
    Pop-Location
}

Write-Host 'Feature Template positive, Tier-specific negative, and restore verification succeeded.'

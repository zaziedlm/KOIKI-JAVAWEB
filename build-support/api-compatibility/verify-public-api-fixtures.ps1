[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$verificationPom = Join-Path $PSScriptRoot 'pom.xml'
$inventoryTool = Join-Path $PSScriptRoot 'PublicApiInventory.java'
$fixtureRoot = Join-Path $PSScriptRoot 'fixture'
$expectedInventory = Join-Path $fixtureRoot 'public-api.txt'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$temporaryRoot = Join-Path (
    [System.IO.Path]::GetTempPath()) (
    'koiki-phase1a-api-fixtures-' + [guid]::NewGuid().ToString('N'))
$localRepository = Join-Path $temporaryRoot 'repository'
$jarDirectory = Join-Path $temporaryRoot 'jars'
$reportDirectory = Join-Path $temporaryRoot 'reports'

function Invoke-Tool {
    param(
        [Parameter(Mandatory)]
        [string]$Command,
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [Parameter(Mandatory)]
        [string]$Label
    )

    $output = @(& $Command @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "$Label failed with exit code $exitCode."
    }
}

function Build-FixtureJar {
    param(
        [Parameter(Mandatory)]
        [string]$FixtureName
    )

    $sourceRoot = Join-Path $fixtureRoot "$FixtureName/src"
    $classes = Join-Path $temporaryRoot "$FixtureName/classes"
    $jarPath = Join-Path $jarDirectory "$FixtureName.jar"
    $sourceFiles = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' |
            Sort-Object FullName |
            ForEach-Object { $_.FullName })
    if ($sourceFiles.Count -eq 0) {
        throw "Fixture source is missing: $FixtureName"
    }

    New-Item -ItemType Directory -Path $classes | Out-Null
    Invoke-Tool -Command 'javac' -Label "$FixtureName fixture compilation" -Arguments (@(
            '--release', '21',
            '-d', $classes
        ) + $sourceFiles)
    Invoke-Tool -Command 'jar' -Label "$FixtureName fixture packaging" -Arguments @(
        '--create',
        '--file', $jarPath,
        '-C', $classes,
        '.'
    )
    return $jarPath
}

function Invoke-Comparison {
    param(
        [Parameter(Mandatory)]
        [string]$Label,
        [Parameter(Mandatory)]
        [string]$OldJar,
        [Parameter(Mandatory)]
        [string]$NewJar
    )

    $comparisonReport = Join-Path $reportDirectory $Label
    $output = @(& $wrapper `
            --batch-mode `
            --no-transfer-progress `
            "-Dmaven.repo.local=$localRepository" `
            '-f' $verificationPom `
            '-Pfixture' `
            'verify' `
            "-Djapicmp.oldJar=$OldJar" `
            "-Djapicmp.newJar=$NewJar" `
            "-Djapicmp.outputDirectory=$comparisonReport" 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }

    $reportEvidence = if (Test-Path -LiteralPath $comparisonReport) {
        @(Get-ChildItem -LiteralPath $comparisonReport -File |
                Where-Object { $_.Extension -in @('.diff', '.xml') } |
                Sort-Object Name |
                ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) })
    } else {
        @()
    }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output = @($output + $reportEvidence) -join [Environment]::NewLine
    }
}

function Get-Inventory {
    param(
        [Parameter(Mandatory)]
        [string]$JarPath
    )

    $output = @(& java --class-path $JarPath $inventoryTool 'fixture' $JarPath 2>&1)
    if ($LASTEXITCODE -ne 0) {
        $output | ForEach-Object { Write-Host $_ }
        throw "Fixture inventory generation failed with exit code $LASTEXITCODE."
    }
    return ($output -join "`n") + "`n"
}

function Assert-ExpectedFailure {
    param(
        [Parameter(Mandatory)]
        [psobject]$Result,
        [Parameter(Mandatory)]
        [string]$Label,
        [Parameter(Mandatory)]
        [string[]]$RequiredDiagnostics
    )

    if ($Result.ExitCode -eq 0) {
        throw "$Label unexpectedly succeeded."
    }
    foreach ($diagnostic in $RequiredDiagnostics) {
        if (-not $Result.Output.Contains($diagnostic, [System.StringComparison]::Ordinal)) {
            throw "$Label did not contain required diagnostic: $diagnostic"
        }
    }
}

try {
    New-Item -ItemType Directory -Path $localRepository | Out-Null
    New-Item -ItemType Directory -Path $jarDirectory | Out-Null
    New-Item -ItemType Directory -Path $reportDirectory | Out-Null

    $baselineJar = Build-FixtureJar -FixtureName 'baseline'
    $compatibleJar = Build-FixtureJar -FixtureName 'compatible'
    $breakingJar = Build-FixtureJar -FixtureName 'breaking'
    $additionJar = Build-FixtureJar -FixtureName 'addition'

    $expectedInventoryText = [System.IO.File]::ReadAllText(
        $expectedInventory).Replace("`r`n", "`n")
    $baselineInventory = Get-Inventory -JarPath $baselineJar
    $compatibleInventory = Get-Inventory -JarPath $compatibleJar
    if ($baselineInventory -ne $expectedInventoryText) {
        throw 'Baseline fixture inventory differs from the approved fixture inventory.'
    }
    if ($compatibleInventory -ne $expectedInventoryText) {
        throw 'Package-private fixture change affected the Public API inventory.'
    }

    $compatible = Invoke-Comparison `
        -Label 'compatible' `
        -OldJar $baselineJar `
        -NewJar $compatibleJar
    if ($compatible.ExitCode -ne 0) {
        throw "Package-private compatible fixture failed with exit code $($compatible.ExitCode)."
    }

    $breaking = Invoke-Comparison `
        -Label 'breaking' `
        -OldJar $baselineJar `
        -NewJar $breakingJar
    Assert-ExpectedFailure `
        -Result $breaking `
        -Label 'Public breaking fixture' `
        -RequiredDiagnostics @('METHOD_RETURN_TYPE_CHANGED')

    $additionInventory = Get-Inventory -JarPath $additionJar
    if ($additionInventory -eq $expectedInventoryText) {
        throw 'Unapproved Public API addition unexpectedly matched the approved inventory.'
    }
    $addedSignature = 'PublicContract#added()'
    if (-not $additionInventory.Contains($addedSignature, [System.StringComparison]::Ordinal)) {
        throw "Addition fixture inventory did not contain expected signature: $addedSignature"
    }

    $addition = Invoke-Comparison `
        -Label 'addition' `
        -OldJar $baselineJar `
        -NewJar $additionJar
    Assert-ExpectedFailure `
        -Result $addition `
        -Label 'Unapproved Public API addition fixture' `
        -RequiredDiagnostics @('METHOD_ADDED_TO_PUBLIC_CLASS')

    Write-Output 'C3 Gate 3 Public API fixture verification: SUCCESS'
    Write-Output 'Package-private implementation change | inventory=MATCH | japicmp modifications=NONE | exit=0'
    Write-Output 'Public return type change | japicmp METHOD_RETURN_TYPE_CHANGED | expected failure=PASS'
    Write-Output 'Unapproved public addition | inventory=MISMATCH | japicmp METHOD_ADDED_TO_PUBLIC_CLASS | expected failure=PASS'
} finally {
    if (Test-Path -LiteralPath $temporaryRoot) {
        $resolvedTemporaryRoot = (Resolve-Path -LiteralPath $temporaryRoot).Path
        $directorySeparators = @(
            [System.IO.Path]::DirectorySeparatorChar,
            [System.IO.Path]::AltDirectorySeparatorChar)
        $systemTemp = [System.IO.Path]::GetFullPath(
            [System.IO.Path]::GetTempPath()).TrimEnd($directorySeparators)
        $temporaryPrefix = $systemTemp + [System.IO.Path]::DirectorySeparatorChar
        $pathComparison = if ($IsWindows) {
            [System.StringComparison]::OrdinalIgnoreCase
        } else {
            [System.StringComparison]::Ordinal
        }
        if (-not $resolvedTemporaryRoot.StartsWith($temporaryPrefix, $pathComparison)) {
            throw "Refusing to remove unexpected temporary path: $resolvedTemporaryRoot"
        }
        Remove-Item -Recurse -Force -LiteralPath $resolvedTemporaryRoot
    }
}

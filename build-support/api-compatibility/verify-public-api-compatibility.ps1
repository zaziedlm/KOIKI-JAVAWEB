[CmdletBinding()]
param(
    [string]$GitHubUser = 'zaziedlm',
    [switch]$GitHubActions
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Resolve-JdkTool {
    param([Parameter(Mandatory)][string]$Name)

    $toolName = if ($IsWindows) { "$Name.exe" } else { $Name }
    $tool = if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME "bin/$toolName"
    } else {
        (Get-Command $toolName -ErrorAction Stop).Source
    }
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "JDK tool was not found: $tool"
    }
    return $tool
}

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$verificationPom = Join-Path $PSScriptRoot 'pom.xml'
$inventoryTool = Join-Path $PSScriptRoot 'PublicApiInventory.java'
$expectedInventory = Join-Path $PSScriptRoot 'public-api.txt'
$wrapper = if ($IsWindows) {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
$javaTool = Resolve-JdkTool -Name 'java'
$temporaryRoot = Join-Path (
    [System.IO.Path]::GetTempPath()) (
    'koiki-phase1a-api-compatibility-' + [guid]::NewGuid().ToString('N'))
$localRepository = Join-Path $temporaryRoot 'repository'
$baselineDirectory = Join-Path $temporaryRoot 'baseline'
$reportDirectory = Join-Path $temporaryRoot 'reports'
$expectedTimestamp = '0.1.0-20260826.091429-1'
$baselineVersion = '0.1.0-c1-baseline'
$currentVersion = '0.1.0-SNAPSHOT'
$packageRoot = 'https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB'
$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)

$artifacts = @(
    [pscustomobject]@{
        Name = 'Architecture Contract'
        ArtifactId = 'koiki-architecture-contract'
        Sha256 = '947EE8CF0E109FE58D81E6008A56C06C8F4C035FF76BDF462F8F6BD9BB50DE45'
        Profile = 'architecture-contract'
    },
    [pscustomobject]@{
        Name = 'ArchUnit Rules'
        ArtifactId = 'koiki-archunit-rules'
        Sha256 = 'A51E26E7386D19E53C18BD63BC4E4F95EC1EAE471F39D519D6AE0CBC7C2DF3F2'
        Profile = 'archunit-rules'
    }
)

function Invoke-Maven {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [Parameter(Mandatory)]
        [string]$Label
    )

    $output = @(& $wrapper `
            --batch-mode `
            --no-transfer-progress `
            "-Dmaven.repo.local=$localRepository" `
            @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "$Label failed with exit code $exitCode."
    }
}

function Install-BaselineJar {
    param(
        [Parameter(Mandatory)]
        [psobject]$Artifact,
        [Parameter(Mandatory)]
        [string]$JarPath
    )

    Invoke-Maven -Label "$($Artifact.Name) baseline classpath installation" -Arguments @(
        '-f', $verificationPom,
        'org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file',
        "-Dfile=$JarPath",
        '-DgroupId=org.koikifw',
        "-DartifactId=$($Artifact.ArtifactId)",
        "-Dversion=$baselineVersion",
        '-Dpackaging=jar',
        '-DgeneratePom=true'
    )
}

$token = $env:KOIKI_PACKAGES_TOKEN
$basicCredential = $null
if ([string]::IsNullOrWhiteSpace($token)) {
    if ($GitHubActions) {
        throw 'The GitHub Actions GITHUB_TOKEN was not supplied through KOIKI_PACKAGES_TOKEN.'
    }
    $secureToken = Read-Host 'PAT classic with read:packages only' -AsSecureString
    $token = [System.Net.NetworkCredential]::new('', $secureToken).Password
}
if ([string]::IsNullOrWhiteSpace($token)) {
    throw 'A PAT classic with read:packages only is required.'
}
if ([string]::IsNullOrWhiteSpace($GitHubUser)) {
    throw 'The GitHub user name is required for package authentication.'
}

try {
    if ($GitHubActions) {
        if ($env:GITHUB_ACTIONS -ne 'true') {
            throw 'The GitHubActions switch may only be used on a GitHub Actions runner.'
        }
        $authentication = 'GITHUB_TOKEN with workflow packages: read'
    } else {
        $userResponse = Invoke-WebRequest `
            -UseBasicParsing `
            -Uri 'https://api.github.com/user' `
            -Headers @{
                Accept = 'application/vnd.github+json'
                Authorization = "Bearer $token"
                'X-GitHub-Api-Version' = '2022-11-28'
            }
        $scopeHeader = [string]$userResponse.Headers['X-OAuth-Scopes']
        $scopes = @($scopeHeader -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        if ($scopes.Count -ne 1 -or $scopes[0] -ne 'read:packages') {
            throw "PAT classic must have read:packages only; reported scopes: $($scopes -join ', ')"
        }
        $authentication = "PAT classic scopes: $($scopes -join ', ')"
    }

    New-Item -ItemType Directory -Path $localRepository | Out-Null
    New-Item -ItemType Directory -Path $baselineDirectory | Out-Null
    New-Item -ItemType Directory -Path $reportDirectory | Out-Null

    $basicCredential = [Convert]::ToBase64String(
        [Text.Encoding]::UTF8.GetBytes("${GitHubUser}:$token"))
    $hashResults = foreach ($artifact in $artifacts) {
        $fileName = "$($artifact.ArtifactId)-$expectedTimestamp.jar"
        $jarPath = Join-Path $baselineDirectory $fileName
        $uri = "$packageRoot/org/koikifw/$($artifact.ArtifactId)/$currentVersion/$fileName"
        Invoke-WebRequest `
            -UseBasicParsing `
            -Uri $uri `
            -Headers @{ Authorization = "Basic $basicCredential" } `
            -OutFile $jarPath

        $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $jarPath).Hash
        if ($actualHash -ne $artifact.Sha256) {
            throw "SHA-256 mismatch for $($artifact.Name): $actualHash"
        }
        Install-BaselineJar -Artifact $artifact -JarPath $jarPath

        [pscustomobject]@{
            Artifact = $artifact.Name
            Timestamp = $expectedTimestamp
            Sha256 = $actualHash
            Result = 'MATCH'
        }
    }
    $basicCredential = $null

    Invoke-Maven -Label 'Current Public API artifact build' -Arguments @(
        '-f', (Join-Path $repositoryRoot 'pom.xml'),
        '-pl', 'koiki-architecture-contract,koiki-archunit-rules',
        '-am',
        'clean',
        'install'
    )

    $currentContract = Join-Path $repositoryRoot (
        "koiki-architecture-contract/target/koiki-architecture-contract-$currentVersion.jar")
    $currentRules = Join-Path $repositoryRoot (
        "koiki-archunit-rules/target/koiki-archunit-rules-$currentVersion.jar")
    $archUnitJar = Join-Path $localRepository (
        'com/tngtech/archunit/archunit/1.5.0/archunit-1.5.0.jar')
    $jSpecifyJar = Join-Path $localRepository (
        'org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar')
    foreach ($requiredPath in @($currentContract, $currentRules, $archUnitJar, $jSpecifyJar)) {
        if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
            throw "Required current artifact or classpath entry is missing: $requiredPath"
        }
    }

    $classPath = @($currentContract, $currentRules, $archUnitJar, $jSpecifyJar) -join (
        [System.IO.Path]::PathSeparator)
    $inventoryOutput = @(& $javaTool '-Xshare:off' --class-path $classPath $inventoryTool `
            'koiki-architecture-contract' $currentContract `
            'koiki-archunit-rules' $currentRules 2>&1)
    if ($LASTEXITCODE -ne 0) {
        $inventoryOutput | ForEach-Object { Write-Host $_ }
        throw "Public API inventory generation failed with exit code $LASTEXITCODE."
    }
    $actualInventory = ($inventoryOutput -join "`n") + "`n"
    $expectedInventoryText = [System.IO.File]::ReadAllText($expectedInventory).Replace("`r`n", "`n")
    if ($actualInventory -ne $expectedInventoryText) {
        $actualInventoryPath = Join-Path $temporaryRoot 'public-api.actual.txt'
        [System.IO.File]::WriteAllText($actualInventoryPath, $actualInventory, $utf8WithoutBom)
        throw 'Public API inventory differs from public-api.txt.'
    }

    $comparisonResults = foreach ($artifact in $artifacts) {
        $baselineJar = Join-Path $baselineDirectory (
            "$($artifact.ArtifactId)-$expectedTimestamp.jar")
        $currentJar = Join-Path $repositoryRoot (
            "$($artifact.ArtifactId)/target/$($artifact.ArtifactId)-$currentVersion.jar")
        $artifactReportDirectory = Join-Path $reportDirectory $artifact.ArtifactId

        Invoke-Maven -Label "$($artifact.Name) japicmp comparison" -Arguments @(
            '-f', $verificationPom,
            "-P$($artifact.Profile)",
            'verify',
            "-Djapicmp.oldJar=$baselineJar",
            "-Djapicmp.newJar=$currentJar",
            "-Djapicmp.outputDirectory=$artifactReportDirectory"
        )

        $diffReport = Join-Path $artifactReportDirectory "$($artifact.Profile).diff"
        $xmlReport = Join-Path $artifactReportDirectory "$($artifact.Profile).xml"
        foreach ($report in @($diffReport, $xmlReport)) {
            if (-not (Test-Path -LiteralPath $report -PathType Leaf)) {
                throw "Expected japicmp report is missing: $report"
            }
        }

        [pscustomobject]@{
            Artifact = $artifact.Name
            Access = 'public'
            Modifications = 'NONE'
            ExitCode = 0
        }
    }

    Write-Output 'C3 Gate 2 Public API compatibility: SUCCESS'
    Write-Output "Authentication: $authentication"
    Write-Output 'Baseline identity:'
    $hashResults | ForEach-Object {
        Write-Output "$($_.Artifact) | $($_.Timestamp) | $($_.Sha256) | $($_.Result)"
    }
    Write-Output 'Inventory: MATCH (5 public types; 4 annotation elements; 2 Rules methods)'
    Write-Output 'japicmp 0.26.1 comparisons:'
    $comparisonResults | ForEach-Object {
        Write-Output "$($_.Artifact) | access=$($_.Access) | modifications=$($_.Modifications) | exit=$($_.ExitCode)"
    }
} finally {
    $env:KOIKI_PACKAGES_TOKEN = $null
    $token = $null
    $basicCredential = $null

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

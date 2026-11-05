param(
    [Parameter(Mandatory=$true)][string]$NodeDirectory,
    [switch]$RunDirectTests
)
$ErrorActionPreference = 'Stop'
$taskRepository = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$taskNode = (Resolve-Path -LiteralPath $NodeDirectory).Path
$taskPackage = Get-Content (Join-Path $taskRepository 'koiki-reference-app/frontend/package.json') -Raw | ConvertFrom-Json
if ((& (Join-Path $taskNode 'node.exe') --version) -ne "v$($taskPackage.engines.node)") { throw 'Node version differs from frontend/package.json' }
if ((& (Join-Path $taskNode 'npm.cmd') --version) -ne $taskPackage.engines.npm) { throw 'npm version differs from frontend/package.json' }
$taskOldPath = $env:PATH
$taskOldBase = $env:MAVEN_BASEDIR
try {
    $env:PATH = "$taskNode;$taskOldPath"
    $env:MAVEN_BASEDIR = $taskRepository
    $taskArguments = @('-B','-ntp','-f',(Join-Path $taskRepository 'pom.xml'),'-pl','koiki-reference-app','-am',
        '-P','b1-session-spa,d3-container','clean','package')
    if ($RunDirectTests) {
        $taskArguments += @('-Dtest=ReferenceContainerSecurityConfigurationTest,ReferenceSessionApiSecurityTest,ReferenceSecurityConfigurationTest',
            '-Dsurefire.failIfNoSpecifiedTests=false','-DargLine=-Xmx768m')
    } else { $taskArguments += '-DskipTests' }
    & (Join-Path $taskRepository 'mvnw.cmd') @taskArguments
    if ($LASTEXITCODE -ne 0) { throw "Reference build failed: $LASTEXITCODE" }
} finally {
    $env:PATH = $taskOldPath
    $env:MAVEN_BASEDIR = $taskOldBase
}

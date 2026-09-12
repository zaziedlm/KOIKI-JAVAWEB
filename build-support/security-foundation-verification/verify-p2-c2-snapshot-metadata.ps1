[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

. (Join-Path $PSScriptRoot 'p2-c2-snapshot-metadata.ps1')

function Assert-Equal {
    param([Parameter(Mandatory)]$Expected, [Parameter(Mandatory)]$Actual, [Parameter(Mandatory)][string]$Label)
    if ([string]$Expected -cne [string]$Actual) {
        throw "$Label differs: expected=$Expected actual=$Actual"
    }
}

function Assert-Throws {
    param([Parameter(Mandatory)][scriptblock]$Action, [Parameter(Mandatory)][string]$Pattern)
    $thrown = $false
    try {
        & $Action
    } catch {
        $thrown = $true
        if ($_.Exception.Message -notmatch $Pattern) { throw }
    }
    if (-not $thrown) { throw "Expected failure matching: $Pattern" }
}

[xml]$historicalMetadata = @'
<metadata>
  <versioning>
    <snapshotVersions>
      <snapshotVersion><extension>pom</extension><value>0.1.0-20260826.091429-1</value><updated>20260826091449</updated></snapshotVersion>
      <snapshotVersion><extension>pom.sha1</extension><value>0.1.0-20260826.091429-1</value><updated>20260826091451</updated></snapshotVersion>
      <snapshotVersion><extension>pom</extension><value>0.1.0-20260830.124441-2</value><updated>20260830124513</updated></snapshotVersion>
      <snapshotVersion><extension>jar</extension><value>0.1.0-20260830.124441-2</value><updated>20260830124513</updated></snapshotVersion>
      <snapshotVersion><classifier>sources</classifier><extension>jar</extension><value>0.1.0-20260830.124441-2</value><updated>20260830124513</updated></snapshotVersion>
      <snapshotVersion><extension>pom</extension><value>0.1.0-20260912.130848-3</value><updated>20260912130935</updated></snapshotVersion>
      <snapshotVersion><extension>jar</extension><value>0.1.0-20260912.130848-3</value><updated>20260912130937</updated></snapshotVersion>
    </snapshotVersions>
  </versioning>
</metadata>
'@

$pomValues = @(Get-P2C2SnapshotValues -Metadata $historicalMetadata -Extension 'pom')
$jarValues = @(Get-P2C2SnapshotValues -Metadata $historicalMetadata -Extension 'jar')
$missingValues = @(Get-P2C2SnapshotValues -Metadata $null -Extension 'pom')
Assert-Equal 0 $missingValues.Count 'Missing metadata history count'
Assert-Equal 3 $pomValues.Count 'Accumulated POM history count'
Assert-Equal 2 $jarValues.Count 'Unclassified JAR history count'

$pomResolved = Resolve-P2C2PublishedValue `
    -BeforeValues @('0.1.0-20260826.091429-1', '0.1.0-20260830.124441-2') `
    -AfterValues $pomValues -ArtifactId 'fixture' -Extension 'pom'
$jarResolved = Resolve-P2C2PublishedValue `
    -BeforeValues @('0.1.0-20260830.124441-2') `
    -AfterValues $jarValues -ArtifactId 'fixture' -Extension 'jar'
Assert-Equal '0.1.0-20260912.130848-3' $pomResolved 'POM delta value'
Assert-Equal $pomResolved $jarResolved 'Coordinate resolved value'
Assert-P2C2CoordinateResolvedValue -ArtifactId 'fixture' -Packaging JAR -PomValue $pomResolved -JarValue $jarResolved

Assert-Throws -Pattern 'found 0' -Action {
    Resolve-P2C2PublishedValue -BeforeValues $pomValues -AfterValues $pomValues -ArtifactId 'fixture' -Extension 'pom'
}
Assert-Throws -Pattern 'found 2' -Action {
    Resolve-P2C2PublishedValue -BeforeValues @() -AfterValues @('0.1.0-a-1', '0.1.0-b-2') `
        -ArtifactId 'fixture' -Extension 'pom'
}
Assert-Throws -Pattern 'resolved snapshot values differ' -Action {
    Assert-P2C2CoordinateResolvedValue -ArtifactId 'fixture' -Packaging JAR `
        -PomValue '0.1.0-a-1' -JarValue '0.1.0-b-2'
}
[xml]$invalidMetadata = '<metadata><versioning><snapshotVersions><snapshotVersion><extension>pom</extension><value>../invalid</value></snapshotVersion></snapshotVersions></versioning></metadata>'
Assert-Throws -Pattern 'Invalid pom snapshotVersion value' -Action {
    Get-P2C2SnapshotValues -Metadata $invalidMetadata -Extension 'pom'
}

Write-Output 'P2-C2 accumulated snapshot metadata verification: SUCCESS'

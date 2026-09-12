function Get-P2C2SnapshotValues {
    param(
        [AllowNull()][xml]$Metadata,
        [Parameter(Mandatory)][string]$Extension
    )

    if ($null -eq $Metadata -or $null -eq $Metadata.metadata.versioning.snapshotVersions) {
        return @()
    }

    $values = @($Metadata.metadata.versioning.snapshotVersions.snapshotVersion | Where-Object {
        $classifierProperty = $_.PSObject.Properties['classifier']
        $classifier = if ($null -eq $classifierProperty) { '' } else { [string]$classifierProperty.Value }
        [string]$_.extension -ceq $Extension -and $classifier -ceq ''
    } | ForEach-Object {
        $value = [string]$_.value
        if ($value -cnotmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') {
            throw "Invalid $Extension snapshotVersion value: $value"
        }
        $value
    } | Sort-Object -Unique)

    return $values
}

function Resolve-P2C2PublishedValue {
    param(
        [AllowEmptyCollection()][string[]]$BeforeValues,
        [AllowEmptyCollection()][string[]]$AfterValues,
        [Parameter(Mandatory)][string]$ArtifactId,
        [Parameter(Mandatory)][string]$Extension
    )

    $before = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($value in @($BeforeValues)) { [void]$before.Add($value) }
    $added = @($AfterValues | Where-Object { -not $before.Contains($_) } | Sort-Object -Unique)
    if ($added.Count -ne 1) {
        throw "$ArtifactId must add exactly one unclassified $Extension snapshotVersion; found $($added.Count)."
    }
    return $added[0]
}

function Assert-P2C2CoordinateResolvedValue {
    param(
        [Parameter(Mandatory)][string]$ArtifactId,
        [Parameter(Mandatory)][ValidateSet('POM', 'JAR')][string]$Packaging,
        [Parameter(Mandatory)][string]$PomValue,
        [AllowNull()][string]$JarValue
    )

    if ($Packaging -eq 'JAR' -and $PomValue -cne $JarValue) {
        throw "$ArtifactId POM and JAR resolved snapshot values differ: pom=$PomValue jar=$JarValue"
    }
}

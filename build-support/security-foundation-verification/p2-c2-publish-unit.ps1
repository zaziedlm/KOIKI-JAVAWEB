function Get-P2C2PublishUnit {
    param([Parameter(Mandatory)][string]$SupportRoot)

    function Read-UnitFile {
        param([Parameter(Mandatory)][string]$Path, [Parameter(Mandatory)][string]$Label)

        return @(Get-Content -LiteralPath $Path | ForEach-Object {
            $line = $_.Trim()
            if (-not $line -or $line.StartsWith('#')) { return }
            $parts = @($line -split '\s+')
            if ($parts.Count -ne 3 -or $parts[0] -notin @('POM', 'JAR')) {
                throw "Invalid $Label entry: $line"
            }
            [pscustomobject]@{
                Packaging = $parts[0]
                ArtifactId = $parts[1]
                ModulePath = $parts[2]
            }
        })
    }

    $formal = Read-UnitFile -Path (Join-Path $SupportRoot 'p2-c2-formal-release-unit.txt') -Label 'formal release unit'
    $publish = Read-UnitFile -Path (Join-Path $SupportRoot 'p2-c2-publish-unit.txt') -Label 'publish unit'
    if ($formal.Count -ne 14 -or @($formal | Where-Object Packaging -eq 'JAR').Count -ne 11) {
        throw 'P2-C2 formal release unit must contain POM 3 / JAR 11 projects.'
    }
    if ($publish.Count -ne 13 -or @($publish | Where-Object Packaging -eq 'JAR').Count -ne 11) {
        throw 'P2-C2 publish unit must contain POM 2 / JAR 11 coordinates.'
    }
    if (@($publish.ArtifactId | Sort-Object -Unique).Count -ne $publish.Count) {
        throw 'P2-C2 publish unit artifact IDs must be unique.'
    }

    $excluded = @($formal | Where-Object ArtifactId -CEQ 'koiki-javaweb-fw-reactor')
    if ($excluded.Count -ne 1 -or $excluded[0].Packaging -cne 'POM' -or $excluded[0].ModulePath -cne '.') {
        throw 'The formal release unit must contain exactly one Root aggregator POM.'
    }
    $expected = @($formal | Where-Object ArtifactId -CNE 'koiki-javaweb-fw-reactor')
    $expectedKeys = @($expected | ForEach-Object { "$($_.Packaging)|$($_.ArtifactId)|$($_.ModulePath)" } | Sort-Object)
    $publishKeys = @($publish | ForEach-Object { "$($_.Packaging)|$($_.ArtifactId)|$($_.ModulePath)" } | Sort-Object)
    $difference = @(Compare-Object -ReferenceObject $expectedKeys -DifferenceObject $publishKeys -SyncWindow 0)
    if ($difference.Count -ne 0) {
        throw "P2-C2 publish unit must equal the formal release unit minus the Root aggregator:`n$($difference | Out-String)"
    }
    return $publish
}

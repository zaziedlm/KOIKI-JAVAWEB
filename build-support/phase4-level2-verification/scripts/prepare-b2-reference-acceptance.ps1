param(
    [Parameter(Mandatory)][string]$RunDirectory,
    [Parameter(Mandatory)][string]$MavenRepository
)
$ErrorActionPreference='Stop'
if(-not $IsWindows) {throw 'B2 preparation currently requires Windows'}
$repository=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$module=Join-Path $repository 'build-support/phase4-level2-verification'
if(-not [IO.Path]::IsPathFullyQualified($RunDirectory) -or
   -not [IO.Path]::IsPathFullyQualified($MavenRepository)) {throw 'Absolute preparation paths required'}
$runPath=[IO.Path]::GetFullPath($RunDirectory)
$mavenPath=[IO.Path]::GetFullPath($MavenRepository)
if(Test-Path -LiteralPath $runPath) {throw 'New preparation directory required'}
if(-not (Test-Path -LiteralPath $mavenPath -PathType Container)) {throw 'Prepared Maven repository required'}
if(-not $runPath.StartsWith($repository+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'Preparation output must remain within the verification source repository'
}
if(-not (Test-Path -LiteralPath (Split-Path $runPath) -PathType Container)) {throw 'Preparation parent directory required'}
New-Item -ItemType Directory -Path $runPath | Out-Null
$classpath=Join-Path $runPath 'tooling-jdbc-test-classpath.txt'
$previousMavenOpts=$env:MAVEN_OPTS
$started=[DateTime]::UtcNow
$results=[Collections.Generic.List[object]]::new()
$sourceFiles=@(Get-ChildItem -LiteralPath (Join-Path $module 'src') -File -Recurse)
$sourceFiles+=Get-Item -LiteralPath (Join-Path $module 'pom.xml')
$sourceBefore=@($sourceFiles | ForEach-Object {[ordered]@{Path=$_.FullName;Sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
$sourceBefore | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runPath 'source-before.json') -Encoding utf8
function Invoke-PreparationGoal([string]$Name,[string[]]$Arguments) {
    if(([DateTime]::UtcNow-$started).TotalMinutes -ge 20) {throw 'Preparation time limit reached'}
    $log=Join-Path $runPath ($Name+'.log')
    $command=@('-o','-B','-ntp',"-Dmaven.repo.local=$mavenPath",'-f',(Join-Path $module 'pom.xml'))+$Arguments
    $begin=[DateTime]::UtcNow
    $process=$null
    try {
        $startInfo=[Diagnostics.ProcessStartInfo]::new()
        $startInfo.FileName=$env:ComSpec
        $startInfo.WorkingDirectory=$repository
        $startInfo.UseShellExecute=$false
        $startInfo.CreateNoWindow=$true
        # Fixed Maven arguments and validated paths; cmd.exe cannot receive metacharacters.
        $tokens=@((Join-Path $repository 'mvnw.cmd'))+$command
        foreach($token in $tokens) {if($token -match '["\r\n&|<>^%!]' ) {throw 'Unsafe preparation argument'}}
        $startInfo.Arguments='/d /s /c "'+(($tokens | ForEach-Object {'"'+$_+'"'}) -join ' ')+' > "'+$log+'" 2>&1"'
        if($log -match '["\r\n&|<>^%!]' ) {throw 'Unsafe log path'}
        $process=[Diagnostics.Process]::Start($startInfo)
        while(-not $process.WaitForExit(1000)) {
            if(([DateTime]::UtcNow-$started).TotalMinutes -ge 20 -or
                ((Test-Path -LiteralPath $log) -and (Get-Item -LiteralPath $log).Length -gt 16MB)) {
                $process.Kill($true)
                $process.WaitForExit()
                throw 'Preparation time or log limit exceeded'
            }
        }
        $exit=$process.ExitCode
        $row=[ordered]@{Goal=$Name;Arguments=$command;Exit=$exit;StartedUtc=$begin.ToString('o');EndedUtc=[DateTime]::UtcNow.ToString('o')}
        $results.Add($row)
        $row | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runPath ($Name+'-result.json')) -Encoding utf8
        if($exit -ne 0) {throw "Preparation goal failed: $Name"}
    } finally {if($null -ne $process) {$process.Dispose()}}
}
try {
    $env:MAVEN_OPTS='-Xmx768m'
    Invoke-PreparationGoal 'package-jdbc' @('-Pjdbc','-DskipTests','-Dmaven.test.skip=true','clean','package')
    Invoke-PreparationGoal 'compile-tests' @('-Pjdbc,s1-contract,s1-web','-DskipTests','test-compile')
    Invoke-PreparationGoal 'classpath-jdbc-test' @('-Pjdbc','org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath','-DincludeScope=test',"-Dmdep.outputFile=$classpath")
    if(-not (Test-Path -LiteralPath $classpath -PathType Leaf) -or (Get-Item -LiteralPath $classpath).Length -gt 1MB) {throw 'Classpath unavailable or oversized'}
    $entries=@((Get-Content -Raw -LiteralPath $classpath).Trim() -split [regex]::Escape([IO.Path]::PathSeparator))
    if($entries.Count -gt 512 -or $entries.Count -eq 0) {throw 'Classpath entry limit exceeded'}
    $artifacts=@()
    foreach($entry in $entries) {
        if(-not [IO.Path]::IsPathFullyQualified($entry) -or -not (Test-Path -LiteralPath $entry -PathType Leaf)) {throw 'Classpath entry unavailable'}
        $artifacts += [ordered]@{Path=$entry;Sha256=(Get-FileHash -LiteralPath $entry).Hash}
    }
    foreach($entry in $sourceBefore) {if((Get-FileHash -LiteralPath $entry.Path).Hash -cne $entry.Sha256) {throw 'Tooling source changed'}}
    $outputs=@(Get-ChildItem -LiteralPath (Join-Path $module 'target/classes') -File -Recurse)
    $outputs+=Get-ChildItem -LiteralPath (Join-Path $module 'target/test-classes') -File -Recurse
    $outputs+=Get-Item -LiteralPath (Join-Path $module 'target/phase4-level2-verification-0.1.0-SNAPSHOT.jar')
    $artifacts+=@($outputs | ForEach-Object {[ordered]@{Path=$_.FullName;Sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
    $artifacts | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runPath 'artifact-hashes.json') -Encoding utf8
    [ordered]@{Status='PASS';SourceStable=$true;Classpath=$classpath;Goals=$results.ToArray();StartedUtc=$started.ToString('o');EndedUtc=[DateTime]::UtcNow.ToString('o')} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $runPath 'summary.json') -Encoding utf8
    Write-Output "B2 preparation complete: $classpath"
} catch {
    [ordered]@{Status='FAIL';Goals=$results.ToArray();StartedUtc=$started.ToString('o');EndedUtc=[DateTime]::UtcNow.ToString('o')} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $runPath 'summary.json') -Encoding utf8
    throw
} finally {$env:MAVEN_OPTS=$previousMavenOpts}

param(
    [ValidateSet('Tooling','Reference')][string]$Stage='Tooling',
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunLabel='first',
    [switch]$RepairToolingGate
)
$ErrorActionPreference='Stop'
$repository=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$module=Join-Path $repository 'build-support/phase4-level2-verification'
function Get-ValidatedB2Cases($Rows,$Expected) {
    if(@($Rows).Count -ne @($Expected).Count) {throw 'Class count mismatch'}
    $total=0
    foreach($item in $Expected) {
        $matches=@($Rows | Where-Object {$_.Class -ceq $item.Name})
        if($matches.Count -ne 1) {throw 'Missing or duplicate class'}
        $row=$matches[0]
        foreach($field in @('Expected','Cases','Failures','Errors','Skipped','Exit')) {
            if($null -eq $row.$field -or $row.$field -is [bool] -or [string]$row.$field -notmatch '^\d+$') {throw 'Invalid numeric result'}
        }
        if([int]$row.Expected -ne $item.Cases -or [int]$row.Cases -ne $item.Cases -or
            [int]$row.Failures -ne 0 -or [int]$row.Errors -ne 0 -or [int]$row.Skipped -ne 0 -or [int]$row.Exit -ne 0) {throw 'Class result mismatch'}
        foreach($field in @('FreshXml','Cleanup','SourceStable')) {
            if($row.$field -isnot [bool] -or $row.$field -ne $true) {throw 'Required evidence flag missing'}
        }
        if($row.CeilingFailure) {throw 'Resource ceiling failure'}
        $total += [int]$row.Cases
    }
    return $total
}
$toolingExpected=@(@{Name='B2SourceFreezeTest';Cases=9},@{Name='B2FrozenSourceProtocolTest';Cases=9})
$toolingRaw=Join-Path $repository 'tmp/b2-verification-0321079-20261008/Tooling-first'
$correctedPath=Join-Path $toolingRaw 'gate-summary-corrected.json'
if($RepairToolingGate) {
    if($Stage -ne 'Tooling' -or $RunLabel -ne 'first') {throw 'Repair is restricted to saved Tooling-first evidence'}
    if(Test-Path -LiteralPath $correctedPath) {throw 'Corrected gate already exists'}
    $originalPath=Join-Path $toolingRaw 'summary.json'
    $originalHash=(Get-FileHash -LiteralPath $originalPath).Hash
    $original=Get-Content -LiteralPath $originalPath -Raw | ConvertFrom-Json
    $rows=@(Get-Content -LiteralPath (Join-Path $toolingRaw 'results.json') -Raw | ConvertFrom-Json)
    $total=Get-ValidatedB2Cases $rows $toolingExpected
    $inputs=@()
    foreach($item in $toolingExpected) {
        $directory=Join-Path $toolingRaw $item.Name
        $rowPath=Join-Path $directory 'result.json'
        $row=Get-Content -LiteralPath $rowPath -Raw | ConvertFrom-Json
        $null=Get-ValidatedB2Cases @($row) @($item)
        $saved=$rows | Where-Object {$_.Class -ceq $item.Name}
        foreach($field in @('Expected','Cases','Failures','Errors','Skipped','Exit','FreshXml','Cleanup','SourceStable','CeilingFailure','ElapsedSeconds')) {
            if($row.$field -cne $saved.$field) {throw 'Class and group JSON differ'}
        }
        $xmlPath=Join-Path $directory 'junit-sanitized.xml'
        [xml]$xml=Get-Content -LiteralPath $xmlPath -Raw
        if($xml.testsuite.name -cne ('org.koikifw.buildsupport.phase4.'+$item.Name)) {throw 'XML class mismatch'}
        foreach($field in @(@('tests','Cases'),@('failures','Failures'),@('errors','Errors'),@('skipped','Skipped'))) {
            $value=$xml.testsuite.GetAttribute($field[0])
            if($value -notmatch '^\d+$' -or [int]$value -ne [int]$row.($field[1])) {throw 'XML and JSON counts differ'}
        }
        if(@($xml.testsuite.testcase).Count -ne $item.Cases -or $xml.SelectNodes('//failure|//error|//skipped').Count -ne 0) {throw 'XML testcase mismatch'}
        foreach($path in @($rowPath,$xmlPath)) {$inputs += [ordered]@{Path=$path;Sha256=(Get-FileHash -LiteralPath $path).Hash}}
    }
    $beforePath=Join-Path $toolingRaw 'source-before.json'
    $source=@(Get-Content -LiteralPath $beforePath -Raw | ConvertFrom-Json)
    if($source.Count -ne 6 -or @($source.Path | Select-Object -Unique).Count -ne 6) {throw 'Source manifest mismatch'}
    foreach($entry in $source) {
        $recorded=@($original.SourceHashes | Where-Object {$_.Path -ceq $entry.Path})
        if($recorded.Count -ne 1 -or $recorded[0].Sha256 -cne $entry.Sha256) {throw 'Original source manifests differ'}
        if($entry.Path -ine $PSCommandPath -and (Get-FileHash -LiteralPath $entry.Path).Hash -cne $entry.Sha256) {throw 'Tooling source changed'}
    }
    # Finite aggregation checks only: no Maven, JVM, JDBC or Docker execution.
    $checks=@([ordered]@{Case='integer-9-plus-9';Passed=($total -eq 18)})
    foreach($mutation in @('missing','duplicate','unexpected','null','wrong-count','failure','error','skip','exit','fresh','cleanup','source','ceiling')) {
        $bad=@($rows | ConvertTo-Json -Depth 6 | ConvertFrom-Json)
        switch($mutation) {
            'missing' {$bad=@($bad[0])}
            'duplicate' {$bad[1].Class=$bad[0].Class}
            'unexpected' {$bad[1].Class='UnexpectedTest'}
            'null' {$bad[0].Cases=$null}
            'wrong-count' {$bad[0].Cases=8}
            'failure' {$bad[0].Failures=1}
            'error' {$bad[0].Errors=1}
            'skip' {$bad[0].Skipped=1}
            'exit' {$bad[0].Exit=1}
            'fresh' {$bad[0].FreshXml=$false}
            'cleanup' {$bad[0].Cleanup=$false}
            'source' {$bad[0].SourceStable=$false}
            'ceiling' {$bad[0].CeilingFailure='limit'}
        }
        $rejected=$false
        try {$null=Get-ValidatedB2Cases $bad $toolingExpected} catch {$rejected=$true}
        if(-not $rejected) {throw 'Aggregation negative check did not reject'}
        $checks += [ordered]@{Case=$mutation;Passed=$rejected}
    }
    foreach($path in @((Join-Path $toolingRaw 'results.json'),$beforePath)) {$inputs += [ordered]@{Path=$path;Sha256=(Get-FileHash -LiteralPath $path).Hash}}
    if((Get-FileHash -LiteralPath $originalPath).Hash -cne $originalHash) {throw 'Original summary changed'}
    [ordered]@{Status='PASS';Stage='Tooling';Cases=$total;ExpectedClasses=$toolingExpected;OriginalPath=$originalPath;OriginalSha256=$originalHash;
        Reason='Owner-approved section 14 integer aggregation correction; saved XML/JSON reconciliation; no test rerun';
        SourceHashes=$source;ScriptCorrection=[ordered]@{Path=$PSCommandPath;BeforeSha256=($source | Where-Object {$_.Path -ieq $PSCommandPath}).Sha256;AfterSha256=(Get-FileHash -LiteralPath $PSCommandPath).Hash};
        EvidenceInputs=$inputs;AggregationChecks=$checks;CorrectedAt=(Get-Date).ToUniversalTime().ToString('o')} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $correctedPath -Encoding utf8
    Write-Output "Corrected Tooling gate: $total cases; $($checks.Count) finite aggregation checks passed; no JUnit rerun"
    return
}
$raw=Join-Path $repository "tmp/b2-verification-0321079-20261008/$Stage-$RunLabel"
if(Test-Path -LiteralPath $raw) {throw 'Run directory already exists'}
New-Item -ItemType Directory -Path $raw | Out-Null
if($Stage -eq 'Tooling') {
    $classes=$toolingExpected
    $pom='build-support/phase4-level2-verification/pom.xml'
    $testRoot=Join-Path $module 'src/test/java/org/koikifw/buildsupport/phase4'
    $reports=Join-Path $module 'target/surefire-reports'
    $prefix='org.koikifw.buildsupport.phase4.'
} else {
    # The independent Tooling gate must already have passed on the current source.
    $gatePath=if(Test-Path -LiteralPath $correctedPath) {$correctedPath} else {Join-Path $toolingRaw 'summary.json'}
    if(-not (Test-Path -LiteralPath $gatePath)) {throw 'Tooling gate missing'}
    $gate=Get-Content -LiteralPath $gatePath -Raw | ConvertFrom-Json
    if($gate.Status -ne 'PASS' -or $gate.Cases -ne 18) {throw 'Tooling gate failed'}
    foreach($entry in $gate.SourceHashes) {
        $expectedHash=$entry.Sha256
        if($entry.Path -ieq $PSCommandPath -and $gate.ScriptCorrection) {
            $routePath=Join-Path $repository 'tmp/b2-runtime-route-0321079-20261008/approval.json'
            $route=Get-Content -LiteralPath $routePath -Raw | ConvertFrom-Json
            if($route.OwnerApprovalSection -ne 21 -or $route.BeforeSha256 -cne $gate.ScriptCorrection.AfterSha256) {throw 'Runtime route approval mismatch'}
            $expectedHash=$route.AfterSha256
        }
        if((Get-FileHash -LiteralPath $entry.Path).Hash -cne $expectedHash) {throw 'Tooling gate source changed'}
    }
    if($gate.ScriptCorrection) {
        foreach($entry in @($gate.EvidenceInputs)+@(@{Path=$gate.OriginalPath;Sha256=$gate.OriginalSha256})) {
            if((Get-FileHash -LiteralPath $entry.Path).Hash -cne $entry.Sha256) {throw 'Saved gate evidence changed'}
        }
    }
    $isolated=Join-Path $repository 'tmp/b2-isolated-compile-r2-0321079-20261008'
    $copyManifest=@(Get-Content -LiteralPath (Join-Path $isolated 'source-before.json') -Raw | ConvertFrom-Json)
    $artifactManifest=@(Get-Content -LiteralPath (Join-Path $isolated 'compiled-artifact-hashes.json') -Raw | ConvertFrom-Json)
    foreach($entry in $copyManifest) {
        if((Get-FileHash -LiteralPath $entry.Original).Hash -cne $entry.Sha256 -or (Get-FileHash -LiteralPath $entry.Copy).Hash -cne $entry.Sha256) {throw 'Isolated source changed'}
    }
    foreach($entry in $artifactManifest) {if((Get-FileHash -LiteralPath $entry.Path).Hash -cne $entry.Sha256) {throw 'Isolated compiled artifact changed'}}
    $registrationRaw=Join-Path $repository 'tmp/b2-verification-0321079-20261008/Reference-registration-first'
    $registration=Get-Content -LiteralPath (Join-Path $registrationRaw 'result.json') -Raw | ConvertFrom-Json
    [xml]$registrationXml=Get-Content -LiteralPath (Join-Path $registrationRaw 'junit-sanitized.xml') -Raw
    if($registration.Status -ne 'PASS' -or $registration.Cases -ne 8 -or $registration.Exit -ne 0 -or
        $registration.Failures -ne 0 -or $registration.Errors -ne 0 -or $registration.Skipped -ne 0 -or
        -not $registration.FreshXml -or -not $registration.Cleanup -or -not $registration.SourceStable -or $registration.CeilingFailure -or
        [int]$registrationXml.testsuite.tests -ne 8 -or $registrationXml.SelectNodes('//failure|//error|//skipped').Count -ne 0) {throw 'Saved registration gate failed'}
    $registrationSources=@(Get-Content -LiteralPath (Join-Path $registrationRaw 'source-before.json') -Raw | ConvertFrom-Json)
    # These acceptance helpers were not used by the registration-only class; their later creation is separately compiled.
    foreach($entry in $registrationSources | Where-Object {$_.Path -notmatch 'B2(FrozenSourceProcess|ReadConnectionHarness)\.java$'}) {
        if((Get-FileHash -LiteralPath $entry.Path).Hash -cne $entry.Sha256) {throw 'Registration source changed'}
    }
    $classes=@(@{Name='B2ProtectedIssueReadTest';Cases=12},
        @{Name='B2IssueBoundaryTest';Cases=8},@{Name='B2IssueTransactionTest';Cases=8})
    $pom='tmp/b2-isolated-compile-r2-0321079-20261008/koiki-reference-app/pom.xml'
    $testRoot=Join-Path $isolated 'koiki-reference-app/src/test/java/org/koikifw/reference/notification'
    $reports=Join-Path $isolated 'koiki-reference-app/target/surefire-reports'
    $prefix='org.koikifw.reference.notification.'
}
foreach($class in $classes) {if(-not (Test-Path -LiteralPath (Join-Path $testRoot ($class.Name+'.java')))) {throw 'Selected class missing'}}
$baselineJava=@(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | ForEach-Object ProcessId)
if(@(docker ps -q).Count -ne 0) {throw 'Container baseline is not empty'}
$sourceFiles=@(Get-ChildItem -LiteralPath (Join-Path $module 'src/test/java/org/koikifw/buildsupport/phase4/b2fixture') -File)
$sourceFiles+=@(Get-Item -LiteralPath (Join-Path $module 'src/test/resources/s1-b2/frozen-source.sql'))
$sourceFiles+=@(Get-ChildItem -LiteralPath (Join-Path $module 'src/test/java/org/koikifw/buildsupport/phase4') -Filter 'B2*.java')
$sourceFiles+=Get-Item -LiteralPath $PSCommandPath
$sourceHashes=@($sourceFiles | ForEach-Object {[ordered]@{Path=$_.FullName;Sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
if($Stage -eq 'Reference') {
    foreach($entry in $copyManifest) {$sourceHashes+=@([ordered]@{Path=$entry.Original;Sha256=$entry.Sha256},[ordered]@{Path=$entry.Copy;Sha256=$entry.Sha256})}
    $sourceHashes+=$artifactManifest
    foreach($path in @((Join-Path $registrationRaw 'result.json'),(Join-Path $registrationRaw 'junit-sanitized.xml'),$correctedPath,$routePath)) {
        $sourceHashes+=[ordered]@{Path=$path;Sha256=(Get-FileHash -LiteralPath $path).Hash}
    }
}
$sourceHashes | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $raw 'source-before.json') -Encoding utf8
$group=[Diagnostics.Stopwatch]::StartNew()
$results=@()
$previousMavenOpts=$env:MAVEN_OPTS
try {
    $env:MAVEN_OPTS='-Xmx768m'
    foreach($class in $classes) {
        $groupCeiling=if($Stage -eq 'Reference') {30} else {45}
        if($group.Elapsed.TotalMinutes -ge $groupCeiling) {throw 'New-test group ceiling reached'}
        $directory=Join-Path $raw $class.Name
        New-Item -ItemType Directory -Path $directory | Out-Null
        $log=Join-Path $directory 'maven.log'
        $errorLog=Join-Path $directory 'maven-stderr.log'
        $xml=Join-Path $reports ('TEST-'+$prefix+$class.Name+'.xml')
        $oldXmlHash=if(Test-Path -LiteralPath $xml) {(Get-FileHash -LiteralPath $xml).Hash} else {''}
        $command='mvnw.cmd -o -B -ntp -f '+$pom+' '+$(if($Stage -eq 'Tooling') {'-Pjdbc '} else {''})+
            '-Dkoiki.b1.resource-limits.enabled=true -Dkoiki.b2.resource-limits.enabled=true '+
            '-Dkoiki.reference.verification.resource-limits.enabled=true -DargLine=-Xmx768m '+
            '-DforkCount=1 -DreuseForks=false -Dparallel=none -Djunit.jupiter.execution.parallel.enabled=false '+
            '-Djunit.jupiter.execution.timeout.default=60s -Dtest='+$class.Name+' surefire:test'
        $started=Get-Date
        $timer=[Diagnostics.Stopwatch]::StartNew()
        $process=Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/c',$command) -WorkingDirectory $repository `
            -WindowStyle Hidden -PassThru -RedirectStandardOutput $log -RedirectStandardError $errorLog
        $null=$process.Handle
        $samples=@()
        $ceilingFailure=$null
        while(-not $process.HasExited) {
            $memory=[long](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024
            $disk=[long](Get-PSDrive C).Free
            $java=@(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {$_.ProcessId -notin $baselineJava})
            $databaseCount=@(docker ps --filter 'ancestor=postgres:17-alpine' -q).Count
            $samples += [ordered]@{Seconds=$timer.Elapsed.TotalSeconds;MemoryBytes=$memory;DiskBytes=$disk;NewJava=$java.Count;Databases=$databaseCount}
            if($timer.Elapsed.TotalMinutes -ge 10 -or $group.Elapsed.TotalMinutes -ge $groupCeiling -or $memory -lt 8589934592 -or $disk -lt 10737418240 -or $java.Count -gt 4 -or $databaseCount -gt 1) {
                $ceilingFailure='Time or resource ceiling reached';$process.Kill($true);break
            }
            Start-Sleep -Seconds 1
            $process.Refresh()
        }
        $process.WaitForExit()
        $exit=$process.ExitCode
        $cleanupDeadline=(Get-Date).AddSeconds(120)
        do {
            $remainingJava=@(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {$_.ProcessId -notin $baselineJava})
            $remainingContainers=@(docker ps -q)
            if($remainingJava.Count -eq 0 -and $remainingContainers.Count -eq 0) {break}
            Start-Sleep -Milliseconds 500
        } while((Get-Date) -lt $cleanupDeadline)
        $cleanup=$remainingJava.Count -eq 0 -and $remainingContainers.Count -eq 0
        $sourceStable=@($sourceHashes | Where-Object {(Get-FileHash -LiteralPath $_.Path).Hash -ne $_.Sha256}).Count -eq 0
        $samples | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $directory 'resources.json') -Encoding utf8
        foreach($output in @($log,$errorLog)) {
            if((Get-Item -LiteralPath $output).Length -gt 1048576) {throw 'Finite log input ceiling reached'}
            $sanitized=(Get-Content -LiteralPath $output -Raw) -replace '(?im)^.*(?:password|username|jdbc|datasourceurl).*(?:\r?\n|$)','[CONNECTION_LINE_REDACTED]'
            Set-Content -LiteralPath $output -Value $sanitized -Encoding utf8
        }
        $actualCases=0;$failures=0;$errors=0;$skipped=0;$fresh=$false
        if(Test-Path -LiteralPath $xml) {
            $fresh=(Get-Item -LiteralPath $xml).LastWriteTime -ge $started -and (Get-FileHash -LiteralPath $xml).Hash -ne $oldXmlHash
            [xml]$report=Get-Content -LiteralPath $xml -Raw
            $actualCases=[int]$report.testsuite.tests;$failures=[int]$report.testsuite.failures
            $errors=[int]$report.testsuite.errors;$skipped=[int]$report.testsuite.skipped
            foreach($node in @($report.SelectNodes('//properties|//system-out|//system-err'))) {[void]$node.ParentNode.RemoveChild($node)}
            $report.Save((Join-Path $directory 'junit-sanitized.xml'))
        }
        $row=[ordered]@{Class=$class.Name;Expected=$class.Cases;Cases=$actualCases;Failures=$failures;Errors=$errors;Skipped=$skipped;
            Exit=$exit;FreshXml=$fresh;Cleanup=$cleanup;SourceStable=$sourceStable;ElapsedSeconds=$timer.Elapsed.TotalSeconds;CeilingFailure=$ceilingFailure}
        $results+=$row;$row | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $directory 'result.json') -Encoding utf8
        $results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $raw 'results.json') -Encoding utf8
        Write-Output ($row | ConvertTo-Json -Compress)
        $process.Dispose()
        if($exit -ne 0 -or -not $fresh -or $actualCases -ne $class.Cases -or $failures -ne 0 -or $errors -ne 0 -or $skipped -ne 0 -or -not $cleanup -or -not $sourceStable -or $ceilingFailure) {throw 'Class failed; evidence preserved; stop'}
    }
    $total=Get-ValidatedB2Cases $results $classes
    [ordered]@{Status='PASS';Stage=$Stage;Cases=$total;ElapsedSeconds=$group.Elapsed.TotalSeconds;SourceHashes=$sourceHashes} |
        ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $raw 'summary.json') -Encoding utf8
} finally {$env:MAVEN_OPTS=$previousMavenOpts}

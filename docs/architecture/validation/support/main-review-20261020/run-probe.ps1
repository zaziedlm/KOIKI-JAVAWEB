$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../../../..')).Path
$report = Join-Path $repo 'koiki-reference-app/target/surefire-reports/TEST-org.koikifw.reference.ReferenceArchitectureTest.xml'
# Existing report supplies dependency paths only, never an assertion of current test success.
$xml = [xml](Get-Content -LiteralPath $report -Raw)
$cp = ($xml.testsuite.properties.property | Where-Object name -eq 'java.class.path').value
if (-not $cp) { throw 'Reference dependency classpath is unavailable' }
$output = Join-Path $repo 'target/main-review-20261020'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$sources = @(
    (Join-Path $PSScriptRoot 'ReviewProbe.java'),
    (Join-Path $repo 'koiki-starters/koiki-starter-api/src/main/java/org/koikifw/starter/api/internal/KoikiProblemDetailsExceptionHandler.java'),
    (Join-Path $repo 'koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java'),
    (Join-Path $repo 'koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseApplicationService.java')
)
$sources += @(Get-ChildItem (Join-Path $repo 'koiki-reference-app/src/main/java/org/koikifw/reference/expense/domain/model') -Filter '*.java' | ForEach-Object FullName)
$sources += @(Get-ChildItem (Join-Path $repo 'koiki-archunit-rules/src/main/java') -Filter '*.java' -Recurse | ForEach-Object FullName)
& javac -proc:none -encoding UTF-8 -classpath $cp -d $output $sources
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
& java -classpath "$output$([IO.Path]::PathSeparator)$cp" org.koikifw.review.ReviewProbe *> (Join-Path $output 'probe.log')
$result = $LASTEXITCODE
Get-Content (Join-Path $output 'probe.log') | Select-String '^REVIEW|Exception in thread|AssertionError'
if ($result -ne 0) { throw "Probe failed: $result" }

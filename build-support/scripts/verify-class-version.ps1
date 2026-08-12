$ErrorActionPreference = "Stop"

$classFile = Join-Path $PSScriptRoot "..\..\walking-skeleton\ws-smoke-lib\target\classes\dev\koiki\walkingskeleton\smoke\lib\GreetingService.class"
$classFile = [System.IO.Path]::GetFullPath($classFile)

if (-not (Test-Path $classFile)) {
    throw "Class file not found. Run Maven package/verify first: $classFile"
}

$output = & javap -verbose $classFile
$output | Select-String "major version"

if (($output | Out-String) -notmatch "major version:\s+65") {
    throw "Expected Java 21 class major version 65."
}

Write-Host "PASS: Java 21 class major version 65 confirmed."

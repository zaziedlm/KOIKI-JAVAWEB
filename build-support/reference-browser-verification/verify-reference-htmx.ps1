[CmdletBinding()]
param(
    [string]$BaseUrl = "http://127.0.0.1:18080",
    [Parameter(Mandatory = $true)]
    [string]$LoginEmail,
    [Parameter(Mandatory = $true)]
    [SecureString]$LoginPassword,
    [switch]$Headed
)

$ErrorActionPreference = "Stop"
$toolDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = (Resolve-Path (Join-Path $toolDirectory "../..")).Path
$mavenWrapper = Join-Path $repositoryRoot "mvnw.cmd"
$toolPom = Join-Path $toolDirectory "pom.xml"
$plainPassword = [System.Net.NetworkCredential]::new("", $LoginPassword).Password
$previousBaseUrl = $env:KOIKI_REFERENCE_BROWSER_BASE_URL
$previousLoginEmail = $env:KOIKI_REFERENCE_BROWSER_LOGIN_EMAIL
$previousLoginPassword = $env:KOIKI_REFERENCE_BROWSER_LOGIN_PASSWORD
$previousHeadless = $env:KOIKI_REFERENCE_BROWSER_HEADLESS

try {
    $env:KOIKI_REFERENCE_BROWSER_BASE_URL = $BaseUrl
    $env:KOIKI_REFERENCE_BROWSER_LOGIN_EMAIL = $LoginEmail
    $env:KOIKI_REFERENCE_BROWSER_LOGIN_PASSWORD = $plainPassword
    $env:KOIKI_REFERENCE_BROWSER_HEADLESS = (-not $Headed.IsPresent).ToString().ToLowerInvariant()

    & $mavenWrapper -f $toolPom test
    if ($LASTEXITCODE -ne 0) {
        throw "Reference HTMX browser verification failed with exit code $LASTEXITCODE."
    }
}
finally {
    $env:KOIKI_REFERENCE_BROWSER_BASE_URL = $previousBaseUrl
    $env:KOIKI_REFERENCE_BROWSER_LOGIN_EMAIL = $previousLoginEmail
    $env:KOIKI_REFERENCE_BROWSER_LOGIN_PASSWORD = $previousLoginPassword
    $env:KOIKI_REFERENCE_BROWSER_HEADLESS = $previousHeadless
    $plainPassword = $null
}

<#
.SYNOPSIS
  社内SSLインスペクションProxy(既定: Netskope)のRoot証明書を、
  指定したJDKのcacertsへインポートする。

.DESCRIPTION
  Windowsの信頼済みRootストア(LocalMachine\Root)には社内Proxyの自己署名
  Root証明書が登録済みだが、JDK同梱のcacertsには反映されないため、
  `mvn`実行時に `PKIX path building failed` が発生する。
  本スクリプトはWindowsのRootストアから対象証明書を検索してexportし、
  keytoolでJDKのcacertsへimportする。

.PARAMETER JdkHome
  証明書を追加する対象JDKのホームディレクトリ。省略時は $env:JAVA_HOME。

.PARAMETER CaSubjectMatch
  Rootストアを検索する際に、証明書のSubjectに対して適用する正規表現。
  会社のProxy製品が異なる場合はここを変更する(例: "Zscaler", "Forcepoint")。

.PARAMETER Alias
  cacertsに登録するエイリアス名。

.EXAMPLE
  ./import-corporate-root-ca.ps1
  ./import-corporate-root-ca.ps1 -JdkHome "C:\jdks\temurin-25"
#>
param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$CaSubjectMatch = "Netskope",
    [string]$Alias = "corporate-root-ca",
    [string]$StorePass = "changeit"
)

$ErrorActionPreference = "Stop"

if (-not $JdkHome) {
    throw "JAVA_HOME が未設定です。-JdkHome で対象JDKのホームディレクトリを指定してください。"
}

$keytool = Join-Path $JdkHome "bin\keytool.exe"
$cacerts = Join-Path $JdkHome "lib\security\cacerts"

if (-not (Test-Path $keytool)) { throw "keytoolが見つかりません: $keytool" }
if (-not (Test-Path $cacerts)) { throw "cacertsが見つかりません: $cacerts" }

$cert = Get-ChildItem Cert:\LocalMachine\Root |
    Where-Object { $_.Subject -match $CaSubjectMatch -and $_.Subject -eq $_.Issuer } |
    Select-Object -First 1

if (-not $cert) {
    throw "Windows Rootストアに '$CaSubjectMatch' に一致する自己署名Root証明書が見つかりません。社内Proxyの証明書名を -CaSubjectMatch で指定してください。"
}

# keytoolはJAVA_TOOL_OPTIONS等の情報をstderrへ出すことがあるため、
# ネイティブコマンド実行時だけエラーアクションを緩める。
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    & $keytool -list -keystore $cacerts -storepass $StorePass -alias $Alias 2>&1 | Out-Null
    $aliasExists = ($LASTEXITCODE -eq 0)

    if (-not $aliasExists) {
        $tempCer = Join-Path $env:TEMP "$Alias.cer"
        Export-Certificate -Cert $cert -FilePath $tempCer -Type CERT | Out-Null
        try {
            & $keytool -importcert -keystore $cacerts -storepass $StorePass -alias $Alias -file $tempCer -noprompt 2>&1 | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "keytool -importcert が失敗しました(exit code: $LASTEXITCODE)。" }
        }
        finally {
            Remove-Item $tempCer -Force -ErrorAction SilentlyContinue
        }
    }
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
}

if ($aliasExists) {
    Write-Host "エイリアス '$Alias' は $cacerts に登録済みのため、インポートをスキップしました。"
    exit 0
}

Write-Host "'$($cert.Subject)' をエイリアス '$Alias' として $cacerts へインポートしました。"

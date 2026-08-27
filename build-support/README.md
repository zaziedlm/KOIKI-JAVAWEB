# build-support

KOIKI自身のBuild / Quality Gate検証を補助するTooling所有の資材を配置します。

## Feature Template

`feature-templates/`はPhase 1a B1のTooling所有Tier 1 SIMPLE / Tier 2 RICH生成資材です。
生成された業務moduleはCustomerまたはReference等の生成先Applicationが所有します。
正式Maven artifactおよびPhase 5のProject Templateではありません。生成方法とRepository内の
統合verificationは`feature-templates/README.md`を参照してください。

`maven/g1-baseline-consumer-pom.xml`は、正式Parentを異なるversionのConsumerから継承し、
BOMの実効versionとtest / runtime scopeを照合する非配布fixtureです。Root Reactorおよび
Phase 1a release unitには含めません。

```powershell
.\mvnw.cmd -DskipTests install
.\mvnw.cmd -f build-support/maven/g1-baseline-consumer-pom.xml help:effective-pom
.\mvnw.cmd -f build-support/maven/g1-baseline-consumer-pom.xml dependency:tree -Dverbose
.\mvnw.cmd -f build-support/maven/g1-baseline-consumer-pom.xml dependency:tree -Dscope=runtime
```

最初の`install`はA2のローカル実効検証専用です。Repository外Consumerの配布証明には使用しません。

## Null Safety

`null-safety/`はPhase 1a B4のTooling所有非配布fixtureです。Parentから継承するJSpecify / NullAwayを、
正常production source、隔離した意図的違反、正常sourceへの復元の順に検証します。

```powershell
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

検証scriptはtracked sourceを書き換えず、Root Reactorや配布artifactへnegative fixtureを含めません。
詳細と期待diagnosticは`null-safety/README.md`を参照してください。

## Public API Compatibility

`api-compatibility/`はPhase 1a C3のTooling所有・非配布検証資材です。C1のtimestamped
snapshot JARをSHA-256付きbaselineとし、現行Architecture Contract / ArchUnit Rulesの
Public API inventoryとjapicmp比較を実行します。Root Reactorには含めません。

```powershell
pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-compatibility.ps1
pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-fixtures.ps1
```

詳細、credential境界およびGateごとの検証範囲は`api-compatibility/README.md`を参照してください。

Walking Skeleton由来のMaven Toolchains例、class version確認、Java 25 runtime確認等は、
正式な代替検証が成立するまで履歴・比較用資材として残します。

## 社内SSLインスペクションProxy環境でのMavenビルドエラー対応

社内ネットワーク(Netskope等のSSLインスペクションProxy配下)でMavenを実行すると、
Maven Central等へのHTTPS通信で以下のようなエラーが発生することがある。

```
Non-resolvable import POM: ... Could not transfer artifact ...
PKIX path building failed: unable to find valid certification path to requested target
```

これはWindowsの証明書ストアには社内ProxyのRoot証明書が登録済みでも、
JDK同梱の`cacerts`には反映されていないために発生する。コード側の問題ではなく、
各開発者の端末ごとに一度だけ対応が必要な環境設定。

### 対応手順

1. VS Codeで使用するJDKのホームディレクトリを確認する(既定では拡張機能同梱のJDK。
   `mvn -v`の`Maven home`/`Java version`や、VS Codeの`java.configuration.runtimes`設定を参照)。
2. PowerShellで以下を実行し、Windowsの信頼済みRootストアから社内Proxyの証明書を
   対象JDKの`cacerts`へインポートする。

   ```powershell
   build-support/scripts/import-corporate-root-ca.ps1 -JdkHome "<JDKのホームディレクトリ>"
   ```

   - 複数のJDK(例: Java 21でビルド、Java 25でランタイム検証)を使う場合は、
     それぞれのJDKホームに対して実行する。
   - 社内ProxyがNetskope以外の場合は `-CaSubjectMatch` で証明書のSubjectに含まれる
     文字列(例: `"Zscaler"`, `"Forcepoint"`)を指定する。
3. `mvn -v`や`mvn -q install`を再実行し、証明書エラーが解消したことを確認する。
4. VS Codeでpom.xmlにエラー表示が残る場合は、コマンドパレットから
   `Java: Clean Java Language Server Workspace`等でJavaプロジェクトを再読み込みする。

### VS Code拡張機能が同梱する別JREにも個別対応が必要

上記手順は`JAVA_HOME`等、開発者が明示的に指定したJDKにのみ有効。
VS Code拡張機能は独自に同梱JREを使うことが多く、その`cacerts`は別途インポートが必要。
実機確認(タスクマネージャで各`java.exe`のコマンドラインを確認)した対象は以下の通り
(拡張機能のバージョンにより実際のパスは変わる)。

- `redhat.java`(Java Language Server本体、および`vmware.vscode-spring-boot`の
  Spring Boot Language Serverも同じJREを共有起動する):
  `<拡張機能フォルダ>\redhat.java-<version>-win32-x64\jre\<jre-version>-win32-x86_64`
  ここが未対応だと、Spring Boot Dashboard/Spring Initializr系の
  `Failed to fetch Generation from Spring IO: ... PKIX path building failed` が発生する。
- `redhat.vscode-xml`(pom.xml等のXSD検証を行うlemminx)は、設定(`xml.java.home`,
  `xml.server.preferBinary`)を有効にしていてもネイティブバイナリ
  (`lemminx-win32.exe`等)で起動される場合がある。ネイティブバイナリは
  `-Djavax.net.ssl.trustStore=<cacertsのパス> -Djavax.net.ssl.trustStorePassword=changeit`
  を`xml.server.binary.args`設定で渡すことで、社内Proxy証明書をインポート済みの
  `cacerts`を信頼させることができる。

いずれもcacerts更新やVS Code設定変更後は、対象プロセスを再起動しないと反映されないため、
VS Codeを完全に終了してから再起動する(ウィンドウの再読み込みだけでは
プロセスが残る場合がある)。

対象スクリプト: [import-corporate-root-ca.ps1](scripts/import-corporate-root-ca.ps1)

### サンプル実例(拡張機能`pleiades.java-extension-pack-jdk`構成の場合)

`pleiades.java-extension-pack-jdk`はJDK 17/21/25/latestを個別フォルダで提供し、
`redhat.java`はさらに専用JREを同梱する。この組み合わせでは、cacertsへのインポートが
必要な対象は最低でも次のパスになる(バージョン番号部分は環境により異なる)。

```powershell
$jdkBase = "$env:APPDATA\Code\User\globalStorage\pleiades.java-extension-pack-jdk\java"

# Maven CLIビルド/ランタイム検証で使う各JDK
pwsh -NoProfile -File build-support/scripts/import-corporate-root-ca.ps1 -JdkHome "$jdkBase\17"
pwsh -NoProfile -File build-support/scripts/import-corporate-root-ca.ps1 -JdkHome "$jdkBase\21"
pwsh -NoProfile -File build-support/scripts/import-corporate-root-ca.ps1 -JdkHome "$jdkBase\25"
pwsh -NoProfile -File build-support/scripts/import-corporate-root-ca.ps1 -JdkHome "$jdkBase\latest"

# Java Language Server / Spring Boot Language Server(redhat.java同梱JRE)
# フォルダ名は redhat.java 拡張機能のバージョンごとに変わるため、更新後は都度確認する。
$redhatJre = Get-ChildItem "$env:USERPROFILE\.vscode\extensions\redhat.java-*\jre\*-win32-x86_64" |
    Select-Object -First 1 -ExpandProperty FullName
pwsh -NoProfile -File build-support/scripts/import-corporate-root-ca.ps1 -JdkHome $redhatJre
```

確認例:

```powershell
& "$jdkBase\21\bin\keytool.exe" -list -keystore "$jdkBase\21\lib\security\cacerts" `
    -storepass changeit -alias corporate-root-ca
```

インポート後はVS Codeを完全終了→再起動し、`Java: Clean Java Language Server Workspace`で
再読み込みしてから、`mvn -q -f pom.xml validate`等でエラーが解消したことを確認する。

### settings.json側の対応

cacertsへのインポートだけでは`redhat.vscode-xml`(lemminx)がネイティブバイナリで
起動する構成に対応できない場合がある。ユーザー設定(`settings.json`)に以下を追加し、
lemminx起動時に社内Proxy証明書をインポート済みのcacertsを明示的に信頼させる。

```jsonc
{
  // lemminxをJavaプロセスで起動させたい場合、対象JDKを指定
  "xml.java.home": "<corporate-root-ca 対応済みJDKのホームディレクトリ>",
  // falseにしてもネイティブバイナリで起動される場合があるため、下のbinary.argsと併用する
  "xml.server.preferBinary": false,
  // ネイティブバイナリ起動時にもtrustStoreを効かせるためのJVM引数
  "xml.server.binary.args": "-Djavax.net.ssl.trustStore=<corporate-root-ca 対応済みcacertsのパス> -Djavax.net.ssl.trustStorePassword=changeit"
}
```

- `xml.java.home`/`xml.server.binary.args`で指すJDK・cacertsは、前述の
  「サンプル実例」でcacertsインポートを実施済みのJDK(例: JDK 21)を指定する。
- `java.configuration.runtimes`でMaven/Java拡張機能が使うJDK一覧を明示している場合は、
  そこに列挙した各JDK(既定JDKを含む)がすべてcacertsインポート対象になる。
- `settings.json`変更後もプロセス再起動が必要なため、VS Codeを完全終了→再起動する。

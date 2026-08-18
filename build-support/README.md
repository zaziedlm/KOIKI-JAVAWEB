# build-support

KOIKI自身のBuild / Quality Gate検証を補助する資材を配置します。

Walking Skeletonでは、Maven Toolchains例、Wrapper bootstrap、
class version確認、Java 25 runtime確認等を置いています。

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

対象スクリプト: [import-corporate-root-ca.ps1](scripts/import-corporate-root-ca.ps1)

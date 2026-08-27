# Public API Compatibility

Phase 1a C3のTooling所有・非配布検証資材である。C1でGitHub Packagesへ公開した
timestamped snapshot JARをimmutable baselineとし、現行のArchitecture Contractと
ArchUnit Rulesについて、承認済みPublic API inventoryとjapicmp比較を検証する。

Root Reactorには含めない。baseline JAR、認証情報、Maven settings実値および比較reportは
Gitへ保存しない。

## Gate 2 local verification

要件:

- JDK 21
- Maven Wrapper 3.9.16
- `read:packages`だけを持つPAT classic

```powershell
pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-compatibility.ps1
```

PATはsecure promptから入力するか、実行processの`KOIKI_PACKAGES_TOKEN`環境変数へ設定する。
scriptはPAT scopeをGitHub APIで検査し、GUID付きtemporary directoryと隔離Maven local
repositoryを作成する。C1 JARのSHA-256、現行JARから生成した`public-api.txt`との一致、
japicmp 0.26.1による全Public API modificationなしを確認後、認証情報とtemporary directoryを
`finally`で削除する。

`pom.xml`と`PublicApiInventory.java`はこのscriptからだけ利用するTooling実装であり、
FrameworkまたはConsumer向けartifactではない。

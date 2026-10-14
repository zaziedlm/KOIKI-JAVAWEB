# Reference critical journey E2E verification

P3-C2の非配布Toolingである。Root Reactorには含めず、同じpackage済み`koiki-reference-app` JARと
使い捨てPostgreSQL 17に対して次のcritical journeyを実行する。

1. Bearer APIで経費申請を作成・参照・提出する。
2. Session認証した実Chromiumでmaster HTMX検索と同申請の承認を行う。
3. Bearer APIで承認済み状態を再取得する。
4. DB state / version / line total、Business / Security Audit、sanitized process logを突合する。
5. browser、issuer、application process、PostgreSQL container、port、temp logをcleanupする。

## Boundary

- Playwright Java / Chromium `1.62.0`、Testcontainers、test-only OIDC / JWKSをToolingのtest scopeだけで使用する。
- fixture userは使い捨てDB内だけに作成し、password、RSA key、Bearer token、Cookieおよびsource HMAC keyは
  各runのmemory内だけで生成する。
- Framework / Reference production source、migration、Root Reactor、`koiki-testing`またはProject Templateへ含めない。
- P3-B3 / B4とP3-C1のfocused negative matrixを複製せず、1本のhappy pathだけを検証する。
- 本commandはlocal / CI共通entrypointである。main rulesetでは`Phase 3 Critical Journey E2E`として8件目の
  required checkに登録済みだが、Toolingの非配布境界とRoot Reactor外の位置づけは変わらない。

## One-time browser setup

Chromiumが未導入の環境だけ、明示的にinstallする。

```powershell
.\mvnw.cmd -f .\build-support\reference-e2e-verification\pom.xml `
  dependency:build-classpath `
  "-Dmdep.outputFile=target/playwright-classpath.txt"

$playwrightClasspath = Get-Content `
  .\build-support\reference-e2e-verification\target\playwright-classpath.txt
java -cp $playwrightClasspath com.microsoft.playwright.CLI install chromium
```

## Run

非clean部分buildのincremental出力へ依存しないよう、Reference JARをclean packageしてから実行する。

```powershell
.\mvnw.cmd -pl koiki-reference-app -am -DskipTests clean package
.\mvnw.cmd -f .\build-support\reference-e2e-verification\pom.xml test
```

Docker EngineとPlaywright Chromiumが必要である。testはdynamic portだけを使用し、成功・失敗のどちらでも
外部processと一時データをcleanupする。秘密値をcommand line、test reportまたはEvidenceへ出力しない。

## S1の明示選択式資源制限

S1の承認済みbaseline／変更後比較だけで、次のsystem propertyを明示する。未設定／falseは従来動作、true以外の不正値は拒否する。notificationの有効化とは別設定である。

```powershell
.\mvnw.cmd -o -f build-support/reference-e2e-verification/pom.xml "-DforkCount=1" "-DreuseForks=false" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dkoiki.reference.verification.resource-limits.enabled=true" test
```

Mavenは実行区間だけMAVEN_OPTSに-Xmx768mを設定し、finallyで元の値を復元する。test forkも768 MiBを要求する。既存Chromiumのみを使い、browser／imageの不足時は取得せず停止する。

有効時はDB memory1 GiB／CPU1／max_connections16、子Reference JVM heap768 MiB、子pool最大4／minimum idle1を適用する。container inspectとDB照会、JVM起動診断とHikariの実効設定ログで値を照合し、不一致を失敗とする。`S1_RESOURCE`は当該runのcontainer ID／child PIDと確認済み値だけを出力する。子logは従来の秘密非出力検査後、heap／poolの安全な診断行だけを`target/s1-resource-limits/sanitized-process.log`に保管し、元の一時logは従来どおり削除する。

Referenceの既存25 classは1 classずつ呼び出し、上記に加えて`-Dtest=<class>`、`-Dsurefire.failIfNoSpecifiedTests=false`、`-Dspring.datasource.hikari.maximum-pool-size=4`、`-Dspring.datasource.hikari.minimum-idle=1`を指定する。各実行後に新しいXML・期待件数・exit codeと当該DB停止を確認してから次へ進む。E2Eの直前は必要証拠を保全したうえでclean packageする。baselineと変更後は同じ集合・設定を使う。

run別に秘密を除いたXML／log・source hash・資源・cleanup結果を保存する。開始時memory8 GiB／disk10 GiB、DB同時1、既存25 class＋E2Eは合計60分、raw合計1 GiB、同原因・条件のrerun最大1回を維持する。上限／残存／失敗時は後続を止め、証拠を保持する。別runのcontainer／processは停止しない。

承認・class集合・停止条件の正本は[初回実行資料](../../docs/development/phase4-s1-reference-foundation-execution-review-20261006.md)、結果は[2026-10-07 Evidence](../../docs/architecture/validation/phase4-s1-reference-foundation-20261007.md)を参照する。通常local／CI command、production設定／Framework／POMは変更しない。

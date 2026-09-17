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
- Framework / Reference production source、migration、Root Reactor、`koiki-testing`、Project Template、workflowへ含めない。
- P3-B3 / B4とP3-C1のfocused negative matrixを複製せず、1本のhappy pathだけを検証する。
- 本commandはCI候補のlocal entrypointであり、workflowまたはrequired checkを追加しない。

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

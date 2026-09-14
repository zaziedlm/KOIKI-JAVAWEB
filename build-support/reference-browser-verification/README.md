# Reference browser verification

このdirectoryは、P3-B3以降のReference Applicationを実browserで確認するための非配布Toolingである。
Root Reactor、Framework / Reference artifact、`koiki-testing`、Project TemplateおよびCustomer dependencyには
含めない。

## Boundary

- Playwright Java `1.62.0`とChromiumだけをToolingのtest scopeで使用する。
- 通常のRoot Reactor `clean verify`はbrowser binaryをdownloadせず、本Toolingを暗黙実行しない。
- login credentialをsource、設定file、Maven command historyまたはEvidenceへ保存しない。
- package済みReference Applicationと使い捨てPostgreSQLは、Local Run Guideに従って別途起動する。
- P3-B3ではmaster検索、history、fragment swap、Validation、CSRF注入・拒否を初期journeyとする。
- P3-B4の2 Session競合、cache / TTLおよびGate Bの横断journeyを先行しない。

## One-time browser setup

通常buildとは分離した明示操作として、Chromiumを一度installする。

```powershell
.\mvnw.cmd -f .\build-support\reference-browser-verification\pom.xml `
  dependency:build-classpath `
  "-Dmdep.outputFile=target/playwright-classpath.txt"

$playwrightClasspath = Get-Content `
  .\build-support\reference-browser-verification\target\playwright-classpath.txt
java -cp $playwrightClasspath com.microsoft.playwright.CLI install chromium
```

browser binary取得にはnetwork accessが必要である。versionを変更した場合は、Java dependencyとbrowser binaryを
同じPlaywright versionの組合せで再setupする。

## Run

Local Run Guideに従いapplicationを起動し、使い捨てdemo dataを投入する。seed scriptが表示したlogin emailと
passwordをRepositoryへ保存せず、次のPowerShell promptだけへ渡す。

```powershell
$password = Read-Host "Demo login password" -AsSecureString
.\build-support\reference-browser-verification\verify-reference-htmx.ps1 `
  -LoginEmail "表示されたdemo login email" `
  -LoginPassword $password
```

目視を併用する場合だけ`-Headed`を加える。実行後はbrowser contextを閉じ、Local Run Guideに従ってapplicationと
使い捨てDB containerを停止・破棄する。

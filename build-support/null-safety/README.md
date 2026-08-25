# Null Safety Verification

Phase 1a B4のJSpecify / NullAwayを検証する、Tooling所有の非配布fixtureです。
Root Reactor、正式artifactおよび通常のproduction sourceには含めません。

次のscriptは、同じfixtureをMaven Wrapperで三段階検証します。

```powershell
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

1. `src/positive/java`をproduction sourceとしてcompileし、成功を確認する。
2. `nullaway-negative` profileで`src/negative/java`へ切り替え、NullAwayのdiagnosticを伴う失敗を確認する。
3. profileなしの正常sourceへ戻し、再度成功を確認する。

profileはMavenのsource directoryだけを切り替えるため、検証中にtracked fileを書き換えません。
negative sourceは意図的違反を持つ隔離fixtureであり、通常buildや配布artifactへ追加しません。

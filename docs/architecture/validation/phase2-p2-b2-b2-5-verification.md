# Phase 2 P2-B2 B2-5 Regression / Evidence verification

## 1. Status and scope

- **Verification date:** 2026年9月7日
- **Work package:** `P2-B2 / B2-5`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`
- **Approved by:** Shuichi Kataoka、2026年9月7日
- **Ownership:** Tooling（回帰・artifact検査）+ Documentation（P2-B2 evidence集約）
- **Baseline:** B2-C1〜C10、B2-2、B2-3、B2-4 Architecture Owner承認済み

本記録はB2-2〜B2-4で個別に確認したIdentity core、local authentication、attempt / lock、
administrationを同一HEADで再検証し、P2-B2のcloseout判断に必要な証拠を集約する。
新しいproduction機能、Spring Session adapter、Reference `identity`またはreset機能は追加しない。

## 2. Evidence summary

| Verification | Result | What it proves |
|---|---|---|
| P2-B2 aggregate | SUCCESS、T0〜T4 56 / 56 | B2-2〜B2-4と既存Security / Audit回帰が同一fixtureで共存する |
| Formal release staging | Reactor 13 / 13 SUCCESS | Rootの正式release unitだけを隔離Maven repositoryへstageできる |
| Identity inventory | 10 public types、12 properties、5 error codes、8 tables、2 imports | 承認済みB2 contractとの完全一致。未承認の公開面を増やしていない |
| Migration / dependency | SUCCESS | production migration、PostgreSQL Flyway module、必須依存、deferred dependency非混入 |
| Sensitive-content boundary | SUCCESS | DB / Auditの意味的assertionと、正式JAR・実行log・Surefire reportのpattern scan |
| Root regression | Reactor 13 / 13、Architecture Contract 4 / 4、ArchUnit 66 / 66 | Repository baselineとmodule境界を破壊していない |
| Null Safety | positive SUCCESS、negative expected failure、restore SUCCESS | NullAwayが有効で、違反を拒否した後に正常状態へ戻る |
| Cleanup | temporary repository削除、fixture非install、PostgreSQL / Ryuk残存0 | 非配布fixtureと一時resourceが検証後に残らない |

## 3. Aggregate verification

実行command:

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b2-identity-core.ps1
```

結果:

```text
Formal release staging: Reactor 13 / 13 SUCCESS
T0-T4: 56 tests, Failures: 0, Errors: 0, Skipped: 0
Identity Public API: 10 types
Identity configuration properties: 12
Identity error codes: 5
Identity tables: 8
Identity Auto Configuration imports: 2
Dependency / deferred-artifact boundary: SUCCESS
Sensitive-content scan: SUCCESS
Temporary repository cleanup: SUCCESS
```

13 suiteの内訳は、Security baseline 6 suite / 23 tests、Audit transaction 1 suite / 8 tests、
Identity public contract / core migration 2 suite / 8 tests、authentication / attempt 2 suite / 9 tests、
administration 2 suite / 8 testsで、合計56 testsである。

### 3.1 Inventory and distribution boundary

- `public-api.txt`と正式JARを照合し、Public Java型10件、property 12件、error code 5件を完全一致で検査した。
- 正式JAR直下のpublic packageへinternal型が漏れていないこと、production migrationが8 tableだけを作ることを検査した。
- password reset、Spring Session、mail、SAML、WebFlux、AWS SDKがIdentity production artifact / runtime dependencyへ
  混入していないことを検査した。
- fixture artifactが隔離Maven repositoryへinstallされていないことを検査した。

### 3.2 Secret / PII boundary

次の2種類を分けて検証した。

1. DB row / Auditの意味的検査
   - raw passwordをcredential rowへ保存せず、encoded valueにもraw値を含めない。
   - SOURCE fingerprintへraw送信元を保存しない。
   - Identity管理Audit payloadへemail形式の値を保存しない。
   - 公開failureへemail、passwordまたはSQL詳細を含めない。
2. 出力artifactの静的検査
   - 正式Identity JAR、表示前のMaven出力、Maven実行log、全Surefire reportを対象とする。
   - private key、credential代入、SOURCE HMAC key代入、email形式PII、Basic / Bearer Authorization headerを拒否する。
   - parameterized SQLの`encoded_password=?`は秘密値を含まないため許容する。
   - Maven失敗時も、生成済みの正式JAR、実行log、Surefire reportをscanしてから元の検証失敗を通知する。

fixture test classには`.test` domainの合成emailや合成passwordを意図的に入力値として保持するため、fixture JARを
PII pattern scanの対象にはしない。代わりに、そのJARが正式release unitや隔離repositoryへ入らないことを機械検査する。

## 4. Repository regression and Null Safety

```text
./mvnw.cmd --batch-mode --no-transfer-progress clean verify
Reactor: 13 / 13 SUCCESS
Architecture Contract: 4 / 4 SUCCESS
ArchUnit: 66 / 66 SUCCESS
Error Prone / NullAway compilation: SUCCESS
```

```text
./build-support/null-safety/verify-null-safety.ps1
Positive fixture: SUCCESS
Negative fixture: expected NullAway diagnostic and build failure
Restore fixture: SUCCESS
```

独立Null Safety検査は、単に正常sourceがcompileできるだけでなく、`@Nullable`をnon-null戻り値として返す負例を
NullAwayが拒否することまで確認した。

## 5. Cleanup and execution notes

- aggregateはGUID付きtemporary directoryだけへ隔離Maven repository、dependency tree、実行logを生成し、`finally`で削除した。
- Testcontainers終了後、`postgres:17-alpine`と`testcontainers/ryuk:0.14.0`の残存containerは0件だった。
- sandbox内からの初回実行はDocker named pipeへのアクセス拒否で停止した。権限を付与した同一scriptの再実行は成功しており、
  product / test failureとしては扱わない。
- 初回失敗時の診断から、Spring test contextがfixture HMAC設定値をfailure reportへ含め得ることを確認した。
  fixture HMAC値を`@DynamicPropertySource`から供給するよう変更し、失敗contextのproperty一覧へ値を含めない構造にした。
- Docker named pipeを利用できない経路を負例として再実行し、Testcontainers起動失敗後にも機微情報scanが完走すること、
  生成された失敗reportのraw HMAC値とHMAC代入形式がともに0件であることを確認した。
- その後Docker利用可能経路でaggregateを再実行し、T0〜T4 56 / 56と全出力scanが成功することを確認した。

## 6. Compatibility boundary and remaining work

- P2-B2では、承認済みIdentity inventoryと現在のJARを完全一致で検査する。
- 既存`verify-public-api-compatibility.ps1`のremote baselineはPhase 1aのArchitecture Contract / ArchUnit Rules用であり、
  Identity artifactを比較しない。このため、それをP2-B2のIdentity互換性証拠として過大claimしない。
- Identityの公開baseline登録とjapicmp方針は、package / Consumer境界を扱うGate B以降でOwner reviewする。
- 実Spring Session row削除、2 process継続、旧Cookie拒否、logout、cleanup / single executionはP2-B3へ残す。
- Reference `identity`のHTTP / MVC、Method Security、管理journeyはP2-B4へ残す。

## 7. Architecture Owner review points

1. B2-2〜B2-4の個別承認を、同一HEADの56 testsとinventory検査でcloseout evidenceへ集約してよいか。
2. DB / Auditは意味的assertion、配布物・表示前log・reportは成功／失敗時pattern scan、合成値を持つfixtureは
   非配布検査とする境界が妥当か。
3. P2-B2ではIdentity inventory完全一致までとし、公開baseline / japicmpをGate B以降へ残す判断が妥当か。
4. temporary repository、fixture install、Testcontainersのcleanup evidenceがP2-B2として十分か。
5. Spring Session実効果とReference journeyを成功claimせず、P2-B3 / P2-B4へ残しているか。

推奨判断は、上記5点を承認してP2-B2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次のCPをP2-B3とすることである。

2026年9月7日、Architecture Ownerは上記5点をreviewし、失敗時reportのSecret / PII境界に対するTooling補強、
Docker接続失敗の負例および補強後のT0〜T4 56 / 56正常回帰を確認した。5点を最終承認し、P2-B2を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とする。次のCPはP2-B3 Spring Session JDBCである。

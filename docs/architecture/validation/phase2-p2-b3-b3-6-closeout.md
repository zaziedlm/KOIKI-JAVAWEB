# Phase 2 P2-B3 B3-6 closeout

## 1. Status and boundary

- **Verification date:** 2026年9月8日
- **Work package:** `P2-B3 / B3-6`
- **Verified commit:** `001e619` (`feat(session): add B3-5 cleanup single execution`)
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`
- **Approved by:** Shuichi Kataoka、2026年9月8日
- **Ownership:** Tooling / Architecture Evidence
- **Framework change in B3-6:** なし

B3-C1〜C10およびB3-2〜B3-5の成果を、cleanな同一HEADのT0〜T6、Root Reactor、Null Safety、
artifact / dependency / Public API inventory、sensitive output、process / container / temporary resource cleanupとして集約した。
Reference `identity`、全Framework migration upgrade、workflow接続およびrequired check変更は本closeoutへ含めない。

## 2. Three consecutive T0-T6 aggregates

各roundは次の順序で、新しい隔離Maven repositoryとPostgreSQLを使用して実行した。

1. `verify-p2-b3-session-core.ps1`
2. `verify-p2-b3-session-two-process.ps1`
3. `verify-p2-b3-session-cleanup-process.ps1`

結果は次のとおりである。

| Round | T0-T5 core / inventory | B3-4 Web T6 | B3-5 non-web T6 | Total |
|---:|---:|---:|---:|---:|
| 1 | SUCCESS `00:02:16.8956236` | SUCCESS `00:02:01.8283952` | SUCCESS `00:02:39.6736898` | `00:06:58.3977086` |
| 2 | SUCCESS `00:02:11.1031296` | SUCCESS `00:02:00.6640003` | SUCCESS `00:02:34.7436844` | `00:06:46.5108143` |
| 3 | SUCCESS `00:02:14.4061157` | SUCCESS `00:01:55.4574855` | SUCCESS `00:02:40.8388583` | `00:06:50.7024595` |

各core runは正式release unit 14 artifactの隔離staging、T0〜T5 `72 / 72`、Session Public API 3型、
Spring property 9件、Session table 2件、Auto Configuration import、runtime dependency / deferred dependency、
fixture非配布および正式JAR / Maven log / Surefire reportのsensitive-content scanを含む。

各B3-4 runは同一package済みJAR 2 processでA/B Session継続、A停止後のB継続、5種のIdentity mutationによる
別process旧Cookie拒否、control Session継続、Session DELETE権限障害時のrollback、logout local消去 / recovery、
直接HTTPと信頼proxy下のCookie属性を確認した。

各B3-5 runは同一package済みnon-web JARで期限切れだけのcleanup、winner exit `0`、contender exit `10`、
DELETE副作用1回、winner OS kill後のadvisory lock消失、transaction rollbackおよびretry exit `0`を確認した。

初回集約試行では、子Harnessが成功表示を返した後のPowerShell native `$LASTEXITCODE`を子script全体の結果とみなし、
B3-4成功を集約側が失敗と誤判定した。製品またはHarness assertionの失敗ではない。集約判定をPowerShellのterminating exception基準へ
修正し、上表の完全な3 roundを最初から数え直した。

## 3. Root and Null Safety regression

3回連続aggregateと同じcommitで最終検証した。

```text
Root clean verify: Reactor 14 / 14 SUCCESS
Architecture Contract: 4 / 4 SUCCESS
ArchUnit: 66 / 66 SUCCESS
Error Prone / NullAway compilation: SUCCESS

Null Safety positive: SUCCESS
Null Safety negative: expected NullAway diagnostic and build failure
Null Safety restore: SUCCESS
```

negative fixtureでは`returning @Nullable expression from method with @NonNull return type`を確認し、その後のrestore buildが成功した。

## 4. Final inventory and cleanup evidence

| Boundary | Result |
|---|---|
| Formal release unit | Reactor 14 artifact、B3 fixture artifact非混入 |
| Session Public API | `SessionCleanup`、`SessionCleanupResult`、`SessionCleanupException`の3型 |
| KOIKI Session property | 追加0件。Spring / Boot標準9件 |
| Framework Session schema | `koiki_session` / `koiki_session_attributes`の2 table |
| Runtime dependency | Spring Session JDBC / Identity / Data / JSpecify。Redis / SAML / WebFlux / AWS SDKなし |
| Sensitive output | private key、credential assignment、HMAC key、email形式PII、Authorization header、Session Cookie値なし |
| Process | 各Harnessが開始したJava process残存0 |
| Container | `koiki-b34-*` / `koiki-b35-*`残存0 |
| Temporary directory | `koiki-session-core-*` / `koiki-session-two-process-*` / `koiki-session-cleanup-*`残存0 |
| Fixture build | workspace内のB3-4 / B3-5 fixture `target`残存0、隔離repositoryへのinstallなし |

検証中断後の再確認では、表示された既存Java process 3件はいずれもB3-6開始より前から稼働しており、Harness childではなかった。
実行sessionは中断中も継続し、Round 2 non-webとRound 3の3 stepを正常終了していた。HEAD、worktree、container、temp状態を
再確認してからroot / Null Safety検証へ進んだ。

## 5. Reproducible closeout runner and CI candidate

`verify-p2-b3-session-closeout.ps1`は、clean worktreeと同一HEADを前後で検査し、上記3 Harnessを3回連続実行する。
各round後にB3 container、一時directory、workspace fixture targetの残存を拒否し、最後にroot regressionとNull Safetyを実行する。

Milestone B PostgreSQL integration jobの候補条件である3回連続成功、実行時間、flakiness、process / container cleanupは満たした。
ただしGate BはP2-B1〜B4 aggregateとpackaged journeyを対象とするため、現時点ではworkflowを追加せずrequired化しない。
P2-B4完了後、Gate Bで既存3 Harnessの1回実行をCI候補としてreviewする。local 3連続closeoutをCIの毎回実行へそのまま持ち込むかは、
約7分 / roundの実測時間とflakinessを踏まえて別途判断する。

## 6. Architecture Owner review points

1. B3-C1〜C10およびB3-2〜B3-5を、同一HEADの3回連続T0〜T6成功でP2-B3 closeoutとして集約してよいか。
2. T5の実PostgreSQL assertion、B3-4のHTTP / Cookie / DB / process assertion、B3-5のDB lock / exit / crash assertionの組合せが十分か。
3. inventory完全一致、sensitive-output scan、非配布fixtureおよびresource cleanupがB3 distribution境界を満たすか。
4. 3回連続成功によりMilestone B CI候補条件を満たす一方、workflow接続 / required化をGate Bへ留保する判断が妥当か。
5. P2-B3のclaimをSession Foundationへ限定し、Reference `identity`をP2-B4、全migration upgradeをP2-C1へ残しているか。

推奨判断は、上記5点を承認してP2-B3を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次のCPをP2-B4
Reference `identity`とすることである。

2026年9月8日、Architecture OwnerはB3-4およびB3-5の個別Evidenceを含めて上記5点を承認した。
P2-B3を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次のCPをP2-B4 Reference `identity`とする。
Milestone B CI接続 / required化はGate Bへ留保し、汎用non-web Worker / Batch基盤は本承認に含めない。

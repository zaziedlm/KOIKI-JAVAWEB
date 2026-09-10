# Phase 2 P2-B3 B3-4 Two-process continuity start handoff

## 1. Handoff status

- **Handoff date:** 2026年9月8日
- **Architecture Owner:** Shuichi Kataoka
- **Branch:** `feature/phase2-security-local-identity-session-audit`
- **Approved baseline commit:** `2f81c27`（B3-3 Session invalidation / logout）
- **Phase status:** `P2-B3 B3-3 COMPLETE / ARCHITECTURE OWNER APPROVED — B3-4 READY`
- **Ownership:** Framework（既存Session / Identity runtime）+ Tooling（非配布T6 process fixture / Harness）
- **Primary target:** package済み同一JARの2 process Session継続、片系停止、別process失効、実Session store操作障害
- **Deferred:** cleanup / single executionはB3-5、T0〜T6 closeoutはB3-6、Reference `identity`はP2-B4

本handoffは、別端末・新規AI対話でB3-4を開始するための作業正本である。handoff追加commitをremoteへpushした後は、
新端末で同branchをcheckoutし、`git status --short`が空であることと、本書を含むHEADであることを確認してから作業する。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `phase2-p2-b3-start-handoff-20260907.md`
5. `../architecture/validation/phase2-p2-b3-contract-review.md`のB3-C4、B3-C5、B3-C9、B3-C10
6. `../architecture/validation/phase2-p2-b3-b3-2-verification.md`
7. `../architecture/validation/phase2-p2-b3-b3-3-verification.md`
8. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`のP2-B3進捗
9. `../architecture/validation/phase2-security-test-design.md`のT5 / T6境界

## 3. Approved baseline — do not redesign

1. browser認証はserver-side HTTP Session、永続化はSpring Session JDBCを使用する。
2. Session tableはFramework Flyway所有の`koiki_session` / `koiki_session_attributes`だけとする。
3. principal indexはimmutable `FrameworkUserId.toString()`であり、email等の可変PIIを使用しない。
4. 既存`UserSessionInvalidator`とB3-3 internal adapterのsignature / ownershipを変更しない。
5. local logoutはSpring Security標準chainを利用し、永続削除失敗時もlocal context / credential / Cookieを消去するが、
   成功redirectとして偽装しない。
6. `ON_SAVE` / `ON_SET_ATTRIBUTE`、initializer `never`、Framework table名、Web cleanup無効を維持する。
7. Starter導入自体をSession persistenceのopt-inとし、KOIKI独自enabled propertyを追加しない。
8. Spring Session / Servlet / JDBC型、process fixture型、Cookie値またはSession IDをKOIKI Public APIへ追加しない。
9. package済みprocess、fixture route / identity、failure orchestrationはTooling所有であり、正式JAR、BOM、`koiki-testing`へ昇格しない。
10. Redis、Oracle、AWS固有adapter、cleanup / advisory lock実装、Reference UIをB3-4へ混在させない。

## 4. Existing implementation and evidence

### 4.1 Framework runtime

- `koiki-starters/koiki-starter-session-jdbc`
  - Spring Session JDBC Auto ConfigurationとFramework migration
  - `KoikiJdbcUserSessionInvalidator`
  - `KoikiSessionLogoutHandler`
- `koiki-starters/koiki-starter-identity`
  - password、disable、user Role、Role Permission、external unlink時の同期`UserSessionInvalidator`呼出し
- Public contractは既存`org.koikifw.identity.UserSessionInvalidator`だけであり、B3-4では追加しない。

### 4.2 Last accepted evidence

```text
B3-3 aggregate: T0-T5 15 suites / 66 tests SUCCESS
Root clean verify: Reactor 14 / 14 SUCCESS
Architecture Contract: 4 / 4
ArchUnit: 66 / 66
Error Prone / NullAway: SUCCESS
```

B3-3のHTTP証拠はMockMvcによるServlet filter chain上の動作確認である。package済みprocessへのnetwork越しHTTP、
2 process共有、process停止、実Session store操作障害はB3-4で初めてclaimする。

## 5. B3-4 implementation boundary

### 5.1 Tooling-owned package fixture

`build-support/security-foundation-verification`配下へ、B3-4で即時使用する非配布fixture applicationとHarnessを追加する。
候補名は次とするが、既存Maven構成との整合を確認して確定する。

- fixture: `session-two-process-fixture/`
- Harness: `verify-p2-b3-session-two-process.ps1`
- Evidence: `../architecture/validation/phase2-p2-b3-b3-4-verification.md`

fixtureは正式release unitを隔離Maven repositoryへstageした後、そのartifactをConsumerとしてpackageする。同じ1個の
executable JARを、異なるloopback portでprocess A / Bとして起動する。fixture artifact自体をBOM、Root Reactorの正式配布単位、
隔離release repositoryまたは`koiki-testing`へinstallしない。

fixtureに限り、readiness、認証済みprincipal確認、Identity管理操作のための最小routeと一時identityを持てる。
production sourceへtest endpoint、sleep、failure switch、固定port、固定credentialを追加しない。routeはloopbackからだけ利用し、
実行ごとに生成するcredential / Cookie / Session IDをconsole、Maven log、process logまたはEvidenceへ出力しない。

### 5.2 Two-process scenario

同一PostgreSQLと同一package済みJARに対して、Harnessから次を外部観測する。

1. clean fixture DBへprocess Aを起動し、bounded pollingでreadinessを確認する。
2. process Bを別portで起動し、A / Bが同一build / DBへ接続していることを確認する。
3. Aでtarget userへloginし、Session Cookie、Session row 1件、immutable principal indexを確認する。
4. A由来CookieをBへ提示し、同じFramework user / Permissionとして認可されることを確認する。
5. Aだけを停止し、同じCookieでBの認証済みrequestが継続することを確認する。
6. targetとは別のcontrol userもSessionを持たせ、後続失効操作でcontrol Sessionが継続することを確認する。
7. Bまたは同じfixtureの管理routeからtargetのIdentity mutationを実行し、A由来の旧CookieをBへ再提示して未認証となること、
   対象Session rowだけが削除されることを確認する。

Identity mutationは少なくとも次の承認済み失効経路を漏れなく外部確認する。操作間でfixture stateを明確に再作成し、
1操作の結果を別操作の証拠として流用しない。

| Mutation | Required observation |
|---|---|
| account disable | target旧Cookie拒否、disabled状態、control Session継続 |
| password変更 | target旧Cookie拒否、password更新、control Session継続 |
| user Role変更 | target旧Cookie拒否、Role更新、control Session継続 |
| Role Permission変更 | 影響Roleを持つ全userの旧Cookie拒否、非対象user継続 |
| external unlink | target旧Cookie拒否、link解除、別認証手段を残す管理unlink条件 |

全table truncate、process全停止、Cookie jar全消去を「対象userの全Session失効」の代用にしない。

### 5.3 Session store operation failure

Identity DBとSession storeは同一PostgreSQLを使用するため、container全停止だけではrequestがIdentityまたはSession読取時点で止まり、
invalidatorの検索 / 削除またはlogoutの削除失敗へ到達した証拠にならない。B3-C5の境界を決定的に確認するため、Harnessは
app用の非owner DB roleを使用し、管理connectionからSession tableの`DELETE`権限だけを一時的に失効させる方式を第一候補とする。
`SELECT`とIdentity table更新権限を維持し、実PostgreSQLのSession削除操作だけを失敗させる。

この方式で次を確認する。

1. password / Role / Permission / external unlink等の全Session失効を伴うmutationはsafe failureとなる。
2. mutationとBusiness Auditはrollbackし、既存Session rowは残る。先行削除がある場合は復元を要求しない。
3. logoutは成功redirectにならず、responseでCookie消去を指示し、current clientのCookie jarからCookieが消える。
4. 永続Session rowは削除失敗により残り得るため、コピー済み旧Cookieまで失効したとはclaimしない。
5. operator向けlogは失敗を識別できるが、credential、Cookie、Session ID、JDBC URL、SQL、stack trace等を外部Evidenceへ露出しない。
6. `finally`で権限を復元し、正常なSession削除と後続fixture cleanupが可能であることを確認する。

もしPostgreSQL権限制御でSpring Sessionの対象operationだけを隔離できないEvidenceが得られた場合は、production failure switchを
追加せず作業を止め、B3-C5 / B3-C9へ戻る。

### 5.4 Cookie / proxy observation

- 通常のloopback HTTP継続試験ではtest scopeだけで`Secure=false`を明示できる。
- 別の外部観測として、信頼proxy条件をfixtureへ設定し、`X-Forwarded-Proto=https`を認識したlogin responseの
  `Set-Cookie`に`Secure` / `HttpOnly` / 承認済み`SameSite`が存在することを確認する。
- forwarded headerを無条件に信頼するproduction設定や、containerへの直接到達をFramework既定として追加しない。

## 6. Harness safety and determinism

1. PowerShell 7とRepositoryのMaven Wrapper / Java 21を使用する。
2. processは`Start-Process -WindowStyle Hidden`で開始し、Harnessが開始したPIDだけを保持する。
3. port、PID、Cookie jar、log、隔離Maven repositoryはGUID付き一時directoryへ閉じ込める。
4. readiness / process終了は期限付きpollingとし、無期限waitを行わない。
5. success / failureの両方で`finally`を通し、process A / B、DB権限、container、一時directoryを回収する。
6. cleanup対象は解決済み絶対pathと開始済みPIDを照合し、workspace rootや未検証pathを再帰削除しない。
7. logだけで判定せず、HTTP status / redirect、Cookie属性、DB row / principal、process生存状態を組み合わせる。
8. success / failure時のJAR、Maven / process log、reportをsecret / Cookie / Authorization / email形式PII patternで検査する。

## 7. B3-4 exit criteria

- 同一package済みJAR 2 processが同一Sessionを共有する。
- process A停止後もA由来Cookieでprocess Bの認証済みrequestが継続する。
- immutable principal indexと対象Session rowだけの失効を外部観測する。
- disable、password、user Role、Role Permission、external unlink後の旧Cookie拒否と対象外Session継続を確認する。
- 実Session削除operation障害時のIdentity rollbackとlogout local消去 / 非成功結果を確認する。
- loopback継続試験とproduction相当proxy下Cookie属性試験を混同しない。
- T0〜T5 66 testsを維持し、T6 process fixtureが成功する。
- production JAR / Public API / migration / property inventoryにfixture固有物が混入しない。
- 全child process、DB権限、container、一時directoryが成功・失敗の両経路で回収される。
- 結果を`phase2-p2-b3-b3-4-verification.md`へ記録し、Architecture Owner review対象とする。

## 8. Explicitly deferred to B3-5 / B3-6

### B3-5

- `SessionCleanup`、`SessionCleanupResult`、`SessionCleanupException`
- non-web maintenance application lifecycle
- `JdbcIndexedSessionRepository.cleanUpExpiredSessions()`
- PostgreSQL advisory lock、`acquired / contended / failed`、2 process競合、crash recovery

### B3-6

- T0〜T6 closeout aggregate
- 同一HEADでのroot / Null Safety / inventory / sensitive scan最終証拠
- 3回連続安定性とGate B CI候補判断

## 9. Start commands on the new terminal

```powershell
git fetch origin
git switch feature/phase2-security-local-identity-session-audit
git pull --ff-only
git status --short
git log -3 --oneline --decorate
```

`git status --short`が空でなく、既存変更の所有者を確認できない場合は編集を開始しない。B3-3 baseline再確認は次を使用する。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b3-session-core.ps1
```

## 10. Immediate next action

既存Tooling / Maven packagingの境界を確認し、非配布fixture applicationとT6 Harnessの最小構成を設計する。最初の実装sliceは
同一package済みJARのprocess A / B起動、A login、B継続、A停止後B継続までとする。その成立後にIdentity mutation matrix、
Session DELETE権限障害、proxy Cookie属性を追加し、B3-5 cleanup / single executionを先行しない。

# Phase 2 P2-B3 B3-3 Invalidation / logout verification

## 1. Status and scope

- **Verification date:** 2026年9月8日
- **Work package:** `P2-B3 / B3-3`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`
- **Approved by:** Shuichi Kataoka、2026年9月8日
- **Ownership:** Framework（Session invalidation / local logout）+ Tooling（T0 / T5 fixture）
- **Baseline:** B3-C1〜C10およびB3-2 Architecture Owner承認済み

本記録は、B2で公開済みの`UserSessionInvalidator`をSpring Session JDBCへ接続し、immutable Framework user IDによる
全Session失効とSpring Security local logoutの永続Session削除を実証したB3-3の証拠である。同一processでの旧Cookie拒否は
確認するが、package済み2 process間でのSession継続と別processからの旧Cookie拒否はB3-4へ残し、本記録ではclaimしない。

## 2. Implemented artifacts

### 2.1 All-session invalidation adapter

- Session Starter内部の`KoikiJdbcUserSessionInvalidator`が既存Public SPI `UserSessionInvalidator`を実装する。
- `FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME`へ`FrameworkUserId.toString()`を完全一致で渡す。
- 検索結果のSession IDを同期的にすべて削除し、検索または1件でも削除に失敗した場合は例外を伝播する。
- 一部削除後の失敗は安全側の部分失効として扱い、削除済みSessionを補償復元しない。
- email、issuer / subject、role codeをindex keyにせず、Spring repository型や削除件数をPublic APIへ露出しない。

Session Auto ConfigurationはSpring Session JDBC構成後、Identity Auto Configurationより前に実行する。これにより
Session Starterを導入した構成ではinvalidator beanがIdentity管理beanの必須条件を満たす。Customerが同SPIを提供した場合は
`@ConditionalOnMissingBean`により置換を妨げず、Spring Session repositoryがない構成ではadapterを生成しない。

### 2.2 Persistent local logout

Servlet applicationでは、Session StarterがSpring Securityの`HttpSecurity` customizerへinternal `LogoutHandler`を追加する。
独自logout endpointやCookie形式は作らず、POST + CSRF、logout URL、成功時redirectはSpring Security標準を維持する。

Handlerはcurrent `HttpSession.invalidate()`を呼び、成功時はSpring Session JDBC rowを削除する。その後、認証credential、local
`SecurityContext`およびSpring Session Cookieを消去する。標準handlerによる二重Session invalidateを避けるため、標準logoutの
`invalidateHttpSession`だけを無効化し、local状態の消去と成功処理は標準chain上で構成する。

Session store障害で`invalidate()`が失敗した場合もlocal context、credential、Cookie消去を先に完了し、安全な固定messageの
internal例外を送出する。例外を成功handlerまで到達させないことで、永続Session削除失敗を`/login?logout`成功redirectとして
偽装しない。元例外のmessageや接続情報はresponse / logへ転記しない。

## 3. Verification results

### 3.1 Auto Configuration context — 5 / 5

`SessionJdbcAutoConfigurationContextTest`で、B3-2のproperty境界3件に加えて次を確認した。

1. Spring Session JDBC repositoryが存在すると既存SPIのinvalidator beanが1件だけ成立し、repository検索障害を隠さず伝播する。
2. current Sessionのinvalidate失敗時もcredential、local `SecurityContext`、Cookie消去が実行され、固定された安全な例外で失敗する。

2件目はmockした`HttpSession.invalidate()`へ障害を注入するT5 component testである。実PostgreSQL停止時のHTTP外部結果は、
package済みprocessを扱うB3-4の障害試験へ残す。

### 3.2 PostgreSQL invalidation / HTTP logout — 5 / 5

`SessionJdbcCoreMigrationFixtureTest`で、B3-2のmigration、write境界、principal serialization 3件に加えて次を確認した。

1. 対象userに2 Session、対象外userに1 Sessionを保存してinvalidatorを実行し、対象2件とその属性rowだけが削除され、
   対象外1件は維持される。
2. MockMvcによるServlet filter chain上のSpring Security loginでSession rowを作成し、Cookieで認証済みrequestが成立することを
   確認した。POST + CSRF logout後は
   成功redirect、Session row 0件、Cookie `Max-Age=0`を観測し、同じ旧Cookieの再提示が未認証redirectになることを確認した。

Mock Servlet環境は組込みserverのCookie属性適用を外部観測しないため、この試験から`Secure` / `HttpOnly` / `SameSite`を
claimしない。production相当proxy条件でのCookie属性はB3-4またはB3-6で確認する。

### 3.3 Cumulative regression and artifact inspection

```text
Formal release staging: Reactor 14 / 14 SUCCESS
Security / Audit / Identity / Session fixture: 15 suites, 66 tests
Failures: 0, Errors: 0, Skipped: 0
Session context: 5 / 5
Session PostgreSQL core / invalidation / logout: 5 / 5
Session inventory / dependency / sensitive-content inspection: SUCCESS
Temporary isolated repository cleanup: SUCCESS
```

`verify-p2-b3-session-core.ps1`はT0〜T5 66 / 66と既存回帰に加え、正式Session JARの既存SPI実装、internal logout
handler、Public Java型0件、dependency / table / property inventory、fixture非配布、secret / PII patternおよび一時領域cleanupを
検査した。Servlet APIはcompileに必要な`provided` / optional dependencyとし、Session StarterからServlet runtimeを強制しない。

Root回帰も再実行し、次を確認した。

```text
Root clean verify: Reactor 14 / 14 SUCCESS
Architecture Contract: 4 / 4
ArchUnit: 66 / 66
Error Prone / NullAway compilation: SUCCESS
```

## 4. Explicitly deferred work

| Work package | Remaining evidence |
|---|---|
| B3-4 | package済み同一JARの2 process Session継続、片系停止、別processからのIdentity mutation / 旧Cookie拒否、実store障害のHTTP結果、proxy下Cookie属性 |
| B3-5 | non-web cleanup、PostgreSQL internal排他、acquired / contended / failed、crash recovery |
| B3-6 | T0〜T6 closeout、3回連続安定性、最終inventory / cleanup evidence |

B3-3ではcleanup Public API、maintenance runner、test endpoint、failure switchをproduction sourceへ追加していない。

## 5. Architecture Owner review points

1. 既存`UserSessionInvalidator`を変更せず、Session Starter内部adapterをIdentity Auto Configurationより先に提供する境界が妥当か。
2. immutable user IDの完全一致検索で対象userの全Sessionだけを同期削除し、部分失効を補償復元せず失敗を伝播する判断がB3-C5どおりか。
3. Spring Security標準logout chainを拡張し、独自endpoint / Cookie形式を作らず永続Session削除を組み込む構成が妥当か。
4. store障害時にもlocal context / credential / Cookieを消去し、固定messageの失敗として成功redirectを止める挙動が承認済みsemanticsどおりか。
5. B3-3の証拠を同一processの全Session失効 / logoutと注入障害までに限定し、2 process旧Cookie拒否と実store障害をB3-4へ残しているか。

推奨判断は、上記5点を承認してB3-3を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、B3-4
Two-process continuityへ進むことである。

2026年9月8日、Architecture Ownerは上記5点をreviewした。「HTTP動作確認」はMockMvcによるServlet filter chain上の
login / logout確認であり、package済みprocessへのnetwork越しHTTP試験ではないことを明確化した。2 process継続、
別processでの旧Cookie拒否および実Session store障害をB3-4へ残す境界を含めて5点を承認し、B3-3を
`COMPLETE / ARCHITECTURE OWNER APPROVED`とする。次はB3-4 Two-process continuityへ進む。

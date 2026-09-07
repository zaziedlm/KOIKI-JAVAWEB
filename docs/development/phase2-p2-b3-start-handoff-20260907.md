# Phase 2 P2-B3 Spring Session JDBC start handoff

## 1. Handoff status

- **Handoff date:** 2026年9月7日
- **Architecture Owner:** Shuichi Kataoka
- **Branch:** `feature/phase2-security-local-identity-session-audit`
- **Start commit:** `889bb40`（P2-B2 Regression / Evidence closeout）
- **Phase status:** `P2-B3 B3-1 COMPLETE / ARCHITECTURE OWNER APPROVED — B3-2 READY`
- **Ownership:** Framework（Session persistence / invalidation / cleanup contract）+ Tooling（非配布T5 / T6、2 process fixture）
- **Primary target:** Spring Session JDBC、全Session失効、logout、2 process継続、期限切れcleanup / single execution
- **Deferred:** Reference `identity`はP2-B4、全Framework migration / supported upgradeはP2-C1、CI required化はGate B

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`の§2、Milestone B、§6〜§10
5. `../architecture/validation/phase2-p2-b2-contract-review.md`のB2-C10
6. `../architecture/validation/phase2-p2-b2-b2-5-verification.md`
7. `../architecture/validation/phase2-p2-b3-contract-review.md`
8. `../architecture/validation/phase2-security-test-design.md`の§3.7、§4、§9、§10
9. `../architecture/validation/phase1b-cp8-single-execution.md`

## 3. Accepted baseline

P2-B3は次を再設計せずbaselineとして扱う。

1. browser認証はserver-side HTTP Sessionを使用し、Bearer API chainへfallbackしない。
2. Session tableは`koiki_session` / `koiki_session_attributes`、Framework Flyway所有とする。
3. Boot / Spring Sessionのschema自動初期化を無効化し、production DDLの正本をFramework migrationだけにする。
4. B2で承認した同期`UserSessionInvalidator.invalidateAll(FrameworkUserId)`をSpring Session JDBC adapterへ接続する。
5. disable、password変更、Role / Permission変更、external unlinkは永続Session失効に失敗した場合、mutationを成功扱いしない。
6. logoutはlocal SecurityContext / Cookie消去を妨げないが、永続Session削除失敗を成功として偽装しない。
7. Web process内の期限切れcleanupを無効化し、non-web maintenance processだけが実行する。
8. single executionはvendor-neutralな`acquired / contended / failed`結果を外部contractとし、PostgreSQL排他はinternal実装とする。
9. Phase 1b Customer-like Consumerのadvisory-lock codeをFrameworkへcopy / 昇格しない。
10. Redis、Oracle、cloud scheduler固有adapter、Reference UIをP2-B3へ混在させない。

## 4. P2-B3 scope and exit criteria

P2-B3は次をproduction構成と実PostgreSQLの外部観測で実証する。

1. Spring Session JDBCがFramework Flyway管理の2 tableだけを使用する。
2. `FlushMode.ON_SAVE`と`SaveMode.ON_SET_ATTRIBUTE`を開始候補として、保存・変更・読取のDB writeを実測して確定する。
3. Secure / HttpOnly / SameSite、CSRF、session fixation protectionおよびlocal logoutが既存Security contractと共存する。
4. 2つの独立したpackage済みprocessが同一PostgreSQL Sessionを共有し、片方停止後も同じCookieで継続できる。
5. immutable Framework user IDをprincipal indexとして、対象userの全Sessionだけを削除する。
6. disable、password、Permission、external unlink後に旧Cookieを別processへ提示しても再利用できない。
7. Session store障害時のIdentity mutation / logout結果が承認済みfailure semanticsと一致する。
8. Web processにcleanup schedulerが存在せず、non-web maintenance processだけが期限切れrowを削除する。
9. 同じcleanup taskの2 process競合で副作用は1回、contenderは識別可能な結果となり、crash後に再取得できる。
10. T0〜T6 aggregate、root回帰、Public API / property / table inventory、secret / Cookie / PII非露出、process / container cleanupが成功する。

Spring Sessionのfilter順やJDBC repository class自体は実装詳細であり、KOIKI Public APIにしない。外部証拠はHTTP、Cookie、
DB row、process終了結果およびsanitized logを正本とする。

## 5. Required contract review before production code

| ID | Decision | Primary question |
|---|---|---|
| B3-C1 | Artifact placement | Session JDBCを新規optional Starter、Identity Starter、Security Starterのどこで所有するか |
| B3-C2 | Public contract | 既存`UserSessionInvalidator`以外にcleanup use case / result型を公開する必要があるか |
| B3-C3 | Schema / initialization | 公式PostgreSQL schemaを`koiki_`名へ適合し、initializer無効をどう機械保証するか |
| B3-C4 | Save / flush / serialization | `ON_SAVE` / `ON_SET_ATTRIBUTE`、許可するSession属性、serialization境界をどう固定するか |
| B3-C5 | Invalidation / logout | principal index、対象範囲、旧Cookie拒否、store障害をどのtransaction / HTTP結果で扱うか |
| B3-C6 | Cleanup execution | Spring標準`cleanUpExpiredSessions()`をnon-web processから呼ぶか、Framework内部SQLとするか |
| B3-C7 | Single execution | PostgreSQL advisory lockのnamespace / lifecycleとvendor-neutral結果をどこまで共通化するか |
| B3-C8 | Configuration | Session有効化、table名、Cookie、cleanup無効化、maintenance modeのpropertyをどう公開するか |
| B3-C9 | Two-process evidence | 同一JAR / 2 process、port、Cookie jar、片系停止、DB障害をどう決定的に観測するか |
| B3-C10 | CI / distribution | T5 / T6をlocal aggregateとGate B CIへどう分け、fixture / secret / processを残さないか |

B3-C1〜C10の比較・推奨案をArchitecture Ownerが承認するまで、新規Starter、Public API、production migration、
runtime dependencyまたはCI workflowを追加しない。

## 6. Spring standard seams confirmed for review

Spring Session 4.1系の公式仕様では、JDBC Sessionの標準repositoryは`JdbcIndexedSessionRepository`であり、
table名変更時の属性tableは`_ATTRIBUTES` suffixとなる。principal index検索、`deleteById`、
`cleanUpExpiredSessions()`、flush / save mode設定を標準seamとして利用できる。

- Spring Session JDBC reference: <https://docs.spring.io/spring-session/reference/configuration/jdbc.html>
- `JdbcIndexedSessionRepository` 4.1.1 API:
  <https://docs.spring.io/spring-session/reference/api/java/org/springframework/session/jdbc/JdbcIndexedSessionRepository.html>
- `@EnableJdbcHttpSession` 4.1 API:
  <https://docs.spring.io/spring-session/reference/api/java/org/springframework/session/jdbc/config/annotation/web/http/EnableJdbcHttpSession.html>

公式既定はflush `ON_SAVE`、save `ON_SET_ATTRIBUTE`、cleanup毎分である。KOIKIは前2件を実測候補とし、Web cleanupだけを
明示的に無効化する。Spring Sessionを置換する独自Session repositoryや独自Cookie formatは作らない。

## 7. Verification topology

既存`build-support/security-foundation-verification`を累積拡張する。

| Layer | Evidence |
|---|---|
| T0 | dependency、Auto Configuration条件、initializer無効、property / table inventory、deferred dependency非混入 |
| T1〜T4 | P2-A1〜P2-B2のSecurity / OIDC / Audit / Identity 56 testsを維持 |
| T5 | JDBC Session row、Cookie、fixation、logout、principal index全失効、Identity mutation、store障害 |
| T6 | package済み2 process、片系停止、旧Cookie拒否、期限切れcleanup、単一実行競合 / crash recovery |

T5のin-process fixtureだけで2 instance継続をclaimしない。T6は実OS processと同一PostgreSQLを使い、port、readiness、
Cookie jar、DB row、終了codeをHarness側から観測する。production sourceへsleep、failure switch、test endpointを追加しない。

## 8. Work breakdown

### B3-1 — Contract review

- B3-C1〜C10の具体的比較・推奨案を作成する。
- artifact、Public API、property、table、failure semantics、single executionの変更inventoryを型・項目単位で示す。
- **Review artifact:** `../architecture/validation/phase2-p2-b3-contract-review.md`。
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`（2026年9月7日）。
- **Commit point:** B3 contract / implementation handoff。Owner承認前にproduction codeを含めない。

### B3-2 — Session core / migration

- 承認済みartifactへSpring Session JDBC、Auto Configuration、2 table migrationを追加する。
- initializer、table名、flush / save、Cookie既定およびSession属性境界を検証する。

### B3-3 — Invalidation / logout

- `UserSessionInvalidator` adapter、principal index、全Session失効、旧Cookie拒否、logout / store failureを実証する。

### B3-4 — Two-process continuity

- package済み同一JARを2 process起動し、Session共有、片系停止後の継続、別processからの失効を確認する。

### B3-5 — Cleanup / single execution

- Web cleanup無効、non-web cleanup、PostgreSQL internal排他、contended / failed / crash recoveryを確認する。

### B3-6 — Regression / evidence

- T0〜T6 aggregate、root、Null Safety、inventory、secret / Cookie / PII、process / container cleanupを確認する。
- P2-B3 Evidenceを記録し、Owner acceptance後にP2-B4へ進む。

## 9. Downstream task map

| Next work | Main outcome | Must remain out of P2-B3 |
|---|---|---|
| **P2-B3** | Session JDBC、2 process、全失効、logout、cleanup / single execution | Reference管理UI、全migration upgrade |
| **P2-B4** | Reference `identity` Tier 1、Method Security / Audit、管理journey | Framework Entity / Repository直接参照 |
| **Gate B** | P2-B1〜B4 aggregate、packaged journey、Public API互換、CI候補 | required化の無審査変更 |
| **P2-C1** | Audit / Identity / Sessionのmigration順序、clean / supported upgrade | Oracle / Customer migration混在 |

## 10. Stop conditions

次の場合は実装を止め、B3 contract reviewへ戻る。

- OIDC / Bearer専用applicationへSession JDBCまたはIdentity DBを強制する。
- Spring Session / Security / JPA型をKOIKI Public APIへ露出する。
- Spring標準repositoryを置換する独自Session実装が必要になる。
- Web cleanupとmaintenance cleanupが同時実行される。
- Session属性へcredential、token、JPA Entityまたは非portableなCustomer objectを保存する。
- principal indexがemail等の可変PIIに依存する。
- Session store障害時にIdentity mutationまたはlogout成功を偽装する。
- Phase 1b Consumer codeを正式Frameworkへcopyする。
- 2 process、旧Cookieまたはcleanupの成立をmock / in-process testだけでclaimする。

## 11. Immediate next action

B3-2 Session core / migrationへ進み、承認済みartifactへSpring Session JDBC、Auto Configuration、2 table migrationを追加する。
initializer、table名、Web cleanup無効、flush / saveおよびserialization境界をT0 / T5で実測し、契約前提が成立しない場合は
実装を止めて再reviewする。

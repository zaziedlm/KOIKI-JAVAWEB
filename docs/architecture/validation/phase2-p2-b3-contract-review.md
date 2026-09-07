# Phase 2 P2-B3 Spring Session JDBC contract review

## 1. Status and scope

- **Review date:** 2026年9月7日
- **Work package:** `P2-B3 / B3-1`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED — B3-2 READY`
- **Approved by:** Shuichi Kataoka、2026年9月7日
- **Ownership:** Framework（Session persistence / invalidation / cleanup）+ Tooling（非配布T5 / T6 fixture）
- **Production change at review:** 0。Owner承認前にStarter、Public API、migration、runtime dependencyまたはCIを追加しない

本書は、P2-B3で追加するSpring Session JDBC、全Session失効、logout、2 process継続、期限切れcleanupおよび
single executionを、artifact、Public API、property、table、失敗時挙動とEvidenceの単位で比較する。

P2-B2で承認済みの`FrameworkUserId`、`FrameworkPrincipal`、`UserSessionInvalidator`およびIdentity mutationの
transaction semanticsをbaselineとする。Reference `identity`の画面 / ControllerはP2-B4、全Framework migrationの
clean / supported upgradeはP2-C1、Gate B CIのrequired化はGate Bの責務であり、本reviewで先行しない。

## 2. Review principles

1. browser Sessionを利用しないBearer専用applicationへJDBC Sessionを強制しない。
2. `SessionRepositoryFilter`、`JdbcIndexedSessionRepository`、principal index、標準logoutを優先し、独自Session方式を作らない。
3. Session tableとcleanupはFramework責務、maintenance processの起動・終了はApplication assembly責務として分離する。
4. user識別にはimmutableな`FrameworkUserId`だけを使い、email、issuer / subjectまたは表示名をSession indexにしない。
5. Web processのcleanupを無効化し、専用non-web processのsingle executionだけを期限切れ削除の正本にする。
6. Session store障害時に全Session失効を伴うIdentity mutationまたはlogoutの成功を偽装しない。
7. Spring / Servlet / JDBC / PostgreSQL型をKOIKI Public Java APIへ露出しない。
8. Phase 1b Customer-like Consumerの単一実行codeをcopyせず、外部contractとEvidenceだけを再利用する。

### 2.1 Primary specification basis

- Spring Boot 4.1.1 Spring Session:
  <https://docs.spring.io/spring-boot/reference/web/spring-session.html>
- Spring Boot 4.1.1 application properties:
  <https://docs.spring.io/spring-boot/appendix/application-properties/index.html>
- Spring Session 4.1.1 JDBC reference:
  <https://docs.spring.io/spring-session/reference/configuration/jdbc.html>
- `JdbcIndexedSessionRepository` API:
  <https://docs.spring.io/spring-session/reference/api/java/org/springframework/session/jdbc/JdbcIndexedSessionRepository.html>
- Spring Security logout:
  <https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html>

Boot 4.1では`spring-boot-starter-session-jdbc`の導入によりServlet applicationのJDBC SessionをAuto Configureできる。
Spring Sessionの公式PostgreSQL schema、table名customize、principal index、cleanup API、flush / save modeを標準seamとする。

## 3. B3-C1 — Artifact placement

| Candidate | Shape | Evaluation |
|---|---|---|
| **A — optional Session JDBC Starter** | 新規`koiki-starter-session-jdbc`がSpring Session JDBC、migration、invalidator adapter、cleanupを所有する | **Recommended.** 導入したapplicationだけがSession DBを持ち、P2-B3で即時利用されるため空artifactにならない |
| B — Identity Starterへ追加 | IdentityがSession dependency / table / cleanupも所有する | Reject。Bearer APIやSessionを使わないIdentity利用までSession永続化を強制する |
| C — Security Starterへ追加 | default deny、protocol、Session persistenceを同じartifactへ置く | Reject。既存Security StarterのDB非依存性を壊し、Bearer / stateless構成にもDBを持ち込む |
| D — Customer / Reference側だけで構成 | 各applicationがSpring Session設定とmigrationを所有する | Reject。Framework user全失効とschema ownershipが案件ごとに分岐する |

**推奨:** Candidate A。新Starterは`koiki-starter-identity`と`spring-boot-starter-session-jdbc`へ依存する。
この依存は、Session-enabled applicationではFramework user ID、Identity管理と全Session失効を一体で成立させるためである。
Bearer専用applicationは新Starterを導入しない。Redis、Oracle、cloud SDK、Reference UIは依存に含めない。

正式release unitは13から14へ増え、Root Reactor、BOM、`koiki-starters/README.md`、Architecture Contractの許可対象を更新する。

## 4. B3-C2 — Public contract

### 4.1 Invalidation contract

P2-B2で公開済みの次のSPIを変更せず、Session StarterがSpring Session JDBC adapterをbeanとして提供する。

```java
public interface UserSessionInvalidator {
    void invalidateAll(FrameworkUserId userId);
}
```

Identity側から見える契約は「対象userの全KOIKI Sessionを同期失効する」だけとし、Session ID、Spring repository、
principal index名、削除件数を公開しない。adapterが存在しない構成では、B2で承認したとおりIdentity管理beanを成立させない。

### 4.2 Cleanup contract comparison

| Candidate | Shape | Evaluation |
|---|---|---|
| internal `ApplicationRunner`がprocess終了まで所有 | Starterだけでcleanupとexit codeを完結させる | Reject。StarterはCustomerの`main`を所有できず、process終了、task選択、scheduler連携が曖昧になる |
| Spring repositoryをApplicationへ公開 | Customer runnerが`JdbcIndexedSessionRepository`を直接呼ぶ | Reject。Spring型、table、排他方式がApplication契約へ漏れる |
| 汎用single-execution framework | 任意task名、lock key、callbackを公開する | Reject。Phase 1bとP2-B3の2例だけで汎用基盤を固定するのは過剰抽象化 |
| **Session固有use caseを公開** | vendor-neutralなcleanup use case / resultだけを公開し、Applicationがnon-web runnerとexitを所有する | **Recommended.** Framework schemaの操作をFrameworkへ閉じ、Application lifecycleとの境界も明確 |

推奨する概念signatureは次の3型である。名称とJavadocを実装前にinventoryへ固定する。

```java
public interface SessionCleanup {
    SessionCleanupResult cleanUpExpiredSessions();
}

public enum SessionCleanupResult {
    COMPLETED,
    CONTENDED
}

public final class SessionCleanupException extends RuntimeException {
    // safe fixed message; vendor / SQL / lock keyを公開しない
}
```

Applicationは`COMPLETED`をexit `0`、`CONTENDED`をexit `10`、`SessionCleanupException`をexit `1`へ変換する。
未知taskや引数不正のexit `64`はApplication command adapterの責務であり、Session Starterへ汎用task parserを追加しない。
`acquired`はcleanup開始後の内部状態であり、Public resultは正常終了した`COMPLETED`と非取得の`CONTENDED`だけとする。

## 5. B3-C3 — Schema and initialization

| Concern | Recommended decision |
|---|---|
| tables | `koiki_session`、`koiki_session_attributes`の2 tableだけ |
| migration owner | `koiki-starter-session-jdbc`内のFramework Flyway migration |
| base schema | Spring Session 4.1.1の公式PostgreSQL schemaを列型・indexともbaselineにし、table / constraint / index名だけ`koiki_`へ適合 |
| initialization | `spring.session.jdbc.initialize-schema=never`を必須にしてBoot / Spring Session initializerを無効化 |
| table binding | `spring.session.jdbc.table-name=koiki_session`を必須にし、属性tableは標準の`_ATTRIBUTES` suffixを使う |
| cleanup scheduler | `spring.session.jdbc.cleanup-cron=-`を必須にし、Web / maintenance双方のSpring内蔵schedulerを無効化 |

予定する物理列は次のとおりである。

| Table | Columns / indexes |
|---|---|
| `koiki_session` | `primary_id char(36)` PK、`session_id char(36)` unique、creation / last-access / expiryのepoch millis、max inactive interval、nullable `principal_name varchar(100)`。expiryとprincipal indexを付与 |
| `koiki_session_attributes` | session PKへのFK `ON DELETE CASCADE`、`attribute_name varchar(200)`との複合PK、`attribute_bytes bytea` |

Starterは上記3つのSpring標準propertyを低優先度defaultとして供給するだけでなく、実効値が異なる場合にstartup failureとする。
これによりCustomer overrideでinitializer、別tableまたはWeb cleanupが静かに有効になることを防ぐ。独自`koiki.*`の同義propertyは作らない。
P2-B3ではclean migrationを検証し、他のFramework migrationとの順序とsupported upgradeはP2-C1で再検証する。

## 6. B3-C4 — Save, flush and serialization

### 6.1 Save / flush mode

| Candidate | Evaluation |
|---|---|
| `FlushMode.IMMEDIATE` | request途中のwriteを増やし、未完了requestの中間状態も永続化し得るため既定にしない |
| `SaveMode.ALWAYS` | read中心requestでも全属性writeが増えるため不採用 |
| `SaveMode.ON_GET_ATTRIBUTE` | mutable属性のin-place変更には強いが、readがwrite対象化しやすいため不採用 |
| **`ON_SAVE` + `ON_SET_ATTRIBUTE`** | **Recommended candidate.** Spring / Boot既定で、request完了時に明示変更属性だけを保存できる |

開始候補は`spring.session.jdbc.flush-mode=on-save`、`spring.session.jdbc.save-mode=on-set-attribute`とする。
T5で新規Session、属性追加、read-only request、属性変更、logoutのSQL / row変化を実測し、SecurityContext保存または
principal indexに欠落があれば実装を止めて再reviewする。性能上の推測だけでSpring既定を置換しない。

### 6.2 Serialization boundary

- Spring標準の`bytea`と既定serializationを開始候補とし、独自JSON形式またはCustomer `ObjectMapper`を正本にしない。
- Framework / ReferenceがSessionへ保存してよいのはSpring Security context、immutable Framework user ID、認証元、
  Permission snapshotおよびCSRF等のSpring標準属性に限定する。
- raw / encoded password、OAuth token、client secret、HMAC key、email、JPA Entity、lazy proxy、request / response、
  DataSourceまたは任意のCustomer業務objectをFramework codeから保存しない。
- 永続化されるinternal principalはcredentialを持たず、安定した`serialVersionUID`と最小fieldを持つ。認証直後と
  package済み同一versionの2 process間でdeserializeできることを実証する。
- Customerが独自Session属性を追加する場合、そのserialization互換性と機微情報管理はCustomer責務であり、KOIKIの
  rolling-upgrade保証には含めない。Framework principalのversion間互換はGate B / release互換性で別途扱う。

DBを直接改変できる主体を信頼境界内とするが、serialized bytesをlog、Audit、HTTP responseまたはEvidenceへ出さない。

## 7. B3-C5 — Invalidation and logout

### 7.1 All-session invalidation

1. 認証成功時のSpring Security principal nameを`FrameworkUserId.toString()`へ固定する。
2. `FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME`でuser ID完全一致検索する。
3. 得られたSession IDを同期削除し、対象外userのSessionを変更しない。
4. repository検索または1件でも削除に失敗した場合は`UserSessionInvalidator`を正常終了させない。

email、external issuer / subject、role codeをindex keyにしない。複数削除途中のDB障害では一部Sessionだけが先に消える可能性があるが、
これは安全側の部分失効である。呼出元Identity mutationはrollbackし、再試行可能なsafe `DEPENDENCY_FAILURE`とalertを返す。
削除済みSessionを補償復元しない。

### 7.2 Mutation and failure mapping

| Operation | Session action | Session store failure |
|---|---|---|
| password変更、user Role変更、external unlink | 対象userの全Session失効 | Identity mutation / Business Auditをrollbackし、safe failure |
| Role Permission変更 | そのRoleを持つ全userの全Session失効 | Identity mutation / Business Auditをrollback。先行失効済みSessionは復元しない |
| account disable | 対象userの全Session失効 | disableを成功扱いしない。Audit failureだけの場合は承認済みどおりdisable / 失効を継続 |
| local / OIDC logout | current Sessionを削除し、SecurityContext、CSRF state、Cookieを消去 | local消去は継続するが、永続削除失敗を成功redirectとして偽装せずsafe failure / alert |

logoutはPOST + CSRFを維持し、Spring Security標準logout handlerを利用する。RP-Initiated Logoutはprovider対応時のopt-inであり、
P2-B3はKOIKI local Session失効だけを必須範囲とする。旧Cookie再提示時は別processでも未認証となることを外部観測する。

## 8. B3-C6 — Cleanup execution

| Candidate | Evaluation |
|---|---|
| Framework独自DELETE SQL | Reject。Spring Sessionの期限判定 / query変更と二重管理になる |
| Web各instanceの内蔵scheduler | Reject。専用maintenance正本と競合し、instance数に応じ実行数が増える |
| **`JdbcIndexedSessionRepository.cleanUpExpiredSessions()`** | **Recommended.** Springが公開する同じrepository / expiry semanticsを再利用できる |

`SessionCleanup`のinternal実装は、single-execution lock取得後にSpring標準`cleanUpExpiredSessions()`を同期実行する。
non-web application contextではServlet filter / serverを起動せず、cleanupに必要なDataSource、transaction operations、
Jdbc repository、KOIKI設定guardだけを成立させる。Web contextでは`SessionCleanup` beanを呼出可能でも自動実行しない。

Spring methodが削除件数を返さないため、Public resultへ推測件数を加えない。T5 / T6では実行前後の期限切れ / 有効rowを
HarnessがDBから数え、期限切れだけが削除されたことを証拠にする。

## 9. B3-C7 — Single execution

| Candidate | Crash recovery | Evaluation |
|---|---|---|
| JVM内lock | process間排他にならないためReject |
| lock table + lease | clock、lease延長、fencing、migrationを増やすため初期候補にしない |
| external schedulerだけ | deployment依存でT6のFramework contractを自己完結して検証できない |
| **PostgreSQL session advisory lock** | connection / process終了で自動解放 | **Recommended.** PostgreSQL baselineで待機なし競合とcrash recoveryを最小構成で実証できる |

- `pg_try_advisory_lock(int, int)`を専用JDBC connectionで取得し、cleanup完了まで同じconnectionを保持する。
- 2整数はKOIKI internal namespaceとexpired-session-cleanup taskを表す固定値とし、Customer入力やhash値から生成しない。
- contenderは待機せず`CONTENDED`を返し、cleanupを呼ばない。
- success / failure時は明示unlock後にconnectionをcloseする。process kill時はPostgreSQL session終了による自動解放を確認する。
- lock取得 / unlock / cleanupのSQL詳細、数値lock key、JDBC URL、credentialをPublic exceptionまたはlogへ出さない。
- transaction-level advisory lockやpooled connectionを一旦返す設計は採用しない。

Phase 1bの外部contractと試験方法は再利用するが、Customer-like `WorkItemExecutionLock`やtask runner codeは参照・copyしない。
この2例だけを根拠に汎用single-execution Public APIへ昇格させない。

## 10. B3-C8 — Configuration

### 10.1 Public configuration surface

| Property | Decision |
|---|---|
| `spring.session.jdbc.initialize-schema` | `never`固定、異なる実効値はstartup failure |
| `spring.session.jdbc.table-name` | `koiki_session`固定、異なる実効値はstartup failure |
| `spring.session.jdbc.cleanup-cron` | `-`固定、異なる実効値はstartup failure |
| `spring.session.jdbc.flush-mode` | `on-save`開始候補、T5実測後に固定 |
| `spring.session.jdbc.save-mode` | `on-set-attribute`開始候補、T5実測後に固定 |
| `spring.session.timeout` / `server.servlet.session.timeout` | Spring / Boot標準を利用。Customerが案件要件で設定 |
| `server.servlet.session.cookie.http-only` | `true`を安全な既定とし、falseをstartupで拒否 |
| `server.servlet.session.cookie.secure` | `true`を安全な既定とし、fixtureのloopback HTTPだけtest scopeで明示override |
| `server.servlet.session.cookie.same-site` | `lax`を既定。`none`はSecure必須かつ別cross-site要件review、`strict`はCustomer選択可 |

Starter導入自体をSession有効化のopt-inとし、`koiki.session.enabled`の二重スイッチは追加しない。Cookie名、domain、path、timeoutは
Spring / Boot標準propertyを使い、同義のKOIKI propertyを作らない。`SameSite=None`やHttpOnly無効化を黙って受け入れない。

maintenance taskの選択、scheduler、retry、process timeoutはApplication / deployment責務とするため、汎用
`koiki.maintenance.*`をStarterへ追加しない。Applicationは公開`SessionCleanup`を自身のnon-web command adapterから呼ぶ。

### 10.2 Startup conditions

- Servlet Web + DataSource + Spring Session JDBCではWeb Session構成、invalidator、cleanup use caseを提供する。
- non-web + DataSourceではfilter / serverなしでcleanup use caseを提供する。
- missing DataSource、repository、Identity contractまたは固定property違反は、必要beanをno-op化せずstartup failureとする。
- Reactive / WebFlux、Redis Session、複数Session storeの自動選択はP2-B3対象外としてfail fastする。

## 11. B3-C9 — Two-process evidence

T6はTooling-owned fixture applicationの同一package済みJARを使い、同じPostgreSQLへ独立processとして接続する。

1. Web process A / Bを異なるrandom portで起動し、readinessをbounded pollingする。
2. Aで認証し、Cookieと`koiki_session*` row / immutable principal indexを確認する。
3. 同じCookieでBへrequestし、同じFramework user / Permissionとして認可されることを確認する。
4. Aを停止し、同じCookieでBへのrequestが継続することを確認する。
5. Bまたは別管理processから対象userを変更し、旧CookieがA由来でもBで拒否されることを確認する。
6. 対象外userのCookieは継続し、全table truncateを全Session失効の代用にしていないことを確認する。
7. Session store障害時のmutation rollbackとlogout local消去 / 永続失敗通知を外部状態で確認する。
8. Web processにcleanup schedulerがないことを、期限切れrowの残存とthread / logの双方で確認する。
9. cleanup process 2本を競合させ、winner `0`、contender `10`、副作用1回を確認する。
10. winnerをOS killし、lock消失後のretry `0`と期限切れrow削除を確認する。

production sourceへtest endpoint、sleep、failure switch、固定user、固定portまたはcredentialを追加しない。Fixture routeとidentityは
非配布Tooling内だけに置く。logだけを証拠にせず、HTTP、Cookie、DB row、process exit、PostgreSQL lock状態を組み合わせる。

## 12. B3-C10 — CI and distribution

| Stage | Content | Decision |
|---|---|---|
| B3 local development | T0〜T5 aggregateを反復し、T6はprocess / container cleanup込みで独立実行 | 各実装sliceのcommit前に必須 |
| B3 closeout | T0〜T6、root、Null Safety、inventory、sensitive scanを同一HEADで実行 | Owner review用Evidence |
| Gate B candidate | Milestone BのPostgreSQL integration入口へ同じaggregateを接続 | B3単独でworkflowを変更しない |
| required check | remote 3回連続成功、時間、flakiness、container / child process cleanupをOwner review | Gate Bで判断 |

正式JARへfixture class / route / identity / secret / failure switchを含めず、fixture artifactをBOMまたは隔離Maven repositoryへinstallしない。
成功時・失敗時ともJAR、Maven / process log、Surefire reportをsecret、Cookie value、Authorization header、email形式PII、
serialized attribute bytesについてscanする。PID、port、Session IDはEvidence内で必要最小限に扱い、永続的な識別子にしない。

## 13. Planned change inventory

| Target | Planned change after approval |
|---|---|
| Framework artifact | `koiki-starter-session-jdbc`を1件追加。release unit 14 |
| Existing Public API | `UserSessionInvalidator` signature変更なし |
| New Public Java API | `SessionCleanup`、`SessionCleanupResult`、`SessionCleanupException`の3型 |
| Runtime dependency | `spring-boot-starter-session-jdbc`、既存`koiki-starter-identity`。Redis / cloud / Oracleなし |
| Framework migration | `koiki_session`、`koiki_session_attributes`の2 table |
| Public KOIKI properties | 追加なし。Spring / Boot標準propertyを利用 |
| Fixed Spring properties | initializer `never`、table `koiki_session`、cleanup cron `-`。flush / saveは実測後確定 |
| Internal adapters | JDBC invalidator、Spring cleanup、PostgreSQL advisory lock、Web / non-web auto-configuration |
| Tooling | 既存Security verificationへT5 / T6、package済み2 process fixtureを追加 |
| Reference / Customer | production変更なし。T6 fixtureだけが非配布Application assemblyを持つ |
| CI workflow | B3では変更なし。Gate Bで候補追加 / required化をreview |

## 14. Architecture Owner review points

1. Session JDBCを新規optional Starterに置き、Sessionを使わない構成へ強制しない境界が妥当か。
2. `UserSessionInvalidator`を維持し、cleanupだけをSession固有3型で公開して汎用single-execution APIを作らない判断が妥当か。
3. Framework Flyway 2 tableを正本とし、initializer / table名 / Web cleanupの実効値をstartup guardで固定してよいか。
4. `ON_SAVE` / `ON_SET_ATTRIBUTE`と標準serializationを実測候補とし、Framework Session属性を最小化する境界が妥当か。
5. immutable user IDのprincipal index、部分失効は安全側、Identity mutation rollback、logout local消去継続という失敗時挙動が承認済みsemanticsと一致するか。
6. cleanup SQLを複製せずSpring標準methodをnon-web processから呼び、件数はHarnessがDB観測する判断が妥当か。
7. PostgreSQL advisory lockをSession cleanupのinternal実装に限定し、固定namespace、専用connection、非待機競合、crash解放を採用してよいか。
8. KOIKI独自Session設定を増やさず、Spring / Boot標準propertyと安全なstartup guardをPublic設定面にしてよいか。
9. package済み同一JAR 2 processと実PostgreSQLで、継続、対象失効、障害、cleanup競合 / crashを実証する範囲が十分か。
10. B3ではlocal Evidenceまでとし、Milestone B CI接続とrequired化をGate Bの3回連続成功reviewへ残す判断が妥当か。

推奨判断はB3-C1〜C10を上記のとおり承認し、B3-2 Session core / migrationへ進むことである。
Public API追加なしでApplication lifecycleを安全に成立させる別案、Spring標準cleanupでnon-web構成が成立しないEvidence、
または標準serializationでFramework principalを2 process共有できないEvidenceが得られた場合は実装を止め、本reviewへ戻る。

2026年9月7日、Architecture Ownerは上記10項目をreviewし、B3-C1〜C10を推奨案どおり承認した。
B3-1を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はB3-2 Session core / migrationへ進む。
実測で上記stop conditionに該当した場合は、承認済み契約を暗黙変更せず本reviewへ戻る。

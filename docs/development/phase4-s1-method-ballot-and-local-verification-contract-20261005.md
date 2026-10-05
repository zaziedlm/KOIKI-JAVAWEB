# S1方式採用票・初回局所検証契約案（2026-10-05）

**状態:** OWNER APPROVED（2026-10-05）/ 局所検証候補・初回作成／実行契約。preflight・初回L1〜4はLOCAL PASS（11 / 9 / 8 / 12 tests、各failures / errors / skipped 0）。正式方式・Tier・Reference接続は後続review。
**今回の範囲:** 承認済みpreflight・初回L1〜4の作成・実行結果を反映。[Evidence](../architecture/validation/phase4-s1-minimum-local-verification-20261005.md)を参照。数値は開始条件・上限であり、未実測部分を含む。
**入力:** [K票・CH変更一覧・ST開始範囲](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)、[Tier・保存 / 権限・V0〜7](phase4-s1-minimum-tier-storage-verification-review-20261005.md)、[成立性確認](phase4-s1-minimum-boundary-feasibility-review-20261005.md)。

## 1. 方式採用票

各票は「局所検証候補として了解」と「正式採用」を区別する。局所検証後の結果が採用判断を変えることを許容し、未成立条件を条件付き採用のまま実行へ転用しない。下表は現在のOwner判定を示す。承認根拠・条件の詳細は§1.1を正本とする。

| 票 / K | 提案する判定対象 | 条件・戻る場合 | Owner判定 |
|---|---|---|---|
| B1 / K1 | 許可＋append-only消費の2種類を初回局所検証候補とする | 許可ID一意INSERT、対象不変・未解決対象の次許可禁止、row lock用version列権限、同transaction。実DB不成立なら保護方式へ戻る | 了解：局所検証候補（2026-10-05） |
| B2 / K2 | 狭いRICH / JPA共有モデルを正式Reference Tierの優先review候補とする | 最小不変条件だけをModel化。ST-BはTier採用を実証しない。SIMPLE代案は配置・単純性の説明を添える | 了解：Tier候補として継続、正式確定は後続（2026-10-05） |
| B3 / K3 | 認証済み限定Webで許可発行、non-Webで真正性・現在権限を再確認、既存Auditへ接続 | permission / scope・失効・分類 / actorと正当なBean接続を正式review。初回fixtureの模擬主体を本人認証PASSとしない | 了解：方向性、残条件の具体化を継続（2026-10-05） |
| B4 / K4 | 通常 / 限定Web / 単発non-Webと、不変operationの実executor伝播を候補にする | ST-Bはmode組立・相関の局所契約まで。正式Reference Web認証 / scan回帰・実traceは後続。欠落時送信拒否 | 了解：局所検証候補（2026-10-05） |
| B5 / K5 | JDBC / UPDATE、kkref維持、有限collectionのpredicate版による厳密対象選別 | 正式migration・依存・Rule / Level reviewは別。options版のfilter前batch制限は局所負例で把握。適用状態・取得上限を検証 | 了解：局所検証候補（2026-10-05） |
| B6 / K6 | 下記ST-B初回範囲だけのfixture作成・検証を個別開始対象にする | path・資源・上限・command・権限 / Evidenceを確認。Reference / Framework production変更、正式採用 / Gate通過を含めない | 了解：初回fixture作成・実行の両方（2026-10-05） |

判定記録は票ID、了解 / 条件付き / REWORK / 保留、対象、開始前必須条件、理由、日付を残す。B1 / B4 / B5を検証候補とする判断はB2 / B3の正式採用と独立する。B6は作成と実行の両方を対象に含めるか明記する。

### 1.1 Owner判定記録（2026-10-05）

Ownerはレビュー支援の見解を了承し、「方式採用票・初回局所検証契約案の判断を承認とします」と指示した。

| 票 | 判定・対象 | 条件・理由 |
|---|---|---|
| B1 | 了解：2種類記録の局所検証候補 | 最小構成として検証する。DB保護・一意消費・競合の成立を確認して確定する |
| B2 | 了解：狭いRICH / JPA共有モデルをTier候補として継続 | 正式Tierは局所結果と不変条件の配置を確認して後続判断する |
| B3 | 了解：許可引渡し・現在権限再確認・既存Audit接続の方向性 | permission / scope・失効・Audit分類 / actor・正当なBean接続の具体化を継続する |
| B4 | 了解：目的別起動・実executor相関の局所検証候補 | 欠落時拒否を検証する。既存Reference Web認証 / scan回帰・実traceは後続 |
| B5 | 了解：JDBC / UPDATE・厳密対象選別の局所検証候補 | 選定と結果を区別する。正式migration・依存・Rule / Level判断は後続 |
| B6 | 了解：§2〜5の初回fixture作成・実行の両方 | 票全体の承認として記載された作成・検証を対象にする。開始前preflight・実効設定確認・必須test枝一覧化、記載上限を適用する |

人の総合判断を維持し、仕組みは指定対象への一意な許可消費を守る。局所PASSは正式採用・DoD / Gateの完了ではない。作業量16〜32時間は完了保証ではなく、8時間時点で見直し、依存追加・範囲拡大・上限超過が必要なら再判断する。実行環境の権限承認手順は引き続き適用する。

## 2. 初回ST-Bの変更対象と除外

既存[非配布Tooling POM](../../build-support/phase4-level2-verification/pom.xml)の`jdbc` profileを利用する候補。Root Reactor外、非配布を維持する。既存Source / V1 / V2 / POMを変更しない初回案とし、追加が必要なら変更票を更新する。

| 予定変更 | path案・条件 |
|---|---|
| 新しい4 test class | `build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/`直下の下記class |
| test専用support / Configuration | 同test root配下の`s1fixture/`。明示import、TestConfiguration等で既存ProbeApplicationのroot scanへ混入させない |
| test専用DDL / role設定 | `src/test/resources/s1-minimum/`。通常Flyway自動探索location外でtestが明示適用。正式Reference migrationへ自動転用しない |
| 実行結果 | `target/surefire-reports/`と`target/s1-minimum-evidence/<run-id>/`。必要証拠を整理して下記validation文書へ |
| Evidence文書 | `docs/architecture/validation/phase4-s1-minimum-local-verification-20261005.md`にpreflight / L1〜4を記録済み。後続の実行日が変わればその日付のEvidenceへ追跡する |

初回では新tracer / exporter、collector、独立HTTP provider、別JVM harness、Web / Security依存、CI / remote、Reference code / POM / migration、正式Rule APIを追加しない。これらを初回で確認できたとはしない。test専用送信probeは副作用件数と操作相関を観測し、実provider冪等性・実Web認証の保証へ広げない。

## 3. 具体testと判定契約

L1 classと専用DDLは作成・実行済み（11必須method、全件成功）、L2は9 invocation、L3は8 invocation全件成功。L4は12 invocation全件成功。L1はSpring contextを起動せず独立した実DB接続で検証した。後続も既存通常probeの自動起動・migration・schedulerを意図せず取り込まない。

| 順 / 予定class | 必須test枝 | 採取・合格条件 |
|---|---|---|
| L1 `S1PermitStorageTest` | 用途別roleの発行 / row lock / 消費、対象・actor改変、消費UPDATE / DELETE / TRUNCATE、同許可再INSERT、rollback / Audit相当記録の同transaction | SQLSTATE / transaction結果・role権限 / owner継承・前後snapshot。消費1件一意、不可操作拒否、commit前送信0。記録probeは正式AuditRecorderの保証を代替しない |
| L2 `S1PermitConcurrencyTest` | barrierで2connectionの同許可消費、別許可同対象、確認終了 / 取消と消費競合、commit後runner終了模擬、commit結果不明として再開拒否 | 消費と確認の順序が矛盾しない、負けた操作送信0、消費不明を取消さない。終了模擬は実OS crash / 通信応答喪失と別証拠 |
| L3 `S1ResubmissionContextTest` | 実Modulith＋JDBC＋標準async executorで、対象外FAILED・同event別publication、predicate厳密選別、options batch負例、context欠落 / 別対象・thread再利用 / 例外 | task投入 / 実行thread・operation / publication / event / listener、対象行前後、送信probe。対象だけ処理・未選定保留・非漏えい。実traceは未検証 |
| L4 `S1ModeAssemblyTest` | test用通常 / 確認 / 復旧構成の明示Bean集合、誤mode / ID欠落拒否、確認時のlistener / sender / runner不在、起動前後副作用不変 | context / Bean一覧とprobe件数。限定Webの設計契約までで、実Web server / SecurityFilterChain / CSRF / SessionのPASSではない |

全枝をID付きでtest methodへ対応付け、開始前に期待件数を一覧化する。必須枝がskip / disabled / 未作成なら未達。L1 / L2が失敗したらL3で送信を進めず原因を直す。L3の選定・相関が成立しなければ方式をREWORKし、markerだけでPASSにしない。

既存`CorrelationObservationTest`はsynthetic trace marker、`ProcessCrashRecoveryIT`は別JVMの既存characterizationであり、今回の4 testでその保証を増やしたとは説明しない。既存testの意図・PASSを維持し、新fixture構成の影響があれば必要な既存回帰を対象票へ追加する。

## 4. 作成後の実行手順案

**以下は各classが作成されてから一つずつ使うcommand。初回L1〜4は実効設定を確認しoffline指定を加えて順次実行済み。** working directoryはRepository root。実効POMでSurefire 3.5.6とargLine / javaagent指定なしを確認した。単一test指定・fork / argLine / no-test失敗の根拠は[Surefire公式parameter](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html)。後続も実効設定を照合する。

1. branch / HEAD / dirty差分、票B6の対象、Java 21・Wrapper / Docker接続、端末資源を確認。新sourceはcommit SHAまたは差分checksumで固定する。preflightの診断・build / Docker実行も個別範囲に従う。
2. test source / DDL / role設定の存在と独立context、既存artifact / snapshotの解決を確認。必要artifact欠落なら停止し、正式artifactのinstall / download等を追加対象へ明記する。既存snapshotを勝手に再publishしない。
3. `-Pjdbc`のeffective POM / dependency treeを出力し、Boot / Modulith / Surefire / Testcontainersと引数の実効値を保存する。これはMaven cache / target書込を伴う。既存argLineのjavaagent等が必要なら上書きせず合成してcommandを更新する。
4. 下記を**一つずつ**実行。各commandの終了コードとXML / console・環境台帳を確認して次へ進む。

```powershell
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1PermitStorageTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1PermitConcurrencyTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1ResubmissionContextTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
.\mvnw.cmd -f build-support/phase4-level2-verification/pom.xml -Pjdbc "-Dtest=S1ModeAssemblyTest" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

5. XMLのclass / method・tests数・failures / errors / skippedを期待一覧と突合する。BUILD SUCCESSでも未作成・必須枝skip・Evidence欠落はPASSにしない。`test`だけなので既存Failsafe IT・別JVM package実演は走らず、その結果を主張しない。
6. 各runの証拠を上書き前に保存し、secret / PIIを除いてchecksum・適用条件・FAIL / BLOCKED / 未検証範囲をvalidation文書へ記録する。cleanupは台帳上の当該container / connection / 出力だけとし、既存DB・他container・未確認証拠を一括削除しない。

権限エラーはAGENTS.mdの手順と実行環境の承認に従い、同じ最小操作で再確認する。既存PL2許可と同pathでも、新しいtest・資源・実行内容をB6に含めて確認する。Maven指定でmodule全体をcompileするため、無関係なcompile失敗があればその影響を報告し、対象外修正へ自動拡張しない。

## 5. 初回局所の資源・上限案

全数値は**実測前の開始条件・上限としてOwner了解（2026-10-05）**。前段の4Reference JVM構成より小さく、局所検証専用であり本番容量 / SLA・全S1工数ではない。

| 対象 | 初回上限 / 確認点候補 | 超過時 |
|---|---|---|
| 同時実行 | Maven 1、test fork 1、PostgreSQL 1。競合はfork内2connection / worker。別Application JVMなし | 次class / 別作業を同時開始しない。Testcontainers管理用Ryuk等も資源台帳に記録 |
| memory / CPU | fork heap 768 MiB、DB memory 1 GiB・CPU 1を候補。端末available memory 4 GiB以上で開始 | Maven・native / Docker VM・管理containerを別採取。DB limitはfixture実装で指定し実効確認。不足なら停止して再設定 |
| DB接続 | test pool最大4、独立競合2、観測 / 管理2、合計8まで。DB max_connections候補16 | role / pool・cleanupを確認。接続枯渇で失敗した枝を安全性PASSへ転用しない |
| data / 保存 | publicationは1実演20件まで、実演data / log合計1 GiB、開始前空き10 GiB以上 | 超過前に停止。image / Maven cacheは別枠採取。無制限collection / cleanupなし再試行をしない |
| 待機 | barrier / lock 10秒、listener終端30秒、1 class 10分、全4 classの初回実行確認枠40分 | 初回image取得 / build待ちは別計測。timeoutはworker停止・未送信証明ではない。残task・DB状態を確認 |
| 再試行 | 同じ失敗枝の原因未変更rerunは1回まで。次は原因・差分をreview | test反復でflakeを隠さない。修正後は必要枝を再検証。標準APIの無制限再送をしない |
| fixture作成作業 | 初回技術作業16〜32標準時間を低確度の再判断枠として提案。8時間時点で成立性 / 不足を確認 | 32時間以内の完了保証ではない。新library・独立process / Web / tracerが必要なら次範囲へ分離し再見積 |
| 保持 / 費用 | 結果確認まで削除しない。証拠7日を確認窓候補、有料service追加0円 | 保留証拠は期限で削除しない。電力・既存設備費の総額0を意味しない |

端末診断 / 1試行の実測後に修正する。16〜32時間は既存W03 / W06 / W09に対応する局所枠で、全額をS1総量へ純追加しない。Ownerのreview稼働・環境待ちは別記録。DB imageは既存`postgres:17-alpine`を起点に実version / digestを固定し、floating tagだけで再現性確定としない。

## 6. 局所結果の出口と後続

初回の結果はB1・B4・B5の局所成立性へ戻し、B2 / B3の正式判断材料にする。権限・一意消費・原子的記録・標準executor相関・mode組立がPASSでも、Framework Public API、Reference Web認証、実tracer / exporter、OS crash、独立provider、DoD / Gateは未検証として残す。

それらは後続ST-Bの追加個別範囲またはST-C〜Eで、必要dependency・資源・test / commandと前置reviewを示して判断する。B票は§1.1の範囲でOwner了解、初回fixture作成・実行は承認済み。初回L1〜4は作成・LOCAL PASS。次はB1 / B4 / B5への結果反映と、B2 / B3・正式Reference接続／追加範囲のreview。正式code・POM・migration・runtime権限・remoteは変更していない。

## 7. 局所結果の方式票への反映（2026-10-05）

**状態:** review材料更新。§1.1のOwner承認を維持し、以下の提案を正式採用・追加実装開始の承認として扱わない。根拠は[局所Evidence](../architecture/validation/phase4-s1-minimum-local-verification-20261005.md)と現行source。L1〜4は別々の順次実行で計40 invocation成功し、全test回帰・統合実演の結果ではない。

| 票 | 局所結果から確認できたこと | 提案と残条件 |
|---|---|---|
| B1：2種類記録 | L1 / L2で列権限・一意消費・改変拒否、同対象許可の競合、消費と確認／取消のロック順序が成立 | 最小候補を継続。正式JPAの発行SQL・更新SQL・lock・transactionと実行roleが同じ制約で成立するか確認する |
| B2：Tier | JDBC fixtureが安全条件を確認した。正式JPA Domain Model・Tierの配置は検証していない | 狭いRICH / JPA共有モデルを優先候補として維持。§8の不変条件配置を確認して正式判断する |
| B3：認可／Audit | L1のAudit相当probeで同transaction rollbackを確認。主体・現在権限は本人認証の検証ではない | 方向性を維持。実Web認証、許可真正性・対象scope・失効、既存Recorderの分類／actor・transactionを確認する |
| B4：起動／相関 | L3で実async executorのoperation伝播、欠落／不一致拒否、例外後の非残留。L4で明示Bean構成・誤起動拒否 | 局所候補を継続。L3の専用decoratorを正式Framework変更へ転用しない。L4のrunnerはID準備までで、正式復旧実行・実DB起動副作用は未検証 |
| B5：対象選別 | L3で有限候補からpublication／状態／attempt／eventを絞り、対象外を保持。options版batch制限の未選定を実確認 | 有限predicate方式を優先候補として維持。正式のlistenerとDB行の対応、取得上限、処理後確認を接続する。呼出し復帰だけを完了判定に使わない |
| B6：初回範囲 | 承認された4 classを作成・実行し、証拠を記録した | 初回局所検証は完了。追加検証は対象・test・実行手順・資源上限を別途具体化する |

特に次の2点は、局所PASSで解決済みとはしない。

- **結果不明の引継ぎ:** L2はcommit／rollback両方で「結果不明」をtest入力として与え、再開を拒否した。別processの再起動後にも不明を保全する経路は未実証。消費行が見えないことだけで再送可へ戻さず、許可を未解決として保ち、DB・process・provider・log／Auditと人の判断を突合する。2種類記録と運用証拠での引継ぎを先に具体化し、管理台帳一式を必須化しない。
- **相関と対象識別:** L3ではoperationにpublication／event／listenerを保持し、listener内でDB行と突合した。公開publication APIやlistener引数だけから全識別情報を得られる保証ではない。正式接続でも取り違えを拒否する対応付けが必要で、operation ID入力だけを真正な許可として扱わない。

## 8. Tier・認可／Audit・正式Reference接続の残条件

以下の4項目は**方針候補の継続・残条件の具体化についてOWNER APPROVED（2026-10-05）**。具体化／検証が必要な条件は維持し、正式Tier採用・追加検証実行・正式Reference実装開始は後続の証拠と個別判断に委ねる。承認記録は§8.3を参照。ST-C〜Eの開始承認ではない。

| 論点 | 推奨する方針候補 | 正式接続前に具体化する条件・証拠 |
|---|---|---|
| Tier・保存 | notificationは狭いTier 2 RICH / JPA共有モデルを優先。許可と消費の2種類を維持 | 対象／actor不変、未解決対象の次許可禁止、消費一意、確認終了の条件をDomain／Application／DBへ割り当てる。Domainは可否の規則、Applicationは現在権限・transaction・外部呼出し順、DBは一意制約・排他・不正更新拒否を担う候補。実JPAのflush／version更新と列限定権限の両立を確認し、不成立ならAdapter／SQL境界を再reviewする |
| 認可・許可引渡し | 既存Web認証で許可発行者を確定し、non-Webは保存済み許可と現在権限を確認する | permissionコード・発行／確認主体・対象scope・環境拘束・期限／失効・権限再確認後の失効窓を定義。無効／権限喪失／主体不明は拒否。IdentityQueryによる現在情報の照会は本人認証の代替ではない。入力user IDや模擬principalから許可を発行しない |
| Audit | 許可発行・消費・確認終了の業務記録と、認可拒否等のSecurity記録を分け、既存Public契約を使う | 分類・action／resource／reason・actor・operation／publicationとの対応を確定。Business Auditと同一transactionで消費を記録し、実Recorder失敗時に消費rollback・送信0を確認する。Security Auditの別transactionは同一原子性の証拠に数えない |
| 正式Reference接続 | 既存Webを維持し、確認／復旧は目的別に必要Beanだけを組み立てる。migration管理主体とruntime主体を分離 | 実entry point・component／Entity／Repository scan・Servlet Security・Identity／Audit Bean・DataSource／transaction manager・Modulith初期化／scheduler／Flywayをmode別に一覧化。確認modeに送信可能Beanがなく、実DBの起動／確認／終了前後にpublication・attempt・送信副作用が増えないことを確認。通常Webの認証／CSRF／Session／method securityの回帰も必要 |

Tier 2の理由は、少数の可否規則と不変条件をModelに置くためである。復旧要求の一覧管理、汎用workflow、長期lease、自動再判定、専用運用UIまで広げる理由にはしない。JPA共有モデルの「共有」は同module内のモデル方針であり、他moduleのEntity／Repositoryを直接利用する許可ではない。

### 8.1 現行Public契約・sourceへの接続で注意する点

- [Security AutoConfiguration](../../koiki-starters/koiki-starter-security/src/main/java/org/koikifw/starter/security/internal/KoikiSecurityAutoConfiguration.java)の`@EnableMethodSecurity`はServlet条件付き。non-Webで同じBean認可が自然に有効になると仮定しない。既存[Reference Security構成](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java)はHttpSecurityを要求するため、non-Webへroot scanをそのまま持ち込まない。認可の配置と拒否testを先に決める。
- [BusinessAuditTransaction](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/BusinessAuditTransaction.java)は`MANDATORY`、[SecurityAuditTransaction](../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/SecurityAuditTransaction.java)は`REQUIRES_NEW`。内部classを直接利用せず既存Public Recorderへ接続し、実際のDataSource／transaction managerで原子性を確認する。L1の`audit_probe`はその証拠を代替しない。
- Audit actorは許可発行者の不変user IDと実行主体の役割を区別して設計する。Framework背景処理用のSYSTEMをReference workerへ無条件に流用しない。既存AuditResultのSUCCESS／FAILUREへ結果不明を勝手に追加せず、不明・保留は対象記録と運用証拠で説明する。

### 8.2 後続の具体化順

1. 上表の方針候補継続・残条件具体化のOwner承認を§8.3へ記録済み。既存の人による総合判断・最小構成の方針と開始前必須条件を維持する。
2. [責務・認可／失効・Audit・mode／DB権限の具体化案](phase4-s1-responsibility-authorization-audit-mode-draft-20261005.md)を作成済み。具体案をreviewし、Reference code／POM／migrationの必要差分と追加検証契約へ渡す。正式採用・物理名固定・開始判断は残る。
3. [追加局所検証契約・再見積案](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md)を作成済み。A：JPA保存、B：Identity／Audit、C：Web／modeの順とtest・command・資源／上限・戻り先を提示。追加作成／実行は未承認で、まずAを個別開始判断へ戻す。初回16〜32時間枠は流用しない。
4. 個別開始と既存Gate経路の判断に従って実施する。実provider／trace／OS crash・E01〜21／DoDの不足は、その後の統合実演へ追跡する。

今回の更新は方式票への結果反映と残条件の整理まで。Tier正式採用・正式Reference変更・追加fixture作成／実行・Gate判定は行っていない。

### 8.3 Owner方針レビュー承認記録（2026-10-05）

Ownerは「4項目の方針候補を継続し、記載された残条件の具体化へ進める。正式Tier採用・追加検証実行・正式Reference実装開始は、後続の証拠と個別判断に委ねる」の方針を承認した。

| 対象 | Owner判定 | 維持する残条件 |
|---|---|---|
| Tier・保存 | 狭いRICH／JPA共有モデルの優先候補継続、残条件具体化を承認 | 最小2種類記録と不変条件配置、実JPAと制限roleの適合。正式Tierは未確定 |
| 認可・許可引渡し | Web本人認証による許可発行・non-Webの真正性／現在権限確認の方針継続、具体化を承認 | permission／scope・環境・期限／失効・拒否条件と実認証の証拠 |
| Audit | 業務／Security分類と既存Recorder接続の方針継続、具体化を承認 | actor・イベント対応、実transactionでの原子性・記録失敗時送信拒否 |
| 正式Reference接続 | 目的別Bean構成と通常Web維持の方針継続、具体化を承認 | scan／Security／DB権限・migration・起動副作用、通常Web回帰 |

承認対象は文書・source照合による残条件の具体化。追加fixture作成／実行、正式Reference code／POM／migration、Framework Public API／Rule・依存の採用、DoD／Gate・remote操作はこの承認に含めない。結果不明の再起動引継ぎ、人による総合判断、将来管理機能との分離を維持する。次は§8.2の2へ進む。

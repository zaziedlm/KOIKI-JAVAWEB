# S1方式採用票・初回局所検証契約案（2026-10-05）

**状態:** OWNER APPROVED（2026-10-05）/ 局所検証候補・初回作成／実行契約。局所fixtureは未作成、全command / testは未実行。
**今回の範囲:** Ownerレビュー結果の記録。以下のtest名・pathは承認された初回作成／実行範囲で、作成済み・実証済みとは扱わない。数値は実測前の開始条件・上限である。
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
| Evidence文書 | `docs/architecture/validation/phase4-s1-minimum-local-verification-20261005.md`を予定。実行日が変われば日付も変更。今回未作成 |

初回では新tracer / exporter、collector、独立HTTP provider、別JVM harness、Web / Security依存、CI / remote、Reference code / POM / migration、正式Rule APIを追加しない。これらを初回で確認できたとはしない。test専用送信probeは副作用件数と操作相関を観測し、実provider冪等性・実Web認証の保証へ広げない。

## 3. 具体testと判定契約

下記classは**作成予定・現在は存在しない**。独立したtest Application / contextを明示構成し、既存通常probeの自動起動・migration・schedulerを意図せず取り込まない。

| 順 / 予定class | 必須test枝 | 採取・合格条件 |
|---|---|---|
| L1 `S1PermitStorageTest` | 用途別roleの発行 / row lock / 消費、対象・actor改変、消費UPDATE / DELETE / TRUNCATE、同許可再INSERT、rollback / Audit相当記録の同transaction | SQLSTATE / transaction結果・role権限 / owner継承・前後snapshot。消費1件一意、不可操作拒否、commit前送信0。記録probeは正式AuditRecorderの保証を代替しない |
| L2 `S1PermitConcurrencyTest` | barrierで2connectionの同許可消費、別許可同対象、確認終了 / 取消と消費競合、commit後runner終了模擬、commit結果不明として再開拒否 | 消費と確認の順序が矛盾しない、負けた操作送信0、消費不明を取消さない。終了模擬は実OS crash / 通信応答喪失と別証拠 |
| L3 `S1ResubmissionContextTest` | 実Modulith＋JDBC＋標準async executorで、対象外FAILED・同event別publication、predicate厳密選別、options batch負例、context欠落 / 別対象・thread再利用 / 例外 | task投入 / 実行thread・operation / publication / event / listener、対象行前後、送信probe。対象だけ処理・未選定保留・非漏えい。実traceは未検証 |
| L4 `S1ModeAssemblyTest` | test用通常 / 確認 / 復旧構成の明示Bean集合、誤mode / ID欠落拒否、確認時のlistener / sender / runner不在、起動前後副作用不変 | context / Bean一覧とprobe件数。限定Webの設計契約までで、実Web server / SecurityFilterChain / CSRF / SessionのPASSではない |

全枝をID付きでtest methodへ対応付け、開始前に期待件数を一覧化する。必須枝がskip / disabled / 未作成なら未達。L1 / L2が失敗したらL3で送信を進めず原因を直す。L3の選定・相関が成立しなければ方式をREWORKし、markerだけでPASSにしない。

既存`CorrelationObservationTest`はsynthetic trace marker、`ProcessCrashRecoveryIT`は別JVMの既存characterizationであり、今回の4 testでその保証を増やしたとは説明しない。既存testの意図・PASSを維持し、新fixture構成の影響があれば必要な既存回帰を対象票へ追加する。

## 4. 作成後の実行手順案

**以下は開始判断後、4 classが作成されてから使うcommand案。今回実行しない。** working directoryはRepository root。現行ParentはSurefire 3.5.6を管理する。単一test指定・fork / argLine / no-test失敗の根拠は[Surefire公式parameter](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html)で確認し、実効POMで対象versionの設定を照合する。

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

それらは後続ST-Bの追加個別範囲またはST-C〜Eで、必要dependency・資源・test / commandと前置reviewを示して判断する。B票は§1.1の範囲でOwner了解、初回fixture作成・実行は承認済み。4 testは未作成、全command未実行。今回の承認記録ではcode・POM・migration・資源 / 権限設定・remoteは変更していない。

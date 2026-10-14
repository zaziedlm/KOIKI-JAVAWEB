# S1正式開始票：Reference保存・認可・Audit基盤（2026-10-06）

**状態:** 初回条件・実行資料・限定正本改訂採用、Gate P4-F初回限定APPROVE LIMITED START、文書commit・clean source固定／preflight成立を条件とする正式作成検証開始OWNER APPROVED（2026-10-06）。source固定／preflightは未実施。実装未開始。
**baseline:** branch `feature/phase4-s1-reference-foundation`／HEAD `f5e2672`。A／B／C受入を含むcommitから分岐し、Reference保護の検討文書と関連導線4ファイルを未commitで引継ぎ。本票を含む文書差分を固定するcommitを、実装開始前のsource baselineとして別途記録する。
**正本入力:** [安全側の接続検討案](phase4-s1-reference-safe-integration-design-draft-20261006.md)、[CH-01〜08／ST-C](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)、[J1〜8・開始経路](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)、[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、[C受入§6](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#6-owner受入承認2026-10-06)。

**具体採用の判断先:** [初回採用判断表§8](phase4-s1-reference-foundation-adoption-decisions-20261006.md#8-owner採用判断2026-10-06)。D1〜D6の推奨案と初回／後段条件区分をOwner採用済み。scope／TTLは初回契約と運用供給元／値の採用時点を分ける。Gate／正本整合と実行開始は別判断。

**文書baseline固定・後続審査（2026-10-06）:** Ownerのcommit `f9ea06a`で本票・採用判断表・安全接続案を固定済み。上記`f5e2672`＋未commitの記述は作成時の履歴。[正本改訂差分・残条件の審査案](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md)にJ1／J8の具体文案、前置／後続review区分、Tier／登録／grant、J7の既存harness資源不整合を整理した。新文案の採用・正本反映・Gate／実行開始は未成立。

## 1. 判断対象・初回の出口

Ownerの「既存Referenceの機能・品質を阻害せず、安全側へ倒した拡張」という指示を受入前提とする。初回はReference-owned `notification`の**許可・append-only消費の保存／認可／Audit基盤**を、通常起動から登録を外した構成で作成する案。ST-Cの部分範囲であり、新しいGateやLevel 2開始例外を設けない。

出口は、正式配置のJPA共有モデルと実Spring transaction／Public IdentityQuery・Recorderを使った保存・拒否・競合のEvidence、追加無効時の既存Reference回帰と新旧DB起動のEvidence。配信完遂／復旧完遂・DoD 4-2／4-3は初回の出口に含めない。

初回はWeb／CLI受付・復旧runner・sender・通常通知listener・Modulith runtime追加・expense承認event接続を作成しない。更新操作は明示構成のintegration testから確認する。対象現在値・停止／突合証拠の正式Adapterが未接続である条件を隠さず、その確認が必要な操作は実運用経路で拒否する。testの有限証拠を正式運用入力へ昇格しない。

## 2. 作成対象と責務・契約の採用案

以下の名称は本票で採用判断に提示する案。採用後に作成し、型数・field・signatureの範囲拡大は差分を戻す。

| path／型候補 | 初回の責務・契約 | 受入で照合すること |
|---|---|---|
| `koiki-reference-app/src/main/java/org/koikifw/reference/notification/package-info.java` | `notification`、RICH／JPA／SHARED。許可と消費にまたがる不変条件・共通可否規則をTier理由とする | 現行Architecture Rulesの適合。他module内部参照なし |
| 同配下`domain/model/RecoveryPermit` | 許可ID、environment／publication／event／listener／expected attempt、発行者不変ID、reason、issued／expires、閉鎖時刻／確認主体／証拠参照、version。生成・期限・対象一致・確認終了の規則 | 不変値の非更新、`now < expiresAt`、閉鎖一式整合、setterなし。EntityをWebへ露出しない |
| `domain/model/RecoveryConsumption` | permit IDを一意キー、operation ID一意、worker世代、消費時刻。生成後の更新・削除操作なし | append-only、必須相関値、permit FK、commit／rollback |
| `domain/repository/` | 必要な取得・lock付き取得・保存／flushのみ。Spring Data Commons Repositoryを継承し、`@NoRepositoryBean`でroot自動登録を抑える案 | Rule 16適合、通常起動でRepository Bean不在。JpaRepository／EntityManagerをDomainへ公開しない |
| `application/RecoveryPermitService`・`application/query/` | 発行／消費記録／確認終了の調整、現在能力・scope・証拠照合、transaction、Business／Security Audit。観察は最終record | DomainへQuery／Recorderを持たせない。消費前確認とAudit失敗時rollback。送信を呼ぶmethodなし |
| `application/port/outbound/` | scope、現在対象snapshot、停止／突合証拠の必要契約。初回で使うものだけ作成 | 未接続・欠落・不一致を拒否／HOLD。任意booleanや入力actor IDで確認済み扱いしない |
| `adapter/outbound/persistence/` | EntityManagerを使う明示Adapter、lock／version、一意競合の変換、scope先行Query | 更新はJPA、JDBC／MyBatis更新を追加しない。同じDB／JpaTransactionManagerとRecorderが接続 |
| `adapter/outbound/identity/`・`configuration/` | 実Public IdentityQuery接続、現在能力・status。条件付きEntity／Adapter／Use Case登録 | Framework内部importなし。通常scanで不要Beanが生成されない |

Permitを可否規則の中心とし、Consumptionは一度性の証拠記録とする。両方を一つの巨大Entityへまとめず、同じApplication transactionとDB制約で整合させる。`@DynamicUpdate`／column mapping／lockはAの知見を踏まえて新しい正式mappingで検証する。fixture classや専用ORM XMLをproductionへコピーしない。

**対話採用済みの期限精度:** 発行時刻・期限をDBのmicrosecond精度へ切り捨て、調整後の期限が発行時刻以下なら発行を拒否する。保存・再読取後も期限ちょうどで消費を拒否し、精度調整による期限延長を許容しない。mapping・実DBの境界検証で確認する。正式TTL値・最大TTLの後段判断は維持する。

**認可・Audit案:** 能力を`NOTIFICATION:PERMIT:ISSUE`／`READ`／`EXECUTE`／`CLOSE`へ分ける採用候補。自moduleのscope契約でenvironmentとpublication範囲を先に拘束し、Identityへ業務属性を追加しない。発行・確認終了のactorは将来の実認証主体、消費actorは真正な許可発行者USER、workerは別相関。初回testでPublic principalを作る経路はApplication境界の検証であり、本人認証成立の追加証拠とはしない。

Businessは発行／消費／確認終了＋同一transaction、resourceはpermit ID、actionは能力名とは別の操作分類とする。具体コード／actor／resource／失敗時の対応は[採用判断表§4](phase4-s1-reference-foundation-adoption-decisions-20261006.md#4-d5初回audit対応表)を採用候補とする。scope／TTLは初回の照合・期限規則・未接続拒否を必須とし、運用供給元／割当・正式Duration／時計差は運用受付前に閉じる案。後段化自体もOwner判断事項であり、testの固定Clock／有限値から正式値を推定しない。

## 3. 通常Referenceを守る登録・migration・依存条件

| 面 | 本票の具体案 | 失敗時の扱い |
|---|---|---|
| 有効化 | property案`koiki.reference.notification.foundation.enabled`、既定false。初回はintegration testから明示有効化、通常の設定fileへtrueを追加しない | 無効／未設定で通常起動を維持。有効時にscope／対象／証拠の供給が欠けた操作は拒否 |
| component／Entity | 条件付きConfiguration以外へ自動登録stereotypeを付けず、EntityScanは有効構成でnotification modelだけ追加。既存master／expense／Framework Entity登録は維持 | 無効時のmetamodel／Beanに追加型が入るなら停止。既存EntityScanを全体置換しない |
| Repository | `@NoRepositoryBean`契約をAdapterが実装し、条件付き`@Bean`から登録。既存root Repository scan／autoconfigurationを無効化しない | interfaceを置いただけで無効時に発見・生成される場合は構成を戻す |
| migration | 新規location候補`db/migration/kkref-notification/`の`V4__create_notification_recovery_records.sql`。既存V1〜V3を維持、追加有効時の管理適用で両location・同じ`kkref_flyway_history`を指定 | 通常`application.properties`のlocation／Flyway設定を変えない。初回にpublication schema／Framework SQLを追加しない。V4衝突時は番号案へ戻す |
| role／grant | 2記録に対する発行列INSERT、閉鎖列＋version UPDATE、消費INSERT／SELECT、観察SELECTを用途別にする。DDL適用者とruntimeを分離 | Reference table権限だけの手順をreview。Identity／Audit等の必要grantはFramework-owned管理の範囲と別表で承認し、owner継承・grant拡大でPASSにしない |
| POM／Framework | 初回は既存Reference dependenciesを使用。Framework API／Rule／BOM／runtime Modulith dependency変更なし | artifact不足は座標・取得／install差分を提示。既存Tooling artifact準備承認を流用しない |

管理適用はfresh（Framework二階層→V1〜V4）とV1〜V3からのupgradeを別caseにする。通常構成は追加schema未適用／適用済みの両方で起動確認する。migration失敗は履歴・既存row保持と診断を確認する。追加schemaを消すdown migrationをrollback手段とせず、機能無効・追加記録保持で戻せる範囲を実証する。旧JAR互換と未解決記録の再開は実証前に保証しない。

具体column／制約は[採用判断表§5](phase4-s1-reference-foundation-adoption-decisions-20261006.md#5-d6ddlの具体採用案)。通常locationからV4が見えない構成でも適用済み履歴のvalidationと両立することを必須証拠に加える。未実証の互換を前提にせず、不成立ならmigration配置へ戻す。既存validationやhistoryを弱めて通さない。

初回の非Web正式entry pointとWeb Security変更は対象外。CのSession／registry除外やChecker不在を正式通常Webへ適用しない。既存login／logout・CSRF／Security Header・default denyを維持する。追加modeの物理entry point、通常通知・再送は後段で個別審査する。

## 4. 必須検証の作成計画

**method対応・後続実行資料:** [残判断・初回実行資料§2](phase4-s1-reference-foundation-execution-review-20261006.md#2-新規6-class55-invocationのmethod対応案)に6 class／55件をmethod対応へ展開した。§3でclassごとのMaven実行・DB停止確認・証拠保全と設定名／用途別接続配分を提示する。資料採用・Gate・作成実行開始は別判断。

**後続採用:** [初回実行資料§6](phase4-s1-reference-foundation-execution-review-20261006.md#6-初回実行資料のowner採用2026-10-06)で提示したmethod対応・設定／接続配分・実行／cleanup手順をOwner採用済み。Gate設置／限定判定と正式作成検証開始は残る。

予定6 class／55 invocationを初回の作成・検証計画としてOwner採用済み（対話確認）。実装前にmethod一覧へ展開し、結果XMLと照合する。必要な追加・変更が判明したら差分を提示する。未作成・skip・必要Adapter未接続をPASSにしない。test代替はscope／対象／運用証拠の有限入力に限定し、Query／Recorder／DB／transactionをmockにしない。

| 新規test（notification配下） | 件数案 | 必須枝 |
|---|---:|---|
| `RecoveryPermitTest` | 12 | 期限直前／一致／直後3、target差異（environment／publication／event／listener／attempt）5、閉鎖済み1、生成必須値2、閉鎖一式整合1 |
| `RecoveryConsumptionTest` | 3 | operation欠落／worker世代欠落／permit欠落の拒否。更新操作のない契約はArchitecture／DB側でも確認 |
| `NotificationFoundationTransactionTest` | 16 | 発行／消費／閉鎖とAudit原子性3、各Audit失敗rollback3、能力不足4、DISABLED／不存在2、Query障害1、scope外1、transactionなしBusiness拒否1、Security障害時拒否維持1 |
| `NotificationFoundationPersistenceTest` | 12 | 不変列UPDATE拒否3、消費UPDATE／DELETE／TRUNCATE拒否3、permit／operation一意競合2、消費競合／閉鎖競合2、commit／rollback可視性2 |
| `NotificationFoundationRegistrationTest` | 7 | 未設定／false時の登録不在2、設定不正1、有効時のscope／対象証拠未接続による拒否2、有効時の必要Entity／Adapter登録1、sender／runner／registry／scheduler不在1 |
| `NotificationFoundationMigrationTest` | 5 | fresh1、upgradeと既存row保持1、適用失敗保全1、追加無効＋旧schema1、追加無効＋適用済みschema1 |
| **合計** | **55** | **件数採用済み。method対応確認と正式開始条件成立後に実装** |

実運用の停止／provider証拠は未成立のままなので、肯定側の有限test proofと、productionで未接続なら拒否する経路を別caseで検証する。UNKNOWN消費を削除しない、未閉鎖対象の次許可を禁止する規則は永続化・競合caseへ含める。生のJPA transactionではなくSpring管理transactionを使う。

既存Referenceの回帰は現在の全25 test classを対象とする。Architecture／Identity境界、Identity管理／認証・Bearer・URL／method security、masterの実DB・read model・無効化・認可、expenseの実DB・read model・Audit rollback・Domain・認可・REST、業務URLを含む。実行前にsource／XMLからclass・invocationを棚卸しし、baselineと新実装を同じcommandで比較する。新規6 classと合わせてReference側31 classを欠落なく確認する。

## 5. command案・資源・Evidence

offline preflightで実効POM／tree、reactor順序、既存artifactとsourceの整合、Docker、既存Chromiumを確認する。今回は実行しない。baselineと実装後の比較では同じ既存Reference test集合を使い、framework upstreamに同名classがある場合は報告先も区別する。

```powershell
# 基準／変更後：Reference sourceと同じreactor依存をclean package
.\mvnw.cmd -o -pl koiki-reference-app -am -DskipTests clean package
# 新規6 class：upstreamに対象classがないことは許容、Reference XMLの6 class／55件を別途必須確認
.\mvnw.cmd -o -pl koiki-reference-app -am "-Dtest=RecoveryPermitTest,RecoveryConsumptionTest,NotificationFoundationTransactionTest,NotificationFoundationPersistenceTest,NotificationFoundationRegistrationTest,NotificationFoundationMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" test
# 既存25 class：baselineのtest集合を明示し、新規6 classとは別実行
$referenceBaselineTests='ReferenceArchitectureTest,ReferenceBusinessUrlSecurityTest,MasterReadModelPostgreSqlIntegrationTest,MasterPostgreSqlIntegrationTest,DepartmentDeactivationPostgreSqlIntegrationTest,ExpenseReadModelPostgreSqlIntegrationTest,ExpensePostgreSqlIntegrationTest,ExpenseAuditRollbackPostgreSqlIntegrationTest,MasterCatalogQueryMethodSecurityTest,MasterAdministrationTest,MasterAdministrationMethodSecurityTest,ReferenceSecurityConfigurationTest,ReferenceJwtAuthenticationConverterTest,ReferenceApiJwtDecoderTest,ExpenseRequestTest,IdentityUserManagementTest,IdentityUserManagementMethodSecurityTest,ExpenseReadServiceMethodSecurityTest,ExpenseApplicationServiceMethodSecurityTest,JpaMasterAvailabilityQueryTest,IdentityUrlSecurityTest,IdentityManagementExceptionHandlerTest,IdentityManagementControllerTest,ExpenseApiPostgreSqlIntegrationTest,ExpenseApiControllerTest'
.\mvnw.cmd -o -pl koiki-reference-app -am "-Dtest=$referenceBaselineTests" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=1" "-DargLine=-Xmx768m" "-Djunit.jupiter.execution.parallel.enabled=false" test
# package済み既存critical journey：既存READMEのlocal entrypoint
.\mvnw.cmd -o -f build-support/reference-e2e-verification/pom.xml "-DforkCount=1" "-DargLine=-Xmx768m" test
```

上記は初期command案の履歴。採用済みclass単位終了・DB停止確認を実行する具体案は[後続実行資料§3](phase4-s1-reference-foundation-execution-review-20261006.md#3-検証設定接続配分実行cleanupの具体化)を優先し、既存25 classを一括指定せずclassごとに呼び出して停止確認後に次へ進む。まだ実行しない。

回帰class一覧は[Reference test directory](../../koiki-reference-app/src/test/java/org/koikifw/reference/)から固定し、実行する正確な引数と期待件数をpreflight Evidenceへ記録する。25 classを任意の少数testへ縮めない。新規・既存・E2Eは逐次、途中の環境／compile障害を無条件rerunしない。offline未準備なら取得・installの必要差分で停止する。Chromium installは本票の承認候補へ含めない。

資源上限案はMaven1／fork1／heap768 MiB、DB1＋Ryuk／DB1 GiB・CPU1／max_connections16、pool最大4、同時connection8以内。lock10秒・DB statement／transaction10秒・各新規DB class10分を候補とする。baseline／既存test／E2Eの既存timeout・pool条件も調査し、変更せず上限を満たせなければ検証方式へ戻す。E2EはDB1と追加application／issuer process・Chromium1を必要とし、通常のDB testと同時起動しない。

**source調査による補正:** E2Eのissuerはtest JVM内HttpServerであり、別issuer JVMではない。別processはReference JARとPlaywright／Chromium側。既存実DB testのcontext保持、DB資源未設定、別Reference JVMのheap未指定を[上限調査§7](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#7-検証上限のsource調査対話確認後2026-10-06)に記録した。逐次実行・上限調査・到達／不整合時停止の方針はOwner採用済み。数値・具体方式と必要な検証専用変更の採用は残る。

**検証専用設定の後続案:** Ownerは明示選択時だけ資源制限を適用する設定の具体化を了承した。[具体案§8](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#8-検証専用設定の具体案)で既存test fixture／E2E／READMEの変更対象、fork寿命・子JVM・pool／DB制限とavailable memory8 GiBへの増額案を提示する。上記memory4 GiB案を承認済みとせず、新旧数値・具体方式は審査後に一本化する。code／POM変更・実行は未開始。

**対象範囲の対話採用:** 既存Reference DB fixture・E2Eへ、明示選択時だけ制限を適用する設定を追加し、通常の検証設定を維持する対象範囲はOwner採用済み。具体設定名・数値・fork寿命とcode変更／環境実行開始は残る。

**対話採用済みの資源条件:** 開始時available memory8 GiB／disk10 GiB以上、DB同時1／memory1 GiB／CPU1／max_connections16、Maven・test・E2E子Referenceの各heap768 MiB、既存回帰／E2Eのpool最大4。資源不足・制限下不成立なら停止して再判断する。従来available memory4 GiB案は8 GiBへ改訂する。heapを全processの総memory上限とは扱わず、実効設定と開始・終了時の利用状況を確認する。

**対話採用済みの検証時間:** 新規DB testは各class10分、既存25 class＋E2Eはbaseline／実装後それぞれ合計60分。上限到達時は追加実行を止め、進行中処理を安全に終了・cleanupする。未実施分・原因・実測時間を示して再判断し、未実施をPASSにしない。

**対話採用済みの記録量・再実行管理:** raw合計1 GiB以内、同じ原因・条件でのrerun最大1回。再実行前に原因と条件を確認して継続理由を記録する。上限到達時は必要証拠を保持して停止・再判断し、証拠を消して続行しない。

**残る具体手順:** 採用済みのclass単位終了・minimum idle・新規接続総予算・DB待ち条件の具体設定／command・cleanup手順は残る。採用済みの条件だけで設定実装・実行開始や全process制限成立を認定しない。

**対話採用済みの既存回帰方式:** 並列化せず、classごとにtest JVM・DBを終了し、DB停止確認後に次へ進む。DB残存時は次を起動せず停止する。baseline／実装後を同条件で比較する。fork1／reuseForks=falseは具体command候補であり、それだけでDB停止確認が成立したとは扱わず、実行・cleanup手順を具体化する。

**対話採用済みの新規DB操作上限:** 新規DB testのロック待ち・SQL実行・transactionは各10秒。超過した操作は失敗として扱い、rollback・記録保全を確認する。既存回帰／E2Eの待ち時間は維持し、新規testへの具体適用方式・実効上限は実装／検証で確認する。

**対話採用済みの接続数管理:** 既存回帰／E2Eはpool最大4／minimum idle1。新規testは発行・消費・観察の用途別poolと管理接続の同時合計8以内。用途別配分・実効値を具体構成と検証で確認する。

**対話採用済みの作業管理条件:** 初回基盤のみ24〜40標準時間の低確度概算を採用。内訳はモデル／契約4〜6、条件付き構成・保存／認可／Audit8〜14、migration／grant・実DB6〜10、既存回帰／package・Evidence6〜10。12時間相当で通常無効構成と登録／migration成立状況を確認し、40時間相当で未完なら停止して残作業・見積を再提示する。標準作業量であり、経過時間・AI実行時間や完了期限を約束する値ではない。Owner review稼働・環境待ち・Gate整合は別枠で未算定。A／B／Cの短時間PASSや過去の見積を承認予算へ流用しない。

Evidence先は`docs/architecture/validation/phase4-s1-reference-foundation-<実行日>.md`、rawはReference／E2Eのtarget配下へrunごとに保存する案。source・artifact・schema hash、実効POM、baseline／変更後XML、55件対応、Bean／metamodel／route／task、DB／Audit突合、資源・cleanup・警告・未達を記録する。test user／password／Cookie／key／Session bytesを出力しない。

## 6. 開始経路・blocking review・残条件

| 判断 | 初回で必要な採用・記録 | 現在 |
|---|---|---|
| J1／J8、OR・CP-F0／P4-F | 現行Gateと限定開始経路の正式判断、正本差分、初回保存基盤の開始位置。P4-F提案の設置・通過と本票開始判断を混同しない | D1／D2と限定正本改訂をOwner承認、対象正本へ反映済み（改訂記録§9）。Gate設置／限定判定・正式実行開始は**未成立** |
| CH-01／Tier | §2のnotification／RICH／JPA SHARED、2記録の責務・契約 | 配置・Domain／Application／Adapter責務・RICH／JPA SHAREDをOwner採用済み（[対話確認§6](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#6-対話による確認記録2026-10-06)）。具体field／signature・mapping適合は実装前照合／検証で確認 |
| CH-02／DDL・権限 | V4／location・既存履歴整合、field／制約／索引、用途別role／必要grantのOwnership・fresh／upgrade | D6と対話による用途別最小DB権限・DDL管理分離・観察の拒否Audit記録を採用し、提示操作・必要Identity参照／Audit INSERTの内容で具体grant表を確定。V4別location／既存履歴は成立確認付き第一案を採用。履歴validation・通常起動・実効権限は未実証、正式開始判断は残る |
| CH-03／Security・登録 | §3の明示登録・既定無効、認可／Audit分類、scope供給・TTL未決の拒否条件、既存Security保護 | D3〜D5と対話による設定別動作・専用Configurationでの処理／DB Adapter／JPA Entity明示登録を採用済み。既存登録を維持し、無効時の追加Bean／Entity不在を検証。具体実装の適合・正式開始票全体の採用は残る |
| J7／実行上限 | §4・5の55件・25 class／E2E、reactor／資源／作業量と調査後の未適合時停止 | 本票の新しい提案。既存PL2 Docker許可をReference全検証へ自動拡張しない |
| CH-04／05／06／07 | registry・復旧runner・expense event・sender／観測・Level／Rules・依存追加 | **初回対象外**。後段の独立reviewと開始条件を維持 |

初回必須はscope照合契約／現在能力確認／供給不能時拒否、TTL判定規則／policy未接続時発行拒否、Audit対応とDDL設計・必要grantの採用。scopeの正式供給元・運用割当、TTL値／時計差を運用受付前へ残すことは[採用判断表D3・D4](phase4-s1-reference-foundation-adoption-decisions-20261006.md#1-採用判断表)で明示的に判定する。採用された場合、初回の肯定側testは有限Adapter／test policyで契約を検証し、運用未接続の拒否を別に検証する。fixture値で運用を有効にしない。provider／旧worker停止の真正性とI/Oまでの失効窓は後段の必須条件として追跡する。

## 7. Owner判定欄・停止点

**最新Gate判断:** [初回実行資料§7](phase4-s1-reference-foundation-execution-review-20261006.md#7-gate-p4-f設置初回限定判定2026-10-06)でGate P4-F設置・初回限定APPROVE LIMITED STARTをOwner承認済み。下記の未成立記載はそれぞれ作成時点の履歴として区別し、現在残る判断はsource固定・preflight条件付き正式作成検証開始。

| 判定単位 | 求める判断 | 記録 |
|---|---|---|
| 技術範囲 | §1〜3の初回対象・責務／登録方式、残るscope／TTL／Audit／DDL採用事項の閉じ方 | D1〜D6と対話による初回範囲・順序、通常無効・専用検証・新旧DB確認、配置・責務・RICH／JPA SHARED、TTL精度調整・専用Configuration明示登録、提示操作範囲の具体grant表を確認／採用済み。実装適合・実効権限の照合／検証等は残る |
| 検証・上限 | §4・5の作成／実行／回帰／Evidence、資源・工数・停止条件 | 対話で受入三本柱と未実施／失敗時不受入、提示した資源数値・検証時間上限・初回24〜40標準時間の管理条件、新規6 class／55件計画を採用。method対応確認・具体command／残る実行方式・環境実行開始は残る |
| 正式開始経路 | §6のJ1／J8・現行Gate／正本整合、必要な個別blocking review成立 | D1／D2と対話の初回前置／後段接続前区分を採用済み。§2正本改訂はOwner承認・反映済み（[改訂記録§9](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#9-正本改訂のowner承認反映記録2026-10-06)）。Gate設置／限定判定・作成実行開始と必要な具体資料は残る |
| 実行開始 | 上記の必要判断と文書baseline固定、preflight成立後に初回を開始すること | 未判断 |

**最終開始判断:** [初回実行資料§8](phase4-s1-reference-foundation-execution-review-20261006.md#8-source固定preflight条件付き正式開始承認2026-10-06)で、文書commit・clean source固定後のpreflightと、成立時の採用済み初回code／test／V4／検証専用設定の作成・Maven／隔離Docker検証をOwner承認済み。上記判定欄の未判断は各審査時点の履歴として保持する。残る開始前提は文書source固定と実際のpreflight成立であり、承認の再取得ではない。

上記の技術範囲・正式開始経路は[採用判断表D1〜D6・§6／7](phase4-s1-reference-foundation-adoption-decisions-20261006.md)と[対話確認§6](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#6-対話による確認記録2026-10-06)へ追跡する。Tier採用後も、J1／J8・Gate整合、具体登録方式、検証上限の採用と実行開始判断は省略しない。

既存回帰失敗、無効時の追加登録／DDL／通信、追加schema未適用で通常起動不能、意図しない既存row更新、権限拡大・dependency／Framework変更、UNKNOWN保全不能、資源／時間上限に達した場合は停止して原因・必要差分・残量を提示する。拒否枝の削除、Rule除外、既存認可の緩和、証拠欠落の安全扱いでPASSにしない。

現在はD1〜D6と対話の初回条件採用、限定正本改訂の承認・反映まで完了。次の出口はmethod対応・具体実行／cleanup手順・F-1〜F-5の提出状態を整え、Gate設置／初回限定判定・正式作成実行開始を判断できる資料にすること。条件が揃うまで正式code／POM／DDL作成・Maven／Docker実行を開始しない。remote push／PR／merge／CI変更／publishは別操作とする。

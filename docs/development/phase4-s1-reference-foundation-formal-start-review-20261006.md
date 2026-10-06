# S1正式開始票：Reference保存・認可・Audit基盤（2026-10-06）

**状態:** DRAFT / OWNER REVIEW。D1〜D6の経路方針・scope／TTL区分・Audit／DDL設計は採用済み。必要Gate／正本整合・残るblocking review・上限・実行開始は未承認、実装未開始。
**baseline:** branch `feature/phase4-s1-reference-foundation`／HEAD `f5e2672`。A／B／C受入を含むcommitから分岐し、Reference保護の検討文書と関連導線4ファイルを未commitで引継ぎ。本票を含む文書差分を固定するcommitを、実装開始前のsource baselineとして別途記録する。
**正本入力:** [安全側の接続検討案](phase4-s1-reference-safe-integration-design-draft-20261006.md)、[CH-01〜08／ST-C](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)、[J1〜8・開始経路](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)、[P4-F提案](KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md)、[C受入§6](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#6-owner受入承認2026-10-06)。

**具体採用の判断先:** [初回採用判断表§8](phase4-s1-reference-foundation-adoption-decisions-20261006.md#8-owner採用判断2026-10-06)。D1〜D6の推奨案と初回／後段条件区分をOwner採用済み。scope／TTLは初回契約と運用供給元／値の採用時点を分ける。Gate／正本整合と実行開始は別判断。

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

予定6 class／55 invocationを以下で固定する案。実装前にmethod一覧へ展開し、結果XMLと照合する。未作成・skip・必要Adapter未接続をPASSにしない。test代替はscope／対象／運用証拠の有限入力に限定し、Query／Recorder／DB／transactionをmockにしない。

| 新規test（notification配下） | 件数案 | 必須枝 |
|---|---:|---|
| `RecoveryPermitTest` | 12 | 期限直前／一致／直後3、target差異（environment／publication／event／listener／attempt）5、閉鎖済み1、生成必須値2、閉鎖一式整合1 |
| `RecoveryConsumptionTest` | 3 | operation欠落／worker世代欠落／permit欠落の拒否。更新操作のない契約はArchitecture／DB側でも確認 |
| `NotificationFoundationTransactionTest` | 16 | 発行／消費／閉鎖とAudit原子性3、各Audit失敗rollback3、能力不足4、DISABLED／不存在2、Query障害1、scope外1、transactionなしBusiness拒否1、Security障害時拒否維持1 |
| `NotificationFoundationPersistenceTest` | 12 | 不変列UPDATE拒否3、消費UPDATE／DELETE／TRUNCATE拒否3、permit／operation一意競合2、消費競合／閉鎖競合2、commit／rollback可視性2 |
| `NotificationFoundationRegistrationTest` | 7 | 未設定／false時の登録不在2、設定不正1、有効時のscope／対象証拠未接続による拒否2、有効時の必要Entity／Adapter登録1、sender／runner／registry／scheduler不在1 |
| `NotificationFoundationMigrationTest` | 5 | fresh1、upgradeと既存row保持1、適用失敗保全1、追加無効＋旧schema1、追加無効＋適用済みschema1 |
| **合計** | **55** | **件数採用とmethod対応確認後に実装** |

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

回帰class一覧は[Reference test directory](../../koiki-reference-app/src/test/java/org/koikifw/reference/)から固定し、実行する正確な引数と期待件数をpreflight Evidenceへ記録する。25 classを任意の少数testへ縮めない。新規・既存・E2Eは逐次、途中の環境／compile障害を無条件rerunしない。offline未準備なら取得・installの必要差分で停止する。Chromium installは本票の承認候補へ含めない。

資源上限案はMaven1／fork1／heap768 MiB、DB1＋Ryuk／DB1 GiB・CPU1／max_connections16、pool最大4、同時connection8以内。lock10秒・DB statement／transaction10秒・各新規DB class10分を候補とする。baseline／既存test／E2Eの既存timeout・pool条件も調査し、変更せず上限を満たせなければ検証方式へ戻す。E2EはDB1と追加application／issuer process・Chromium1を必要とし、通常のDB testと同時起動しない。

available memory4 GiB／disk10 GiB以上を開始・終了で確認する案。raw1 GiB以内、原因未変更rerun1回まで。Reference全回帰＋E2Eの確認枠は合計60分を初回案とし、超過時は残量と実測を提示して再判断する。既存E2E processのheap等を未調査のまま制限成立とは認定しない。

技術量は初回基盤のみ24〜40標準時間の低確度案：モデル／契約4〜6、条件付き構成・保存／認可／Audit8〜14、migration／grant・実DB6〜10、既存回帰／package・Evidence6〜10。12時間時点で通常無効構成と登録／migration条件を確認し、40時間で未完なら残量を再判断する案。Owner review稼働・環境待ち・Gate整合は別枠で未算定。A／B／Cの短時間PASSや過去の見積を承認予算へ流用しない。

Evidence先は`docs/architecture/validation/phase4-s1-reference-foundation-<実行日>.md`、rawはReference／E2Eのtarget配下へrunごとに保存する案。source・artifact・schema hash、実効POM、baseline／変更後XML、55件対応、Bean／metamodel／route／task、DB／Audit突合、資源・cleanup・警告・未達を記録する。test user／password／Cookie／key／Session bytesを出力しない。

## 6. 開始経路・blocking review・残条件

| 判断 | 初回で必要な採用・記録 | 現在 |
|---|---|---|
| J1／J8、OR・CP-F0／P4-F | 現行Gateと限定開始経路の正式判断、正本差分、初回保存基盤の開始位置。P4-F提案の設置・通過と本票開始判断を混同しない | D1／D2の経路・準備方針は採用済み。必要な正本反映・Gate判定は**未成立** |
| CH-01／Tier | §2のnotification／RICH／JPA SHARED、2記録の責務・契約 | 採用判断待ち |
| CH-02／DDL・権限 | V4／location・既存履歴整合、field／制約／索引、用途別role／必要grantのOwnership・fresh／upgrade | D6の設計・grant方針採用済み。具体grant実行表・正式開始判断は残る |
| CH-03／Security・登録 | §3の明示登録・既定無効、認可／Audit分類、scope供給・TTL未決の拒否条件、既存Security保護 | D3〜D5採用済み。具体登録方式・正式開始票全体の採用は残る |
| J7／実行上限 | §4・5の55件・25 class／E2E、reactor／資源／作業量と調査後の未適合時停止 | 本票の新しい提案。既存PL2 Docker許可をReference全検証へ自動拡張しない |
| CH-04／05／06／07 | registry・復旧runner・expense event・sender／観測・Level／Rules・依存追加 | **初回対象外**。後段の独立reviewと開始条件を維持 |

初回必須はscope照合契約／現在能力確認／供給不能時拒否、TTL判定規則／policy未接続時発行拒否、Audit対応とDDL設計・必要grantの採用。scopeの正式供給元・運用割当、TTL値／時計差を運用受付前へ残すことは[採用判断表D3・D4](phase4-s1-reference-foundation-adoption-decisions-20261006.md#1-採用判断表)で明示的に判定する。採用された場合、初回の肯定側testは有限Adapter／test policyで契約を検証し、運用未接続の拒否を別に検証する。fixture値で運用を有効にしない。provider／旧worker停止の真正性とI/Oまでの失効窓は後段の必須条件として追跡する。

## 7. Owner判定欄・停止点

| 判定単位 | 求める判断 | 記録 |
|---|---|---|
| 技術範囲 | §1〜3の初回対象・責務／登録方式、残るscope／TTL／Audit／DDL採用事項の閉じ方 | D1〜D6の対象・条件区分・Audit／DDL設計採用済み。Tier／具体登録方式等は未判断 |
| 検証・上限 | §4・5の作成／実行／回帰／Evidence、資源・工数・停止条件 | 未判断 |
| 正式開始経路 | §6のJ1／J8・現行Gate／正本整合、必要な個別blocking review成立 | D1／D2の方針採用済み。正本改訂・Gate判定・必要review成立は未判断 |
| 実行開始 | 上記の必要判断と文書baseline固定、preflight成立後に初回を開始すること | 未判断 |

上記の技術範囲・正式開始経路は[採用判断表D1〜D6・§6／7](phase4-s1-reference-foundation-adoption-decisions-20261006.md)へ追跡する。初回と後段の区分を採用しても、J1／J8・Gate整合、Tier／登録方式、検証上限の採用と実行開始判断は省略しない。

既存回帰失敗、無効時の追加登録／DDL／通信、追加schema未適用で通常起動不能、意図しない既存row更新、権限拡大・dependency／Framework変更、UNKNOWN保全不能、資源／時間上限に達した場合は停止して原因・必要差分・残量を提示する。拒否枝の削除、Rule除外、既存認可の緩和、証拠欠落の安全扱いでPASSにしない。

今回は文書化とD1〜D6採用記録の反映まで。次の出口は採用済み方針によるJ1／J8の必要提出物・正本改訂差分の準備と、本票の残る技術・検証上限・実行開始判断。条件が揃うまで正式code／POM／DDL作成・Maven／Docker実行を開始しない。remote push／PR／merge／CI変更／publishは別操作とする。

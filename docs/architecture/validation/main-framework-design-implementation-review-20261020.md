# main のフレームワーク設計と実装レビュー

レビュー日：2026-10-20  
担当：Codex  
対象：ローカル `main` / `521572d195a5a854c57ef58b2b61c59ce5c5a0b8`  
状態：レビュー実施済み。修正・受入・Phase開始の承認記録ではない。

## 1. 結論

Ownership分離、Tier別構造、Applicationのtransaction境界、DTOによる外部境界、default deny、Business／Security Auditの分離という設計方針は維持できている。一方、**個別機能を組み合わせたときの設定・例外処理・検証対象の境界**に問題がある。特に汎用例外処理、Session Cookie設定、scopeより前のversion判定を優先して修正対象とする。

全面的な設計変更を要する証拠はない。P4-B1では、既存MVC・Session・APIを組み合わせる前に、以下の限定された問題を解決する方がよい。非同期系の未達をP4-B1への移行条件に追加しない。

添付 `KOIKI-JavaWev-v0.1.5_Claude点検.md` は10項目の整理と優先順位案であり、元レビュー全文や再現ログではない。その指摘と提案を参考にし、文書末尾の進め方の質問などを今回の指示・承認とは扱っていない。

## 2. レビュー計画と実施範囲

本レビューはPhase横断の現状評価で、実装開始ではない。成果はTooling所有の再現用sourceと本記録。Framework／Reference／Toolingそれぞれの実装を、グランドデザイン、ADR register、現行AGENTS、2つのKOIKI Skillに照合した。

| 順序 | 点検軸 | 実際に点検した主な対象 | 手段 |
|---|---|---|---|
| 1 | 配布・依存・拡張性 | Root／Parent／BOM、Starter構成、AutoConfiguration、defaults、Reference POM | source・契約照合 |
| 2 | WebとSecurityの合成 | API Advice、MVC Advice、API version、SecurityFilterChain、CookieSerializer | source＋MockMvc／Boot context |
| 3 | Identity・Session・Audit | 認証provider、管理処理、失効adapter、cleanup、Audit transaction | source・既存Evidence照合 |
| 4 | 業務・永続化 | expense aggregate／service／read model、masterの有効性照会と同期veto、migration | source＋scope/version最小再現 |
| 5 | 規約・検証・運用 | ArchUnit入口、既存test、CI、専用非同期profile、Observability、provider adapter | source＋import対象の比較 |
| 6 | 導入と次作業 | Developer Journey、受入Evidence、Phase4残タスク§8、Claude整理 | 所見の優先度・依存関係整理 |

広範な設計境界と主要実装経路を対象としたレビューであり、全メソッドの精査、侵入試験、性能測定、最新CVE調査、全test再実行ではない。remote照合・fetchはしていないため、remote mainとの一致は主張しない。

開始時のworking treeはclean。実行検証は単一Java process内のDB・HTTP server不要のprobeに限定した。S1専用集合、Docker、通常root build、CIは実行していない。

## 3. 優先所見

優先度はP1＝影響経路の実装・受渡し前に対応、P2＝条件を明示して計画対応。確度は「実行再現」「source確認」「未実証の懸念」を分ける。

### F1 汎用AdviceがSecurityとHTTP例外の意味を失わせる

**P1 / Framework / 実行再現 / Claude R3-1を支持し影響範囲を追加。**

対象：[KoikiProblemDetailsExceptionHandler.java](../../../koiki-starters/koiki-starter-api/src/main/java/org/koikifw/starter/api/internal/KoikiProblemDetailsExceptionHandler.java) 92–100行。

`@ExceptionHandler(Exception.class)` が全Controllerに適用され、特別な例外をすべて500へ変換する。MockMvcで `AccessDeniedException`、`BadCredentialsException`、`@ResponseStatus(404)` 付き独自例外の3種が500となった。最後の例外はこのAdviceがなければ404。

Security filter内の拒否がすべて500になるという意味ではない。MVC内で発生し、先に別Adviceが解決しない例外が対象。Reference expense MVCの局所再throwはこの問題を回避するが、Customer全般の解決にならない。認可迂回ではなく、拒否応答・認証entry point・クライアントのエラー処理を壊す問題である。

修正方向：Security例外はSecurityの処理へ伝播し、標準のHTTP意味を持つ例外を尊重する。単純にAccessDeniedだけを403へ変換すると、未認証時のentry pointやMVC/HTMX応答を飛ばす可能性がある。局所回避策を外すのは合成test成立後。

最小受入：認証済み権限不足403、未認証時の経路別401／login遷移、独自404、通常予期せぬ例外500をAPI／MVCの両側で確認。

### F2 ReferenceのCookieSerializerがBootのCookie設定を迂回する

**P1 / Reference・導入例 / 実行再現 / Claude R4-2を支持。**

対象：[ReferenceSecurityConfiguration.java](../../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java) 29–32行。

独自Beanが無設定の `DefaultCookieSerializer` を返す。BootのSessionAutoConfigurationを含むcontextで `secure=true`、`same-site=strict` を指定しても、非secureなmock requestに対するCookieにSecureは付かず、Strictも適用されなかった。secureなrequestではSecureが付く。

これはHTTPS通信が常に非Secure Cookieになるという指摘ではない。TLS終端後のHTTPS認識がない場合などに、明示したSecure設定で強制できない。HttpOnly／Laxの既定が消失するという主張でもない。

Boot 4.1.1は自前CookieSerializerがあると、ServerPropertiesを適用するBean生成をback offする。公式の[SessionAutoConfiguration](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.1/module/spring-boot-session/src/main/java/org/springframework/boot/session/autoconfigure/SessionAutoConfiguration.java)の条件と一致する。[Spring Sessionの説明](https://docs.spring.io/spring-session/reference/guides/java-custom-cookie.html)でも、無指定のSecureはrequest判定に従う。

修正方向：Boot管理へ戻せるかを確認する。logout customizerが必要とするBeanとAutoConfiguration順序も含める。独自Beanを残すなら設定を明示的に反映する。

最小受入：HTTP／HTTPS認識の有無、明示Secure設定、SameSite上書き、Cookie削除時のpath／name整合。propertyの存在確認だけでなく実際のSet-Cookieを検査する。

### F3 scope外の申請についてversion情報を区別できる

**P1 / Reference / 実行再現 / Claude R5-1を支持。**

対象：[ExpenseApplicationService.java](../../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseApplicationService.java) 251–255行。

`requireVersion` がowner／approver scope検査より前にある。他人の申請に対する `submit` を直接呼ぶ最小fixtureで、version一致は `NOT_FOUND`、不一致は `CONCURRENT_MODIFICATION` になった。API Adviceではそれぞれ404／409に写像される。既知のUUIDに対して、存在やversion一致を応答差で判別できる。

更新が成功する問題ではない。正しいoperation権限を持つが対象scope外の利用者に対し、対象を秘匿する契約が崩れる。probeはApplicationを直接呼び、method securityやHTTP経路全体は通していない。

修正方向：対象認可後にversionを照合する。owner／approver両方の操作へ適用し、scope内の競合応答は維持する。

最小受入：存在しないID、scope外の一致／不一致／負version、scope内の一致／不一致を比較。拒否時にmutation／Audit／eventが発生しないことも確認。

### F4 API versionの既定がMVCにも適用される

**P2 / Framework構成契約 / 実行再現 / Claude R3-3を支持。**

対象：[KoikiApiDefaultsEnvironmentPostProcessor.java](../../../koiki-starters/koiki-starter-api/src/main/java/org/koikifw/starter/api/internal/KoikiApiDefaultsEnvironmentPostProcessor.java) のpath segment／required既定。

同じ既定をSpring MVCへ設定したprobeでは `/`、`/login`、`/expenses/list` が400、`/api/1/probe` が200。Reference相当のAPIパス限定resolverとrequired=falseではloginとAPIがともに200だった。

現行Referenceは [application.properties](../../../koiki-reference-app/src/main/resources/application.properties) と [ExpenseApiVersionConfiguration.java](../../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/configuration/ExpenseApiVersionConfiguration.java) で回避済み。したがって「P4-B1に先立ってFramework既定値を必ず変える」とはしない。共存構成を明示し維持する選択肢もある。

修正方向：API専用既定と混在アプリの適用方法を契約化する。既定property変更はPublic API inventoryと互換性のreview対象。

最小受入：MVC、login、存在しないpath、正しい／不正／未対応API version。required=falseだけにして不正APIを受け入れていないかも見る。

### F5 Architecture testが本番とtest fixtureを混在して検査する

**P2 / Reference test / 既存classの実行比較 / Claude R2-2を支持。**

対象：[ReferenceArchitectureTest.java](../../../koiki-reference-app/src/test/java/org/koikifw/reference/ReferenceArchitectureTest.java) 13行・21–22行。

両importに `DO_NOT_INCLUDE_TESTS` がない。手元のクラスパスでbusiness rulesを評価すると241 classes中67がtest-classes由来、違反15件。test除外では174 classes、違反0件だった。

この数は既存compiled classを含むprobeの観測であり、現HEADをclean compileした通常rootの成否ではない。現在のRules sourceはprobe用に再compileしたが、Reference全testの再compileはしていない。source上のimport対象指定不足と、既存classでの混入を区別する。

修正方向：production architectureの検査対象を明示する。テストを単に無効化したり、違反クラスを名前で除外する対応は避ける。必要なtest fixture向け規約は独立させる。

Claude R1-1の「配置」について、`ReferenceBusinessUrlSecurityTest.java` のdirectoryと宣言packageに不一致はある。しかしJavaのpackage宣言は `referenceacceptance.web` であり、directoryだけでproduction scan混入や誤動作を断定しない。配置整理とimport修正は別事項。

最小受入：clean test-compile後のArchitecture testと、production違反を入れた時に検出できる既存負例。過去の305件PASS等を現在の再実行結果へ転記しない。

### F6 StarterのEntityScanがCustomerの暗黙scanを置き換える

**P2 / Framework導入 / sourceとBoot実装で確認、起動再現は未実施 / Claude R4-1を支持。**

対象：[KoikiAuditAutoConfiguration.java](../../../koiki-starters/koiki-starter-audit/src/main/java/org/koikifw/audit/internal/KoikiAuditAutoConfiguration.java) 20行、[KoikiIdentityAutoConfiguration.java](../../../koiki-starters/koiki-starter-identity/src/main/java/org/koikifw/identity/internal/KoikiIdentityAutoConfiguration.java) 31行。

Framework側でEntityScanPackagesを登録する。Bootの [JpaBaseConfiguration](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.1/module/spring-boot-jpa/src/main/java/org/springframework/boot/jpa/autoconfigure/JpaBaseConfiguration.java) は、その一覧が空のときだけApplicationのAutoConfigurationPackagesへfallbackする。そのため、Customerが自分のEntityScanを宣言しない構成で、業務Entityの暗黙検出を失う。

Referenceは各業務のPersistenceConfigurationで明示登録するため問題を隠している。内部packageをCustomerへ指定させる回避策は採用しない。

修正方向：既定scanとFramework Entity登録を共存させるか、Customer側明示登録を正式な導入契約にする。後者なら最小Consumer例と診断を整備する。

最小受入：別packageの小さなCustomer Entity＋Repositoryを持つConsumerが、Audit／Identityと同じEntityManagerFactoryで動くこと。Referenceだけで評価しない。

### F7 Framework FlywayとBoot管理Flywayの接続設定が分離している

**P2 / Framework構成境界 / source確認・構成別実DB未検証 / Claude R3-4を条件付きで支持。**

対象：[KoikiDataFlywayAutoConfiguration.java](../../../koiki-starters/koiki-starter-data/src/main/java/org/koikifw/starter/data/internal/KoikiDataFlywayAutoConfiguration.java) 27–40行。

Framework用Flywayは注入DataSourceと固定location／historyで別生成される。Customer用Flywayのschema、専用接続・資格情報、customizerを参照しない。アプリDataSourceと異なるmigration用接続やschemaをBoot propertyで指定すると、二つのmigrationの実行先・権限が分かれる可能性がある。また、Customerが独自FlywayMigrationStrategyを置くとKOIKI strategy全体がback offし、Framework migrationの先行実行もCustomer責任になる。

既存CP4は「同じDataSource」という設計を明記しているため、未検証の構成をすべて対応済みと読む方が問題。直ちに全Flyway propertyをコピーする修正は、Ownership分離を壊す可能性がある。

修正方向：対応するDataSource／schema構成とstrategy置換時の責任を明示し、未対応構成を起動時に拒否するか、限定的に対応する。

Claude R3-5のbaseline既定trueはコード通り。ただし二階層migrationではFrameworkのtable作成後にCustomer historyを開始するため、baseline 0は既存の成立条件でもある。**trueという理由だけで欠陥／データ損失と断定せず、falseへ単純変更しない。** [既存のhistory実証](phase2-p2-c2-c2-3-verification.md)も参照する。

## 4. 新たに抽出した検証課題

### H1 部門無効化と申請新規作成の並行実行

**Reference / 未実証の設計懸念。** 確定不具合件数には含めない。

[MasterAdministration.deactivateDepartment](../../../koiki-reference-app/src/main/java/org/koikifw/reference/master/application/MasterAdministration.java) は同期eventでpendingの不存在を確認してから部門を更新する。[DepartmentDeactivationGuard](../../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/DepartmentDeactivationGuard.java) はexists queryで確認し、expense createDraft側は別の有効master照会を使う。DRAFTもpendingに含まれるため、既存draftのsubmitとの競合をこの例に使わない。

次の交錯を止める共通lock／DB制約／isolation指定は点検箇所に見当たらない。

1. 申請がまだない部門について、createDraftが有効な部門を読む。
2. 部門無効化が、まだcommitされていない新規draftを見ずpendingなしと判定する。
3. 両transactionがcommitし、無効部門と新規draftが共存する。

同一transaction内の同期veto成立は、別transaction間の直列化を保証しない。受入済みの逐次ケースを否定せず、「commit時にもpendingと無効部門の共存を禁止する」のかを確認し、その要件なら2 transactionのバリア付きDB testを最小の次検証とする。Framework一般lock機構を先行追加しない。

## 5. CIと文書についてのClaude所見の評価

| Claude項目 | Codex判断 |
|---|---|
| R6-1 非同期CI対象外 | `.github/workflows/ci.yml` は通常package／通常testで、専用async profile／enable propertyを指定しない。専用testにはEnabledIfSystemPropertyがある。自動回帰に空白はあるが、現行の意図的分離・Ownerの非同期区切りを尊重する。CI job追加を自動着手・P4-B1前提にしない |
| R10-1 落とし穴未文書化 | 「文書が全くない」は支持しない。CP4や各Evidenceに説明はある。Developer JourneyからCookie、例外、EntityScan、Flywayの組合せ制約へ辿る導線が弱い。修正採否と併せて短い導入チェックを既存Journeyへ集約する |
| R10-2／R10-3 統制粒度 | 品質の指摘と承認手続きを分けることに賛成。詳細が添付にないため内容の全面追認はしない。内部不具合修正、公開既定値変更、remote／workflowを分け、今回のレビューを新たな包括開始承認にしない |
| 同じStarterの変更を一括検証 | moduleが同じことだけでは適切な単位にならない。F1はSecurityとの合成、F4はMVC mapping／property契約。共通回帰はまとめられるが、変更判断と受入条件は別に保つ |

非同期の自動回帰不足は今後expense/event境界を変更するときの影響評価へ残す。通常buildへの全専用test取り込みや、停止済みの復旧追加作業を提案しない。

## 6. 維持すべき設計と確認した範囲

| 設計 | 点検結果 |
|---|---|
| 配布とOwnership | Root／Parent／BOMを分離し、ReferenceをStarter内部へ持ち込む依存は点検箇所にない。非同期JDBC runtimeはReferenceの選択profile |
| Security | fallback chainはdenyAll。Referenceのbrowser chainはCSRF有効。BearerはAPI限定・statelessでissuer／audience／token_useを検証する構成。CSRF無効をSession側へ拡張していない |
| Identity | 認証失敗の汎用化、資格情報消去、失敗記録、依存失敗時の拒否を確認。管理APIの認可はReference Application側も点検対象とし、Framework内部へ業務権限を追加しない |
| Audit | BusinessはMANDATORY、SecurityはREQUIRES_NEWでtransactionを分け、storeはpersist／flush。今回DB rollbackや失効競合の再実証はしていない |
| 業務モデル | expenseは状態遷移をaggregateへ集約。Moneyは加算overflowを検出。masterはTier 1。MVC／APIは同じApplicationを使い、別DTO／Form |
| read modelとcache | scope条件をSQLへ入れ、表示recordへmaterializeする。選択肢cacheは表示用途で、commandは別の有効master照会を行う |
| Observability | requestIdを検査してMDCへ設定しfinallyで復元する。専用非同期executorとのcontext伝播や全logの機密値監査は今回未実証 |
| 非同期境界 | 通常無効・loopback限定provider、redirect拒否、有限応答サイズ、UNKNOWN/HOLD分岐を確認。本番provider／復旧保証には読み替えない |

## 7. 次の対応順序

**2026-10-20 Owner合意後の作業への接続：** 「P4-B1の調査・設計は進めつつ、直接影響する問題だけを実装・受入より先に解決する」進め方で合意した。[Phase4との統合作業計画](../../development/phase4-review-integrated-task-plan-20261020.md)に、以下の所見と残タスクを作業単位で対応付けた。F5は検証準備でありB1の調査・設計を止めない。F1／F2／F3の是正とB1設計を実装前に合流し、F4はB1設計内、F6／F7／H1・非同期CIは条件に応じた後続として扱う。以下の順序をPhase4全体の停止条件に読み替えない。

1. **F5の検証対象整備**：productionとtestのimportを分離し、clean compileで再確認する。既存testの成否を信頼して比較するための準備。
2. **F1・F2・F3の限定修正**：利用者に見える拒否応答、Cookie属性、対象秘匿を優先。各修正で上記最小受入を満たす。production修正は今回実施していない。
3. **P4-B1差分確認**：F4のReference回避策を含め、Session APIとMVCの共存契約を固定する。Frameworkの既定値変更は必要性を別に判断。
4. **Customer導入の確認**：F6・F7を小さな外部Consumerで検証し、対応範囲を選択する。H1は必要な業務不変条件を確認して有限DB検証を計画。
5. **文書と回帰運用**：既存Journeyへの制約集約。非同期回帰の実行契機は現行の区切りを維持して別判断。

レビューから次作業へ渡す単位は、巨大なFramework改修ではなく「例外境界」「Cookie適用」「対象認可順序」「検証対象」「Customer構成」の具体的な差分とする。

## 8. 実行証拠と再現方法

[ReviewProbe.java](support/main-review-20261020/ReviewProbe.java) と [run-probe.ps1](support/main-review-20261020/run-probe.ps1) を追加した。テスト用sourceは正式artifact／root reactorに組み込まない。

```powershell
& docs/architecture/validation/support/main-review-20261020/run-probe.ps1
```

既存 `ReferenceArchitectureTest` Surefire XMLからdependency classpathだけを取得する。API Advice、Reference Security構成、expense service／domain、Architecture Rulesを現在のsourceから別出力先へjavacでcompileする。その他は手元の既存class／dependencyを使用する。Java 21、Spring Framework 7.0.9、Boot／Session 4.1.1。Error Prone／NullAwayを通す正式Maven buildの代用ではない。

観測結果：

```text
global-advice: denied=500, unauthenticated=500, annotated missing=500
without-advice: annotated missing=404
reference-cookie: requestSecure=false -> cookieSecure=false
reference-cookie: requestSecure=true  -> cookieSecure=true
global-version: /=400, /login=400, /expenses/list=400, /api/1/probe=200
reference-version: /login=200, /api/1/probe=200
boot-cookie: configuredSecure=true configuredSameSite=Strict
             actualSecure=false actualStrict=false
other-owner: expectedVersion=0 -> NOT_FOUND
other-owner: expectedVersion=1 -> CONCURRENT_MODIFICATION
arch-import: all=241 testClasses=67 production=174
arch-rules: allViolations=15 productionViolations=0
REVIEW completed
```

最後の実行はexit 0。これは現行の問題挙動を再現できたという意味であり、期待する製品挙動に対するPASSではない。直接例外を投げるMockMvc fixture、Bootの部分context、repository代替によるApplication呼出という限定条件を持つ。

初回probeはversion strategy未設定のfixtureにversion付きControllerを混在させて起動失敗した。fixtureを分離して成功、その後F2のBoot context・F3・F5を加えて最終実行が成功した。失敗を製品不具合へ数えていない。

依存のjavap読取時にsandboxからMaven cache内JARへのAccessDeniedExceptionが発生し、同じ診断を環境の権限付き手順で再実行して成功した。probeも同手順を使用。既存一般ガイダンスで対処できたため、権限回避策やAGENTS／Skillの変更は追加していない。

raw logはローカル `target/main-review-20261020/probe.log`。既存test reportsを今回のPASSとして利用しない。変更は本記録と再現用2ファイルのみで、production／既存test／POM／migration／CI／remote／commitに変更操作は行っていない。

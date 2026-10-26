# KOIKI-JavaWeb 提供機能・適用条件・選択ガイド

作成日：2026-10-19。対象読者：KOIKI-JavaWebを利用する開発チームと、その構成・採用を判断する担当者。

初期標準一覧の整理日：2026-10-23。現行入口は§0〜1。§0の区分と各行の状態を併せて読み、必要な機能だけを選択する。

**状態：OWNER APPROVED（2026-10-19）。** Ownerはガイド全体の内容と「KOIKI-JavaWeb」の名称を承認した。その後、製品versionの改変を先送りし、開発向けsourceをGitタグで固定するチェックポイント方式への訂正を承認した。本文の承認は維持する。Python版の呼称「KOIKI-FW」と区別するため、本書ではJava版を「KOIKI-JavaWeb」と表記する。この承認は個別機能の新規採用、正式release、実チーム受入または本番利用の承認ではない。

**2026-10-23追記の位置付け：初期標準一覧の整理初版・方向性OWNER APPROVED／最終提供判断は未実施。** [見直し推進計画§9](phase4-review-integrated-task-plan-20261020.md#9-初期標準確立に向けた見直し推進計画owner-approved2026-10-23)のOwner承認に基づき、既存実装・検証・受入と今後の判断対象を対応付けた。Ownerは本一覧を確認し「方向性を承認します」と明示した。これは一覧整理の方向性の承認であり、最終提供版・全機能の保証・本番利用の承認とは区別する。D2・D1・保証範囲判定・E1の結果を本書へ反映して確立する。

本書は、特定案件に依存しないFramework機能と、その選択に必要な条件を説明する。KOIKI-JavaWeb提供側は、共通機能を独立した提供対象として扱い、公開契約・品質・互換性・利用方法の整備に集中する。案件の業務分析や方式決定を代行せず、開発チームへ選択肢と判断材料を提示する。特定案件の要求を、そのままFramework拡充の前提にしない。

| 識別事項 | 今回の位置付け |
| --- | --- |
| 今回照合するsource断面 | `05911cc`（実装成果は`87728b7`、C2 A受入記録は`05911cc`）。計画文書の未commit差分を保持して一覧を整理。過去の各検証sourceは各Evidenceに帰属 |
| 作業branch | `docs/phase4-review-integrated-plan-20261020` |
| 10月19日の確認断面・branch（履歴） | `8762a71d67fabbd4ab89ccc1f981ae291c249cba`／`docs/v0.1.5-development-baseline-handoff` |
| checkpoint方針（履歴） | 既存の`checkpoint/v0.1.0-phase4-s1-async-boundary-main-20261017`と、次の候補名`checkpoint-v0.1.5`。今回tag作成・remote最新状態の確認はしておらず、現在の提供版をこの候補名だけで確定しない |
| 本書の位置付け | 過去承認済みの選択ガイドに、現断面の初期標準候補・限定成果・判断待ちを追記した一覧初版 |
| 現在のMaven version | `org.koikifw`／`0.1.0-SNAPSHOT`を維持。今回POM・artifactのversion変更なし。初期提供版の固定version・取得先はE1で確定 |

`v0.1.5`は0.1系で進んだ開発成果の区切りを示すタグ上の識別名であり、変更量・互換性・品質を数値だけで保証するものではない。当初の製品version改変案は、`v0.1.5`のチェックポイント方式へ訂正した。正式なversion管理・配布・更新方式は後続で整理する。

開発チームには、利用するcheckpoint、取得方法、前checkpointからの変更点・利用上の影響・既知制約を一つの入口から案内する。内部のPhaseや途中のタグを逐一追うことは求めない。成果物を渡す際は、提供側でタグ・commit・artifact checksumの対応を記録する。同じ`0.1.0-SNAPSHOT`という座標だけで提供物の同一性を判定しない。

機能の選択には本書を使い、実際の導入・開発には[引継ぎガイド](application-team-handoff-guide.md)、[開発環境構築手順](application-team-development-environment-guide.md)、[アプリケーション開発者ガイド](application-developer-guide.md)を使う。Public API・property・認証契約の詳細はリンク先の正本に従い、本書で新たな契約を定義しない。

## 0. 初期標準一覧の読み方と確定までの判断

### 0.1 提供区分

| 区分／一覧の場所 | 現時点の意味 | 残る判断 |
| --- | --- | --- |
| 初期標準の基本候補／§1.1 | 既存Frameworkの公開契約と基盤機能。各行のEvidence・制約を踏まえて初期提供版へ含める候補 | E1で提供artifact・対応構成・版を最終固定。全Starterの一括必須化ではない |
| 条件付き候補・判断待ち／§1.3 | 既存機能でも構成・有効化条件を要するもの、またはD2／D1等で成立範囲を確定するもの | 実装済みと未実装を行ごとに区別。未検証の候補を提供済みにしない |
| Referenceで限定実証済み／§1.2・§2.2 | 利用例の選択経路・限定結果。汎用Framework機能や案件完成品とは別 | B1・C2・S1の保証範囲／DoD全体判断は推進計画の第3段階で整理 |
| 今回の標準対象外／§2.3 | 現断面の完成済み標準に含めないもの | 後続課題・採用条件を保持。不要・廃止の認定ではない |

ここで「基本候補」とすることは、既存の受入成果を未実装へ戻すことでも、現在の案件に対する提供・本番利用を承認することでもない。既存Evidenceを根拠に、複数案件へ同じ条件で案内する初期提供版の範囲を整理している。利用者は機能名だけでなく、適用条件と案件側責任を確認する。

### 0.2 artifact候補と版の識別

Root POMと各module POMを静的照合し、Referenceを除く提供候補は**14座標＝12 JAR＋2 POM**。Root Reactorを含めて数える既存の「15 projects／12 JAR」と矛盾しない。Root Reactor自体は案件向け利用座標に含めない。

| 候補の種類 | artifactId（groupIdは全て`org.koikifw`） | packaging・用途 |
| --- | --- | --- |
| Build・依存管理 | `koiki-parent`、`koiki-dependencies-bom` | 2 POM。案件のbuild・依存基準 |
| アーキテクチャ | `koiki-architecture-contract`、`koiki-archunit-rules` | 2 JAR。module宣言と、明示的に呼ぶtest用規約検査 |
| Runtime Starter | `koiki-starter-api`、`koiki-starter-data`、`koiki-starter-data-jpa`、`koiki-starter-observability`、`koiki-starter-security`、`koiki-starter-audit`、`koiki-starter-identity`、`koiki-starter-session-jdbc`、`koiki-starter-web-mvc` | 9 JAR。必要なStarterだけ選択 |
| test支援 | `koiki-testing` | 1 JAR。Tooling-owned、Customer-facingなtest用途。production runtimeではない |

`koiki-reference-app`、Root Reactor、build-supportのConsumer／harness／seed、frontend sourceは正式Framework artifact候補に含めない。Referenceの同梱SPAもFramework JARではなくReference-ownedである。

現座標は可変`0.1.0-SNAPSHOT`。E1で、固定version・配布repository・取得方法・全対象artifactのchecksumとsource対応・更新方法を確定する。今回JARを生成・取得・配布したものではなく、14座標の現物や同一build由来checksumを検証したという意味ではない。

### 0.3 一覧確立までの残判断

| 担当タスク | 今回の整理から次へ渡すもの・出口 |
| --- | --- |
| D2-01〜03 | Java21 target／build・Java21／25 runtimeと、VT既定無効・opt-in条件を区別。CIのJava21 VT記載とJava25有効化条件の整合、比較・実CIを根拠に適用条件を確定 |
| D1S-01〜03 | 構造化log／requestId／healthの現基盤から、実trace／exporter／sink・運用診断をどこまで提供するか、依存・設定・sampling・資源・非露出・運用責任を確定 |
| S1-T1〜3／B1-08／C2-03 | 既存限定結果から保証範囲・DoD成立／未達を整理。不足は有限追試・利用制限・対象外・後続化を区別。追加機能を抑制 |
| E1＋R-05／06・R-09・AD | Customer Entity scan、対応するFlyway構成、独立Consumer／受領側の取得・build・起動・診断と、提供版・配布／更新方法を確定 |
| D3 | frontend／backend配置、Java containerと実ECSの成立範囲を段階別に確認し、E1の提供版・対応表・案内へ反映 |

今回の一覧整理は既存source・文書の静的照合であり、testや環境適合を再検証していない。正確な対応version・support状態は[Baseline対応表](../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md)とE1の整合対象とし、現行のJava21 target／build・Boot4.1.1を変更しない。

## 1. 提供する機能と公開された利用方法

### 1.1 Framework機能

以下は全て**初期標準の基本候補**。既存実装・公開契約と受入済み範囲を出発点に、E1で提供版への収録・適用条件を固定する。全Starterを一括導入する前提ではない。

| 機能 | 主な成果物 | 既存機能・根拠 | 適用条件・案件側責任／残確認 |
| --- | --- | --- | --- |
| Build・依存基準 | `koiki-parent`、`koiki-dependencies-bom` | Java21 target／build、依存version・品質設定。[Baseline](../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md) | Customer POM・必要依存・CIは案件所有。Java21／25の過去互換証拠とD2のVT判断は別。固定提供版・現断面の互換性確認はE1へ |
| 構造・品質の検査 | `koiki-architecture-contract`、`koiki-archunit-rules`、Parent | module宣言・Tier・依存方向・internal参照・null安全性。[規約ガイド](architecture-rules-developer-guide.md)、[R0限定受入](../architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md) | architecture testからRule APIを明示実行。検査対象・module Level宣言・業務testは案件所有。Level2を静的に許容するAPIは配信・復旧のruntime保証ではない。MyBatis規約は対象外 |
| REST API基盤 | `koiki-starter-api` | Servlet MVC／Validation／Jackson3、ProblemDetail、path API versioning、Spring標準Resilience有効化。[Starter説明](../../koiki-starters/koiki-starter-api/README.md)、[R-01](../architecture/validation/phase4-r01-exception-boundary-results-20261020.md) | endpoint・DTO・業務例外・認可は案件所有。API／MVC共存のversion設定、外部clientとResilienceの適用条件は§1.3 |
| 永続化 | `koiki-starter-data`、`koiki-starter-data-jpa` | 同一DataSourceのFramework先行／Customer別履歴、JPA・OSIV既定無効。[CP4](../architecture/validation/phase1b-cp4-data-runtime.md)、[Data説明](../../koiki-starters/koiki-starter-data/README.md) | 検証DB baselineはPostgreSQL。業務Entity／Repository／transaction／migrationとdriverは案件所有。Customer Entity scanとFlyway採用構成はR-05／06でE1へ接続 |
| 認証・認可の基盤 | `koiki-starter-security` | default deny、CSRF／Header、method security、OIDC Client／Bearer Resource Server依存と構成境界。[Security説明](../../koiki-starters/koiki-starter-security/README.md)、[Phase2受入](../architecture/validation/phase2-gate-c-c2-remote-evidence.md) | route／chain・認証方式・IdP・Role／Permission・業務認可は案件所有。実IdP・案件SSOの成立を意味しない |
| Identity | `koiki-starter-identity` | 公開Identity契約、user／Role／Permission・external link、local認証・管理操作。[契約](../architecture/validation/phase2-p2-b2-contract-review.md)、[構成条件](../../koiki-starters/koiki-starter-identity/README.md) | local認証は明示opt-inとSOURCE保護条件が必要。管理BeanはAudit等の必須依存が揃う時だけ登録。provisioning UI・reset配送・業務属性は含めない |
| Session保存 | `koiki-starter-session-jdbc` | JDBC保存・全Session失効・logout・期限切れcleanup。[契約](../architecture/validation/phase2-p2-b3-contract-review.md)、[構成境界](../../koiki-starters/koiki-starter-session-jdbc/README.md) | Session利用applicationだけ導入。Framework migration・固定table／cleanup条件を守る。独自CookieSerializerの設定反映は案件側で確認。[R-02](../architecture/validation/phase4-r02-cookie-logout-results-20261021.md)はReferenceの限定訂正 |
| Audit | `koiki-starter-audit` | Business Auditは既存transaction内、Security Auditは独立transaction、DB rowが正本。[契約](../architecture/validation/phase2-p2-b1-contract-review.md) | actor／業務操作・記録内容・保持・調査は案件側。log／traceの代替ではなく、秘密・生email等を記録しない |
| 観測の基礎 | `koiki-starter-observability` | 構造化log・検証済みrequestId・MDC復元、TaskDecorator・Actuator基本health。[CP5](../architecture/validation/phase1b-cp5-observability.md)、[Starter説明](../../koiki-starters/koiki-starter-observability/README.md) | 自動提供を実trace／exporter／cloud backendまで広げない。公開endpoint既定はhealthのみ。readiness依存・metric収集／sink・閾値・運用は案件側。拡充判断はD1へ |
| サーバー側UI | `koiki-starter-web-mvc` | Thymeleaf／Validation依存、共通resource・選択的HTMXの基盤。[Starter説明](../../koiki-starters/koiki-starter-web-mvc/README.md)、[Phase3受入](../architecture/validation/phase3-gate-c-final-acceptance.md) | Security／Session・業務routeを自動構成しない。画面・Form・View DTO・theme・UXとHTMX操作は案件所有 |
| test支援 | `koiki-testing` | Spring Boot Testcontainers／PostgreSQL Testcontainers依存を揃える。[成果物説明](../../koiki-testing/README.md) | test用途。規約検査は別artifact。案件test戦略・fixtureは案件所有。Reference seed・user・migration・browser harnessを配布しない |

依存とSecurityの具体的な選択は[Phase 2 Developer Journey](phase2-developer-journey.md)、検査の導入と検出範囲は[Architecture Rules説明](architecture-rules-developer-guide.md)を参照する。BOMに管理versionがあることは、そのlibraryや構成の採用承認を意味しない。

Customerは別RepositoryでMaven成果物を利用する。FrameworkのRoot Reactor、source相対参照、`internal`実装への依存を導入方法にしない。公開された拡張点の条件を確認し、DI可能という理由だけで差替契約があると解釈しない。

### 1.2 Reference・検証資材

| 資材 | 提供する判断材料 | 利用上の境界 |
| --- | --- | --- |
| [Reference Application](../reference/README.md) | MVC／REST、認証、永続化、Audit等をつなぐ利用例 | 業務class・DTO・migration・seedをCustomerの正本としてそのまま採用しない |
| [Runtime Consumer](../../build-support/runtime-foundation-consumer/README.md) | Root外Consumerとnon-Web maintenance process等の検証例 | 完成済みbatch製品や案件向け運用基盤ではない |
| [Security Consumer](../../build-support/security-foundation-consumer/README.md) | Security接続の検証例 | test user・key・route等を本番資材へ転用しない |
| S1非同期Reference・限定Evidence | 正常非同期経路、失敗分離、選択復旧・consume接続の成立範囲 | 共通非同期サービスの正式配布や包括的な復旧保証ではない |
| B1 Session SPA＋MVC Reference | 最小React・Session API・同一origin同梱、同じUse CaseのMVC／SPA接続。[設計](phase4-b1-session-spa-mvc-design-20261020.md)、[選定browser結果受入](../architecture/validation/phase4-b1-08-local-acceptance-input-20261022.md) | 実装・選定検証済み、自宅PCの選定browser結果はOwner受入済み。B1／DoD4-8・4-9全体受入は別。案件向けfrontend製品・汎用SPA Starterではない |
| C2 A外部照会Reference | Session権限付き参照GET、loopback模擬provider、timeout・503限定retry・同時実行枠2。[限定結果受入](../architecture/validation/phase4-c2-a-implementation-results-20261022.md#7-aの限定検証結果に対するowner受入承認2026-10-23) | 新規29 PASS／1 SKIP。接続timeout等は未実証、root分割結果を保持。実provider・副作用ありB・汎用HTTP Adapterは未提供。C2／DoD4-7全体は未認定 |

Reference、Consumer、Toolingは正式Framework artifactと区別して案内する。参照可能であることと、Customer向けの配布・support対象であることは別に表示する。

### 1.3 条件付き候補・判断待ち

| 機能・構成 | 実装／証拠の現在地 | 提供範囲を確定する条件・担当タスク |
| --- | --- | --- |
| APIとMVC／Session SPAの併存 | API versioningの既定設定が全経路へ及ぶ点をReference側設定で扱い、B1で選定経路を実証 | Customerは非version経路・matcher・CSRFを明示設計。B1限定結果を任意構成へ一般化しない。B1-08／R-04の保証範囲判断 |
| 外部HTTPとResilience | API StarterのSpring標準Resilienceとretry既定値は既存。C2でconnect200ms／read300msの低優先度既定値とmetadataを追加。Reference clientが明示採用 | 任意client全体へtimeoutを自動適用しない。retry対象・層・上限・同時実行枠・外部副作用は案件設計。接続timeout未実証、overrideの限界はC2結果を保持し第3段階で評価 |
| local認証／OIDC／Bearerの選択 | Security／Identity基盤と非配布fixtureの既存Evidenceあり | 実IdP・credential・claim／authority mapping・Session／Bearer経路分離は案件所有。方式選択は[認証構成ガイド](frontend-authentication-profile-guide.md)、実案件結合はB2等へ |
| Virtual Threads | Java21生成ReferenceをJava25.0.5でVT有効／無効実行する同期Webの限定検証済み（43論理checkpoint、Owner結果確認済み）。既定無効を維持 | [D2-02結果・制限§10〜11](../architecture/validation/phase4-d2-02-java25-virtual-threads-results.md#10-最終の成立範囲計測結果)。観測側は実Observabilityを外付け。早期拒否の相関欠落・DB逼迫時403、別executor・将来Java版・本番SLOは条件として残す。[適用ガイド](virtual-threads-adoption-guide.md)と[D2-03](phase4-d2-03-ci-and-adoption-guide-20261026.md)を整備し、CI専用経路のローカル6構成／43項目成立。実CI／DoD4-11・正式提供保証はD2-03／E1で判断 |
| 同期OpenTelemetry・運用観測 | 現Starterは基礎log／相関／health。実trace／exporter／sinkの確立は後続 | D1で提供境界・依存／設定・診断と収集先障害を検証。sink運用とFramework責任を分離 |
| 非同期利用・module Level2 | R0のmodule Level選択APIとReferenceの限定通知／復旧材料は存在。業務処理全体・運用保証は別 | [S1現品質](phase4-s1-dod-integrated-status-owner-review-20261016.md)の未達と人手HOLD条件を維持。第3段階で適用範囲と残アクションを判断。案件への一律導入はしない |

## 2. 検証済みの範囲・制約・未提供事項

### 2.1 証拠の読み方

Phase 1／2の基盤とPhase 3のReferenceは、それぞれの受入済み範囲を持つ。根拠は[Baseline Compatibility](../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md)、[Phase 3実行計画](KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md)と各検証記録へ追跡する。

| 証拠 | 判断できること | 別途確認が必要なこと |
| --- | --- | --- |
| Frameworkのtest・Consumer検証 | 対象source・artifact・環境での契約成立 | 案件の依存構成、実IdP、実DB・cloud設定での成立 |
| Referenceの業務・browser検証 | 実装例の選択経路と接続の成立 | Customer固有の画面、API、業務ルール、性能 |
| CI成功 | 当該commitに対する実行jobの成功 | skip／対象外の機能、実運用、本番受入 |
| 限定実証のOwner受入 | 記録された条件・ケースの受入 | 網羅性、別source／artifact、他の実行条件への一般化 |

過去のPASSは各記録のsource・artifact・実行時点に帰属する。新しいtagや文書の作成で過去検証の対象を置き換えない。今回の文書作成では実行検証・最新脆弱性評価・ライセンス評価を追加しておらず、それらの合格を認定しない。

### 2.2 非同期の限定成果

非同期の成果と未達は[S1現品質・残課題](phase4-s1-dod-integrated-status-owner-review-20261016.md)に集約されている。正常通知・失敗分離等の限定結果を紹介できる一方、FAILED完遂、一般的な二重送信防止、正式運用入口、監視・保持・追跡の統合実証等には未達を残す。

不明な一件は人が照合し、担当付きHOLDとする方針を維持する。ただし、この方針だけでは実担当・記録先・停止維持・実providerの受理照合が成立したことにはならない。非同期を利用しない構成に、S1全体の完遂を一律の提供条件として課さない。追加整備は具体的な必要が判明した時に限定して判断する。

### 2.3 完成済みの提供物に含めないもの

| 対象 | 現時点の扱い |
| --- | --- |
| Next.js／BFF、案件向け完成済みfrontend | 未提供。B1の最小Session SPA実装・限定実証は§1.2で別に案内し、未実装とは扱わない |
| 汎用batch運用一式 | non-Webの既存材料と、単一実行・停止後再開等の後続課題を区別する |
| 実provider用の外部API共通Adapter・副作用あり操作B | C2 Aの限定成果・HTTP既定値と区別。Bは運用機能確定／バリエーション必要性発生時に後続化 |
| File／Object Storage、Container／ECS | 完成済み標準には含めない。C4は要件具体化時、D3は承認済み優先第5段階でlocal／実ECSを別に実証 |
| 利用先のmanaged DB・cloud環境全体への適合保証 | 既存PostgreSQL検証とは別に環境適合を確認する。AWS／Aurora PostgreSQLもこの確認対象の一例であり、本書が指定する必須環境ではない |
| MyBatis、Oracle、SAML等 | trigger・方式選択・個別判断の境界を維持する |
| Project Template、正式Upgrade Guide、一般提供repository、support付きrelease | 現断面で確定済みの提供物とはしない |

後続テーマの状態・着手順は[見直し推進計画§9](phase4-review-integrated-task-plan-20261020.md#9-初期標準確立に向けた見直し推進計画owner-approved2026-10-23)を参照する。本書から提供時期を約束しない。MyBatis・SAML／EDGEは現時点の必要性評価が低いが、当初成果物の廃止判断ではなく、必要入力・再開条件を保持する。

## 3. 構成別の利用候補とサジェスト

### 3.1 構成選択の入口

以下は一般的な構成ごとの選択材料であり、特定のfrontend製品・認証方式・配置先を必須にするものではない。各構成の提供・検証状態は§2と併せて読む。認証方式の詳細は[UI／Authentication Profile Selection Guide](frontend-authentication-profile-guide.md)を参照する。

| 構成・用途 | 利用候補 | 開発チームへのサジェスト |
| --- | --- | --- |
| BFF＋KOIKI REST（Next.js／Node.js等） | API、Security、必要なData／JPA、Audit、Observability | BFFとAPI間のcredential、actor、認可、logout、障害時の責任を先に分ける。Bearer Resource Serverは接続候補として、採用する認証契約との適合を確認する |
| KOIKI単体MVC | Web MVC、Security／Identity／Session、必要な永続化機能 | 既存Referenceを利用例として参照し、Customerの画面・権限を設計する |
| Same-origin SPA＋Session API | API、Security／Session等とB1の最小React Reference | 選定実装・検証とbrowser結果受入は§1.2。CSRF・route・Cookie条件を案件構成へ照合し、B1／DoD全体受入とは区別する |
| non-Web処理・batch | 既存non-Web Consumer、Data／JPA等 | 起動可能性と、業務jobの再実行・二重起動防止・途中再開を分けて確認する |
| 外部システム連携 | API／Data等の既存基盤とCustomer-owned Adapter | 接続方式・整合性・失敗時の意味を案件側で判断する。公開契約の利用方法や不足の切分けはFramework側へ相談できる |
| 非同期が必要になった場合 | S1 Referenceと限定Evidence | 通常処理との境界、失敗許容、照合・HOLDの条件を確認して採用範囲を判断する |

既存の認証構成ガイドは、KOIKIが所有する業務処理の認可・状態遷移・DB更新をApplication／Domainへ配置する境界を定めている。BFFのUI向け集約・変換等と、この境界を区別する。BFFに処理を配置できることを、KOIKI所有の更新ルールの複製や認可迂回の許可と解釈しない。異なる責務配置が必要な場合は、対象となる契約差分を確認する。

### 3.2 処理・環境の選択に応じた確認事項

以下は、Web UIからDB更新を行うシステムや、Web UI操作等のイベントを契機にDB更新・テーブル同期を行うシステムを含め、利用する処理・環境に応じて参照する表である。各行は独立した選択肢であり、組み合わせて一つの標準構成や特定案件の採用構成を定義するものではない。未選択の方式をFramework側で補完せず、開発チームが必要に応じて判断する。

| 処理・環境の選択肢 | 本書から案内すること | 決定を所有する側 |
| --- | --- | --- |
| 通常のWeb UIからDB更新を行うシステム | REST・永続化・Auditと構造検査の既存機能 | 業務要件・実装は案件開発チーム |
| UIとbackendの間にBFFを配置する場合 | §3.1のBFF構成と認証構成ガイド | frontend・処理配置は案件開発チーム。Framework契約変更は別review |
| 外部IdPによるSSOを利用する場合 | 認証選択肢と責任境界。IdP・連携方式に対応する既存契約と未提供部分を確認する | 案件／認証基盤担当 |
| managed DB・cloud環境へ配置する場合 | DB基盤の利用条件と実環境確認の区別 | 案件／platform担当 |
| batchでDB反映を行う場合 | non-Webの既存材料とbatch残課題 | 案件開発チーム／運用担当 |
| Web UI操作等のイベントを契機にDB更新・テーブル同期を行う場合 | 起動契機・処理対象・接続方式を選ぶための既存基盤と制約。イベント起点だけで非同期方式の採用とはせず、汎用テーブル同期機能を提供済みとも扱わない | 案件開発チーム |
| 非同期を利用しない場合 | 機能の存在と制約を紹介し、利用構成への組込みを必須にしない | 案件開発チーム |

方式が未定の場合は、利用側の選択事項として保留する。業務詳細のヒアリングや方式確定をFramework機能の説明・整備の前提にはせず、公開契約との接点について選択肢と判断材料を提示する。

## 4. Framework側と案件側の責任分担

| 対象 | Framework提供側 | 案件開発チーム／platform・運用側 |
| --- | --- | --- |
| 機能・品質 | 共通契約、既定動作、互換性、検証範囲、既知制約を説明する | 自案件に必要な機能を選び、業務・結合・性能を検証する |
| 設計 | 利用方法・選択肢・適用条件を提示する | 業務モデル、処理方式、BFF、外部連携を設計する |
| Security | 公開契約と安全な既定を維持する | 実IdP、権限体系、provisioning、credential・secretの管理を行う |
| データ | 永続化・Framework migrationの契約を管理する | 業務schema・移行・データ品質・同期・backup／復元を管理する |
| 運用 | 観測・診断の入口と制限を示す | 配備、監視、担当、障害対応を成立させる。HOLD・照合は利用する処理に応じて整える |
| 不具合 | Framework契約違反を調査し、共通修正の対象を判断する | 利用versionと再現情報を提示し、案件影響と暫定対応を管理する |
| 更新 | 提供artifactと互換性・変更点を明示する | 更新タイミングを選び、利用経路の回帰を確認する |

役割分担を説明したことと、担当者・連絡先・対応期限を合意したことは区別する。support窓口・応答条件は未確定のまま約束しない。案件チームへの送付・説明会・実環境受入は、本書作成だけでは実施済みにならない。

### 開発向け提供判断の残項目

| 当初の確認観点 | 今回の整理 | 次に確定する事項 |
| --- | --- | --- |
| 機能・提供範囲 | 10月19日のガイドはOwner承認済み。10月23日の初期標準一覧は既存根拠を整理した初版 | 今回追記の最終提供判断は未実施。D2／D1／保証範囲判定を反映し、E1で実際の提供版・適用条件と対応付ける |
| 提供成果物 | checkpoint方式を採用し、Maven versionは`0.1.0-SNAPSHOT`を維持 | 対象座標、配布先、取得方法、checkpointタグ・commit・artifact checksumの対応 |
| 受け手の確認 | [実チーム受入worksheet](p4-ar6-actual-team-reception-worksheet.md)を既存入口として利用 | 受領側のbuild／run／代表経路・診断の実施結果。案件全業務の認定とは分ける |
| 支援・更新 | 責任の区分を提示 | 問合せ・修正・更新の運用、担当、既知制約の通知方法 |

10月19日の本文とチェックポイント方式は承認済みである。今回の一覧初版や、4観点すべての提供可判定、tag作成・配布完了は、その過去承認だけでは成立しない。Maven成果物のversionは今回変更せず、E1で初期提供版・管理方式を確定する。未確定事項はOPENのまま担当と次の判断へつなぐ。

## 5. フィードバックと機能拡充の判断原則

相談時には、業務詳細を網羅して提出する代わりに、利用source／artifact、期待する公開契約上の動作、実際の結果、最小再現、影響と必要時期を示す。credential・個人情報・業務機密をFramework側の記録へ持ち込まない。

| 相談の種類 | 最初の扱い |
| --- | --- |
| 公開契約どおりに動かない | Framework不具合候補として再現・影響を確認する |
| 既存機能の使い方が分からない | ガイド・Reference・診断入口の改善で解消できるか確認する |
| 案件固有の業務・連携が必要 | Customer-owned実装と既存拡張点の利用を案内する |
| 共通契約に不足がある | Spring標準での充足、複数用途での共通性、Ownership、互換性、検証・保守負担を審査する |

Frameworkへの昇格は[グランドデザイン](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)§9.2とADRの承認条件に従う。一案件で便利であることや、Referenceに存在することだけで正式Framework機能に昇格させない。新規機能の採否・優先順位・開始範囲は個別判断とし、相談受付を実装や納期の約束にしない。

依存構成の誤選択については[依存制御の論点](dependency-control-issues-20261001.md)を参照する。追加の制御方式は未決定であり、検査PASSを技術採用承認に読み替えない。Framework提供側は、特定案件への追随を目的に機能を増やすのではなく、共通機能の責任と品質を保ちながら、開発チームが選択できる材料を整える。

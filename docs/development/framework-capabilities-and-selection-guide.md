# KOIKI-JavaWeb 提供機能・適用条件・選択ガイド

作成日：2026-10-19。対象読者：KOIKI-JavaWebを利用する開発チームと、その構成・採用を判断する担当者。

**状態：OWNER APPROVED（2026-10-19）。** Ownerはガイド全体の内容と「KOIKI-JavaWeb」の名称を承認した。その後、製品versionの改変を先送りし、開発向けsourceをGitタグで固定するチェックポイント方式への訂正を承認した。本文の承認は維持する。Python版の呼称「KOIKI-FW」と区別するため、本書ではJava版を「KOIKI-JavaWeb」と表記する。この承認は個別機能の新規採用、正式release、実チーム受入または本番利用の承認ではない。

本書は、特定案件に依存しないFramework機能と、その選択に必要な条件を説明する。KOIKI-JavaWeb提供側は、共通機能を独立した提供対象として扱い、公開契約・品質・互換性・利用方法の整備に集中する。案件の業務分析や方式決定を代行せず、開発チームへ選択肢と判断材料を提示する。特定案件の要求を、そのままFramework拡充の前提にしない。

| 識別事項 | 今回の位置付け |
| --- | --- |
| 確認するsource断面 | `8762a71d67fabbd4ab89ccc1f981ae291c249cba` |
| 既存checkpoint | `checkpoint/v0.1.0-phase4-s1-async-boundary-main-20261017` |
| 文書整備branch | `docs/v0.1.5-development-baseline-handoff`（チェックポイント整理の作業branch） |
| 本書の位置付け | 開発向けsource checkpointに対応する提供機能・適用条件・選択ガイド（Owner承認済み） |
| 次のcheckpointタグ名 | `checkpoint-v0.1.5`。方針採用済み、タグは未作成。対象commitは成果固定時に確定する |
| 現在のMaven version | `0.1.0-SNAPSHOT`を維持。今回のチェックポイント整理ではPOM・artifactのversionを変更しない |

`v0.1.5`は0.1系で進んだ開発成果の区切りを示すタグ上の識別名であり、変更量・互換性・品質を数値だけで保証するものではない。当初の製品version改変案は、`v0.1.5`のチェックポイント方式へ訂正した。正式なversion管理・配布・更新方式は後続で整理する。

開発チームには、利用するcheckpoint、取得方法、前checkpointからの変更点・利用上の影響・既知制約を一つの入口から案内する。内部のPhaseや途中のタグを逐一追うことは求めない。成果物を渡す際は、提供側でタグ・commit・artifact checksumの対応を記録する。同じ`0.1.0-SNAPSHOT`という座標だけで提供物の同一性を判定しない。

機能の選択には本書を使い、実際の導入・開発には[引継ぎガイド](application-team-handoff-guide.md)、[開発環境構築手順](application-team-development-environment-guide.md)、[アプリケーション開発者ガイド](application-developer-guide.md)を使う。Public API・property・認証契約の詳細はリンク先の正本に従い、本書で新たな契約を定義しない。

## 1. 提供する機能と公開された利用方法

### 1.1 Framework機能

以下は現断面に存在する機能の選択表である。全Starterの一括導入を勧めるものではなく、正式な配布対象・取得方法の確定は別途行う。

| 機能 | 主な成果物・利用入口 | 利用できること | 開発チームが所有すること |
| --- | --- | --- | --- |
| Build・依存基準 | `koiki-parent`、`koiki-dependencies-bom` | buildと依存versionの基準を揃える | Customer POM、必要依存の選択、CIへの接続 |
| 構造・品質の検査 | `koiki-architecture-contract`、`koiki-archunit-rules`、Parentの品質設定 | module境界・Tier・依存方向等の検査、null安全性の検査に接続する | module宣言、検査対象・実行の維持、業務test |
| REST API基盤 | `koiki-starter-api` | 公開契約に沿ってAPIの入口と共通処理を構成する | endpoint、DTO、Use Case、業務エラーの意味 |
| 永続化 | `koiki-starter-data`、`koiki-starter-data-jpa` | DBアクセス・JPA・migrationの基盤を利用する | Entity、Repository、業務transaction、Customer migration |
| 認証・認可の基盤 | `koiki-starter-security` | default deny、CSRF／Security Header、Session・OIDC Client・Bearer Resource Serverの既存契約を利用する | route、認証方式、IdP接続、Role／Permission、業務認可 |
| Identity | `koiki-starter-identity` | 公開されたIdentity契約を利用する | 利用者のprovisioning、外部属性との対応、業務属性 |
| Session保存 | `koiki-starter-session-jdbc` | Spring Session JDBCの既存契約を利用する | Sessionを所有する境界、環境設定、運用 |
| Audit | `koiki-starter-audit` | Business／Security Auditの既存契約を利用する | 業務操作とactorの対応、必要な記録、保存・調査運用 |
| 観測 | `koiki-starter-observability` | log・metric等の観測基盤を利用する | 実sink、監視閾値、alert、担当、障害診断の運用 |
| サーバー側UI | `koiki-starter-web-mvc` | Thymeleaf HTML主軸のMVC、選択的HTMXの利用基盤 | 画面、Form、View DTO、UX |
| test支援 | `koiki-testing` | 公開されたtest支援を利用する | 案件のtest戦略、fixture、成功・拒否・障害経路の検証 |

依存とSecurityの具体的な選択は[Phase 2 Developer Journey](phase2-developer-journey.md)、検査の導入と検出範囲は[Architecture Rules説明](architecture-rules-developer-guide.md)を参照する。BOMに管理versionがあることは、そのlibraryや構成の採用承認を意味しない。

Customerは別RepositoryでMaven成果物を利用する。FrameworkのRoot Reactor、source相対参照、`internal`実装への依存を導入方法にしない。公開された拡張点の条件を確認し、DI可能という理由だけで差替契約があると解釈しない。

### 1.2 Reference・検証資材

| 資材 | 提供する判断材料 | 利用上の境界 |
| --- | --- | --- |
| [Reference Application](../reference/README.md) | MVC／REST、認証、永続化、Audit等をつなぐ利用例 | 業務class・DTO・migration・seedをCustomerの正本としてそのまま採用しない |
| [Runtime Consumer](../../build-support/runtime-foundation-consumer/README.md) | Root外Consumerとnon-Web maintenance process等の検証例 | 完成済みbatch製品や案件向け運用基盤ではない |
| [Security Consumer](../../build-support/security-foundation-consumer/README.md) | Security接続の検証例 | test user・key・route等を本番資材へ転用しない |
| S1非同期Reference・限定Evidence | 正常非同期経路、失敗分離、選択復旧・consume接続の成立範囲 | 共通非同期サービスの正式配布や包括的な復旧保証ではない |

Reference、Consumer、Toolingは正式Framework artifactと区別して案内する。参照可能であることと、Customer向けの配布・support対象であることは別に表示する。

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
| Next.js／BFF・Reactのproduction Reference | 未提供。構成選択のガイドと、backendの既存契約を利用する |
| 汎用batch運用一式 | non-Webの既存材料と、単一実行・停止後再開等の後続課題を区別する |
| 外部API耐障害性、File／Object Storage、Container／ECS等の後続機能 | 個別の実装・検証状況に従う。機能名が計画にあるだけで提供済みにしない |
| 利用先のmanaged DB・cloud環境全体への適合保証 | 既存PostgreSQL検証とは別に環境適合を確認する。AWS／Aurora PostgreSQLもこの確認対象の一例であり、本書が指定する必須環境ではない |
| MyBatis、Oracle、SAML等 | trigger・方式選択・個別判断の境界を維持する |
| Project Template、正式Upgrade Guide、一般提供repository、support付きrelease | 現断面で確定済みの提供物とはしない |

後続テーマの状態は[残タスク棚卸し](phase4-continuation-task-inventory-20261016.md)を参照する。本書は機能拡充の着手順や提供時期を約束しない。

## 3. 構成別の利用候補とサジェスト

### 3.1 構成選択の入口

以下は一般的な構成ごとの選択材料であり、特定のfrontend製品・認証方式・配置先を必須にするものではない。各構成の提供・検証状態は§2と併せて読む。認証方式の詳細は[UI／Authentication Profile Selection Guide](frontend-authentication-profile-guide.md)を参照する。

| 構成・用途 | 利用候補 | 開発チームへのサジェスト |
| --- | --- | --- |
| BFF＋KOIKI REST（Next.js／Node.js等） | API、Security、必要なData／JPA、Audit、Observability | BFFとAPI間のcredential、actor、認可、logout、障害時の責任を先に分ける。Bearer Resource Serverは接続候補として、採用する認証契約との適合を確認する |
| KOIKI単体MVC | Web MVC、Security／Identity／Session、必要な永続化機能 | 既存Referenceを利用例として参照し、Customerの画面・権限を設計する |
| Same-origin SPA＋Session API | API、Security／Session等 | CSRFとroute境界を確認する。Session SPA Referenceの後続実証を完了済みと扱わない |
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
| 機能・提供範囲 | 案件非依存の機能・条件・責任を記載した本書全体をOwner承認済み | 文書としての確認は完了。実際の配布範囲は提供成果物の確定と対応付ける |
| 提供成果物 | checkpoint方式を採用し、Maven versionは`0.1.0-SNAPSHOT`を維持 | 対象座標、配布先、取得方法、checkpointタグ・commit・artifact checksumの対応 |
| 受け手の確認 | [実チーム受入worksheet](p4-ar6-actual-team-reception-worksheet.md)を既存入口として利用 | 受領側のbuild／run／代表経路・診断の実施結果。案件全業務の認定とは分ける |
| 支援・更新 | 責任の区分を提示 | 問合せ・修正・更新の運用、担当、既知制約の通知方法 |

本書の本文とチェックポイント方式は承認済みである。4観点すべての提供可判定、tag作成・配布完了は、この文書承認だけでは成立しない。Maven成果物のversion変更は先送りとし、未確定事項はOPENのまま担当と次の判断へつなぐ。

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

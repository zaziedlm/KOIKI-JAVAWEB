# KOIKI-JavaWeb-FW Glossary

**版:** v0.1  
**初稿日:** 2026年8月17日  
**状態:** ACCEPTED  
**Architecture Owner:** Shuichi Kataoka  
**承認日:** 2026年8月17日  
**Decided by:** Shuichi Kataoka

## 1. 目的

本書は、KOIKI-JavaWeb-FWの設計、実装、reviewで用いる語の意味を固定する。
一般的なJava / Spring用語を網羅するのではなく、次を対象とする。

- KOIKI固有の所有権、構造、運用上の用語
- 一般的な用法より狭い、または特定の意味で採用するJava / Spring用語
- KOIKI-PYFW dev/v0.8とJava版の概念対応
- 責務や所有者を曖昧にするため使用しない表現

設計判断の詳細はグランドデザインとADRを正本とし、本書はそれらを上書きしない。

## 2. 記述規則

- 説明文では日本語表記に英語の正式名を併記してよい。package、class、annotation等は実際の識別子を用いる。
- `Framework`、`Reference`、`Customer`、`Walking Skeleton`は、技術layerではなくOwnershipの区分である。
- `Domain`、`Application`、`Adapter`は責務を表す。Spring stereotypeの有無で判定しない。
- `Model`、`Service`、`API`、`Entity`のような多義語は単独で用いず、本書の限定語を付ける。

## 3. Ownershipと成果物

| 用語 | KOIKIでの意味 | 主な使用箇所／注意 |
|---|---|---|
| **KOIKI Framework** | 業務語彙を含まない再利用可能な共通契約。BOM、Starter、共通library、testing support等で配布する | Spring標準で代替できる機能や顧客固有機能を入れない |
| **KOIKI Reference Application** | Frameworkの正しい利用例、integration test対象、release smoke test、AI参照実装 | デモではない。Frameworkの内部実装でもない |
| **Customer Application** | 顧客固有の業務ルール、画面、外部連携、migrationを所有する別repository | KOIKI本体をcopyして改造せず、version付き依存として利用する |
| **Walking Skeleton** | 設計の実装可能性を最小構成で検証する使い捨て実装 | Java classやSQLをFramework / Referenceへ直接昇格させない |
| **Ownership** | 成果物の変更理由と最終責任をFramework / Reference / Customer / Walking Skeletonのいずれかに定めること | package配置や同一repository内の物理的近さとは別の概念 |
| **Framework昇格** | Reference / Customer / Walking Skeleton発の候補を、安定したFramework契約として引き受けること | グランドデザイン§9.2の全7条件とADR承認が必要 |
| **Public API** | 外部consumerとの後方互換義務を負うFramework契約 | REST APIだけを意味しない。Java type、annotation、configuration property等も含む |
| **Architecture Baseline** | Architecture Ownerが承認し、後続Phaseの設計・実装が従う判断集合 | 後続実装の完了を意味しない。証拠が前提を否定した場合は再reviewする |

## 4. モジュールと内部責務

| 用語 | KOIKIでの意味 | 主な配置／注意 |
|---|---|---|
| **業務モジュール** | 業務機能とそのデータ、Use Case、入出力Adapterをまとめる変更単位 | 技術layerをapplication全体に横断配置しない |
| **モジュラーモノリス** | 単一deployableの内部を、業務モジュールと明示的な依存方向で分離するarchitecture | 単にpackageを分けたモノリスではない |
| **Module Boundary** | 他モジュールから参照できる契約と内部実装の境界 | 公開契約は原則として`domain.event`の識別子と値のみを持つイベント。他モジュールのApplication、Domain Model、Repository、Adapterを直接参照しない |
| **Tier 1 SIMPLE** | Domain Model層を持たず、単純な業務判断と処理調整をApplication Use Caseが担う構造 | 永続化modelは振る舞いを持たない。未使用のDomain packageを作らない |
| **Tier 2 RICH** | Domain Modelが不変条件と状態遷移を所有し、Application Use Caseが処理を調整する構造 | Tier 1との差は主に`domain.model`、`domain.service`、`domain.repository`、`domain.gateway` |
| **Inbound Adapter** | REST、MVC、Event、Batch、Messageを受け付け、形式検証、入力変換、応答整形を行う境界 | `adapter.inbound`。業務上の妥当性判断、業務ルール、Repository操作を置かない |
| **Application Use Case** | transaction境界、権限呼出、処理順序、Domain操作、event発行を調整する責務 | `application.usecase`。一般的な「Service層」より責務を限定する |
| **Domain Model** | Tier 2の業務状態、不変条件、状態遷移を表現するobject | `domain.model`。Controller、HTTP DTO、SQLを含めない |
| **Domain Service** | Domain ModelやValue Objectのいずれにも自然に属さないTier 2の業務判断 | `domain.service`。処理手順、DTO変換、Repositoryの薄いwrapperに使わない |
| **Domain Event** | モジュールが公開する、業務上発生した事実を表す不変なJava `record`契約 | `domain.event`。Tier 1 / Tier 2共通。識別子と値のみを持ち、Entityを含めない |
| **Outbound Port** | Tier 2でApplication / Domain側が所有する外部機能の契約 | 差し替えの現実味がある場合に設ける。Tier 1では設けず、未使用Portを先行作成しない |
| **Outbound Adapter** | DB、外部API、File、Messaging、複雑query等の技術実装 | `adapter.outbound`。Use Caseの判断を置かない |
| **Repository** | 永続化対象の取得・保存を表す契約 | Tier 2ではAggregateを扱う`domain.repository`が所有する。Tier 1ではApplication Use CaseがPersistence Modelを扱うSpring Data Repositoryを直接利用し、追加のPortを重ねない |
| **Gateway** | 外部システム連携を業務側から表すPort | Tier 2の`domain.gateway`。HTTP client自体の名称ではない |
| **Query Port** | Tier 2の一覧、検索、帳票、集計でread modelを返すApplication所有の参照契約 | `application.query`。Outbound AdapterがJPA射影、JdbcClientまたはMyBatisで実装する。Tier 1では専用のread modelやPortを先行作成せず、Application DTOと必要最小限の参照経路を用いる |
| **Configuration** | Bean構成とAdapter選択 | `configuration`。業務ルールを置かない |
| **shared-kernel** | 複数モジュールが共有せざるを得ない最小の安定した契約 | 所有者不明の共通置き場として使わない |

## 5. データと変換

| 用語 | KOIKIでの意味 | 主な配置／注意 |
|---|---|---|
| **Aggregate** | Tier 2で、一つのtransactionにより不変条件を保つDomain Modelの一貫性境界 | 複数の識別可能なDomain objectやValue Objectを含められ、単なるtable単位とは限らない |
| **JPA Entity** | JPAが永続化対象として管理するclass | Tier 2兼用方式ではDomain Modelと同一class。Tier 1では振る舞いを持たない永続化model |
| **Persistence Model** | DB schemaとSQL実行の都合を表す永続化用object | Domain Modelと常に同一とは限らない |
| **兼用方式（Shared Domain / Persistence Model）** | Tier 2のDomain ModelとJPA Entityを同一classで表す方式 | Tier 2 + JPAの既定。setterを公開せず、業務methodで状態を変更する |
| **分離方式（Separated Domain / Persistence Model）** | Domain ModelとPersistence Modelを別classにし、Adapterで変換する方式 | グランドデザイン§11.7のトリガに該当する場合のみ。MyBatis採用時は必須 |
| **DTO** | 層や外部境界を越えるための値の形 | HTTP request / response、MVC view model、Application入出力等。Domain ModelやJPA Entityの別名ではない |
| **read model** | 一覧、検索、帳票、集計向けに完全にmaterializeされた参照専用の結果型 | Tier 2では`application.query`所有のJava `record`。Domain Modelではなく、復元経路も持たない |
| **JPA projection** | Entity全体ではなく必要な値だけをquery結果に投影する手段 | KOIKIのread modelではJava `record`へのclass-based projectionに限定する |
| **converter** | Domain ModelとPersistence Modelを相互変換する手書きcomponent | 分離方式の`adapter.outbound.persistence.converter`。read modelとDTOの一般変換器として使わない |
| **mapper** | MyBatisのSQL定義interface | `adapter.outbound.persistence.mapper`。Domain / Persistence変換は`converter`と呼び、mapperと呼ばない |
| **reconstitute** | 分離方式で永続化済み状態からDomain Modelを復元する静的factory method | `converter`以外から呼ばない。業務的な新規生成methodと区別する |
| **materialize** | 永続化またはqueryの結果から、Use Caseで必要なAggregate、DTO、read modelを、境界通過後の追加DB accessなしで利用できる状態へ完全に組み立てること | OSIV無効時のView / API境界より前で完了させる |
| **Migration** | DB schemaと必要な初期dataのversion付き変更 | FlywayでOwnershipごとにlocationとhistoryを分離する |

## 6. 実行境界と連携

| 用語 | KOIKIでの意味 | 注意 |
|---|---|---|
| **UIプロファイル** | API基盤を共通の正本とし、その上で選択できるThymeleaf + HTMXまたはREST API + SPAの提供形態 | 両者は対等であり、優劣や別Frameworkを意味しない |
| **OSIV** | Open Session in View。View描画までJPA persistence contextを延長する仕組み | KOIKIでは無効。DTO / read modelをtransaction内でmaterializeする |
| **同期Domain Event** | listenerの成功が発行元の業務成立条件である場合の同一transaction内連携 | listenerの例外は発行元transactionをrollbackさせる |
| **非同期Domain Event** | 発行元の成立後に実行できる副作用、派生処理、外部I/Oの連携 | Spring Modulith Level 2、耐久性、冪等性、監視、再送を伴うため、正式実装は後続Phaseの設計・検証対象とする |
| **Browser Auth** | MVCや一般的なsame-origin SPAに対するbrowser向け認証profile | HTTP Sessionを第一標準とするが、stateless SPAを禁止しない |
| **API Auth** | 外部API、service間連携、stateless SPAに対する認証profile | OAuth 2.0 Bearer JWTを第一標準とし、issuer、audience、scopeを検証する。Token失効、refresh rotation、Logout等のlifecycleは後続Phaseで設計・検証する |
| **Support Line** | 特定のKOIKI / Spring Boot / Java baselineの組み合わせとそのsupport期間 | 「LTS」と呼ばず、support終了日を実日付で示す |

## 7. KOIKI-PYFW dev/v0.8との概念対応

次表は、グランドデザイン§3で整理済みのKOIKI-PYFW dev/v0.8との対応を基礎とする。
原典の改訂を確認した場合は、グランドデザインと本表を同時にreviewする。

| KOIKI-PYFWの概念 | Java版の概念 | 対応の考え方 |
|---|---|---|
| `framework layer` / `components/libkoiki/` | KOIKI Framework | 再利用可能な共通契約というOwnershipを継承する |
| `reference application layer` / `components/koiki_ref_app/` | KOIKI Reference Application | 正規利用例と検証対象という役割を継承する |
| `downstream/customer-specific` / `apps/` | Customer Application | Ownershipは継承するが、Java版ではKOIKIと物理的に別repositoryとする |
| `API` | Inbound Adapter | REST以外にMVC、Event、Batch、Messageがあるため、Java版ではより広い入力境界として扱う |
| `Service` | Application Use Case、Tier 2では加えてDomain Model / Domain Service | 直接的1対1対応ではない。処理調整はApplication、不変条件と状態遷移はDomain Modelへ分ける |
| `Repository` | Tier 1のSpring Data Repository、Tier 2の`domain.repository` | Tierにより契約の所有位置が異なる。MyBatis分離方式ではOutbound Adapterが実装する |
| `Model` | Tier 1のPersistence Model、Tier 2のDomain Model、DTO、read model | 最も単純対応させてはならない。責務に応じて用語と配置を選ぶ |
| `API → Service → Repository → Model` | `Inbound Adapter → Application Use Case → Domain / Port ← Outbound Adapter` | Tier 1は学習コストを抑えた近い構造、Tier 2は業務ルールをDomain側へ移して依存方向を明確化する |

## 8. 使用しない、または限定する表現

本表は、KOIKIの設計・実装で責務名を付ける場合に適用する。Java / Spring / MyBatis等の
正式な型名・機能名、旧構造の引用、および過去の検証証拠に現れる表記まで禁止するものではない。

| 表現 | 問題 | 使用する表現 |
|---|---|---|
| `Service層` | 処理調整と業務ルールの所有者が曖昧 | Application Use Case、Domain Model、Domain Serviceのいずれか |
| `Model` | Domain、Persistence、DTO、Viewの区別がない | Domain Model、Persistence Model、JPA Entity、DTO、MVC view model、read model |
| `Entity` | Domain上の識別子を持つobjectとJPA Entityが混同する | JPA Entity、Domain Model、Persistence Model |
| `Mapper` | MyBatis Mapperとobject変換が混同する | SQL定義はMyBatis Mapper、Domain / Persistence変換はconverter |
| `DAO` | Repository、MyBatis Mapper、query実装のどれかが不明 | Repository、MyBatis Mapper、Query Port / Adapter実装 |
| `API` | REST endpoint、Java Public API、Application Use Caseが混同する | REST API、Public API、Application Use Case |
| `common`、`util` | Ownership不明の共通置き場になる | 所有モジュールと具体的な責務名。shared-kernelは最小限 |
| `Framework化` | 昇格条件と互換義務が表現されない | Framework昇格 |
| `KOIKI LTS` | OSSと商用延長supportの条件が曖昧 | KOIKI versionとsupport終了日 |
| `readmodel` | 説明文で読みづらく、read modelを独立した技術layerや旧package配置として扱う誤解を招く | 説明文では`read model`。Java typeは具体的な業務名を使う |

## 9. 更新規則

- 新しいKOIKI固有語を公開文書、Skill、Public APIに追加する場合は、本書への追加要否をreviewする。
- 本書とグランドデザインが競合する場合は、グランドデザインとADRを優先し、用語集を更新する。
- 用語の意味変更がArchitecture判断を伴う場合は、先にグランドデザインまたはADRを更新・承認し、その結果を本書へ反映する。本書だけでArchitecture判断を変更しない。
- 用語の意味を変更する場合は、影響するSkill、ArchUnit message、Reference Applicationを同時に確認する。
- Architecture Ownerがreviewし、対象、証拠、判定、判定日、Decided byをGovernanceに従って記録してからDoD 0-2をCOMPLETEとする。

## 10. 参照

- [KOIKI-JavaWeb-FW グランドデザイン v0.2](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)
- [ADR Register](../architecture/adr/README.md)
- [Architecture Governance](../architecture/governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md)
- [Phase 0 DoD Closeout](../architecture/KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md)
- [KOIKI Project Overview Skill](../agent/skills/koiki-project-overview/SKILL.md)
- [KOIKI Business Feature Work Skill](../agent/skills/koiki-business-feature-work/SKILL.md)
- 外部Repositoryの原典: KOIKI-PYFW dev/v0.8 `docs/design_kkfw_0.8.0.md`
- 外部Repositoryの原典: KOIKI-PYFW dev/v0.8 `docs/agent/architecture.md`

## 11. 承認記録

| 項目 | 内容 |
|---|---|
| 対象 | KOIKI-JavaWeb-FW Glossary v0.1、およびPhase 0 DoD 0-2 |
| Decision | ACCEPTED / COMPLETE |
| Evidence | グランドデザインv0.2、全43件が承認済みのADR Register、§3〜§10のOwner Review、`koiki-archunit-rules`のテスト13件成功 |
| Rationale | KOIKI固有語、Java / Springでの採用意味、KOIKI-PYFWとの概念対応、および非推奨表現が、承認済みArchitectureと実装証拠に整合するため |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月17日 |
| Revisit trigger | Architecture用語の意味変更、公開文書・Skill・Public APIへの新語追加、KOIKI-PYFW原典の改訂、または実装証拠が本書の前提を否定した場合 |

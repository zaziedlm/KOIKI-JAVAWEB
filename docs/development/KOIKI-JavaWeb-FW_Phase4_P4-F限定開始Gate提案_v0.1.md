# KOIKI-JavaWeb-FW Phase 4 P4-F限定開始Gate提案 v0.1

**状態:** DRAFT / P4-PL2 INPUT。Gate P4-Fの設置・通過、Phase 4 production開始は未承認  
**作成日:** 2026年9月26日  
**判断の起点:** [Phase 4見直し草案 R1〜R7](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#7-architecture-owner-review票)。R6は本提案書の作成だけを承認した。

## 1. 提案の目的と現行Gateとの差分

[P4-AR計画](KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md)は、P4-AR6の実チーム受入、AR-D10の責任分担判断、Gate P4-ARを経て、Phase 4開始を別途判断する。
現時点でP4-AR6の実チーム入力は未取得である。P4-Fは、その入力に依存しないFramework共通の限定範囲について、先行開始を**別Gateで判断する案**である。
現行Gateを黙示的に迂回せず、採用する場合はP4-AR計画と`AGENTS.md`のGate関係を明示的に改訂する。

| 現行 | P4-Fを採用する場合に必要な変更案 |
|---|---|
| Gate P4-ARのacceptance後にPhase 4開始を別判断 | P4-AR6 / AR-D10 / Gate P4-ARは未完了のまま保持し、Framework限定の先行開始だけをGate P4-Fで個別判断する |
| 実案件側の責任分担をPhase 4 production実装より先に確定 | 先行範囲をCustomer設定・環境・API・IdPに依存しない技術契約とReference実証に限定し、実案件責任分担はP4-AR6で後続確定する |
| Phase 4全体の開始と完了を別判断 | P4-F通過をPhase 4全体開始・DoD完了・正式受渡し・remote変更の承認と読み替えない |

この差分が現行のtransition Gateの意図を維持できるかを、P4-PL2後にArchitecture Ownerが判断する。

## 2. 候補範囲とOwnership

| 候補 | 成果物の範囲 | Ownershipと境界 |
|---|---|---|
| P4-A1 Level 2基盤 | event publicationの永続化、失敗状態・再送・パージの技術契約と運用入口。DoD 4-2・4-4・4-5・4-12の前提 | Frameworkが共通契約を判断。保存schema・runtime依存・配置moduleとPublic APIは未決定。Toolingが故障・復旧を検証 |
| P4-A2 通知Reference | `expense`承認からの非同期通知、失敗時の承認非rollback、重複配信時の二重送信防止。DoD 4-1・4-3とA1の統合実証 | Referenceが業務event、通知log、送信Adapter、検証用providerを所有。Customerの通知文面・provider・運用を持ち込まない |
| P4-D1 非同期観測 | FAILED publication件数・滞留、相関IDの伝播、trace / metric / logの照合。DoD 4-4・4-12の観測面 | Framework / Toolingが観測契約を判断。exporter・監視基盤・retention・alertは運用Ownerとの後続判断 |

P4-A1とD1を同時に設計し、A1の契約を確定してからA2を実装する。
DoD 4-2・4-4・4-5・4-12はA1単体でPASSとせず、A1・A2・D1の統合実演で判定する。
P4-C1 accounting、P4-B1 SPA、P4-B2 Customer BFF、実案件IdP / SSO、P4-E1正式受渡しはP4-Fの開始対象外とする。
MyBatis adoption Gate、当初DoD 4-8 / 4-9、P4-AR6とGate P4-ARの未完了状態を維持する。

## 3. 現行baselineと未決定の技術判断

| 領域 | 現行baseline | P4-F前のblocking review |
|---|---|---|
| Module event | [P3-A4](../architecture/validation/phase3-p3-a4-level1-synchronous-event.md)は同期`@EventListener`でcommand整合を実証し、非同期・Level 2を導入していない | 同期veto経路と非同期side effectを分け、transaction境界、event payload、publication保存と復旧を設計する。Rule 28 / 29の変更範囲を確認 |
| Modulith依存 | [BOM](../../koiki-dependencies-bom/pom.xml)はSpring Modulithを管理するが、Level 0 / 1ではruntime Level 2基盤を提供しない | 利用するSpring Modulith機能、依存scope、Reference / Framework配置、migration、障害時の挙動を比較し、選定理由を記録 |
| 観測 | [Observability Starter](../../koiki-starters/koiki-starter-observability/README.md)は構造化log、Servlet `requestId`、TaskDecorator、healthを提供する | HTTP request外のevent / retry / job相関、metric、trace、個人情報非露出、exporterの責任境界を定義 |
| 単一実行 | [Phase 1b CP8](../architecture/validation/phase1b-cp8-single-execution.md)はCustomer-like Consumerで単一実行を実証し、Framework Java APIは追加していない | publicationパージへ契約を再利用できるか、Batchとの共通実行基盤が必要かを判定。CP8 fixtureを正式artifactへ自動昇格しない |

[Data Starter](../../koiki-starters/koiki-starter-data/README.md)はFramework migrationを`db/migration/koiki`、Customer migrationを`db/migration/customer`に分ける。
publication tableをFramework所有にするかReference側へ置くかでmigrationの配置と配布義務が変わる。
[現行Rule 28](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java)はtransactional listenerを拒否し、Rule 29は未実装である。
Level 2を採用する際は、Rule 28の適用条件とRule 29の導入を同じblocking reviewで扱う。

### 3.1 PL2で比較する実装方式

[Spring Modulith 2.1のEvent Publication Registry](https://docs.spring.io/spring-modulith/reference/events.html)はtransactional listener向けpublicationを元の業務transactionで記録し、未完了・失敗・再送・パージのAPIを提供する。
JDBC / JPAの永続化方式、完了時のUPDATE / DELETE / ARCHIVE、stale publicationの扱いを選択する必要がある。
JDBCを選ぶ場合は自動schema初期化と既存Flyway migrationの責任が重複しないようにする。

| 論点 | 比較候補 | 決定に必要な検証 |
|---|---|---|
| publication store | Spring Modulith JDBCまたはJPA | 既存JPA transactionとの整合、schema / Flyway所有、restart時の未処理検出、dependency tree |
| Framework提供形態 | ApplicationでSpring標準を構成しKOIKIは規約・検証を提供、またはKOIKI Starter / Java APIを追加 | 二つ目の正式artifactが本当に必要かを判断。新規Public APIとmigrationを先行固定しない |
| listener | `@ApplicationModuleListener`等のModulith標準とReference内Adapter | 元の承認transactionから外部I/Oを分離し、失敗・process kill後もpublicationを失わない。同期veto用`@EventListener`は維持 |
| 再送 | `FailedEventPublications` / `IncompleteEventPublications`と明示運用入口 | PUBLISHED / PROCESSINGで停止したrecordのstale判定、試行回数、同時再送、operator権限 |
| 完了・パージ | UPDATE維持＋retention / purge、DELETE、ARCHIVE | DoD 4-5の「完了済みpublicationパージ」を実演でき、失敗・未処理を削除しない方式を選ぶ。DELETEを選ぶならDoD解釈のOwner判断が必要 |
| メール冪等性 | event IDと通知送信境界の永続key、providerの冪等機能またはReference stub | 同一event再配信に加え、外部送信成功直後のprocess killを検証。provider側の重複抑止ができない場合はDoD 4-3の成立範囲を明示判断 |
| 観測 | Spring Modulithの標準span / event計数＋別途FAILED件数・滞留metric | [Modulith Observability](https://docs.spring.io/spring-modulith/reference/production-ready.html)の発行counterだけでDoD 4-4のFAILED gaugeを満たしたとしない。event / retry / jobを跨ぐ相関を検証 |

[Spring BootのObservability契約](https://docs.spring.io/spring-boot/reference/actuator/observability.html)では、asyncのcontext propagationにexecutor構成または明示opt-inが必要になる。
既存TaskDecoratorの実効範囲を検証し、trace / logで同じ業務操作を追跡できるかを測る。
OpenTelemetry exporterの配布既定はこの段階で固定しない。

## 4. Gate P4-Fを審査できる状態にする作業

| 順 | 提出するEvidence | 完了の判断点 |
|---|---|---|
| F-1 | clean source identity、Phase 3 accepted baseline、P4-AR1〜AR5とFramework側P4-AR6準備の差分 | 実チーム入力とFrameworkで再現した結果を混同しない |
| F-2 | A1 / A2 / D1の責任分担、module・dependency・migration・Public API / ArchUnit Rule影響、代替案と選定理由 | 各変更をFramework / Reference / Toolingへ配置できる |
| F-3 | DoD 4-1〜4-5・4-12の実演手順。process kill、再起動後配信、重複配信、外部送信失敗、FAILED観測、再送、パージ、相関を含む | 成功・失敗・復旧をpackage済みReferenceと独立した検証手段で再現できる計画がある |
| F-4 | commit pointとrollback方針、module別見積、環境・CI費用、実施Owner、Evidence保管先 | 工数をDoD実演単位へ追跡できる。見積が未記入ならGateを開催しない |
| F-5 | P4-AR計画・`AGENTS.md`の改訂差分、P4-AR6 / AR-D10 / Gate P4-ARとの関係、停止条件 | 現行承認との衝突と残す義務が見える |

F-3の実演単位は次のように分ける。失敗を注入する仕組みはToolingまたは非配布fixtureに置き、正式Framework artifactへ含めない。

| DoD | 実演する操作と観測点 | Evidence Owner候補 |
|---|---|---|
| 4-1 | 承認をcommitし、通知を非同期処理する。送信失敗でも承認状態・Business Auditが残る | Reference + Tooling |
| 4-2 | publication記録後・listener処理前など停止位置を制御し、process kill / 再起動後に未処理eventを配信する | Framework契約 + Tooling |
| 4-3 | 同一eventを二度配送し、外部送信境界で通知が一回だけになる。送信成功直後の停止窓も検査する | Reference + Tooling。providerの冪等機能がない場合はOwner判断 |
| 4-4 | FAILED件数と滞留時間を観測し、権限を持つ運用者の再送手順で状態が回復する | Framework観測契約 + Tooling |
| 4-5 | 単一実行のパージを起動し、completedだけがretention条件に従って消え、未処理・FAILEDは残る | Framework運用契約 + Tooling |
| 4-12 | 承認request、非同期listener、再送時のlog / traceで相関を辿り、別requestへ相関値が漏れない | Framework観測契約 + Tooling |

P4-PL2でF-2〜F-5を具体化する。[PL2 P4-F判定資料 v0.1](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md)に
配置候補、DoD実演とfixture Evidenceの差、既存見積の仮配賦、停止点を記録した。
store・migration方式、Phase共通の再見積、運用Ownerなどは未確定であり、Gate P4-Fは判定不能である。

### 4.1 暫定規模と再見積もり

[Phase 0で承認された見積](../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md#7-phase-4--enterprise-integration)は、P4-Fの中心となるDoD 4-1〜4-5・4-12を合計**47〜80標準人日**とした。
これは各DoDを実演可能にする直接工数の過去の概算であり、A1 / A2 / D1別の見積ではない。
Phase共通25〜40標準人日はSAML、Storage、ECS等も含むため、全額をP4-Fへ足さない。

| 見積の再校正項目 | PL2で埋める内容 |
|---|---|
| A1 / A2 / D1への帰属 | DoD 4-1〜4-5・4-12と共有作業を二重計上せず、設計・実装・test・実演・文書へ分解 |
| Phase共通からの配賦 | Modulith採用review、migration、Rule 28 / 29、CI、運用文書のP4-F分だけを計上 |
| 検証環境 | PostgreSQL、package済みReference、mail stub、process kill / restart、観測sinkの必要資源とCI費用 |
| 不確実性 | provider冪等性、publication復旧・schema方式、非同期相関の試験結果を受けてrangeを更新 |

Ownerの判定資料には、再校正後の標準人日、AI支援Owner稼働日、外部待ち時間を別々に記録する。
上記47〜80をそのままGate P4-Fの実行予算として承認しない。

### 4.2 PL2の非配布検証案（実施承認済み）

**Owner判断:** APPROVED / 2026-09-26。本節のTooling fixtureとPostgreSQL検証、Evidence記録まで。
正式Framework / Reference実装、配布、remote操作、Gate P4-Fの採用・production開始は含まない。

Spring Modulithの方式選定とDoD実演計画を確かめるため、`build-support/phase4-level2-verification/`に
Root Reactor・正式release unitから独立したTooling-owned fixtureを置く案とする。
これはP4-A1 / A2 / D1のproduction実装でも、Reference業務codeの追加でもない。

| 検証項目 | 具体的な確認 | 成果物 |
|---|---|---|
| publication | JDBC / JPA候補のtransaction連動、Flyway管理schema、completion / stale状態、再起動時の未処理復旧をPostgreSQLで比較 | 選定表、dependency tree、migration所有案 |
| 冪等性 | 同一eventの再配送と、stub送信直後のprocess killを注入。provider側の冪等keyがある場合・ない場合の限界を示す | DoD 4-3の成立条件と未解決の外部依存 |
| 観測・運用 | FAILED件数・滞留時間、明示再送、completedパージ、event / retry / jobの相関を確認 | DoD 4-2・4-4・4-5・4-12の実演手順案 |
| 規約 | Rule 28を維持した現行baselineと、Level 2だけを許す変更案・Rule 29候補を比較 | ArchUnit変更案とnegative fixture案。正式Rules変更はしない |

結果は`docs/architecture/validation/phase4-pl2-level2-verification.md`へ記録する。
実Customer Repository、実IdP、実mail provider、正式Framework Public API / migration / Starter、remote操作、snapshot publishは対象外とする。
process、container、port、一時credentialを検証後にcleanupし、fixtureをFramework成果物へ自動昇格しない。
本検証の結果をF-2〜F-4へ入力し、Gate P4-Fの採否は改めて判断する。

**実施preflightと進捗（2026-09-26時点の記録）:** Maven Wrapper 3.9.16 / JDK 21.0.12.1を確認した。
通常のsandbox権限ではDocker named pipeと`~/.m2`への書込みを拒否されるが、権限付き実行で
Rancher Desktop Engine 29.5.3とMaven依存取得が成立した。`postgres:17-alpine`のTestcontainersで
JDBC / JPAそれぞれSurefire 7件、Failsafe 6件がPASSした。再送回数のfilter候補とFAILEDのpublication年齢も検証した。
複数JVMで同一publicationのlistenerが同時実行される競合を確認した。CP8型advisory lockによる専用復旧JVM同士の排他候補は両方式で成立したが、
通常listenerが生存中の明示再送とlock接続だけの喪失でも重複実行を再現した。安全な対象選別・fail-stop / fencingを未解決のblocking issueとする。
FAILED遷移からの滞留時間、非同期相関、Rule 28 / 29、
正式migration所有と方式選定は残す。詳細と未実施項目は
[PL2 Level 2検証Evidence](../architecture/validation/phase4-pl2-level2-verification.md)に記録する。
その後のV1 / V2 / V5の結果と最新の検証件数は同Evidenceを正本とし、
[PL2 P4-F判定資料§5](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#5-次のpl2作業)で継続順序を管理する。

## 5. Gate提案時の判定と停止条件

Gate P4-Fの判定案は`APPROVE LIMITED START`、`REWORK`、`REJECT`のいずれかとし、承認時は対象commit point、Owner、Evidence、停止点を明記する。
Public API、dependency、migration、Starter、Security既定、workflow、remote push / PR / merge、snapshot publishは、それぞれ既存のblocking reviewと個別承認を要する。
実案件要件への依存、P4-AR6で責任分担未決の変更、DoD変更、FrameworkへのCustomer / Reference codeの無審査昇格を検出した場合は、対象packageの開始を停止して再reviewする。

**次の作業:** [見直し草案§7.2](KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md#72-次reviewに必要な資料)と
[PL2 P4-F判定資料](KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md#5-次のpl2作業)に沿って、
未検証事項とF-2〜F-5の未確定部分を埋める。Gate P4-Fの開催・採用はその後のOwner判断とする。

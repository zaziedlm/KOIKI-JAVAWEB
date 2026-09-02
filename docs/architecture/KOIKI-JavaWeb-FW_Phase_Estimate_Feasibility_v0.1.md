# KOIKI-JavaWeb-FW Phase Estimate / Feasibility

**版:** v0.1<br>
**評価日:** 2026年8月19日／2026年8月31日（Oracle scope再校正）<br>
**状態:** ACCEPTED（2026年8月31日scope addendum反映）<br>
**対象:** Phase 1a〜5<br>
**基準Commit:** `8d90ea1`

## 1. 目的

グランドデザイン v0.2 §27.4〜§27.9の各DoDについて、実演可能な証拠を得るまでの規模、
依存関係、主要リスクを見積もり、一人projectとしての実現可能性を判断する。

本書はPhase 0 DoD 0-5の評価証拠である。実装方式を先行確定する文書ではなく、各Phaseの
開始時には依存製品のbaseline、外部環境、実績工数を用いて再見積もりする。

## 2. 見積もり方法

**Owner Review:** ACCEPTED（2026年8月19日、Shuichi Kataoka）
**承認scope:** 標準規模の単位、AI支援実行モデル、Phase別AI支援係数、および再校正方針

### 2.1 標準規模の見積もり単位

- 1標準人日を、設計、実装、test、文書化、CIまたは実演証拠の整備を含む比較用の作業単位とする。
- 各DoDの値は、当該DoDを「実演できる」状態へ進めるために帰属させた概算rangeである。
- 複数DoDが共有するbuild、dependency更新、ADR、Skill、migration等は`Phase共通`へ一度だけ計上する。
- `Phase共通`には、グランドデザイン§27.2の共通DoDに対する更新、検査、証拠整備も含む。
- 外部serviceの契約待ち、Architecture Ownerの承認待ち、release後の観測期間は人日に含めない。
- Walking Skeletonのcodeは流用せず、検証済みの設定、規約実装、判断、test観点だけを再利用する。

| 規模 | 標準人日の目安 |
|---|---:|
| XS | 1〜2人日 |
| S | 3〜5人日 |
| M | 6〜10人日 |
| L | 11〜20人日 |
| XL | 21〜40人日 |

標準人日は、Phase間およびDoD間の技術的な規模を比較し、将来の体制変更後も見積もりを
再利用するために残す。今回の実行日数には、次のAI支援実行モデルを適用する。

### 2.2 AI支援実行モデル

今回の実行体制は、**Javaアーキテクト兼Architecture Owner 1名が、Codex AIエージェントを
常時利用して開発する一人project**とする。他の人間による並行開発は前提にしない。

`AI支援Owner稼働日`は、OwnerがAIへ作業を委譲し、生成物をreviewし、test・CI・実演結果を
確認して受け入れるまでのOwner稼働日である。AIの処理時間を別の人日として加算しない。

AI支援による短縮を期待する作業は次のとおりである。

- Repository調査、正本間の照合、差分reviewの一次分析
- 定型的なJava code、test、configuration、migration、文書の生成と修正
- test失敗、dependency、静的検査結果の分析と修正候補作成
- 複数の独立したread-only調査または検証の並行実行

次はAIへ委譲しても短縮率が低いか、Owner自身の判断・確認時間を省略できない。

- Architecture、Public API、Security、support scope、例外受容の最終判断
- 外部service、OIDC、cloud、license、契約、secret等の環境成立待ち（Oracleはoptional `P4-ORACLE`承認時だけ対象）
- 非決定的なIntegration / E2E障害、性能観測、運用手順の実演
- Owner Review、Phase完了判定、release判断

AI生成物は、test、CI、静的検査、実演または文書間照合で検証できた場合だけ完了とする。
生成速度を理由にreview範囲、negative test、Ownership確認を省略しない。

### 2.3 Phase計画range

`直接見積もり`は各行の単純合計である。`計画range`は、後続Phaseほど大きい技術・外部環境の
不確実性を反映し、直接見積もりへPhaseごとのcontingencyを加えた値である。これは期限や
commitmentではなく、scope比較と分割判断のための初期値である。

`AI支援係数`は、標準計画rangeに対するOwner稼働日の比率である。機械的なcode・test・文書作業が
多いPhaseでは小さくし、Security、外部環境、運用実演、Owner判断が多いPhaseでは大きくする。
現時点の係数は計画仮説であり、実測済みの生産性指標ではない。

| Phase | 直接見積もり | Contingency | 標準計画range | AI支援係数 | AI支援Owner稼働日 | 判定 |
|---|---:|---:|---:|---:|---:|---|
| Phase 1a | 45〜74標準人日 | 20% | 54〜89標準人日 | 0.45〜0.60 | **24〜53日** | **実行可能** |
| Phase 1b | 69〜114標準人日 | 25% | 86〜143標準人日 | 0.50〜0.65 | **43〜93日** | **要分割** |
| Phase 2 | 96〜158標準人日 | 30% | 125〜206標準人日 | 0.60〜0.75 | **75〜155日** | **要分割** |
| Phase 3 | 120〜194標準人日 | 30% | 156〜252標準人日 | 0.50〜0.65 | **78〜164日** | **要分割** |
| Phase 4 | 140〜231標準人日 | 35% | 189〜312標準人日 | 0.60〜0.75 | **114〜234日** | **要分割** |
| Phase 5 | 155〜255標準人日 | 35% | 209〜344標準人日 | 0.55〜0.70 | **115〜241日** | **要分割** |

AI支援係数は、次の確度と作業特性を根拠とする。

| Phase | 見積もり確度 | AI支援係数の主な根拠 |
|---|---|---|
| Phase 1a | 高 | Walking Skeletonで検証済みのbuild、ArchUnit、外部consumerを正式化する定型作業が中心 |
| Phase 1b | 中〜高 | runtime codeは加速できるが、単一実行基盤と性能baselineは実環境での確認が必要 |
| Phase 2 | 中 | Security判断、複数instance、OIDCのOwner確認と外部依存が大きい |
| Phase 3 | 中 | 業務仕様が確定済みでcode・test生成を加速できるが、全状態・権限・拒否flowの受入確認が必要 |
| Phase 4 | 低 | 非同期、外部I/O、SPA、Batch、observabilityの統合と障害実演が支配する。Oracleは必須scope外 |
| Phase 5 | 低 | 文書・recipe・templateは加速できるが、Security Review、support、cloud実演はOwner責務が大きい |

AI支援Owner稼働日は、下限を`標準計画range下限 × 係数下限`、上限を
`標準計画range上限 × 係数上限`として整数日に丸めた。Phase間の値を単純合算して全体納期と
しない。各Phase開始時に、直前Phaseの実績から係数とcontingencyを再計算する。

`要分割`はロードマップ上のPhaseやrelease番号の変更を意味しない。同一Phase内に、独立して
検証可能な内部マイルストーンを置く必要があるという判定である。現時点で`保留`とするPhaseはない。

## 3. Phase 1a — Build Foundation

**Owner Review:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

Walking SkeletonのV1、V3、V7とbuild foundationの証拠を利用でき、外部業務serviceには依存しない。
ただし、artifact repositoryとCI platformを基盤前提とする。正式なFramework成果物へ再実装する
必要はあるが、技術的不確実性は最も低い。

| DoD | 規模（標準人日） | 主な依存 | 主要リスク |
|---|---:|---|---|
| Phase共通 | 6〜10人日 | Phase 0承認済みADR、Repository構成 | Walking Skeletonのartifact名・package・設定を誤って正式成果物へ流用する |
| 1a-1 | 8〜13人日 | Parent、BOM、Enforcer、Template方針 | Tier差をTemplateへ過剰に固定し、未使用packageを生成する |
| 1a-2 | 10〜16人日 | `koiki-archunit-rules`、ADR参照、CI | 誤検出または修正不能なerror messageにより顧客開発を阻害する。Rule 19は任意のdata-flowを完全検査できず、典型違反の近似検査であるため、OSIV無効化とWeb testによる後続の防御線を維持する |
| 1a-3 | 6〜10人日 | 正式artifact repository、repositoryから分離した外部consumer test | local installまたはreactor内testだけが通り、正式な配布形態でdependency解決できない |
| 1a-4 | 4〜7人日 | NullAway、`@NullMarked`配置 | generated codeやSpring APIとの境界で抑制が増える |
| 1a-5 | 6〜10人日 | Public API定義、japicmp baseline | baseline対象の選定が早すぎ、変更可能な内部APIまで固定する |
| 1a-6 | 5〜8人日 | Java 21 build、Java 21 / 25 runtime CI | build JDKとruntime互換性検証を混同する |

**内部マイルストーン:** (A) 正式Maven座標・Parent・BOM・Architecture Contract・CI骨格、
(B) Feature Template・ArchUnit・NullAway、
(C) artifact公開・外部consumer・japicmp・Java runtime matrix。<br>
**判定:** 実行可能。Phase 0の証拠により重大な技術blockerは解消済みである。

## 4. Phase 1b — Runtime Foundation

**Owner Review:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

Flyway二階層、Testcontainers、OSIV境界は実証済みである。一方、単一実行基盤と性能baselineは
運用環境の選択を伴うため、runtime coreとoperationsを分けて実行する。

`Phase共通`には、Core Configuration、Jackson 3標準設定、Resilience自動構成とtimeout既定値、
API Versioning設定、MyBatis BOM管理、Domain Event規約、およびPhase共通DoDに伴う
ADR・Skill・CI更新を割り当てる。

| DoD | 規模（標準人日） | 主な依存 | 主要リスク |
|---|---:|---|---|
| Phase共通 | 15〜25人日 | Phase 1a成果物、Starter ownership、runtime共通成果物 | 個別設定をStarterへ過剰集約し、Spring標準から乖離する。また、個別DoDへ現れない成果物の検証が脱落する |
| 1b-1 | 7〜12人日 | PostgreSQL、Flyway、V2証拠、正式Starter ownership、Referenceを含む一般化判断 | Framework / Reference / Customer migration ownershipの混在 |
| 1b-2 | 7〜11人日 | MVC、Problem Details、Jackson 3、Validation | 例外詳細の漏えい、MVCとAPIで不整合なerror contractになる |
| 1b-3 | 7〜12人日 | 構造化log、TaskDecorator、async executor | thread境界でcontextが欠落または再利用時に漏えいする |
| 1b-4 | 4〜6人日 | Docker engine、Testcontainers、CI runner | container起動時間と外部image取得がCIを不安定にする |
| 1b-5 | 3〜5人日 | Actuator、DB health indicator | health endpointの情報露出、依存障害時の判定過多 |
| 1b-6 | 3〜5人日 | OSIV無効化、V5証拠 | test用の意図的失敗経路を本番codeへ混入する |
| 1b-7 | 15〜25人日 | Web process外の起動契約、外部scheduler、複数instanceでの排他方針 | local / ECS / Kubernetes間で二重起動防止の前提が異なる。特定cloud固有実装をKOIKI標準として先行固定する |
| 1b-8 | 8〜13人日 | 計測harness、再現可能な環境、workload・warm-up・反復条件 | workload、warm-up、反復回数、環境、version、noise管理の記録が不足し、比較不能な数値になる |

**内部マイルストーン:** (A) runtime core・Problem Details・Validation・Jackson・Resilience・
API Versioning、(B) Flyway・Testcontainers・構造化log・Actuator・OSIV、
(C) 単一実行基盤・性能baseline・Domain Event規約・MyBatis BOM。<br>
**判定:** 要分割。技術的には実行可能である。単一実行基盤では特定cloudをKOIKI標準として
固定せず、Web process外から起動し、複数instanceでも二重起動しない契約を実演する。
ECSまたはKubernetes固有のReferenceは後続Phaseで扱う。

## 5. Phase 2 — Security Foundation

**Owner Review:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

Security、identity、監査、共有sessionおよびPostgreSQL Migrationを扱うため、独立した3系列へ分ける。
Reference仕様§14.2の業務結果も同じtest証拠へ統合する。

`Phase共通`には、Password・Reset、Security Integration Test、第三者管理tableの例外一覧、
OpenRewrite recipeの試作、およびPhase共通DoDに伴うADR・Skill・CI更新を割り当てる。

Local Identityのscopeは次のとおりとする。

- FrameworkはUser・Role・Permissionの標準モデル、table・migration、Password、Lock、Reset、
  Session失効、外部IdPとのlink Portを所有する。
- Referenceの`identity`は、最小限のユーザー・Role・Permission管理画面とUse Caseを実証する。
- 実ユーザーデータは配備先systemの運用データであり、Frameworkの製品dataではない。
- 社員番号、所属、雇用状態、顧客固有Role・Permission、provisioning、人事連携、IdP固有claim mapping、
  業務固有認可policyはCustomer Ownershipとし、Phase 2の標準実装へ含めない。

| DoD | 規模（標準人日） | 主な依存 | 主要リスク |
|---|---:|---|---|
| Phase共通 | 15〜25人日 | Phase 1b、Security threat model、identity設計、共通Security成果物 | 認証方式ごとの責務が混在し、既定設定が経路ごとに不一致となる。また、個別DoDへ現れない成果物の検証が脱落する |
| 2-1 | 5〜8人日 | SecurityFilterChain、Method Security | URL保護とmethod保護の片方だけで成立したと誤認する |
| 2-2 | 7〜12人日 | Permission model、Reference AC-P2-02 | UI制御を認可と誤認し、scope外dataを取得する |
| 2-3 | 10〜16人日 | OIDC provider、local login | account linkと外部claim mappingが顧客固有責務へ越境する |
| 2-4 | 7〜11人日 | OAuth 2.0 Resource Server、JWT issuer / audience / scope | Authorization headerのBearer JWTについて署名、`iss`、`aud`、`exp`、`nbf`、scopeを検証せず受理する。Authorization Server、token発行、refresh、logout・失効をResource Serverのscopeへ混入する |
| 2-5 | 12〜20人日 | Spring Session JDBC、2 instance環境、session fixation・serialization・logout | session serialization、failover、DB負荷、session fixation、logout時失効の見落とし |
| 2-6 | 12〜20人日 | 認証試行制御、並行試行、独立したsecurity audit | 並行試行で閾値を迂回する、lock更新の原子性が崩れる、監査がrollbackに巻き込まれる |
| 2-7 | 6〜10人日 | 業務transaction、business audit | 監査の3分類を誤り、残すべき記録を失う |
| 2-8 | 7〜12人日 | 1b-7、Spring Session cleanup、既定process内cleanupの無効化 | 各Web instanceで既定cleanupが残り、多重起動または外部jobとの削除競合が発生する |
| 2-9 | 7〜11人日 | CSRF、Cookie、Security Header、Session / Bearer経路のSecurityFilterChain | 認証経路のmatcherに隙間が生じる、例外的無効化が広がる、Phase 4で決めるSPA固有方式を先行固定する |
| 2-10 | 8〜13人日 | `identity` ownership、Framework tableの公開contract | 業務moduleがFramework内部schemaへ密結合する、顧客固有属性をFramework tableへ取り込む |
**内部マイルストーン:** (A) 認証・認可profile、OIDC、Bearer Resource Server、CSRF・Header既定値、
(B) Local Identity・Spring Session・認証試行制御・監査3分類・cleanup、
(C) PostgreSQL Migration・第三者管理table一覧・package / Consumer・OpenRewrite試作。<br>
**判定:** 要分割。OIDC test providerと複数instance test環境を
Phase開始条件として確保すれば実行可能である。SPAのtoken保管、refresh、logout、CSRFの
具体方式はPhase 4へ残し、Phase 2ではSession経路とBearer Resource Server経路を独立して実証する。

旧DoD 2-11（4〜6標準人日）と2-12（15〜25標準人日）は2026年8月31日のOwner判断でPhase 2から除外し、
上表とPhase 2集計を再校正した。Oracleはoptional `P4-ORACLE`承認時に新しいscopeと見積を設定する。

## 6. Phase 3 — Reference Vertical Slice

**Owner Review:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

Phase 0のTier 2 Walking Skeletonにより、集約、同期event rollback、OSIV境界は実証済みである。
正式Referenceでは承認済み業務仕様の全状態・権限・拒否flowへ範囲が広がる。

Phase 3の業務scopeは、Reference仕様で承認された次の範囲とする。

- `DRAFT`、`SUBMITTED`、`APPROVED`、`REJECTED`、`RETURNED`、`SETTLED`の6状態と`TR-01`〜`TR-07`
- `UC-MST-01`〜`UC-MST-02`、`UC-EXP-01`〜`UC-EXP-06`、`UC-EXP-Q01`
- `INV-EXP-01`〜`INV-EXP-04`、`INV-MST-01`、`AC-P3-01`〜`AC-P3-10`
- 自己承認、参照scope外、状態不整合、楽観lock競合を含む代表的な拒否flow

`UC-EXP-06`は、手動精算と`SETTLED`への状態遷移までをPhase 3で実装する。
`ExpenseSettled`の非同期処理と月次締めjobはPhase 4で扱い、Phase 3へ先行導入しない。

`Phase共通`には、Spring Modulith Level 1の正式構成と公開境界、Reference仕様からtest証拠への
traceability、SPA実装を伴わないAPI契約文書、Agent Skills、Phase終了時のMyBatis規約、
converter・`@MybatisTest`・楽観lock・reconstituteの検証、およびArchUnit Rule 35〜37を割り当てる。

| DoD | 規模（標準人日） | 主な依存 | 主要リスク |
|---|---:|---|---|
| Phase共通 | 18〜30人日 | Phase 2、Reference仕様、Level 1判断、Skills、ArchUnit Rule 35〜37 | Walking Skeleton codeの昇格、業務仕様とFramework規約の混在、個別DoDへ現れない横断成果物の検証脱落 |
| 3-1 | 18〜28人日 | `master` / `expense` module、Tier Template、Reference仕様の全Use Case・不変条件 | 代表flowだけで完成とみなし、承認済みの状態・権限・拒否flowが欠落する |
| 3-2 | 8〜12人日 | 同期Domain Event、4未処理状態 | `DRAFT` / `SUBMITTED` / `RETURNED` / `APPROVED`で無効化を拒否せず、`REJECTED` / `SETTLED`との判定を混同する |
| 3-3 | 5〜8人日 | module公開境界、ArchUnit / Modulith | event型以外の直接参照が紛れ込む |
| 3-4 | 3〜5人日 | OSIV無効化、Entity露出rule | MVC testと静的検査の責務が曖昧になる |
| 3-5 | 15〜25人日 | Thymeleaf、HTMX 11契約、CSRF、検索・paging・部分更新 | 11契約の標準化がUI component作り込みへ膨張する |
| 3-6 | 8〜13人日 | `@Version`、2 session E2E | 競合再現testがtiming依存になる |
| 3-7 | 12〜20人日 | JPA projection、JdbcClient、read model、認可済み参照scope | query都合がDomain modelへ逆流する、または参照scopeを取得後のfilterだけで処理する |
| 3-8 | 10〜16人日 | Phase 2 business audit、申請・承認・却下・差戻し・精算・master管理 | audit記録と業務transactionの原子性が崩れる |
| 3-9 | 5〜8人日 | cache provider、TTL、master query | stale dataの許容条件が曖昧になる |
| 3-10 | 10〜16人日 | Phase 1b API versioning、Jackson 3、MVCと共通のApplication Use Case | MVCとRESTで認可、不変条件、Problem Detailsの意味が分岐する |
| 3-11 | 8〜13人日 | browser / E2E runner、CI、critical journey | 全組合せをE2Eへ集約して保守不能になる、またはtest dataと画面待機がflakyになる |

**内部マイルストーン:** (A) Domain・状態遷移・同期event・module境界・transaction・監査、
(B) read model・MVC / HTMX・楽観lock・cache、
(C) REST API・critical journey E2E・Spring Modulith Level 1・SPA向け契約・Skills・MyBatis規約。<br>
**判定:** 要分割。Phase 2が完了し、Reference仕様§14.3を受入証拠へ一対一でtraceできれば実行可能である。

## 7. Phase 4 — Enterprise Integration

**Owner Review:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

非同期耐久配信、MyBatis、外部API、SPA、Batch、observability、containerを含み、
最も統合リスクが高い。外部環境を早期に用意し、4系列を別々に完成させてから統合する。

Oracle対応は現行Phase 4の必須scope外である。明示Customer要件と優先度に基づくoptional `P4-ORACLE` Gateが
承認された場合にだけ、対象範囲と見積を別work packageとして追加する。

Reference仕様`AC-P4-01`〜`AC-P4-08`をPhase 4の業務受入範囲とし、通知・仕訳・remindの
at-least-once処理、外部I/O障害、application再起動、およびBatch重複起動に対して、
元の業務transactionをrollbackせず、二重副作用を発生させず、結果を追跡・再処理できることを検証する。
月次締めはPhase 3と同じ精算Application Use Caseを使用し、MVC、REST API、SPAで認可と業務結果を分岐させない。

ReferenceのSPAは、同一originのMVC併用構成としてHTTP Session CookieとCSRF double-submitを実証する。
CookieはSpring Sessionが管理するserver-side Sessionの識別子であり、JWT Cookieではない。
Phase 2で成立させたBearer JWT Resource Server経路も維持し、外部APIまたはstateless SPAでは
Authorization headerのBearer JWTを第一標準とする。Session SPAとBearer APIのSecurity matcherを分離し、
未認証、権限不足、CSRF token不正、および経路matcherの隙間に対する負例を検証する。

`Phase共通`には、SAML Extension、File / Object Storageの安全な連携baseline、Container / ECS Reference、
Resilience4j採否評価、横断的なADR・Skills・CI・運用文書を割り当てる。接続先固有DTO、認証属性mapping、
顧客schema、外部service固有設定はCustomer Adapterの責務とし、Referenceで得た抽象を直接Frameworkへ昇格させない。

| DoD | 規模（標準人日） | 主な依存 | 主要リスク |
|---|---:|---|---|
| Phase共通 | 25〜40人日 | Phase 3、Level 2設計、SAML test IdP、File / Object Storage、Container / ECS環境 | 個別DoDへ現れない成果物が見積もり・検証から脱落する、またはReference固有実装をFrameworkへ過剰昇格する |
| 4-1 | 10〜16人日 | `notification`、非同期event、mail stub | 外部I/Oが元transactionへ混入する |
| 4-2 | 10〜18人日 | Event Publication Registry、restart test | process停止位置により未処理eventを失う |
| 4-3 | 8〜14人日 | event ID、冪等key、notification記録 | at-least-once配信で二重副作用が発生する |
| 4-4 | 8〜14人日 | metrics、FAILED運用、再送手順 | metricだけあり、運用者が安全に再送できない |
| 4-5 | 5〜8人日 | 1b-7、publication retention | purgeと配信が競合し、必要recordを削除する |
| 4-6 | 10〜16人日 | MyBatis楽観lock、共通error contract | JPAとMyBatisで競合意味・HTTP応答がずれる |
| 4-7 | 10〜16人日 | external API stub、retry / concurrency limit、Resilience4j採否評価 | 非冪等操作をretryする、timeoutの層が競合する、評価なしに第三者libraryを標準化する |
| 4-8 | 18〜30人日 | React SPA、Phase 2認証、Phase 3 REST API、CSRF設計、frontend test | Session CookieとJWT Cookieを混同する、token保管・Cookie属性・CSRF境界を誤設計する |
| 4-9 | 10〜16人日 | MVC / SPA routing、Session / Bearer SecurityFilterChain | 経路matcherの隙間から保護設定を迂回する、またはtransportごとに認可・Use Caseの意味が分岐する |
| 4-10 | 12〜20人日 | Spring Batch、remind・月次締め、1b-7、job repository | schedulerとBatch双方の排他が不整合となる、再実行で通知または精算を重複する |
| 4-11 | 8〜13人日 | Java 21互換系統、Java 25以上のVirtual Threads有効系統、負荷test | Java 21で誤って既定有効化する、thread-local依存・driver・pool制約を検証せず互換性を誤認する |
| 4-12 | 6〜10人日 | async context propagation、OpenTelemetry | trace / correlationの欠落または高cardinality化 |

**内部マイルストーン:** (A) Level 2・notification・冪等性・監視・再送・パージ、
(B) accounting・MyBatis・PostgreSQL・外部API、
(C) React SPA・MVC併用・Session / Bearer認証・CSRF・SAML、
(D) Batch・File / Object Storage・OpenTelemetry・Container / ECS・Virtual Threads。<br>
**判定:** 要分割。Phase 3完了に加え、SAML test IdP、mail / API stub、Object Storage、
container / ECS検証環境、およびJava 25以上のCI runnerを開始前に確保する。各系列で
グランドデザイン§27.8とReference仕様§14.4へ追跡可能な統合証拠が得られない場合はPhase 5へ進まない。

## 8. Phase 5 — Production Baseline

**Owner Review:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

Phase 5は新機能より、配布、移行、運用、supportの再現性を製品として完成させるPhaseである。
前Phaseの成果を入力にするため、Phase 4完了時の実績で必ず再見積もりする。

`Phase共通`には、Starter安定化、Reference Application完成判定、Public API棚卸し、japicmp baseline、
release candidateの正式artifact repositoryへの公開、release notes、第三者license・notice、
標準保守契約templateへの年次更新条項、およびv1.0最終実演を割り当てる。
Phase 4までの実験的artifactやReference固有実装を、昇格判断なしにv1.0 Public APIへ固定しない。

Reference Applicationの完成は新しい業務機能の追加ではなく、承認済みReference仕様§14.2〜§14.4と
グランドデザイン§27の受入証拠が揃った状態とする。Project TemplateとAgent Skillsの検証は、
repository内reactorやlocal installに依存せず、正式artifact repositoryを使用する独立した生成先で行う。

OpenRewrite recipeはKOIKI自身のAPI・設定変更を担当し、Spring Boot本体の移行recipeを重複して所有しない。
固定した旧版fixtureへrecipeを適用し、build成功だけでなく、test、設定、実行時behavior、
自動変換できない手動残件を検証する。Security Reviewで未解決のCriticalまたはHigh指摘がある場合は、
v1.0を公開しない。

| DoD | 規模（標準人日） | 主な依存 | 主要リスク |
|---|---:|---|---|
| Phase共通 | 20〜32人日 | Phase 1a〜4、release governance、Public API inventory、正式artifact repository | 未完の実験的artifactをv1.0 Public APIへ固定する、またはrelease成果物間のversionが不一致となる |
| 5-1 | 20〜32人日 | Project Template 2種類、Reference完成、独立した生成先 | repository内reactorやlocal cacheだけで成立し、顧客が正式artifactから開始できない |
| 5-2 | 8〜13人日 | ArchUnit、NullAway、CI template、生成先repository | 顧客側でquality gateが任意化される、またはFramework repository内だけで違反検出が成立する |
| 5-3 | 5〜8人日 | 0-8 baseline表、support方針 | upstream日付変更が公開情報へ反映されない |
| 5-4 | 18〜30人日 | KOIKI固有OpenRewrite recipe、固定した旧版fixture、移行後test | recipeがbuildだけ直し、設定・behavior・手動残件を含む意味的移行を保証しない |
| 5-5 | 10〜16人日 | SBOM生成、脆弱性scanner、第三者license、release CI | false positive対応または例外期限が運用されない、release成果物が検査対象から漏れる |
| 5-6 | 7〜11人日 | 1b-8 harness、同一workload、同等環境 | workloadまたはJVM・container条件の変更によりPhase間の比較が無効になる |
| 5-7 | 12〜20人日 | Agent Skills、独立した生成先、未実装の代表業務feature | 成功例だけを選び、Skillの曖昧さやrepository内部知識への依存を検出できない |
| 5-8 | 15〜25人日 | threat model、全security profile、外部I/O、File、deployment | Critical / High指摘を未解決のまま文書上だけ受容する |
| 5-9 | 12〜20人日 | version差分、OSIV / Jackson 3 migration、実行可能fixture | guide、recipe、実際のbehaviorが乖離する |
| 5-10 | 8〜13人日 | 商用repositoryへの実access、BOM、契約条件 | 再配布権・認証・CI利用条件を技術検証と混同する、または文書確認だけで利用可能と判定する |
| 5-11 | 20〜35人日 | ECS Fargate / EKS、IaC、observability、秘密値管理、cloud account | 2基盤の保守負担、cloud費用、秘密値管理が膨張し、再現可能なReference Deploymentにならない |

**内部マイルストーン:** (A) Starter安定化・RC公開・Template・顧客CI・Skills、
(B) Public API・baseline対応表・support・migration・SBOM、
(C) Security Review・Performance Review、
(D) ECS / EKS Reference Deployment・最終実演・v1.0判定。<br>
**判定:** 要分割。Phase 4完了後にscopeと実績を再評価する。商用repository、cloud account、
脆弱性scanner等の外部環境を確保できない場合は完了扱いとせず、Governanceに従い`保留`または
例外承認とする。すべてのPhase証拠、2種類のProject Template、2種類のdeployment reference、
Public API、support終了日、migration、SBOMのrelease versionが揃ったことを確認してからv1.0判定へ進む。

## 9. Cross-Phase依存関係

```text
Phase 1a Build Foundation
  -> Phase 1b Runtime Foundation
       -> Phase 2 Security Foundation
            -> Phase 3 Reference Vertical Slice
                 -> Phase 4 Enterprise Integration
                      -> Phase 5 Production Baseline

Phase 1b-7 単一実行基盤 -> Phase 2-8 / Phase 4-5 / Phase 4-12
Phase 1b-8 性能harness  -> Phase 5-6
Phase 2 認証・認可      -> Phase 3 MVC / API -> Phase 4 SPA
optional P4-ORACLE      -> 明示Customer要件・優先度・別見積による条件付きwork package
Phase 3 MyBatis規約     -> Phase 4 accounting
Phase 0-8 baseline表    -> Phase 5-3
```

後続PhaseのDoDを先行実装しない。ただし、外部環境の調達、license確認、CI runner能力の確認は、
実装の依存blockを避けるため一つ前のPhaseから準備してよい。

## 10. 実現可能性の総合判断

1. Phase 1a〜5に、現時点で技術的に成立不能と判断するDoDはない。
2. Phase 1aは現在のscopeのまま実行可能である。
3. Phase 1b〜5は、一人projectでPhase全体を一括着手せず、本書の内部マイルストーン単位に分割する。
4. Phase 2以降は承認済みscopeに必要な外部環境を開始条件として確認する。利用不能な場合は該当DoDを黙って除外せず、
   Governanceに従い`保留`または例外承認として記録する。
5. 各Phase開始時に前Phaseの実績を反映してrangeを再計算し、上限を継続的に超える場合はscopeを
   削るのではなく、Phase内分割またはロードマップ改定をArchitecture Ownerが判断する。
6. Phase 1a完了時に、標準人日とAI支援Owner稼働日の実績差を記録し、Phase 1b以降のAI支援係数を
   最初に再校正する。

## 11. Owner Review観点

- 見積もり単位とrangeはPhase間のscope比較に十分か。
- Javaアーキテクト1名とCodexによるAI支援実行モデル、およびOwner責務の境界は妥当か。
- AI支援係数と見積もり確度は、各Phaseの作業特性を適切に反映しているか。
- Phase 1b〜5を`要分割`とする判断は妥当か。
- 内部マイルストーンの境界は、独立した実演証拠を作れる単位か。
- 外部環境と後続Phaseへ保留した判断が明示されているか。
- DoDまたはReference仕様§14の受入条件を見積もりから脱落させていないか。

2026年8月19日に承認者、承認日、判定を本書へ記録し、Phase 0 Closeoutの0-5をCOMPLETEへ更新した。

**Owner Review結果:** ACCEPTED（2026年8月19日、Shuichi Kataoka）<br>
Phase 1aは`実行可能`、Phase 1b〜5は`要分割`として承認した。これは各Phaseの規模・依存・risk・
実現可能性評価の承認であり、後続Phaseの実装完了またはv1.0公開承認を意味しない。

## 12. 参照

- `grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §27
- `KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md`
- `../reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md` §14
- `validation/walking-skeleton-phase0-completion.md`
- `governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md`

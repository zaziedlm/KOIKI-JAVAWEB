# KOIKI-JavaWeb-FW Pre-Phase 4 Framework Adoption Readiness 計画

**状態:** P4-AR0 COMPLETE / OWNER APPROVED / EXECUTION AUTHORIZED AFTER PLAN MERGE AND CLEAN-MAIN SYNC  
**作成日:** 2026年9月17日  
**計画branch:** `docs/phase3-closeout-pre-phase4-readiness-plan`  
**開始候補baseline:** Phase 3 merge commit `aa83fa578b5f689ce72e2a2540ad3ec2b659c083`以降、本計画をmergeしたclean `main`  
**Architecture Owner:** Shuichi Kataoka

## 1. Background and purpose

Phase 3 Reference Vertical SliceはGate Cで`COMPLETE / ACCEPTED`となり、PR #35のmergeとmerge後`main` CIも完了した。
そのpost-merge closeout時点で、KOIKI Frameworkを採用する実案件プロジェクトの始動と、当初Phase 4でKOIKI側が
単独実施する想定だった作業の一部を実案件側が担当する見込みが明らかになった。

実案件との連携前に、Framework開発者側で次を優先する必要がある。

- merge済み`main`を唯一の起点として、Frameworkの機能・処理・配布境界を再検証する。
- Customer-like Consumerとpackage済みReferenceを用い、Repository内unit testだけでは見えない利用経路を確認する。
- JDK、Maven Wrapper、Docker、PostgreSQL、browser、network / proxyおよびIDEを含む開発環境の不足を洗い出す。
- 現存するFramework artifact、Customer-like Consumer、Reference Application、ToolingおよびJourneyを受渡し候補として
  棚卸しし、業務アプリ開発チームの受入側視点で実際にbuild・起動・操作・診断できるかを確認する。
- Framework、実案件、共同Evidenceの責任分界をPhase 4 production実装より先に確定する。
- 実案件で得た知見を、Customer固有codeやsecretをFrameworkへ混入させず還元できる経路を定義する。

本計画は、この移行条件を`P4-AR`（Pre-Phase 4 Adoption Readiness）として扱い、見直し後のPhase 4開始判断へ
入力することを目的とする。

## 2. Phase and governance positioning

- P4-ARはPhase 3のDoDやGate Cを遡及変更せず、Phase 3を再オープンしない。
- P4-ARはPhase 4 production実装ではなく、Phase 3 post-acceptance transitionに置く開始前Gateである。
- 本計画のRepository反映後、`main`を再同期し、clean worktreeから検証を開始する。
- 検証でsource defectや開発環境不足を検出した場合、分類・Evidence・影響範囲を先に記録し、修正を別commit pointで扱う。
- Framework Public API、module、Starter、migration、dependency、property、workflowまたはrulesetの変更は、
  対応するblocking reviewより前に行わない。
- Phase 4開始、remote push / PR / merge、workflow dispatch、ruleset変更およびsnapshot publishは個別承認を要する。

## 3. Ownership

| 対象 | Primary ownership | Boundary |
|---|---|---|
| Framework artifact / Parent / BOM / Starter / Public API | Framework | 業務語彙とCustomer固有要件を持ち込まない |
| 検証script / isolated repository / Consumer / browser harness | Tooling | 正式release unitやCustomer配布物へ含めない |
| Reference Application / master / expense / migration | Reference | Framework内部またはProject Templateとして扱わない |
| 実案件の業務code / migration / configuration / deployment | Customer | Framework Repositoryへ直接取り込まない |
| Gate、責任分担、Evidence受入 | Architecture | Owner判断を記録してから次境界へ進む |

## 4. Scope

### 4.1 Included

- clean `main`のsource identity、worktree、JDK、Maven WrapperおよびDocker Engineのpreflight
- Root Reactor、Architecture Rules、NullAway、Feature TemplateおよびPublic API compatibility
- Runtime / Security Foundationの累積verificationとCustomer-like Consumer
- package済みReference JAR、PostgreSQL 17、MVC / REST、browser、Audit、logおよびcleanup
- Java 21 buildとJava 21 / 25 runtime compatibility
- Windows開発端末とGitHub-hosted Linux runnerの差分
- corporate SSL inspection proxy、JDK / IDE trust store、port、processおよびcontainer診断
- 業務アプリ開発チームへ提示可能な受渡し候補のinventoryと、clean環境での受入rehearsal
- 実案件へのartifact、設定、migration、Security profile、開発手順および問い合わせ境界
- Phase 4成果物ごとのFramework / Customer / joint Evidence責任分担

### 4.2 Excluded until separately approved

- Phase 4の`notification`、`accounting`、SPA、SAML、Batch、External API、File / Object Storage等のproduction実装
- `PersistenceModel.SEPARATED`、Rule 25〜27 / 30〜37、MyBatis fixtureまたはdependencyの追加
- KOIKI-hosted Authorization Server、Oracle、AWS固有AdapterおよびCustomer固有cloud資材
- Project Template、正式release、一般公開repository、snapshot publishおよびCustomer repositoryへの書込み
- 実案件のsource、schema、credential、token、個人情報または機密logのKOIKI Repositoryへの複製

## 5. Work packages

| ID | Work package | Main output | Stop point |
|---|---|---|---|
| P4-AR0 | Plan / boundary review | 本計画、責任分担の決め方、検証順序のOwner承認 | 承認前は検証開始・Phase 4実装を行わない |
| P4-AR1 | Clean-main environment preflight | source identity、JDK 21 / 25、Wrapper、Docker、PowerShell、network / proxy、port inventory | 前提不足を回避設定で隠さない |
| P4-AR2 | Framework build / quality baseline | Root verify、Architecture、NullAway、Template、Public API、runtime compatibility Evidence | tracked source変更を検出したら停止 |
| P4-AR3 | Runtime / Security Consumer baseline | isolated repository、正式release unit、Customer-like Consumer、PostgreSQL integration Evidence | Root Reactorへの偶発依存を検出したら停止 |
| P4-AR4 | Reference application baseline | package済みJAR、MVC / REST、critical E2E、DB / Audit / log / cleanup Evidence | secret、SQL、stack traceまたはresource残存を検出したら停止 |
| P4-AR5 | Developer handoff readiness | clean setup、受渡し候補inventory、受入側build / run / operation / troubleshooting rehearsal | 手作業だけでしか復元できない前提を未記録のまま受入れない |
| P4-AR6 | Actual-project handoff contract | Framework / Customer / joint Evidence責任分担表、artifact受渡し、issue routing | Customer固有実装をFramework成果物へ自動昇格しない |
| P4-AR7 | Finding remediation | defect / DX gap / Customer requirement / Phase 4 feature分類と承認済み修正 | Public API等のblocking reviewを省略しない |
| Gate P4-AR | Adoption Readiness acceptance | accepted baseline、blocking 0、Phase 4再計画への入力 | Phase 4開始は別承認 |

## 6. Planned verification matrix

コマンドは本計画merge後のclean `main`で、P4-AR1のenvironment inventoryを確定してから実行する。
実行時のsource commit、JDK、OS、開始・終了時刻、PASS / FAIL、cleanupおよび再現手順をEvidenceへ記録する。

| Layer | Planned verification | Primary evidence |
|---|---|---|
| Root build | Maven Wrapper `clean verify` | 全reactor test、Architecture Rules、NullAway |
| Feature structure | Tier 1 / Tier 2 Feature Template verification | 生成結果、positive / negative architecture path |
| Public API | accepted timestamped baselineとのjapicmp比較、fixture | 互換性差分0または承認済み差分 |
| Runtime compatibility | Java 21 build、同一artifactのJava 21 / 25 runtime | class major、manifest、hash、実行結果 |
| Runtime Consumer | CP10 closeout aggregate | isolated repository、package済みCustomer-like JAR、Web / maintenance journey |
| Security Consumer | Phase 2 closeout aggregate | default deny、Session、OIDC / Bearer、Audit、Identity、migration、cleanup |
| Reference focused | Reference unit / PostgreSQL integration、API / browser focused tests | master / expense、MVC / REST、optimistic lock |
| Critical journey | package済みReference Critical Journey E2E | Browser / API / DB / Audit / log、cleanup |
| Manual developer journey | Local Run Guideによるclean DB起動と操作 | setup時間、操作結果、診断gap、環境依存 |
| Recipient-side adoption rehearsal | Framework artifact解決、Customer-like Consumer build、Reference起動・操作・診断 | 受渡し候補ごとの利用可否、前提、非提供境界 |
| Runtime environment | process / container / port / temp file inspection | 実行前後の残存0 |

Public API baseline取得にcredentialが必要な場合は、環境変数または承認済みcredential helperだけを使う。
credentialをcommand line、log、EvidenceまたはRepositoryへ出力しない。credential未供給時は検査不能を成功扱いせず、
P4-AR1のblocking preconditionとして記録する。

## 7. Developer handoff readiness

P4-AR5では、Framework開発者が既知の端末だけで動かせることではなく、実案件の業務アプリ開発チームが
受渡し候補を受け取り、再現可能な入口から実際に動かせることを確認する。

| Area | Checkpoint |
|---|---|
| Java | JDK 21 build、JDK 25 runtime、`JAVA_HOME` / Toolchains / IDE runtimeの役割が区別できる |
| Maven | Wrapperだけでbuildでき、通常local repositoryの偶発artifactへ依存しない |
| Network | Maven Central / GitHub Packages / corporate proxy / trust store障害を診断できる |
| Container | Docker Engine接続、PostgreSQL 17、Testcontainers、port競合、cleanupを確認できる |
| IDE | VS Code Java / Spring / XML processが使用するJREとtrust storeを特定できる |
| Application | package済みJAR、migration、configuration、local Session MVCを手順どおり起動できる |
| Security | test credential / key / Cookie / tokenをproduction artifactや共有Evidenceへ残さない |
| Diagnostics | build、startup、migration、authentication、authorization、DB、browserを段階的に切り分けられる |

不足が見つかった場合は、Framework codeで解決すべき問題、Tooling / documentationで解決すべき問題、
実案件環境が所有する問題に分類する。環境回避策をFramework既定へ無条件に組み込まない。

### 7.1 Handoff candidate inventory

P4-ARでは次を「現時点で業務アプリ開発チームへ何を示し、何を動かせるか」を判断する受渡し候補として扱う。
これは正式配布物一覧ではなく、見直し後Phase 4で正式提供範囲を決めるための実行可能性inventoryである。

| Candidate | Recipient can verify | Current boundary | P4-AR output |
|---|---|---|---|
| KOIKI Parent / BOM / Starter / Architecture artifact | 通常Maven coordinatesで依存解決し、Customer codeをbuildできる | 正式releaseではなく承認済み内部snapshot / isolated stage | 必要座標、version、解決経路、利用可否 |
| Runtime Foundation Customer-like Consumer | Tier 1 / 2、HTTP、JPA、event、observability、maintenance processをbuild / runできる | Tooling-owned、Root Reactor外、非配布 | 実案件構成へ応用できる範囲と不足 |
| Security Foundation Customer-like Consumer | Security chain、profile、Java 21 / 25 runtime境界を確認できる | test identityや実業務を提供しない | Security依存選択と起動前提 |
| Reference Application source / package済みJAR | module構造を参照し、Identity、master、expense、MVC / HTMX、REST、Audit、migrationを一体で起動・操作できる | 利用例でありCustomer Templateではない | 動作例として渡せる範囲と非転用箇所 |
| Reference local demo / verification Tooling | clean DB、使い捨てdata、browser / API / critical journeyを再現できる | test credential、seed、issuer、browser harnessは非配布 | 確認用導線とproductionへ持ち込まない境界 |
| Developer Journey / Local Run / troubleshooting docs | setup、起動、操作、障害切り分けを追跡できる | 実案件固有provisioning / deploymentは含まない | 欠落手順、理解困難点、Phase 4追加候補 |

### 7.2 Recipient-side execution rehearsal

Framework開発者は、受入側を模したcleanな作業条件で次のjourneyを実行する。通常local Maven repositoryに残る
未説明artifact、Framework sourceの直接参照、固定credentialまたは既知端末だけの暗黙設定へ依存しない。

```text
受渡し候補と前提を読む
  -> JDK / Maven / Docker / networkをpreflightする
    -> Framework artifactをisolated repositoryから解決する
      -> Customer-like Consumerをbuildしpackage済みJARを起動する
        -> Reference Applicationをclean PostgreSQLで起動する
          -> Session MVC / REST / migration / Auditの代表操作を行う
            -> 意図した失敗を診断し、process / container / port / tempをcleanupする
```

各候補について、次をEvidenceへ記録する。

- 業務アプリ開発チームが受け取る単位と入口
- 必須software、version、credential、repositoryおよびnetwork前提
- 最初のbuild / run / representative operationまでの手順と所要時間
- 成功時の観測点と、代表的な失敗の診断入口
- Framework、Reference、Tooling、CustomerのOwnership
- 現在渡せるもの、検証専用で渡さないもの、Phase 4で新規整備すべきもの

P4-ARはこのrehearsalが成立するところまでを受け持つ。正式な受渡し対象、配布repository、案件向けstarter set、
versioning / support境界または統合開発環境を何にするかは、P4-AR Evidenceを入力として見直し後Phase 4準備の
新しいwork packageで決定する。正式Project TemplateはPhase 5境界を維持し、Phase 4へ先行させない。

Framework側のrehearsalで成立した候補は、P4-AR6で実案件側の受入担当者へ提示し、利用対象、実行前提、非提供境界、
問い合わせ経路および追加gapを確認する。実案件repositoryへの変更、artifact配布、credential共有または環境操作が
必要な場合は、本計画の承認へ含めず、対象と影響を示して別途承認を得る。

#### 7.2.1 Application-developer one path and acceptance

P4-AR5では、技術checkpointの実行結果だけでなく、Phase履歴を知らない業務アプリ開発者が次の一本道をたどれる
単一入口のhandoff guideを用意する。

1. KOIKIから何が提供されるか。
2. 現時点で提供されないものは何か。
3. MVC、REST、React / BFFのどれから始めるか。
4. 必要なStarterをどう選ぶか。
5. Referenceをどう動かして理解するか。
6. 最初のCustomer-owned業務moduleをどう設計するか。
7. Referenceからコピーしてはいけないものは何か。
8. Framework境界付近の機能をどう分類するか。
9. 失敗時にどこを確認するか。
10. どの判断をアプリチームで行い、どこからFramework側へ戻すか。

P4-AR5の追加受入条件は次のとおりとする。

- Phase履歴を知らない開発者が、単一の入口文書から開始できる。
- 利用可能なartifactと検証専用Toolingを区別できる。
- 30〜60分程度でReferenceまたはConsumerを起動できる。
- MVC / REST / frontend構成の次の判断先を選べる。
- Customer Ownershipの最初の業務moduleをどこに置くか説明できる。
- ReferenceのDTO、Entity、migration、fixtureをコピーしない理由を説明できる。
- 不足機能をStandard use / Approved extension / Customer isolation / Framework gapへ分類できる。
- Security既定を弱めず、Framework側へ戻す条件が分かる。

P4-AR5は[Application Team Handoff Guide](application-team-handoff-guide.md)と、その中の受入確認チェックリストを
Owner review可能な状態まで整備する。実際のアプリ開発チームによる実行、理解度確認、所要時間計測およびfeedback分類は
P4-AR6で行い、P4-AR5のFramework開発者側rehearsalを実案件受入の代替とはしない。

### 7.3 Application team extension boundary

Frameworkが提供する契約で充足できる機能は、業務アプリ開発チームがSpring標準の作法と、承認済みFramework Public API、
StarterおよびArchitecture Rulesを利用して実装する。業務module、画面、外部連携、案件固有configuration / migrationは
Customer Ownershipに置き、Framework内部へ混在させない。

ここで目指す「機能十分」は、あらゆる案件機能をFrameworkが先回りして内包することではない。頻度が高く安定した横断契約と
安全な既定・拡張境界をFrameworkが提供し、案件ごとに変わる機能を通常のSpring Applicationとして実装できる状態を指す。

一方、見直し後Phase 4の機能が未実装である場合や、FrameworkとCustomerの責務境界に近い機能を実案件側が先行して
設計・製造する場合は、次の順序で現実解を判断する。

1. Spring標準機能とCustomer-owned module / Adapter / configurationの組合せで実現する。
2. 承認済みPublic APIまたは明示された拡張点へ、composition、Port / Adapter、型と`Qualifier`が明確なDIで接続する。
3. 案件固有bridgeをCustomer側へ隔離し、Framework内部package、bean name、未公開classまたは実装順序へ依存させない。
4. 安全な拡張点がない場合は無理に統合せず、F2、F4またはF5 findingとして不足と影響を記録し、blocking reviewへ戻す。
5. Framework本体の変更が必要な場合は、Customer非依存性、再利用性、後方互換性、Security / Audit、検証可能性および
   support責任を設計した上で、Public API等の承認手順とArchitecture Owner判断を経る。

ここでいう機能差替えは、Frameworkが契約として用意した拡張点を使用することを指す。Springのbean definition overriding、
同名beanによる偶発的shadowing、内部実装classの継承または起動順序依存を一般的な拡張手段とはしない。必要な差替え契約が
存在しなければ、DIで接続できるという理由だけで採用しない。

次は回避すべき状態としてP4-AR5 Evidenceで確認する。

- Framework sourceまたは内部実装をCustomer repositoryへ複製し、独立進化させる。
- Frameworkと同じ責務を持つ横付け機能が、Identity、認可、Audit、transaction、migrationまたはmodule境界を迂回する。
- Framework既定を無効化または弱化して、案件固有処理を暗黙に優先する。
- 実案件だけで成立した仕組みを、十分な設計・回帰検証なしにFramework成果物へ昇格する。
- 将来の統合を理由に、現時点で不自然な共通化、Public API化またはdeployable統合を強制する。

P4-AR5では個々の未実装機能のproduction設計を確定しない。受渡し候補ごとに、標準利用、承認済み拡張、Customer隔離、
Framework gapのどこに位置するかを確認し、無理のない接続方式が存在しないものは未解決として明示する。P4-AR6はこの結果を
用いてFramework / Customer / joint responsibilityを判断する。

### 7.4 General design review

§7.3の方針は、一般的なApplication Framework / Platform Engineeringにおけるstable core、explicit extension point、
composition over forkおよびconsumer-owned integrationの考え方と整合する。ただし、実務で過度な統制または不自然な統合に
ならないよう、手段を次のように区別する。

| Mechanism | Appropriate use | Required boundary |
|---|---|---|
| Configuration | endpoint、timeout、feature有効化等の契約済みvariation | 型付き設定、validation、安全なdefault、設定変更時の影響を明記 |
| Composition | Customer module、業務Use Case、外部連携Adapterの追加 | 依存方向、transaction、Identity / Permission / Auditの責務を維持 |
| Designed substitution | Frameworkが明示したinterface / SPIまたはconditional defaultの差替え | Public contract、選択条件、default back-off、複数候補時のfail-fast、contract test |
| Separate integration | 別deployable、BFF、gatewayまたは案件固有serviceが妥当な場合 | protocol、認証、障害、整合性、observabilityおよび運用Ownerを明示し、Framework責務を迂回しない |
| Provisional bridge | 納期上、正式拡張点の確定前に案件側で一時的に接続する必要がある場合 | Customer隔離、Owner、期限、削除trigger、既知risk、upgrade確認および正式化／廃止の再review日を記録 |
| Framework contribution | 複数案件へ共通化すべき安定契約で、Spring標準では代替できない場合 | Grand Design §9.2、Public API review、ADR、互換性・回帰検証、support義務 |

Spring Bootの`@ConditionalOnMissingBean`等によるback-offはdesigned substitutionを作る手段になり得るが、DI containerが
技術的に差替え可能であること自体は拡張契約を意味しない。Framework側が対象型、既定動作、選択条件、lifecycleおよび
差替え後も守るべきsemanticsを公開し、consumer側のcontract testで検証できる場合に限り正式な拡張点として扱う。
`@Primary`や`@Qualifier`は候補選択を明確にする手段であり、Ownershipや互換性を保証するものではない。

差替えまたはbridgeの評価では、機能結果だけでなく次のinvariantを確認する。

- default deny、Identity、Permission、CSRF / CORS等のSecurity境界を弱めない。
- Business / Security Auditの記録主体、transaction境界、失敗時semanticsを変えない、または差を明示する。
- migration / schema ownership、error contract、configuration validationおよびstartup fail-fastを維持する。
- log、metric、trace、health、cleanupおよび障害診断の観測点を失わない。
- Framework upgrade時に内部実装へ追随する必要がなく、Public API互換検査とCustomer側Architecture Rulesを通過する。

別deployableや案件固有Adapterは、それ自体を「歪な横付け」と判定しない。責務が明確で、安定したprotocolを介し、重複する
正本やSecurity / Auditの迂回を作らず、独立した運用責任を持てる場合は正当な分離である。反対に、同一process内のDIであっても
内部実装依存、暗黙の優先順位または責務重複があれば不適切とする。

Architecture Owner reviewは、Framework Public API、default、安全性、共通artifactまたは複数案件support義務を変える判断へ
集中する。Customer内部の通常実装まで逐次承認対象にせず、公開済み契約とArchitecture Rulesの範囲内はアプリ開発チームが
自律的に進められる状態を受渡し目標とする。

### 7.5 Architecture Owner approval record

2026年9月18日、Architecture Ownerは§7.3および§7.4を、P4-AR5で用いるアプリ開発チームの位置づけと
拡張判断方式として承認した。

承認対象は、標準利用、承認済み拡張、Customer隔離、Framework gapの分類、designed substitution、
provisional bridgeおよびFramework contributionの判断方式である。これはP4-AR5の実行完了、個別機能の方式選定、
新規拡張点またはPublic API、Framework本体改修、実案件repository操作、artifact配布、P4-AR6の責任分担、
Gate P4-AR acceptanceまたはPhase 4開始を承認するものではない。

### 7.6 P4-AR5 completion approval record

同日、Architecture Ownerは技術checkpoint 1〜8のPASS、受渡し候補inventory、finding分類、
[アプリ開発チーム向け引継ぎガイド](application-team-handoff-guide.md)の10項目の一本道と8項目の受入条件、
日本語表現、表の読み方および説明補足を確認し、現時点で問題なしとして承認した。

この承認によりP4-AR5を`COMPLETE / OWNER APPROVED`とする。実際のアプリ開発チームによる実行、理解度、
所要時間およびfeedbackの確認はP4-AR6で行う。本承認は正式配布物、実案件repository操作、artifact配布、
P4-AR6の責任分担、Gate P4-AR acceptanceまたはPhase 4開始を承認するものではない。

## 8. Phase 4 responsibility reallocation review

実案件側の担当範囲はP4-AR6で具体化する。現時点では次の成果物ごとに、実装OwnerとPhase 4 acceptance Evidenceの
Ownerを分けて判断し、実案件側が実装するという理由だけでKOIKI側のDoDを自動充足としない。

| Phase 4 area | Framework responsibility candidate | Actual-project responsibility candidate | Decision |
|---|---|---|---|
| `notification` / Level 2 | event / idempotency / retry / purgeの共通契約と検証可能性 | 業務通知内容、provider、運用 | P4-AR6で決定 |
| `accounting` / MyBatis | adoption Gate、構造規約、Architecture Rules、共通error contract | 実schema / SQL / 会計連携 | P4-AR6で決定。trigger成立前はRule 8拒否 |
| SPA / MVC coexistence | Security profile、Session / CSRF / CORS契約 | 実画面、frontend構成、UX | P4-AR6で決定 |
| External API resilience | timeout / retry / concurrency / error translation契約 | 接続先固有Adapter、credential、SLA | P4-AR6で決定 |
| Batch / file / object storage | 単一実行、共通処理境界、検証指針 | 業務job、file format、storage設定 | P4-AR6で決定 |
| Observability | correlation、metric / trace契約、非露出 | 実監視基盤、alert、運用runbook | P4-AR6で決定 |
| Container / ECS / VT | Framework runtime要件とReference Evidence | 実deployment、resource sizing、network | P4-AR6で決定 |

実案件EvidenceをKOIKI側で受け入れる場合は、再現条件、使用artifact、source identity、非機密化、Customer非依存性、
Framework契約との対応および継続実行Ownerを明示する。条件を満たさない結果は案件固有Evidenceとして保持し、
Framework acceptanceへ昇格させない。

## 9. Finding classification and remediation boundary

| Class | Meaning | Treatment |
|---|---|---|
| F1 Framework defect | 承認済みFramework契約を満たさない | P4-AR blocking。修正、全回帰、Owner review |
| F2 Developer-experience gap | setup、診断、Tooling、文書が不足 | 実案件連携前に優先度を判断し、必要分を修正 |
| F3 Environment-specific issue | proxy、IDE、OS、container runtime等に依存 | 所有者別手順へ分離し、Framework既定を歪めない |
| F4 Customer requirement | 実案件固有の業務・schema・deployment要件 | Customer backlogへ置き、Framework昇格条件を別review |
| F5 Phase 4 feature | Grand Design上のPhase 4成果物 | 見直し後Phase 4計画へ割当。P4-AR中に先行実装しない |
| F6 Optional Gate | Authorization Server、Oracle、cloud固有等 | 対応optional Gate未承認なら開始しない |

## 10. Definition of Done

| ID | Completion condition |
|---|---|
| AR-D1 | 本計画merge後のclean `main`をsource identityとして固定している |
| AR-D2 | P4-AR2〜AR4の必須自動検証がPASSし、検査不能を成功扱いしていない |
| AR-D3 | 正式release unitをisolated repositoryからCustomer-like Consumerが利用できる |
| AR-D4 | package済みReference JARのMVC / REST / DB / Audit / log / browser journeyが成立する |
| AR-D5 | secret非露出とprocess / container / port / temp file cleanupを確認している |
| AR-D6 | §7.1の受渡し候補を棚卸しし、現在渡せるもの／検証専用で渡さないもの／Phase 4で整備するもの、および§7.3の標準利用／承認済み拡張／Customer隔離／Framework gapを分類し、§7.2.1の単一入口から説明できる |
| AR-D7 | §7.2の受入側execution rehearsalでbuild / run / representative operation / diagnosis / cleanupが成立し、§7.2.1の8受入条件をP4-AR5でOwner review可能、P4-AR6で実チーム確認可能な形にしている |
| AR-D8 | 実案件連携を妨げる未記録の開発環境前提がない |
| AR-D9 | F1 blockingが0で、F2〜F6のOwner、優先度、次Gateが明示され、Framework copy、内部実装依存または責務迂回を未記録のまま受入れていない |
| AR-D10 | 実案件側の受入確認を踏まえ、Phase 4成果物のFramework / Customer / joint Evidence責任分担と、見直し後Phase 4への入力をOwnerが承認している |

## 11. Evidence and commit points

| Commit point | Content | Remote boundary |
|---|---|---|
| AR-CP0 | Phase 3 post-merge closeout + 本計画 | 現在の文書PR。production / workflow変更0 |
| AR-CP1 | clean-main preflight / baseline Evidence | local検証結果をreview後、push可否を個別判断 |
| AR-CP2 | Framework / Consumer / Reference aggregate Evidence | failure修正を混在させず、原因単位でcommit |
| AR-CP3 | Developer handoff rehearsal、environment gapと必要な補正 | code / docs / ToolingをOwnership別に分離 |
| AR-CP4 | 実案件handoff contract / Phase 4責任分担 | Customer機密情報を含めない |
| Gate P4-AR | final acceptance | Phase 4開始、publish、workflow変更は別承認 |

実行Evidenceは`docs/architecture/validation/`へ置く。長大なprocess log、secret、token、Cookie、password、key、
Customer source、個人情報またはSQL値をEvidenceへ貼付しない。

## 12. Stop conditions

次の場合は作業を停止し、Architecture Ownerへ戻す。

- clean `main`でない、またはtracked sourceに未説明差分がある。
- testを通すために承認済みbaseline、Security既定、Architecture Rulesまたはrequired checkを弱める必要がある。
- Public API、migration、dependency、Starter、workflow、rulesetまたは配布単位の変更が必要になる。
- Customer固有code / data / credentialをFramework Repositoryへ持ち込む必要がある。
- Phase 4成果物のOwnerまたはacceptance Evidence責任が曖昧なままproduction実装へ進もうとしている。
- MyBatis adoption triggerを理由にP3-C3延期案を自動採用しようとしている。
- cleanup不能、secret非露出違反または検査不能を検出する。

## 13. Proposed execution sequence

1. **COMPLETE:** Phase 3 post-merge closeoutと本計画を同一branchでreviewする。
2. **COMPLETE:** P4-AR0で本計画、検証範囲、credential前提および実案件情報の非持込み境界をOwner承認する。
3. **COMPLETE:** 承認記録を含む文書差分をcommitし、個別承認に従ってPR / required checks / mergeを完了した。
4. **COMPLETE:** local `main`をremote `main`へ同期し、clean worktreeとsource commitを記録した。
5. **COMPLETE:** P4-AR1のenvironment preflightを実施した。
6. **COMPLETE:** P4-AR2〜AR4を順に実行し、Framework / Consumer / Reference baselineを確定した。
7. **COMPLETE / OWNER APPROVED:** P4-AR5で受渡し候補inventory、受入側execution rehearsal、開発環境gap分類を行い、アプリ開発チーム向けhandoff guideを承認した。
8. **COMPLETE（Framework側で確認済みのfinding）:** P4-AR3、P4-AR4およびP4-AR6準備で検出したfindingをOwnership別に補正し、該当範囲を再検証した。実チーム受入で新たに得るfindingはP4-AR7の入力とする。
9. **WAITING FOR ACTUAL-TEAM INPUT:** P4-AR6のhandoff契約、R2 Tooling、Framework rehearsalおよび実チーム受入worksheetは`OWNER APPROVED`である。実チーム受入セッション、AR-D10の責任分担判断および最終handoff承認は、実案件情報を入手してから実施する。
10. **PENDING:** Gate P4-ARでAdoption Readinessを判定する。
11. **PENDING:** accepted inputを用いてPhase 4実行計画を見直し、Phase 4開始可否を別途判断する。

## 14. Architecture Owner review points

| ID | Decision requested | Recommended disposition | Owner decision |
|---|---|---|---|
| AR-1 | P4-ARをPhase 3再オープンでもPhase 4開始でもないtransition Gateとして置くか | APPROVE | APPROVED |
| AR-2 | 本計画merge後のclean `main`からFramework検証を開始するか | APPROVE | APPROVED |
| AR-3 | Framework / Tooling / Reference / CustomerのOwnership境界を§3どおり維持するか | APPROVE | APPROVED |
| AR-4 | §6の検証matrixと検査不能を成功扱いしない境界を採用するか | APPROVE | APPROVED |
| AR-5 | §7のDeveloper Handoff Readinessと受入側execution rehearsalを実案件連携前の必須確認とするか | APPROVE | APPROVED |
| AR-6 | 実案件担当分も§8の責任分担とEvidence受入条件を経てPhase 4へ割り当てるか | APPROVE | APPROVED |
| AR-7 | P3-C3、optional Gate、remote mutationおよびPhase 4開始の停止条件を維持するか | APPROVE | APPROVED |

本計画の作成とRepository内reviewは、P4-AR検証実行、source修正、remote push / PR / merge、snapshot publish、
実案件repository操作またはPhase 4開始を許可しない。

## 15. Architecture Owner decision

Architecture Ownerは§14の推奨承認内容を確認し、AR-1〜AR-7をすべて承認した。

> Phase 3を再オープンせず、P4-ARをPhase 4開始前のtransition Gateとして設けることを承認する。
> AR-1〜AR-7を承認し、本計画をRepositoryへ反映した後、同期済みclean `main`からFramework、
> Customer-like Consumer、Reference Application、Developer Handoff Readinessの検証を開始してよい。
> 受渡し候補の実行可能性はP4-ARで確認するが、正式な受渡し対象、Phase 4の責任分担、Public API変更、
> artifact配布、workflow変更、snapshot publishおよびPhase 4開始は別途判断する。Project TemplateはPhase 5境界を維持する。

**Decision:** APPROVED — P4-AR0 COMPLETE / AR-1〜AR-7 APPROVED

**Decided by:** Shuichi Kataoka, Architecture Owner

**Decision date:** 2026年9月17日

**Authorized next action:** 承認記録を含む本branchの文書差分をcommitし、個別承認に従ってpush、PR、required checksおよびmergeを進める。

P4-AR1以降の検証は、本計画が`main`へmergeされ、local `main`をremote `main`へ同期し、clean worktreeと
source commitを記録した後に開始する。本承認はremote push / PR / merge、source修正、snapshot publish、
実案件repository操作、Phase 4 production実装またはPhase 4開始を一括許可しない。

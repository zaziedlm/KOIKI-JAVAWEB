# Phase 2 Security Foundation start handoff

## 1. Purpose and status

本書は、Phase 1b Runtime Foundation完了後にPhase 2 Security Foundationの開始前
preflight／inventoryを新しいAI対話sessionで再開するための引継ぎである。

- **Handoff date:** 2026年8月30日
- **Architecture Owner:** Shuichi Kataoka
- **Current branch:** `feature/phase2-security-foundation`
- **Baseline main commit:** `b2e2123605e4d971c3ed5ccc729f668d91189d83`
- **Phase 1b status:** `GATE 2 ACCEPTED / MILESTONE C COMPLETE / PHASE 1B COMPLETE`
- **Phase 2 status:** `START PREFLIGHT / INVENTORY NOT STARTED`
- **OpenSpec:** handoff時点でRepositoryに`openspec/`は存在しない

> **Superseded scope notice (2026-08-31):** 本書は開始時点のhistorical baselineとして保持する。
> 後続のArchitecture Owner判断により、Oracleは採用確度の低い将来optional `P4-ORACLE`へ再配置された。
> 本書内のPhase 2 Oracle image、Driver、共通DDL、DoD 2-11 / 2-12、nightlyおよびPhase 4必須Oracle Integrationの記述は
> 現行計画ではない。現行判断はGrand Design、ADR-010 / ADR-044、Phase 2実行計画およびstart preflightを正本とする。

PR #27のmerge commit `b2e2123`に対する最終CI run `33313844608`とJava Runtime Compatibility
run `33313844638`はSUCCESSである。local `main`を`origin/main`へfast-forwardした後、同commitから本branchを作成した。

本書の追加はPhase 2のproduction code、Public API、migration、module構成または最小実装案の承認を意味しない。
最初にpreflight／inventoryを行い、変更対象、検証方法、外部環境、分割案をOwnerへ提示してから実装へ進む。

## 2. Authoritative sources and reading order

新規sessionでは次を順に読む。

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`
   - §9 Ownership／Public API
   - §14 Security
   - §15 Audit
   - §16.7 Migration ownership
   - §16.8 Oracle compatibility
   - §19.2 single execution
   - §21 Test／CI
   - §26.3 Reference `identity`
   - §27.2、§27.6 Phase 2 DoD
5. `docs/architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md` §2、§5
6. `docs/architecture/adr/README.md`と対象ADR
7. `docs/architecture/validation/phase1b-closeout.md`
8. `docs/development/KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md`
9. 実効`pom.xml`、`.github/workflows/`、既存Starterとverification tooling

設計文書と実装が競合する場合は推測で整合させず、実装証拠、影響範囲、Ownershipと候補案をOwnerへ提示する。
業務機能であるReference `identity`を設計・実装・reviewする段階では、追加で
`docs/agent/skills/koiki-business-feature-work/SKILL.md`を使用する。

## 3. Inherited Phase 1b baseline

### 3.1 Formal artifacts

Phase 1b closeoutでroot aggregatorを除く次の9成果物を内部snapshotとして公開し、空のMaven repositoryから
resolve後、独立Consumerの`clean verify`まで成功している。

1. `koiki-dependencies-bom`
2. `koiki-parent`
3. `koiki-architecture-contract`
4. `koiki-archunit-rules`
5. `koiki-starter-api`
6. `koiki-starter-data`
7. `koiki-starter-data-jpa`
8. `koiki-starter-observability`
9. `koiki-testing`

Phase 2開始時点のbaselineはSpring Boot 4.1.1、Java build baseline 21、Java 21／25 runtime matrix、
Maven Wrapper 3.9.16である。BOMと全module POMにはSpring Security、Spring Session、OAuth2の依存はまだ存在しない。
依存追加前にSpring Boot 4.1.1のdependency managementと公式互換性を再確認する。

### 3.2 Reusable capabilities

- Framework／CustomerのFlyway二階層と独立history
- PostgreSQL Testcontainers integration
- OSIV無効化
- Problem Details、Validation、Jackson 3、API Versioning
- structured logging、correlation、TaskDecorator
- Actuator health contract
- Web process外の単一実行contractと複数process排他
- Public API compatibility、NullAway、ArchUnit、Java runtime guards
- packaged Consumer journeyとCP10 aggregate verification
- CP9公式performance baseline

Phase 2の期限切れsession cleanupはPhase 1bの単一実行contractへ接続する。既存Consumer、performance fixture、
Walking SkeletonをFramework／正式Referenceへ直接昇格させない。

### 3.3 Remote evidence to preserve

| Evidence | Result |
|---|---|
| Phase 1b implementation merge | PR #26、merge `40d16f9` |
| Final closeout merge | PR #27、merge `b2e2123` |
| Final merge CI | run `33313844608`、SUCCESS |
| Final runtime compatibility | run `33313844638`、SUCCESS |
| Phase 1b snapshot | run `33311794583`、SUCCESS、9 artifacts |
| Protected environment | `phase1b-internal-snapshot`、main限定、required reviewerあり |

Phase 2開始のためにPhase 1b snapshotやCP9公式baselineを再採取・上書きしない。変更影響を確認するsmokeと、
Phase 2固有の新しいEvidenceを分離する。

## 4. Phase 2 scope and ownership

### 4.1 In scope

- Spring Security標準構成
- local User／Role／Permission
- HTTP SessionとSpring Session JDBC
- Password／Lock／Resetと認証試行制御
- CSRF、Cookie、Security Headerの安全な既定値
- Audit Event 3分類とtransaction挙動
- OIDC Loginとlocal loginの共存
- OAuth 2.0 Resource ServerによるBearer JWT検証
- Security integration test
- Reference Applicationの`identity` module
- Framework所有table／migration
- Oracle互換SQL review規約とOracle nightly smoke
- Framework管理の第三者table例外一覧
- OpenRewrite recipeの試作開始
- Phase 2で新設する規約のADR／Agent Skills／CI反映

### 4.2 Ownership boundary

| Owner | Phase 2で所有するもの |
|---|---|
| Framework | User／Role／Permission標準model、table／migration、Password／Lock／Reset、Session失効、認証試行制御、Audit API、外部IdP link Port、Securityの安定した共通契約 |
| Reference | `identity` Tier 1 JPA moduleの最小管理画面とUse Case。Framework所有tableを正しい公開contractから操作する利用例 |
| Customer | 社員番号、所属、雇用状態、顧客固有Role／Permission、provisioning、人事連携、IdP固有claim mapping、業務固有authorization policy |
| Walking Skeleton／Tooling | 実装可能性やfailure pathを確認するfixture。正式成果物へ直接昇格しない |

Reference `identity`のtableはFramework所有であり、Referenceは管理画面とUse Caseを所有する。Customer tableから
Framework tableへ外部キーを作らず、識別子の値と公開contractで連携する。

### 4.3 Explicitly deferred

- Phase 3の`master`／`expense`、Thymeleaf＋HTMX、業務Vertical Slice
- Spring Modulith Level 1／2とdurable async
- Phase 4のSPA固有token保管、refresh、logout／revocation、double-submit CSRF詳細
- Authorization Server、token発行、Refresh Token lifecycle
- SAML extension、MyBatis accounting、Batch、外部API、Object Storage、OTEL、ECS固有構成
- Oracle本番正式support。Phase 2はOracle Freeによる設計適合smokeだけを扱う
- Customer固有identity属性・claim mapping・業務認可policy
- 正式release、Project Template、Production Baseline

## 5. Phase 2 Definition of Done

全Phase共通DoDとして、最新のサポート対象Spring Boot minor、ADRとOwner承認、全CI Gate、Skill反映、
table／Flyway ownershipを満たす。Phase 2固有DoDは次の12項目である。

| DoD | Acceptance outcome |
|---|---|
| 2-1 | 未認証accessがURL securityとMethod Securityの双方で拒否される |
| 2-2 | UI制御を回避した直接requestでもRole／Permission認可が強制される |
| 2-3 | OIDC loginとlocal loginが共存する |
| 2-4 | Bearer JWT API認証で署名、issuer、audience、期限、scopeを検証できる |
| 2-5 | 2 instanceでsessionを共有し、一方停止後もsessionが維持される |
| 2-6 | login失敗閾値でlockされ、security auditが業務rollbackに巻き込まれない |
| 2-7 | 業務transaction rollback時にbusiness auditもrollbackする |
| 2-8 | expired-session cleanupがPhase 1bの単一実行基盤から起動する |
| 2-9 | CSRFとSecurity Headerが既定有効で、無効化は明示設定を要する |
| 2-10 | Reference `identity`からFramework所有tableを操作できる |
| 2-11 | Oracle互換SQL規約がreview項目として継続適用される |
| 2-12 | Oracle nightlyでFlyway、基本CRUD、paging、optimistic lockを確認できる |

**2-6と2-7のrollback挙動の対比がPhase 2の核心である。** テスト名や文書上の分類だけではなく、
実DB transactionの成功／失敗状態としてEvidenceを残す。

## 6. Provisional internal milestones

Phase 0のfeasibilityはPhase 2を「要分割」と判定している。開始rangeの150〜246標準人日、
AI支援Owner 90〜185日は期限やcommitmentではなく、初期の規模比較値である。Phase 1a／1bの実績を用いて
preflight時にcontingency、AI支援係数、PR分割を再見積もりする。

| Milestone | Scope | Principal DoD |
|---|---|---|
| A Authentication／Authorization profiles | SecurityFilterChain、URL／Method Security、OIDC、Bearer Resource Server、CSRF／Header defaults、integration test | 2-1〜2-4、2-9 |
| B Local Identity／Session／Audit | Local Identity、Password／Lock／Reset、Spring Session JDBC、2 instance、認証試行、audit 3分類、cleanup、Reference `identity` | 2-5〜2-8、2-10 |
| C Oracle／Migration support | Oracle SQL規約、Oracle nightly、第三者table一覧、OpenRewrite試作、closeout | 2-11、2-12、全Phase共通DoD |

Milestone境界とCP番号はpreflight結果を受けてOwnerが承認する。外部環境の準備は並行できるが、
Milestone B／Cのproduction実装をGate前に先行させない。

## 7. Start gates

### 7.1 Gate P2-1 — preflight／inventory

次をread-only中心に棚卸しし、事実、未決定、推奨案、代替案、検証方法を分離して記録する。

1. **Repository／release unit**
   - 現在の9成果物、root reactor、Starter責務、Consumer／Tooling境界
   - Security機能を置く既存module／新規module／Starter候補
   - 配布単位、Public Java API、configuration properties、auto-configuration候補
2. **Dependency baseline**
   - Spring Boot／Security／Session／OAuth2／Testcontainers Oracleの公式対応version
   - BOM管理可能範囲、追加dependency tree、license、Java 21／25 runtime影響
   - Spring Security 7 MFAをPhase 2標準へ含めるかの判断材料
3. **Authentication／authorization profiles**
   - Session、local login、OIDC、BearerのSecurityFilterChain境界とmatcher
   - default deny、URL／Method Security、Permission表現、resource ownershipの責務
   - issuer／audience／scope、clock、JWKS／test key、negative path
4. **Identity／table／migration**
   - User／Role／Permission、join、password、reset、lock、login attempt、audit table inventory
   - ID、timestamp、Boolean、version、index、retention、PII、secretを保存しない境界
   - `koiki_`接頭辞、Spring Session table名、第三者table例外、Flyway location／history
   - PostgreSQL／Oracle共通DDLの成立性。vendor分岐は実測で必要になるまで導入しない
5. **Session／single execution**
   - Spring Session JDBCのsave／flush mode、cookie、fixation、logout、serialization
   - 2 instance failover fixture、DB書込み負荷、session cleanupの起動・排他・競合
6. **Audit transaction**
   - business audit、security audit、副作用・連携のAPIとtransaction boundary
   - `REQUIRES_NEW`、rollback、failure時の業務成立条件、standard fields、機密値除外
   - Password変更／Resetを経路別にどの分類へ割り当てるか
7. **External environments／credentials**
   - OIDC test provider、redirect URI、test client、claim fixture
   - Oracle edition／version、container image、JDBC Driver、license、CI secret、runner capability
   - PostgreSQL上の2 instance構成、port／container cleanup、CI実行時間
8. **Test／CI／evidence**
   - focused、negative、integration、2 instance、nightly、packaged journeyのtest pyramid
   - required checkへ追加する時期、nightly failureの扱い、artifact／log retention
   - secret、Token、Password、stack traceをlog／Problem Details／artifactへ残さない検査
9. **Governance**
   - ADR-007〜009、020、029、036、041、042、044等と実装案の整合
   - 新規ADRが必要な判断と、既存ADRの実装証拠だけで足りる事項
   - Security／business feature用Agent Skillの新設・更新範囲
   - Phase 1b actualを反映した工数、milestone、CP、PR、Gate計画

### 7.2 Gate P2-2 — minimum implementation approval

Gate P2-1結果から、少なくとも次をOwnerへ提示する。

- Phase 2実行計画と内部milestone／CP
- module／artifact／package／table ownership
- Public API／configuration property候補と非公開境界
- 採用dependencyと除外dependency
- migration／Oracle strategy
- OIDC、JWT、2 instance、Oracleのtest environment
- focusedからaggregate、PR、main、nightlyまでの検証command
- credential、remote operation、ruleset、environment approvalの境界
- ADR／Skill／README／validation文書の変更対象
- 実装する最小差分、明示的なdeferred scope、stop conditions

OwnerがGate P2-2を承認する前にproduction code、Public API、production migration、workflow required check、
remote environment、secret、package publicationを変更しない。

## 8. Decisions and external readiness required

| Topic | Required decision／evidence |
|---|---|
| OIDC | test provider、issuer、client、redirect URI、claim fixture。provider固有mappingはCustomerへ残す |
| Bearer JWT | issuer／audience／scope contract、test JWKS。Authorization Serverはscope外 |
| MFA | Spring Security 7標準機能のPhase 2採否。採用しない場合も再判断条件を記録する |
| Identity contract | Framework tableをReferenceが操作する公開境界。schema直接依存を避ける |
| Audit API | business／securityの呼出契約とtransaction propagation、failure semantics |
| Spring Session | table名、save／flush mode、cleanup、cookie、serialization、2 instance fixture |
| Oracle | edition／version、image、driver、license、nightly runner、secretとretention |
| CI governance | nightlyとrequired checkの分離、ruleset追加時期、flaky／external outageの扱い |

外部環境が準備できない場合、対応DoDを黙って削除しない。blocker、代替fixture、残るrisk、hold／exception、
再開条件をOwnerへ提示する。

## 9. New session startup

Repository rootで次を実行する。

```powershell
git branch --show-current
git status --short --branch
git log -8 --oneline --decorate
git rev-parse HEAD
git merge-base HEAD main
java -version
.\mvnw.cmd -version
docker version
```

期待状態:

- branchは`feature/phase2-security-foundation`
- handoff作成時のbaseは`b2e2123605e4d971c3ed5ccc729f668d91189d83`
- merge baseは`b2e2123605e4d971c3ed5ccc729f668d91189d83`
- handoff commit後のworktreeはclean
- Java 21、Maven Wrapper 3.9.16、Docker Linux daemonを利用可能
- Spring Boot baselineは4.1.1だが、Phase開始preflightで最新サポートminorを公式情報から再確認する

remote確認が必要でも最初は`git fetch origin`までとし、自動pull／reset／pushを行わない。Phase 2 inventoryのために
CP10 aggregate、CP9公式performance計測、snapshot publishを再実行しない。

## 10. First work in the new session

1. Repository／environment identityと本handoff baseを確認する。
2. 正本、Phase 1b closeout、ADR、POM、workflowを読む。
3. Gate P2-1の9 inventory領域を、確定／未決定／外部依存／推奨案に分類する。
4. 現在のdependency tree、module、Public API、configuration property、migration、CI jobを機械的に採取する。
5. Spring Boot／Security／Session／OAuth2／Oracle Testcontainersの公式baselineを確認する。
6. Ownership、SecurityFilterChain、table、Audit transaction、test topologyの候補を比較する。
7. Phase 1b actualを踏まえ、Milestone A／B／C、CP、PR、Gateを再見積もりする。
8. Gate P2-2案をOwnerへ提示する。承認後だけ最小実装へ進む。

## 11. Stop conditions

- Spring標準で満たせる機能を独自Security frameworkとして作る。
- local login、OIDC、Bearer、SPA、Authorization Serverの責務を1つのfilter chainへ無計画に混在させる。
- UIの非表示、route guard、JavaScriptだけを認可境界にする。
- Customer固有identity属性、claim mapping、業務policyをFrameworkへ入れる。
- Reference `identity`にFramework table／migrationの所有権を移す。
- Token、Password、client secret、private key、個人情報をsource、log、文書、test resultへ残す。
- Spring SessionのWeb instance内cleanupと単一実行jobを競合させる。
- audit分類を名前だけで実装し、2-6／2-7のrollbackを実DBで検証しない。
- Oracle vendor分岐を共通DDLの不成立確認前に導入する。
- Oracle Free smokeを本番Oracle正式supportと表現する。
- Walking Skeleton、Phase 1b Consumer、fixtureを正式成果物へ直接昇格する。
- Phase 3／4／5成果物、SPA固有方式、Level 2、cloud固有実装を先行する。
- Gate P2-2承認前にproduction差分、push、ruleset、environment、secret、publishを進める。

該当時は変更を拡大せず、事実、Evidence、影響範囲、Ownership、代替案、再判断条件をOwnerへ提示する。

## 12. Prompt for the next AI session

次の文章を新しいsessionの最初の依頼として使用できる。

> KOIKI-JavaWeb-FWのPhase 2 Security Foundation開始前preflight／inventoryを開始します。
> Repository rootの`AGENTS.md`、`docs/agent/skills/koiki-project-overview/SKILL.md`、
> `docs/development/phase2-security-foundation-start-handoff-20260830.md`、Grand Design §14、§15、
> §16.7〜16.8、§26.3、§27.6、Phase Estimate Feasibility §5、ADR register、Phase 1b closeoutを確認してください。
> branchは`feature/phase2-security-foundation`、baseline main commitは`b2e2123`です。最初にGit／Java／Maven／Docker状態を
> 確認し、Gate P2-1としてrelease unit、dependency baseline、認証profile、Local Identity、table／migration、Spring Session、
> audit transaction、OIDC／JWT／Oracle外部環境、test／CI、ADR／Skills、Phase 1b実績に基づくmilestone／CP分割をinventoryしてください。
> Reference `identity`を扱う段階では`koiki-business-feature-work`も使用してください。Gate P2-2のOwner承認前にproduction code、
> Public API、production migration、workflow、remote environment、secret、snapshotまたはSkill正本を変更せず、pushもしないでください。
> Phase 1b snapshotとCP9公式baselineは再採取しません。

## 13. Handoff judgment

- Phase 1bのlocal／remote／snapshot closeoutは完了し、Phase 2の依存基盤として利用できる。
- Phase 2は技術的に実行可能だが、Security、identity、session、audit、Oracleを含むため3内部milestoneへ分割する。
- 最初の作業はGate P2-1 preflight／inventoryであり、Security実装ではない。
- OIDC provider、固定Oracle環境、2 instance test環境、threat modelはPhase 2開始条件として確認する。
- Framework／Reference／CustomerのOwnershipと2-6／2-7のtransaction挙動を最優先で保護する。
- 本handoff時点でPhase 2の最小実装、Public API、migration、workflow変更、remote操作は未承認である。

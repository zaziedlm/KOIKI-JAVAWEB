# KOIKI-JavaWeb-FW Phase 1a Walking Skeleton引継ぎ台帳

**版:** v0.1  
**棚卸日:** 2026年8月21日  
**状態:** ACCEPTED  
**承認日:** 2026年8月21日  
**Phase 1a C5処置状態:** Gate 1 ACCEPTED / Gate 2 READY FOR OWNER REVIEW（2026年8月27日）<br>
**Architecture Owner:** Shuichi Kataoka  
**対象比較:** `main` (`73dcb82`) / `walking-skeleton` (`b3ba79f`)  
**共通祖先:** `55a4359`

## 1. 目的

Phase 0 Walking Skeletonで得た成果を、Phase 1a Build Foundationの正式成果物へ
機械的に昇格させないため、`main...walking-skeleton`の差分を引継ぎ方針別に分類する。

本台帳は実装の取込指示ではない。正式なMaven座標、Public API、Module ownership、
CIおよび配布形態は、Phase 1aの設計と検証を経て確定する。

## 2. 比較結果

- `main`だけに存在するcommit: 5件
- `walking-skeleton`だけに存在するcommit: 28件
- `main`から見たWalking Skeleton側の変更: 185 files、10,838 insertions、206 deletions
- `main`と`walking-skeleton`は共通祖先以降に分岐しており、branch全体をmergeしない。
- 共通祖先ですでに`main`へ入っている初期Walking Skeletonも、差分とは別に整理対象とする。

## 3. 分類定義

| 区分 | 意味 | Phase 1aでの扱い |
|---|---|---|
| A: 正式化 | Phase 0で承認済みの判断・正本文書・継続利用するRepository運営契約 | `main`を基準に内容とlinkを照合して正式配置する |
| B: 再設計・再実装 | 技術的成立は検証済みだが、正式座標・Ownership・Public APIで作り直す対象 | Walking Skeletonを仕様・test観点として参照し、新しい正式成果物として実装する |
| C: 検証証拠としてのみ参照 | Phase 1aの判断根拠には使うが、成果物自体を正式コードへ持ち込まない対象 | `walking-skeleton` branchと検証記録を参照し、直接copyしない |
| D: Phase 1a対象外 | 後続Phaseまたは別のGovernance判断で扱う対象 | Phase 1aのReactor、BOM、Public API、CIへ先行導入しない |

## 4. A: 正式化する対象

**Owner Review:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**承認scope:** Phase 0で承認されたArchitecture Baseline、Governance、仕様、用語、検証証拠、
完了済み計画、およびKOIKI固有SkillをPhase 1aの正式本線へ配置する範囲

この承認は、後続Phaseの実装開始、Walking Skeleton codeの昇格、またはOpenSpecを
Phase 1aの必須toolingとする判断を意味しない。

| 対象 | 取扱い | 理由・注意 |
|---|---|---|
| `docs/architecture/grand-design/` | 承認済み版を正式配置 | Phase 0 Architecture Baselineの上位設計 |
| `docs/architecture/adr/README.md` | 承認記録を正式配置 | 有効ADR 43件のregisterと証拠scopeを維持する |
| `docs/architecture/KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md` | 正式配置 | Phase 0全体のCOMPLETE判定 |
| `docs/architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md` | 正式配置し、Phase 1a開始時の再見積り入力にする | Phase 1aの内部milestone A〜Cと初期rangeを定義済み |
| `docs/architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md` | 正式配置し、実効POMと再照合 | baselineは新versionの存在だけでは更新しない |
| `docs/architecture/governance/` | 正式配置 | Owner Review、例外、Phase判定の手続きを維持する |
| `docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md` | 正式配置 | `org.koikifw`、Ownership、Walking Skeleton非昇格を明記済み |
| `docs/standards/KOIKI-JavaWeb-FW_Glossary_v0.1.md`とindex | 正式配置 | Phase 1aの命名・責務用語の基準 |
| `docs/reference/` | 正式配置 | 後続Phaseの実行可能なArchitecture仕様。Phase 1aで実装はしない |
| `docs/architecture/validation/` | Phase 0の履歴証拠として正式配置 | code昇格の根拠ではなく、再実装時の受入観点として使う |
| `docs/development/KOIKI-JavaWeb-FW_WalkingSkeleton実装計画_v1.0.md` | 完了済み履歴計画として正式配置 | 未完のPhase 1a計画として再利用しない |
| `docs/agent/skills/koiki-project-overview/` | 正本として正式配置 | Phase・Ownership・正本選択の横断手順 |
| `docs/agent/skills/koiki-business-feature-work/` | 正本として正式配置 | 後続の業務機能作業で利用。Phase 1aで業務実装はしない |
| `.agents/skills/koiki-*`、`.claude/skills/koiki-*` | 薄いadapterとして正式配置 | 設計規則をadapterへ複製しない |
| `docs/agent/README.md`、`docs/agent/skills/README.md` | 正式配置 | Skill正本とadapterの関係を明示する |
| 各architecture / standards / reference index | linkを照合して正式配置 | 正本文書を探索可能にする |

### 4.1 正式配置時の承認条件

1. `docs/agent/README.md`の「Phase 0で検証する」を、Phase 0で検証済みであり正式工程でも
   継続利用することが分かる表現へ更新する。
2. `docs/agent/skills/README.md`の「Phase 0ではこの2種だけ」を履歴表現へ変更し、Skill追加は
   各Phaseで実証が必要な判断手順に限定する。
3. `koiki-project-overview`における`openspec/`は、存在する場合だけ参照する任意のchange正本とし、
   OpenSpecをPhase 1aの必須前提にしない。
4. KOIKI Skill正本、薄いadapter、Phase 1a向け`AGENTS.md`・`CLAUDE.md`を同一作業単位で反映し、
   一時的な参照切れを作らない。
5. A区分の配置後に、Validation、Closeout、各indexを含む相対linkを一括検査する。

承認済みのPhase見積もりv0.1は変更せず、Phase 1a開始時の再見積もりは別の実行計画または
改訂履歴として管理する。`docs/architecture/validation/`は規範ではなくPhase 0時点の履歴証拠、
`docs/reference/`はReference Ownershipの正式仕様として扱い、いずれもPhase 1aの業務実装scopeを
拡張する根拠にはしない。

## 5. B: 再設計・再実装する対象

**Owner Review:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**承認scope:** Phase 0で成立を確認したbuild・Architecture Contract・ArchUnit・Null Safety・
配布検証を、正式座標、Public API、独立配布およびCIを前提として再設計・再実装する範囲

| Walking Skeleton側の対象 | Phase 1aの正式成果 | 再実装時の境界 |
|---|---|---|
| `pom.xml` | 正式Root Reactor | `dev.koiki.walkingskeleton`と`ws-*` moduleを除き、必要な正式moduleだけを列挙する |
| `koiki-parent/pom.xml` | 正式Parent | `main`側のError Prone fork設定を保持し、Plugin管理、Enforcer、Java 21 build contractを再確認する |
| `koiki-dependencies-bom/pom.xml` | 正式BOM | Spring Boot等の承認baselineとSpring Modulith Level 0の2.1.0を反映する。Level 1 / 2の依存と採用方式は先行固定しない |
| `BUILD-BASELINE.json` | 実効build baseline | Baseline正本文書、POM、Wrapper、CI matrixとの重複と責務を整理する |
| `.mvn/wrapper/maven-wrapper.properties`、`mvnw`、`mvnw.cmd` | 正式Maven Wrapper | 公式Wrapperから再生成し、Maven 3.9.16を検証する |
| `build-support/`のbuild・class version・Java runtime検証 | 正式build support | `main`側の社内SSL proxy対応を失わず、WindowsとCIの両経路で再検証する |
| `walking-skeleton/ws-architecture-contract/` | 正式Architecture Contract artifact | 一時packageと座標を使用せず、annotationをPublic APIとして個別reviewする |
| `koiki-archunit-rules/` | 正式ArchUnit rules artifact | 検証済み実装を仕様・test観点として用い、正式contractと全対象規則に合わせて再構成する |
| `walking-skeleton/archunit-external-consumer/` | Reactor外の外部Consumer検証 | local installや同一reactorに依存せず、正式artifact repository経由で検証する |
| `walking-skeleton/negative-tests/nullaway/` | NullAway negative test | test fixtureまたは専用検証手順として再構成し、通常sourceへ意図的違反を混入しない |
| Walking Skeleton差分に存在しない新規成果物 | Tier 1 / Tier 2 Feature Template | DoD 1a-1を満たす最小構成とし、未使用module・packageを先行生成しない |
| Walking Skeleton差分に存在しない新規成果物 | CI骨格 | `mvn verify`、Architecture検査、NullAway、互換性検査を再現可能な経路にする |
| Walking Skeleton差分に存在しない新規成果物 | japicmpとPublic API baseline | `internal`を固定対象から分離し、承認済みPublic APIだけを対象にする |
| Walking Skeleton差分に存在しない新規成果物 | snapshot artifact公開経路 | 外部releaseを意味しない内部検証用repositoryとして、同一versionの成果物を公開する |
| Walking Skeleton差分に存在しない新規成果物 | Java 21 / 25 runtime matrix | Java 21で生成した同一成果物を再compileせず両runtimeで検証する |
| `koiki-dependencies-bom/pom.xml`のSpring Modulith候補 | Level 0のtest dependency管理 | 承認baseline 2.1.0を使用し、`spring-modulith-starter-test`だけをtest scopeで利用する。Level 1 / 2とruntime依存は導入しない |
| `.gitignore`、`.vscode/settings.json` | Contributor環境設定 | `JAVA21_HOME`前提とWindows限定scopeを明記し、build contractそのものとは分離する |
| `AGENTS.md`、`CLAUDE.md`、Repository root `README.md` | Phase 1a向けRepository guidance | Phase 0の実行案内を残さず、正本・Ownership・正式build手順へ書き換える |
| `koiki-archunit-rules/README.md` | 正式artifact利用文書 | Walking Skeleton、一時座標、将来予定という記述を正式契約へ置き換える |

B区分は原則としてcommitのcherry-pickやdirectory copyを行わない。必要な設定値、失敗事例、
negative test、受入条件を抽出し、`main`側の変更を保持したまま実装する。

### 5.1 再設計・再実装時の承認条件

1. Walking Skeleton差分に存在しないFeature Template、CI骨格、japicmp、snapshot artifact公開、
   Repository外Consumer検証、およびJava runtime matrixもPhase 1a成果物に含める。
2. Spring ModulithはPhase 1aのLevel 0としてB区分で扱い、承認baseline 2.1.0と
   `spring-modulith-starter-test`のtest scopeに限定する。Level 1 / 2とruntime依存は導入しない。
3. Architecture Contractはannotation、enum、artifactId、`org.koikifw` package、既定値、
   retention、targetを個別reviewし、japicmp対象となるPublic APIとして設計する。
   `JDBC`や`SEPARATED`を未検証の実装方式として先行固定しない。
4. ArchUnitの正式Public APIへ`phaseZeroRules`等の検証Phase名を固定しない。DoD 1a-2の
   代表違反5件を必須とし、規則ごとの適用Phaseを整理してから正式rule setを確定する。
5. 外部ConsumerはRoot Reactor、local install、local Maven Repositoryへ依存せず、正式な
   snapshot artifact repositoryから同一versionのBOM、Parent、Architecture Contract、
   ArchUnit Rulesを解決して意図的違反を検出する。
6. Maven基盤は`main`を基準に再構成し、Error Proneの`fork=true`、社内SSL proxy・証明書対応、
   Root Reactor / Parent / BOMの責務分離を保持する。Wrapperは公式手段で再生成する。
7. Build JDKとtarget bytecodeをJava 21、runtimeをJava 21 / 25とし、Java 25検証では
   再compileしない。NullAwayはPhase 1a開始時から適用し、負例をproduction sourceへ混入しない。
8. `.vscode/settings.json`は`JAVA21_HOME`を利用するWindows開発者向け補助設定とし、
   Maven、CI、Consumerへ要求する正式build contractとは扱わない。
9. `AGENTS.md`、`CLAUDE.md`、Root READMEはA区分のKOIKI Skill正本・adapterと同一作業単位で
   Phase 1a向けに更新し、参照切れを作らない。

正式Maven version、Architecture ContractのartifactId・package・API、artifact repository、
CI platform、およびjapicmp baselineは、この承認だけで確定したものとは扱わず、各実装着手前に
設計判断として確認する。

## 6. C: 検証証拠としてのみ参照する対象

**Owner Review:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**承認scope:** Walking Skeletonの既存code、fixture、作業計画およびOpenSpec成果を、
固定Commit上の検証証拠として保持し、正式成果物へ直接取り込まない範囲

| 対象 | 参照する知見 | 正式成果物へ持ち込まないもの |
|---|---|---|
| `walking-skeleton/ws-smoke-lib/` | Multi-module、NullAway、Java 21 class生成 | `GreetingService`、一時座標、`dev.koiki.walkingskeleton` package |
| `walking-skeleton/ws-smoke-app/` | executable JAR、Java 25 runtime、layer抽出、non-root container | `SmokeApplication`、検証用properties、`Dockerfile.ws` |
| `walking-skeleton/ws-architecture-contract/` | annotationでTier等を宣言できること | 一時artifactと実装classの直接copy |
| `koiki-archunit-rules/src/**` | 規則の成立性、誤検出、error message、fixture設計 | 検証時点のsourceを無審査でPublic API化すること |
| `walking-skeleton/archunit-external-consumer/` | 外部projectから規則を解決・実行できること | 一時repository、固定されたcustomer fixture |
| `walking-skeleton/Walking-Skeleton-plan-20260812.md` | V1〜V7の検証意図と実行履歴 | Phase 1aの実装計画としての再利用 |
| `openspec/changes/archive/2026-08-13-expense-tier2-walking-skeleton/` | OpenSpec試行と要件・実装traceability | expense changeをPhase 1a changeとして流用すること |
| `openspec/specs/expense-*`、`module-event-*`、`application-module-*` | Phase 0実験で確認した期待behavior | Phase 1aの正式Framework仕様としての取込 |

同じpathがBとCの両方に現れる場合、Cは既存fileの扱い、Bはそこから得た知見を使って作る
新しい成果物の扱いを示す。既存file自体を正式化する意味ではない。

`docs/development/KOIKI-JavaWeb-FW_WalkingSkeleton実装計画_v1.0.md`はA区分の完了済み正式履歴計画、
`walking-skeleton/Walking-Skeleton-plan-20260812.md`はC区分の検証作業メモである。後者を
Phase 1aの実装計画として再利用しない。

### 6.1 検証証拠を参照するときの承認条件

1. Evidenceは可変のbranch名だけで参照せず、最終比較snapshot `b3ba79f`、各Validation文書の
   基準Commit、および後続の訂正Commitを記録する。
2. `main`に既存の`ws-smoke-*`、NullAway負例資料、Walking Skeleton作業メモは、B区分の
   正式な代替成果物と同等検証が成功した後、Root ReactorとPhase 1aの正式本線から除去する。
   `walking-skeleton` branchとGit履歴には証拠として保持する。
3. A区分の完了済み正式履歴計画と、C区分のWalking Skeleton検証作業メモを混同しない。
4. BとCで同じ概念または将来pathを扱う場合も、Cは固定Commit上の既存内容、Bは正式座標・
   Public APIで新規に構成する成果物として区別する。
5. OpenSpec成果はPhase 0のprocess・traceability・strict validationの証拠に限定し、Expense spec、
   directory構成、changeをPhase 1aのFramework仕様または必須toolingとして再利用しない。
6. C区分のcode、SQL、Template、Dockerfile、fixtureを正式Root Reactor、公開JAR、source JAR、
   Project Templateまたは外部Consumer向け成果物へ含めない。受入観点、失敗条件、設定知見だけを
   B区分へ反映する。
7. Phase 0証拠の誤りを発見した場合は過去Commitを書き換えず、新しいValidation記録、影響する
   ADR・正本の再review、およびPhase 1a判断への影響をGovernanceに従って記録する。

### 6.2 Evidence Provenance

Walking Skeletonの検証証拠は、可変のbranch名だけでなく次の固定Commitから参照する。
`b3ba79f`をA区分の正式配置元と比較した最終snapshotとし、個別文書の内容と後続訂正は
次の対応で追跡する。

| Evidence | 記録Commit | 補足 |
|---|---|---|
| Agent Skills Validation | `3bfac88` | Phase 0完了時のREADME・Validation整合を含む |
| ArchUnit Rules and Distribution Validation | `d7706ba` | V1主要規則の検証記録。ADR参照訂正は`681c406` |
| Build Foundation Validation | `9954cc4` | Build FoundationのCompleted更新 |
| Flyway Two-tier Validation | `5e3882a` | Walking Skeleton検証状態との同期を含む |
| Phase 0 Completion | `3bfac88` | Walking Skeleton全体の完了記録 |
| Tier 2 Practicality Validation | `3bfac88` | Phase 0完了時の最終Validation文書 |
| Expense Tier 2 OpenSpec archive | `ebe7cb3` | archive済みchangeとmain spec同期の証拠 |
| 最終比較snapshot | `b3ba79f` | Phase 0 Closeoutを含む`walking-skeleton` branchの固定snapshot |

本表の記録Commitは正式成果物へcodeを移植する起点ではない。B区分の再実装時に、検証条件、
失敗例、訂正履歴および判断根拠を再現するための参照点として使用する。

## 7. D: Phase 1a対象外

**Owner Review:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**承認scope:** Phase 1aをBuild Foundationへ限定し、Runtime、Security、Reference業務、
Enterprise IntegrationおよびProduction Baselineを所定の後続Phaseへ送る範囲

| 対象 | 送付先・判断時期 | Phase 1aで行わないこと |
|---|---|---|
| `walking-skeleton/ws-flyway-two-tier/`、Problem Details、Validation、Jackson 3、Resilience、構造化log、Actuator、OSIV、Testcontainers、単一実行基盤、性能baseline | Phase 1b | Runtime FoundationのStarter、設定、運用contractを先行実装すること |
| `walking-skeleton/ws-tier2-practicality/`、正式Reference、MVC / HTMX、最小REST API、Spring Modulith Level 1、MyBatis詳細規約 | 主にPhase 3。一部runtime境界はPhase 1b | expense / masterdata、Controller、Template、JPA Entity、migration SQL、同期eventを正式Referenceへ昇格すること。Level 0の検証方法だけはB区分で参照できる |
| Identity、認証・認可、Session、監査、CSRF、Security Header | Phase 2 | Security profile、table、Starterまたは仮Public APIを先行生成すること |
| React SPA、Spring Modulith Level 2、非同期event、外部I/O、Batch、Oracle統合 | Phase 4 | Node.js frontend build、耐久event配信、外部連携contractを先行実装すること |
| Project Template、正式Container / Cloud Deployment、OpenRewrite、SBOM、Support、正式release | Phase 5 | Feature Templateを顧客Repository生成用Project Templateへ拡張すること、または内部snapshotを正式releaseと扱うこと |
| `.agents/skills/openspec-*` | OpenSpec正式運用を決めるchange | OpenSpec試行用SkillをKOIKIの正式build成果物とみなすこと |
| `.claude/commands/opsx/`、`.claude/skills/openspec-*` | 同上 | agent別生成物を無条件に正式配置すること |
| `.agents/skills/.openspec-target`、`openspec/config.yaml` | OpenSpec正式配置のGovernance判断 | Phase 1a開始条件として固定すること |
| `.node-version` | Node.jsを必要とする正式tooling導入時 | OpenSpec試行だけを理由にPhase 1aのbuild前提へNode.jsを加えること |

### 7.1 Phase 1a対象外を維持する承認条件

1. Flyway以外のProblem Details、Validation、Jackson 3、Resilience、構造化log、Actuator、OSIV、
   Testcontainers、単一実行基盤、性能baselineもPhase 1bへ送り、Phase 1aへ先行導入しない。
2. Spring ModulithはLevel 0をB区分のPhase 1a、Level 1をPhase 3、Level 2をPhase 4として分離する。
   `ws-tier2-practicality`の業務codeはD区分とし、Level 0の検証方法だけをB区分で参照できる。
3. OpenSpecはPhase 1a changeの任意の計画支援として利用できるが、Repository必須構成、正式build成果物、
   Maven / CI / Consumerの前提にはしない。
4. Node.js 24 LTSと`.node-version`はOpenSpecまたは将来のSPA tooling向けとし、Parent、BOM、
   Maven検証およびPhase 1a CI runnerの必須build contractから分離する。
5. Phase 1aのTier 1 / Tier 2 Feature Templateと、Phase 5の顧客Repository生成用Project Templateを
   区別し、Phase 1aで`koiki-project-template`全体を先行実装しない。
6. D区分の成果物について、将来必要になるという理由だけで空Maven module、空package、仮Starter、
   仮Public APIまたは未使用dependencyを生成しない。
7. Phase 1aの外部Consumer検証用snapshot公開は内部検証であり、一般公開release、Support開始、
   Public Repository公開またはRuntime Foundation完成とは扱わない。
8. `Dockerfile.ws`はPhase 0の証拠とし、Phase 1aのJava 21 / 25 runtime検証を理由に、正式な
   Reference Container、base image、Cloud Deploymentを先行固定しない。
9. 後続Phaseのblockerを避けるread-only調査、artifact / dependency取得確認、license確認、
   外部環境および開始条件の確認は許容するが、その調査を理由にruntime dependency、Starter、
   Public APIをPhase 1aへ追加しない。

## 8. 差分外だがPhase 1aで処置する対象

`main`と`walking-skeleton`の共通祖先に含まれるため185 filesの差分には出ないが、次は
Phase 1aの正式化時に必ず処置する。

| `main`の既存対象 | 処置 |
|---|---|
| Root POMの`dev.koiki.walkingskeleton`座標とWalking Skeleton向け名称 | 正式座標・名称へ変更する |
| `walking-skeleton/ws-smoke-lib`、`walking-skeleton/ws-smoke-app` | 正式な代替成果物の同等検証後にRoot ReactorとPhase 1aの正式本線から除去し、検証証拠として`walking-skeleton` branchとGit履歴に保持する |
| `walking-skeleton/negative-tests/nullaway` | 正式なnegative test方式の成立後にPhase 1aの正式本線から除去する |
| Phase 0向けRoot `README.md`、`AGENTS.md` | Phase 1aの正式Repository説明へ更新する |
| Wrapper bootstrap script | 正式Wrapper生成後の継続要否を判断する |

また、`main`だけに存在する次の変更はPhase 1aの入力として保持し、Walking Skeleton側のfileで
上書きしない。

- `koiki-parent/pom.xml`のError Prone向け`fork=true`
- `build-support/scripts/import-corporate-root-ca.ps1`
- `build-support/README.md`の社内SSL proxy / certificate手順

## 9. Phase 1a開始時の取込順序

1. A区分の承認済み正本文書とlinkを`main`基準で整合する。
2. Phase 1aのMaven座標、正式module、Public API候補を決定する。
3. Root Reactor、Parent、BOM、Wrapper、build-supportをB区分として再構成する。
4. Architecture ContractとArchUnit rulesを別commit単位で再実装する。
5. NullAway、Java 21 build、Java 21 / 25 runtime、外部Consumer、CIを正式経路で再検証する。
6. D区分がReactor、BOM、Public APIへ混入していないことを確認する。

## 10. Owner Review観点

- A区分の文書をPhase 1a branchへ正式配置する範囲は妥当か。
- Architecture Contractの正式artifact名とPublic API scopeをどこまで固定するか。
- ArchUnit rulesはPhase 0実装を参照しつつ、どの単位で正式再実装するか。
- OpenSpecとNode.jsをPhase 1aの必須toolingにしない判断は妥当か。
- `.vscode/settings.json`を共有Contributor設定として保持するか。
- Phase 1aの外部Consumer検証に使用するartifact repositoryとCI platformを何にするか。

## 11. Owner Review結果

| 項目 | 判定 |
|---|---|
| 対象 | A「正式化」、B「再設計・再実装」、C「検証証拠としてのみ参照」、D「Phase 1a対象外」の全区分 |
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月21日 |
| Conditions | §4.1の5件、§5.1の9件、§6.1の7件、§7.1の9件 |
| Rationale | Phase 0成果を正式文書、再実装対象、固定Commit上の証拠、後続Phase対象へ分離し、Walking Skeleton codeの機械的昇格、Public APIの先行固定、Runtime機能のPhase 1a混入を防止できる |
| Revisit trigger | 正式配置対象の追加・除外、Phase 1a DoDまたはbaselineの変更、Phase 0証拠が設計前提を否定した場合、OpenSpecを正式必須toolingとする場合 |

この承認は分類と引継ぎ境界の承認であり、承認時点ではA区分の正式配置、B区分の実装、
C区分の除去、D区分の後続Phase実装が完了したことを意味しなかった。Phase 1a C5での
処置状況は§12に追記し、Owner Reviewまでは完了扱いにしない。

## 12. Phase 1a C5処置台帳

**状態:** Gate 1 ACCEPTED / Gate 2 READY FOR OWNER REVIEW<br>
**基準Commit:** `ca37e5c`（C4 PR #19 merge）<br>
**作業branch:** `feature/phase1a-closeout`

Phase 1aで正式代替が成立した対象について、C5 Gate 2で正式本線からの除去と正本同期を行う。
削除対象は`walking-skeleton` branch、固定snapshot `b3ba79f`およびGit履歴に保持し、現在の
build、配布物、Public API、Templateへ直接昇格させない。

| 移行対象 | C5 Gate 2での処置 | 正式代替・保持先 | 状態 |
|---|---|---|---|
| Root POMの一時座標・名称 | A2で正式座標`org.koikifw`と4 module Reactorへ置換済み | Root / Parent / BOM、`phase1a-build-foundation.md` | VERIFIED |
| `walking-skeleton/ws-smoke-lib`、`ws-smoke-app` | 正式本線から削除 | Architecture Contract、ArchUnit Rules、Feature Template、C1〜C4 Evidence。旧sourceは固定snapshotとGit履歴 | IN REVIEW |
| `walking-skeleton/negative-tests/nullaway` | 正式本線から削除 | `build-support/null-safety/`の再現可能なpositive / negative検証、`phase1a-null-safety.md` | IN REVIEW |
| Walking Skeleton作業計画 | 正式本線から削除 | 承認済みPhase 0 Validation、固定snapshot、Git履歴 | IN REVIEW |
| Phase 0向けRoot説明 | Phase 1aのmodule、配布、互換性、残置物処置へ同期 | Root `README.md`、`REPOSITORY-TREE.txt`、Validation index | IN REVIEW |
| Maven Wrapper bootstrap | 継続保守用に保持し、既存Wrapperと同じ`bin`方式へ修正 | `.mvn/README.md`、`bootstrap-maven-wrapper.ps1` / `.sh` | IN REVIEW |
| `run-with-java25.ps1`、`verify-class-version.ps1` | C4の同一artifact runtime検証成立後に削除 | `runtime-compatibility-fixture/`、`runtime-compatibility.yml`、`phase1a-java-runtime-matrix.md` | IN REVIEW |
| D区分のRuntime / Security / Reference / Production成果物 | Phase 1aへ追加しない | 後続PhaseのGovernance判断 | VERIFIED |

Gate 2では処置内容と回帰検証をOwner Reviewへ提示する。`IN REVIEW`は削除・同期を実装済みで
あることを示し、Gate 2 ACCEPTEDまたはPhase 1a COMPLETEを意味しない。

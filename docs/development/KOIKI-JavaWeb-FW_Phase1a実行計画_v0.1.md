# KOIKI-JavaWeb-FW Phase 1a Build Foundation 実行計画

**版:** v0.1  
**作成日:** 2026年8月21日  
**文書状態:** ACCEPTED<br>
**実行状態:** IN PROGRESS（Milestone A・B COMPLETE / Milestone CのC1〜C4 COMPLETE・C5 Gate 1〜3 ACCEPTED / Gate 4 LOCAL COMPLETE・REMOTE PENDING）<br>
**Architecture Owner:** Shuichi Kataoka  
**承認日:** 2026年8月21日  
**最終状態更新日:** 2026年8月27日<br>
**対象Phase:** Phase 1a Build Foundation  
**実行方式:** `main`基準のWP別feature branch / Pull Request<br>
**開始基準Commit:** `b5194b1`<br>
**C1実装merge commit:** `9573b1c`（PR #14）、**C1完了Evidence merge commit:** `b92493b`（PR #15）<br>
**CI Linux化merge commit:** `10c6ca2`（PR #16）

## 1. 目的

グランドデザイン v0.2 §27.4のPhase 1a DoD 1a-1〜1a-6を、独立してreview・検証できる
Work Packageへ分解し、正式なKOIKI build foundationを実装する順序、判断ゲート、品質ゲート、
証拠および完了判定を定める。

本計画はWalking Skeleton codeの昇格計画ではない。Phase 0で成立を確認した設定、失敗条件、
test観点を参照し、正式なOwnership、Maven座標、Public APIおよび配布経路で再実装する。

## 2. 作業位置

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1a / Milestone A・B COMPLETE、Milestone CのC1〜C4 COMPLETE／C5 Gate 1〜3 ACCEPTED・Gate 4 LOCAL COMPLETE・REMOTE PENDING |
| Ownership | Framework: Architecture Contract、Tooling: Parent / BOM / Build Support / ArchUnit / CI |
| 対象module | Root Reactor、`koiki-parent`、`koiki-dependencies-bom`、Architecture Contract、`koiki-archunit-rules`、検証用Consumer |
| 適用指針 | グランドデザイン v0.2、Repository Architecture、ADR Register、Baseline Compatibility、Phase 1a引継ぎ台帳 |
| 検証 | Maven、Enforcer、NullAway、ArchUnit、Spring Modulith Level 0、japicmp、Java 21 / 25 runtime、Repository外Consumer、CI |
| 保留 | Runtime Foundation、Security、Reference業務、REST、SPA、Level 1 / 2、MyBatis詳細、正式release |

## 3. 開始時点

### 3.1 完了済みの準備

| 項目 | 状態 | Evidence |
|---|---|---|
| Phase 0 Architecture Baseline | COMPLETE | `../architecture/KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md` |
| Walking Skeleton引継ぎ分類 | ACCEPTED | `KOIKI-JavaWeb-FW_Phase1a_WalkingSkeleton_Transition_Inventory_v0.1.md`、Commit `5a8e41a` |
| A区分の正式文書配置 | COMPLETE | Commit `b5194b1` |
| B〜D区分の実装境界 | ACCEPTED | 引継ぎ台帳§5〜§7 |

これらはPhase 1aの準備完了を示すが、DoD 1a-1〜1a-6の実装完了を意味しない。

### 3.2 A2着手前の移行状態

- Root POM、Parent、BOMにはPhase 0の一時座標`dev.koiki.walkingskeleton`が残る。
- Root Reactorには`walking-skeleton/ws-smoke-*`が含まれる。
- 正式Maven Wrapper、Architecture Contract、ArchUnit rules、CI、japicmp baselineは未構成である。
- `main`側のError Prone `fork=true`と社内SSL proxy・証明書対応は保持対象である。
- OpenSpecとNode.jsはPhase 1aの必須build前提ではない。

### 3.3 A2技術検証後の状態

- Root Reactor、Parent、BOMおよびWrapperを正式座標と責務で再構成した。
- 正式Reactorから`walking-skeleton/ws-smoke-*`を除外した。
- G1候補の実効POM、dependency tree、Java 21正常系およびJava 25 build拒否を確認した。
- Architecture Owner Reviewにより、A2を`COMPLETE`、G1を`ACCEPTED`とした。

### 3.4 A3完了後の状態

- `koiki-architecture-contract`を正式Reactorへ追加し、G3で承認した4 Public APIだけを実装した。
- reflection、Customer相当package、依存scope、JAR内容およびPublic API inventoryを検証した。
- 隔離Maven repositoryから正式Reactorの対象buildを再現し、Parent / BOM前提とC1 / C2の境界を確認した。
- Architecture Owner Reviewにより、A3検証証拠を`ACCEPTED`、A3を`COMPLETE`とした。

### 3.5 A4実装後の状態

- G4-CIに従い、Windows / UbuntuでJava 21の`clean verify`を行うread-onlyの`ci.yml`を実装した。
- 外部Actionをfull commit SHAで固定し、secretとpackage書込み権限をCIから分離した。
- checkout credentialを保持しない構成を含め、CI骨格をArchitecture Ownerが条件付き承認した。
- PR #6でWindows / Ubuntu両jobの正常系成功を確認した。
- PR #7の意図的負例でWindows / Ubuntu両jobが失敗し、PRをmergeせずcloseした。
- `main`の保護規則で両jobをrequired checkとし、最新pushの成功後にマージ条件を満たすことを確認した。
- Architecture Owner Reviewにより、A4検証証拠を`ACCEPTED`、A4を`COMPLETE`とした。

### 3.6 B4完了後の状態

- B1でTier 1 / Tier 2 Feature Template、B2 / B3でArchUnit Public API・25 failure rule・2許容predicate・必須5負例を正式化した。
- B4でParentのJSpecify / NullAway設定を、隔離fixtureによるpositive→negative→restoreとWindows / Ubuntu CIで実証した。
- B4 PR #11をmerge commit `b742ecf`で`main`へmergeし、push後のCI run #32803500566で両OS jobの成功を確認した。
- Architecture Owner Reviewにより、B1〜B4は`COMPLETE`。Milestone Bの次回WPはB5 Template統合検証とする。
- B5、C1以降および後続Phaseの成果物は未実装である。

### 3.7 B5完了後の状態

- B5でTier 1 / Tier 2 Feature TemplateへArchUnit、Spring Modulith Level 0、NullAwayを統合した。
- 全7 production packageの`@NullMarked`宣言、正常系、Tier別ArchUnit負例2経路、
  Tier別NullAway負例2経路、復元およびruntime依存境界を再現可能なscriptで検証した。
- PR #13のCI run #32825065374でWindows / Ubuntu両jobの成功を確認し、
  Architecture Owner ReviewによりGate 1〜4、B5およびMilestone Bを`COMPLETE`とした。
- Phase 1aの次回WPはMilestone CのC1内部snapshot公開とする。C1以降および後続Phaseの成果物は未実装である。

### 3.8 C1完了後の状態

- C1でBOM、Parent、Architecture Contract、ArchUnit Rulesの4成果物を同一version release unitとして
  GitHub Packagesへ公開した。
- `org.koikifw:*:0.1.0-20260826.091429-1`のPOM / JAR、SHA-256、公開元commit、credential非露出および
  隔離local repositoryからの依存解決を確認した。
- Architecture Owner ReviewによりC1 Gate 1〜4を`ACCEPTED`、C1を`COMPLETE`とした。Evidence正本は
  `../architecture/validation/phase1a-internal-snapshot.md`とする。
- 開発初期のCI速度改善として、通常CIとsnapshot公開preflightは`ubuntu-24.04`だけを実行する。
  `windows-2025`用stepは再有効化に備えてworkflowへ残し、GitHub Rulesetのrequired status checkも
  `Verify (ubuntu-24.04)`だけへ同期済みである。
- Phase 1aの次回WPはC2 Repository外Consumerとする。C2〜C5および後続Phaseの成果物は未実装である。

### 3.9 C2完了後の状態

- KOIKI本体とは別のPUBLIC GitHub RepositoryにTooling-owned Consumerを配置した。
- localではPAT classic `read:packages`だけ、remote CIではConsumerの`GITHUB_TOKEN`と`packages: read`だけで、
  C1 snapshotをGUID付き空Maven repositoryへ解決した。
- 両Public APIの正常系、`KOIKI-ARCH-001` / ADR-022の期待failure、effective dependency scope、timestamped
  snapshotおよび6 payload SHA-256をWindows localとUbuntu fresh runnerで確認した。
- Architecture Owner ReviewによりC2 Gate 1〜4を`ACCEPTED`、C2とDoD 1a-3を`COMPLETE`とした。Evidence正本は
  `../architecture/validation/phase1a-external-consumer.md`とする。
- C2完了時点の次回WPはC3 Public API Compatibilityであった。

### 3.10 C3完了後の状態

- C1 timestamped snapshotをimmutable baselineとし、Architecture Contractの5 public型・4 annotation element、
  ArchUnit Rulesの2 public methodをinventoryへ固定した。
- 正式artifactのjapicmp比較、Public API破壊・未承認追加の期待failureおよびpackage-private変更の許容を、
  Windows localとUbuntu fresh runnerで確認した。
- CIはRepository `GITHUB_TOKEN`と`packages: read`だけでbaselineを取得し、credential実値を露出せず完了した。
- `Public API Compatibility`をmain rulesetのrequired checkへ追加し、既存`Verify (ubuntu-24.04)`、strict policy、
  PR保護およびbypassなしを維持した。
- Architecture Owner ReviewによりC3 Gate 1〜4を`ACCEPTED`、C3とDoD 1a-5を`COMPLETE`とした。Evidence正本は
  `../architecture/validation/phase1a-public-api-compatibility.md`とする。
- C3完了時点の次回WPはC4 Java runtime matrixであった。

### 3.11 C4完了後の状態

- Tooling-owned非配布fixtureをJDK 21で一度だけbuildし、class major `65`の同一JARをJava 21 / 25で起動した。
- 独立workflowのjob間artifact受け渡しでJAR SHA-256、source commit、dirty状態を検証し、runtime jobでは
  compile、packageまたはruntime別artifact生成を行っていない。
- Java 25 build、hash改変、期待runtime major不一致の3 negative guardsをlocalとUbuntu fresh runnerで確認した。
- `Java Runtime Compatibility`をmain rulesetのrequired checkへ追加し、既存2 checks、strict policy、
  PR保護およびbypassなしを維持した。
- Architecture Owner ReviewによりC4 Gate 1〜4を`ACCEPTED`、C4とDoD 1a-6を`COMPLETE`とした。Evidence正本は
  `../architecture/validation/phase1a-java-runtime-matrix.md`とする。Phase 1aの次回WPはC5 Closeoutである。

### 3.12 C5着手後の状態

- C5 Gate 1でread-only調査、5項目の実装境界およびGate 1〜4計画をArchitecture Ownerが承認した。
- C5はArchitecture / Tooling closeoutに限定し、新規機能、Public APIまたは後続Phase成果物を追加しない。
- Gate 2でBaseline、Repository hygiene、Walking Skeleton残置物と正式代替の対応を同期し、Architecture Ownerが内容と結果を承認した。
- Gate 3でDoD 1a-1〜1a-6、共通DoD、ADR / Skill / Flyway要否を既存Evidenceへ照合し、Architecture Ownerが内容と結果を承認した。
- Gate 4 localでRoot、Feature Template、NullAway、Public API fixture、Java 21 / 25 runtimeおよび3 negative guardsを一括再検証し、remote確認待ちである。
- Evidence正本は`../architecture/validation/phase1a-closeout.md`とし、Gate 3のDoD / Governance reviewと
  Gate 4の最終検証・Owner Review前にPhase 1a COMPLETEとは扱わない。

## 4. Scope

### 4.1 In scope

- 正式Maven座標と同一version release unit
- Root Reactor / Parent / BOM / Maven Wrapper / build-support
- Java 21 build contractとEnforcer / Toolchains
- JSpecify / NullAway
- Architecture Contractと`@KoikiModule`
- Spring Modulith 2.1.0 Level 0（test scope、runtime依存なし）
- Tier 1 / Tier 2 Feature Template
- Phase 1a適用ArchUnit rule setとRepository外配布
- Public API inventoryとjapicmp baseline
- Java 21で生成した同一成果物のJava 21 / 25 runtime検証
- CI quality gateと内部snapshot artifactによる外部Consumer検証
- Phase 1a実装証拠、ADR、Skill、Baselineの必要な更新

### 4.2 Non-scope

- Flyway Starter、Problem Details、Jackson、Resilience、構造化log、Actuator、OSIV等のPhase 1b成果物
- Identity、認証・認可、Session、監査等のPhase 2成果物
- 正式Reference Application、MVC / HTMX、最小REST API、Spring Modulith Level 1等のPhase 3成果物
- React SPA、非同期event、Spring Modulith Level 2、外部I/O、Batch等のPhase 4成果物
- Project Template、正式Container / Cloud Deployment、OpenRewrite、SBOM、Support、正式release等のPhase 5成果物
- 将来用途だけを理由とする空module、空package、仮Starter、仮Public API

## 5. 実行原則

1. `main`を基準に再実装し、`walking-skeleton` branchをmergeまたは一括copyしない。
2. Public APIとなるannotation、enum、rule entry pointは、実装前にscopeをreviewする。
3. Spring標準で代替できる機能をKOIKI独自wrapperにしない。
4. Feature TemplateはPhase 1aのTier構造検証に限定し、Phase 5のProject Templateへ拡張しない。
5. 意図的違反はtest fixtureまたは独立Consumerへ隔離し、production sourceへ混入しない。
6. 外部ConsumerはRoot Reactor、`-am install`、local Maven Repositoryへ依存させない。
7. Java 25 runtime検証ではJava 21成果物を再compileしない。
8. 後続Phaseの調査は許容するが、調査だけを理由にdependency、Starter、Public APIを追加しない。
9. Walking Skeleton codeは正式な代替検証が成功するまで除去せず、成功後に正式本線から除去する。

## 6. 判断ゲート

各Gateが指定する後続WPへの着手前に、必要な判断を行う。すべてのGateをPhase 1a開始時に
一括確定せず、直近WPの実装・検証に必要な範囲で順に承認する。Public API、配布方式または
Phase scopeが変わる場合は、Architecture Ownerの承認を得て、必要ならADRを追加・改訂する。

本節のOwner Reviewは判断ゲート制度を承認するものであり、G1〜G6の技術選択を一括承認するものではない。
各Gateは個別承認まで`PENDING`とし、状態、承認日、Owner、EvidenceおよびADR要否を記録する。

| Gate | 判断事項 | 完了条件 | 後続 | 状態 |
|---|---|---|---|---|
| G1 Baseline | Spring Boot、Spring Modulith、Java、Maven、build pluginの開始baseline | 承認済みbaselineを開始候補とし、公式情報の確認日、候補POM、実効POM、dependency treeを照合して変更要否を記録する | A2 | ACCEPTED |
| G2 Maven coordinates | 内部development / snapshot version、artifactId、同一version release unit、module graph、および確定済み`org.koikifw`の適用 | 一時座標と`ws-*`を含まず、内部snapshotと正式releaseを区別した座標表・依存図をOwnerが承認する | A2 / A3 | ACCEPTED |
| G3 Architecture Contract | G2で確定したartifactIdに対するpackage、annotation属性、enum、retention、target、dependency、Public API scope | 依存なしまたは必要最小限のContractとし、承認した型だけをPublic API inventoryとjapicmp対象候補にする。`JDBC`や`SEPARATED`等の未検証方式を先行固定しない | A3 / B2 | ACCEPTED |
| G4 CI / artifact repository | CI platformと内部snapshot repositoryを個別に判断し、Consumer認証、credential境界、保持・削除方針を定める | 両判断を個別記録し、外部Consumerが利用でき、秘密値を露出せず、正式releaseと誤認しない運用をOwnerが承認する | A4 / C1 | ACCEPTED |
| G5 ArchUnit API | rule適用Phase、公開entry point、ADR message contract、Rule 19の限界 | DoD 1a-2の必須5違反と適用対象ruleのmatrixを承認し、Rule 19を完全なdata-flow保証と表現しない | B2 / B3 | ACCEPTED |
| G6 Runtime fixture | Java 21 / 25で起動するTooling所有の検証用成果物の配置と非配布境界 | Framework Public API、Reference、製品runtimeへ昇格させず、同一artifactのhash、class major 65、Java 21 / 25での起動を証拠化するfixtureとして承認する | C4 | ACCEPTED |

### 6.1 G5の必須違反

G5では、DoD 1a-2が定める次の5違反をrule matrixへ明記する。

1. Tier宣言の欠落
2. `domain.model`のController露出
3. `internal`の外部参照
4. `@TransactionalEventListener`の使用
5. モジュール間の直接Bean呼出

### 6.2 Gate decision record

G4のCI platformとartifact repositoryは別々の判断として記録する。各Gateを承認するときは、
次の表を更新し、Evidenceを固定Commitまたは承認文書から追跡できるようにする。

| Gate | Decision | Decided by | Date | Evidence | ADR |
|---|---|---|---|---|---|
| G1 Baseline | ACCEPTED | Shuichi Kataoka | 2026年8月21日 | §6.4、`../architecture/validation/phase1a-build-foundation.md`、Baseline Compatibility v0.1、公式一次情報 | 不要（ADR-001〜003の範囲内） |
| G2 Maven coordinates | ACCEPTED | Shuichi Kataoka | 2026年8月21日 | §6.5、Repository Architecture v0.1 | 不要（承認済みRepository Architectureの具体化） |
| G3 Architecture Contract | ACCEPTED | Shuichi Kataoka | 2026年8月21日 | §6.6、Phase 1a引継ぎ台帳、ArchUnit Distribution Validation | 不要（グランドデザイン§11.2、§21.3およびADR-041の具体化） |
| G4-CI CI platform | ACCEPTED | Shuichi Kataoka | 2026年8月21日 | §6.7、GitHub Actions公式文書 | 不要（Phase 1a CI実装方式の具体化） |
| G4-Repository artifact repository | ACCEPTED | Shuichi Kataoka | 2026年8月21日 | §6.7、GitHub Packages Maven registry公式文書 | 不要（Phase 1a内部snapshot検証方式の具体化） |
| G5 ArchUnit API | ACCEPTED | Shuichi Kataoka | 2026年8月21日 | §6.8、ArchUnit Distribution Validation、Phase 1a引継ぎ台帳 | 不要（グランドデザイン§21.3、ADR-005・022・023・025・041の具体化） |
| G6 Runtime fixture | ACCEPTED | Shuichi Kataoka | 2026年8月21日 | §6.9、Build Foundation Validation、Phase 1a引継ぎ台帳 | 不要（グランドデザイン§21.5・§27.4およびADR-001の具体化） |

### 6.3 Owner Review結果

| 項目 | 判定 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月21日 |
| Scope | §6の判断ゲート制度。G1〜G6の個別技術判断は含まない |
| Conditions | 後続WP単位の承認、Gate記録、G1の実効依存照合、G2の確定namespaceとversion区別、G3の最小Public API、G4の個別判断、G5の5違反とRule 19制約、G6のTooling所有と同一artifact証拠 |
| Rationale | Public API、配布、CIおよびruntime fixtureを実装後に追認せず、未確定事項を後続WPへ先行固定しない統制として妥当である |

### 6.4 G1 BaselineのOwner Review結果

2026年8月21日時点の公式一次情報とPhase 0の承認済みbaselineを照合し、次をPhase 1a開始baselineとする。

| Component | Phase 1a baseline | 判断 |
|---|---:|---|
| Spring Boot | 4.1.1 | 4.1 minorを維持し、Phase 0の4.1.0からpatch追従する |
| Spring Modulith | 2.1.0 | 維持。Level 0の`spring-modulith-starter-test`をtest scopeに限定する |
| Java target bytecode / Build JDK | 21 / 21 | 維持 |
| Java runtime matrix | 21 / 25 | 維持。同一artifactを再compileせず検証する |
| Maven / Maven Wrapper Plugin | 3.9.16 / 3.3.4 | Maven 4 RCを採用せず、公式Wrapperを再生成する |
| Maven Compiler / Enforcer / Toolchains Plugin | 3.15.0 / 3.6.3 / 3.3.0 | 維持 |
| Error Prone / NullAway / JSpecify | 2.50.0 / 0.13.8 / 1.0.0 | 維持 |

**Decision:** ACCEPTED（2026年8月21日、Shuichi Kataoka）<br>
**Gate status:** ACCEPTED

この承認は、Baseline Compatibility v0.1のPhase 0 development行を未検証のまま上書きする
ものではない。A2で正式POMとWrapperを構成し、次の条件を実効検証した結果に基づく。

1. 候補POM、実効POMおよびdependency treeが一致する。
2. Spring FrameworkがSpring Boot 4.1.1の管理する7系へ統一される。
3. Spring Boot関連artifactが4.1.1へ統一される。
4. Spring Modulith 2.1.0を使用し、`spring-modulith-starter-test`がtest scopeに限定される。
5. Spring Modulithのruntime用artifactがproduction runtimeへ混入しない。
6. 公式Maven Wrapper 3.9.16とBuild JDK 21で正式Reactorのbuildが成功する。

2026年8月21日にA2の正式POM、公式Wrapper、実効POMおよびdependency treeを用いて上記6点を
技術検証し、すべて成立した。加えて、異なるversionのConsumerから正式Parentを継承しても
KOIKI BOM `0.1.0-SNAPSHOT`が解決されること、Java 25によるbuildがEnforcerで拒否されることを
確認した。実結果とscopeは`../architecture/validation/phase1a-build-foundation.md`へ記録する。
Architecture Owner Reviewにより、この証拠を承認し、G1を`ACCEPTED`とした。

確認した公式一次情報:

- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Modulith Reference](https://docs.spring.io/spring-modulith/reference/index.html)
- [Apache Maven Release History](https://maven.apache.org/docs/history)
- [Apache Maven Plugins](https://maven.apache.org/plugins/index.html)
- [Error Prone Releases](https://github.com/google/error-prone/releases)
- [NullAway Releases](https://github.com/uber/NullAway/releases)
- [JSpecify](https://github.com/jspecify/jspecify)

### 6.5 G2 Maven coordinatesのOwner Review結果

Phase 1aで構成する正式Maven成果物について、次の7点を承認条件とする。

1. Maven `groupId`は全成果物で`org.koikifw`に統一する。
2. Javaの正式package体系は`org.koikifw.<module>`とし、
   `org.koikifw.libkoiki.web`およびWalking Skeletonの`dev.koiki.walkingskeleton`を使用しない。
3. Phase 1aの内部development versionは`0.1.0-SNAPSHOT`とし、正式release versionとは
   区別する。正式releaseの実施およびversion確定はPhase 1aのscopeに含めない。
4. Rootは`org.koikifw:koiki-javaweb-fw-reactor:0.1.0-SNAPSHOT`、`packaging=pom`の
   Reactor / Aggregator専用成果物とし、依存管理またはPlugin管理のOwnerにしない。
5. `koiki-dependencies-bom`、`koiki-parent`、`koiki-architecture-contract`、
   `koiki-archunit-rules`をPhase 1aの同一version release unitとする。
6. BOMはdependency version、Parentはbuild・plugin policyを所有する。ParentがBOMをimportし、
   ContractとArchUnit RulesがParentを継承し、ArchUnit Rulesだけが必要に応じてContractへ依存する。
7. `ws-*`、空の将来module、仮Starterおよび一時artifactを正式Reactorまたはrelease unitへ含めない。

| 成果物 | Maven coordinates | packaging | Phase 1aでの役割 |
|---|---|---|---|
| Root Reactor | `org.koikifw:koiki-javaweb-fw-reactor:0.1.0-SNAPSHOT` | `pom` | 正式moduleの集約のみ |
| Dependencies BOM | `org.koikifw:koiki-dependencies-bom:0.1.0-SNAPSHOT` | `pom` | dependency version管理 |
| Parent | `org.koikifw:koiki-parent:0.1.0-SNAPSHOT` | `pom` | build・plugin policy |
| Architecture Contract | `org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT` | `jar` | 最小Architecture Public API |
| ArchUnit Rules | `org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT` | `jar` | 顧客側へ配布する構造規約 |

```text
koiki-javaweb-fw-reactor (aggregates only)
├── koiki-dependencies-bom
├── koiki-parent ── imports ──> koiki-dependencies-bom
├── koiki-architecture-contract ── inherits ──> koiki-parent
└── koiki-archunit-rules ── inherits ──> koiki-parent
                           └─ depends on ─> koiki-architecture-contract（必要最小限）
```

**Decision:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**Gate status:** ACCEPTED

G2は座標、release unitおよびmodule graphの設計判断を確定する。G3で扱うannotation型、
package詳細およびPublic API scope、G4で扱うartifact repositoryへの公開方法は、この承認に含めない。

### 6.6 G3 Architecture ContractのOwner Review結果

`org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT`をFramework所有の最小Public API
artifactとし、次の8点を承認条件とする。

1. Java packageは`org.koikifw.architecture`とし、Walking Skeletonの一時packageを引き継がない。
2. Public APIは`KoikiModule`、`ModuleTier`、`PersistenceTechnology`、`PersistenceModel`の
   4型だけとし、実装補助型、空packageまたは`internal`型を先行生成しない。
3. `KoikiModule`は`public @interface`、`@Target(PACKAGE)`、`@Retention(RUNTIME)`、
   `@Documented`とする。`@Inherited`は使用せず、モジュールrootの`package-info.java`に付与する。
4. annotation属性は`name`、`tier`、`persistence`、`persistenceModel`とし、defaultを設けず
   すべて明示させる。`name`とモジュールroot package名の一致はArchUnit側の検査候補とする。
5. `ModuleTier`は`SIMPLE`と`RICH`、`PersistenceTechnology`は`JPA`と`MYBATIS`、
   `PersistenceModel`は検証済みの`SHARED`だけを定義する。`JDBC`と`SEPARATED`は定義しない。
6. `MYBATIS`はADR-039で承認された宣言語彙であり、Phase 1aでMyBatis実装、Starterまたは
   利用可能なFeature Templateを提供する意味にはしない。詳細規約と`SEPARATED`追加は後続Phaseで判断する。
7. production依存はpackageへの`@NullMarked`適用に必要な`org.jspecify:jspecify`だけとし、
   Spring、Spring Modulith、ArchUnit、JPA、MyBatisおよびLombokへ依存しない。
8. 4型の型名、package、annotation属性、enum定数、retention、target、default有無をPublic API
   inventoryとjapicmp対象候補にし、変更時はADR-041に従って互換性を判定する。

| Public API | 承認する契約 |
|---|---|
| `KoikiModule` | `name`、`tier`、`persistence`、`persistenceModel`を持つpackage annotation |
| `ModuleTier` | `SIMPLE`、`RICH` |
| `PersistenceTechnology` | `JPA`、`MYBATIS` |
| `PersistenceModel` | `SHARED` |

A3では次を検証する。

1. Repository内では正式Reactorから`-pl koiki-architecture-contract -am verify`が成功する。
   `-f`による直接実行はParent / BOMを解決できることを前提とし、公開snapshotからの独立解決はC1 / C2で検証する。
2. reflection testで`@Target(PACKAGE)`、`@Retention(RUNTIME)`、`@Documented`を確認する。
3. annotation属性、defaultなし、およびenum定数が承認内容と一致する。
4. 同じ`package-info.java`に`@NullMarked`と`@KoikiModule`を併記できる。
5. Customer相当の別packageから4型を使用できる。
6. dependency treeのproduction依存がJSpecifyだけであり、Spring、ArchUnitおよび永続化実装が混入しない。
7. Public API inventoryを生成し、後続のjapicmp baselineへ登録できる。

**Decision:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**Gate status:** ACCEPTED

G3は最小Architecture ContractのPublic APIを確定する。`SEPARATED`の実装規約、MyBatisの
利用開始、Spring Modulith Named Interface、Runtime Starter、FlywayおよびG5のArchUnit公開APIは
この承認に含めない。

### 6.7 G4 CI platform / artifact repositoryのOwner Review結果

CIとartifact repositoryを別の権限境界として扱い、G4-CIにGitHub Actions、
G4-RepositoryにGitHub PackagesのApache Maven registryを採用する。

#### 6.7.1 G4-CI: GitHub Actions

1. PR必須検査は`windows-2025` / Temurin 21と`ubuntu-24.04` / Temurin 21の2経路とし、
   Windowsの`mvnw.cmd`・PowerShell経路とLinux上のplatform非依存性を同格に検証する。
2. `ci.yml`は`pull_request`と`main`へのpushで`clean verify`を行い、secretおよびpackage書込み権限を持たない。
3. `runtime-compatibility.yml`はJava 21で一度だけbuildした同一artifactを受け渡し、
   Java 21 / 25で実行する。Java 25では再compileしない。
4. `publish-snapshot.yml`はPR workflowから分離し、`workflow_dispatch`または検証済み`main`への
   pushだけを起点として、`clean verify`成功後に公開する。
5. Repositoryの既定`GITHUB_TOKEN`はread-onlyとし、workflowごとに最小`permissions`を明示する。
   `pull_request_target`を使用せず、fork PRへsecretまたはwrite tokenを渡さない。
6. 外部Actionはfull commit SHAで固定し、更新はreview対象とする。Maven dependency cacheは
   高速化だけに使用し、成果物または検証証拠として扱わない。
7. 同一branchの旧実行は`concurrency`で取消可能とし、branch protectionではWindows / Ubuntuの
   required check成功をmerge条件にする。
8. Workflow artifactはjob間の同一artifact受渡しと一時的な検証証拠に限定し、
   Maven Consumer向けのdependency repositoryとして使用しない。

**2026年8月26日 運用更新:** 開発初期のCI速度改善のため、`windows-2025` matrixは通常CIと
snapshot公開preflightで一時停止し、`ubuntu-24.04`だけをrequired checkとする。Windows専用stepは
再有効化に備えてworkflowへ残す。この更新はJava 21 / 25 runtime matrixのC4検証や、正式な
platform support範囲の決定を変更しない。

#### 6.7.2 G4-Repository: GitHub Packages

Phase 1aの内部snapshot Maven repositoryを次とする。

```text
https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB
```

公開対象はG2で承認した同一version release unitの4成果物に限定する。

```text
org.koikifw:koiki-dependencies-bom:0.1.0-SNAPSHOT
org.koikifw:koiki-parent:0.1.0-SNAPSHOT
org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT
org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT
```

1. Root Reactor、Feature Template、fixture、`ws-*`および正式release versionは公開しない。
2. publish jobだけに`contents: read`と`packages: write`を付与し、Repository固有の`GITHUB_TOKEN`を使う。
   publish用PATをRepository secretとして保持しない。
3. GitHub Actions上の別Repository ConsumerはpackageへのActions read accessを明示して
   `GITHUB_TOKEN`を使用する。ローカルまたは独立Consumerは`read:packages`だけを持つPAT classicを使う。
4. credential実値をPOM、workflow、Repository内のMaven settings templateまたはlogへ保存しない。
   templateは環境変数を参照し、Consumerごとのユーザー設定で注入する。
5. Consumer側でsnapshot取得を有効化し、公開元Git commit、解決したsnapshot、POM / JARのchecksumを
   Evidenceへ記録して、可変な`0.1.0-SNAPSHOT`だけを検証識別子にしない。
6. Phase 1a中は検証用snapshotを保持し、Validationまたはjapicmp baselineが参照する版を削除しない。
   自動削除用の高権限tokenは導入せず、Closeout時にOwnerが不要版を棚卸しする。
7. GitHub Packagesは認証を必要とするPhase 1a内部検証用repositoryとし、一般公開、Support開始、
   Maven Centralまたは顧客企業内Nexus / Artifactoryの選択を意味しない。

**G4-CI Decision:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**G4-Repository Decision:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**Gate status:** ACCEPTED

この承認はCI platform、内部snapshot配布およびcredential境界の設計判断を確定する。
Workflow、branch protection、package権限、公開・Consumer解決およびchecksum記録はA4 / C1で実証する。
Maven Central、正式release、artifact署名、SBOM、Container registryおよびProduction Deploymentは
後続Phaseの判断とする。

確認した公式情報:

- [GitHub-hosted runners](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)
- [GitHub ActionsでMavenをbuild・testする方法](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven)
- [GitHub Actionsのsecurity guidance](https://docs.github.com/en/code-security/tutorials/secure-your-organization/protect-against-threats)
- [GitHub Packages Apache Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [GitHub Packagesのpermission](https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages)

### 6.8 G5 ArchUnit APIのOwner Review結果

`org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT`をTooling所有のtest用artifactとし、
Customer、Reference、Feature TemplateおよびKOIKI自身が同じ公開entry pointを利用できるようにする。

#### 6.8.1 Public API

`org.koikifw.archunit`の`KoikiArchitectureRules`だけを公開し、次の2つの合成ruleをPublic APIとする。

```text
org.koikifw.archunit.KoikiArchitectureRules
  + businessModuleRules(String businessBasePackage): ArchRule
  + frameworkOwnershipRules(
        String frameworkBasePackage,
        String... consumerBasePackages): ArchRule
```

| API | 適用先 |
|---|---|
| `businessModuleRules` | Customer、Reference、Feature Templateの業務モジュール |
| `frameworkOwnershipRules` | KOIKI自身のFramework / Reference / Customer所有権検査 |

Walking Skeleton固有の`representativeRules`、`phaseZeroRules`、`layerAndTierRules`および
規則ごとの個別public methodは正式Public APIへ昇格させない。Consumerが必須規則を任意選択する
方式を避け、Tier別適用は`@KoikiModule`の宣言から合成rule内部で判断する。

#### 6.8.2 Phase 1a rule matrix

| 状態 | 規則 | 扱い |
|---|---|---|
| Phase 1a適用 | 1〜24、28、38〜39 | 27項目。Tier別規則と明示例外を含む |
| Tier 2分離方式まで保留 | 25〜27、35〜37 | `SEPARATED`の実証後に判断 |
| Spring Modulith Level 2まで保留 | 29 | Level 2採用方式とともに判断 |
| MyBatis詳細規約まで保留 | 30〜34 | Phase 3末尾〜Phase 4で判断 |

規則10と23のような明示的許容もmatrixへ含めるが、違反を発生させる独立ruleとは数えない。
規則28はPhase 1aからLevel 1までの予防的禁止として、直接またはmeta-annotation経由の
`@TransactionalEventListener`へ適用する。`@ApplicationModuleListener`もPhase 1aではRule 28違反とし、
Level 2採用時にRule 29と合わせて再判定する。

#### 6.8.3 DoD 1a-2の必須5違反

| 必須違反 | 対応規則 | 判断根拠 |
|---|---:|---|
| Tier宣言の欠落 | 7〜8 | ADR-022 |
| `domain.model`のController露出 | 17〜20 | ADR-023 |
| `internal`の外部・他モジュール参照 | 3、13 | ADR-041 |
| `@TransactionalEventListener`の使用 | 28 | ADR-005 |
| モジュール間の直接Bean参照・呼出 | 9〜10 | ADR-025 |

各違反を独立fixtureで検出し、1つのfixtureが別の違反によって偶然失敗する構成にしない。

#### 6.8.4 Error message contract

各failureには次の要素を含める。

```text
[KOIKI-ARCH-<3桁のrule number>] [ADR-<number>]
違反内容: <違反したclass、method、packageまたは依存先>
影響: <設計・保守上の影響>
修正: <具体的な修正方法>
```

rule numberは`KOIKI-ARCH-007`のように3桁で表し、グランドデザイン§21.3の1〜39へ対応させ、
別規則へ再利用しない。
文言改善は許容するが、rule ID、判断根拠、影響および修正方法を失わない。

#### 6.8.5 Rule 19の制約

規則19は、同じMVC handler内で`domain.model`を生成する、または`domain.model`を返すUse Caseを
呼び出し、`Model.addAttribute`もしくは`ModelAndView`へ渡す代表経路を検出する近似ruleとする。
`Object`へ型消去したhelper、field経由、複数method間、reflection等の任意data flowは保証しない。
また、sourceとModel書込みの同居だけで判定すると、domain.modelをDTOへ変換してDTOだけを渡す正常経路を
誤検出し得る。B3では代表違反の失敗、DTO変換正常経路の成功、保証外経路の記録を同時に満たす。

```text
ArchUnit近似検査 + OSIV無効化 + 実レンダリングWeb test
```

後続Phaseで上記の防御線を構成し、ArchUnit単独でEntity露出を完全防止できると表現しない。

#### 6.8.6 Dependency boundaryと検証

production依存は`koiki-architecture-contract`、ArchUnitおよびJSpecifyに限定する。
Spring Framework、Spring Modulith、JPAおよびMyBatisは完全修飾名で検出し、必要なfixture依存を
test scopeだけへ置く。Consumerは`koiki-archunit-rules`をtest scopeで利用する。

B2で検証契約を設計し、実行証拠は次のWPで取得する。

1. Tier 1 / Tier 2 compliant fixtureが成功する。
2. 必須5違反が独立fixtureでそれぞれ失敗する。
3. 27項目の適用matrixと実装が対応し、保留12項目を実装済みと誤認させない。
4. 規則10、23等の明示例外で誤検出しない。
5. 規則19の代表違反を検出し、その保証限界をREADMEとValidationへ記録する。
6. 全failureにrule ID、ADRまたは設計節、影響、修正方法および違反箇所を含める。
7. Spring等がproduction dependencyへ混入しない。
8. C1 / C2でGitHub PackagesのsnapshotをRepository外Consumerから解決して同じ違反を検出する。
9. C3で正式Public API inventoryを生成し、japicmp baselineとArchUnit dependency互換性を確認する。

B3は1〜7をRepository内の正式rule実装、fixture、dependency treeおよびCIで実証する。8はsnapshot公開を
所有するC1 / C2、9は互換性baselineを所有するC3の完了条件とし、B2 / B3の完了を先行WPの成果で代用しない。

**Decision:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**Gate status:** ACCEPTED

G5はPhase 1aの公開entry point、適用rule、message contractおよび既知の検査限界を確定する。
分離方式、Level 2、MyBatis詳細規則、OSIV・Web test実装およびNamed Interfaceはこの承認に含めない。
2026年8月21日のG5承認時点ではArchUnit本体versionを未選定としたが、B2 Gate 4で2026年8月24日時点の
Phase 1a baselineを1.5.0に確定した。後続versionへの更新はC3の互換性検証対象とする。

#### 6.8.7 B2 / B3実行境界（B2 Gate 1）

| WP | 完了対象 | このWPで行わないこと |
|---|---|---|
| B2 | Public API、入力契約、dependency、39規則matrix、message contract、Rule 19制約、compliant fixture仕様のOwner承認 | no-op rule、正式Maven module、rule実装、fixture実行結果を作らない |
| B3 | 正式`koiki-archunit-rules`、25 failure rule＋2許容predicate、全rule focused test、compliant fixture成功、必須5負例の独立failure、CI証拠 | B2承認済みPublic APIや適用Phaseを実装都合で変更しない |

§7.2のB2に記載した「compliant fixture成功」は、B2でfixtureと期待結果を設計し、B3のrule実装後に
実行成功を得る連続した受入条件として扱う。B2を形式的に完了させるためのno-op ruleまたは一部ruleだけの
暫定Public APIは作成しない。B2は設計のOwner承認で完了し、実行証拠はB3の完了条件とする。

**Decision:** ACCEPTED（2026年8月24日、Shuichi Kataoka）<br>
**Gate status:** B2 GATE 1 ACCEPTED

B2のGate 2〜5は`../architecture/validation/phase1a-archunit-api-design.md`でOwner Reviewし、
2026年8月24日に全Gate ACCEPTED、B2 COMPLETEとした。

### 6.9 G6 Runtime fixtureのOwner Review結果

Java 21で生成した同一成果物をJava 21 / 25で起動するDoD 1a-6の証拠として、
`build-support/runtime-compatibility-fixture/`にTooling所有の最小CLI JARを配置する。
Walking Skeletonの`ws-smoke-app`からは検証条件だけを引き継ぎ、code、一時座標、設定および
`Dockerfile.ws`を再利用しない。

#### 6.9.1 Ownershipと非配布境界

1. fixtureはJava標準機能だけで起動し、Spring Boot、業務機能、Reference Applicationおよび
   Phase 1bのruntime機能を含めない。
2. Java packageは`org.koikifw.buildsupport.internal.runtime`とし、利用者向けPublic APIを提供しない。
3. Maven上の識別子は検証専用とし、G2で確定した正式release unit、GitHub Packagesへのsnapshot公開、
   Public API inventoryおよびjapicmp対象へ含めない。
4. fixtureのJAR、source JARまたはclassをFramework、Starter、Reference、Feature Template、
   Project TemplateおよびRepository外Consumer向け成果物へ同梱しない。

#### 6.9.2 Buildと同一artifactの検証契約

1. Maven WrapperとBuild JDK 21で一度だけfixture JARを生成する。
2. fixture内のclassがtarget bytecode Java 21、class major version `65`であることを確認する。
3. 生成直後にJARのSHA-256を記録し、Java 21 / 25の両実行前に同じhashであることを確認する。
4. runtime検証ではMaven、`javac`、再packageおよびruntime別のartifact生成を行わず、受け渡された
   同一JARに対する`java -jar`だけを実行する。
5. 成功時は固定marker、実際のJava runtime versionおよび終了コード`0`を記録する。
6. CI結果を正式証拠とし、Windows開発環境でも同じ検証を再現できる手順を提供する。

#### 6.9.3 C4の完了条件

C4では次を1つのValidation記録として証拠化する。

| 確認対象 | 必須証拠 |
|---|---|
| Build | Maven Wrapper、Build JDK 21、生成JAR名、Commit |
| Bytecode | 検査対象class、major version `65` |
| Artifact identity | Java 21 / 25実行で一致するSHA-256 |
| Java 21 runtime | runtime vendor / version、固定marker、終了コード`0` |
| Java 25 runtime | runtime vendor / version、固定marker、終了コード`0` |
| Recompile禁止 | runtime jobにcompile・package処理がないこと |
| Ownership | fixtureが正式配布物とPublic APIへ含まれないこと |

**Decision:** ACCEPTED（2026年8月21日、Shuichi Kataoka）  
**Gate status:** ACCEPTED

G6はJava 21 bytecodeのJVM起動互換性を検証するfixtureの設計判断を確定する。
Spring Boot runtime、Web、DB、Container、性能、Java 25固有最適化および後続Phaseの製品runtime互換性は
この承認に含めず、それぞれ所定のPhaseで実装・検証する。

## 7. 内部マイルストーン

### 7.1 Milestone A — Formal Build and Contract Skeleton

**目的:** 正式座標、build policy、最小Public Contract、CIの土台を確立する。

| WP | 作業 | 主な成果物 | 検証・判定 |
|---|---|---|---|
| A1 | G1〜G4の設計判断 | 座標表、module graph、Public API候補、CI / repository方針、必要なADR | Owner Review |
| A2 | Root Reactor / Parent / BOM / Wrapper再構成 | 正式POM、公式Wrapper、Enforcer、Toolchains、build-support | COMPLETE（2026年8月21日、Owner Review済み） |
| A3 | Architecture Contract再実装 | `@KoikiModule`等の最小artifact、JSpecify適用、API test | COMPLETE（2026年8月21日、Owner Review済み） |
| A4 | CI骨格 | PR quality gate、cache・secret境界、Level 0 test経路 | COMPLETE（2026年8月21日、正常系・負例・required checkをOwner Review済み） |

**Exit criteria:**

- 一時Maven座標を使用しない正式ReactorがJava 21で成功する。
- Parent / BOM / ContractのOwnershipと同一version release unitが明示される。
- Spring Modulith Level 0がtest scopeだけで動作し、runtime依存へ混入しない。
- CI骨格が、後続WPのquality gateを追加できる状態になる。

### 7.2 Milestone B — Feature and Architecture Quality Gates

**目的:** Tier構造、ArchUnit、Null Safetyを顧客開発と同じ形で破ると落ちる規約にする。

| WP | 作業 | 主な成果物 | 検証・判定 |
|---|---|---|---|
| B1 | Feature Template設計・実装 | 最小Tier 1 / Tier 2 template、生成・利用手順 | COMPLETE（2026年8月24日、Gate 1〜5 Owner Review・Windows / Ubuntu CI済み） |
| B2 | ArchUnit APIとrule matrix設計 | 安定した公開entry point、Phase 1a適用rule、ADR message contract、compliant fixture仕様 | COMPLETE（2026年8月24日、Gate 1〜5 Owner Review済み） |
| B3 | ArchUnit rules再実装 | `koiki-archunit-rules`、positive / negative fixture | COMPLETE（2026年8月25日、Gate 1〜5 Owner Review・Windows / Ubuntu CI済み） |
| B4 | Null Safety正式化 | Parent compiler設定、`@NullMarked`方針、negative test | COMPLETE（2026年8月25日、Windows / Ubuntu CI・Gate 2 Owner Review済み） |
| B5 | Template統合検証 | ArchUnit、Level 0、NullAwayを両Tier templateへ適用 | COMPLETE（2026年8月25日、Gate 1〜4 Owner Review・PR #13 Windows / Ubuntu CI済み） |

**Exit criteria:**

- DoD 1a-1、1a-2、1a-4をRepository内の再現可能な検証で満たす。
- Rule 19の近似検査を完全なdata-flow保証と表現しない。
- 後続Phaseで方式未確定のrule、package、dependencyを先行導入しない。

### 7.3 Milestone C — Distribution and Compatibility Proof

**目的:** 正式配布形態、Public API互換性、Java runtime互換性をRepository外から実証する。

| WP | 作業 | 主な成果物 | 検証・判定 |
|---|---|---|---|
| C1 | 内部snapshot公開 | BOM、Parent、Contract、ArchUnit rulesの同一version artifact | COMPLETE（2026年8月26日、Gate 1〜4 Owner Review・GitHub Packages公開・Evidence記録済み） |
| C2-0 | C2開始前ドキュメント同期 | Root README、実行計画、Repository Tree、Validation index | COMPLETE（2026年8月26日、Root POM・実ツリー・C1 Evidenceとの整合確認済み） |
| C2 | Repository外Consumer | 独立Consumerと再現手順 | COMPLETE（2026年8月26日、Gate 1〜4 Owner Review・local PAT / remote `GITHUB_TOKEN`検証・Evidence記録済み）。snapshotだけから解決し、意図的違反とADR messageを確認 |
| C3 | Public API / japicmp | API inventory、baseline artifact、除外・例外方針 | COMPLETE（2026年8月27日、Gate 1〜4 Owner Review・local / remote CI・required check・Evidence記録済み）。Public API破壊と未承認追加で失敗、`internal`変更は規約どおり許容 |
| C4 | Java runtime matrix | G6の起動fixture、Java 21 build artifact | COMPLETE（2026年8月27日、Gate 1〜4 Owner Review・local / remote Java 21 / 25・3 negative guards・required check・Evidence記録済み） |
| C5 | Phase 1a Closeout | Validation、ADR / Skill / Baseline更新、Walking Skeleton残置物の処置 | Gate 1〜3 ACCEPTED / Gate 4 LOCAL COMPLETE・REMOTE PENDING（2026年8月27日）。全DoD traceability、CI成功、Owner Reviewで完了判定 |

**Exit criteria:**

- DoD 1a-3、1a-5、1a-6を正式な配布・互換性経路で満たす。
- 外部Consumerがlocal installまたはRoot Reactorへ依存しない。
- Phase 1aで作成したPublic APIとjapicmp baselineが一致する。
- 正式代替の成立後、`main`由来の`walking-skeleton/ws-smoke-*`等を正式本線から除去する。

## 8. DoD Traceability

| DoD | 主担当WP | 必須証拠 |
|---|---|---|
| 共通-1 Baseline | A1 / A2 / C5 | Baseline review、実効POM、dependency tree、runtime matrix |
| 共通-2 ADR | 各Gate / C5 | 追加・改訂ADRとOwner承認。判断不要の場合は理由を記録 |
| 共通-3 CI | A4 / B5 / C5 | 全quality gate成功 |
| 共通-4 Skills | C5 | 新規規約のSkill反映要否reviewとSkill利用結果 |
| 共通-5 Flyway | C5 | Phase 1aではtable追加なし。該当なしを明示 |
| 1a-1 Feature Template | B1 / B5 | Tier 1 / Tier 2生成と`mvn verify`成功 |
| 1a-2 Architecture violation | B2 / B3 / A4 | 5必須違反、CI failure、ADR・影響・修正方法 |
| 1a-3 External Consumer | C1 / C2 | snapshot dependency解決と外部違反検出 |
| 1a-4 NullAway | A2 / B4 / B5 | 正常→失敗→復元成功 |
| 1a-5 japicmp | C3 | Public API破壊によるbuild failure |
| 1a-6 Java runtime | A2 / C4 | Java 21 build、class 65、Java 21 / 25同一artifact起動 |

## 9. CI Quality Gate計画

| 経路 | 実行内容 | 頻度 |
|---|---|---|
| PR fast gate | Java 21 `clean verify`、Enforcer、NullAway、ArchUnit、Level 0、unit test | 全PR |
| Compatibility gate | 主系統成果物のJava 21 / 25 runtime起動 | nightlyおよびMilestone C |
| Distribution gate | snapshot公開後のRepository外Consumer | Milestone C、release unit変更時 |
| API gate | japicmp baseline比較 | Public API変更時、Milestone C |
| Negative gate | 5 ArchUnit違反、NullAway違反、Public API破壊の期待失敗 | 専用fixture変更時、Milestone exit |

CIの具体的workflow、credential、artifact保持はG4で決定する。秘密値をRepositoryへ保存しない。

## 10. 見積もりと再校正

Phase 0で承認されたPhase 1aの直接見積もり45〜74標準人日、20% contingency後の
54〜89標準人日、AI支援Owner稼働日24〜53日を開始rangeとして維持する。

| Milestone | 直接見積もり配分 | AI支援Owner稼働日配分 | 不確実性 |
|---|---:|---:|---|
| A | 14〜23標準人日 | 7〜14日 | Maven座標、Public API、CI / repository選定 |
| B | 17〜28標準人日 | 9〜18日 | ArchUnit誤検出、Feature Templateの過剰固定、NullAway境界 |
| C | 14〜23標準人日 | 8〜21日 | artifact公開、外部Consumer、japicmp baseline、runtime環境 |
| 合計 | 45〜74標準人日 | 24〜53日 | 承認済み開始rangeと一致 |

Phase 1a準備作業は完了しているが、Owner稼働時間を一貫して計測していないため、根拠のない日数を
開始rangeから控除しない。Milestone A完了時にWP別実績、待ち時間、AI支援比率、手戻りを記録し、
Milestone B / Cのrangeを最初に再校正する。各Milestone終了時にも同じ方法で更新する。

### 10.1 Milestone A・B完了時の再校正記録

2026年8月25日にMilestone A・Bの完了を確認したが、A1〜B5のOwner稼働時間、待ち時間、
AI支援比率および手戻りを一貫した基準で計測していない。このため、過去の稼働日を推測で補完せず、
Milestone Cの当初rangeは現時点で維持する。

Milestone CはC1着手時に、artifact repository、外部Consumer、japicmp、Java 21 / 25 runtime環境の
利用可否とB5 CIの実測を用いて再校正する。

## 11. Commit・Review境界

原則として次を独立commitまたはreview単位にする。検証結果や設計判断を、無関係な実装へ混在させない。

1. 本実行計画とG1候補およびG2〜G6のOwner Review結果
2. Root Reactor / Parent / BOM / WrapperとG1実効検証
3. Architecture Contract
4. CI骨格
5. Feature Template
6. ArchUnit API・rules・fixture
7. NullAway統合
8. snapshot公開・外部Consumer
9. japicmp・Public API baseline
10. Java runtime matrix
11. Phase 1a Validation・Closeout・Walking Skeleton残置物処置

実装途中の小さな修正は関連WPへ含められるが、Public API、dependency baseline、Ownership、
Phase scopeを変更する修正は独立してreviewする。

## 12. リスクと停止条件

| リスク | 対応 | 停止条件 |
|---|---|---|
| 一時artifact・packageの昇格 | B / C区分の固定Commitを証拠としてだけ参照 | `dev.koiki.walkingskeleton`が正式成果物へ入った場合 |
| Public APIの早期固定 | G3 / G5、API inventory、japicmp対象review | Ownershipまたは利用者を説明できない公開型が必要になった場合 |
| ArchUnit誤検出 | compliant / negative fixture、ADR message review | 正常templateが違反し、修正方針を提示できない場合 |
| 外部Consumerがlocal環境だけで成功 | snapshot repository経由の独立build | local installまたはReactor参照なしで解決できない場合 |
| Java 21 / 25検証の混同 | artifact hash、class major、再compile禁止 | runtimeごとに別artifactを生成した場合 |
| 後続Phaseの混入 | D区分checklist、dependency / module review | Runtime、Security、Reference業務等がPhase 1a完了条件に必要となった場合 |
| CI / repository未確保 | G4をMilestone AのGateとする | Milestone Cの正式経路を実演できない場合 |

停止条件に該当した場合は暫定実装で迂回せず、Ownerへ報告し、ADR、scope、内部Milestoneまたは
外部環境の見直しを行う。

## 13. Owner Review観点

- Milestone A〜Cは、それぞれ独立した実演証拠を作れる単位か。
- G1〜G6はPublic API、配布、CIを実装前に判断する十分なGateか。
- Feature TemplateとProject Template、内部snapshotと正式releaseを混同していないか。
- DoD 1a-1〜1a-6と全Phase共通DoDに、担当WPと証拠が割り当てられているか。
- Java 21 buildとJava 21 / 25 runtimeを正しく分離しているか。
- Walking Skeletonのcode昇格と後続Phase成果物の混入を防げるか。
- 見積もりrangeを根拠なく短縮せず、Milestoneごとに再校正する計画か。

### 13.1 Owner Review結果

| 項目 | 判定 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月21日 |
| Scope | §1〜§14のPhase 1a実行計画、G1 baseline候補、およびG2〜G6の個別設計判断 |
| Conditions | G1はA2の正式POM、実効POM、dependency treeおよびbuild結果に基づき`ACCEPTED`とする。A3以降の各Milestone実装・Validation・Closeoutは本承認と分離して実証する |
| Rationale | Walking Skeletonを直接昇格させず、Ownership、Public API、配布、CIおよびruntime互換性を独立WPと判断Gateで管理する実行計画として妥当である |

## 14. 参照

- `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §27.2、§27.4
- `../architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md`
- `../architecture/KOIKI-JavaWeb-FW_Phase_Estimate_Feasibility_v0.1.md` §3
- `../architecture/KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md`
- `KOIKI-JavaWeb-FW_Phase1a_WalkingSkeleton_Transition_Inventory_v0.1.md`
- `../architecture/validation/walking-skeleton-build-foundation.md`
- `../architecture/validation/walking-skeleton-archunit-distribution.md`
- `../architecture/validation/walking-skeleton-phase0-completion.md`

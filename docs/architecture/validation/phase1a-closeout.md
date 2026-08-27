# Phase 1a C5 Closeout Validation

## 1. Status

- Work Package: C5 Phase 1a Closeout
- Status: Gate 1〜2 ACCEPTED / Gate 3 READY FOR OWNER REVIEW
- Date: 2026-08-27
- Architecture Owner: Shuichi Kataoka
- Base commit: `ca37e5c`（C4 PR #19 merge）
- Working branch: `feature/phase1a-closeout`
- Ownership: Architecture / Tooling

本記録はC5の判断、Repository hygiene、DoD traceabilityおよび最終CIを集約する正本である。
Gate 3時点ではPhase 1aの完了を宣言せず、Gate 3のOwner ReviewとGate 4の最終検証・
remote CI・Owner Reviewを残す。

## 2. Gate plan

| Gate | 内容 | 状態 |
|---|---|---|
| Gate 1 | read-only調査、5項目の境界確認、実装計画 | ACCEPTED（2026-08-27） |
| Gate 2 | baseline同期、Walking Skeleton残置物処置、Repository hygiene | ACCEPTED（2026-08-27） |
| Gate 3 | DoD 1a-1〜1a-6、共通DoD、ADR / Skill / Flyway判定 | READY FOR OWNER REVIEW |
| Gate 4 | local最終検証、PR CI、Owner Review、main最終CI | PENDING |

## 3. Gate 1で承認した境界

1. C5はArchitecture / Tooling closeoutに限定し、新規機能、Public API、後続Phase成果物を追加しない。
2. Spring Boot 4.1.1、Maven 3.9.16、Java 21 build / Java 21・25 runtimeをPhase 1a baselineとする。
   Spring Modulithは2.1.0を維持し、2.1.1への更新はPhase 1b開始時に再reviewする。
3. `walking-skeleton/`とC4で代替済みの旧Java 25補助scriptを正式本線から除去し、証拠は
   `walking-skeleton` branch、固定snapshot `b3ba79f`、Git履歴、Validation文書に保持する。
4. ADR / Skillに新規判断がないこと、Phase 1aではtable・migration追加がなくFlywayが該当しないことを
   理由付きで記録し、Phase 1aの6件と共通5件のDoDを追跡する。
5. CI待ち時間を抑えるためlocal commitをまとめ、PR CIは原則2回以内とし、main merge後の最終CIは
   closeout記録を残した上で非同期確認する。C5ではworkflowの検証論理自体を最適化対象にしない。

## 4. Gate 1 read-only evidence

- `main`、`origin/main`、作業開始点は`ca37e5c`で一致し、C4までのmergeと最終CI完了を確認した。
- Root ReactorはBOM、Parent、Architecture Contract、ArchUnit Rulesの正式4 moduleだけである。
- GitHub Packagesの4成果物、Repository外Consumer、Public API compatibility、Java runtime matrixの
  証拠が成立している。
- main rulesetはstrict、bypassなしで、`Verify (ubuntu-24.04)`、`Public API Compatibility`、
  `Java Runtime Compatibility`をrequired checkとしている。
- 通常CIは直近で約4.5分、うちFeature Template検証が約221秒を占めた。C5では安全性を下げず、
  commit / push / CI確認の回数をまとめる運用で待ち時間を抑える。
- Phase 1aにはFlyway、table、migration SQLの追加がない。

Baselineの外部確認先は[Spring Boot](https://spring.io/projects/spring-boot)、
[Apache Maven release history](https://maven.apache.org/docs/history.html)、
[Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)、
[Spring Modulith reference documentation](https://docs.spring.io/spring-modulith/reference/)とした。

## 5. Gate 2 implementation

### 5.1 正式本線から除去する対象

| 対象 | 除去理由 | 証拠・正式代替 |
|---|---|---|
| `walking-skeleton/ws-smoke-lib`、`ws-smoke-app` | Phase 0一時座標・sourceを正式成果物へ昇格させない | 固定snapshot `b3ba79f`、Architecture Contract、ArchUnit Rules、Feature Template、C1〜C4 Validation |
| `walking-skeleton/negative-tests/nullaway` | 正式なNullAway positive / negative fixtureが成立済み | `build-support/null-safety/`、`phase1a-null-safety.md` |
| `walking-skeleton/Walking-Skeleton-plan-20260812.md` | 作業メモをPhase 1a正本にしない | Git履歴、承認済みPhase 0 Validation |
| `run-with-java25.ps1`、`verify-class-version.ps1` | C4の同一artifact方式と3 negative guardsで代替済み | `runtime-compatibility-fixture/`、`runtime-compatibility.yml`、`phase1a-java-runtime-matrix.md` |

### 5.2 維持・同期する対象

- Maven Wrapper bootstrapは保守用に維持し、既存Wrapperと一致する`bin`方式へ修正する。
- Root README、Build Support、workflow、reserved ownership、Repository Architecture、Repository Treeを
  現在の4 module構成とC3 / C4検証経路へ同期する。
- Baseline Compatibilityと`BUILD-BASELINE.json`をPhase 1a実効値へ同期する。
- Walking Skeleton引継ぎ台帳へC5処置状況を追記し、削除と証拠保持の対応を明示する。

## 6. Preliminary DoD evidence map

Gate 3で内容、判定、ADR / Skill / Flyway reviewを確定する。Gate 2では証拠の所在だけを確認する。

| DoD | 主な既存Evidence | Gate 2状態 |
|---|---|---|
| 1a-1 Feature Template | `phase1a-feature-template.md`、`phase1a-template-integration.md` | LOCATED |
| 1a-2 Architecture Contract / Level 0 | `phase1a-architecture-contract.md`、`phase1a-template-integration.md` | LOCATED |
| 1a-3 同一version release unit / 外部Consumer | `phase1a-internal-snapshot.md`、`phase1a-external-consumer.md` | LOCATED |
| 1a-4 ArchUnit / Null Safety | `phase1a-archunit-rules.md`、`phase1a-null-safety.md` | LOCATED |
| 1a-5 Public API compatibility | `phase1a-public-api-compatibility.md` | LOCATED |
| 1a-6 Java runtime matrix | `phase1a-java-runtime-matrix.md` | LOCATED |
| 共通DoD 5件 | Baseline、ADR、CI、Skills、Flyway | GATE 3 REVIEW PENDING |

## 7. Gate 2 acceptance criteria

- trackedな`walking-skeleton/` sourceと旧2 scriptsが正式本線候補から除去されている。
- Root Reactorが正式4 moduleのままで、Public API、dependency、後続Phase成果物を追加していない。
- `BUILD-BASELINE.json`、Markdown link、Repository Tree、Validation indexが整合する。
- Maven root verificationが成功し、削除対象の正式代替と証拠保持先を追跡できる。
- Architecture OwnerがGate 2の内容と結果を確認する。

## 8. Gate 2 verification result

2026年8月27日に次を確認し、Gate 2をOwner Reviewへ提示した。

| 確認 | 結果 |
|---|---|
| 正式本線からの残置物処置 | Walking Skeleton 10 filesと旧Java 25補助script 2 filesを正式本線から除去 |
| Root Reactor | BOM、Parent、Architecture Contract、ArchUnit Rulesの正式4 moduleだけで一致 |
| Baseline JSON | PowerShell `ConvertFrom-Json`成功 |
| Repository文書 | local Markdown link検査成功、`git diff --check`成功 |
| Maven回帰 | `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`成功 |
| Tests | Architecture Contract 4件、ArchUnit Rules 65件、合計69件。failure / error / skipは0 |

Gate 2ではFeature Template統合の再実行、remote pushおよびCIを行っていない。正式4 moduleと文書・
残置物処置に対するlocal回帰へ限定し、重い統合検証とremote required checksはGate 4でまとめて行う。

### 8.1 Gate 2 Owner Review結果

| 項目 | 結果 |
|---|---|
| Decision | ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月27日 |
| Scope | §5〜§8のBaseline同期、残置物処置、正式代替、Repository hygiene、local検証 |
| Commit | `39f09eb`（`chore: complete C5 repository hygiene`） |

Architecture OwnerがGate 2の内容と結果を承認し、上記commitへ確定した。remote required checksを
実行していないことは承認scopeどおりであり、Gate 4の最終条件として維持する。

## 9. Gate 3 Phase 1a DoD traceability

グランドデザインv0.2 §27.4の完了条件を、Owner承認済みの再現可能なEvidenceへ照合した。
判定はDoD本文を変更せず、既存実演結果に基づく。

| DoD | 判定 | 実演・Evidence |
|---|---|---|
| 1a-1 | SATISFIED | [`phase1a-feature-template.md`](phase1a-feature-template.md)でTier 1 `catalog`とTier 2 `approval`を生成し、6-project Reactor、Level 0、両Tier testを成功。B5の[`phase1a-template-integration.md`](phase1a-template-integration.md)でArchUnit / NullAway統合後も再生成・`verify`・復元を実証 |
| 1a-2 | SATISFIED | [`phase1a-archunit-rules.md`](phase1a-archunit-rules.md)で必須5違反を独立fixtureとして検出し、25 failure reportにrule ID、ADR、違反内容、影響、修正を確認。Public compositeとCI経路を含めGate 1〜5 ACCEPTED |
| 1a-3 | SATISFIED | [`phase1a-internal-snapshot.md`](phase1a-internal-snapshot.md)の4成果物公開と[`phase1a-external-consumer.md`](phase1a-external-consumer.md)の別Repositoryを連続証拠とし、空local repository / fresh runnerから`koiki-archunit-rules`を取得して違反を検出。DoD 1a-3 COMPLETE |
| 1a-4 | SATISFIED | [`phase1a-null-safety.md`](phase1a-null-safety.md)でNullAway positive → expected negative → restoreをlocal / CIで実証し、B5でも両Tierの負例と復元を確認 |
| 1a-5 | SATISFIED | [`phase1a-public-api-compatibility.md`](phase1a-public-api-compatibility.md)でC1 immutable baselineに対するjapicmp正常比較、Public API破壊と未承認追加の期待failure、package-private変更の許容をlocal / fresh runnerで実証。DoD 1a-5 COMPLETE |
| 1a-6 | SATISFIED | [`phase1a-java-runtime-matrix.md`](phase1a-java-runtime-matrix.md)でJava 21 build、class major `65`、同一SHA-256 JARのJava 21 / 25起動、3 negative guards、job間artifact受け渡しを実証。DoD 1a-6 COMPLETE |

最重要の1a-2 / 1a-3は、Repository内fixtureだけでなく、公開artifactを取得する独立Consumerでも
`KOIKI-ARCH-001`、ADR-022、影響、修正および違反箇所を確認している。

## 10. Gate 3 全Phase共通DoD review

| §27.2 | 判定 | 根拠・残条件 |
|---|---|---|
| 1. Spring Boot baseline | SATISFIED | 2026年8月27日の公式情報と実効POMを照合し、OSSサポート中の最新minor 4.1.1へ同期。Baseline Compatibilityと`BUILD-BASELINE.json`へ記録 |
| 2. ADR / Owner approval | SATISFIED | Phase 1aの各Gateは既存ADRとグランドデザインの具体化であり、新しいarchitecture decisionを導入していない。G1〜G6と各WPのADR要否・Owner承認を実行計画とValidationに記録 |
| 3. CI quality gates | BASELINE SATISFIED / GATE 4 RECONFIRMATION | C4 merge commit `ca37e5c`で通常CI、Public API Compatibility、Java Runtime Compatibilityが成功。C5最終差分に対する3 required checksはGate 4で再確認し、それまではPhase 1a COMPLETEにしない |
| 4. Agent Skills | SATISFIED | Phase 1aで追加した機械検査規則はArchUnit / NullAway / japicmpへ保持し、Skillへ複製しない。既存2 SkillがOwnership・Tier・責務判断を網羅し、C5でも`koiki-project-overview`を適用してArchitecture / Tooling境界と後続Phase保留を維持 |
| 5. Table / Flyway ownership | NOT APPLICABLE | Phase 1aではtable、migration SQL、Flyway dependency / workflowを追加していない。Flyway Runtime FoundationはPhase 1bのため、配置すべき新規migrationが存在しない |

共通DoD 3だけは、既存成果物の証拠不足ではなくC5差分に対する最終remote再確認を残す。このため
Gate 3でDoD本文を緩和したり例外承認したりせず、Gate 4をPhase完了の必須条件とする。

## 11. ADR / Skill / Flyway判定

### 11.1 ADR

Phase 1aはADR-001〜003、005、022、023、025、041、045等の承認済み判断を、正式Maven成果物、
Public API、機械検査、内部snapshot、ConsumerおよびCIへ具体化した。Maven座標、内部snapshot経路、
japicmp fixture、runtime fixture、C5残置物処置はいずれもPhase 1a内部の実装・検証方法であり、
Framework昇格、採用Level、Support、外部releaseまたは後続Phase scopeを変更しない。

したがって新規ADRまたは既存ADR改訂は`NOT REQUIRED`と判定する。再判断triggerは、Public API / Maven
release unitの変更、Spring Modulith採用Level変更、正式release / support開始、Runtime Foundation成果物の
前倒し、または既存ADRの前提を否定する検証結果が生じた場合とする。

### 11.2 Agent Skills

正本の`koiki-project-overview`と`koiki-business-feature-work`を全件reviewした。Phase 1aで追加した
ArchUnit rule、NullAway、japicmp、Java runtime guardは機械検査が所有し、Skill更新規律にいう新しい
判断基準ではない。Ownership、Tier、責務、Spring Modulith Level、後続Phase保留の既存判断フローに
不足はないため、Skill変更は`NOT REQUIRED`と判定する。

### 11.3 Flyway

tracked treeに`.sql`または`db/migration`は0件であり、POM / workflowにもFlyway参照は0件である。
Phase 1aはBuild Foundationでtableを追加せず、Flyway二階層の正式実装はPhase 1b Runtime Foundationで
扱う。共通DoD §27.2-5は`NOT APPLICABLE`であり、空migration、仮Starterまたはdependencyを生成しない。

## 12. Gate 3 verification and review target

| 確認 | 結果 |
|---|---|
| Phase 1a DoD 1a-1〜1a-6 | 6 / 6 SATISFIED |
| 共通DoD | Baseline、ADR、SkillsはSATISFIED、Flywayは理由付きNOT APPLICABLE |
| CI | C4基準でSATISFIED、C5最終差分の再確認をGate 4へ明示 |
| ADR | 追加・改訂NOT REQUIRED。理由とrevisit triggerを記録 |
| Skills | 2正本をreviewし、変更NOT REQUIRED。`koiki-project-overview`適用結果を記録 |
| Flyway | SQL / migration / build参照0件、Phase 1bへ保留 |
| Scope | 正式4 module、Public API、dependency、後続Phase成果物に変更なし |

Gate 3の判定対象は§9〜§12である。Architecture Owner承認後にGate 3を`ACCEPTED`とし、Gate 4では
local統合検証、PR、3 required checks、Owner Reviewおよびmain最終CIを実施する。

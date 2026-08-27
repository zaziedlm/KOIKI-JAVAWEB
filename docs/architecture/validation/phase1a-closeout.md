# Phase 1a C5 Closeout Validation

## 1. Status

- Work Package: C5 Phase 1a Closeout
- Status: Gate 1 ACCEPTED / Gate 2 READY FOR OWNER REVIEW
- Date: 2026-08-27
- Architecture Owner: Shuichi Kataoka
- Base commit: `ca37e5c`（C4 PR #19 merge）
- Working branch: `feature/phase1a-closeout`
- Ownership: Architecture / Tooling

本記録はC5の判断、Repository hygiene、DoD traceabilityおよび最終CIを集約する正本である。
Gate 2時点ではPhase 1aの完了を宣言せず、Gate 3のDoD・Governance reviewとGate 4の
Owner Review・remote CIを残す。

## 2. Gate plan

| Gate | 内容 | 状態 |
|---|---|---|
| Gate 1 | read-only調査、5項目の境界確認、実装計画 | ACCEPTED（2026-08-27） |
| Gate 2 | baseline同期、Walking Skeleton残置物処置、Repository hygiene | READY FOR OWNER REVIEW |
| Gate 3 | DoD 1a-1〜1a-6、共通DoD、ADR / Skill / Flyway判定 | PENDING |
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

2026年8月27日に次を確認し、Gate 2をOwner Reviewへ提示できる状態とした。

| 確認 | 結果 |
|---|---|
| 正式本線からの残置物処置 | Walking Skeleton 10 filesと旧Java 25補助script 2 filesがworktreeから除去され、Git差分で削除として追跡される |
| Root Reactor | BOM、Parent、Architecture Contract、ArchUnit Rulesの正式4 moduleだけで一致 |
| Baseline JSON | PowerShell `ConvertFrom-Json`成功 |
| Repository文書 | local Markdown link検査成功、`git diff --check`成功 |
| Maven回帰 | `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`成功 |
| Tests | Architecture Contract 4件、ArchUnit Rules 65件、合計69件。failure / error / skipは0 |

Gate 2ではFeature Template統合の再実行、remote pushおよびCIを行っていない。正式4 moduleと文書・
残置物処置に対するlocal回帰へ限定し、重い統合検証とremote required checksはGate 4でまとめて行う。
Gate 2の`ACCEPTED`はArchitecture Owner Review後に記録する。

# Phase 1a Null Safety — B4実装・検証記録

**実装・検証日:** 2026年8月25日<br>
**対象branch:** `feature/phase1a-null-safety`<br>
**状態:** IMPLEMENTED / LOCAL VERIFIED / REMOTE CI PENDING<br>
**Ownership:** Tooling<br>
**開始baseline:** `main` / `86b0033`（B3 PR #10 merge）

## 1. 目的と完了条件

Phase 1a実行計画のB4とDoD 1a-4に基づき、ParentのJSpecify / NullAway設定が
`@NullMarked`配下のJava production sourceへ実効適用され、正常、意図的違反、正常復元の三経路を
Repository内とCIで再現できることを証明する。

B4の完了条件は次のとおりとする。

1. Parentの実効compiler設定、annotation processor version、JSpecify dependency境界を記録する。
2. 正常なnull-marked production sourceがMaven Wrapperで成功する。
3. 意図的なnull違反がNullAway固有diagnosticを伴って非0終了する。
4. 同じ検証経路を正常sourceへ戻すと再び成功する。
5. 通常production source、Root Reactorおよび配布artifactへ意図的違反を残さない。
6. Windows / Ubuntu CIで同じ検証scriptを実行する。
7. B5、C1以降または後続Phaseの成果を先行実装しない。

## 2. Owner Review Gate

| Gate | Review対象 | 状態 |
|---:|---|---|
| 1 | 隔離fixture、profile切替、三段階script、CI統合、stop condition | ACCEPTED（2026年8月25日、Shuichi Kataoka） |
| 2 | ローカル実装証拠、Windows / Ubuntu CI、Deferred、B4最終判定 | PENDING |

Gate 1では、tracked production sourceを一時変更してGit復元する方式を採らず、Reactor外fixtureの
source directoryをprofileで切り替える方式を承認した。Gate 2はremote CI成功後に最終判定する。

## 3. ParentとJSpecifyの実効構成

`koiki-parent/pom.xml`を継承する正式Architecture ContractとB4 fixtureのeffective POMを確認した。

| 項目 | 実効値 |
|---|---|
| Java release | 21 |
| Maven Compiler Plugin | 3.15.0 |
| compiler process | `fork=true` |
| Error Prone | 2.50.0、annotation processor path |
| NullAway | 0.13.8、annotation processor path |
| Error level | `-Xep:NullAway:ERROR` |
| 対象境界 | `-XepOpt:NullAway:OnlyNullMarked=true` |
| JSpecify mode | `-XepOpt:NullAway:JSpecifyMode=true` |
| JSpecify | 1.0.0、BOM管理、fixtureではcompile direct dependency |

Parent設定はA2の承認baselineどおり実効動作したため、B4では変更していない。JSR 305 annotation、
新規compiler pluginまたは抑制設定も追加していない。

## 4. `@NullMarked`方針と現状

NullAway対象とするproduction packageは、そのpackage自身の`package-info.java`で`@NullMarked`を宣言し、
subpackageへの暗黙適用に依存しない。annotationをcompileするmoduleはJSpecifyを直接dependencyとして宣言する。

B4開始時点の正式production sourceは次の2 packageであり、いずれも方針を満たす。

| Module | Production package | 宣言 |
|---|---|---|
| `koiki-architecture-contract` | `org.koikifw.architecture` | `package-info.java`で`@NullMarked` |
| `koiki-archunit-rules` | `org.koikifw.archunit` | `package-info.java`で`@NullMarked` |

Feature Templateが生成する各subpackageへの最終適用とnegative integrationはB5で扱う。B4ではTemplate source、
業務moduleまたは将来packageを変更しない。

## 5. 検証fixture設計

`build-support/null-safety/`へTooling所有の非配布fixtureを新規実装した。

| 対象 | 役割 |
|---|---|
| `verification/pom.xml` | Parentを継承し、KOIKI BOMとfixtureを同一検証Reactorへ含める |
| `verification/fixture/pom.xml` | 既定のpositive sourceと`nullaway-negative` profileのnegative sourceを切り替える |
| `src/positive/java` | `@NullMarked`配下で非null値を返す正常production source |
| `src/negative/java` | `@NullMarked`配下で意図的に`null`を返す隔離production-source fixture |
| `verify-null-safety.ps1` | positive、expected failure、restoreの終了コードとdiagnosticを検査する |

fixtureはRoot `pom.xml`のmoduleではなく、Root Reactor、通常production source、正式release unitおよび
配布artifactへ含まれない。BOMを検証Reactorへ含めるため、`install`済みlocal repositoryにも依存しない。
profileはMavenの`sourceDirectory`だけを切り替え、検証中にtracked fileを書き換えない。

## 6. ローカル検証結果

### 6.1 Environment

| 項目 | 値 |
|---|---|
| OS | Windows 11 / amd64 |
| Java | Eclipse Temurin 21.0.12 |
| Maven | Repository同梱Wrapper / Maven 3.9.16 |
| Encoding | UTF-8 |

社内SSL inspection proxyのRoot CAをRepository付属scriptでJDK 21の`cacerts`へ追加し、Wrapperの
初回取得とdependency解決を正常化した。ユーザーPATHには`%JAVA21_HOME%\bin`を追加した。

### 6.2 Positive → negative → restore

実行command:

```powershell
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

| 段階 | Maven source | 期待 | 実結果 |
|---|---|---|---|
| Positive | `src/positive/java` | exit 0 | BUILD SUCCESS / exit 0 |
| Negative | `src/negative/java` | 非0、NullAway diagnostic | BUILD FAILURE / exit 1 |
| Restore | `src/positive/java` | exit 0 | BUILD SUCCESS / exit 0 |

negativeでscriptが確認したdiagnostic:

```text
[NullAway] returning @Nullable expression from method with @NonNull return type
```

restore後のfixture JARを`javap -public`で確認し、公開methodは正常fixtureの
`normalize(java.lang.String)`だけで、negative側の`deliberateViolation()`は存在しない。

### 6.3 Regression

```powershell
.\mvnw.cmd --offline --batch-mode --no-transfer-progress clean verify
pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1
```

| 検証 | 結果 |
|---|---|
| Root Reactor | 5 moduleすべてSUCCESS |
| Architecture Contract | 4 tests、failure / error 0 |
| ArchUnit Rules | 64 tests、failure / error 0 |
| Tier 1 / Tier 2 Feature Template | 6 moduleすべてSUCCESS、runtime dependency境界もPASS |
| `git diff --check` | PASS |

JDK CDS archive mismatch、Surefire native stream、SLF4J NOP providerの既知通知はあるが、test件数、
artifact生成、NullAway diagnosticまたはbuild結果には影響しない。通知を消すためのproduction dependencyは
追加していない。

## 7. CI経路

`.github/workflows/ci.yml`の既存Windows 2025 / Ubuntu 24.04 matrixへ、同じ
`verify-null-safety.ps1`を実行するstepを追加した。scriptはWindowsで`mvnw.cmd`、Linuxで`mvnw`を選択し、
両OSでpositive、expected failure、restoreを同じ順序とdiagnostic契約で検査する。

CIの権限は引き続き`contents: read`だけで、artifact公開、secret、`packages: write`またはlocal installを追加しない。
remote CIのrun / job URLと結果はPR実行後に本書へ追記し、Gate 2を判定する。

## 8. Walking Skeletonとの差分

Walking Skeletonでは正常sourceへ一時的に`return null`を追加し、検証後に手動復元していた。B4はその検証条件と
diagnosticだけを引き継ぎ、code、`dev.koiki.walkingskeleton` package、一時Maven座標および手動復元方式を
再利用していない。negative sourceは専用directoryへ隔離し、通常buildを恒常的に失敗させない。

## 9. Deferredと最終判定

- B5: Tier 1 / Tier 2 Feature TemplateへArchUnit、Level 0、NullAwayを最終統合する。
- C1 / C2: snapshot公開とRepository外Consumerを検証する。
- C3: japicmpとPublic API baselineを実装する。
- C4: Java 21でbuildした同一artifactをJava 21 / 25 runtimeで検証する。
- 後続Phase: Runtime、Security、Reference業務、REST、SPA、Flyway、MyBatisを扱う。

B4実装は新規Public API、正式artifact、migrationまたはarchitecture decisionを追加しないため、ADR追加・変更は
不要と判定した。機械検査はfixtureとCIが所有するため、KOIKI Skillへの規則複製も行わない。

ローカル証拠は完了している。remote Windows / Ubuntu CI成功とOwner Review Gate 2承認後に、B4を`COMPLETE`とする。

# Phase 1b着手前 MyBatis metadata guard検証

## 1. 目的

グランドデザイン§11.7および全Phase共通DoD項番30に従い、
`PersistenceModel.SEPARATED`が未提供の期間に
`persistence = MYBATIS, persistenceModel = SHARED`が承認済みmetadataとして通過しないことを、
Phase 1b着手前に機械検査へ反映する。

## 2. Scopeと判断

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b着手前のPhase 1a architecture contract補正、COMPLETE / ACCEPTED |
| Ownership | Framework |
| 対象 | `koiki-archunit-rules`のKOIKI-ARCH-008 |
| Public API | 変更なし |
| japicmp baseline | 変更なし |
| ADR | ADR-039の既存判断を明確化。新規判断なし |
| 後続判断 | `MYBATIS`定数の維持／削除と`SEPARATED`提供時期は保留 |

## 3. 実装

- KOIKI-ARCH-008のADR参照へADR-039を追加した。
- `MYBATIS`宣言を検出した場合、`SEPARATED`必須かつ未提供であることをfailure reportへ出力する。
- `JPA + SHARED`だけを現時点の正常系として検証する。
- 既存の`metadata.rich` fixtureは`MYBATIS + SHARED`の負例として維持する。
  これにより`ModuleMetadata`のenum値読取テストを失わず、同じ宣言がrule8では拒否されることを検証する。
- `PersistenceTechnology`、`PersistenceModel`、`KoikiModule`およびPublic API inventoryは変更しない。

## 4. 検証

| 検証 | 結果 |
|---|---|
| `./mvnw -pl koiki-archunit-rules -am test` | SUCCESS。Architecture Contract 4件、ArchUnit Rules 66件、failure / error / skip 0 |
| `./mvnw clean verify` | SUCCESS。正式5-project Reactor、Architecture Contract 4件、ArchUnit Rules 66件、failure / error / skip 0 |
| `verify-public-api-fixtures.ps1` | SUCCESS。package-private変更はinventory MATCH / japicmp `NONE`、return type破壊と未承認追加は期待failure |
| `git diff --check` | SUCCESS |

新規テスト`rule8RejectsMybatisWithoutSeparatedPersistenceModel`により、failure reportへ
`KOIKI-ARCH-008`、ADR-039、`MYBATIS`、`PersistenceModel.SEPARATED`、未提供理由が含まれることを確認した。
正式なC1 timestamped baselineとのjapicmp比較は認証を必要とするためlocalでは再実行せず、PRおよび
main最終`Public API Compatibility` required checkで確認した。Public API sourceと`public-api.txt`には
差分がない。

## 5. Remote Evidence

### 5.1 PRとmerge identity

| 項目 | 値 |
|---|---|
| Pull Request | [#22](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/22) `fix: reject unsupported MyBatis shared metadata` |
| State | MERGED |
| Final PR head | `a3b582addc322468258c6d0712ef8b2f43dbad00` |
| Merge commit | `776e7729233cd14d46a2c0bc1ee6b0dd5f5a8fdf` |
| Merged at | 2026年8月27日 18:57:49 JST |
| Merged by | `zaziedlm`（Shuichi Kataoka） |

### 5.2 PR required checks

| Workflow / job | Evidence | 結果 |
|---|---|---|
| `CI` / `Verify (ubuntu-24.04)` | [run 33060465623 / job 98477675625](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060465623/job/98477675625) | SUCCESS |
| `CI` / `Public API Compatibility` | [run 33060465623 / job 98477675359](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060465623/job/98477675359) | SUCCESS |
| `Java Runtime Compatibility` / `Build Runtime Fixture (Java 21)` | [run 33060465628 / job 98477675592](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060465628/job/98477675592) | SUCCESS |
| `Java Runtime Compatibility` | [run 33060465628 / job 98477833776](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060465628/job/98477833776) | SUCCESS |

### 5.3 main最終required checks

mainへのpush eventで、merge commit `776e7729233cd14d46a2c0bc1ee6b0dd5f5a8fdf`を直接検証した。

| Workflow / job | Evidence | 結果 |
|---|---|---|
| `CI` / `Verify (ubuntu-24.04)` | [run 33060931866 / job 98479231134](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060931866/job/98479231134) | SUCCESS |
| `CI` / `Public API Compatibility` | [run 33060931866 / job 98479231205](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060931866/job/98479231205) | SUCCESS |
| `Java Runtime Compatibility` / `Build Runtime Fixture (Java 21)` | [run 33060931914 / job 98479230806](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060931914/job/98479230806) | SUCCESS |
| `Java Runtime Compatibility` | [run 33060931914 / job 98479371140](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/33060931914/job/98479371140) | SUCCESS |

通常CIではRoot Reactor、Tier 1 / Tier 2 Feature Template、NullAwayのpositive / negative / restoreを
すべて成功した。

### 5.4 Public API compatibility

| 検査 | main最終結果 |
|---|---|
| Authentication | Repository `GITHUB_TOKEN`、workflow `packages: read` |
| Architecture Contract baseline | `0.1.0-20260826.091429-1`、SHA-256 `947EE8CF0E109FE58D81E6008A56C06C8F4C035FF76BDF462F8F6BD9BB50DE45`、MATCH |
| ArchUnit Rules baseline | `0.1.0-20260826.091429-1`、SHA-256 `A51E26E7386D19E53C18BD63BC4E4F95EC1EAE471F39D519D6AE0CBC7C2DF3F2`、MATCH |
| Inventory | 5 public types、4 annotation elements、2 Rules methods、MATCH |
| japicmp | Architecture Contract / ArchUnit Rulesとも`access=public`、modifications `NONE`、exit `0` |
| fixtures | package-private変更は許容、return type破壊と未承認追加は期待failure PASS |

この結果により、KOIKI-ARCH-008のpackage-private実装修正がfreeze済みPublic APIとC1 baselineを
変更していないことをremoteで確認した。snapshot再公開とbaseline更新は行わない。

### 5.5 Java runtime compatibility

| 項目 | main最終結果 |
|---|---|
| Build | Java 21、class major `65` |
| Runtime JAR SHA-256 | `3AB5058FE02B1A81DB0975B1F5E6D3EE265A396E1D9FD1EE3077840BDE0DAB40` |
| Java 21 / 25 | 同一JARを再compileせず実行し、markerと実行前後hashが一致 |
| Negative guards | Java 25 build、hash改変、runtime major不一致がすべてEXPECTED FAILURE PASS |
| Workflow artifact | ID `9641663973`、digest `sha256:9e5623c35de996e938955f0c504cc796baff8da9b500815da8f015a1cefc01e1` |

### 5.6 Owner Review結果

| 項目 | 結果 |
|---|---|
| Decision | COMPLETE / ACCEPTED |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月27日 |
| Scope | KOIKI-ARCH-008修正、PR #22、main最終required checks、Public API / runtime / deferred scope |
| Evidence | PR head `a3b582a`、merge commit `776e772`、runs `33060931866` / `33060931914` |
| Rationale | `MYBATIS + SHARED`を機械的に拒否し、Public API、baselineおよび後続Phaseの保留範囲を変更していない |
| Revisit trigger | Phase 1bのrelease unit / baseline方針決定、Public API変更、修正版snapshotをRepository外Consumerへ配布する場合 |

Architecture Ownerが内容をreviewしてPR #22をmainへmergeし、main最終required checksの成功を確認した。
これによりPhase 1b着手前のMyBatis metadata guard補正を`COMPLETE / ACCEPTED`としてcloseする。

## 6. Deferred decisions

- `PersistenceTechnology.MYBATIS`をPublic APIへ維持するか、破壊的変更手続きで削除するか。
- `PersistenceModel.SEPARATED`の追加とMyBatis詳細規約の正式実装。
- MyBatis Starter、Mapper配置、`@Entity`排除等のDoD 31〜34の正式実装。

これらはPhase 1bのRuntime Foundationへ先行実装せず、グランドデザインとADR-039が定める
Phase 3末尾〜Phase 4の実装検証、およびPublic API baselineのrevisit triggerで扱う。

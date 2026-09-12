# Phase 2 P2-C2 C2-4 Public API baseline candidate verification

## 1. Status and boundary

- 検証日: 2026年9月11日
- 作業パッケージ: `P2-C2 / C2-4`
- baseline commit: `0d869cd0fe5b4659391310d1792e1c296ffea3f5`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- 状態: `COMPLETE / ARCHITECTURE OWNER APPROVED — C2-5 PUBLISH REVIEW READY`
- Ownership: Tooling-owned inventory / compatibility fixture / Evidence
- production artifact / Framework Public API change: 0

C2-4では、C2-2で承認済みのformal release unit 11 JARをpackage済みartifactから走査し、Phase 2 Public APIの
初回baseline candidateを作成した。既存Phase 1aのpublished baseline、検証scriptおよびrequired jobは変更しない。
比較対象となる過去公開baselineがないPhase 2 artifactについて、現在版同士の比較を後方互換性のEvidenceとは扱わない。

## 2. Assets and ownership

| Asset | Ownership / role |
|---|---|
| `build-support/api-compatibility/p2-c2/pom.xml` | 11 JARを完全なruntime classpathで読み込む非配布Tooling POM |
| `FullPublicApiInventory.java` | package済みJARから完全署名を正規化するsource-file Java tool |
| `fixture/baseline` / `compatible` / `nullness` | internal追加許容とnullness変更検出のsynthetic fixture |
| `p2-c2-public-api.txt` | Architecture Owner review対象のbaseline candidate |
| `verify-p2-c2-public-api.ps1` | stage、inventory照合、positive / negative fixture、cleanupのHarness |

いずれも`build-support`配下の非配布Toolingであり、Root Reactor、BOM、formal release unit、Framework Public API、
Reference、Customer成果物または`koiki-testing`へ含めない。

## 3. Formal JAR and Public type inventory

HarnessはGUID付き空Maven repositoryへformal release unit 14 projectsをstageし、C2-2 manifestの11 JARを直接検査した。

| Artifact | Public Java types | Baseline position |
|---|---:|---|
| `koiki-architecture-contract` | 4 | Phase 1a公開済み |
| `koiki-archunit-rules` | 1 | Phase 1a公開済み |
| `koiki-starter-audit` | 6 | Phase 2 candidate |
| `koiki-starter-identity` | 10 | Phase 2 candidate |
| `koiki-starter-session-jdbc` | 3 | Phase 2 candidate |
| `koiki-starter-api` | 0 | Public型なし |
| `koiki-starter-data` | 0 | Public型なし |
| `koiki-starter-data-jpa` | 0 | Public型なし |
| `koiki-starter-observability` | 0 | Public型なし |
| `koiki-starter-security` | 0 | Public型なし |
| `koiki-testing` | 0 | Public型なし |
| **formal release unit合計** | **24** | **Phase 1a公開済み5＋Phase 2 candidate 19** |

Public型0件の6 JARもartifact sectionとしてbaseline candidateへ固定した。formal release unit全体の24型は、Phase 1aで
公開済みのArchitecture Contract / ArchUnit Rules 5型と、Phase 2 candidateのAudit / Identity / Session 19型から成る。
24型の完全修飾名は、既存Phase 1 aggregate inventoryおよび各moduleの`public-api.txt`と完全一致する。この照合結果は
承認済みtype inventoryとの型集合の継続一致を示すものであり、過去versionとのsignature互換性を示すものではない。
`.internal.` packageのpublic classは対象外であり、5つのPublic API packageがすべて`@NullMarked`であることを確認した。

## 4. Normalized signature coverage

baseline candidateには次を決定的順序で記録する。

- artifact、packageとpackage nullness
- class / interface / enum / annotationの種別と修飾子
- superclass、interfaceおよびgeneric型
- public constructor、field、method、引数、戻り値、throws
- enum値
- annotation retention、target、documented属性およびmethod default値
- JSpecify `@Nullable`を含む型使用位置のnullness

したがって、型名だけのinventoryでは見落とすmethod signature、generic、annotation contractおよびnullnessの変化も
candidateとの差分として検出する。

## 5. Compatibility fixtures

| Scenario | Expected / observed result |
|---|---|
| package-private implementation change | inventory一致、japicmp変更なし、success |
| `.internal.` public type追加 | full inventory一致、success |
| Public APIのnullness-only変更 | full inventory不一致、期待どおり検出 |
| public method戻り値変更 | japicmp `METHOD_RETURN_TYPE_CHANGED`、期待failure |
| 未承認public method追加 | inventory不一致かつjapicmp `METHOD_ADDED_TO_PUBLIC_CLASS`、期待failure |

既存Phase 1a fixture verifierをそのまま呼び出してpositive / negative挙動を回帰し、C2-4固有fixtureでinternal除外と
nullness検出を補完した。

## 6. Verification command and result

次を実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-public-api.ps1
```

最終結果:

```text
Phase 2 P2-C2 C2-4 Public API verification succeeded (11 JARs / 24 public types / full signatures / nullness and negative fixtures).
```

Harness終了後、GUID付き隔離Maven repository、classpath、fixture JARおよび一時inventoryを削除した。remote publish、
artifact hash確定、workflow / required check変更は実施していない。

## 7. Architecture Owner review points

1. C2-2 manifestの11 JARをbaseline単位とし、Phase 1a公開済み5型、Phase 2 candidate 19型およびPublic型0件の
   6 JARから成るformal release unit全体24型の内訳を正式inventory candidateとしてよいか。
2. 型、継承、member、generic、例外、enum、annotation metadata / defaultおよびJSpecify nullnessを含む正規化形式と、
   `.internal.` package除外をC2-4のPublic API境界としてよいか。
3. 24型の完全修飾名が既存の承認済みaggregate / module inventoryと完全一致する結果を、過去versionとの
   signature互換性証明ではなく、型集合の継続一致確認としてよいか。
4. internal追加の許容、nullness-only変更、戻り値破壊および未承認public追加のfixture結果を差分検出Evidenceとしてよいか。
5. Phase 2 artifactについて過去公開baselineとの互換性を主張せず、既存Phase 1a published baseline / required jobを維持し、
   remote publish、hash確定およびCI変更をC2-5の個別Owner reviewへ残す境界でよいか。

推奨結論は上記5点を承認し、C2-4を`COMPLETE / ARCHITECTURE OWNER APPROVED`として、C2-5 baseline publish /
remote operationの実施可否reviewへ進むことである。

2026年9月11日、Architecture Ownerは上記5点を確認した。review時の指摘に基づき、formal release unit全体24型を
Phase 1a公開済み5型とPhase 2 candidate 19型に分け、Public型0件の6 JARを正式artifact名で明記した。また、既存inventoryとの
照合結果を過去versionとのsignature互換性証明ではなく、型集合の継続一致確認と明確化した。反映後の5点すべてを承認し、
C2-4を`COMPLETE / ARCHITECTURE OWNER APPROVED`、次をC2-5 baseline publish / remote operationの実施可否reviewとする。

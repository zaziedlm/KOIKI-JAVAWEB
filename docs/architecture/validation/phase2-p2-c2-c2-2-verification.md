# Phase 2 P2-C2 C2-2 formal package verification

## 1. Status and boundary

- 検証日: 2026年9月11日
- 作業パッケージ: `P2-C2 / C2-2`
- baseline commit: `4d9230ff19105ddf4f6a5a2e213ef0cee349a90b`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- 状態: `COMPLETE / ARCHITECTURE OWNER APPROVED — C2-3 READY`
- Ownership: Tooling Evidence
- production artifact / Public API change: 0

Architecture Owner承認済みC2-C1〜C7のうち、C2-2ではformal Framework release unitのmanifest固定と
空の隔離Maven repositoryへのstageだけを実装・検証した。Root外Consumer、Public API baseline、snapshot publish、
OpenRewrite prototype、CIおよびremote stateは変更していない。

## 2. Added verification assets

| Asset | Responsibility | Distribution |
|---|---|---|
| `p2-c2-formal-release-unit.txt` | 14 reactor projects、packaging、artifact ID、module pathの正本 | Tooling-only |
| `verify-p2-c2-package-static.ps1` | Root / BOM / staged repositoryをmanifestと照合 | Tooling-only |

両assetは`build-support/security-foundation-verification`が所有し、Root Reactor、BOM、formal release unit、
`koiki-testing`またはsnapshot publishへ含めない。

## 3. Formal release manifest

| Packaging | Artifact |
|---|---|
| POM | `koiki-dependencies-bom` |
| POM | `koiki-parent` |
| JAR | `koiki-architecture-contract` |
| JAR | `koiki-archunit-rules` |
| JAR | `koiki-starter-api` |
| JAR | `koiki-starter-data` |
| JAR | `koiki-starter-data-jpa` |
| JAR | `koiki-starter-observability` |
| JAR | `koiki-starter-audit` |
| JAR | `koiki-starter-security` |
| JAR | `koiki-starter-identity` |
| JAR | `koiki-starter-session-jdbc` |
| JAR | `koiki-testing` |
| POM | `koiki-javaweb-fw-reactor` |

14 projectsの内訳はPOM 3件とJAR 11件である。Root Reactorはこれらに`koiki-reference-app`を加えた15 projectsであり、
Referenceは引き続きBOMおよびformal Framework repositoryの対象外である。

## 4. Assertions

Harnessは次を検査した。

1. manifestが14件、JARが11件、artifact IDが一意である。
2. Root POMのmoduleがformal 13 modulesとReference 1 moduleに完全一致する。
3. BOMの`org.koikifw`管理対象がmanifestの11 JARに完全一致する。
4. `!koiki-reference-app`でformal release unitだけを空の隔離repositoryへstageする。
5. `org/koikifw`直下のstaged coordinatesがmanifest 14件に完全一致する。
6. POM projectはPOMだけ、JAR projectはPOMとJARをinstallする。
7. JARへReference package、build-support package、Customer migration、Java sourceまたはtemplateが混入しない。
8. `koiki-reference-app`、Security Consumerおよび`koiki-migration-recipes`がstaged repositoryへ存在しない。
9. GUID付き一時repositoryを成功・失敗のどちらでも安全確認後にcleanupする。

C2-2はsignature inventoryを生成しない。11 JARのPublic API signatureとnegative compatibility fixtureはC2-4が所有する。

## 5. Verification result

次を実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-package-static.ps1
```

| Check | Result |
|---|---|
| manifest parser | 14 projects / 11 JARs |
| Root module inventory | formal 13 modules + Reference 1 module |
| BOM inventory | 11 / 11 JARs |
| formal Maven stage | 14 / 14 projects `BUILD SUCCESS` |
| staged coordinate inventory | 14 / 14 match |
| Reference / Tooling / Customer content exclusion | success |
| temporary repository cleanup | residual 0 |
| PowerShell parser / `git diff --check` | success |

最終結果:

```text
Phase 2 P2-C2 C2-2 package verification succeeded (14 projects / 11 JARs / Reference and Tooling excluded).
```

Maven実行時のCDS warningは既存環境由来であり、compile、package、installまたはinventory検査へ影響しなかった。

## 6. C2-3 handoff

C2-3では、空の隔離repositoryへ本manifestのformal unitをstageした後、
`build-support/security-foundation-consumer`候補をRoot Reactor外の別Maven invocationでbuild / test / packageする。
C2-2 manifest assertionをcopyせず同じmanifestを再利用し、Phase 1b ConsumerとReferenceを変更しない。

ConsumerはJava 21でbuildし、package済み同一JARをJava 21 / 25とPostgreSQLで実行する。Consumer固有route、
synthetic credential、Customer migrationおよびfailure switchは非配布fixtureに限定する。

## 7. Architecture Owner review points

1. formal release unitをPOM 3件 / JAR 11件の14 projectsとしてmanifestへ固定してよいか。
2. Root 15 projectsからReferenceだけを除外し、BOM管理対象を11 JAR、staged repositoryをPOM 3件 / JAR 11件の
   14 coordinatesへ限定する検査がOwnership境界を満たすか。
3. Reference / Tooling / Customer migration / source template非混入とtemporary repository cleanupがC2-2に十分か。
4. Consumer、Public API baseline、publishおよびOpenRewriteを先行せず、次をC2-3 Root外Consumerとしてよいか。

2026年9月11日、Architecture Ownerは上記4点を確認し、すべて承認した。
これによりC2-2を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はC2-3 Root Reactor外Consumerへ進む。

# Phase 2 P2-C1 C1-4 closeout

## 1. Status and boundary

- 検証日: 2026年9月11日
- 作業パッケージ: `P2-C1 / C1-4`
- 検証対象commit: `f7a3d906af70f13619c30022f235d2c37618fe93`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- 状態: `COMPLETE / ARCHITECTURE OWNER APPROVED — P2-C2 CONTRACT REVIEW READY`
- Ownership: Tooling / Architecture Evidence
- C1-4でのproduction実装変更: なし

C1-4は、承認済みC1-C1〜C7、C1-2 static inventoryおよびC1-3 PostgreSQL実測を、同じcleanなHEAD上で
focused、Root Reactor、Null Safety、Public API、Gate B回帰、sensitive-outputおよびcleanupの観点からcloseoutする。
Architecture Owner承認、P2-C2のpackage / Consumer契約、CI変更、remote実行およびruleset変更は本実施検証に含めない。

## 2. Verification commands

次を同一commitに対して実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-migration-static.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-postgresql.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -ExpectedHead f7a3d906af70f13619c30022f235d2c37618fe93
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -InspectOnly -ExpectedHead f7a3d906af70f13619c30022f235d2c37618fe93
```

## 3. P2-C1 focused result

| Layer | Result |
|---|---|
| C1 static inventory | success: package済みFramework migration 3件 / 11 table |
| focused Audit reactor | 3 / 3 modules `BUILD SUCCESS` |
| Session dependency closure | 7 / 7 modules `BUILD SUCCESS` |
| formal Framework release unit | 14 / 14 projects `BUILD SUCCESS` |
| package済みReference | `BUILD SUCCESS`、Reference-owned migration 0件 |
| PostgreSQL | 17.11 |
| clean install | Audit / Identity / Session JDBC / Referenceの4 / 4 profiles success |
| restart no-op | 4 / 4 profiles success |
| Phase 1b supported upgrade | success |
| failure contracts | 4 / 4 success |

static inventoryは、owner artifactごとのmigration配置、version `2026090300`、`2026090301`、`2026090701`の
一意性と依存順、Audit Entity列契約、fixture固有DDLおよび未使用indexの非混入を確認した。

PostgreSQL実測は、各profileのversion / checksum / history分離、table / column / constraint / index、JPA `validate`、
再起動no-op、Spring Session initializer無効を確認した。詳細なhistory表記、`baselineOnMigrate`実測および物理inventory集計値は
`phase2-p2-c1-c1-3-verification.md`を正本とし、C1-4で変更していない。

## 4. Gate B regression and stability

Gate B closeoutを、失敗sliceだけを再試行せず3ラウンド連続で実行した。

| Round | B1 Audit | B2 Identity | B3 core | B3 two-process | B3 non-web cleanup | B4 Reference |
|---:|---|---|---|---|---|---|
| 1 | success, 31 / 31 | success, 56 / 56 | success, 72 / 72 | success | success | success |
| 2 | success, 31 / 31 | success, 56 / 56 | success, 72 / 72 | success | success | success |
| 3 | success, 31 / 31 | success, 56 / 56 | success, 72 / 72 | success | success | success |

最終結果は次のとおりである。

```text
Phase 2 P2-B4 closeout succeeded: three consecutive P2-B1-B4 aggregates, Root Reactor, Null Safety, inventory, sensitive-output and cleanup checks. Gate B review may begin after Architecture Owner approval.
```

C1-4の確定実行前に、C1-2で追加したAudit production migrationとの所有権整合を既存Gate B fixtureへ反映した。
two-process fixtureによるAudit tableの重複作成を除去しfixture固有constraintだけを追加する構成へ変更し、
Reference journeyの手動Audit DDLを除去した。またIdentity Public API inventory検査を改行形式に依存しない行単位検査へ補正した。
これらはcommit `f7a3d906`に含まれるGate B fixture / Harness互換補正であり、production migrationの不良ではない。

## 5. Root, Null Safety and inventory

- Root Reactorは15 / 15 projects、104 tests、failure / error / skip 0で成功した。
- NullAway positive検証は成功した。
- NullAway negative fixtureは期待したcompile failureを検出し、その後のrestoreも成功した。
- Audit Public API 6型、Identity Public API 10型、Session cleanup Public API 3型の承認済みinventoryを維持した。
- artifact、dependency、migration、source / route、sensitive-outputの各inventory検査に成功した。
- Framework正式release unit 14 projectsと、Referenceを含むRoot Reactor 15 projectsのownership境界を維持した。

## 6. Repository and cleanup

aggregate完了後の独立したinspection-only実行で次を確認した。

- `HEAD`は検証対象commitから不変である。
- worktreeはcleanである。
- Harness所有container、Java child process、一時directoryおよび非配布fixture targetの残留はない。
- runtime credentialおよびsensitive command lineをEvidenceへ出力していない。
- workflow、remote state、ruleset、required checkおよびsnapshot publishを変更していない。

## 7. Architecture Owner review points

1. C1-2 static inventory、C1-3 PostgreSQL実測およびGate B 3連続回帰が同じcleanなcommitで成功したことを根拠に、P2-C1をcloseしてよいか。
2. package済みFramework migration 3件 / 11 table、4 clean profilesおよびPhase 1b supported upgradeが、C1-C1〜C7の契約を満たすか。
3. Root Reactor、Null Safety、Public API inventoryおよびGate B回帰が、Audit migration追加後の非migration領域の互換性を十分に示すか。
4. fixture / Harness互換補正が非配布Tooling内に留まり、Framework / Reference / Customer ownershipを混在させていないか。
5. cleanup、sensitive-outputおよびclean HEAD検査を含む本Evidenceを承認し、P2-C2 package / Consumer contract reviewへ進めてよいか。

2026年9月11日、Architecture Ownerは上記5点を確認し、相違がないことからすべて承認した。
これによりP2-C1を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、次はP2-C2 package / Consumer contract reviewへ進む。
P2-C2の契約承認前にproduction artifact、Public API、Consumer fixture、OpenRewrite recipeまたはCIを追加しない。

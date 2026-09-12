# Phase 2 P2-C2 C2-7 closeout

## 1. Status and boundary

- 検証日: 2026年9月12日
- 作業パッケージ: `P2-C2 / C2-7`
- 検証対象commit: `5704cdd5dbe407e33c8eba6b8367fc6bf98f544e`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- 状態: `COMPLETE / LOCAL VERIFIED / ARCHITECTURE OWNER REVIEW PENDING`
- Ownership: Tooling / Architecture Evidence
- C2-7でのproduction artifact / Framework Public API変更: なし

C2-7は、承認済みC2-2〜C2-6を同じclean HEADで再検証し、Root Reactor、Null Safety、Gate B、P2-C1、
sensitive-output、cleanupおよび最終release / Tooling inventoryの観点からP2-C2をcloseoutする。
workflow変更、protected environment作成、push、PR、main反映、workflow dispatch、remote snapshot publishおよび
required check変更は本local closeoutに含めない。

## 2. Verification commands

次を同一commitに対して順に実行した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-package-static.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-consumer.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-public-api.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-publish-dry-run.ps1
pwsh -NoProfile -File build-support/openrewrite-feasibility/verify-p2-c2-openrewrite.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-migration-static.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c1-postgresql.ps1
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -ExpectedHead 5704cdd5dbe407e33c8eba6b8367fc6bf98f544e
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-b4-closeout.ps1 `
  -InspectOnly -ExpectedHead 5704cdd5dbe407e33c8eba6b8367fc6bf98f544e
./mvnw.cmd --batch-mode --no-transfer-progress `
  -f build-support/null-safety/verification/pom.xml clean
```

ConsumerとC1 PostgreSQL Harnessの最初のDocker Engine確認はfilesystem sandbox内では利用不可となったため、その実行を
Evidenceに採用せず、承認されたlocal host accessで各Harnessを先頭から完全再実行した。確定実行は成功しており、Gate B
3ラウンドでは失敗sliceの再試行やtest抑止を行っていない。

## 3. P2-C2 focused result

| Slice | Local result |
|---|---|
| C2-2 package | success: formal 14 projects / distributed 11 JAR、Reference / Tooling非混入 |
| C2-3 Consumer | success: isolated package、Java 21 / 25、PostgreSQL、migration no-op / failure guard |
| C2-4 Public API | success: 11 JAR / 24 Public型、完全signature、nullness / negative fixture |
| C2-5 publish dry run | success: 13座標、24 payload hash、aggregate signature、11 same-source japicmp |
| C2-6 OpenRewrite | success: before / after、旧定義非変更、冪等性、変換後test、手動残件、非配布境界 |

C2-5で実行したのはGUID付き一時file repositoryへのlocal dry runだけである。GitHub Packagesへの接続、remote metadata取得、
environment作成またはworkflow dispatchは行っていない。remote snapshot publishは引き続き`NOT APPROVED / NO-GO`である。

## 4. P2-C1 regression

| Layer | Result |
|---|---|
| migration static | success: Framework migration 3件 / 11 table |
| focused Audit reactor | 3 / 3 modules success |
| Session dependency closure | 7 / 7 modules success |
| formal release unit | 14 / 14 projects success |
| PostgreSQL clean install | Audit / Identity / Session / Referenceの4 profile success |
| restart no-op | 4 / 4 profile success |
| Phase 1b supported upgrade | success |
| failure contracts | success |

package済みmigrationの配置、version / checksum / history分離、table / column / constraint / index、JPA `validate`、
Customer history分離およびSession initializer拒否を維持した。

## 5. Gate B regression and stability

既存`verify-p2-b4-closeout.ps1`を変更せず再利用し、6工程を3ラウンド連続実行した。

| Round | B1 Audit | B2 Identity | B3 core | B3 two-process | B3 non-web cleanup | B4 Reference |
|---:|---|---|---|---|---|---|
| 1 | 31 / 31 | 56 / 56 | 72 / 72 | success | success | success |
| 2 | 31 / 31 | 56 / 56 | 72 / 72 | success | success | success |
| 3 | 31 / 31 | 56 / 56 | 72 / 72 | success | success | success |

```text
Phase 2 P2-B4 closeout succeeded: three consecutive P2-B1-B4 aggregates, Root Reactor, Null Safety, inventory, sensitive-output and cleanup checks. Gate B review may begin after Architecture Owner approval.
```

## 6. Root, Null Safety and final inventory

- Root ReactorはReferenceを含む15 / 15 projects、104 tests、failure / error / skip 0で成功した。
- NullAway positive、expected negativeおよびsource restoreは成功した。
- formal Framework release unitは14 projects（POM 3 / JAR 11）を維持した。
- publish候補はRoot aggregatorを除く13 coordinates（POM 2 / JAR 11）を維持した。
- Public API aggregateは11 JAR / 24型を維持した。
- `koiki-reference-app`はRootだけに参加し、BOM / formal release unit / publish unitから分離した。
- Consumer、OpenRewrite prototype、API fixture、publish toolingおよびC1 fixtureはRoot / BOM / formal release unitへ含まれない。
- OpenRewrite prototypeは初期フレームワーク運用、Customer配布、runtimeおよびrequired CIへ含まれない。

## 7. Sensitive output and cleanup

各focused HarnessとGate B Harnessが、正式JAR、Consumer JAR、dependency tree、Public API / japicmp report、process logおよび
fixture成果物に対する既存sensitive-output検査を成功させた。OpenRewriteの固定source / expected / manual-actionsについても、
private key、Bearer credentialおよびGitHub token形式の値がないことを確認した。token、settings実値、runtime credential、
JARおよび一時reportをEvidenceへ保存していない。

Gate B完了後のinspection-only実行はclean HEAD、所有process / container / temporary directoryおよびGate B fixture targetの
非残留を確認した。追加のC1 / C2 inspectionでは、C1 / C2一時directory、file repository、C1 / C2 / Gate B container、
Consumer / migration / Public API / publish / OpenRewrite fixture targetの非残留を確認した。

Null Safety verifierは正負検証とrestore成功後にfixture `target`を残すため、最終inspectionでこれを検出し、上記Maven `clean`で
生成物だけを削除した。その後の再検査では対象fixture targetは0、C1 / C2 / Gate B containerは0、一時directoryは0である。
HEADは検証対象commitから不変で、Evidence編集開始前のworktreeはcleanだった。

## 8. Handoff and deferred state

C2-2〜C2-7のlocal EvidenceとOwner承認後、次はP2-C3 Developer Journey / DoD closeoutへ引き渡す。
C2-5 remote snapshot publishの`NO-GO`はP2-C2 local closeoutによって解除されない。protected environment、push、PR、main、
workflow dispatch、GitHub Packages、remote hash / signature / japicmp Evidenceおよびrequired checkは個別承認またはGate Cへ残す。

正式OpenRewrite recipe artifact、旧版Reference / Project Template fixture、Spring Boot recipe構成およびrelease CIはPhase 5へ残す。

## 9. Architecture Owner review points

1. C2-2〜C2-6のfocused 5工程とC1回帰が同じclean commitで成功したことを、P2-C2 closeout Evidenceとしてよいか。
2. Gate Bの6工程3ラウンド、Root 15 projects / 104 testsおよびNull Safety正負 / restore成功が非C2領域の回帰に十分か。
3. formal 14 projects / 11 JAR、publish候補13座標、Public API 24型、Reference分離およびTooling非配布の最終inventoryを承認するか。
4. sensitive-output検査と、process / container / temporary repository / directory / fixture targetの最終cleanupを承認するか。
5. C2-5 remote snapshot publishを`NOT APPROVED / NO-GO`のまま維持し、P2-C2 local closeoutと分離するか。
6. P2-C2を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてcloseし、P2-C3 Developer Journey / DoD closeoutへ進めてよいか。

推奨結論は上記6点を承認し、P2-C2 local work packageをcloseしてP2-C3へ引き渡すことである。remote操作の権限は
本承認に含めない。

2026年9月12日、Architecture Ownerは上記承認文案を確認し、6判断を提案どおり承認した。C2-2〜C2-6のfocused検証、
P2-C1回帰、Gate B 6工程の3ラウンド、Root Reactor、Null Safety、最終inventory、sensitive-output検査およびcleanupを
P2-C2 closeout Evidenceとして受け入れる。P2-C2を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてcloseし、次は
P2-C3 Developer Journey / DoD closeoutへ進む。

C2-5 remote snapshot publish、protected environment作成、push、PR、main反映、workflow dispatch、required check変更および
その他のremote操作は本承認に含めず、引き続き`NOT APPROVED / NO-GO`とする。

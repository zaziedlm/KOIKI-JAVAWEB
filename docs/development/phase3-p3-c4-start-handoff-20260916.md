# Phase 3 P3-C4開始引継ぎ

## 1. Handoff status

- 作成日: 2026年9月16日
- Architecture Owner: Shuichi Kataoka
- branch: `feature/phase3-reference-vertical-slice`
- approved baseline commit: `56ddfb4c3a4f5e2f7491c77a1b4391d76def3743`
  （P3-C3 MyBatis adoption-trigger deferral）
- Phase status: `P3-C0〜C2 COMPLETE / OWNER APPROVED — P3-C3 DEFERRED — P3-C4 READY`
- Primary ownership: Architecture documentation / closeout verification
- Production implementation: 原則0
- planned Evidence: `docs/architecture/validation/phase3-p3-c4-traceability-closeout.md`
- immediate next action: Phase 3のDoD / AC / ADR / Skill / inventoryを実装差分と照合する

P3-C4は新しい業務機能、Framework機能またはtest harnessを追加するCPではない。Phase 3開始baselineから
現在HEADまでの実装、承認記録および検証結果を横断し、DoD、Reference AC、Public API、migration、dependency、
artifact、property / profile、route、Toolingおよびdeferred itemが矛盾なく追跡できる状態にする。

文書上の不足はP3-C4で補正できる。production code、test、契約または検証証拠の不足を検出した場合は、
closeout文書で推測補完せず、所有CPと影響範囲を特定して停止する。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 本書
4. `KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`の§1〜17、§18〜29
5. `../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`のPhase 3 DoD 3-1〜3-11
6. `../reference/KOIKI-JavaWeb-FW_Reference_Application_Specification_v0.1.md`のAC-P3-01〜10
7. `../architecture/adr/README.md`
8. `../architecture/validation/phase3-p3-cp0-start-baseline.md`
9. `../architecture/validation/phase3-p3-a0-contract-review.md`から
   `../architecture/validation/phase3-p3-c3-mybatis-deferral.md`までのPhase 3 Evidence
10. `../reference/README.md`
11. `../reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md`
12. `phase2-developer-journey.md`
13. `../../build-support/api-compatibility/public-api.txt`
14. `../../build-support/reference-browser-verification/README.md`
15. `../../build-support/reference-api-verification/README.md`
16. `../../build-support/reference-e2e-verification/README.md`

## 3. Work positioning

```text
Phase / status: Phase 3 / P3-C4 READY
Ownership: Architecture documentation / closeout verification
Target Maven module: none by default
Business modules under review: master / expense
Verification: baseline diff + trace matrix + local aggregate + focused Tooling
Deferred: Remote Gate、DoD 3-11実CI PASS、Gate C、Phase 4、MyBatis adoption
```

P3-C4はRemote GateまたはGate Cを兼ねない。local Evidenceとinventoryを閉じた後も、workflow変更、
required check化、remote実行、push / PR / merge、snapshot publishおよびDoD 3-11の最終充足には個別Owner承認が必要である。

## 4. Closeout deliverables

P3-C4では次を1つのtraceability Evidenceへまとめる。

1. DoD 3-1〜3-11と実装・test・Evidenceの対応表。
2. AC-P3-01〜10と成功 / 拒否 / rollback / scope / transport Evidenceの対応表。
3. Phase 3で追加・fitting・延期したADRと実装事実の対応表。
4. `AGENTS.md`とKOIKI固有Skillが現在のPhase、Ownershipおよびdeferred境界と一致すること。
5. Java Public API、Maven artifact / publish unit、dependency、resource contractの開始baseline差分。
6. Framework / Reference migration、table Ownership、FKおよびschema履歴のinventory。
7. MVC / REST route、Security profile、property、cacheおよびHTMX assetのconsumer-visible contract inventory。
8. Root Reactor外Toolingと正式成果物の非混入確認。
9. 未完了、非blocking observation、後続Phase、optional Gate、Remote Gateのdeferred inventory。
10. Engineer-facing Journeyの入口、手動操作、自動検証、credential / cleanup境界の整合。
11. clean local aggregateとfocused Toolingの最終結果、およびGate Cへ渡す未完了条件。

## 5. Preliminary implementation inventory

2026年9月16日、Phase 3開始baseline `c88b335efdd556613c9ef7f4c5267214fdb8254b`と
HEAD `56ddfb4c3a4f5e2f7491c77a1b4391d76def3743`をread-onlyで比較した。

| Area | Preliminary result | P3-C4 close action |
|---|---|---|
| Git | worktree clean、branchはremoteより4 commit ahead | close時のclean HEADとcommit SHAを再確認 |
| Java Public API | `build-support/api-compatibility/public-api.txt`差分0 | japicmp / inventoryで再確認 |
| Formal artifact | `koiki-starter-web-mvc`を1 JAR追加 | formal 15 projects / 12 JAR、publish 14座標を照合 |
| Root Reactor | 1 module追加後16 projects / 13 JAR | `clean verify` reactor summaryと一致確認 |
| Web dependency | HTMX `2.0.10`をBOM管理し、Web MVC StarterがMVC / Thymeleaf / Validation / HTMX / JSpecifyを集約 | approved P3-B0 / B2契約とeffective dependencyを照合 |
| Reference dependency | Web MVC Starter、API Starter、Cache / Caffeine、test-scope `koiki-testing` | 未承認dependencyがないことを確認 |
| Framework migration | Phase 2の3 migration / 11 tableから変更なし | checksum / count / ownershipを確認 |
| Reference migration | `kkref` V1 master、V2 expense、V3 approver scope | 6 table、module内FKのみ、production seed 0を確認 |
| Reference profile | 通常Sessionを維持し、P3-C1限定`api-bearer` profileを追加 | matcher、CSRF、認証fallbackなしをEvidenceへtrace |
| Tooling | browser、API、critical E2Eの3つをRoot Reactor外へ追加 | test scope、非配布、cleanup、secret非保存を確認 |
| Engineer Journey | Reference Local Run GuideはP3-B2対象表示のまま。Reference READMEと3 Tooling READMEに個別導線あり | Phase 3完成形の入口と手動 / 自動境界を最小差分で接続 |
| MyBatis | P3-C3は明示的DEFERRED | Rule 8拒否、`SEPARATED` / fixture / test dependency 0を確認 |
| Workflow / remote | Phase 3開始baselineから変更0 | Remote Gateまで変更しない |

この表は開始時inventoryであり、P3-C4 close承認ではない。実コマンドと各Evidenceを照合し、差異は
`phase3-p3-c4-traceability-closeout.md`へ記録する。

## 6. Required trace matrices

### 6.1 Phase 3 DoD

| DoD | Primary Evidence candidate | Close status expectation |
|---|---|---|
| 3-1 | P3-A1 / A2 | master Tier 1、expense Tier 2の動作と選定理由 |
| 3-2 | P3-A4 / Gate A | 未処理申請による部門廃止rollback |
| 3-3 | P3-A4 / Gate A | masterがexpenseを知らない依存方向 |
| 3-4 | P3-B2 / ArchUnit | Entity / Domain ModelのTemplate流出検出 |
| 3-5 | P3-B3 / Gate B | HTMX検索・paging・部分更新とCSRF |
| 3-6 | P3-B4 / Gate B | 2 Sessionの先行更新 / 後発競合画面 |
| 3-7 | P3-B1 / Gate B | master JPA射影、expense JdbcClient read model |
| 3-8 | P3-A3 / C1 / C2 | 申請・承認・却下のBusiness Audit |
| 3-9 | P3-B4 / Gate B | 経費科目cacheとTTL後の再読込 |
| 3-10 | P3-C0 / C1 | `/api/v1` path versioning |
| 3-11 | P3-C2 + Remote Gate / Gate C | local CI候補は承認済み。実CI PASSはPENDING |

### 6.2 Reference acceptance criteria

| AC | Primary Evidence candidate |
|---|---|
| AC-P3-01 | P3-A1、P3-B2 / B3 |
| AC-P3-02〜03 | P3-A2、P3-B2、P3-C1 |
| AC-P3-04〜05 | P3-A3、P3-B2 / B4、P3-C1 |
| AC-P3-06〜07 | P3-A4、Gate A |
| AC-P3-08 | P3-B4、Gate B |
| AC-P3-09 | P3-A3、P3-B1、Gate B |
| AC-P3-10 | P3-C1、P3-C2 |

P3-C4では各行を単なる文書リンクで終わらせず、対象test、観測したDB / Audit / log、Owner判断、
既知の制約および最終statusまで記録する。

## 7. Deferred inventory to preserve

少なくとも次を「欠落」ではなく、理由・再開条件・Ownerを持つdeferred itemとして追跡する。

- DoD 3-11の実workflow PASS、workflow / required check / remote操作、snapshot publish。
- P3-C3 MyBatis規約fixture、`PersistenceModel.SEPARATED`、Rule 25〜27 / 30〜37。
- Phase 4 `accounting`、notification、SPA、Spring Modulith Level 2、非同期event。
- Project Template、正式Upgrade / Migration Guide、正式OpenRewrite recipe。
- Authorization Server、SAML、Redis、WebFlux、Oracle optional Gate、AWS固有Adapter。
- P3-B2で記録した申請者email表示とIdentity / 業務profile属性Ownershipの非blocking observation。
- Gate Bで限定確認したaccessibility範囲を越える全画面・全Role・全支援技術certification。
- P3-C0で継続したReact / Next.js、SSO、Access / Refresh TokenのPhase 4判断。

deferred itemをPhase 3完了と誤認させず、同時にGate Cを不要にblockしない分類を行う。

## 8. Engineer-facing Journey proposal

P3-C4開始時点では、Referenceの手動起動・MVC操作は
`docs/reference/KOIKI-JavaWeb-FW_Reference_Application_Local_Run_Guide_v0.1.md`、自動確認は3つの
Tooling READMEへ分散している。Local Run Guideの対象表示はP3-B2であり、Phase 3完成形の通常Session MVC、
P3-C1限定`api-bearer` profile、focused APIおよびcritical E2Eの使い分けを入口から追いにくい。

P3-C4では次を推奨案として評価する。

1. `docs/reference/README.md`をPhase 3 Referenceのengineer-facing入口とする。
2. 既存Local Run Guideを通常Session MVCのpackage済み手動journeyとしてPhase 3現状へ更新する。
3. Bearer API、browser focused、critical E2Eは各Tooling READMEへリンクし、commandとfixture説明を複製しない。
4. Phase 2 Securityの依存選択は`phase2-developer-journey.md`を正本として再利用する。
5. 新しい`phase3-developer-journey.md`は、上記構成では表現できない独立audience / flowが確認された場合だけ作成する。
6. credential、token、Cookie、key、demo seed、cleanupの境界を全入口で一致させる。

この提案はP3-C4での文書補正候補であり、profile、route、fixtureまたはproduction設定を変更する根拠にしない。

## 9. Proposed execution sequence

1. Phase 3開始baselineからHEADまでのGit差分をartifact / code / resource / configuration / documentationへ分類する。
2. DoD 3-1〜3-11、AC-P3-01〜10、ADR、Skillのtrace matrixを作成する。
3. Public API、artifact / publish unit、dependency tree、migration / table、property / profile、routeを棚卸しする。
4. ToolingがRoot Reactor / formal artifact / Reference JARへ混入していないことを確認する。
5. P3-C3延期を含むdeferred / nonblocking / pending inventoryを確定する。
6. §8のEngineer-facing Journey導線を評価し、必要最小限の文書・Skill不一致を補正する。
7. productionまたはtestの不足なら所有CPへ戻す判断を記録して停止する。
8. Root Reactor `clean verify`とPublic API compatibilityを実行する。
9. package済みJARを作成し、API focused Toolingとcritical E2E Toolingを各1回実行する。
10. browser focused journeyは既存P3-B3 / B4 Evidenceとの重複と起動条件を確認し、Gate C実演へ渡す範囲を分離する。
11. cleanup、残存process / container、secret / SQL / stack trace非出力を確認する。
12. P3-C4 EvidenceをOwner reviewし、承認後にcommit pointを閉じる。
13. Remote GateまたはGate Cは個別Owner判断なしに開始しない。

## 10. Required verification at close

- DoD 3-1〜3-10とAC-P3-01〜10が、実装・自動test・Owner Evidenceへ双方向にtraceできる。
- DoD 3-11を`PENDING CI EVIDENCE`として正確にGate Cへ送る。
- ADR-027 / 038 / 039 / 049を含むPhase 3 fitting / deferralと実装が一致する。
- Project Overview / Business Feature Skill、AGENTS.md、実行計画の現在地とdeferred境界が一致する。
- Java Public API差分0、Web MVC StarterのJava Public API 0型、resource contractだけの追加を確認する。
- formal release / publish / Root Reactor inventoryがP3-B0承認値と一致する。
- Framework migration不変、Reference V1〜V3 / 6 table、module内FKのみである。
- 未承認dependency、property、profile、route、table、migration、Maven moduleまたは正式artifactがない。
- browser / API / E2E ToolingがRoot Reactor外・test scope・非配布である。
- Reference README、Local Run Guide、Phase 2 Developer Journey、Tooling READMEの入口と責務が重複せず接続する。
- MyBatisがRule 8で拒否され、`SEPARATED`、fixture、test dependencyが存在しない。
- local aggregateとfocused ToolingがPASSし、resource残存とsecret露出がない。
- Evidence作成後のworktreeがclose review可能な範囲に限定されている。

## 11. Owner close review points

1. DoD 3-1〜3-10、AC-P3-01〜10、ADR / Skill traceがPhase 3実装を過不足なく表しているか。
2. Public API、artifact / publish unit、dependency、migration、property / profile、route inventoryに
   未承認差分がないか。
3. P3-C3 MyBatis延期、非blocking observationおよびPhase 4項目の分類と再開条件が妥当か。
4. §8のEngineer-facing Journey構成が、重複せずPhase 3の手動 / 自動確認へ到達できるか。
5. local aggregate / focused Tooling結果をP3-C4 close Evidenceとして受け入れられるか。
6. DoD 3-11実CI PASSだけをRemote Gate / Gate Cへ継続し、P3-C4を閉じてよいか。

## 12. Stop conditions

- traceability不足を補うためproduction code、test、Public APIまたはdependencyをその場で追加する。
- EvidenceがないDoD / ACを文書上の推測だけでPASSにする。
- P3-C3を再開せずMyBatis、`SEPARATED`または関連Ruleを追加する。
- nonblocking / deferred itemを削除して「完了」に見せる、またはGate Cを不要にblockする。
- ToolingをRoot Reactor、Framework artifact、Reference JAR、`koiki-testing`またはProject Templateへ含める。
- secret、token、Cookie、password、private key、PII、SQLまたはstack traceをEvidenceへ保存する。
- Ownerの個別承認なしにworkflow、required check、remote push / PR / merge、rulesetまたはsnapshot publishを変更する。
- P3-C4 close前にRemote Gate、Gate CまたはPhase 4へ進む。

## 13. Start readiness

2026年9月16日に次を確認した。

- HEAD `56ddfb4c3a4f5e2f7491c77a1b4391d76def3743`
- branch `feature/phase3-reference-vertical-slice`、worktree clean、remoteより4 commit ahead
- P3-C0〜C2 `COMPLETE / OWNER APPROVED`
- P3-C3 `DEFERRED — MyBatis adoption trigger required`
- Phase 3開始baselineからのJava Public API inventory差分0
- Web MVC Starter、Root Reactor、dependency、Reference migration、profileおよび非配布Toolingの差分をread-onlyで確認
- workflow差分0、remote操作0

次回は§9手順1から開始する。P3-C4準備ではMaven、Docker、Browserを実行しておらず、既存のclose Evidenceを
再利用しつつ、最終検証結果はP3-C4 Evidenceへ新たに記録する。

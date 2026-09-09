# Phase 2 Gate B開始引継ぎ

## 1. 引継ぎステータス

- 引継ぎ日: 2026年9月9日
- Architecture Owner: Shuichi Kataoka
- branch: `feature/phase2-security-local-identity-session-audit`
- 引継ぎcommit: `4931f83`（P2-B4承認済みcloseout記録）
- Phase status: `GB-2 COMPLETE / ARCHITECTURE OWNER APPROVED — GB-3 READY`
- 所有者: Tooling / Architecture Evidence / CI Policy
- 主対象: DoD 2-5〜2-8および2-10、package済みjourney、Public API inventory / `japicmp`方針、CI候補review
- 先送り: P2-C1 migration集約、業務application、申請承認workflow、snapshot publish

本handoffは、Milestone BのP2-B1〜B4で成立した証拠をGate Bとして集約し、remote CIおよびrequired check化を
個別判断するための開始境界である。新しいFramework / Reference機能を追加する作業ではない。

## 2. 必須確認順序

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`のMilestone B、§7〜§9
4. `../architecture/validation/phase2-p2-b1-contract-review.md`と`../architecture/validation/phase2-p2-b1-t4-verification.md`
5. `../architecture/validation/phase2-p2-b2-contract-review.md`と`../architecture/validation/phase2-p2-b2-b2-5-verification.md`
6. `../architecture/validation/phase2-p2-b3-contract-review.md`
7. `../architecture/validation/phase2-p2-b3-b3-6-closeout.md`
8. `../architecture/validation/phase2-p2-b4-contract-review.md`
9. `../architecture/validation/phase2-p2-b4-b4-5-closeout.md`
10. `../../build-support/security-foundation-verification/README.md`
11. `../../.github/workflows/README.md`と`../../.github/workflows/ci.yml`
12. `../../build-support/api-compatibility/README.md`

## 3. 承認済み開始baseline

1. P2-B1 Audit、P2-B2 Identity、P2-B3 Session、P2-B4 Reference `identity`はArchitecture Owner承認済みである。
2. `verify-p2-b4-closeout.ps1`は、同一clean HEADでB1〜B4の6工程を3回連続実行し、Root ReactorとNull Safetyまで確認する。
3. 検証対象commit `1ec1fae`で3連続closeoutに成功し、承認記録commitは`4931f83`である。
4. Framework正式release unitは14 project、Referenceを含むRoot Reactorは15 projectである。
5. `koiki-reference-app`はRoot Reactorへ参加するが、Framework BOM、正式release stagingおよびPublic API互換対象へ含めない。
6. Phase 2で承認したPublic Java APIは各Harnessのinventory完全一致検査で保護されている。
7. 既存のpublished `japicmp` baselineはPhase 1a C1のArchitecture Contract / ArchUnit Rulesだけを対象とする。
8. workflow追加、remote実行、required check変更、push、PR、mergeは個別Owner承認後だけ行う。
9. 2026年9月10日、Architecture OwnerはGate B契約GB-C1〜C7を承認した。GB-2では途中失敗時の最終残留検査と
   一次失敗を保持するerror handlingを受入条件とする。
10. 同日、Architecture OwnerはGB-2 CI候補実装のreview points 1〜5を承認した。次はworkflow候補を含むclean HEADで
    GB-3 local final verificationを実行する。remote操作とrequired check変更は引き続き未承認である。

## 4. Gate B完了条件

Phase 2実行計画のGate B完了条件を次の検査可能な境界へ展開する。

| 計画上の条件 | Gate Bで確認する証拠 |
|---|---|
| DoD 2-5 | package済み同一JARの2 process間Session共有と一方停止後の継続 |
| DoD 2-6 | 認証失敗閾値、account lock、独立Security Audit、業務rollbackからの分離 |
| DoD 2-7 | Identity mutationとBusiness Auditの同一transaction rollback |
| DoD 2-8 | 期限切れSession cleanup、single execution、競合、crash recovery |
| DoD 2-10 | Reference `identity`からFramework contract経由でFramework所有tableを操作 |
| packaged journey | 実PostgreSQL、package済みprocess、HTTP / DB / CookieによるAC-P2-01 / 02 |
| Public API | 承認済みinventory完全一致とpublished baselineに対する既存`japicmp`成功 |
| CI候補 | Linux fresh runnerで再現でき、secret非露出、process / container / temporary resource残留0 |

## 5. Gate B作業分解案

### GB-1 — 完了条件・契約review

- DoD 2-5〜2-8、2-10とB1〜B4 Evidenceのtraceを確定する。
- local aggregate再利用、Public API / `japicmp`、CI job、remote / required化の判断事項をOwner reviewへ提示する。
- 成果物: `../architecture/validation/phase2-gate-b-contract-review.md`
- commit point: 文書と判断事項だけ。workflowや検証codeを変更しない。

### GB-2 — Gate B CI候補実装

- Ownerが承認した場合だけ、Milestone B Security aggregate用jobを`ci.yml`へ追加する。
- `verify-p2-b4-closeout.ps1`を正本として再利用し、Gate B専用の重複aggregateを作らない。
- Linux runner差異が判明した場合はToolingだけを補正し、Framework / Reference semanticsを変更しない。
- workflow README、timeout、権限、concurrency、cleanupを同じsliceで更新・検査する。

### GB-3 — local final verification

- workflow候補を含むfinal HEADでlocal Gate B aggregateを実行する。
- Public API inventory、既存published baseline compatibility、secret / PIIおよびresource cleanupを確認する。
- remote操作前にArchitecture Ownerへlocal結果を提示する。

### GB-4 — remote PR Evidenceとrequired化review

- 個別Owner承認後だけpush / PRを行い、同一final HEADをfresh Linux runnerで3回連続成功させる。
- 実行時間、失敗時を含むcleanup、権限、既存required checksへの回帰を確認する。
- 3回のremote successだけでrequired化せず、観測結果をArchitecture Ownerがreviewしてからrulesetを変更する。

### GB-5 — Gate B closeout

- local / remote Evidence、Public API方針、required check判断、PR / mergeおよびmerge後main CIを記録する。
- Architecture Owner承認後にGate Bを`COMPLETE / ACCEPTED`とし、P2-C1へ引き渡す。

## 6. 明示的に先送りする事項

- P2-C1のFramework Flyway正本、全第三者table一覧、clean install / supported upgrade
- 新しいsnapshot baselineの公開とPhase 2全artifactのpublished `japicmp`比較
- P2-C2のRoot Reactor外Consumer、OpenRewrite試作、Migration Support境界
- 業務application、申請承認workflow、`expense` module
- HTMX、REST API、SPA、Redis、Oracle、SAML、AWS固有Adapter

## 7. 作業開始条件

最初に`phase2-gate-b-contract-review.md`の判断事項をArchitecture Ownerがreviewする。承認前は次を実施しない。

- `.github/workflows/ci.yml`の変更
- remote push / PR / rerun
- main rulesetまたはrequired checkの変更
- environment / secret / Packages権限の追加
- P2-C1以降のproduction実装

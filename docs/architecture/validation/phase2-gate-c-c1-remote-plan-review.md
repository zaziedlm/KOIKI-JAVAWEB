# Phase 2 Gate C C-1 remote plan review

## 1. Status and boundary

- 点検日: 2026年9月12日
- Gate: `Phase 2 / Gate C / C-1`
- planning identity: `82880fb`（C3-3 Owner承認commit）
- branch: `feature/phase2-p2-c1-postgresql-migration`
- current remote main: `8873942`
- status: `REMOTE PLAN APPROVED / ORDER 1 COMMIT PENDING`
- Ownership: Tooling / Architecture Evidence / CI Policy
- remote mutation: 0

P2-C3は`COMPLETE / LOCAL VERIFIED / GATE C READY`としてArchitecture Owner承認済みである。
Gate C-1は実装を追加する工程ではなく、final PR、main CI、protected environment、一回限りのPhase 2 snapshot publishおよび
Gate C-3最終判定までのidentity、順序、権限、失敗時境界を確定するremote plan reviewである。

本点検ではGitHub CLI認証を回復し、remote refs、PR、workflow、environment、ruleset、Actions permissionおよびPackagesを
read-onlyで確認した。push、PR作成、merge、ruleset / environment変更、workflow dispatchまたはpublishは行っていない。

## 2. Read-only remote inventory

| Subject | Current fact | Gate C treatment |
|---|---|---|
| Repository | `zaziedlm/KOIKI-JAVAWEB`、PUBLIC、default branch `main` | 維持 |
| Worktree | clean | plan作成前の基準 |
| Branch relation | `origin/main`はHEADのancestor。HEADはmainより17 commits先 | final PRへまとめる |
| Tracking branch | `origin/feature/phase2-p2-c1-postgresql-migration`より7 commits先 | Owner承認後だけpush |
| Existing PR | 対象branchのopen / closed PRなし | final PRを新規作成 |
| Remote workflows | `CI`、`Publish Phase 1b internal snapshot`、`Java Runtime Compatibility`の3件 | Phase 2 workflowはmain反映前なので未登録 |
| Environments | `phase1a-internal-snapshot`、`phase1b-internal-snapshot` | `phase2-internal-snapshot`は未作成 |
| Maven packages | Phase 1bまでの9 packages、各version count 1 | Phase 2新規4 packagesは未公開 |
| Main ruleset | `main-merge-protection`、active / strict / bypass 0 | 変更しない |
| Required checks | 7 contexts | すべてfinal PR headで成功を要求 |
| Actions default permission | `read`、workflowからのPR approval不可 | 維持。publish jobだけ`packages: write` |

Phase 2初回公開対象となる新規packageは`koiki-starter-audit`、`koiki-starter-security`、
`koiki-starter-identity`および`koiki-starter-session-jdbc`の4件である。既存9 packagesと合わせた13座標を
Phase 2 release unitとして一度に公開する。

## 3. Source and release identity

`82880fb`はGate C-1開始時のplanning identityであり、publish対象commitではない。本review文書のcommitを含むfinal PR head、
PR merge commitおよびmerge後main HEADは順に確定してEvidenceへ記録する。

publish workflowの`expected_commit`には、required checksを通過したPRをmergeし、merge後main CIが成功した時点の
40文字main commit SHAを指定する。workflowは`main` refと`GITHUB_SHA`がこの値に一致しなければpublish前に失敗する。
Phase 2 workflowはdefault branchへ反映された後にだけmanual dispatchする。

## 4. Approved-order candidate

| Order | Action | Acceptance before next action |
|---:|---|---|
| 1 | 本Gate C-1 planをOwner reviewしcommit | local worktree clean |
| 2 | current branchをoriginへpush | local / remote branch head一致 |
| 3 | `main`向けfinal PRを作成 | base / head / Phase 2 C1〜C3・Gate C-1 diff inventoryを確認 |
| 4 | final PR required checksを実行 | 7 / 7 success、strict main最新化、bypassなし |
| 5 | merge commit方式でmainへmerge | PR final headとmerge commitを記録 |
| 6 | merge commitのmain CIを確認 | push eventの全対象job success |
| 7 | `phase2-internal-snapshot` environmentを作成・保護 | §6設定をread-back確認 |
| 8 | final main SHAでPhase 2 workflowを一度dispatch | authorize / preflight success後にenvironment承認 |
| 9 | publish / fresh remote verifyを確認 | §7 Evidenceすべてsuccess |
| 10 | Gate C-3 final Owner review | Phase 2 `COMPLETE / ACCEPTED`を判断 |

各remote mutationはこの順序で行い、後続成功を先行条件の代替にしない。Gate C-1承認前はorder 2以降を実行しない。

## 5. PR and main CI contract

main rulesetへ新しいrequired contextを追加せず、現在の次の7件をfinal PR headで要求する。

1. `Verify (ubuntu-24.04)`
2. `Public API Compatibility`
3. `Java Runtime Compatibility`
4. `Milestone B Integration`
5. `Milestone C Closeout`
6. `Security Foundation Integration`
7. `Local Identity Session Audit Integration`

rulesetのactive、strict、bypassなしを維持し、required checkの一時解除、管理者bypass、force pushまたは直接main pushを使わない。
merge後はmain commitに対する通常CI / runtime workflowの全job成功を確認してからenvironment作成とpublishへ進む。

## 6. Protected environment contract

`phase2-internal-snapshot`は既存Phase 1b environmentと同じ最小境界で新規作成する。

- required reviewer: `zaziedlm`
- prevent self review: false（単独Ownerがdispatchと承認を行える構成）
- admin bypass: disabled
- deployment branch policy: custom branch `main`だけ
- environment secret: 追加しない
- publish credential: Repository固有`GITHUB_TOKEN`だけ

作成後はAPIでname、reviewer、bypassおよびbranch policyをread-backし、期待値との一致前にdispatchしない。

## 7. Snapshot publish and remote Evidence

`.github/workflows/publish-phase2-snapshot.yml`を唯一のPhase 2 publish入口とする。

1. `authorize`: `main`とOwner承認済み40文字SHAの一致
2. `preflight`: formal package、aggregate Public API、local publish / acquisition path
3. `publish`: protected environment承認後、manifest由来のPOM 2 / JAR 11を単一sessionで公開
4. `verify-remote`: fresh runnerから取得し、hash-only manifestと照合

最終Evidenceは、同一workflow run / source commit、13座標のresolved snapshot value、13 POM / 11 JARの24 payload SHA-256、
11 JAR / 24型のaggregate signatureおよび11 same-source `japicmp`変更0である。remoteの共通build numberは要求しない。
Phase 1a固定baselineを差し替えず、Phase 2初回published baselineはAudit 6型、Identity 10型、Session 3型、Security 0型とする。

publish workflowは通常CIのrequired checkにせず、final main CI成功後に一度実施するPhase 2最終受入れ操作とする。

## 8. Failure and retry boundary

- `authorize` / `preflight`失敗時はremote package mutationなしとして原因を修正し、sourceが変わる場合はPR / main CIからやり直す。
- `publish`開始後の失敗は部分公開の可能性を否定せず、run、座標別metadataおよび取得可能payloadをread-onlyで保全・調査する。
- `Capture`または`verify-remote`失敗時も、成功扱いまたは自動retryをしない。
- retry、package version削除、workflow rerunまたは別SHAでのpublishは、調査結果と対象SHAに対する追加Owner承認後だけ行う。
- 失敗runのpayloadをaccepted baselineとせず、Gate C-3で承認した成功runのmanifestだけをPhase 2 baseline identityとする。

## 9. Explicit non-changes

- main ruleset / required checksを変更しない。
- Phase 1a / Phase 1b environmentまたはsnapshot workflowを変更しない。
- 通常CIへ`packages: write`、environmentまたは追加secretを与えない。
- package / versionを削除、上書きまたは再公開しない。
- Gate C-3前に`PHASE 2 COMPLETE`または`ACCEPTED`と表現しない。

## 10. Architecture Owner review points

1. read-only remote inventoryと、current branchをfinal PRへ継続使用する判断を承認するか。
2. Gate C-1 review commit後にbranchをpushし、`main`向けfinal PRを新規作成してよいか。
3. rulesetを変更せず、active / strict / bypassなしと既存required checks 7件をfinal PR条件としてよいか。
4. 7件成功後にmerge commit方式でmergeし、merge後main CIの全対象job成功をpublish前提としてよいか。
5. §6の`phase2-internal-snapshot` environmentをmain限定・required reviewer・admin bypass禁止・追加secretなしで作成してよいか。
6. final main CI成功後、承認済み40文字main SHAを指定してPhase 2 workflowを一度dispatchしてよいか。
7. §7のhash / signature / `japicmp`をPhase 2 snapshot remote Evidenceとしてよいか。
8. §8の失敗時境界を採用し、publish開始後の失敗を無断retryまたはdeleteしない方針でよいか。
9. remote Evidence成功後にGate C-3最終Owner reviewを行い、それまではPhase 2を完了扱いしないか。
10. Gate C-1承認をorder 2以降の個別remote操作に対する実行承認とし、順序とidentityを変更する場合は再reviewするか。

推奨結論は上記10点を承認し、§4の順序でGate C-2 remote Evidence取得へ進むことである。
本reviewが承認されるまではpush、PR、merge、ruleset / environment変更、workflow dispatchおよびsnapshot publishを
`NOT APPROVED / NO-GO`として維持する。

## 11. Architecture Owner approval

2026年9月12日、Architecture Ownerは§10の1〜10を提案どおり承認した。read-only remote inventory、current branchの
継続使用、既存main rulesetおよびrequired checks 7件を受け入れる。

本承認記録のcommit後、§4の順序に従い、branch push、final PR作成、required checks、merge commit方式のmain反映、
merge後main CI確認、`phase2-internal-snapshot` environment作成、および承認済みfinal main SHAに対する一回限りの
snapshot publish / remote verifyへ進めてよい。

rulesetの緩和、bypass、追加secret、無断retry、package削除または別SHAでのpublishは承認しない。順序またはidentityを
変更する場合は再reviewする。remote Evidence成功後にGate C-3最終Owner reviewを行い、それまではPhase 2を完了扱いしない。

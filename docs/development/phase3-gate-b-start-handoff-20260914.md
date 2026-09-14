# Phase 3 Gate B開始引継ぎ

## 1. Handoff status

- 作成日: 2026年9月14日
- 再開予定日: 2026年9月15日
- Architecture Owner: Shuichi Kataoka
- branch: `feature/phase3-reference-vertical-slice`
- accepted baseline commit: `cd3355c`（P3-B4実装・検証・Owner承認）
- Phase status: `P3-B1〜P3-B4 COMPLETE / OWNER APPROVED — GATE B INVENTORY READY`
- Ownership: Architecture Evidence / Reference Web and Query Acceptance
- production change: 0
- immediate next action: P3-B1〜P3-B4 Evidenceのread-only横断棚卸し

本handoffは、別端末でGate Bを安全に再開するための開始境界である。P3-B1〜P3-B4の実装を再開または
変更するものではなく、Web / query acceptanceを横断評価する。Gate B承認前にP3-C0 REST contract review、
REST production code、workflow、required check、PRまたはmergeへ進まない。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. 本書
5. `KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`のMilestone B、Gate B、Hybrid Verification、§20〜24
6. `../architecture/validation/phase3-p3-b0-mvc-htmx-contract-review.md`
7. `../architecture/validation/phase3-p3-b1-read-model.md`
8. `../architecture/validation/phase3-p3-b2-mvc-thymeleaf.md`
9. `../architecture/validation/phase3-p3-b3-htmx.md`
10. `../architecture/validation/phase3-p3-b4-lock-cache-contract-review.md`
11. `../architecture/validation/phase3-p3-b4-lock-cache.md`
12. `../architecture/README.md`

## 3. Approved transfer baseline

1. P3-B1 read modelは`COMPLETE`、P3-B2〜P3-B4は`COMPLETE / OWNER APPROVED`である。
2. masterはTier 1 SIMPLE / JPA、expenseはTier 2 RICH / JPA共有モデルを維持する。
3. expense表示専用read modelのscope先行read-only JOINと、current-value master確認の狭い同期contractは
   別の承認済み例外であり、更新、認可判断または業務不変条件へ拡張しない。
4. Thymeleaf full-page HTMLを主軸とし、HTMXはmaster検索、paging、登録Validationだけへ選択適用する。
5. Phase 2のdefault deny、CSRF / Security Header、Identity、Business / Security AuditおよびSpring Session JDBCを再利用する。
6. P3-B4はReference専用409画面、先行transactionだけの確定、active経費科目optionだけの30秒cache、
   非cache current-value確認によるfail-safeを実証した。
7. cache dependency / configurationは`koiki-reference-app`、browser harnessとdemo seedは非配布Toolingに限定する。
8. Framework Public API、Starter、BOM、migration、Root Reactor moduleおよびworkflowをP3-B4で変更していない。
9. accepted baselineでRoot Reactor 16 / 16 projects、Reference 85 tests、headed Playwright 2 tests、
   PostgreSQL state / Audit / DB、TTL前後、stale category拒否およびapplication logを検証した。
10. local Applicationと`--rm` PostgreSQL containerは停止・破棄済みで、credential、Cookie、Session ID、
    CSRF tokenまたは一時HMAC keyを文書・Repositoryへ保存していない。

## 4. Gate B inventory scope

| Viewpoint | Gate Bで横断評価する内容 |
|---|---|
| Ownership | Framework / Reference / ToolingがB1〜B4を通じて混在していない |
| Query | applicant / approver / accounting scope、materialize、paging、current-value例外が一貫する |
| MVC | Form / View DTO / Controller / Application Use Case境界と通常HTML経路を維持する |
| HTMX | 選択適用、CSRF、history、loading、focus、partial errorと通常HTML fallbackが整合する |
| Conflict | hidden version、Application比較、JPA `@Version`、409回復導線、state / Audit原子性が整合する |
| Cache | option一覧だけ、30秒TTL、master管理query非cache、command fail-safeを維持する |
| Security | default deny、Permission、resource ownership、部門scope、Sessionと情報非露出を弱めていない |
| Accessibility | label、error、focus、loading、status / alert、keyboard操作のEvidenceと未確認範囲を分ける |
| Hybrid verification | API / MockMvc / 実DB / browser / Owner目視 / log / Audit / DBの証拠を重複なくtraceする |
| Deferred | REST、distributed cache、Framework昇格、SPA、Level 2、remote変更を先行していない |

## 5. Verified and not yet verified

### Verified before handoff

- B1〜B4個別のApplication、MVC、Security、PostgreSQLおよびarchitecture tests。
- Root Reactor `clean verify`、実行可能JAR生成、package inventory。
- headed PlaywrightによるHTMX回帰と独立2 BrowserContext競合。
- Ownerによる通常HTML、Validation、HTMX履歴、cache TTL前後、stale category拒否の目視。
- 競合とfail-safeのstate / version / Audit / DB突合。
- application logの予期しないERROR、stack trace、secretおよびrequest parameter非露出。

### Not yet verified as Gate B

- P3-B1〜P3-B4を一体として捉えたacceptance traceと重複・欠落の棚卸し。
- accessibility Evidenceの横断評価と、検証済み／目視未確認／deferredの明示。
- 引継ぎ後のclean HEADに対するRoot Reactor最終再検証。
- Gate B Architecture Owner decisionとclose record。

個別CPで成功済みの検証を理由なく再実行しない。Gate Bで必要な再検証は、横断評価で欠落または矛盾が判明した場合と、
最終clean HEADのRoot Reactorに限定する。

## 6. Tomorrow start checklist

```powershell
git status --short --branch
git branch --show-current
git log -3 --oneline --decorate
git rev-parse HEAD
java -version
.\mvnw.cmd --version
docker version
```

期待値:

- branchが`feature/phase3-reference-vertical-slice`。
- `cd3355c`を含むhandoff commitがHEADで、origin tracking branchと一致する。
- worktreeがclean。
- Java 21とRepository Maven Wrapperを利用できる。
- DockerはGate B棚卸し開始時には必須ではない。最終Root検証時に利用可能性を確認する。

## 7. Proposed Gate B sequence

1. P3-B1〜P3-B4 Evidenceをread-onlyで横断し、成立事項と検証外事項を分類する。
2. `docs/architecture/validation/phase3-gate-b-web-query-acceptance.md`へGate B棚卸し案を作成する。
3. Architecture Owner decisionsと非blocking / blocking事項をreviewする。
4. 必要な場合だけ限定修正とfocused testを行う。
5. clean HEADでRoot Reactor `clean verify`を実行し、最終Evidenceを記録する。
6. Architecture Owner承認後にGate Bを`COMPLETE / ACCEPTED`としてcommitする。
7. Gate B承認後にだけP3-C0最小REST API contract reviewへ進む。

## 8. Stop conditions

- Gate B棚卸し前にP3-C0 REST Controller、REST DTO、endpointまたはerror contractを追加する。
- 個別CPの検証を無目的に繰り返し、横断評価と未確認範囲の整理を置き換える。
- 表示専用read modelまたはcacheを更新、認可、current-value判断へ拡張する。
- Referenceの競合画面、cache、fixtureまたはbrowser harnessをFramework Public API / Starterへ昇格する。
- test user、password、HMAC key、Cookie、Session IDまたはCSRF tokenをEvidenceへ保存する。
- Ownerの個別承認なしにPR、merge、workflow dispatch、ruleset変更またはsnapshot publishを行う。

## 9. Immediate next action

明日はproduction実装や再テストから開始せず、P3-B1〜P3-B4のEvidenceを横断し、Gate Bで既に成立している事項、
追加確認が必要な事項、明示的deferredを分類する。最初の成果物はGate B acceptance棚卸し案だけとする。

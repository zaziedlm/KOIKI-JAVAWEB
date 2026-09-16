# Phase 3 P3-C4 Journey / ADR / Skill / DoD traceability closeout

## 1. Status

| Item | Result |
|---|---|
| Date | 2026年9月16日 |
| Branch | `feature/phase3-reference-vertical-slice` |
| Phase 3 start baseline | `c88b335efdd556613c9ef7f4c5267214fdb8254b` |
| P3-C4 start HEAD | `4f6b2ccc4b186c46d5dfcba1f83d0e5fb951da65` |
| Current status | `IN PROGRESS — C4-1 BASELINE CLASSIFICATION COMPLETE` |
| Ownership | Architecture documentation / closeout verification |
| Production change in C4-1 | 0 |
| Remote operation | 0 |
| Next | C4-2 DoD / AC / ADR / Skill trace matrix |

P3-C4は、Phase 3で追加した実装と承認Evidenceを横断して、DoD、Reference AC、Journey、ADR、Skill、
Public API、migration、dependency、artifactおよびdeferred inventoryを一致させるcloseout CPである。

本節までに実施したC4-1はread-onlyのGit baseline差分分類である。Maven、Docker、PostgreSQL、Browser、
package済みJARまたはfocused Toolingはまだ実行しておらず、P3-C4 closeを宣言しない。

## 2. Comparison method

次の範囲をGitのcommit済み差分として比較した。

```text
c88b335efdd556613c9ef7f4c5267214fdb8254b..4f6b2ccc4b186c46d5dfcba1f83d0e5fb951da65
```

- `git diff --name-status`で追加・変更・削除を分類した。
- `git diff --shortstat`とtop-level / source-set単位の件数を突合した。
- POM、Public API inventory、migration、workflowは個別に差分を確認した。
- commit履歴をP3-CP0〜P3-C4 start handoffのCP / Gate記録と照合した。
- 内容判断が必要なFramework rule、Security設定、application propertyは実diffを確認した。

生成物、untracked file、作業中の差分をbaseline比較へ混入させないため、C4-1はP3-C4準備commit後の
clean HEADから開始した。

## 3. Aggregate result

| Measure | Result |
|---|---:|
| Commits after Phase 3 baseline | 28 |
| Changed files | 194 |
| Added | 172 |
| Modified | 22 |
| Deleted | 0 |
| Insertions | 14,833 |
| Deletions | 79 |

主要file typeは次のとおりである。

| Type | Count |
|---|---:|
| Production Java | 96 |
| Test Java | 26 |
| Maven POM | 7 |
| Markdown | 41 |
| SQL migration | 3 |
| HTML / Thymeleaf | 14 |
| Properties | 2 |
| PowerShell | 2 |
| JavaScript | 1 |
| CSS | 1 |
| Auto-configuration imports metadata | 1 |

削除またはrenameはなく、既存Phase 2契約を除去する差分は認めない。

## 4. Top-level classification

| Root / area | Files | Ownership | Classification | Primary CP / Gate |
|---|---:|---|---|---|
| `AGENTS.md` | 1 | Repository guidance | Phase状態とstop conditionの同期 | P3-CP0〜C4 |
| Root `pom.xml` | 1 | Build Foundation | Web MVC StarterをRoot Reactorへ追加 | P3-B0 / B2 |
| `koiki-dependencies-bom` | 1 | Framework | Web MVC Starter座標とHTMX 2.0.10を管理 | P3-B0 / B2 |
| `koiki-archunit-rules` | 4 | Framework | ADR-049の狭いmodule contract例外とpositive / negative fixture | P3-A0 / A1 |
| `koiki-starters/koiki-starter-web-mvc` | 8 | Framework | 正式Starter、内部auto-configuration、resource contract | P3-B0 / B2 / B3 |
| `koiki-reference-app` | 132 | Reference | master / expense、MVC、REST、Security統合、migration、test | P3-A1〜C1 |
| `build-support` | 13 | Tooling | local demo、browser、API、critical E2E、index | P3-B2〜C2 |
| `docs` | 34 | Architecture / Reference / Development | contract、Evidence、Journey、Skill、handoff | 全CP / Gate |
| **Total** | **194** | — | — | — |

想定外のtop-level directory、Customer artifact、Project Template、workflow、cloud固有Adapterまたは
Phase 4 moduleの変更は存在しない。

## 5. Framework classification

### 5.1 Build / artifact

| Change | Result | Disposition |
|---|---|---|
| Root Reactor | `koiki-starter-web-mvc`を1 module追加 | P3-B0承認、P3-B2実装 |
| Formal release inventory | 15 projects / 12 JAR | P3-B0承認値と一致する見込み。C4 inventoryで再検証 |
| Root Reactor inventory | 16 projects / 13 JAR | P3-B2 Evidenceと一致 |
| Formal publish unit | Root aggregatorを除く14座標 | P3-B0承認値。Remote publishは未実施 |
| BOM | Web MVC Starter座標、HTMX `2.0.10`を追加 | P3-B0承認済み |
| Java Public API inventory | Phase 3 start baselineとの差分0 | Web MVC StarterはJava Public API 0型 |

`build-support/api-compatibility/public-api.txt`はbaselineと同一であり、`PersistenceModel.SEPARATED`を含む
Public API追加はない。新Starterは正式artifactだが、Java classをconsumer APIとして公開せず、internal
auto-configurationとclasspath resource contractだけを提供する。

### 5.2 Web MVC Starter

追加8 filesは次の責務へ限定される。

- Maven dependency aggregation。
- `internal` packageのauto-configuration。
- Spring Boot auto-configuration imports metadata。
- 共通Thymeleaf fragment。
- `/koiki-web/**`配下のCSSとHTMX integration JavaScript。
- artifact README。

Reference業務Controller、Form、View DTO、Template、PermissionまたはmigrationはStarterへ混入していない。

### 5.3 Architecture rule

`BusinessModuleRuleSet`はRule 3へADR-049の狭いread-only contract例外を追加した。許可条件は次のすべてである。

- 呼出元がconsumer moduleの`adapter.outbound`である。
- 呼出先がprovider moduleの`contract`である。
- contractがinterfaceであり、名称が`Query`で終わる。

test fixture 2 filesと`BusinessModuleRuleSetTest`変更により、許可経路と境界違反を検査する。
Framework Identity、Security、Data、AuditまたはSessionのPublic APIは変更していない。

## 6. Reference classification

`koiki-reference-app`の132 filesは次のsource setへ分類できる。

| Source set | Files | Classification |
|---|---:|---|
| `src/main/java` | 93 | master、expense、identity Security fitting、home |
| `src/main/resources` | 18 | application config 2、migration 3、Template 13 |
| `src/test/java` | 20 | Domain / Application / Persistence / MVC / REST / Security integration |
| module POM | 1 | approved Starter / cache / test dependency selection |

### 6.1 Business module ownership

| Module / area | Main Java files | Responsibility | Primary Evidence |
|---|---:|---|---|
| `master` | 29 | Tier 1管理Use Case、JPA、current-value contract、MVC | P3-A1、B1〜B3 |
| `expense` | 56 | Tier 2 Domain、JPA共有model、scope、event、read model、MVC、REST | P3-A2〜A4、B1〜B4、C1 |
| `identity.configuration` | 7 added + 1 modified | MVC Session chain維持、P3-C1限定Bearer chain / Problem response | P3-B2、C0 / C1 |
| home | 1 | server-side UI navigation | P3-B2 |

master / expenseは単一`koiki-reference-app`内の業務packageであり、別Maven artifactへ分割していない。
expenseはJPA共有モデルを維持し、MyBatis code、Mapperまたは分離Persistence Modelを含まない。

### 6.2 Resources and schema

| Resource | Count | Classification |
|---|---:|---|
| `application.properties` | 1 modified | Reference Flyway、cache / TTL、MVC API versioning基礎設定 |
| `application-api-bearer.properties` | 1 added | P3-C1限定Bearer代表profile |
| Reference migration | 3 added | V1 master、V2 expense、V3 approver scope |
| Reference table | 6 | master 3、expense 3。module内FKのみ |
| Thymeleaf Template | 13 changed | home、identity fitting、master、expense |

Framework migration差分は0であり、Reference migrationは`classpath:db/migration/kkref`と
`kkref_flyway_history`に分離される。production seedは追加していない。

### 6.3 Dependency classification

Reference POMは次の承認済み変更を持つ。

- Spring MVC / Thymeleaf直接依存を正式`koiki-starter-web-mvc`へ置換。
- REST / Problem Detailsのため`koiki-starter-api`を追加。
- Reference限定cacheのためSpring Cache / Caffeineを追加。
- test scopeへ`koiki-testing`を追加。

MyBatis、Redis、WebFlux、Spring Modulith runtime、SPA、SAML、OracleまたはAWS固有dependencyは追加していない。

## 7. Tooling classification

`build-support`差分13 filesはすべて非配布Toolingである。

| Tooling | Files | Purpose | Boundary |
|---|---:|---|---|
| Root Tooling index | 1 modified | 各verification入口を案内 | code / artifactなし |
| `reference-local-demo` | 2 added | 使い捨てlocal seedと手動確認支援 | production seedではない |
| `reference-browser-verification` | 4 added | HTMX / CSRF / history / conflictの実Chromium確認 | Root Reactor外、test scope |
| `reference-api-verification` | 3 added | package済みJARのBearer API journey | Root Reactor外、test scope |
| `reference-e2e-verification` | 3 added | Browser / API / DB / Audit / log critical journey | Root Reactor外、test scope |

Playwright、test issuer、key、token、user、password、failure / seed fixtureはFramework artifact、Reference JAR、
`koiki-testing`、Project TemplateまたはCustomer dependencyへ含めていない。

## 8. Documentation / governance classification

docs差分34 filesは次のように分類する。

| Area | Files | Classification |
|---|---:|---|
| Agent Skills | 2 modified | Phase状態、Ownership、業務feature workflowの同期 |
| Architecture | 23 | ADR / Grand Design fitting、Phase estimate、index、Phase 3 Evidence |
| Development | 6 | Phase 3実行計画とGate / CP start handoff |
| Reference | 3 | 業務仕様、Reference index、Local Run Guide |

Phase 3 validation EvidenceはP3-CP0、P3-A0〜A4、Gate A、P3-B0〜B4、Gate B、P3-C0〜C3を
連続して記録する。P3-C3は未完了扱いではなく、Architecture Owner判断による
`DEFERRED — MyBatis adoption trigger required`として記録済みである。

## 9. Commit / CP classification

baseline後28 commitsは、次の承認系列から外れていない。

| Sequence | Commit purpose | Classification |
|---|---|---|
| P3-CP0 / A0 | Phase開始、Docker baseline補正、module / migration contract | Documentation / blocking review |
| P3-A1〜A4 | master、expense、authorization / Audit、同期event | Reference production + test |
| Gate A | Milestone A acceptance | Documentation / Owner acceptance |
| P3-B0 | MVC / HTMX contract | Documentation / blocking review |
| P3-B1〜B4 | read model、MVC、Tooling、HTMX、lock / cache | Framework + Reference + Tooling |
| Gate B | inventory、browser stabilization、accessibility remediation、acceptance | Production / test / documentation |
| P3-C0 | REST contract | Documentation / blocking review |
| P3-C1 | REST API、Bearer profile、API Tooling | Reference + Tooling |
| P3-C2 | critical journey E2E | Tooling + Evidence |
| P3-C3 | MyBatis adoption-trigger deferral | Documentation only |
| P3-C4 start | traceability closeout handoff | Documentation only |

P3-C3またはP3-C4準備commitにproduction、Public API、dependency、migrationまたはworkflow変更はない。

## 10. Boundary checks

| Check | C4-1 result |
|---|---|
| Unexpected top-level path | 0 |
| Deleted / renamed baseline contract | 0 |
| Java Public API inventory diff | 0 |
| Framework migration diff | 0 |
| Reference migration | V1〜V3、3 files / 6 table |
| Workflow diff | 0 |
| MyBatis `SEPARATED` / fixture / test dependency | 0 |
| Root ReactorへのTooling混入 | 認めない |
| Phase 4 production code / module | 0 |
| Remote mutation | 0 |

## 11. Findings and follow-up

### 11.1 Approved / consistent

- 194 filesはすべて承認済みCP、Gate、P3-C3延期またはP3-C4開始準備へ分類できる。
- Framework / Reference / Tooling / DocumentationのOwnershipを跨いだ未説明の昇格は認めない。
- 正式Framework追加はWeb MVC Starter 1 artifactであり、P3-B0 / B2の承認範囲と一致する。
- Public API、Framework migrationおよびworkflowは承認済みbaselineを維持する。

### 11.2 Explicitly deferred / pending

- P3-C3 MyBatis規約fixture、`PersistenceModel.SEPARATED`、Rule 25〜27 / 30〜37。
- DoD 3-11の実CI PASS、workflow / required check / remote operation。
- Phase 4、optional Gateおよび実行計画§13のdeferred scope。

### 11.3 Documentation gap for C4-2 / Journey work

Reference Local Run Guideの対象表示はP3-B2であり、Phase 3完成形の通常Session MVC、P3-C1限定Bearer profile、
browser / API / critical E2E Toolingへの入口がReference engineer-facing Journeyとしてまだ集約されていない。
production defectではなく、P3-C4のJourney / Skill closeoutで最小差分補正するdocumentation gapと分類する。

## 12. C4-1 conclusion

Phase 3開始baselineからP3-C4開始HEADまでの194 filesを、Framework、Reference、Tooling、Documentation、
承認CP / Gateへ分類できた。未説明のproduction、Public API、migration、dependency、workflowまたはPhase 4差分は
検出していない。

C4-1を`COMPLETE`とし、次はC4-2 DoD 3-1〜3-11、AC-P3-01〜10、ADRおよびSkillのtrace matrixを作成する。
この結論はMaven / runtime verificationまたはP3-C4 close承認を代替しない。

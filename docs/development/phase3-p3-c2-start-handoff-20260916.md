# Phase 3 P3-C2開始引継ぎ

## 1. Handoff status

- 作成日: 2026年9月16日
- Architecture Owner: Shuichi Kataoka
- branch: `feature/phase3-reference-vertical-slice`
- approved baseline commit: `c9f8ab174d9d3a9978427301b7e09e519de9f18b`
  （P3-C1 minimal REST API close承認）
- Phase status: `P3-C0〜C1 COMPLETE / OWNER APPROVED — P3-C2 READY`
- Primary ownership: 非配布Browser / API / DB / log E2E Tooling
- Reference production change: 原則0。package済み`koiki-reference-app`を検証対象として使用する
- planned placement: `build-support/reference-e2e-verification`
- immediate next action: P3-C2 critical journey fixtureとcleanup境界の実装

本handoffはDoD 3-11のcritical journey E2Eを開始する導線である。P3-B3 / B4のbrowser journeyと
P3-C1のpackage済みAPI journeyを置き換えず、同じpackage済みJARとPostgreSQL上でBrowser、API、DB、Audit、
logを一連で突合する代表happy pathだけを追加する。CI候補としてruntime、flakiness、cleanupを測定するが、
Remote Gate承認前にworkflow、required checkまたはremote設定を変更しない。

## 2. Required reading order

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. 本書
5. `KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`のMilestone C、§10、§14〜17、§27
6. `../architecture/validation/phase3-p3-c1-minimal-rest-api.md`
7. `../architecture/validation/phase3-gate-b-web-query-acceptance.md`
8. `../../build-support/reference-browser-verification/README.md`
9. `../../build-support/reference-api-verification/README.md`

## 3. Work positioning

```text
Phase / status: Phase 3 / P3-C0〜C1 COMPLETE / OWNER APPROVED / P3-C2 READY
Ownership: Tooling
Target: package済み koiki-reference-app JAR
Business modules: master / expense
Verification: L3 package HTTP + L4 Chromium + L6 DB / Audit / log
Deferred: workflow / required check / remote、P3-C3以降、Phase 4
```

Reference production source、Framework Public API、migration、正式Maven module、BOM、Root Reactor moduleおよび
Project TemplateはP3-C2の変更対象にしない。検証でproduction defectを検出しReference変更が必要になった場合は、
Tooling変更へ混在させず原因と影響範囲を記録して停止する。

## 4. Critical journey contract

P3-C2では次の1 journeyだけを同一process / DBで自動化する。

1. Testcontainersで使い捨てPostgreSQL 17を起動する。
2. 実行ごとのRSA keyとloopback OIDC / JWKS issuerをTooling内で生成する。
3. package済みReference JARを`api-bearer` profileで起動する。local Session loginは有効のまま維持する。
4. applicantとapprover、Role / Permission、部門、経費科目、所属、承認scopeを使い捨てDBへ準備する。
   password、key、tokenおよびCookieは実行中だけ保持し、固定値またはEvidenceへ保存しない。
5. applicantのBearerでexpense draftを作成し、detailを取得してsubmitする。
6. approverが実ChromiumからSession loginし、代表master HTMX検索を1回行い、同じ申請を承認する。
7. applicantのBearer detailで`APPROVED`を確認し、transportをまたいだ最終状態を確認する。
8. DBでrequest state / version、明細合計、applicant / approver scopeを突合する。
9. Business Auditの`SUBMIT_EXPENSE`と`APPROVE_EXPENSE`、Security Auditのlogin成功を突合する。
10. process logへtoken、password、Cookie、private key、PIIまたは内部例外が出ていないことを確認する。
11. 成否にかかわらずBrowserContext、Chromium、issuer、Reference process、PostgreSQL、temp logを終了・除去する。

このjourneyはSession CookieをAPIへfallbackさせたり、BearerをMVC loginへ利用したりしない。同一process上で
独立したSecurityFilterChainが同時に成立することを確認するが、P3-C1の認証negative matrixをE2Eへ複製しない。

## 5. Reuse and non-duplication

- Playwright Java / Chromiumは承認済み`1.62.0`を維持し、別browser engineを追加しない。
- P3-B3 / B4のHTMX、Validation、CSRF、history、2 Session競合journeyは既存Toolingに残す。
- P3-C1のJWT validation、401 / 403、Problem Details、rollback matrixは既存testに残す。
- P3-C2はhappy pathのtransport連携と外部状態突合に限定し、全画面・全Role・全拒否組合せを集約しない。
- issuer、process起動、seed、HTTP clientおよびcleanupの考え方は既存Toolingを再利用するが、
  test fixtureをFramework、Reference JAR、`koiki-testing`またはProject Templateへ昇格させない。

## 6. Proposed implementation sequence

1. Root外の非配布`reference-e2e-verification` Maven Toolingを追加し、Playwright、Testcontainers、PostgreSQL、
   JOSEをtest scopeだけに置く。
2. package済みJAR、PostgreSQL、issuer、dynamic port、sanitized process logをlifecycle管理するfixtureを実装する。
3. browser loginに必要なrandom passwordと、API Bearerに必要なtokenをmemory内で生成するseedを実装する。
4. §4のAPI → browser → API critical journeyとDB / Audit / log assertionを実装する。
5. success、assertion failure、application startup failureの各経路でcleanupを確認する。
6. focused journeyを3回連続実行し、各runtime、成否、残存process / container / port / temp fileを記録する。
7. Root Reactor `clean verify`を実行し、既存P3-B3 / B4およびP3-C1 focused Toolingを回帰確認する。
8. `docs/architecture/validation/phase3-p3-c2-critical-journey-e2e.md`へEvidenceとCI候補判断を記録する。
9. P3-C2 close review後にcommitし、P3-C3より先へ進まない。

## 7. Required verification at close

- package済みJARだけを外部processとして起動し、classes directoryへfallbackしない。
- BrowserはSession、APIはBearerを使用し、同一DBの1申請が`DRAFT → SUBMITTED → APPROVED`になる。
- API / browser response、DB state / version / line total、Business / Security Auditが同じactor / resourceへ一致する。
- representative HTMX requestがfragment、`HX-Request`、200およびfocus維持を満たす。
- token、password、Cookie、private key、PII、SQLまたはstack traceをlog / test report / Evidenceへ保存しない。
- 3回連続PASSし、runtimeの異常な増加やtiming依存待機がない。
- 各run後にapp process、issuer、browser、PostgreSQL container、portおよびtemp logが残らない。
- Root Reactorと既存browser / API focused Toolingが回帰しない。
- CIから同じlocal entrypointを呼べるが、workflow追加やrequired化はRemote Gateへ送る。

## 8. Stop conditions

- E2E都合でReference production codeへtest route、fixed user、failure switchまたはseedを追加する。
- password、token、Cookie、Session ID、CSRF token、private keyまたは個人情報を出力・保存する。
- E2Eへ全画面、全Role、全negative matrixを集約する。
- `api-bearer`のP3-C1境界、MVC Session / CSRFまたはREST契約を変更する。
- Browser / E2E fixtureをRoot Reactor、Framework artifact、Reference JAR、`koiki-testing`またはProject Templateへ含める。
- cleanup failureを無視してCI候補と判断する。
- Ownerの個別承認なしにworkflow、required check、push / PR / merge、rulesetまたはsnapshot publishを変更する。
- P3-C2 close前にP3-C3、Phase 4、Framework Public APIまたはmigration変更へ進む。

## 9. Start readiness

2026年9月16日に次を確認した。

- HEAD `c9f8ab174d9d3a9978427301b7e09e519de9f18b`
- Java `21.0.12.1`、Maven Wrapper `3.9.16`
- Docker Engine `29.5.3`、残存PostgreSQL 17 containerなし
- Playwright Java `1.62.0`対応Chromium cacheあり
- P3-C1 commit後のworktree clean

次回は§6手順1から開始する。Chromiumの再download、workflow変更またはremote操作は開始条件にしない。

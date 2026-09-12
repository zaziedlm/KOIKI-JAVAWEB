# Phase 2 P2-C3 start handoff

## 1. Purpose

P2-C2 Owner承認後、P2-C3 Developer Journey / DoD closeoutを安全に開始するための引継ぎである。
P2-C3はPhase 2成果物を横断的に再現・整理する工程であり、新しいFramework機能を追加しない。機構の成立だけでなく、
エンジニアが入口を発見し、構成を選び、Ownershipを守って実装し、失敗を診断・修正・cleanupできる利用者接点を重点確認する。

## 2. Git and status

| Subject | State |
|---|---|
| branch | `feature/phase2-p2-c1-postgresql-migration` |
| baseline HEAD | `cf6537955590c72e0864da3f9a7a6a5c021c8578` |
| main merge base | `8873942b3c8b9c83f08b607f9fd03ebbad928324` |
| upstream | `origin/feature/phase2-p2-c1-postgresql-migration`、baseline時点でahead 3 / behind 0 |
| Java / Maven | Temurin 21.0.12.1 / Maven Wrapper 3.9.16 |
| C2 state | `COMPLETE / ARCHITECTURE OWNER APPROVED` |
| C3-1 | `COMPLETE / ARCHITECTURE OWNER APPROVED` |
| next | C3-2 Engineer-facing documentation / Developer Journey |
| remote publish | `NOT APPROVED / NO-GO` |

本handoff作成時の未commit差分はC2-7 Owner承認記録、Phase 2実行計画更新、本handoffおよびC3-1 contract reviewである。
pull、rebase、reset、branch作成またはpushを自動実行しない。

## 3. Scope and boundaries

- Ownership: Architecture Evidence / Tooling
- formal Framework release unit: 14 projects / 11 JARを維持
- Root Reactor: Referenceを含む15 projectsを維持
- Public API: aggregate 24型を維持
- Framework migration: 3 SQL / 11 tableを維持
- Reference: Root参加、formal release / BOM / publishから分離
- Tooling / fixture: Root / BOM / formal releaseへ昇格しない

P2-C3でproduction code、Public API、production migrationまたは新しいMaven artifactが必要になった場合は作業を停止し、
要求、Ownership、DoDへの影響および別CP案をOwnerへ提示する。

## 4. Authoritative documents

1. `AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/development/KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`
4. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §27.6 / §27.10
5. `docs/architecture/validation/phase2-p2-c3-contract-review.md`
6. `docs/architecture/validation/phase2-p2-c2-c2-7-closeout.md`
7. Gate A / Gate B / P2-C1 / P2-C2の各validation Evidence

Phase 1b CP10 closeoutは構成上の参考にできるが、Phase 2のDoD、release unit、remote判断の正本にはしない。

## 5. Work sequence

1. C3-1 contract / inventoryをArchitecture Ownerがreviewする。2026年9月12日承認済み。
2. Repository / Architecture / Consumer / verification / Reference README、DoD ledgerおよびhuman-operableな
   Developer Journeyを最小差分で整備する。
3. 文書 / Tooling差分をcommitし、clean HEADを検証identityとして固定する。
4. Gate A、P2-C1、P2-C2、Gate B、Root、Null Safety、sensitive-output、cleanupを横断実行する。
5. C3-3 local EvidenceをOwner reviewへ提示する。
6. local承認後、Gate C remote planを別途reviewする。

## 6. Stop conditions

- C3-1承認前のproduction、workflow、remote変更
- Consumer / Reference / fixtureのformal Framework artifactへの昇格
- 新しいPublic API、migration、Starter、将来packageの追加
- 通常local Maven cacheまたはFramework source pathに依存したConsumer成功
- secret、credential、token、private keyまたはPIIのsource / log / Evidence保存
- C2-5承認条件を満たさないsnapshot publish
- final PR checks / main CI前のPhase 2完了表現
- Phase 3、Authorization Server、SAML、Redis、WebFlux、Oracle、AWS固有Adapter、正式OpenRewrite recipeの先行

## 7. Resume point

`phase2-p2-c3-contract-review.md` §9の10判断は2026年9月12日にArchitecture Owner承認済みである。
次はC3-2のEngineer-facing README、DoD ledger、Developer Journeyおよび必要最小限のaggregate接続を整備する。
production code、Public API、migration、workflowまたはremote stateは変更しない。

# KOIKI-JavaWeb-FW Phase 0 DoD Closeout

**版:** v0.1  
**棚卸日:** 2026年8月14日  
**状態:** In Progress  
**基準Commit:** `3bfac88`

## 1. 目的

グランドデザイン v0.2 §27.3のPhase 0完了条件0-1〜0-8について、現状、証拠、
不足、および完了判定に必要な成果物を一か所で管理する。

Walking Skeletonの技術検証完了とPhase 0全体の完了を混同しない。後続Phaseで決める
REST API、Security、SPA、非同期event等の具体的な実装規約は、本Closeoutで先行確定しない。

## 2. DoD棚卸し

| DoD | 状態 | 現状と証拠 | 不足 | 完了条件 |
|---|---|---|---|---|
| 0-1 ADR | COMPLETE | グランドデザイン§30の有効ADR 43件を`adr/README.md`へ過不足なく登録し、区分（確定28件／Phase 0で検証15件）、証拠、検証scopeをreviewした。2026年8月15日に43件すべてをArchitecture Ownerが承認 | なし | 承認状態を維持し、後続証拠が前提を否定した場合はGovernanceに従って再reviewする |
| 0-2 用語集 | COMPLETE | `../standards/KOIKI-JavaWeb-FW_Glossary_v0.1.md`に45語、KOIKI-PYFW概念対応8項目、非推奨・限定表現10項目を整理した。§3〜§10のOwner Review、参照link、ArchUnit fixtureの責務整合とテスト13件成功を確認し、2026年8月17日にArchitecture Ownerが承認 | なし | 新語追加または意味変更時に用語集の更新要否と影響する正本・Skill・実装をreviewする |
| 0-3 Walking Skeleton | COMPLETE | 実装計画の全項目が完了し、`validation/walking-skeleton-phase0-completion.md`に最終再検証を記録した | なし | 完了状態を維持し、Walking Skeleton codeを正式成果物へ直接昇格させない |
| 0-4 規約調整 | COMPLETE | V1〜V7がすべてPASS。ArchUnit ruleの実装中に判明した`package-info`等の調整も検証済み | なし | 実装不能なPhase 0規約を残さず、検証証拠から参照可能な状態を維持する |
| 0-5 Phase計画 | PARTIAL | §27.4〜§27.9にPhase 1a〜5の成果物とDoDがある | DoD単位の規模見積りと、依存関係を踏まえた実現可能性の明示判定がない | 各DoDへ規模、依存、主要リスクを付け、各Phaseを実行可能／要分割／保留のいずれかで判定する |
| 0-6 Reference Application仕様 | PARTIAL | §26に題材、module構成、状態遷移、不変条件、Phase別実証範囲がある | 独立した業務仕様、完全な状態遷移条件、権限matrix、代表use case、範囲外、受入条件が確定していない | 業務仕様を独立文書化し、状態遷移、不変条件、権限matrix、代表flow、非目標を一貫してレビューできる状態にする |
| 0-7 Governance | COMPLETE | `governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md`でPrimary MaintainerをOwnerに任命し、代理・継続性、承認記録、四半期review、Phase判定を定めた | なし | Maintainerまたは運営体制の変更時にGovernanceを更新する |
| 0-8 Baseline対応表 | PARTIAL | §8.1にKOIKI／Spring Boot／Javaの対応原則とreleaseごとの更新要求がある | 正本の配置場所、項目、更新契機、更新責任、review方法が定まっていない | 対応表の正本pathと更新手順を定め、初期baselineを記録する。具体的なsupport終了日の公開はPhase 5 DoD 5-3で行う |

現状は、COMPLETE 5件、PARTIAL 3件、NOT STARTED 0件である。したがって、
Phase 0全体は未完了である。

## 3. 正本間の不整合

### 3.1 ADR件数

旧§27.3の0-1は「40本」としていたが、§30のADR tableには43件ある。ADR-018とADR-021は
欠番であり、ADR-001〜045のうちこの2件を除いた43件が有効な行である。

ADRは将来追加され得るため、完了条件を固定本数ではなく、次の意味へ変更した。

> Phase 0で有効な全ADRが記述され、各ADRに「確定」「Phase 0で検証」の区分と承認状態が付いている。

43件の分類と検証証拠を`adr/README.md`へ記録し、Architecture OwnerがAI支援reviewで
各判断とscopeを確認した。2026年8月15日に全件が承認され、0-1を完了と判定した。

### 3.2 文書状態

グランドデザイン v0.2は`Draft for Review`である。Phase 0完了時には、残件を解消した
revisionをレビュー済み状態へ変更し、判定日と判定者を記録する必要がある。

### 3.3 一人projectと代理者

2026年8月14日に、一人project向けGovernanceモデルを採用した。Primary Maintainerを
Architecture Ownerとし、単独運営中は代理者を置かない。Owner不在時は最終判断を停止し、
文書、ADR、検証記録、Git履歴で継続性を確保する。二人目の意思決定可能なMaintainerが
参加した時点で代理者を任命する。これに合わせて§9.4、§9.5、0-7を調整した。

## 4. 残件の実行順序

| 順序 | Work Package | 対象DoD | 成果物候補 | 完了判定への依存 |
|---:|---|---|---|---|
| 1 | Governanceの一人project適合（COMPLETE） | 0-7 | Architecture Governance文書 | ADRとPhase完了承認の前提 |
| 2 | ADR棚卸し・分類（COMPLETE） | 0-1 | ADR registerと§30改訂 | 0-7の承認方式 |
| 3 | 用語集 | 0-2 | `docs/standards/`配下のGlossary | Reference業務仕様の用語統一 |
| 4 | Reference Application業務仕様 | 0-6 | Reference Application Specification | 用語集、ADR-043、§26 |
| 5 | Phase規模・実現可能性評価 | 0-5 | Phase Estimate / Feasibility表 | 0-6を含む全成果物scope |
| 6 | Baseline対応表の運用設計 | 0-8 | `docs/standards/`配下のBaseline表 | §8.1、Phase 5 DoD 5-3 |
| 7 | Phase 0最終判定 | 0-1〜0-8 | グランドデザイン改訂、Closeout更新 | 全Work Package |

0-8はWork Package 2〜5と並行して作業できる。0-6の業務仕様を設計・レビューする際は、
`koiki-business-feature-work` Skillも適用する。

## 5. 先行確定しない事項

次はPhase 0 DoDの残件ではなく、該当Phaseの実装検証で確定する。

- Spring Modulith Named Interfaceと正式version
- Flyway Starterの所属と三階層への一般化
- 非同期eventのLevel 2運用
- MyBatisの詳細実装規約
- REST API、Security、SPAの具体的な実装pattern

## 6. Closeout更新規則

- 成果物を作成しただけではCOMPLETEへ変更しない。完了条件との対応を確認する。
- PARTIALからCOMPLETEへ変更するときは、証拠pathと判定日を記録する。
- DoD本文を変更した場合は、変更理由と旧条件との対応を残す。
- 全項目COMPLETE後に、グランドデザインの文書状態とPhase 0判定を更新する。

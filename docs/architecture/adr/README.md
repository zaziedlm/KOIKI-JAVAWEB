# ADR Register

**棚卸日:** 2026年8月14日

**状態:** Accepted

**承認日:** 2026年8月15日

**Decided by:** Shuichi Kataoka

**Architecture Owner:** Shuichi Kataoka

グランドデザイン v0.2 §30のADRを、Phase 0 DoD 0-1に必要な区分、検証証拠、承認状態と
対応付ける。ADRの決定本文はグランドデザインを正本とし、本registerは状態管理の正本とする。

## 区分

| 区分 | 意味 |
|---|---|
| 確定 | Phase 0のArchitecture Baselineとして採用する設計判断。後続Phaseでの実装完了を意味しない |
| Phase 0で検証 | Walking Skeleton等の実行証拠を必要とし、Phase 0で対象scopeの成立を確認した判断 |

「Phase 0で検証」はADR全体の将来実装を完了したという意味ではない。たとえばSpring Modulithの
Level 2以降、Flyway三階層、非同期event等は、registerで示すPhase 0検証scopeの外である。

全有効ADRは、Architecture Ownerによるreviewを2026年8月15日に完了し、Phase 0 Architecture Baselineとして承認された。

## Register

| ADR | テーマ | 区分 | Phase 0証拠／scope | 承認 |
|---|---|---|---|---|
| ADR-001 | Java Baseline | Phase 0で検証 | `../validation/walking-skeleton-build-foundation.md`（Java 21 bytecode／Java 25 runtime） | ACCEPTED |
| ADR-002 | Spring Boot Version | 確定 | — | ACCEPTED |
| ADR-003 | Build | Phase 0で検証 | `../validation/walking-skeleton-build-foundation.md`（Maven／Parent／BOM） | ACCEPTED |
| ADR-004 | Architecture | Phase 0で検証 | `../validation/walking-skeleton-archunit-distribution.md`、`../validation/walking-skeleton-tier2-practicality.md`（module境界） | ACCEPTED |
| ADR-005 | Module Tooling | Phase 0で検証 | `../validation/walking-skeleton-tier2-practicality.md`（Level 0相当のmodule検証と同期連携。Level 2以降は対象外） | ACCEPTED |
| ADR-006 | UIプロファイル方針 | 確定 | —（API基盤を正本とし、Thymeleaf＋HTMXとSPAは任意の対等profile。具体契約は後続Phaseで検証） | ACCEPTED |
| ADR-007 | Browser Auth | 確定 | —（MVC／一般的なsame-origin SPAではHTTP Sessionが第一標準。stateless SPAを禁止しない） | ACCEPTED |
| ADR-008 | API Auth | 確定 | —（外部API／stateless React SPAではAuthorization headerのBearer JWTが第一標準。Token lifecycleは後続Phaseで検証） | ACCEPTED |
| ADR-009 | Enterprise SSO | 確定 | — | ACCEPTED |
| ADR-010 | Database | 確定 | — | ACCEPTED |
| ADR-011 | Persistence | 確定 | — | ACCEPTED |
| ADR-012 | Migration | Phase 0で検証 | `../validation/walking-skeleton-flyway-two-tier.md`（Flyway二階層） | ACCEPTED |
| ADR-013 | Distribution | 確定 | — | ACCEPTED |
| ADR-014 | Repository | 確定 | — | ACCEPTED |
| ADR-015 | Deployment | Phase 0で検証 | `../validation/walking-skeleton-build-foundation.md`（Executable JAR／Container） | ACCEPTED |
| ADR-016 | Reference Runtime | 確定 | — | ACCEPTED |
| ADR-017 | Support & Upgrade Policy | 確定 | —（最新／直前の2ラインを管理し、OSS修正は対応Spring Bootの公式サポート終了日まで。12か月の移行期間は商用延長サポートを選択肢とする） | ACCEPTED |
| ADR-019 | マルチテナンシー | 確定 | —（v1.0は単一テナント。テナントID、Repository、認可、Flyway等の先行実装は行わない） | ACCEPTED |
| ADR-020 | セッションストア | 確定 | —（Session方式のprofileに適用し、stateless Bearer JWT経路は対象外） | ACCEPTED |
| ADR-022 | モジュール内部構造 | Phase 0で検証 | `../validation/walking-skeleton-archunit-distribution.md`、`../validation/walking-skeleton-tier2-practicality.md`（Tier 1／Tier 2） | ACCEPTED |
| ADR-023 | Tier 2のモデル方針 | Phase 0で検証 | `../validation/walking-skeleton-tier2-practicality.md`（JPA兼用方式。分離方式は対象外） | ACCEPTED |
| ADR-024 | Tier 2のRepository方針 | Phase 0で検証 | `../validation/walking-skeleton-tier2-practicality.md`（`domain.repository`とSpring Data） | ACCEPTED |
| ADR-025 | Domain Event | Phase 0で検証 | `../validation/walking-skeleton-tier2-practicality.md`（同期eventとrollback。非同期は対象外） | ACCEPTED |
| ADR-026 | UIプロファイルの提供順序 | 確定 | —（Phase配置による実装順序であり、公式profileとしての優劣を意味しない） | ACCEPTED |
| ADR-027 | HTMXの同梱と第三者library | 確定 | —（HTMX同梱を確定。`htmx-spring-boot`の実採用はPhase 3開始時に§8.7で再確認し、不適合時は代替実装へ切り替える） | ACCEPTED |
| ADR-028 | Open Session in View | Phase 0で検証 | `../validation/walking-skeleton-tier2-practicality.md`（OSIV無効とEntity露出失敗） | ACCEPTED |
| ADR-029 | Migration Support | 確定 | —（方針の承認。OpenRewrite recipeの正式提供とCI検証はPhase 5） | ACCEPTED |
| ADR-030 | JSON Processing | 確定 | — | ACCEPTED |
| ADR-031 | API Versioning | 確定 | —（Spring Framework 7正式APIに合わせ、mapping例を`path`属性で記載） | ACCEPTED |
| ADR-032 | Resilience | 確定 | —（Spring Framework 7正式APIの`maxRetries`を使用し、最大試行回数3回とする） | ACCEPTED |
| ADR-033 | HTTP Client | 確定 | —（HTTP Service Interfaceを`@ImportHttpServices`で登録するSpring Framework 7正式APIに準拠） | ACCEPTED |
| ADR-034 | Null Safety | Phase 0で検証 | `../validation/walking-skeleton-build-foundation.md`（JSpecify／NullAway） | ACCEPTED |
| ADR-035 | Virtual Threads | 確定 | —（既定無効、Java 25以上でopt-in。JEP 491後も残るpinningと依存ライブラリをPhase 4で検証） | ACCEPTED |
| ADR-036 | レート制御 | 確定 | — | ACCEPTED |
| ADR-037 | キャッシュ | 確定 | —（Caffeineの一時的不整合を許容できる対象とTTLに限定。認可関係は即時失効要件に応じて除外し、分散cacheへの変更時は再検証） | ACCEPTED |
| ADR-038 | read model | 確定 | —（Query契約と`record`は`application.query`が所有し、Outbound Adapterがmaterialize。JPAはclass-based射影に限定） | ACCEPTED |
| ADR-039 | MyBatis | 確定 | —（Boot 4対応StarterをBOM管理するLevel B方針の承認。詳細規約と実装検証はPhase 3末尾～Phase 4） | ACCEPTED |
| ADR-040 | 昇格ポリシーの運用化 | 確定 | —（Reference／Customer／Walking Skeletonの候補をFrameworkへ昇格する場合に適用。Phase 1の定義済み基盤構築は「2案件の実績」の対象外） | ACCEPTED |
| ADR-041 | Public API境界 | Phase 0で検証 | `../validation/walking-skeleton-archunit-distribution.md`（外部consumerを含む） | ACCEPTED |
| ADR-042 | テーブル所有権とFlyway | Phase 0で検証 | `../validation/walking-skeleton-flyway-two-tier.md`（所有者別location／history） | ACCEPTED |
| ADR-043 | Reference Application | 確定 | —（題材と単一モジュラーモノリスの構成方針を承認。Walking Skeletonのexpenseは正式Referenceではなく、DoD 0-6の業務仕様完了は別途判定） | ACCEPTED |
| ADR-044 | Oracle検証戦略 | 確定 | —（Phase 2は固定したOracle Free環境での設計適合smoke。本番Oracleの正式対応はPhase 4のIntegration Baselineで判定） | ACCEPTED |
| ADR-045 | Agent Skillsの設計方針 | Phase 0で検証 | `../validation/walking-skeleton-agent-skills.md`（最小2 Skillの正本、Codex／Claude Code導線、OpenSpecとの責務分離を検証。5本構成の残り3 Skillは後続Phaseで整備） | ACCEPTED |

ADR-018とADR-021は欠番であり、有効ADR数へ含めない。

## Review Log

| 日付 | 対象 | Decision | Decided by |
|---|---|---|---|
| 2026年8月14日 | ADR-001、ADR-003、ADR-015、ADR-034 | ACCEPTED | Shuichi Kataoka |
| 2026年8月14日 | ADR-004、ADR-005、ADR-022、ADR-023、ADR-024、ADR-025 | ACCEPTED | Shuichi Kataoka |
| 2026年8月14日 | ADR-012、ADR-028、ADR-042 | ACCEPTED | Shuichi Kataoka |
| 2026年8月14日 | ADR-041、ADR-045 | ACCEPTED | Shuichi Kataoka |
| 2026年8月14日 | ADR-002、ADR-010、ADR-011、ADR-013、ADR-014、ADR-016 | ACCEPTED | Shuichi Kataoka |
| 2026年8月14日 | ADR-006、ADR-007、ADR-008、ADR-009、ADR-020、ADR-026、ADR-027 | ACCEPTED | Shuichi Kataoka |
| 2026年8月14日 | ADR-030、ADR-031、ADR-032、ADR-033、ADR-036 | ACCEPTED（Spring Framework 7正式APIへの記述修正を含む） | Shuichi Kataoka |
| 2026年8月14日 | ADR-017、ADR-029、ADR-035、ADR-040、ADR-043 | ACCEPTED（サポート期間、Virtual Threads、Framework昇格の適用scope補足を含む） | Shuichi Kataoka |
| 2026年8月15日 | ADR-019、ADR-037、ADR-038、ADR-039、ADR-044 | ACCEPTED（cacheのセキュリティ制約、read modelの依存方向、Oracle検証scopeの補足を含む） | Shuichi Kataoka |

## 集計

| 項目 | 件数 |
|---|---:|
| 有効ADR | 43 |
| 確定 | 28 |
| Phase 0で検証 | 15 |
| PENDING | 0 |
| ACCEPTED | 43 |

## Owner Review Result

Architecture Ownerは次を確認し、全有効ADRをPhase 0 Architecture Baselineとして承認した。

- §30の有効ADR 43件と過不足がない
- 区分が実装済み／未実装という意味に誤読されない
- 「Phase 0で検証」の15件に再現可能な証拠がある
- 検証scope外の後続判断を先行確定していない
- 各決定をPhase 0 Architecture Baselineとして維持できる

後続Phaseで実装証拠が前提を否定した場合は、Architecture Governanceに従って対象ADRを再reviewする。

今後、後から再判断し得る技術判断を追加・変更する場合も、手続きのためにADRを増やすのではなく、
技術判断、理由、状態、再判断条件を残すために使用する。

# koiki-archunit-rules

Customer Applicationへtest dependencyとして配布する、Tooling-ownedかつCustomer-facingなformal testing artifactの
Canonical ownership locationです。配布単位上はFramework成果物に含みますが、production runtime機能ではありません。

独立Maven artifactとして実装済みで、Customer Applicationからtest dependencyとして利用します。
業務moduleのTier / package / dependency、Framework internal参照禁止、MVC境界などを検査します。
利用側は自身のarchitecture testから公開Rule APIを明示的に呼び出し、rule versionをFramework artifactと揃えます。

Ruleの設計根拠は[グランドデザイン](../docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)、
導入判断は[アプリ開発チーム向け引継ぎガイド](../docs/development/application-team-handoff-guide.md)を参照してください。

2026-10-09に[ADR-050](../docs/architecture/adr/ADR-050-module-event-level-selection.md)・[R0規約票](../docs/development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)でmodule単位Level選択の仕様を採用しました。実装・検証は文書source固定／preflight後、現行artifactのAPIはまだ変更していません。

採用仕様は`businessModuleRules(String, Map<String, ModuleEventLevel>)`と`LEVEL_0`／`LEVEL_1`／`LEVEL_2`です。例えば`Map.of("notification", ModuleEventLevel.LEVEL_2)`で当該moduleだけ標準module listenerを許容します。Level 0／1・未指定は共通の現行拒否条件で、同期eventの不存在は検査しません。旧1引数APIと他規則を維持し、新APIのRule 29は同期listener→outbound Adapterの直接依存だけを検査します。間接副作用・runtime・配信の成立は別途確認します。結果受入前に新APIを利用可能と扱わないでください。

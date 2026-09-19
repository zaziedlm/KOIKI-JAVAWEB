# koiki-archunit-rules

Customer Applicationへtest dependencyとして配布する、Tooling-ownedかつCustomer-facingなformal testing artifactの
Canonical ownership locationです。配布単位上はFramework成果物に含みますが、production runtime機能ではありません。

独立Maven artifactとして実装済みで、Customer Applicationからtest dependencyとして利用します。
業務moduleのTier / package / dependency、Framework internal参照禁止、MVC境界などを検査します。
利用側は自身のarchitecture testから公開Rule APIを明示的に呼び出し、rule versionをFramework artifactと揃えます。

Ruleの設計根拠は[グランドデザイン](../docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md)、
導入判断は[アプリ開発チーム向け引継ぎガイド](../docs/development/application-team-handoff-guide.md)を参照してください。

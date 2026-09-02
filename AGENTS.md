# KOIKI-JavaWeb-FW Agent Guidance — Phase 2

このRepositoryでは、`KOIKI-JavaWeb-FW グランドデザイン v0.2`の
`ACCEPTED（Phase 0 Architecture Baseline）`を上位設計とする。

Phase 2 Security Foundationでは、Phase 1a Build FoundationとPhase 1b Runtime Foundationの
承認済み成果物をbaselineとして維持し、次を優先する。

1. Spring標準機能を優先する。
2. Framework / Reference / Customer / Walking SkeletonのOwnershipを混在させない。
3. Walking Skeleton、Reference、Customerまたはtest fixtureのcode、Template、migration SQL、一時Maven座標を正式成果物へ直接昇格させない。
4. Phase 2の変更は`docs/development/KOIKI-JavaWeb-FW_Phase2実行計画_v0.1.md`の承認済みGateとCP境界に従い、P2-A1から順に実装する。
5. P2-A1ではSecurity Starterの最小dependency、default deny、CSRF / Security Header既定、T0 / T1 HarnessおよびPublic API inventoryに限定し、P2-A2以降を先行しない。
6. Authorization Server、SAML、Redis、WebFlux、Oracle、AWS固有Adapter、production Migration、Reference業務またはworkflowを、承認されたCPより前に追加しない。
7. Security acceptance fixture、test user、test route、test key、failure switchを正式artifact、`koiki-testing`またはFramework Public APIへ昇格させない。
8. 未使用の将来Module / Package / Starter / Public APIを先行生成しない。
9. 実装で確認できる事項は文書上の推測より実装検証を優先し、結果を`docs/architecture/validation/`へ記録する。
10. Repository内の作業を位置づけるときは、`docs/agent/skills/koiki-project-overview/SKILL.md`を読む。
11. 業務機能を設計・実装・レビューするときは、加えて`docs/agent/skills/koiki-business-feature-work/SKILL.md`を読む。

`docs/agent/skills/`をKOIKI固有Skillの正本とする。`.agents/skills/`と`.claude/skills/`は、
各エージェントから正本を発見するための薄い導線とし、設計規則を複製しない。

OpenSpecは、Repositoryに採用済みのchangeが存在する場合に限り、変更固有の要求、設計、タスクの
正本として参照できる。Phase 2の必須tooling、Maven build、CIまたはConsumerの前提にはしない。

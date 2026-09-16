# KOIKI-JavaWeb-FW Agent Guidance — Phase 3

このRepositoryでは、`KOIKI-JavaWeb-FW グランドデザイン v0.2`の
`ACCEPTED（Phase 0 Architecture Baseline）`を上位設計とする。

Phase 0、Phase 1a Build Foundation、Phase 1b Runtime Foundationおよび
Phase 2 Security Foundationは`COMPLETE / ACCEPTED`である。Phase 3 Reference Vertical Sliceは、
`docs/development/KOIKI-JavaWeb-FW_Phase3実行計画_v0.1.md`のGate P3-1承認とCP境界に従う。
P3-CP0とP3-A0は`COMPLETE`、P3-A1〜P3-A4およびGate Aは`COMPLETE / ACCEPTED`、P3-B0は
`COMPLETE / OWNER APPROVED`、P3-B1は`COMPLETE`、P3-B2〜P3-B4は
`COMPLETE / OWNER APPROVED`、Gate Bは`COMPLETE / ACCEPTED`、P3-C0〜P3-C2は
`COMPLETE / OWNER APPROVED`である。P3-C3 MyBatis規約fixture / Rule 35〜37はArchitecture Owner判断により
`DEFERRED — MyBatis adoption trigger required`であり、`PersistenceModel.SEPARATED`、Rule 25〜27 / 30〜37、
MyBatis fixtureおよびtest dependencyを追加せず、Rule 8のMyBatis拒否を維持する。P3-C4 Journey / ADR /
Skill / DoD traceは`COMPLETE / OWNER APPROVED`である。
Remote Gate計画は`OWNER APPROVED`であり、RG-4 push、RG-5 draft PRおよびRG-6 required check追加まで
`APPROVED / EXECUTED`である。`Phase 3 Critical Journey E2E`を含むrequired checks 8件を維持する。
workflow rerun、PR ready化、mergeまたはGate C acceptanceは未承認である。
Phase 4、Framework Public API、migrationまたは未承認のremote変更を先行しない。
DoD 3-11の実CI PASSは充足済みであり、Gate C final acceptanceへ入力する。

Phase 3では、承認済みbaselineを維持し、次を優先する。

1. Spring標準機能を優先する。
2. Framework / Reference / Customer / Tooling / Walking SkeletonのOwnershipを混在させない。
3. Walking Skeleton、Reference、Customerまたはtest fixtureのcode、Template、migration SQL、一時Maven座標を正式Framework成果物へ直接昇格させない。
4. P3-A1以降を実行計画の順に進め、Gateまたはblocking reviewを先行しない。
5. P3-CP0は承認記録、Agent導線、start Evidenceに限定し、production code、Public API、Maven module、migration、dependencyまたはworkflowを変更しない。
6. P3-A0で承認済みの有効master検証契約、承認者部門scopeのReference Ownership、Reference migration / FK方針とADR-049を維持する。
7. masterはTier 1 SIMPLE / JPA、expenseはTier 2 RICH / JPA共有モデルとし、単一`koiki-reference-app`内の業務packageとして分離する。別Maven artifactへ分割しない。
8. Spring Modulith Level 1ではcommand整合に同期Domain Eventを使用する。current-valueの有効master確認はADR-049の狭い同期read-only module contractをexpense Port / Adapter経由で利用し、他moduleのApplication、Domain、RepositoryまたはAdapterを直接参照しない。表示専用read modelだけはADR-038 P3-B1 fittingに従い、scopeをSQL内で先に強制し、更新・認可判断・業務不変条件へ利用しないread-only JOINからApplication所有の最終recordを直接materializeしてよい。Level 1のためのruntime依存、transactional / async eventを追加しない。
9. Phase 2のdefault deny、CSRF / Security Header、Identity、Business / Security Audit、Spring Session JDBCの承認済み契約を再利用し、弱めない。業務属性をFramework Identityへ追加しない。
10. MVC / Thymeleaf / HTMXとRESTは同じApplication Use Caseを利用するが、Controller、Form、View DTO、REST DTOを共有しない。Domain Model / JPA Entityを外部へ露出しない。server-side UIはThymeleaf HTMLを主軸とし、HTMXは効果と検証可能性を説明できる操作だけに選択適用する。
11. APIと自動testを回帰の主軸とし、操作面が成立するP3-B2以降は実browserでの目視・手動操作とlog / Audit / DB突合を組み合わせる。
12. 個別のPublic API、property、migration SQL、Starter、外部library、cacheまたはREST契約は、実行計画が指定するblocking reviewとEvidenceより前に固定・追加しない。
13. Project Template、SPA、Spring Modulith Level 2、MyBatis accounting、Oracle、AWS固有Adapter、Authorization Server、SAML、Redis、WebFluxおよびPhase 5成果物を先行しない。
14. Security acceptance fixture、test user、test route、test key、failure switch、browser harnessを正式artifact、`koiki-testing`、Project TemplateまたはFramework Public APIへ自動昇格させない。
15. 実装で確認できる事項は文書上の推測より実装検証を優先し、結果を`docs/architecture/validation/`へ記録する。
16. Ownerの個別承認なしにremote push / PR / merge、ruleset変更、workflow dispatchまたはsnapshot publishを行わない。
17. Repository内の作業を位置づけるときは、`docs/agent/skills/koiki-project-overview/SKILL.md`を読む。
18. 業務機能を設計・実装・レビューするときは、加えて`docs/agent/skills/koiki-business-feature-work/SKILL.md`を読む。

`docs/agent/skills/`をKOIKI固有Skillの正本とする。`.agents/skills/`と`.claude/skills/`は、
各エージェントから正本を発見するための薄い導線とし、設計規則を複製しない。

OpenSpecは、Repositoryに採用済みのchangeが存在する場合に限り、変更固有の要求、設計、タスクの
正本として参照できる。Phase 3の必須tooling、Maven build、CIまたはConsumerの前提にはしない。

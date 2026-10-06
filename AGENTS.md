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
DoD 3-11の実CI PASS、Gate CおよびPhase 3全体は`COMPLETE / ACCEPTED`である。
PR #35はmerge commit `aa83fa578b5f689ce72e2a2540ad3ec2b659c083`として`main`へmerge済みであり、
merge後CI 7 / 7 jobsとJava Runtime Compatibility 2 / 2 jobsは成功した。Phase 3 remote closeoutは完了している。
post-merge closeoutでFramework採用実案件の始動とPhase 4作業の一部移管見込みが明らかになったため、
Phase 3を再オープンせず、Pre-Phase 4 Adoption Readiness（P4-AR）をtransition Gateとして追加した。
P4-AR0とAR-1〜AR-7は`COMPLETE / OWNER APPROVED`である。承認記録をRepositoryへ反映してから、同期済みclean `main`で
P4-AR1以降のFramework検証を開始する。Phase 4開始は未承認である。
Phase 4全体開始は未承認のまま維持する。Gate P4-Fの設置・限定開始と必要正本改訂が
Architecture Ownerにより明示承認された場合だけ、承認記録のsource、Ownership、対象、
検証上限と停止条件に従ってS1初回Reference基盤を作成・検証できる。初回はnotificationの
許可・append-only消費の保存／認可／Auditと条件付き登録、Reference-owned追加migrationに限定する。
通常起動では無効とし、既存identity／master／expenseとSecurityを保護する。
Web／CLI受付、sender、listener、復旧runner、publication schema、Modulith runtime、
Framework API／Rules／依存変更は含めない。後段は必要reviewと個別開始判断を経る。
この限定条件の正本改訂は2026-10-06にOwner承認済みである。改訂承認はGate設置／限定判定・
実行開始の承認ではない。[承認記録](docs/development/phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md#9-正本改訂のowner承認反映記録2026-10-06)と
[正式開始票](docs/development/phase4-s1-reference-foundation-formal-start-review-20261006.md)に従う。
後続判断でGate P4-Fを設置し、ST-C初回Reference保存・認可・Audit基盤だけを
`APPROVE LIMITED START`とした（2026-10-06、[Gate判定記録](docs/development/phase4-s1-reference-foundation-execution-review-20261006.md#7-gate-p4-f設置初回限定判定2026-10-06)）。
正式作成・検証開始は別判断であり、文書source固定・承認済みpreflight成立前に開始しない。
後続の[正式開始承認記録](docs/development/phase4-s1-reference-foundation-execution-review-20261006.md#8-source固定preflight条件付き正式開始承認2026-10-06)で、
文書commit・clean source固定後のpreflightと、成立時の採用済み初回code／test／V4／検証専用設定の作成・
Maven／隔離Docker検証をOwner承認済み（2026-10-06）。条件未成立で作成・検証を先行しない。
P4-ARではFramework本体だけでなく、Customer-like Consumer、package済みReference Application、検証Toolingおよび
Developer Journeyを受渡し候補として棚卸しし、業務アプリ開発チームの受入側視点でbuild / run / operation / diagnosisを実証する。
正式な受渡し対象はP4-AR Evidenceを入力とする見直し後Phase 4準備で判断し、Project TemplateはPhase 5境界を維持する。
Framework Public API、migrationまたは未承認のremote変更を先行しない。

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
    Gate P4-Fで個別承認された初回Reference基盤には上記限定条件を適用する。Spring Modulith Level 2そのもの、通知・復旧の接続、MyBatis等の対象外開始制限は維持する。
14. Security acceptance fixture、test user、test route、test key、failure switch、browser harnessを正式artifact、`koiki-testing`、Project TemplateまたはFramework Public APIへ自動昇格させない。
15. 実装で確認できる事項は文書上の推測より実装検証を優先し、結果を`docs/architecture/validation/`へ記録する。
16. Ownerの個別承認なしにremote push / PR / merge、ruleset変更、workflow dispatchまたはsnapshot publishを行わない。
17. Repository内の作業を位置づけるときは、`docs/agent/skills/koiki-project-overview/SKILL.md`を読む。
18. 業務機能を設計・実装・レビューするときは、加えて`docs/agent/skills/koiki-business-feature-work/SKILL.md`を読む。

## 検証環境の権限エラーと承認済み手順

Architecture Ownerは、今後のAIセッションで検証実行が権限エラーに妨げられた場合、
承認済みの解決手順をAgent guidanceと関連Skillへ記録することを許可している。
記録前に、実行を許可された作業範囲、失敗した操作とエラー、承認済みの再実行方法、
実際の検証結果を確認する。汎用的な判断手順はここへ、作業固有の条件と結果は
`docs/architecture/validation/`へ記録し、Skillには正本への導線だけを置く。
この記録許可は、新しい権限回避策や対象外の操作を実行する許可ではない。
実行環境が権限付き実行の承認を要求する場合はその手順に従い、拒否された操作を迂回しない。

### ローカルDocker検証

Architecture Ownerは、Repository内の承認済み検証に必要なローカルDocker診断と
Testcontainers実行について、Docker named pipeへ接続できる権限付き実行を許可している。
この許可には、read-onlyの`docker version` / `docker info` / `docker ps` / `docker images`と、
承認済みTooling fixtureのMaven test / verifyが使う使い捨てcontainerの起動・停止を含む。
現時点の対象はP4-PL2の`build-support/phase4-level2-verification/`である。
2026-10-06の[S1初回正式開始承認](docs/development/phase4-s1-reference-foundation-execution-review-20261006.md#8-source固定preflight条件付き正式開始承認2026-10-06)により、
文書commit・clean source固定後のpreflightと、成立後の`koiki-reference-app`および
`build-support/reference-e2e-verification/`の承認済み隔離DB検証・既存回帰に必要な
使い捨てcontainerの起動・停止も対象に含む。実行環境の権限付き承認手順と、採用済み資源・
cleanup・停止条件に従う。既存P4-PL2許可の流用ではなく、この個別承認に基づく対象追加である。

通常のsandboxで`npipe:////./pipe/docker_engine`への接続を拒否された場合は、
それだけでRancher DesktopまたはDocker Engineの障害と判定しない。
対象作業の承認範囲を確認し、実行環境が提供する権限付き実行・承認手順を使って
最小限の同じ診断または検証を再試行する。Mavenの`~/.m2`書込み拒否も同様に扱う。
権限付き実行で`docker version`のServer応答と検証結果を確認し、Evidenceへ記録する。
実行環境側で権限付き実行を拒否された場合は迂回せず、その理由を報告する。

この記載はOSやAI実行環境の権限設定を変更しない。また、未承認のproduction実装、
任意のcontainer操作、image配布、remote操作、Gate通過を許可するものではない。
検証範囲が変わる場合は対応するOwner判断に従う。

`docs/agent/skills/`をKOIKI固有Skillの正本とする。`.agents/skills/`と`.claude/skills/`は、
各エージェントから正本を発見するための薄い導線とし、設計規則を複製しない。

OpenSpecは、Repositoryに採用済みのchangeが存在する場合に限り、変更固有の要求、設計、タスクの
正本として参照できる。Phase 3の必須tooling、Maven build、CIまたはConsumerの前提にはしない。

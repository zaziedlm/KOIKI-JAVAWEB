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
2026-10-07の[scope／TTL供給の限定開始承認](docs/development/phase4-s1-managed-scope-ttl-limited-start-review-20261007.md#8-owner承認条件付き限定開始2026-10-07)により、
Reference-ownedの外部管理設定・起動固定snapshotによるscope／TTL Adapter、発行直前の期間再検査、
条件付き登録と当該test／Evidenceを、文書commit・clean source固定・採用済みpreflight成立後に作成・検証できる。
検証は新規5 class／40 case候補、初回55件と既存25 class／99件・package済みE2Eに限定し、
同票の資源・時間・作業量・cleanup・停止条件に従う。運用target／停止／provider証拠は未接続時拒否を維持する。
実設定配備・運用操作、Web／CLI受付・worker認証／委譲、sender／listener／runner、publication／Modulith runtime、
Framework API／Rules／依存・remoteへ拡張しない。local commitはOwnerへの事前確認またはOwner自身の操作とする。
2026-10-08の[対象・運用証拠ハーネスの限定開始承認](docs/development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#9-owner承認条件付き限定開始2026-10-08)により、
test所有の可変現在対象・停止制御model・provider観測stub・証拠台帳と現行Portへの接続検証、
既存Harnessの必要最小変更・当該Evidenceを、文書commit・clean source固定・preflight成立後に作成・検証できる。
新規5 class／48 invocation候補、scope／TTL40件・初回55件・既存99件＋E2Eと、同票の資源・時間・
作業量・cleanup・停止条件に限定する。D07の隔離DB内Audit権限一時REVOKE／finally復元を含む。
本番main／Port／schema／grant定義／依存／通常構成は変更しない。同一環境・publicationの未閉鎖permit一意制約を維持し、歴史対象閉鎖・別publicationにまたがる論理通知の抑止・
照合後競合の未達は観測・記録だけで、実運用保証にしない。実運用Adapter・停止操作・provider通信、
受付／worker／送信、publication／Modulith、Framework／CI／remote・Phase 4全体への開始承認ではない。
preflightで判明したD11の前提誤りは同開始票§10へ記録し、§11でOwner訂正承認済み（2026-10-08）。
訂正・承認文書commit後のclean source再固定と成立済みpreflightからのsource／環境差分確認を経て、
訂正後D11を含む承認済みtest作成・48件検証へ進む。同じ範囲の開始承認を再要求しない。
2026-10-08の[B-1読取接続の条件付き限定開始承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#11-owner承認条件付き限定開始2026-10-08)により、
承認文書commit・clean source固定後のpreflightと、成立時のTooling test所有の読取契約／collector／process台帳／
独立schema証拠保管／SELECT専用JDBC reader、専用test SQL・新規6 class／52件・Evidenceを作成・検証できる。
対象は`build-support/phase4-level2-verification/`。本票の使い捨てDB／専用schema・role、有限fixture準備、当該子JVMの
起動・イベント発行・stub受理・当該終了待機・観測／証拠保存・cleanupを限定副作用として含む。
Ownerが限定環境管理責任を兼務し、Agentへ開始条件成立後の本票内操作を委任する。
選択PL2回帰6件・Reference関連143件・既存99件＋E2E、資源・時間・作業量・raw／再実行上限は同票に従う。
Reference／Tooling main・既存test／migration・POM／依存・Root Reactor・Security・通常構成・CIは変更しない。
B-2のReference Adapter／肯定Port登録・許可操作、外部送信／復旧runner／Reference Level 2・Phase 4全体・
DoD／正式受渡し・remoteへ拡張せず、D11／D12と網羅性懸念・未達を保持する。副Agentは使用しない。
local commitはOwner操作または操作前のOwner確認とし、実装結果の受入は別判断とする。
2026-10-08の[同開始票§13の訂正承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#13-12訂正のowner承認2026-10-08)により、
既存test-only依存を含む`jdbc,s1-contract,s1-web` profile補足と、Toolingの`PublicationRecoveryTest.java`／
`ProcessCrashRecoveryIT.java`の最小資源設定・launch差分、test-only `B1ResourceLimits.java`1件追加を許可する。
既存test変更禁止の例外はこの2ファイルの明示選択時の資源設定だけとし、assertion・件数・通常起動条件を維持する。
訂正・承認文書commitとclean source再固定・source／環境差分確認後に、実効制限確認に必要な3ファイルを
作成・限定検証する。制限成立前に新規52件のハーネス作成・検証を開始しない。
検証集合・資源／時間／作業量・raw上限・停止条件・main／POM／migration等の除外は維持し、同じ訂正承認を再要求しない。
2026-10-08の[同開始票§15のruntime profile訂正承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#15-14訂正のowner承認2026-10-08)により、
`jdbc,s1-contract,s1-web`はoffline test-compile専用とし、既存4件と選択IT2件のruntimeは`jdbc`単独とする。
§13の文書commit・clean source固定済み`fad5ded`を基点に、今回の承認記録・source／環境差分確認後、
compile済みtest-classesのSurefire／Failsafe直接goalで既存4件の正常化・実効制限を確認し、成立時に選択IT2件へ進む。
Tooling JARもjdbcのmain依存でpackage・確認する。既存失敗を保全し、code差分3件・検証集合・上限・停止条件を維持する。
制限成立前の新規52件、main／POM／依存／migration追加変更へ拡張せず、同じ訂正承認を再要求しない。
結果受入・B-2／実運用／remoteは別判断とする。
2026-10-08の[同開始票§17の診断保全承認](docs/development/phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md#17-16診断保全のowner承認2026-10-08)により、
test-only `B1ResourceLimits.java`の失敗診断保全と選択IT2件の限定再検証を許可する。
credential／接続情報を除去した有限量の子log・終了状態を既存cleanup前に保存し、子終了・元log cleanupを維持する。
これまでの訂正経緯の点検は文書・差分・実行記録の照合だけとし、追加原因修正は別判断とする。
assertion・件数・資源上限・停止条件を維持し、新規52件・結果受入・実運用・remoteへ拡張しない。
同開始票§18へOwnerの点検記録確認と調査続行を記録した。helperの診断条件を変えた限定確認でも回帰失敗したため、
追加実行・原因修正は停止し、残る保持接続の診断追加は同票§19のOwner判断を経る。
2026-10-08の同開始票§20で§19採用をOwner承認済み。helperのpackage logger・起動中1回のSELECT診断と
選択IT2件1回を実施できる。保持元の完全特定を目標とせず、得られる実値・DB状態を実装判断へ整理する。
資源／migration／業務assertion・件数を維持し、診断反復・追加原因探索・原因修正を自動で先行しない。
同開始票§22で§21の子pool設定供給訂正をOwner承認済み。helperの明示選択launchだけでHikari数値を
canonical command propertyへ揃え、source／環境差分確認・compile後に選択IT2件を1回確認する。
数値・秘密のenv供給・transaction／migration／業務assertion・件数を維持し、起動初期からの実値を確認する。
不一致・回帰失敗・上限超・cleanup失敗では停止し、追加修正・新規52件・結果受入・remoteへ拡張しない。
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
2026-10-07のscope／TTL限定開始票§8で承認された条件成立後のMaven／隔離Docker検証も、
同票の対象・資源・cleanup・停止条件と実行環境の権限付き承認手順に従う。
2026-10-08の対象・運用証拠ハーネス限定開始票§9で承認された条件成立後のMaven／隔離Docker検証も、
同票の対象・資源・cleanup・停止条件と実行環境の権限付き承認手順に従う。
2026-10-08のB-1読取接続限定開始票§11で承認された条件成立後のMaven／隔離Docker・当該子JVM検証も、
同票の専用schema／role・fixture準備副作用、検証集合・上限・cleanup・停止条件に限定して対象に含む。
文書commit・clean source固定前に実行せず、環境の権限付き承認要求を迂回しない。

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

# S1 R1：Reference正常非同期経路・必要最小限の開始レビュー案（2026-10-12）

**状態：OWNER APPROVED / 文書source固定・clean source・preflight条件付きR1限定開始（2026-10-12）。** Ownerは本票を指定して「確認し、これを承認します」と明示した。採用された条件と実施前の残条件は§8に記録する。[R0区切り](phase4-s1-r0-closeout-remaining-tasks-handoff-20261010.md)を入力とし、[旧開始草案](phase4-s1-r0-reference-async-start-review-20261009.md)の実行集合・予算・開始条件を本票へ置き換える。旧草案の20件／90分／S1専用64件一括再検証は実行指示として使わない。

作成基点：`feature/phase4-s1-reference-foundation` / `5f31d41d462687adfb5136d29ed94b88938c70f1`、worktree clean。remote追跡情報との差は0／0（fetchによるlive照合ではない）。OwnerはR0記録のcommit・push済みを報告した。R0規約・旧API互換性・最小検証は受入済み。R1用のbranch・source固定と個別開始はこれから行う。

## 1 出口と変更の責任

R1の出口は「選択したpackage済みReferenceで、既存expense承認から値event・publication・notification・ローカルprovider stub受理・正常completionまで照合でき、通常構成を保護できる」である。停止・再送・復旧、実provider配信、DoD全体を出口に含めない。

| 設計判断 | R1で採用する案 |
|---|---|
| Ownership／module／Tier | Reference expenseの既存RICH／JPA SHAREDを維持。値eventはexpense所有、受信・送信調整はnotification所有。既存notification permitモデル・保存契約は変更しない |
| 責務配置 | approveだけに発行hook。受信は`notification.adapter.inbound.event`、調整はApplication、HTTP接続はOutbound Adapter。共通mutationの他操作で発行しない |
| 公開境界 | Entity・他moduleのApplication／Repositoryを共有しない。expense所有の不変な値eventだけを渡す。既存MVC／REST signatureとSecurityを維持 |
| persistence／read model | expenseはJPA。publicationは同じDataSource・発行元transactionに参加するJDBC registryを採用候補とし、Reference-owned schema／Flywayで管理。新しい表示read modelなし |
| async主体／Audit | 元requestのSecurityContext伝播を前提にしない。既存承認Auditを維持し、event／受理の相関を記録。追加Auditに新主体表現が必要なら既存契約へ適合するか確認し、不適合なら停止。主体を捏造しない |
| 静的規約 | 下記の受入済みAPIを使用。Rules改訂・負例fixtureの再作成なし |

```java
KoikiArchitectureRules.businessModuleRules(
    "org.koikifw.reference", Map.of("notification", ModuleEventLevel.LEVEL_2))
    .check(classes);
```

`ReferenceArchitectureTest`のbusiness規約2引数化だけを行い、identityのFramework ownership検査・全違反0件を維持する。master／expense／identityは未指定の共通Level 0／1拒否条件を維持。静的Level選択をruntimeの耐久性・冪等性保証へ読み替えない。

## 2 採用・設定・保存の限定契約

旧草案§2〜5のReference限定構成を次の条件で採用する案。Framework API／Rules／Starter／BOM／Parent、Root Reactor、Security、CIを変更しない。

- `spring-modulith-events-api`はReference compile依存、JDBC starterはReference専用Maven profile `s1-reference-async`。versionは実sourceのBOM管理値`2.1.1`を維持。別versionの追加・自動更新なし。
- 起動設定候補`koiki.reference.notification.async.enabled`は既定false。profileと設定の両方で選択する。無効時はevent発行・listener／executor・registry／追加migration・送信を抑止し、既存auto-configuration除外と合成する。profileだけでは有効化しない。
- Reference-owned schema `kkref_notification_async`、追加location `db/migration/kkref-notification-async`、候補`V5__create_notification_async_publications.sql`。通常propertiesとV1〜V4のbytesを維持。V4を自動有効化せず、選択済みV4とのlocation併用も確認する。
- registryはUPDATE完了方式。libraryの自動DDL・再起動時再公開を無効にし、registry対象を標準`ApplicationModuleListener`に限定する。scheduler／復旧API／purgeを登録しない。
- keyは`expense-approved:v1:{expenseRequestId}:{approvalVersion}:applicant`。flush後の確定versionを使用。event IDは発行元transactionで一度だけ生成しpayloadへ保存。publication ID／attempt／process世代／traceをkeyへ含めない。
- 同key同payloadはstubで原子的に初回受理1件、その後は同じ受理ID。異payloadはconflictで初回記録不変。payloadは識別子・確定値のみ、canonical表現を固定し再要求の観測項目をhashへ混ぜない。
- provider stubはTooling test所有、独立transaction／専用schema／role、loopback動的port。Reference Adapterのendpointは数値loopbackに限定しredirectを追わない。stub・故障switch・受理台帳をReference JARへ含めない。stub受理は実配信保証ではない。

現行sourceでapproveは権限・scope・version→状態変更→flush→Business Auditを同じtransactionで行う。追加hookは承認Audit成立後・transaction終了前。JPAとpublicationのtransaction参加設定と通常結果をR1で照合するが、publication保存失敗時の一括rollback、通知失敗後の承認保持、拒否時通知なしのasync有効構成実証はR2へ残す。正常結果だけで原子性の全負例をPASSにしない。

公式[2.1.1設定資料](https://github.com/spring-projects/spring-modulith/blob/2.1.1/src/docs/antora/modules/ROOT/pages/appendix.adoc)のschema設定・自動DDL抑止・completion方式・再起動再公開設定を参照した（2026-10-12）。Context7の結果にはmain文書も含まれるため、実装前に端末の**2.1.1 JAR／POM／DDL／auto-configuration metadataをhash付きで照合**する。BOM管理はReference上のruntime互換性証明ではなく、未成立条件はpreflight／起動時に検出して停止する。

## 3 必要ケースと残す回帰

新規は**4 class／9 invocation候補**。件数を合格率の分母にせず、下記の目的・assertion・fresh XMLの対応で判定する。通常rootの新規はK01〜02とD03の3件。D01〜02／CはRoot Reactor外のE2E Toolingで明示選択する。

| ID／配置 | 必須assertionと必要性 |
|---|---|
| K01／Reference値event test | 必須識別子・確定version等の欠落／不正値を拒否し、Entity・可変参照・秘密をpayloadへ含めない |
| K02／同class | 同事象のkey／canonical payload安定、別expense・別承認versionの識別。旧K03〜04を同じ契約確認へ集約 |
| D01／Tooling provider契約 test | 独立stubへHTTP初回＋同key同payload再要求で受理record総数1、同じ受理ID。実Reference Adapter経由の正常送信はC04で確認。停止復旧の実証ではない |
| D02／同class | 同key異payload拒否・初回record不変・成功扱いなし |
| D03／Reference endpoint契約 test | Reference Adapterの非loopback endpointを通信前に拒否し、redirectによる外部接続を許さない。必要なHTTP応答fixtureはtest内だけに置く |
| C01／既存Critical Journey 1件 | 通常artifact／async無効。既存Bearer・Session・業務・Security・Audit・DB・log assertionを維持し、追加schemaなしのassertionだけ補足。既存1件として数える |
| C02／Tooling package test | async artifact／async無効。最小の既存HTTP承認を行い、承認・Audit成立、追加schema・publication・stub送信なし。C01のbrowser全操作を複製しない |
| C03／同class | 通常artifact／async有効をstarter不足で起動初期拒否。追加migration／送信・業務操作に進まない。既存通常schemaも変更しない |
| C04／同class | async artifact／async有効、V4無効。既存HTTP認証・権限で2つの異なる申請者／expenseを承認し、それぞれのAudit／event／publication／key／独立stub受理／COMPLETEDを一致照合。取り違えなし、V4非自動選択も確認 |
| C05／同class | async artifact／async有効、V4も明示選択したfresh DB。V4＋V5 migration共存・正常通知1件と既存permit関連source不変更を照合。未接続拒否の受入済み契約を保持し、consume／closeは操作しない。新たなpermit拒否検証のPASSは主張しない |

C01は既存、残るK2＋D3＋C4＝新規9。C04は旧P01／P02を同じ実processで照合する。C01／C02が旧P03／P04を兼ねる。旧C01〜04の二重実行と旧Architecture負例A01〜04の再作成を外す。C05は既存V4と追加V5の合成に対する具体的な必要ケースとして残す。

| 既存集合 | 今回の扱い |
|---|---|
| R0 Rules107・PL2既存4・旧Consumer | 保存結果を維持。単独再実行・互換性fixture再作成なし。Rules107が最終rootで実行されても独立成果へ二重加算しない |
| ReferenceArchitectureTest 2件・ExpenseApplicationServiceMethodSecurityTest 3件 | 新規K2・D03と合わせ、配線・規約の早期確認8 invocationを1回。最終rootとの重複を台帳で区別 |
| ExpensePostgreSqlIntegrationTest 6件・ExpenseAuditRollbackPostgreSqlIntegrationTest 1件・DepartmentDeactivationPostgreSqlIntegrationTest | 認可／scope／version／承認Audit／同期vetoの影響回帰。最終通常root内で確認し、別実行を一律追加しない |
| 通常root | 実装差分確定後の最終統合で1回。基点389＝297実行＋92skip、新規root3だけなら392＝300実行＋92skip候補。fresh XMLとcase名で実集計。予期しないskip／失敗はPASSにしない |
| S1専用64／B2 process28・B1専用 | 今回は再実行しない。source／設定の不変更と通常build分離を照合。直接影響が判明した場合だけ必要部分を別判断 |

通常rootは全変更の統合確認に使い、各修正のたびに反復しない。R0のXML／JARをR1実行証拠へ流用しない。新sourceの通常／async JAR・main内容・nested依存・migrationと入力hashを別固定する。rootの通常JARとasync profile JARを同じartifactとして扱わない。

## 4 許容差分・正本整合

code／test／POM／SQL／metadataは合計28 file以内、文書は12 file以内を提案する。

| 対象 | 有限差分 |
|---|---|
| Reference POM・expense main | 限定依存／profile、値event・発行Port／Adapter／設定（新規4以内）、approve hook・constructor配線 |
| notification main | listener・Application・provider Port／HTTP Adapter／条件付き設定（新規5以内）。既存permit code不変更 |
| 起動設定／resource | gate新規1、登録metadata1、選択時設定1、V5 SQL1。通常properties・既存migration・Security・ReferenceApplication不変更 |
| Reference test | 値event・endpoint test新規2 class／3件、既存method-securityのconstructor配線1 file、ArchitectureのAPI切替1 file。追加配線差分が必要なら対象一覧を点検し最大3 file内に限定 |
| E2E Tooling test | provider契約・package test新規2 class／6件、共通stub／process／観測helper合計2 file以内。既存Critical Journeyは追加schema不在assertionの補足だけ。Tooling POM・既存assertion不変更。Reference型へのcompile依存を新設せず、provider契約は独立stubへのHTTPで検証する |
| 正本／文書 | Owner承認後に旧草案§8の対象文書・Reference限定ADRへ限定例外と本票への導線を反映。受入済みR0 Evidenceは上書きしない |

変更path・理由・旧bytes保持・通常無効条件を実装時のmanifestへ記録する。限度内の命名／private型分割は記録して進め、対象外module・public契約・依存version・schema／Security拡大は停止してOwner判断へ戻す。

## 5 実行前の順序と環境の最小準備

1. Ownerが本票のReference限定採用・必要正本改訂・R1条件付き限定開始を明示判断する。資料作成やR0受入だけで開始しない。
2. 採用した正本・ADR・承認記録を現ブランチで反映し、Ownerのcommit承認／操作で固定する。その固定commitから`feature/phase4-s1-reference-async-r1`を作成する案。新branch作成時点で起点HEADを記録する。main merge／push／PRは別判断。
3. clean source・承認対象・source hash、端末JDK21／Maven・依存JAR／DDLを確認する。不足artifact／不可欠なtransitiveは有限一覧を先に出し、取得承認なしにdownloadしない。
4. R1専用run／tmpを作り、Git対象外の旧R0 raw／runnerを混用しない。Docker Server、既存Java PID＋creation identity／container、空き容量・portを一度確認し、今回所有のDB・子JVM・観測接続を記録する。
5. 既に成立した所有Job方式を必要範囲で再利用し、所有子の起動・正常終了・baseline保持を短いjava-version probe1回で確認する。74例等のpure網羅は再実行・拡張しない。権限拒否は実行環境の承認手順へ戻し、別の権限回避・全Java停止へ進まない。
6. preflight成立後にだけ対象code／testを作成し、早期8件→最終通常root→artifact固定・Tooling D／Cの直列検証→cleanup／Evidence集約へ進む。通常root成功後のJARをC01／C03へ使い、別にasync profile JARをpackageする。新JAR作成時はtest二重実行を避け、どの集合を省略したか記録する。

preflightでは環境実行可否を確認し、未実装のexecutor／poolは作成後の起動で実効値を確認する。無効構成は登録条件・auto-configuration除外metadataの静的照合と、package実起動でのDDL／送信なしを合わせて判定する。logに登録名がないことだけで全bean不在を断定しない。bean全件列挙・汎用監視の作成は出口条件に加えず、条件不一致や予期しない副作用が判明した場合だけ停止し必要な診断を提示する。

## 6 有限予算・資源・停止／cleanupの案

旧R0残予算は閉じたまま。R1の検証・環境管理に**新規70分**を提案し、文書編集とOwner待ちは含めない。

| 段階 | 上限／初回回数 |
|---|---|
| preflight／最小準備 | 8分、各確認1回。java-version probe1回 |
| compile・早期K／D03／Architecture／method-security 8件 | 12分、選択集合1回 |
| 最終通常root | 10分、1回。他集合失敗中にrootへ逃げない |
| 通常／async artifact照合・async package | 10分、各必要package1回。通常JARはroot成果を保持 |
| Tooling D2＋C02〜05、既存C01 | 20分、各集合1回、直列。試行総数とXML invocationを区別 |
| Evidence／source／artifact照合 | 5分 |
| cleanup予約 | 5分、検証へ流用しない |

全phase最大同時JVM4、PG1。通常root／既存Critical Journeyは**Ryuk最大2／DB client14**。新規Tooling D／Cは**Ryuk最大1／client8**（Reference pool4＋observer最大2＋stub最大1＋準備最大1）。phaseを直列化し、既存E2Eのpool4／min idle1を変更しない。新規async poolは4／min idle0、executor1 thread／queue16、HTTP接続2秒・応答5秒、completion待機30秒・新規process起動120秒・正常終了30秒を上限候補とする。既存harnessにこれより短い期限がある場合は既存値を延長しない。

空きmemory8 GiB／disk10 GiB以上、raw合計100 MiB／run全体2 GiB／各子log10 MiB以内。今回所有JVM heap768 MiB以内、PG memory1 GiB／CPU1／max_connections16。rootは既存pool・assertionを変えず、実効資源が上限不成立なら停止する。DB観測は明示database・SELECT専用role・TCP起動確認、失敗時は秘密を除去した有限stderrを保全する。有限sampleであり連続保証ではない。

追加再実行は0回を基本とする。同じ範囲のharness訂正も、まず失敗原因・差分・残予算・必要な追加回数を一つの訂正案へまとめる。自動root反復や追加pure網羅を行わない。Ownerが訂正範囲／追加実行を明示委任した場合はその条件で継続する。残時間だけで再試行しない。

今回のDB／schema／role・loopback stub・package子JVM・browserとcleanupだけを有限副作用候補とする。他sessionのJava・既存container・本番roleには触れない。所有PID＋creation／Jobを確認し正常終了を先行、未終了時の限定終了もOwner委任範囲に従う。元baseline保持・owned Job空・container／接続終了を照合して結果を固定し、失敗rawを後続runで上書きしない。cleanup不成立は工程停止、受入PASSにしない。

## 7 網羅を拡げる条件と今回の判断欄

| 追加の必要性が生じる具体条件 | 対応時期 |
|---|---|
| key／payload／endpointや必要設定の枝が本票で覆えず、本体成立を判定できない | 実装前または最初の検証失敗時に、その枝だけ有限追加案 |
| gate／location変更が既存permit登録・S1保存・認可に直接影響する | 影響caseだけ追加案。S1／B2全件へ自動拡張しない |
| V5適用後に未適用V4を後から選択する運用が必要になる | R1のfresh DB共存とは別条件。Flyway履歴順序のreviewへ戻し、out-of-order設定を自動変更しない |
| provider失敗・rollback・応答喪失／停止位置・旧worker生存など | R2／R3／R4の各開始票で必要ケースを確定 |
| 所有process／DB観測が曖昧で上限・cleanupを証明できない | 本体を停止し必要な診断だけを提示。網羅監視の一般化を進捗へ加算しない |

R1結果でE01／E08の正常枝と通常保護だけを報告する。R2失敗分離、R3保護モデル選択と停止／復旧、R4consume／close・認可／Audit、R5保持／purge／traceは未開始。D11／D12・B-C01〜10・DoD 4-1〜4-5／4-12・正式受渡し・Phase 4全体は未達を維持する。

**Owner判断欄：承認済み（2026-10-12、§8）。** 対象は§1〜2のReference限定契約、§3の必要集合、§4〜6の差分・正本整合・source／branch順序・有限開始条件・環境操作委任。local commitとremoteは別判断。草案作成時に行ったのはread-only確認と文書作成だけで、code／POM／SQL変更、Maven／Docker／test、branch作成・commit／remoteは実行していない。

## 8 Owner承認反映と文書source固定の対象（2026-10-12）

Ownerの承認により、Reference限定契約・新規4 class／9 invocation候補・既存影響回帰／最終root・70分／cleanup5分予約・資源／停止条件・今回所有の有限環境操作を採用した。設置済みGate P4-FへR1だけを追加限定判定する。承認は正本／ADR／承認文書commit→新R1 branchでclean source固定→preflight成立→限定作成検証の順序を維持し、条件未成立の実装開始を許可しない。全ての検証結果はまだ未成立である。

§5.2に従い、今回のcommit候補は次の**文書12 fileだけ**。新規ADRは[ADR-051](../architecture/adr/ADR-051-reference-notification-async-r1.md)。既存ADR台帳は変更せず、本票・Gate・Skillから採用判断へ直接リンクする。

| path | 固定する内容 |
|---|---|
| `AGENTS.md` | R1の条件付き限定開始・環境操作・停止条件への導線 |
| `docs/agent/skills/koiki-project-overview/SKILL.md` | 現在のR1承認位置とsource／preflight条件 |
| `docs/agent/skills/koiki-business-feature-work/SKILL.md` | Reference限定の値event／listener接続と保証境界 |
| `docs/architecture/adr/ADR-051-reference-notification-async-r1.md` | 限定JDBC store／二段階選択／key／migration判断 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4_P4-F限定開始Gate提案_v0.1.md` | R0受入とR1追加限定判定 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4_PL2_P4-F判定資料_v0.1.md` | CP-F1／CP-F2の部分開始と未達境界 |
| `docs/development/KOIKI-JavaWeb-FW_Phase4実施計画_見直し草案_v0.1.md` | R1限定例外、Phase 4全体未承認の維持 |
| `docs/development/KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md` | Reference限定開始と正式受渡しの分離 |
| `docs/development/phase4-s1-purpose-dod-stage-mapping-20261009.md` | R1条件付き開始承認と残DoD |
| `docs/development/phase4-s1-r0-closeout-remaining-tasks-handoff-20261010.md` | 次作業入口・文書固定待ち |
| `docs/development/phase4-s1-r0-reference-async-start-review-20261009.md` | 旧草案から本票へ置換した境界 |
| `docs/development/phase4-s1-r1-minimal-start-owner-review-20261012.md` | 最小開始契約・承認記録・commit対象一覧 |

文書commitは本票§5・§7で別Owner判断としたため、まだ未実施。提案メッセージは`docs: approve minimal Reference async R1 start conditions`。code／test／POM／SQL、旧R0 Evidence／archive、tmp／JAR／rawはcommit対象外。新規不足artifact取得・追加再実行・remote・結果受入は個別条件に従い、残予算だけで先行しない。

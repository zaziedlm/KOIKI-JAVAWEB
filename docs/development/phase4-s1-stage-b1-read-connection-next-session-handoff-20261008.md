# Phase4 S1 B-1 読取接続：次セッション引継ぎ（2026-10-08）

## 1. 再開地点とsource

Ownerが資源制限・子JVM設定供給の変更をコミットした。次セッションへ作業開始点と残作業を引き継ぐ。今回の引継ぎ作成では新しい実装・検証を開始していない。

- Repository：`C:\Users\kataoka\Desktop\KOIKI-JAVA\KOIKI-JAVAWEB`
- branch：`feature/phase4-s1-reference-foundation`
- HEAD：`23379b352bfb9ef261789290a3acf40a6805c7e6`
- commit：`test: S1 B-1検証の資源制限と子JVM設定供給を整備`
- この文書作成直前の作業ツリーはclean。上記commitの対象は6ファイル。
- この引継ぎ文書は上記commit後に追加した未コミット文書。Agentはadd／commit／pushを行っていない。

次は**source／環境差分と残るpreflightを確認し、成立後に承認済みのTooling test所有・新規6 class／52件を作成・検証する段階**。資源helperの整備だけが完了しており、新規52件は未作成・未実行。

## 2. 最初に読む正本

1. [AGENTS.md](../../AGENTS.md) と [KOIKI project overview Skill](../agent/skills/koiki-project-overview/SKILL.md)。副Agent禁止、Ownerのコミット操作、所有境界を確認する。
2. [B-1限定開始票](phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md)：§1〜§8の契約・副作用・検証集合・上限と、§11の開始承認。後続の§13／§15／§17／§20／§22の訂正承認を合わせて読む。前半の「候補」「未判断」は提出時の履歴であり、後段の承認記録を優先する。
3. [B-1検証記録](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md)：§12の訂正経緯点検と、§15の最新PASS・保証限界・rawを読む。
4. [段階B追加契約](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md) と [Tooling供給元選択](phase4-s1-stage-b-tooling-source-selection-20261008.md)。リポ内Toolingを限定実供給元として採用しており、外部システム選定待ちに戻さない。

条件付き限定開始と同じ訂正範囲の承認を再要求しない。条件未成立や対象外差分が必要な場合は、具体的な不足・差分を示して停止する。実装結果のOwner受入、B-2、Phase4全体、DoD／正式受渡しは別判断。

## 3. 今回完了したこと

Toolingの既存`PublicationRecoveryTest.java`／`ProcessCrashRecoveryIT.java`の明示選択時だけ資源設定を適用し、test-only `b1fixture/B1ResourceLimits.java`を追加した。業務assertion・件数・通常起動条件を維持。Framework／Reference main、API、Rules、POM／依存、migration、通常設定に変更はない。

| 確認項目 | 結果と限界 |
|---|---|
| PublicationRecoveryTest | 4件PASS（failure／error／skip各0）。先行runのPASSを保持 |
| 選択ProcessCrashRecoveryIT | 2件PASS（failure／error／skip各0）。class24.619秒、Maven27.271秒。資源供給訂正後に1回実行 |
| 選択回帰6件 | 先行4件＋最新2件。単一run・同一helper hashで全6件を再実行した結果とは区別 |
| 親fork・子JVM | heap768 MiB。4子を逐次起動し、子の実heapをjcmdで確認、同時子1を確認 |
| 子Hikari | maximum-pool-size2／minimum-idle0／connection-timeout10000。4子のconfig assertion成功、最初の子は初期logとV1／V2 migration・正常起動も観測 |
| PostgreSQL | 各使い捨てDBを逐次利用。memory1 GiB／CPU1／max_connections16、lock／statement／transaction timeout各10秒を確認 |
| 接続予算 | 起動中・起動後sampleは各3接続、予算8以内。全実行の連続最大値を証明したものではない |
| cleanup | 最新検証終了時にcontainer0、今回の子／fork／Mavenなし、一時phase4-crash directory0 |

子の数値ENV供給では初期Hikari値が10／10／30000になったため、承認済み§21訂正でcanonical command propertyへ変更し、2／0／10000での起動・再起動が成立した。URL／username／passwordはENVのまま、秘密をcommand lineへ出さない。診断leak-detection-thresholdは2000。

Ownerの指示は「原因追及が目標ではない。とりえる情報から得られる情報に集中」。正常起動・上限・cleanupの確認が得られたため、保持接続の完全特定やライブラリ内部原因の調査を再開しない。広いtransaction timeout適用の本番保証・未選択全Tooling回帰・Reference接続成立をこのPASSから導出しない。

## 4. 次セッションの手順

1. Ownerの再開指示後、branch／HEAD／statusと上記sourceの差分を確認する。この引継ぎ文書の未コミット状態は、承認済みcode／設定変更と区別する。意図しない差分を上書きしない。
2. Docker到達性、既存container／Javaと今回のprocessの区別、memory8 GiB／disk10 GiB以上、JDK21・Wrapper Maven・artifact／PG17／Ryuk／Chromium cache、cleanup経路を再確認する。既存process・常設containerを操作しない。
3. 残るpreflightを確認する。Chromium cacheの存在確認と実browser起動の成立は別であり、実起動は未確認。開始票のpackage／classpath・非混入確認も、再開sourceと生成物に対応付ける。完了済み確認を理由なく再実行せず、差分・残件に絞る。
4. 成立した条件と残件をEvidenceへ追記してから、§4／§5の承認済みtest-only fixture／SQL／新規52件を作成し、有界・逐次検証する。契約列・canonical本文・case IDを実コードへ対応付ける。
5. 選択PL2回帰、Reference関連143件、既存25 class／99件＋package済みE2Eの計画を進め、部分検証と最終sourceでの検証を区別する。今回の資源確認でReference／E2E集合まで実行済みとは扱わない。
6. Evidenceにcase／枝・資源・失敗保全・cleanup・未達を整理し、Owner結果レビューへ渡す。コミット操作はOwner自身、または操作前のOwner確認による。

## 5. 作成範囲と保証境界

起点は非配布`build-support/phase4-level2-verification/`。`B1ResourceLimits.java`は作成済み。これからの補助5件は同じtest packageの`B1ReadContract`、`B1SourceCollector`、`B1ProcessHarness`、`B1EvidenceLedger`、`B1JdbcReadClient`。専用SQLは`src/test/resources/s1-b1/read-source.sql`。READMEと既存Evidenceを必要最小限更新する。

| 新規test class | case ID／件数 |
|---|---|
| B1TargetReadTest | T01〜T10／10 |
| B1ProviderReadTest | P01〜P08／8 |
| B1StopObservationTest | S01〜S08／8 |
| B1EvidenceLedgerTest | E01〜E10／10 |
| B1ReaderPrivilegeTest | R01〜R08／8 |
| B1ReadConnectionIT | I01〜I08／8 |

総計52 invocationはcoverage候補であり、網羅性認定ではない。供給元所有viewとSELECT専用role、厳密tuple／binding、snapshot鮮度・失効、一意immutable証拠の先行保存、管理下の子process台帳・世代を検証する。有限fixtureの起動・イベント発行・stub受理・当該終了待機・観測保存だけが承認副作用。

DBのFAILEDや受理行なしから再送可能と推測しない。取得不能・曖昧・矛盾は拒否／UNKNOWNを保持する。全管理下子の終了は観測であり、drain・分散fencing・台帳外process不在の保証ではない。D11／D12を保持し、I05の最後の照合後変更窓は未保護の観測として記録する。

B-2のReference Adapter／肯定Port登録・issue／consume／close、実外部通信、sender／listener／復旧runner、新publication schema／Modulith runtime、Framework API／Rules／依存、CI／remoteは対象外。既存test変更の例外は承認済み2ファイルの資源・launch差分だけ。

## 6. Maven入口と再検証時の注意

`jdbc`単独のlifecycle test-compileは既存S1 testのtest-only依存が不足する。一方、support profile付きruntimeはStarter／Flyway構成を変えてしまう。承認済みの**compileはsupport profile付き、runtimeはjdbc単独の直接goal**を維持する。各実行は`MAVEN_OPTS=-Xmx768m`を一時設定し、finallyで元値へ復元する。

```powershell
# test-compile（offline）
.\mvnw.cmd -o -B -ntp -f build-support/phase4-level2-verification/pom.xml '-Pjdbc,s1-contract,s1-web' -DskipTests test-compile

# 既存4件：再実行が必要な場合の承認済み入口
.\mvnw.cmd -o -B -ntp -f build-support/phase4-level2-verification/pom.xml -Pjdbc '-Dkoiki.b1.resource-limits.enabled=true' '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' '-Dtest=PublicationRecoveryTest' surefire:test

# 選択IT2件：最新PASSの入口
.\mvnw.cmd -o -B -ntp -f build-support/phase4-level2-verification/pom.xml -Pjdbc '-Dkoiki.b1.resource-limits.enabled=true' '-DargLine=-Xmx768m' '-DforkCount=1' '-DreuseForks=false' '-Djunit.jupiter.execution.parallel.enabled=false' '-Dit.test=ProcessCrashRecoveryIT#incompletePublicationIsDeliveredAfterProcessRestart+acceptedSendBeforeCrashIsNotDuplicatedAfterRestart' failsafe:integration-test failsafe:verify
```

直接goalは適切なsourceからcompile済みであることが前提。新規testの入口はcase ID・実invocationを対応付けて具体化する。新download／installなし。Docker named pipe／Maven書込みがsandbox権限で拒否された場合は、環境の権限付き実行手順を使い、拒否を迂回しない。

最新検証のTooling JAR：`target/phase4-level2-verification-0.1.0-SNAPSHOT.jar`、SHA-256 `9FFC2607B1C22A65E238A437BDE496EF75E3D9F879205D583888187F434399D5`。既存JARの不変とtest helper非混入を確認した。生成物はGit管理外なので再開時に現在の状態を確認する。

## 7. raw・環境の最後の観測と上限

rawはGit管理外の`build-support/phase4-level2-verification/target/s1-b1-read-20261008/`。`preflight`、`resource-limits`、`runtime-jdbc`、`diagnostic-preservation`、`connection-ownership`、`startup-observation`、最新PASSの`canonical-pool`を保持する。失敗runも保全済み。最新directoryにsanitized XML／log、resource値、SQL分類sample、source／開始終了状態、初期pool観測記録がある。正常子の完全logは既存cleanupで削除され、全4子の先頭log保存とは区別する。

**cleanはtarget内の診断保全も削除する。** 開始票の最終clean package前に、sanitized rawを消さない保全先・手順を具体化する。無断削除・resetや、failed runを上書きした再実行を行わない。

最後の実測は2026-10-08 **11:49:52 JST**：container0、今回の子／fork／Mavenなし、既存Java PID2660／4684のみ、memory18,277,336 KiB、disk47,752,577,024 bytes。これは前セッションの観測履歴であり再開時の現在値ではない。既存Javaをcleanup対象にしない。JDK Temurin21.0.12.1／Maven3.9.16／Docker29.5.3、PG17-alpine／Ryuk0.14 cacheを確認済み。

開始票§7の上限を継続する。セッション変更を理由にraw／再実行／作業量の累積をリセットしない。

- Maven／class逐次、JUnit並列無効、fork1／reuseForks=false、各heap768 MiB、通常子同時1、DB同時1。
- DB1 GiB／CPU1／max_connections16、役割別接続予算計8。timeout10秒、子起動60秒／終了10秒。
- 新規各class15分・52件計90分、選択PL2回帰30分、Reference関連143件60分、既存99件＋E2E60分、全実経過240分（cleanup含む）。
- raw全run計1 GiB。同原因・同条件rerun最大1回。最新rawは約1.33 MBで上限内だが、再開後は累積を確認する。
- 作業量16〜28標準時間、12時間相当で進捗確認、28時間相当で停止・再評価。履歴に未集計の時間がある場合は推測で残予算を断定せず、記録から確認する。
- classごとに当該子／Executor／pool／DB／Ryuk／portをcleanup確認。権限・隔離・秘密保護・上限・回帰・cleanupに問題が出たら次classを開始せず、具体的な不足を記録する。

## 8. 次セッションへ渡す依頼文

> この引継ぎ文書を起点に、commit `23379b3` からのsource／環境差分と残るpreflightを確認してください。条件成立後、承認済みB-1のTooling test所有・新規6 class／52件の作成・限定検証へ進めます。診断保全と累積上限、D11／D12・網羅性の未達を維持し、原因追及自体を目的にしないでください。コミットはOwner操作または事前確認とし、push等remote操作は行いません。

# Phase 4 S1：専用92件の補完開始・test fixture競合で停止（2026-10-16）

状態：**FIXTURE CORRECTION VERIFIED / 専用92件PASS＋訂正後の最終通常root305件PASS・予定skip92件。** 初回D06 ERRORと停止を履歴として保持する。§1〜5は訂正前のrun、§6以降はOwnerの問題解消指示後の現行結果であり、結果受入・commit・tag・remoteは別判断。

## 1. 目的・source・今回の範囲

Ownerは、[タグ前検証記録§8](phase4-s1-main-pretag-verification-stop-20261016.md#8-専用92件への影響確認と補完前のsource固定)で残した直接影響24件を含め、専用92件を環境準備して確認する方針を示した。有意性を点検し、旧fixtureの作り直しやR4へ深く入り込まないことを条件とした。test変更と記録のlocal commitを先行し、その確認後に専用検証へ戻る指示を受けた。

- source：clean `main`、`d38d6b6c998736783ed918eba7a9bcc9e4a4b06f`。origin/mainよりlocal commit 1件先行。commit前の事前確認source `6aa4f70`を今回の実行sourceとして流用していない。
- 対象はS1専用7 class／64件＋B2 process-backed 3 class／28件＝92件。保存・認可・Audit・scope／TTL・許可発行／読取という現行基盤の回帰として有意であり、通知配信全体やR4を完了させる集合ではない。
- 順序は直接影響24件（管理設定DB6、登録6、運用証拠DB12）→残りS1 40件→B2 28件。各class初回1回、失敗時は証拠保全・cleanupで後続を停止。追加改修・依存取得・再実行を自動で進めない。
- 過去の検証予算は流用せず、今回runで40分の実行区切りとcleanup予約5分を設定した。実際は約1分10秒で停止した。副Agent、commit、tag、remoteは行っていない。

## 2. 環境・準備と有意性

JDK21.0.12、Wrapper Maven3.9.16、Docker Server29.1.3、Windowsの既存image／dependency cacheをofflineで使用した。今回のclean commitとsource fingerprintを基点に標準`clean install -DskipTests`で既存local Maven repositoryへ準備した。新しい依存・POM・migration等は追加していない。旧受入artifactは別の固定JARとして保持され、今回のlocal cache出力と混同しない。

開始時container0、保護Java PID46576／46884だけ、空きmemory14,352,452 KiB・C空き525,385,011,200 bytes。Maven／test fork heap768 MiB、fork1／reuseなし／JUnit並列なし。専用有効化flagを明示し、既存harnessのDB memory1 GiB／CPU1／max_connections16、各pool・DB timeoutと業務assertionを維持した。通常rootへ専用flagを混ぜた一括実行はしていない。

一時directoryは今回run内の実体pathをMaven起動時・fork argLine・TEMP／TMPへ供給し、終了時にenvを復元した。これはB2を含む一時設定fileのpath前提を満たすための環境準備であり、production loaderの安全条件を弱める変更ではない。classpathの新規準備には既存`prepare-b2-reference-acceptance.ps1`を使う計画だったが、S1で停止したため未起動。固定tmpへ依存する旧履歴`verify-s1-b2.ps1`は起動していない。

## 3. 実行結果

| 対象 | 結果 |
| --- | --- |
| 標準offline `clean install -DskipTests` | PASS／exit0、46秒。test実行のPASSには加算しない |
| `ManagedRecoveryConfigurationDbTest` | exit1、XML6件＝**D06 ERROR1＋失敗後停止skip5**。PASS0、failure0。skip5は通常rootの専用無効化とは異なる |
| 残りの直接影響18件 | 未実行 |
| その他S1 40件 | 未実行 |
| B2準備・28件 | 未実行 |

`D06RetainsConsumptionAndExpiresOnlyAfterSnapshotReplacement`は、`db.open(Mode.PERMIT, false, manifest, true)`からのSpring context構築で停止した。`notificationPermitService`のScope引数に対し、`notificationManagedScope`と`s1ManagedTestTargets`の2 beanが候補となり、`NoUniqueBeanDefinitionException`を生じた。今回のpath訂正をしたfileはloaderで拒否されておらず、別のfixture登録問題を観測している。消費・snapshot置換後の保持／期限等のD06本体assertionは到達していない。

## 4. 読取だけで特定した原因と限定訂正候補

`NotificationFoundationDbHarness.open`のfiniteEvidence枝は次の登録を行っている。

```java
registerSingleton("s1ManagedTestTargets", (RecoveryTargetPort) ports);
```

`ports`の実型`FinitePorts`はTargetに加えてScope・TTL・Evidenceの4 Portを実装する。JavaのcastはSpringに登録した実体の実装interfaceを制限しないため、管理設定から生成される本来のScope beanと衝突する。実測したのはScopeの重複であり、TTL等の競合はこのrunで別途実証していない。

履歴の読取では、`b064084`（R3-A preflight compile baseline訂正）が、旧`(RecoveryTargetPort) ports::current`から上記castへ変更していた。[当時の記録§5](phase4-s1-r3a-source-and-preflight-stop-20261013.md)は登録対象と動作を維持すると整理したが、今回の専用D06は実型の公開interfaceが増えた影響を示している。旧epochの受入を失敗へ上書きせず、現行sourceで検出したtest fixture回帰として扱う。

限定訂正の候補は、同じ`current`呼出しへ委譲する**RecoveryTargetPortだけを実装した匿名adapter**を、このsingletonへ登録する1か所の変更である。旧method referenceをそのまま戻すと当時のError Prone診断を再発させる可能性があるため、compile成立も確認対象とする。`@Primary`／bean名指定で偽Scopeを選ぶ、productionのDI契約を変える、assertionを削る対応は採らない。

今回は方針どおり**追加編集・compile・D06再実行・後続classを開始していない**。次に訂正する場合はこのtest-only登録と必要な限定検証を対象にし、source／実行epochを分ける。無制限な原因探索・R4や本番復旧へは広げない。

## 5. 証拠・cleanup・未達

今回runは`tmp/main-d38d6b6-dedicated92-20261016/`。`preflight.json`、`source-before.json`、build環境／Docker version、`standard-install/`、`ManagedRecoveryConfigurationDbTest/`の実引数・Maven log・fresh XML・結果JSON・有限cleanup sample、`progress.json`、`summary.json`を保存した。source fingerprintとGitのclean状態はclass終了後も一致した。tmp現物はGitで移送されない。

| 失敗証拠 | SHA-256 |
| --- | --- |
| `ManagedRecoveryConfigurationDbTest/maven.log` | `75B62FBE7231F404F5132D52828A49FC7272A0597E008D6E09D5427C745DF13E` |
| 同directoryの`TEST-org.koikifw.reference.notification.ManagedRecoveryConfigurationDbTest.xml` | `6C58015385BBA23CB7399D7B481F12D677D986C23D5159B0981A6A4E37812757` |

2026-10-16 10:41:34 JSTの終了確認はcontainer0、Javaは保護対象2 PIDだけ、検証用子process残存0。今回のfixture競合は有意な検出結果である一方、専用92件は**完走していない**。通常root305 PASSと前段の選択非同期等の結果も、この専用集合のPASSへ転記しない。タグ前の直接影響24件を含む残検証と、B03a既知FAIL・E08残枝・全体DoDの境界を維持する。

## 6. 問題解消指示・test-only訂正と局所確認

Ownerは上記の停止・原因と限定訂正候補を確認し、「問題解消へ進めましょう」と指示した。この指示に基づき、`NotificationFoundationDbHarness.open`のfiniteEvidence枝のsingleton登録1か所だけを変更した。

```java
context.getBeanFactory().registerSingleton("s1ManagedTestTargets", new RecoveryTargetPort() {
    @Override public Optional<RecoveryTarget> current(String environment, UUID publication) {
        return ports.current(environment, publication);
    }
});
```

登録実体がTarget以外のScope／TTL／Evidenceとして認識されないようにし、Targetの処理は同じ`ports.current`へ委譲する。managed Scope／TTLの実bean、Evidence委譲、一般finitePorts枝、業務assertion・件数は維持した。production、Public API、Rules、POM／依存、Security、migration、CIは不変更。

sourceは`d38d6b6c998736783ed918eba7a9bcc9e4a4b06f`＋上記未commit差分1 file。新run `tmp/main-d38d6b6-dedicated92-fixture-correction-20261016/`で、開始時Git差分・source hashを固定した。harness訂正後SHA-256は`6073D567521A761849F9039E3E118C049C13ED28F598FB7DD9C4C60FF5D77A5A`で、終了までsource fingerprintの変化はなかった。初回停止のraw／XMLを上書きしていない。

まずoffline Reference reactorの`test-compile`1回が成功し、当時のError Prone診断を再発させずにcompileできることを確認した。続くD06単独1件はPASS。さらに同class全6件を含む専用92件を各class1回実行した。D06単独は局所確認の追加1 invocationであり、92種類へ二重計上しない。

## 7. 訂正後の専用92件・環境準備・cleanup

| 集合／class | 件数 | 結果 |
| --- | ---: | --- |
| 直接影響：`ManagedRecoveryConfigurationDbTest` | 6 | PASS |
| 直接影響：`ManagedRecoveryConfigurationRegistrationTest` | 6 | PASS |
| 直接影響：`OperationalRecoveryEvidenceDbTest` | 12 | PASS |
| `NotificationFoundationMigrationTest` | 5 | PASS |
| `NotificationFoundationPersistenceTest` | 12 | PASS |
| `NotificationFoundationRegistrationTest` | 7 | PASS |
| `NotificationFoundationTransactionTest` | 16 | PASS |
| `B2ProtectedIssueReadTest` | 12 | PASS |
| `B2IssueBoundaryTest` | 8 | PASS |
| `B2IssueTransactionTest` | 8 | PASS |
| **S1 64＋B2 28＝専用集合** | **92** | **failure／error／skip 0** |

各classのfresh XML・期待件数・exit0を確認し、classごとにcontainerと検証用Java子processの終了を確認してから次へ進んだ。通常rootでの92 skipをPASSへ置き換えたのではなく、明示選択した今回の92件が実際に実行された結果である。B2登録8件は前段の通常root／局所20件で確認済みで、今回の92件へ加算していない。

B2は既存`prepare-b2-reference-acceptance.ps1`を1回使用し、jdbc main package、`jdbc,s1-contract,s1-web` test-compile、jdbc単独test-scope classpath生成がすべて成功した。coordinator・reader・SQL等を今回sourceから準備し、Tooling source hashも不変。旧履歴runner・固定旧classpathは使っていない。Maven／fork heap768 MiB、並列無効、実体pathの専用TEMP、既存harnessのDB／pool／timeout・有限cleanup条件を維持し、run終了時にMAVEN_OPTS／TEMP／TMPを復元した。凍結source DBへの独自監視接続は追加していない。

実行は2026-10-16 10:57:12〜11:06:58 JST、compile・D06単独・専用92件・B2準備・class後cleanup込みで**9分46秒**。最終11:06:57 JSTの確認はcontainer0、保護Java PID46576／46884のみ、検証用子process残存0。source・Git差分の不変を確認した。これは有限の観測とfixture自身のassertionであり、全瞬間の最大使用量の証明ではない。

新runの`summary.json`、`progress.json`、`source-before.json`、`source-change.patch`、各classの実引数／Maven log／XML／結果／cleanup、`b2-preparation/`のsource／artifact hash・classpathを保全した。B2のsanitized coordinator logは30個を`b2-coordinator-logs/`へ退避し、`b2-coordinator-log-index.json`へhashを保存。所有directoryのready／partial file残存0も確認した。主要証拠のhash台帳は`evidence-hashes.json`、最終`summary.json`のSHA-256は`72AB7CB9EC5C2BD17BAC44902BBFD0BBCED218B09FE8592DD6F72E0185CDEA1C`。tmp現物はGitでは移送されない。

## 8. 現行判定と成果固定境界

今回のfixture競合はtest-onlyの登録訂正で解消し、直接影響24件を含む専用92件の補完は成立した。compile・D06単独と全92件以外の検証を自動追加せず、通常rootや非同期選択の過去runも反復していない。

これは現行の保存・認可・Audit・scope／TTL・許可発行／読取を既存の有限fixtureで確認した結果であり、実provider配信、肯定consume／closeの本番復旧接続、B03a自動完了FAIL、E08残枝、R4／R5、全体DoD、正式受渡し、Phase 4全体の未達を解消したとはしない。caseを削除・skip化したり期待を弱めてPASS化したりしていない。

訂正後の未commit対象はharness 1 fileと本記録。Owner結果受入、成果固定commit、訂正を含むタグ対象commitとタグ作成、remote操作はそれぞれ別判断に残す。既存commit `d38d6b6`そのものに今回のharness訂正が含まれると扱わない。

## 9. Owner指示による最終通常root確認（2026-10-16）

専用92件の成立後、Ownerは「最終念のためですが、通常root検証を通しておきましょう」と指示した。同じharness訂正を含むsourceで、通常構成のoffline `clean package`を新たに1回実行した。§8の「通常rootを反復していない」はこの指示前の記録である。

- source：`d38d6b6`＋本harness訂正。開始／終了時のhelper2 fileのhash、Git HEAD・差分集合は不変。harness hashは§6の値と一致する。
- 実行条件：Maven／fork heap768 MiB、fork1／reuseなし、JUnit並列無効、通常root専用DB制限、実体pathの専用TEMP、JAR強制作成。S1／B2専用有効化flagとasync profileは供給していない。MAVEN_OPTS／TEMP／TMPはfinallyで復元した。
- 結果：**BUILD SUCCESS／exit0、fresh XML62 file、397件＝305 PASS＋予定skip92件、failure／error0**。2026-10-16 11:10〜11:13 JST、Maven実行時間3分25秒。
- 最終cleanup：11:17:34 JST、container0、Javaは保護PID46576／46884だけ、検証用子process残存0。

証拠は`tmp/main-d38d6b6-final-normal-root-20261016/`の`preflight.json`、`args.json`、`normal-root.log`、`exit.json`、`root-reports/`の62 XML、`report-index.json`、cleanup、`summary.json`、固定通常JARに保存した。通常JARのevents-jdbc runtimeは0。JAR SHA-256は`3704CD2D50A1ABE60F78BFEB71ED01BA0650CE3F1A3C3B56FF01CC724129F808`、log SHA-256は`0758381D900EB56AF3EA0FC68F632061A2CE7B8A91E10D7EB35903DD879D583B`。これらもGit移送対象外である。

通常root XMLのskip92件と、§7の専用XMLの実行92件をclass／method IDで集合照合し、各92種類・差分0を確認した。台帳は`dedicated92-case-mapping.json`。したがって通常rootの305件と専用の92件が対応する397種類を別経路で確認できた。D06単独の追加1件を種類数へ二重加算せず、rootのskip自体をPASSとは数えない。

本確認後も未commitはharnessと本記録の2 file。新しいsource訂正、専用／非同期の追加再実行、commit・tag・remote操作は行っていない。§8の本番復旧・既知FAIL・R4以降と全体DoDに関する境界は維持する。

## 10. 成果固定commit・branch pushのOwner指示

Ownerは、作業branchへのcommit・push→PR／checks確認→merge→main CI確認→checkpointタグという手順案を確認後、「コミット、pushへ進めます」と指示した。今回の操作は、local mainの`d38d6b6`を起点に`fix/phase4-s1-pretag-verification-20261016`を作り、harness訂正と本記録の2 fileをcommitし、`d38d6b6`を含む同branchをpushする範囲とする。mainへの直接push、PR作成・merge、タグ作成・タグpushは今回実行しない。

§8〜9の未commit記述はこの指示前の検証終了時点である。検証sourceは引き続き§6・§9のHEAD＋harness fingerprintで識別し、後から作るcommit hashを実行時HEADへ読み替えない。tmpのJAR・XML・log等はpush対象外であり、本記録の所在・hash・適用限界を引き継ぐ。

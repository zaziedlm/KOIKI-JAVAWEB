# S1対象・運用証拠ハーネス：preflight・検証記録

**最新結果（2026-10-08）：新規5 class／48件、scope／TTL40件、初回55件、既存25 class／99件とpackage済みE2E1件はPASS。** 最終provider補強後も新規48件・ArchitectureはPASS。source不変・fixture非混入・cleanupを確認（§8）。限定ハーネスの実装・検証結果は`COMPLETE / OWNER APPROVED`（§10）、変更7ファイルは未commit。filenameは開始票の指定を維持し、実確認日は2026-10-08とする。

## 1. 承認・clean source固定

- [開始票§9](../../development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#9-owner承認条件付き限定開始2026-10-08)の条件付き限定開始承認を確認。Ownerが文書4件をcommitした。
- branch：`feature/phase4-s1-reference-foundation`。固定HEAD：`ea07badc5720949a2cc7b03c9f522e011183c539`（`docs: S1対象・運用証拠ハーネスの条件付き限定開始承認を記録`）。開始時clean、upstream比ahead8、`git diff --check`成功。Agentはadd／commit／pushしていない。
- 対象はtest所有の段階A。本番main／Port／schema／依存／通常構成、実運用Adapter・停止操作・provider通信・送信は変更しない。

## 2. 端末・cache・実効制限の確認（2026-10-08 JST）

| 項目 | 実確認結果 |
|---|---|
| 開始空き資源（05:08:19） | memory16,435,508 KiB（15.67 GiB）、disk47,627,243,520 bytes（44.36 GiB）。開始条件8／10 GiB以上 |
| Java／Maven | Temurin21.0.12.1、Repository Wrapper Maven3.9.16。新規installなし |
| Docker | Server29.5.3到達、稼働container0。`postgres:17-alpine` image ID `18cfe3ef5e68`、`testcontainers/ryuk:0.14.0` ID `7c1a8a9a47c7`が既存cacheにある。pullなし |
| offline compile | heap768 MiBのMavenで`-o -B -ntp -pl koiki-reference-app -am -DskipTests test-compile` SUCCESS、10.295秒。test未実行 |
| E2E POM | 同じheap設定で`-o -B -ntp -f build-support/reference-e2e-verification/pom.xml validate` SUCCESS、0.797秒。E2E未実行 |
| browser | 既存Playwright classpathの全JAR存在を確認。非配布の既存`BrowserPreflight.java`をheap768 MiB、download禁止で使用。Chromium151.0.7922.34のheadless起動、静的title、browser／driver終了がPASS |
| 隔離DB smoke | 既存`NotificationFoundationPersistenceTest#preventsNextPermitForUnclosedTarget` 1件PASS、failure／error／skip0。Maven22.608秒、command23.91秒。新規48件・全55件／40件／99件のPASSには数えない |
| smoke実効制限 | Harnessのcontainer inspect・DB SHOW・JVM・pool assertion成功。DB memory1,073,741,824 bytes、nanoCPUs1,000,000,000、max_connections16、lock／statement／transaction timeout10秒、fork heap805,306,368 bytes、用途別pool2／idle0・connection timeout10秒、connection予算8 |
| 終了空き資源（05:15:35） | memory16,857,896 KiB（約16.08 GiB）、disk47,390,539,776 bytes（約44.14 GiB）。連続資源monitorの最小値ではなく開始／終了sample |

compileとsmokeではMAVEN_OPTSを実行区間だけ`-Xmx768m`に設定し、finallyで元の値へ復元。smokeはfork1／reuseForks=false、JUnit並列無効、resource-limits有効、offline。全既存testはまだ実行していない。今回の新規classも作成していない。

通常権限ではCIMアクセス拒否とDocker named pipe接続拒否を確認。AGENTSの承認済み権限エラー手順と本限定開始承認に基づき、実行環境の権限付き承認で同じread-only診断を再実行して成功した。Docker障害とは判定していない。browserも通常権限で`spawn EPERM`、権限付きの同じ既存browser起動・終了で成功。権限拒否を迂回した実行やOS権限変更は行っていない。

## 3. cleanup・source・記録

smoke DB ID：`a5b16cc6bdbe8716c7df4c454acbb6011d231639c7722a3342dd7378de5025f0`。test終了直後にはRyukが短時間残っていたが、後続read-only診断で当該DB不存在と稼働container0を確認。既存停止containerを変更していない。

browser helperは`browserAndDriverClosed=true`を出力。最終process診断でchrome-headless-shellなし、当該BrowserPreflight／Surefire／Maven processなしを確認。既存Java2件は前日12:02起動・VSCodeと同じparentのprocessで、操作していない。引数全体や秘密は出力しない。

HEADは固定値を維持し、本番main tracked145件について`git diff --name-only -- koiki-reference-app/src/main`は0件。新規test／Harness変更もなし。preflight後に生じた差分はD11訂正文書・AGENTSと本Evidenceだけ。

非配布rawは`build-support/reference-e2e-verification/target/operational-evidence-20261007/preflight-20261008/`のcompile／E2E validate log、sanitized DB smoke log／XMLへ保存。確認時合計69,797 bytes、1 GiB以内。後段runのraw予算へ含める。DB smoke XMLからsystem-properties／system-out／system-errを除去した。

実効制限の意味は開始票に従う。heapはprocess全memory、CPU1は端末全process CPU、transaction10秒は全外部I/O／JVMの強制停止を保証しない。新規D08の2 task・失効台帳・証拠追跡は未実装／未検証で、既存smokeから実証済みとしない。

## 4. D11の前提訂正と次の条件

preflightのsource確認で、Agentが開始票D11に記載した「同一対象へ複数permitが発行可能」という前提誤りを発見した。実際にはV4の`uk_kkref_notification_permit_unclosed_target`がenvironment・publicationごとの未閉鎖許可を1件に制限する。今回の既存smokeでも、未閉鎖・消費後・期限経過後の次許可拒否を確認した。

[開始票§10](../../development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#10-preflightでのd11前提訂正2026-10-08owner確認待ち)に訂正案を記録。D11は既存一意保証と古い対象閉鎖の未達を分けて検証し、別publicationにまたがる同じ論理通知の横断抑止を未達として残す。5 class／48件、main変更なし、上限は変更しない。

環境条件は成立したが、承認されたD11と異なるtestを無断で作成せず、本訂正箇所のOwner確認後に新規ハーネス作成へ進む。同じ承認済み範囲の全面的な再承認は求めない。実装・検証結果のOwner受入、実運用接続／DoD／remoteは未成立。

## 5. D11訂正承認と開始条件（2026-10-08）

Ownerは[開始票§11](../../development/phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#11-d11訂正のowner承認2026-10-08)で§10の訂正を了承・承認した。§4の確認待ちは解消し、訂正後D11・新規48件と既存承認条件を採用する。preflightの成功・制約・raw記録は変更しない。

次は訂正・承認記録を含む文書4件のcommitによるclean source再固定。その後はsource／環境差分を確認し、同じ承認範囲のハーネス作成へ進む。main／test変更や新たな懸念がないcompile／browser／既存smokeは無条件に再実行しない。Agentはgit add／commit／pushを行っていない。

## 6. 訂正後source固定とtest所有ハーネス作成（2026-10-08）

Ownerのcommit `4c8761a5fa267d49b98bed63ab0a72961610a204`をclean sourceとして再固定。`ea07bad`からの差分は訂正・承認・preflight記録の文書4件のみで、main／test差分0。開始直前05:23:01 JSTのread-only診断でmemory16.18 GiB・disk44.03 GiB、Docker29.5.3、稼働container0と既存PG17／Ryuk imageを再確認。前項のcompile／browser／smokeはsource不変・追加懸念なしとして反復していない。

`OperationalRecoveryTestSupport`と新規5 test classを作成し、T01〜T10／S01〜S08／P01〜P08／E01〜E10／D01〜D12の48 methodに対応付けた。既存Harnessの拡張は不要。実DBでは既存Harnessの用途別contextから実Identity／管理scope・TTL／Repository／Recorders／transaction managerを取得し、同じ本番`RecoveryPermitService`へtest modelのtarget／evidence Portだけを渡す。実DBの制約や本番のBean選択は置き換えない。

採取・保存はtransaction外。本文とsealed原本の一致、一意key、別管理の失効・制御世代、対象・停止・providerの照合をprocess内だけで行う。実provider／platform・耐久保管の真正性を主張しない。test用hookはtest helperだけに置き、D06／D12で照合中・照合後の失効を区別する。

D01はService transaction内の証拠呼出しとDBのpermit relation `RowShareLock`を観測。D07はconsumer roleだけのAudit INSERTを一時REVOKE、finally復元し、消費／version／Auditのrollbackと先行証拠の未使用を確認。D08の2 taskは認証を各taskに設定・解放し、latch／Future／Executor終了を各10秒以内で有界に扱う。

D11は同environment・publicationの未閉鎖許可、消費後・期限後の次許可拒否を維持し、attempt変更／削除後の歴史対象閉鎖拒否を確認。index定義から別publication間の論理通知を抑止する制約ではないことを区別する。D12は照合中に検知した制御喪失を拒否する一方、最後の肯定照合後に制御を喪失させると消費がcommitし得る窓を観測。新しい原子的保護や送信保証を実装したと説明しない。

offline test-compileはSUCCESS、11.737秒。新規5 class／48件はfailure／error／skip0でPASS、classごとのcleanupも成功。既存回帰・package／E2Eは実行中であり、最終結果は次項へ記録する。main／Port／schema／grant定義／依存／通常設定は変更していない。git add／commit／pushは行っていない。

## 7. 新規48件・scope／TTL40件・初回55件の結果と補強

| 新規class | method／invocation | 最終結果 |
|---|---|---|
| OperationalRecoveryTargetContractTest | T01〜T10／10 | PASS |
| OperationalRecoveryStopEvidenceTest | S01〜S08／8 | PASS |
| OperationalRecoveryProviderEvidenceTest | P01〜P08／8 | PASS |
| OperationalRecoveryEvidenceStoreTest | E01〜E10／10 | PASS |
| OperationalRecoveryEvidenceDbTest | D01〜D12／12 | PASS |

非配布`target/operational-evidence-20261007/suite-20261008/`に初回17 class／145件（新規48＋scope／TTL40＋初回55＋既存Architecture2）のclass別sanitized XML／log／manifest、source hashと資源sampleを保管。全failure／error／skip0、cleanup成功。新規集合のclass別合計99.89秒、scope／TTL119.45秒、初回基盤198.20秒で各採用時間枠内。Architecture2件は既存99件内で重複計数しない。全class別合計435.51秒、85 sampleのmemory最小15.71 GiB、disk最小43.76 GiB、DB同時最大1。

結果レビューでD02の別主体拒否を、不存在userに加えて実在するACTIVE・現在EXECUTE能力・明示scope許可を持つ別userまで補強した。能力／scopeの肯定を実Portで確認したうえでも、発行者不一致で消費をHOLDする。能力失効・不存在userはSecurity Audit、発行者不一致は既存Serviceの固定HOLD分類を維持し、worker本人認証／委譲を実装した扱いにしない。S08も独立した復旧lock保持値を明示し、肯定停止証拠へ転用できないことを確認した。

補強差分は新規test3ファイル（helper・Stop・Db）だけ。本番main・既存Harness・40／55件のsourceは不変。初回suite実行中にJava sourceを変更せず、終了後に補強した。`final-new-20261008/`で最終の新規48件＋Architecture2件を再実行し、6 class／50件は全PASS、failure／error／skip0、cleanup成功。変更による再検証で、失敗の無条件反復ではない。class別合計116.91秒、23 sampleのmemory最小15.73 GiB、disk最小43.74 GiB、DB同時最大1。

最終new集合の実行前後Java hash差分0を確認。run別のhash・case対応・XMLを保全し、初回145件だけですべての最終test sourceを実行したと説明しない。Windowsでのlocal結果であり、Linux／CI・実運用供給元の受入には拡張しない。既存25 class／99件とpackage済みE2Eの最終結果は次項へ記録する。

## 8. 既存回帰・最終provider補強・JAR・cleanup（2026-10-08）

`regression-20261008/`の既存25 class／99件とpackage済みE2E1件は全PASS、failure／error／skip0。前回baselineと同じclass・invocation数を維持した。05:39:58〜05:50:40 JST、642.13秒で60分以内。122 sample、memory最小15.38 GiB・disk最小43.53 GiB・DB同時最大1。sanitized XML／log、run manifest、資源記録・source hashを保全し、回帰前後のJava hash差分0を確認した。

E2E直前のoffline clean packageはSUCCESS。Reference child PID22948、heap805,306,368 bytes、pool最大4／minimum idle1、隔離DB memory1 GiB／CPU1／max_connections16を実効検査で確認。browser／issuer／child／port／一時logのcleanup成功。

**JAR SHA-256：** `33997D4275DF3DC71037364E87BD55D6672D56CE71EAFFAF7A6D7FFE3A8243DE`。最終検査でも同hash。JAR entryにOperationalRecovery各test／helper、ManagedRecoveryTestSupport、NotificationFoundationDbHarness、隔離grant SQL、assignment.jsonなし。新しい有限UUID・Clock・運用model・故障注入はtest内だけで、本番Bean／mode／propertyを追加していない。

providerの最終補強では、最新receiptとの一致だけでなく、対象に固定した通知key・payload・宛先への一致を独立に強制した。P08は最新receiptを別通知へ差し替えても肯定にならないことを確認。P04はstubの独立受理台帳と「未受理」観測が矛盾した場合も再実行証拠を拒否する。台帳・照会・受理はprocess内stubで、実providerへの保証ではない。

回帰終了後に新規helper／Provider testの2ファイルだけを補強。本番main・既存test・Harness・JARは変更していない。`final-binding-20261008/`で最終新規48件＋Architecture2件（6 class／50 invocation）を再検証し、全PASS・failure／error／skip0、cleanup成功。class別合計117.87秒、新規48件分99.85秒。23 sample、memory最小15.41 GiB・disk最小43.36 GiB・DB同時最大1。補強による条件変更後の再検証で、同原因・同条件の失敗反復ではない。

最終binding run前後のJava hash差分0。最終hashを`target/operational-evidence-20261007/final-source-hashes.json`に保存。本番mainはHEADからの差分0。初回suite・主体／lock補強・既存回帰・provider補強のsourceと結果を区別し、単一runで全最終test差分を実行したとは説明しない。既存40／55件が依存するsourceとmainは全期間不変。

06:08:32 JSTの最終read-only診断で稼働container0、E2E当該DB不存在、child22948不存在、browser／Maven／Surefireの当該processなし。既存Java2件は前日からのIDE側processで操作していない。既存停止containerも変更していない。

最終rawは合計2,968,233 bytesで1 GiB以内。新規／関連・既存回帰・補強／cleanupを含む実経過40.84分で全検証180分以内。各class10分、新規集合60分、scope／TTL＋初回集合60分、既存＋E2E60分の各上限内。heap／CPU／timeoutの保証範囲は開始票どおりで、全process・外部I/Oの強制停止保証にはしない。

## 9. 差分レビュー対象・未達・次の判断

差分は新規test6ファイル（helper＋5 class）と本Evidence1件の計7件。既存Harnessの変更は不要で、本番main／Port／migration／grant定義／依存／通常properties／Security／CIは変更していない。git add／commit／pushは行っていない。

Ownerレビューの中心：

- [OperationalRecoveryTestSupport](../../../koiki-reference-app/src/test/java/org/koikifw/referenceacceptance/notification/OperationalRecoveryTestSupport.java)：対象・停止・provider各tuple、本文とsealed原本、一意keyと失効の分離、固定通知key／payload／宛先の照合、実Serviceへのtest Portだけの接続。
- [TargetContractTest](../../../koiki-reference-app/src/test/java/org/koikifw/reference/notification/OperationalRecoveryTargetContractTest.java)、[StopEvidenceTest](../../../koiki-reference-app/src/test/java/org/koikifw/reference/notification/OperationalRecoveryStopEvidenceTest.java)、[ProviderEvidenceTest](../../../koiki-reference-app/src/test/java/org/koikifw/reference/notification/OperationalRecoveryProviderEvidenceTest.java)、[EvidenceStoreTest](../../../koiki-reference-app/src/test/java/org/koikifw/reference/notification/OperationalRecoveryEvidenceStoreTest.java)：T／S／P／E各IDと開始票の枝の対応、取得不能・変更・期限一致・改変・UNKNOWN拒否。
- [EvidenceDbTest](../../../koiki-reference-app/src/test/java/org/koikifw/reference/notification/OperationalRecoveryEvidenceDbTest.java)：D01の実transaction／lock・証拠追跡、D02の実ACTIVE別主体、D07のAudit rollback、D08の有界競合・認証解放、D09の消費保持、D10の人の照合、D11／D12の既存保証と未達の区別。

今回のPASSはtest所有modelと実保存／認可／Audit接続のlocal検証に限定する。実publication・停止基盤・provider・証拠保管の耐久性／ACL／信頼元認証、worker本人認証／委譲・送信、時計健全性・本番運用値・CIは未達。歴史対象閉鎖・別publication間の論理通知抑止・照合後の原子的保護も未達を維持する。D12の消費commitを「送信してよい」と扱わず、実運用接続前に追加契約を判断する。

差分7件と本EvidenceのOwner結果レビューは承認済み（§10）。次はOwner操作またはコミット操作の事前確認でcommit固定する。段階B・通知／復旧／Level2・DoD・正式受渡し・remoteはこの結果で開始しない。

## 10. 実装・検証結果のOwner承認（2026-10-08）

Ownerは本Evidenceの「9. 差分レビュー対象・未達・次の判断」まで確認し、次の発言で結果を承認した。

> ## 9. 差分レビュー対象・未達・次の判断 まで確認し、これを承認します

これにより、段階Aのtest所有ハーネス（新規helper＋5 test class）と本Evidence、計7ファイルの限定実装・検証結果を`COMPLETE / OWNER APPROVED`とする。新規48件と関連・既存回帰のPASS、補強履歴、fixture非混入とcleanupを含む§6〜§8の記録、および§9の未達・次の判断を承認対象とする。

§9の未達は維持する。この承認は段階Bの実運用接続、通知／復旧／Level2、DoD、正式受渡し、Phase 4全体開始またはremote操作の承認ではない。次は承認済み差分7件のcommit固定であり、Agentはgit add／commit／pushを行っていない。コミット操作はOwnerが行うか、操作前にOwnerへ確認する。

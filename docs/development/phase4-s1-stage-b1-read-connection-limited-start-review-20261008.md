# S1 B-1：Tooling読取接続方式・限定開始票案（2026-10-08）

**状態：OWNER APPROVED／条件付き限定開始（2026-10-08、§11）。** §1〜§8の具体方式・補強差分・検証集合・副作用・上限・担当・開始／停止条件は承認済み。承認文書commit・clean source固定後のpreflightと、成立時の対象作成・検証に限定する。実装・検証結果の受入は別判断。

**確認source：** `feature/phase4-s1-reference-foundation` / `08f47402e470d72dc8538452f5766331bb4dc323`。作成前には段階B文書3件の未commit差分あり。今回source読取と文書整理のみ。環境到達性・artifact／image cache・資源はpreflightで別途確認する。

**Ownership／対象：** 実供給元・読取クライアント・独立証拠保管はTooling-owned、非配布の`build-support/phase4-level2-verification/`。Referenceへの適用契約はnotificationが所有するが、本票はReference本体のAdapter／Port登録を変更しない。Framework／Customer／Root Reactor／正式artifactへ昇格しない。KOIKI project overview／business feature workの所有境界と既存AGENTSを適用する。

**入力：** [段階B契約・承認](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)、[供給元具体化票](phase4-s1-stage-b-tooling-source-selection-20261008.md)、[段階A Evidence](../architecture/validation/phase4-s1-operational-evidence-harness-20261007.md)。網羅性懸念とD11／D12の未達を保持する。以下の型・path・SQL構成・数値は本票で採用を判断する候補で、今回生成しない。

## 1. 採用候補：供給元所有ビュー＋最小権限JDBC

同一端末の使い捨てPG17内に、既存Toolingのpublication／provider保存と、B-1専用の観測・binding・証拠schemaを分離する。供給元が所有する読取ビューを契約とし、**読取クライアントはビューだけをSELECTできるrole**でJDBC接続する。既存内部tableの構造をconsumerの契約にしない。

HTTP方式は接続先の認証・server・依存／timeout等が増えるため、初回候補から外す。JDBC方式でも接続先の同一性、credential管理、role・対象隔離を検証する。loopback接続のローカルfixtureに限定し、分散環境のTLS・本番credential運用のPASSにはしない。

```text
Tooling通常子JVM（既存ProbeApplication／ProviderStub）
  → Tooling publication・provider記録
Tooling採取主体（manifest・厳密識別・process観測）
  → 専用binding／観測snapshot・失効記録
  → append-only証拠保管（別schema）
供給元所有の読取ビュー
  → Tooling読取クライアント（SELECT専用role、JDBC）
  → 対象・鮮度・証拠・UNKNOWNの照合結果／sanitized Evidence
```

Reference本体は通常無効・未接続拒否を維持する。B-1読取クライアントはReference向け契約の候補を検証するToolingであり、実Reference Adapter接続完了とは呼ばない。B-2では本票の結果を入力にReference-owned AdapterとPort登録・必要な契約差分を個別判断する。

## 2. 供給契約・snapshot・保管の候補

| 項目 | 本票の方式候補／拒否条件 |
|---|---|
| 契約値 | environment・publication・event・listener・attempt、供給元識別、source revision、適格性、観測／有効期間、非秘密の通知key・payload識別・宛先識別、証拠参照。操作可否とは別の観測値として返す |
| 正式な対象識別 | publication IDで1行を選び、event typeと厳密に復元したevent ID、listener、attemptを照合。bindingはfixture生成前に確定し、別publication・別環境・差替えを拒否。JSON LIKE・件数だけの対応付けなし |
| revision／一貫性 | 供給元所有の単調な観測世代＋採用した元rowの識別を保持。別の採取transactionで一貫したsnapshotを作る。読取は一つのread-only transactionで参照し、現行rowとの不一致・観測世代差異・欠落・期限で拒否。独自世代をModulithの原子的revisionと説明しない |
| 適格性 | 原status・attemptと観測分類を記録。FAILED・古さから再実行可を算出しない。正常読取とは正しい観測／拒否結果の取得で、消費肯定ではない |
| provider binding | 既存stub受理行のevent／idempotency key・受理行IDを、生成前bindingの通知key／payload識別／宛先識別と結び付ける。同key異内容・宛先差異・重複行・観測矛盾を拒否。bindingだけで既存stubにpayload検査機能があるとは主張しない |
| provider分類 | 受理行の一致はACCEPTED観測。行なしは既定UNKNOWN。静止したfixture内で未受理を確認する枝では、全該当process終了・採取範囲・遅延要求不在のfixture条件を併記する。実providerの未受理証明／安全な再送へ昇格しない |
| 停止観測 | harnessが起動した全子processのProcess handle・起動時刻・run世代・JAR／source識別を台帳化。終了待機完了／生存・制御喪失・不明を採取。強制終了はdrain完了と区別し、停止確認fileは補助入力だけ |
| 継続制御 | harnessが子process起動を所有するrun内の管理世代を用いる。再起動・移譲・台帳喪失で失効。台帳外のprocess不在や第三者の再起動抑止は本票では保証しない。最後の照合後の変更窓は保持 |
| 独立保管 | 別schemaにimmutable本文と一意key、採取run・主体・対象・観測世代・期限を保存。失効は別append-only記録。同key同本文の再照会は同じ参照、異本文は拒否。保存完了前に有効参照を返さない |
| 保持・再起動 | 同じDBを保持して採取／読取JVMを終了・再起動し、一意追跡・本文不変・失効を確認。DB container削除後の保全・本番backup／災害復旧は対象外。消費との対応はB-2で実接続する |
| ACL・真正性 | SELECT専用reader、採取INSERT主体、元fixture writer、setup管理者を区別。readerはraw tableと書込みを拒否。証拠本文はwriterのUPDATE／DELETE権限を与えず、必要な変更拒否をDB側でも検証。DB管理者／同OS利用者の改変能力は限界として記録し、digest単独を作成者認証にしない |

ビューの正確な名前・column・canonical本文は実装前に本票とcaseへ対応付ける。readerに拒否情報や秘密payloadを露出しない。期限・失効の照会には有界timeoutを適用し、取得不能時にcacheの肯定値へfallbackしない。

## 3. fixture準備の副作用と役割

初回fixtureは1環境・1論理通知を基本とし、負例用の別publication／環境・宛先は有限test値で生成する。既存UUIDをReference通常設定へコピーしない。

1. 使い捨てDBを作成し、既存Tooling migrationを変更せず適用。B-1追加schema・ビュー・roleをtest専用SQLで作る。
2. fixture manifest・bindingを確定後、既存Toolingの通常子JVMを起動し、承認イベント1件とstub受理のある枝／ない枝を準備する。B-1 readerはsend・resubmitを呼ばない。
3. 生存観測または当該子JVMだけの終了待機を行い、snapshotと証拠を先行保存。自動再送を無効にしたrunで採取・再起動枝を管理する。
4. readerを起動／照会し、実DB role・対象・期限・矛盾・保管／失効を確認。終了後に当該JVM／DB／port・一時秘密をcleanupする。

起動・イベント発行・stub DB INSERT・子JVM終了・観測／証拠保存は、読取とは別の準備副作用として本票の承認対象候補に含める。実外部送信、既存常設container停止、既存業務DB操作、復旧resubmit、Reference issue／consume／closeは含めない。既存PL2回帰testが内部で行うresubmitは§6の選択test内だけで、B-1 readerへ機能追加しない。

**担当案：** Ownerが方式・範囲・残存リスク・結果受入を判断し、限定環境管理責任を兼務する。個別開始承認後、Agentが本票の当該fixture作成・起動停止・隔離DB操作・cleanupを実行する案とする。UNKNOWNの再送／閉鎖判断は人の責任を保持する。この分担・操作委任を本票の判断対象に含める。

## 4. 作成・変更path候補

起点は`build-support/phase4-level2-verification/`。すべて新実装はtest scope・非配布。以下は候補名で、sourceには先行追加しない。

| path候補 | 責務 |
|---|---|
| `src/test/java/org/koikifw/buildsupport/phase4/b1fixture/B1ReadContract.java` | immutable観測・参照・拒否分類のtest契約。Framework／Reference Public APIではない |
| 同package `B1SourceCollector.java` | manifest・元publication／providerの厳密照合、snapshot先行保存。供給元側のraw参照をここへ限定 |
| 同package `B1ProcessHarness.java` | 既存Tooling通常processの有界起動／終了、全子台帳・世代・失効・cleanup。既存ProcessCrashRecoveryITは書換えない |
| 同package `B1EvidenceLedger.java` | 別schemaの一意登録・本文不変・失効・先行保存・再参照 |
| 同package `B1JdbcReadClient.java` | ビュー限定role、対象・鮮度・世代／binding照合、timeout、UNKNOWN／拒否。Reference Serviceを呼ばない |
| `src/test/resources/s1-b1/read-source.sql` | 専用schema・契約ビュー・role／grant・証拠不変制約。既存V1／V2・Reference V4は変更しない |
| `src/test/java/org/koikifw/buildsupport/phase4/B1TargetReadTest.java`ほか§5の6 class | 実DB／実process境界の新規検証 |
| `README.md` | test専用入口・対象・限界・cleanupへの導線を追加 |
| `docs/architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md`（Repository root起点） | source・case・実効制限・raw・未達・cleanup・Owner結果レビュー |

補助5＋test6＋SQL1＋Evidence1の新規13件、既存README1件の計14件候補（承認文書は別）。不足して追加helper／process entryが必要なら差分と上限への影響を提示する。main／POM／依存／Root Reactor／workflow・既存test／既存migration・Reference main／Port／Security／通常propertiesは変更しない。既存通常JVMは既存Tooling JARを起動し、新fixture型を正式JARへ含めない。

## 5. 新規6 class／52 invocationの候補

IDをJUnit methodへ対応付ける。下表はcoverage候補で完全性を認定しない。正常読取・正しい拒否・既知未達観測の結果を区別する。

| class／ID | case候補 |
|---|---|
| B1TargetReadTest／T01〜T10（10） | 厳密一致、同event別publication、別環境、別event type／ID、別listener、別attempt、削除、不適格／revision変化、未来／期限一致、供給不能／古いsnapshot拒否 |
| B1ProviderReadTest／P01〜P08（8） | 受理行一致、行なしUNKNOWN、応答喪失の不明保持、矛盾／重複行、同key同binding一意追跡、異payload／宛先拒否、観測／key期限、別通知のreceipt混同拒否 |
| B1StopObservationTest／S01〜S08（8） | 全当該子終了観測、生存／強制終了とdrainの区別、別process世代、manifest／source差異、期限一致、観測後再起動、台帳／制御喪失、停止file／復旧lockだけの拒否 |
| B1EvidenceLedgerTest／E01〜E10（10） | 正常先行保存、一意再照会、異本文拒否、参照欠落／曖昧、tuple差異、主体／run差異、失効／期限、保存失敗、writer UPDATE／DELETE拒否、JVM再起動後の本文・参照保持 |
| B1ReaderPrivilegeTest／R01〜R08（8） | view SELECT、raw table拒否、INSERT／UPDATE／DELETE拒否、別schema／環境拒否、誤credential／供給元差異、statement／connection timeout、失効照会不能、秘密・source非混入 |
| B1ReadConnectionIT／I01〜I08（8） | 子JVM→元DB→collector→view→readerの実接続、stub受理後終了と一意追跡、未受理と全子終了のfixture条件、採取中変更拒否、最後の読取後変更の未保護観測、collector失敗で有効参照なし、reader再起動／DB保持で追跡、異常終了でも当該process／DB cleanup |

総計は**52件（10＋8＋8＋10＋8＋8）**。invocation増減はcase対応と理由・予算影響を記録し、必須枝を落とさない。複数負例を1 methodで扱う場合も枝をEvidenceで識別する。

I05はD12の未保護窓の観測であり、原子的保護を追加したPASSにはしない。消費・歴史閉鎖・別publication間の横断抑止は本票で実装せず、B-2への未達として保持。stub受理・停止観測・JVM再起動後保持の各PASSも、本番provider／分散fencing／災害復旧保証へ拡張しない。

## 6. 回帰・package・記録

Toolingは`jdbc` profileだけを選択し、新規52件と既存`PublicationRecoveryTest`4件、`ProcessCrashRecoveryIT#incompletePublicationIsDeliveredAfterProcessRestart`／`#acceptedSendBeforeCrashIsNotDuplicatedAfterRestart`の2件を別集合で実行する。既存IT選択は6件のPL2回帰内だけ。自動でPL2全suite・JPA profile・V4規則採用へ拡張しない。選択method・実際のinvocation数をsourceとXMLで確認する。

Referenceは段階A48件・scope／TTL40件・初回55件を関連集合、既存25 class／99件＋package済みE2E1件を回帰集合として維持する。Architecture2件は既存99件内で重複計数しない。Referenceへの接続完了をこれらの既存PASSから導出しない。

offline clean packageで既存Tooling JAR・Reference JARを固定し、JAR hash・main／既存testの不変を確認する。新B1 helper／SQL／credential・有限値が正式JARに混入しないこと、B1 package専用fixtureが既存package経路に登録されないことを検証する。

raw候補は非配布`build-support/phase4-level2-verification/target/s1-b1-read-20261008/`とReference／E2E各`target/`のrun別directory。run manifestにsource、case対応、command条件、実効資源、sanitized XML／log、cleanup、既知未達を保存する。秘密をcommand line・system-properties・logへ出さない。部分再検証と全回帰のsource履歴を区別する。

## 7. 資源・時間・数値・作業量の採用候補

| 項目 | 本票の候補条件 |
|---|---|
| preflight | available memory8 GiB・disk10 GiB以上、JDK21／Wrapper Maven、必要artifact・既存PG17／Ryuk・Chromium cache、Docker到達、classpath／profile・cleanup経路。新download／installなし。不足時は停止 |
| JVM・実行 | Maven／class逐次、JUnit並列無効、fork1／reuseForks=false、各JVM heap768 MiB。B-1はtest fork＋通常子1＋Mavenの最大3 JVM、子process同時1。再起動枝も前子終了後。既存選択PL2 ITの各子も同じ上限を実効確認 |
| DB・接続 | 同時DB1、memory1 GiB／CPU1／max_connections16。子pool2／idle0、採取writer2／idle0、reader2／idle0、管理2の総計8以内。証拠保管は同DB別schema・採取writerから利用。役割別追加poolを無断で増やさない。関連・回帰は各既存Harness条件、E2E child4／idle1 |
| timeout | DB lock／statement各10秒、接続／pool待機10秒。採取・読取transaction10秒候補。JVM起動準備60秒、終了待機10秒、Future／latch待機10秒。全JVM／外部I/Oの強制終了保証とは区別し、timeout後の当該process終了を確認 |
| fixture時間値 | 観測有効期間60秒、key binding保持10分をtest値として明示。期限一致拒否、Clock注入の境界testと実UTC観測を区別。同端末基準時計、source間差1秒超を拒否するtest枝。これらは本番値・時計健全性の認定ではない |
| 検証時間 | 新規各class15分・新規52件計90分、選択PL2回帰6件30分、Reference関連143件60分、既存99件＋E2E60分、全実経過240分以内（cleanup含む） |
| raw・再実行 | 今回の全run合計1 GiB以内。同原因・同条件rerun最大1回。初回失敗保全、原因未確認の反復なし。資源不足では残時間があっても次classを開始しない |
| 作業量 | fixture・SQL・test・Evidence作成／検証16〜28標準時間候補。12時間相当で進捗確認、28時間相当で停止・再評価。Owner review・環境承認待ちは別枠 |
| cleanup | classごとに全当該子・Executor・DB／Ryuk・pool／portを確認。既存process／停止containerを操作しない。保存／再起動caseではDBを保持する区間をmanifestで明示し、最後に終了する |

既存ProcessCrashRecoveryITのlaunchではheap指定がないため、本票の選択回帰も子JVMへ限定的にheap／pool設定を渡し、実効確認する実行方法をpreflightで具体化する。既存test sourceを無断変更しない。設定が届けられない・上限が実証できない場合は停止して差分reviewへ戻す。

heapはprocess全memory、CPU1は端末全process、各timeoutは全実行経路を制限するものではない。role・実container設定・DB SHOW・JVM／pool・連続資源sampleと限界をEvidenceに残す。

## 8. 開始・停止・戻し方

**開始条件：** Ownerが本票のJDBC／view方式、Tooling test限定差分、fixture準備副作用・役割／操作委任、新規52件と選択回帰・既存保護、上限、既知未達、停止／cleanup条件を個別承認する。必要なAGENTS正本導線にこの対象を記録する。Owner操作またはコミット操作への事前確認で承認文書をcommitし、clean sourceを固定する。その後、承認済みpreflightと成立時の対象作成・Maven／隔離Docker・当該子JVM検証へ進む条件付き開始案。

既存PL2・段階Aの包括許可を今回の新schema／role・process準備・検証集合へ流用しない。今回の限定開始判断で対象を明示する。実行環境の権限付き承認要求に従い、拒否を迂回しない。副Agentは使用しない。

**停止条件：** Reference／Tooling main・既存test／migration・POM／依存の変更が必要、role／対象隔離不成立、供給元識別・snapshotの一貫性／保管一意性不足、肯定fallback／UNKNOWN解消の推測、想定外の副作用・台帳外process、秘密出力、回帰失敗、資源・時間・作業量上限到達、cleanup失敗。必要差分・根拠・残存リスクを提示して再reviewする。I05の既知窓は未達観測として扱い、それ以外の安全不足を肯定testで覆い隠さない。

**戻し方：** 読取・採取を止め、当該子JVM・pool・Executor・使い捨てDBを終了し、sanitized rawを保全。未commit差分の扱いはOwnerと確認する。無断reset／delete・既存保存データ／Audit削除・down migration・履歴改変・一律再送を行わない。

## 9. Owner判断欄・次の工程

**未判断。** §1〜§8について承認／条件変更／保留を記録する。本票は方式・限定開始条件を具体化した段階で、code／SQL作成・Maven／Docker・process操作はまだ開始していない。

個別承認後は承認記録・AGENTS導線を反映し、文書commit／clean source固定後にpreflightへ進む。同じ承認範囲の開始許可を再要求しない。実装・検証結果は新EvidenceでOwnerレビューする。B-2のReference Adapter・肯定Port登録／消費・閉鎖、送信／復旧／Level2本体・正式受渡し・DoD／remoteは別判断。

今回の変更は開発文書のみ。git add／commit／pushは行っていない。

## 10. Owner確認・限定検証と記録の方針（2026-10-08）

Ownerは本票を確認し、次の意向を示した。

> この案は、もちろん実運用に耐えうる想定あるものの、これら範囲明確にした検証そのものに意味があると思いますので、記録という面でもしっかり整えて進めておきます。

実運用を見据えて、範囲を明確にした検証と追跡可能な記録を重視する方針として反映する。B-1の結果では、次の事項をEvidenceで区別する。

- 確認したsource・供給元・環境・role・実効上限と、実行したcase／枝・runを対応付ける。
- 正常読取、正しい拒否、既知未達の観測を分け、各PASSが保証する範囲を記載する。
- snapshot・証拠本文・失効・JVM再起動後の追跡について、実測で確認した条件と未検証の条件を残す。
- 新たに判明した不足を根拠・契約ID・追加検証枝・残存リスク・必要な判断とともに記録し、網羅性の懸念を継続して扱う。
- 部分再検証・補強のsource履歴、失敗・変更理由、raw参照、cleanupを保全する。
- B-2接続と実案件の採用へ引き継げる事項、追加review／実証が必要な事項を整理する。

実運用を見据えた設計意図と、B-1の限定検証で実証する保証を区別する。実運用受入・配送保証・DoD達成をこの確認発言だけで成立したとは扱わない。

本発言には§8の準備副作用・操作委任・上限を含む条件付き限定開始への明示的な承認表現がないため、Owner確認と記録方針を記録し、個別開始の承認は確認待ちとする。§9の未判断記載はレビュー案提出時の状態として保持する。承認が確認できた後は承認記録・AGENTS導線を反映し、Ownerによる文書commit・clean source固定後にpreflightへ進む。同じ範囲の承認を繰り返し要求しない。

## 11. Owner承認・条件付き限定開始（2026-10-08）

Ownerは確認に対し、次の発言で明示承認した。

> はい、§1〜§8の条件付き限定開始の承認として記録してOKです

§1〜§8を提示内容で採用する。供給元所有ビュー＋SELECT専用roleのJDBC、Tooling test所有のcollector・process台帳・独立schema証拠保管・reader、候補path、新規6 class／52件、選択PL2回帰6件とReference関連143件・既存99件＋E2E、資源・時間・作業量・raw・再実行上限、既知未達、停止／戻し／cleanup条件を承認対象とする。§9の未判断と§10の確認待ちは過去の状態であり、今回の承認で解消した。

§3の使い捨てDB・専用schema／roleの作成、有限binding・fixture準備、当該Tooling子JVMの起動・イベント発行・stub受理／当該終了待機・snapshot／証拠保存を限定副作用として含む。Ownerは限定環境管理責任を兼務し、Agentによる本票内のfixture作成・隔離DB／process操作・cleanupを、開始条件成立後に委任する。未知状態の再送・閉鎖判断は人の責任を維持する。

承認文書と必要AGENTS導線をOwner操作またはコミット操作への事前確認でcommitし、clean sourceを固定した後のpreflight、および成立時の対象code／test／SQL・Evidence作成、Maven／隔離Docker・当該子JVM検証を条件付きで承認する。条件未成立で作成・検証を先行しない。環境の権限付き承認要求と本票の停止条件に従い、同じ範囲の開始許可を再要求しない。

Reference／Tooling main・既存test／migration・POM／依存・Root Reactor・Security・通常properties・CIは変更しない。B-2のReference Adapter／肯定Port登録・issue／consume／close、外部送信／復旧runner／Reference Level 2・実案件の運用受入・Phase 4全体・DoD・正式受渡し・remoteは承認対象に含めない。§10の記録方針、網羅性懸念、D11／D12・分散保証等の未達を保持する。

Agentは今回、承認記録・AGENTS導線だけを反映し、git add／commit／pushを行わない。実装・検証結果は別途Ownerレビューする。

## 12. preflightで判明したprofile・実効制限の差分案（2026-10-08、Owner確認待ち）

Ownerの文書commit `df3b653651e29dde9c288fb02f5b1c0a44cf1dab`をclean sourceとして固定し、[preflight記録](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md)の環境・cache・offline compileを確認した。次の不足が判明したため、§8の停止条件に従いtest作成・実行を開始していない。

- `jdbc`単独では既存S1 test sourceのtest依存が不足する。既存`jdbc,s1-contract,s1-web` profileによるoffline test-compileは成功。publication storeはjdbc、新依存追加なし、実行対象testは§6の選択集合のまま。profile指定にこのtest-only依存条件を補足する案とする。
- 選択した既存`PublicationRecoveryTest`／`ProcessCrashRecoveryIT`は、container memory／CPU・DB max_connections／timeoutを設定していない。子heap／poolの外部指定だけではcontainer条件を満たせない。既存test変更禁止のまま選択回帰の上限を成立させられる、という開始票の前提に不足があった。

**最小訂正案：** Toolingの既存test2ファイルに、B-1資源制限を明示選択した場合だけ働くtest-only設定導線を追加する。新test helper `src/test/java/org/koikifw/buildsupport/phase4/b1fixture/B1ResourceLimits.java`を候補とし、container作成前のmemory1 GiB／CPU1、DB max_connections16・lock／statement timeout10秒、当該子heap768 MiB・pool2／idle0・接続待機10秒を設定・実効確認する。既存test methodのassertion・件数・通常の既存起動条件は保持する。管理接続を含むconnection総計8、子同時1、timeout後cleanupも確認する。

追加変更対象は既存`PublicationRecoveryTest.java`／`ProcessCrashRecoveryIT.java`の資源設定・launch部分と上記helper1件だけ。新B-1 fixtureにも同じ上限を適用する。§4の差分候補14件に3件を追加し、計17件候補（承認文書別）となる。main／POM／依存／既存migration・Reference本体を変更しない。

新規6 class／52件・選択回帰6件・関連143件・既存99件＋E2E、資源・時間・作業量・raw上限、未知状態HOLD・D11／D12の未達を維持する。profile補足と既存testの最小資源設定差分についてだけOwner判断を求め、§11の他の承認範囲を再要求しない。承認前にhelper・既存testを変更せず、文書訂正・承認commit後のclean source再固定とsource／環境差分確認へ進む。

## 13. §12訂正のOwner承認（2026-10-08）

Ownerは§12の訂正案を確認し、「了解承認いたします」と明示した。既存test-only依存を含む`jdbc,s1-contract,s1-web` profileの補足と、既存`PublicationRecoveryTest.java`／`ProcessCrashRecoveryIT.java`の最小資源設定・launch差分、test-only `B1ResourceLimits.java`1件の追加を採用する。§12の確認待ちは提出時の履歴であり、承認待ち条件は解消した。

既存test methodのassertion・件数・通常起動条件を保持し、B-1明示選択時だけ§12の上限を設定・実効確認する。候補差分17件（承認文書別）、新規6 class／52件・選択回帰6件・関連143件・既存99件＋E2E、§7の上限と§8の停止条件を維持する。§11の「既存test変更なし」は、この承認済み2ファイルの最小差分だけを例外とする。main／POM／依存／既存migration・Reference本体は変更しない。

訂正・承認・preflight記録とAGENTS導線を文書commitし、clean sourceを再固定してsource／環境差分を確認する。その後、実効制限の成立確認に必要な承認済み3ファイルの作成・変更と限定検証を進め、制限成立前に新規52件のハーネス作成・検証を先行しない。確認済みのcompile／cache情報は保持し、変更や追加懸念のない確認を無条件に反復しない。

この承認でpreflight完了・実効制限PASS・新規52件PASSを認定するものではない。実効値・cleanupと成立判定はEvidenceへ記録する。同じ訂正範囲の開始承認を再要求せず、実装結果の受入、B-2・実運用・DoD・remoteの別判断を維持する。Agentは今回文書だけを反映し、git add／commit／pushを行っていない。

## 14. 実効確認でのruntime profile訂正案（2026-10-08、Owner確認待ち）

§13の承認文書commit後、clean `fad5ded`とsource／環境差分を確認し、承認済み3ファイルの資源設定を作成した。[Evidence§7〜§8](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#7-訂正後source資源設定作成限定検証2026-10-08)に実効確認と失敗を記録した。

既存PublicationRecoveryTest4件の資源assertionは成功したが、compile用`jdbc,s1-contract,s1-web` profileの依存が実行時にも入り、KOIKI Data Starter等のautoconfigurationが既存PL2のmigration選択へ影響した。probe schemaが作られず4件がERROR。§8の回帰失敗停止条件に従い、子JVMのIT2件・新規52件は未実行。同条件の再実行はしていない。

**訂正案：** `jdbc,s1-contract,s1-web`は既存test sourceのoffline test-compile専用とし、PublicationRecoveryTestと選択IT2件のruntimeは`jdbc`単独に戻す。compile済みtest-classesに対する`-Pjdbc surefire:test`と`-Pjdbc failsafe:integration-test failsafe:verify`の直接goalで対象を限定する候補。通常lifecycleのtest-compileを再度jdbc単独で呼んで同じcompile不足を再現しない。Tooling JARもjdbcのmain依存でpackageし、test-only support依存がruntimeへ混入していないことを確認する。

codeの許可差分3件・検証集合・上限は変更しない。追加POM／main／migration／業務assertion修正・test除外で既存件数を減らす案ではない。失敗を保全し、修正条件で既存4件を再検証して正常化と実効制限を確認し、成立時に選択IT2件へ進む。profile条件の変更と失敗からの再開についてだけOwner確認を求め、他の承認済み範囲を再要求しない。

## 15. §14訂正のOwner承認（2026-10-08）

Ownerは§14の訂正案を確認し、「案を承認します」と明示した。`jdbc,s1-contract,s1-web`を既存test sourceのoffline test-compile専用とし、既存PublicationRecoveryTest4件と選択ProcessCrashRecoveryIT2件のruntimeを`jdbc`単独とする案を採用する。compile済みtest-classesに対するSurefire／Failsafe直接goalと、jdbcのmain依存によるTooling JARのpackage・runtime依存確認を承認する。§14の確認待ちは提出時の履歴であり、今回の承認で解消した。

§13で要求した承認文書commit・clean source固定は`fad5ded`で実施済みである。今回の承認を記録し、既存code3件・source／環境の差分を確認した後、失敗runを保全して訂正条件で既存4件を再検証する。正常化と実効制限を確認できた場合に選択IT2件へ進む。source不変・追加懸念なしのcompile結果は再利用し、同じ訂正承認を再要求しない。

codeの許可差分3件、既存assertion・件数・通常起動条件、検証集合・上限・cleanup・停止条件を維持する。main／POM／依存／migrationの追加変更は含めない。実効制限成立前に新規52件を作成・検証しない。失敗解消・preflight成立・結果受入・B-2／実運用／DoD／remoteの承認を意味しない。local commitは引き続きOwner操作または操作前のOwner確認とする。

## 16. 子JVM起動失敗の診断保全・再開案（2026-10-08、Owner確認待ち）

§15承認後、code3件のhash不変と環境を確認し、jdbc単独のpackageと直接Surefire goalを実行した。既存4件はPASSし、schema欠落が解消した。一方、選択IT2件はともにhelperの`B1 child startup failed`でFAILUREとなった。[Evidence§10](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#10-jdbc単独での限定再検証結果2026-10-08)に結果を記録した。§8の回帰失敗停止条件に従い、同条件rerun・新規52件へ進んでいない。

子logは既存ITのfinallyで削除された。現在のassertionは「子生存かつStarted ProbeApplicationを観測」をまとめて判定しており、終了理由・起動logを保全していないため詳細原因は未確定。timeoutを緩める、業務assertionを変える、起動判定を省く、といった対処を先行しない。

**最小訂正案：** test-only `B1ResourceLimits.java`の失敗診断だけを補い、既存ITのcleanup前に当該子PID・終了状態・待機経過と、credential・接続情報を除去した子logを非配布run directoryへ有限量で保存する。子終了確認と元log／work directoryのcleanupは維持する。main／POM／migration／既存業務assertion・件数・資源上限を変更しない。

Owner採用後、診断差分を確認して選択IT2件だけを限定再検証する。原因が確定して追加修正が必要な場合は、その差分・根拠を別途提示し、原因不明の反復をしない。同原因・同条件rerun上限、raw上限、停止条件を維持する。既存4件は今回PASSを保持し、追加変更が影響する場合だけ再検証を判断する。診断保全差分と失敗からの再開についてOwner判断を求め、採用前にhelperを変更しない。実効制限全体・preflight・新規52件・結果受入は未成立のままとする。

## 17. §16診断保全のOwner承認（2026-10-08）

Ownerは「診断保全の変更と再検証について、最小訂正案を了解承認します」と明示した。§16のhelper限定診断保全と選択IT2件の再検証を採用する。同時に、これまでの訂正が場当たり的でないかを経緯に沿って点検するよう依頼した。点検は文書・実コード差分・実行記録の照合とし、診断保全以外の追加実装は含めない。

§13のclean source `fad5ded`、§15のruntime訂正、既存4件PASSを引き継ぎ、source／環境差分確認後にhelperの診断だけを変更・compileし、jdbc単独で選択IT2件を実行する。元の失敗と今回のrunを分けて保管し、子終了・元log cleanup・資源上限・assertion・件数を維持する。追加原因修正は必要差分を提示して別判断とし、新規52件・結果受入・実運用・remoteへ拡張しない。Agentはgit add／commit／pushを行わない。

## 18. 点検記録のOwner確認と原因調査の続行（2026-10-08）

OwnerはEvidence§12を確認し、「整理を納得し、作業を続けます」と述べた。点検の整理を確認済みとして記録する。実効制限成立や検証結果受入の承認とは扱わない。

続行は既存の診断保全と原因調査の範囲とする。JAR／classpath・main class／resourceと、cachedライブラリの接続取得経路をread-onlyで照合し、保持接続の内訳が未確定のため、helperの明示選択時だけHikari取得stackの診断（leakDetectionThreshold2000 ms）とFlyway DEBUGを追加する。leak警告は接続を2秒以上保持した取得stackの観測であり、接続漏れの断定ではない。失敗logの有限量保全は先頭16 KiB＋末尾48 KiBとして起動時のstackを残し、credential除去・元log cleanupを維持する。

診断条件を変えた選択IT2件の限定確認を1回行い、同じ条件の反復をしない。pool2・接続待機10秒・DB／JVM上限・migration・業務assertionを変更しない。追加の原因修正は差分と影響を整理して別判断とし、新規52件・main／POM等へ進まない。

## 19. 保持接続の残る1本を特定する診断案（2026-10-08、Owner確認待ち）

§18の診断でFlyway初期接続1本の取得stackと返却を確認したが、timeout時のactive2のうち残る1本は未特定。既存4件とJAR内の共通依存77件・main class／resourceのhashは一致した。これは設定・起動経路の同値証明ではない。詳細は[Evidence§13](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#13-owner点検確認後の接続内訳調査2026-10-08)に記録し、追加実行を停止した。pool3必要・恒久的なconnection leakとは判断しない。

次の診断はhelper1件に集約し、同じ情報不足による反復を避ける。明示選択時のpackage logger `com.zaxxer.hikari`と`org.flywaydb`のDEBUGを確実に指定し、子poolの実設定・追加／貸出／返却のlog、既存の2秒保持stackを保全する。起動待機中に1回だけ管理接続1本で`pg_stat_activity`の接続数・state／wait種別と既知SQLの分類を観測する候補とする。credential・SQL本文／bind値は保存せず、fixture DB限定・query timeout10秒・接続はfinallyで閉鎖する。管理2枠の内1枠を使い、追加poolや常時collectorは作らない。子logは先頭16 KiB＋末尾48 KiBの有限量保全・sanitizationを維持する。

この診断条件で選択IT2件を1回だけ確認し、取得stack・DB状態・pool実値・migration進行を同じ時系列で照合する。記録保存失敗・接続予算8超・回帰失敗・cleanup失敗で停止し、原因修正は別判断とする。pool／timeout／migration locking・Flywayの有効状態、既存業務assertion・件数、main／POM／依存、既存4件PASS、新規52件開始条件は変更しない。採用前に診断追加と再実行を先行せず、同条件の反復は行わない。

## 20. §19採用承認と判断に必要な情報への集中（2026-10-08）

Ownerは「診断追加案§19の採用を承認します」と明示し、「原因追及が目標ではありませんので、とりえる情報から得られる情報に集中してください」と指示した。§19のhelper限定診断・選択IT2件1回・資源／cleanup・既存条件維持を採用する。保持元の完全特定を完了条件とせず、取得できたpool実値・migration進行・DB状態から実装判断に使える事実と未確定事項を整理する。

前回のsource・JAR・既存4件PASSと失敗rawを保持し、source／環境差分確認後に診断追加をcompile・実行する。診断条件の同一反復や追加の原因探索を自動で行わない。失敗時も採取結果と適用できる選択肢を提示し、資源上限・起動方式等の変更は具体的な差分・根拠を別判断へ戻す。新規52件・結果受入・実運用・remoteへ拡張せず、git add／commit／pushを行わない。

## 21. 子pool設定供給の限定訂正案（2026-10-08、Owner確認待ち）

§20の1回限定診断では、両子のHikari起動logがmaximumPoolSize10／minimumIdle10／connectionTimeout30000 msを示した。helperは2／0／10000 msをenvで要求しているが、起動logの初期値と一致していない。失敗時は約10秒の接続取得timeout、起動中1回のDB sampleは子のidle接続2本＋管理1本の計3だった。保持元の完全特定を続けるより、設定供給を既存4件のcanonical property指定へ揃え、pool初期化時から採用済み値であることを確認するのが次の実装判断となる。[Evidence§14](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#14-19採用承認後の1回診断判断に使える結果2026-10-08)へ記録した。

**限定訂正案：** test-only `B1ResourceLimits.java`の明示選択時launchだけで、現行のHikari数値env供給を削除し、`--spring.datasource.hikari.maximum-pool-size=2`／`--spring.datasource.hikari.minimum-idle=0`／`--spring.datasource.hikari.connection-timeout=10000`のcanonical propertyを子commandへ渡す。診断のleakDetectionThreshold2000も同方式へ揃える。数値は変更せず、URL／username／passwordはenvに維持してcommand lineへ出さない。loggerのpackage指定・有限量のdiagnostic／cleanupを維持する。transaction timeoutやFlywayの有効状態／locking、main／POM／migration・既存業務assertion・件数は変更しない。

起動logの最初のpool初期化で2／0／10000を示すこと、正常起動時VM flags・pool確認、選択IT2件の業務assertionとcleanupを採用済み上限内で確認する。初期logに10／10／30000が残る場合は、後段の値やPASSだけで成立としない。新しい供給条件でIT2件を1回だけ実行し、情報が足りなければ条件未成立として結果・選択肢を整理する。追加原因探索や資源増量を自動で行わない。既存4件PASSは保持し、差分により影響が生じた場合だけ回帰を判断する。

canonical指定が初期化前の適用と起動正常化を保証することはまだ実証していない。これは根本原因を断定した案ではなく、観測できた設定不一致に対応する限定案である。採用前に供給方法の変更・再実行を先行しない。code3件・文書3件の差分枠を維持し、新規52件・結果受入・実運用／remoteは別判断とする。

## 22. §21限定訂正・検証のOwner承認（2026-10-08）

Ownerは§21について「限定検証へ進めることを承認します」と明示した。helperの明示選択時launchに限り、Hikari数値のenv供給をcanonical command propertyへ揃え、起動時からの実値・VM flags・選択IT2件とcleanupを1回の限定検証で確認する。§21の確認待ちは履歴として保持し、今回の承認で解消した。

数値・URL等の秘密のenv供給・transaction timeout・Flyway有効状態／locking・既存業務assertion／件数を維持する。source／環境差分確認とoffline compile後、jdbc単独の選択IT2件を実行する。初期値不一致が残る・回帰失敗・上限超・cleanup失敗では停止し、得られた情報と実装判断を整理する。原因追及の反復・未承認の追加修正・新規52件・結果受入・実運用／remoteへ拡張しない。git add／commit／pushを行わない。

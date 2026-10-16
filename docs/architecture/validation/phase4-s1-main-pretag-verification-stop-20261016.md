# Phase 4 S1：mainタグ前検証・通常root停止（2026-10-16）

状態：**初回STOPを保持／test fixture訂正後の通常root・Critical Journey・選択非同期／provider契約はPASS。専用92件の補完は未実施で、タグ前検証全体は未完了。** §1〜4は初回停止時点、§5以降はOwnerの解析・問題解消指示後の現行記録である。

Ownerの「タグ付け前にmainで可能な検証を実施し、既存の検証プログラム・シェルを活用する」という指示に基づく新しい実行である。過去のR1〜R3の検証予算・再実行枠は流用しない。初回通常rootで失敗したため後続を停止した。タグ・commit・remote操作、production／test修正、追加再実行は行っていない。

## 1. Source・準備と実行条件

- source：clean `main` / `origin/main`、`6aa4f70afc154ae1858186af46af0607cfc2ab4f`。前回統合検証source `450b6009a85024543d5e406e9a8ac9fa7aa3c297`との差分は統合検証記録1文書のみ。
- JDK 21.0.12、Maven Wrapper 3.9.16、Docker Server 29.1.3。既存のPostgreSQL 17／17-alpine・Ryuk imageとChromiumの所在を確認した。
- 通常sandboxではDocker named pipe／設定読取とCIM読取を拒否された。AGENTS.mdの承認済み手順に従い権限付きで同じ診断を行い、Docker Server応答を確認した。権限拒否をEngine障害とは扱っていない。
- 開始時Docker稼働container 0、空きmemory 13,739,480 KiB、C空き525,823,352,832 bytes。既存Java PID 46576／46884は保護対象として記録した。
- 通常rootはoffline `clean package`を1回だけ実行。Maven／test fork heap 768 MiB、fork 1、fork再利用なし、JUnit並列なし、`koiki.reference.normal-root.db-limits.enabled=true`、JAR強制作成、今回専用の`java.io.tmpdir`を明示した。実引数は`normal-root-args.json`へ保存し、MAVEN_OPTSはfinallyで復元した。
- 実行は2026-10-16 09:56〜09:59 JST、Maven実行時間3分24秒。失敗後にMaven／Dockerの再実行は行っていない。

## 2. 対象と結果

| 対象 | 今回の結果 |
| --- | --- |
| 通常root | **BUILD FAILURE / exit 1**。fresh Surefire XML 62 file、397件＝303 PASS＋1 failure＋1 error＋専用92 skip。実行305件 |
| `B2FrozenSourceRegistrationTest#C07_managedScopeTtlRemainIndependent` | **FAIL**。managed configurationを有効にしたcontextの起動で`notificationManagedSnapshot`生成が失敗し、`Managed recovery configuration invalid` |
| `ManagedRecoveryConfigurationLoaderTest#L01LoadsImmutableSnapshot` | **ERROR**。設定fixtureの初回loadで同じ`IllegalArgumentException` |
| 通常JAR固定・Critical Journey | **未実行**。root packageが成立しなかったため、今回の通常JARを固定していない。browser／Bearer API／DB／Auditの既存E2Eは開始しなかった |
| async JAR作成・静的対照 | **未実行** |
| R1〜R3の選択27 method＋provider契約2件 | **未実行**。既存Toolingの選択候補として点検したが、通常root失敗により開始しなかった |

選択候補の27 methodはR1 4件、R2 8件（診断用`normalArtifactCapabilityDenialOriginComparison`を除外）、R3-A 7件、R3-B 8件（既知FAILの元`B03aAcceptedButLostResponseCompletesWithoutSecondPost`を除外）である。元B03aのFAILを新しい人手HOLD methodのPASSへ置き換える意図はない。未実行の候補はPASS・skip件数に加算しない。

## 3. 読取照合で分かったこと・未確定の原因

失敗した2件は`ManagedRecoveryTestSupport.file/json/hash`から`ManagedRecoveryConfigurationLoader.load`へ至る設定読取を共有する。loaderはpathの実体／正規化／symlink、読取、hash、UTF-8、JSON、snapshotの検査中に生じた例外を、原因chainなしの同一メッセージへ変換する。このため今回のXMLだけでは拒否条件を特定できない。

fixtureは固定時刻から30分の有効期間を生成するが、loader／snapshot構築自体は実現在時刻による有効期限判定をしていない。日付が変わったことによる期限切れとは断定しない。前回保存XMLのL01は12件中failure／error 0であり、Java version／home／user.dirは今回と同じ。専用tmp pathはrunごとに異なる。path・権限・hash・parser等のどこで拒否したかは未確定で、製品不具合／環境要因の分類も未確定である。

次の判断材料は、この共通読取経路に限定した診断の対象・秘密非露出・実行回数と、必要時の最小訂正／限定再検証範囲である。原因未確定のままloaderの安全条件を弱めたり、testをskip・期待変更してPASS化したりしない。タグ前検証を完了とするには、問題の扱いを判断した後に通常rootと未実行の後続検証を成立させる必要がある。

## 4. 保全・cleanupと判定境界

今回の証拠directoryは`tmp/main-6aa4f70-pretag-verification-20261016/`。`preflight.json`、実引数、`normal-root.log`、終了値、`root-reports/`のXML 62 file、`root-report-index.json`、有限の資源sample、`cleanup.json`を保存した。tmp現物はGitでは移送されない。

| 主な証拠 | SHA-256 |
| --- | --- |
| `normal-root.log` | `907D297D37D2B51D055F30E1319FE76DD8E18E791A0531CB6109C97BE3323BC1` |
| `root-report-index.json` | `5353AF3ED868CFE86BD1BB61FD40F5F9B655F39E0950B2A897389B1D9F90DEB4` |
| `TEST-org.koikifw.reference.notification.ManagedRecoveryConfigurationLoaderTest.xml` | `49C8F0BA00277B8CFC45AECC482E99B23ECE3BAE746005C694EB2E7C19E23FD6` |
| `TEST-org.koikifw.reference.notification.B2FrozenSourceRegistrationTest.xml` | `0BC762A9E9A3003210DAA264CF74B734D844926160F94AEE888718D9C53E53E2` |

2026-10-16 10:00 JSTのcleanup確認はcontainer 0、Javaは開始前の保護対象2 PIDだけ。検証用Java子processの残存はなく、保護対象を停止していない。終了時の空きmemoryは13,258,684 KiB、C空き525,812,670,464 bytes。検証終了・診断後、本記録作成前のGit差分は0だった。資源sampleは連続監視ではなく、終了時0から実行中の全最大値を推定しない。

前回の[main統合検証](phase4-s1-main-r3-integrated-verification-20261015.md)のPASSは別epochの結果として保持する。今回はタグ前のfresh通常rootが不成立であり、成功したcheckpointとしてのタグ付けは保留を推奨する。既存のB03a自動完了FAIL、E08残枝、DoD 4-2／4-3、R4／R5、正式受渡しとPhase 4全体の未達・未承認境界も変更しない。

## 5. Ownerの解析・問題解消指示と原因特定

Ownerは本停止記録を確認後、「通常rootの失敗分について、解析、問題解消へ進めます」と指示した。この指示に基づく診断・最小訂正・再検証であり、初回の実行枠を流用したものではない。

1. 既存test classpathと同じ合成fixtureを使った有限Java診断では、今回専用directoryの実体path上でloaderがPASSした。
2. 元の環境と実行条件で失敗2 methodだけを選択し、JUnitの失敗時temp保持を明示した。**2件＝1 failure＋1 errorを再現**し、XMLと合成fixtureを保持した。
3. Windowsの`TEMP`／`TMP`はユーザーdirectoryを8.3短縮名で表していた。保持した2つの`assignment.json`について、その短縮path表記は絶対・正規化済み・通常fileで、bounded read／JSON parseも成功する一方、`path.toRealPath().equals(path)`がfalseとなりloaderは拒否した。**同じfile・同じ内容を実体pathで指定すると両方PASS**した。診断はfixtureの値やparser原因chainをproduction logへ出していない。

原因は、正常系test fixtureがJUnit由来の短縮temp pathをそのまま返し、loaderの実体path要求に違反していたこと。productionのpath拒否や期間判定を変更する必要はない。Mavenで明示した`java.io.tmpdir`のXML上の値だけでは、JUnitが実際に作ったtemp pathの表記を保証できなかった。

訂正はtest-onlyの`ManagedRecoveryTestSupport.file` **1 file・1か所**。合成fileを書き終えた後、返すpathに`toRealPath()`を適用する。production loader・Framework API／Rules・Security・POM／依存・migration・CIは不変更。間接path等の拒否テストは引き続きloaderへ拒否対象pathを直接渡すため、拒否条件を迂回していない。

## 6. 訂正後の検証

| 対象 | 結果 |
| --- | --- |
| `ManagedRecoveryConfigurationLoaderTest`全12件＋`B2FrozenSourceRegistrationTest`全8件 | **20 PASS、failure／error／skip 0**。短縮TEMP環境は維持。間接path・ACL／読取権限・不正JSON・manifest不一致等の既存負例もPASS |
| 通常root offline `clean package` | **BUILD SUCCESS、305 PASS＋専用92 skip、failure／error 0**。fresh XML 62 file。10:09〜10:12 JST、3分34秒。初回と同じheap／fork・並列無効・通常root DB制限・専用tmp条件で1回実行 |
| 固定通常JARでCritical Journey | **1 PASS、failure／error／skip 0**。実ChromiumとBearer API、Session承認、DB／Audit／sanitized log突合。DB memory1 GiB／CPU1／max_connections16、test／child heap768 MiB、子pool4／1の実効値を確認 |
| async JAR・静的対照 | offline clean package 1回成功。通常JARのevents-jdbc runtime 0、async JARは1。両JARのアプリケーションclass **177個ずつのentry hash差分0**。V5／V6は両方に存在し、通常起動無効の境界はR1でも確認 |
| R1正常非同期 | **4 PASS**、failure／error／skip 0 |
| R2失敗分離 | **8 PASS**、failure／error／skip 0。診断用の通常JAR拒否比較methodは対象外 |
| R3-A停止後復旧・拒否 | **7 PASS**、failure／error／skip 0 |
| R3-B受理観測・UNKNOWN／HOLD | **7 PASS**、failure／error／skip 0。元B03a自動完了FAILは選択せず、別methodの人手HOLDを明示選択 |
| provider契約 | **2 PASS**、failure／error／skip 0 |

訂正後のhelper SHA-256は`EDC3256775E26DCB962C3D0B92ADDC4E31B60A2A2F816738687F4CEDF9A7434A`。証拠は初回と同じ親directory内の`targeted-diagnostic/`（訂正前2件再現）、`retained-shortpath-diagnostic.log`、`targeted-correction/`（20件）、`corrected-root/`（root 62 XML・実引数・source fingerprint）、`critical-journey/`へepochを分けて保存した。

Critical JourneyのMaven成功直後の初回cleanup sampleにはcontainer 1があったため、後続を止めて読取確認した。そのcontainerは後続確認時に存在せず、稼働container 0・検証用Java子process 0を確認した。初回sampleは`cleanup-initial.json`へ保持し、testは再実行せず、有限の終了待ちを加えたcleanup確認後に未実行phaseだけを再開した。Critical JourneyのXML上のPASSとcleanup確認の時点を区別する。

§2の初回計画の「27件」はAgentの候補集計誤りであり、**R1 4＋R2 8＋R3-A 7＋R3-B 7＝26件**へ訂正する。R3-Bは全8 methodのうち既知FAIL 1件を除いた7件。R3-A完了後のR3-B実行前件数checkが停止し、未実行のR3-B／providerだけを訂正後に開始した。既にPASSしたtest・packageは再実行していない。provider2件を加えた実動作選択は計28件で、Critical Journey 1件と対象20件・通常rootとは別集合として集計する。

## 7. 最終証拠・cleanupと次の成果固定

- 固定通常JAR：`corrected-normal-reference-fixed.jar`、SHA-256 `D2AF9D3CFFFE505298AAAFDD4C93C1D90C975EEA8FB4CDF507A2679764E7F389`。
- 固定async JAR：`corrected-async-reference-fixed.jar`、SHA-256 `9DB7688EF2A107D74BC24E6A064556D5743F0F91DDD9279FCB8ECC5967C04BB5`。R3子processの出力hashもこの固定値に一致。
- 各`selected-<class>/`へ実method選択引数・Maven log・fresh XML・結果JSON・有限cleanup sampleを保存。集計は`selected-final-summary.json`、主要証拠のhashは`corrected-evidence-hashes.json`、class対照は`corrected-jar-app-class-comparison.json`。
- 診断で保持した2つの合成JSONは`retained-fixtures/`へ退避し、当該system-temp directoryは内容と対象を点検後、再帰削除を使わず所有file／directoryだけを除去した。
- 全phase終了後の2026-10-16 10:23:53 JST、**container 0、Javaは開始前の保護対象PID 46576／46884だけ**。所有子processの残存なし。最終helper hashは§6と一致する。

今回の通常root失敗2件は、test-only fixture訂正により解消した。タグ前に選んだ通常構成・browser／API／Auditと非同期限定経路の検証も成立した。初回FAIL・診断再現と訂正後PASSは別epochで保持し、前回の結果を上書きしない。B03a自動完了FAIL・E08残枝・DoD 4-2／4-3・R4／R5・実provider・正式受渡し・Phase 4全体の受入には拡張しない。

変更はtest helper 1 fileと本記録。commit・tag・remote操作は未実施であり、既存main `6aa4f70`自体にはこのhelper訂正はまだ含まれない。次はこの差分・結果のreviewと成果固定commitのOwner判断を経て、訂正を含むタグ対象commitを改めて確定する。tmp証拠・固定JARはGit移送対象外なので、必要な保全と所在の引継ぎを分けて扱う。

## 8. 専用92件への影響確認と補完前のsource固定

通常rootの予定skipはS1専用64件＋B2 process28件であり、今回の通常root PASSや非同期選択28件で実行済みにはならない。今回訂正したhelperの`file`を直接使う専用集合は、`ManagedRecoveryConfigurationDbTest`6件、`ManagedRecoveryConfigurationRegistrationTest`6件、`OperationalRecoveryEvidenceDbTest`12件の計24件。前節の「選んだ検証が成立」は上記実行済み集合だけの結果であり、この24件の補完を含むタグ前検証全体の完了判断ではない。

Ownerは、直接影響24件を含む専用92件について、環境を整えて既存の完走経路を確認する方針を示し、有意性を点検し深追いしないことを求めた。現行の保存・認可・Audit・許可発行／読取に対する回帰として有意と整理し、24件→残りS1 40件→B2 28件の順を候補とした。既存の有限fixtureと準備手順だけを使い、旧履歴runnerは起動せず、失敗時は保全・cleanupして停止し、自動で追加改修・依存取得やR4へ広げない。

直後にOwnerは「この専用92件に取り組むまえに、現在のテスト変更分をコミットしておくべき」と指示した。これに基づきhelper訂正と本記録の2 fileをlocal commitの固定対象とする。対象点検時点で専用92件のtest／DB／子JVMは未起動。`tmp/main-6aa4f70-dedicated92-20261016/`にあるのはsource差分・環境／資源の事前確認だけで、検証結果ではない。補完実行ではcommit後のsourceを新たに照合し、この事前確認のsource IDを実行sourceへ転用しない。タグ作成・remote操作の承認には拡張しない。

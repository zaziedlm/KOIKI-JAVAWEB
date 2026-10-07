# S1後段：対象・運用証拠ハーネスの限定検証開始票案（2026-10-07）

**状態：OWNER APPROVED / 文書source固定・preflight条件付き限定開始（2026-10-08、§9）。** Ownerは接続契約案の段階Aと本票のレビュー整理を確認し、内容を承認した。文書commit・clean source固定・preflight成立前にtest作成・Maven／Docker検証を先行しない。

**source：** `feature/phase4-s1-reference-foundation` / `6cf029e09d9c1a73550f0ef8e3f8a5996c67390d`＋未commitの後段文書。本票作成前には運用接続レビュー更新と接続契約案の2件が未commit。scope／TTL結果は[検証記録§7](../architecture/validation/phase4-s1-managed-scope-ttl-20261007.md#7-実装検証結果のowner承認2026-10-07)でOwner承認済み。

**位置／責任：** 既存Gate P4-FのS1個別限定経路。新Gateを設置しない。契約の所有者はReference `notification`、実装対象は同appのtest所有ハーネス。Architecture Ownerが採用・開始・結果受入を判断し、Codexは作成／検証を支援する。サブエージェントなし。Framework／Customer／既存業務moduleの責務は変更しない。

## 1. 今回の到達点と対象外

到達点は、現行`RecoveryTargetPort`／`RecoveryEvidencePort`へtest Adapterを接続し、対象・停止制御・provider観測・証拠保持の可変状態を照合したときの成功／拒否と、実Identity／JPA／Auditの整合を確認すること。これは接続契約案の段階Aであり、実運用Adapter接続ではない。

本番main source、Portの値契約、V4／schema／grant、POM／依存、通常properties、Framework API／Rules、Security、CIは変更しない。現在対象／証拠の本番未接続拒否を維持する。Web／CLI受付、worker認証／委譲、sender／listener／runner、publication／Modulith runtime、実配備／運用停止・provider通信、remoteも含めない。

次の三点は現行契約の制約として試験・記録し、今回解決した扱いにしない。

- 対象／証拠照合直後の変更を現行Serviceだけでは原子的に抑止できない。
- attempt変更／対象削除後の旧許可は現在対象照合で閉鎖できない。
- 同一論理通知に対する複数permitの発行を初回基盤だけでは抑止しない。

この未達の確認は想定済みの試験結果として区別する。それ以外の新たな安全条件不足、または本番接続へ進むための契約修正が必要なら停止して再reviewする。肯定testのために対象照合・認可・Auditを緩和しない。

## 2. 作成・変更path候補

pathの起点は`koiki-reference-app/`。class名は本票の候補であり、現在sourceには存在しない。

| path候補 | 責務 |
|---|---|
| `src/test/java/org/koikifw/referenceacceptance/notification/OperationalRecoveryTestSupport.java` | 有限UUID・test Clock、可変対象供給元、停止制御model、provider観測stub、証拠／失効台帳、現行Port実装とtest用Bean登録をまとめる。責務別nested型で分離し、不足なら同package内分割案を提示 |
| 同packageの既存`NotificationFoundationDbHarness.java` | 必要な場合だけ追加test source登録・context管理を最小変更。既存有限Port・55件・40件の枝を変えない |
| `src/test/java/org/koikifw/reference/notification/OperationalRecoveryTargetContractTest.java` | 現在対象の識別・鮮度・revision・失効 |
| 同packageの`OperationalRecoveryStopEvidenceTest.java` | 停止・世代・drain・移譲／再起動・制御喪失 |
| 同packageの`OperationalRecoveryProviderEvidenceTest.java` | provider分類、key／payload対応、UNKNOWN・期限 |
| 同packageの`OperationalRecoveryEvidenceStoreTest.java` | 証拠の不変性、一意追跡・失効・先行保存・閉鎖照合 |
| 同packageの`OperationalRecoveryEvidenceDbTest.java` | 実Identity／管理scope・TTL／JPA／Auditで発行・消費・閉鎖、rollbackと現行不足を確認 |
| `docs/architecture/validation/phase4-s1-operational-evidence-harness-20261007.md` | source・case対応・証拠・資源・制約・cleanup・結果レビューを記録 |

候補差分は新規7件＋既存Harness最大1件、これに本票等の開発文書を加える。別の共通test helper変更が必要なら、用途・既存集合への影響を示して対象差分を確認する。本番構成へ追加Bean／mode／propertyを作らない。

## 3. ハーネス内の契約・時間・保管

| 項目 | 今回のtest model |
|---|---|
| 現在対象 | 5項目snapshot＋revision・適格性・観測期間。現在値は可変で、別環境、削除、revision変更、障害時は肯定値なし |
| 停止証拠 | process／起動世代、deploy revision、drain／終了、移譲不在、再起動抑止世代、採取主体・対象・期間を一体で照合。復旧worker lockは独立値で停止判定を代替しない |
| provider観測 | 安定通知key＋payload識別＋受理分類＋観測期間／key保持。受理済み・UNKNOWN・矛盾・照会障害・期限切れは追加実行証拠を返さない。確実な未受理／拒否も停止等の他条件を満たす場合だけ肯定 |
| 消費証拠 | environment＋permit＋operation＋worker世代の一意keyからimmutable本文へ解決。同じkeyの異本文は拒否。同一本文の再照会は同じ証拠として返せるが、再消費の許可ではない |
| 閉鎖証拠 | permit／歴史対象／confirmedBy／resultRefから判断本文へ解決。今回の肯定は対象が現行snapshotと一致し、provider照合済み・人の判断が記録された枝だけ。UNKNOWN閉鎖を肯定しない |
| 保存・失効 | 証拠本文はimmutable、有効性／失効は別の可変管理。先行保存失敗ではPortの肯定値を作らない。rollback後は未使用証拠とし、DB消費から同じ証拠へ一意に辿れるかを確認 |
| test時間値 | 固定UTC基準＋可変Clock、証拠有効期間60秒、provider key保持10分をfixture値として使用。`validFrom <= now < validUntil`、期限一致拒否。実運用値・保持保証へ昇格しない |

採取・証拠先行保存はService transaction外で明示実行し、Port照合は短いprocess内参照だけにする。呼出し時のtransaction active／lock状態をtestで観測する。実network／file／platform通信を追加しない。非同期の故障注入にはlatch等の有界待機を使い、検証をsleep依存にしない。

証拠の保管はtest process内だけで、process再起動後の耐久性・ACL・信頼元認証・改変検知を保証しない。可変Clockは時計監視や分散時計同期の実証ではない。各testをresetし、状態・SecurityContextが別testへ漏れないことを確認する。

## 4. 新規5 class／48 invocationの候補

IDをそのままJUnit method prefixへ対応付ける。parameterized invocationを使用する場合も下記48件と照合し、複数負例の枝は記録で識別する。想定未達の観測testは、成功／拒否保証のtestと分けて記載する。

| class／ID | case候補 |
|---|---|
| Target／T01〜T10（10） | T01正常一致、T02同event別publication、T03別environment、T04別event、T05別listener、T06別attempt、T07削除、T08revision変更／不適格、T09期限境界／未来観測、T10供給障害／古いcache拒否 |
| Stop／S01〜S08（8） | S01停止＋drain＋再開制御成立、S02生存／drain未完、S03別process／起動世代、S04deploy／対象／採取主体不一致、S05期限一致、S06確認後再起動、S07task移譲／制御喪失、S08復旧lockだけ／本文改変 |
| Provider／P01〜P08（8） | P01確実な未受理＋必要条件、P02受理済みの追加実行拒否、P03受理後応答喪失でUNKNOWN、P04照会障害／DBとの矛盾、P05同key同payloadの同一受理識別、P06同key異payload拒否、P07key／観測期限、P08別通知key／宛先識別の混同拒否 |
| Store／E01〜E10（10） | E01正常保存・一意再参照、E02同key同本文再照会、E03同key異本文拒否、E04参照不能／曖昧、E05permit／target不一致、E06operation／worker不一致、E07失効／期限一致、E08先行保存失敗／未使用証拠、E09照合済み閉鎖・確認者／参照不一致、E10UNKNOWN閉鎖拒否 |
| Db／D01〜D12（12） | 以下の実DB集合 |

実DB集合：

1. **D01** 実Identity・管理scope／TTLで許可発行→証拠先行保存→用途別consumer roleで消費commit。消費1件・Business Audit・証拠一意追跡を確認。
2. **D02** consumerの現在能力失効／別の認証主体で拒否。消費0・Security Audit、秘密や対象の外部非露出を確認。
3. **D03** consume前の対象revision変更／削除／tuple差異で拒否。消費0。
4. **D04** consume前の停止制御喪失／再起動で拒否。消費0。
5. **D05** provider受理済み／UNKNOWN／矛盾で消費証拠なし、消費0。
6. **D06** 先行保存障害／照合時失効／証拠参照不能で拒否。消費0。
7. **D07** Business Audit INSERT権限をtest内だけで拒否して消費・permit versionをrollback。grantをfinally復元、先行証拠は未使用と区別。
8. **D08** 同permitへ2要求を有界に競合させ、一度性とcommit済み消費保持を確認。各taskへtest認証を設定・解放し、実DB roleを維持。
9. **D09** 消費commit後に制御を失効。test側の実行可否判定はHOLD、消費は保持。commit結果不明のtest modelも追加実行拒否。senderはなく実送信保証は未達。
10. **D10** 照合済み人の結果で閉鎖＋Audit、UNKNOWN結果では閉鎖0。同じ対象の消費・証拠を保持。
11. **D11** attempt更新／対象削除後の旧permit閉鎖は現行拒否と記録。同一対象への複数permitが現行で発行可能な点も観測し、運用接続前の未達とする。
12. **D12** 照合後に状態が変わる窓をtest hookで観測。Port自身が検知した差異の拒否と、最後の照合後に変わる状態を現行Serviceでは原子的に防げない点を区別。実platform／publication fencingのPASSにしない。

D11／D12は現行不足を確認する想定test。新たな本番制約を実装したと説明しない。P05もstub内の識別検証であり、実並行送信の受理保証ではない。必要枝の追加・件数変更はcoverageを落とさず、本票との差異と上限影響を提示する。

## 5. 回帰・package・検証記録

新規48件、管理scope／TTL5 class／40件、初回基盤6 class／55件、既存25 class／99件＋package済みE2E1件を別集合で確認する。Architecture2件は既存99件内で、再実行を新規件数へ加算しない。既存class・invocation・規則をbaselineと照合する。

offline clean package後のJARで既存critical journey E2Eを実行する。main source hashの不変を確認し、JARから新test helper／証拠／有限値／故障注入が除外されることを確認する。JAR hashは再buildで変わり得るため、前commitのhashとの同一性を必須条件にしない。今回使用したJAR・source・run manifestを固定する。

raw保存先候補は非配布`build-support/reference-e2e-verification/target/operational-evidence-20261007/`配下のrun別directory。XML、sanitized log、method対応、source hash、実効資源、cleanupを保全する。部分再検証と全回帰のsource差分を明示し、単一runの全最終source検証を根拠なく主張しない。

## 6. 資源・時間・作業量の候補

前票の許可を流用せず、今回の追加48件・回帰集合に対する新しい上限として提出する。

| 項目 | 本票の候補条件 |
|---|---|
| 開始資源 | available memory8 GiB、disk10 GiB以上。JDK21／Wrapper Maven・必要artifact・既存Chromium・Docker到達をpreflightで確認。新規download／installなし |
| 実行 | Maven／class逐次、fork1／reuseForks=false、JUnit並列無効。各JVM heap768 MiB。D08だけprocess内2 taskを有界に使う |
| DB | 使い捨て既存PG17 image、同時1、memory1 GiB／CPU1／max_connections16。用途別pool2／idle0＋管理2、同時connection総計8以内。E2E childは既存pool4／idle1 |
| timeout | DB lock／statement／Spring transaction各10秒、pool connection10秒。testのlatch／Future待機は各10秒以内でfinally終了。これらは全外部I/O・全JVMの停止保証ではない |
| 検証時間 | 新規class各10分・新規集合合計60分、scope／TTL40件＋初回55件合計60分、既存99件＋E2E合計60分、全検証実経過180分以内（cleanup含む） |
| 記録／再実行 | 今回raw合計1 GiB以内。同原因・同条件のrerun最大1回。初回失敗を保全し、原因／変更を確認せず反復しない |
| 作業量 | test／Evidence作成・検証12〜20標準時間候補。10時間相当で進捗確認、20時間相当で停止・再評価。Owner review・環境承認待ちは別枠 |
| cleanup | classごとに当該DB／Ryuk／JVM／Executor終了を確認。E2E browser／issuer／child／port／一時logも終了確認。無関係process／既存停止containerは操作しない |

資源sampleで不足が判明した場合、残時間があっても次classへ進まない。実効制限、制限の意味と限界、権限付き実行経路をEvidenceへ記録する。環境の承認拒否を別手段で迂回しない。

## 7. 開始条件・停止・戻し方

**開始条件：** 本票の方式・test集合・上限・想定未達の扱いをOwnerが個別承認し、必要なAGENTS／計画の正本整合を確認・反映する。Owner操作またはコミット操作への事前確認で承認文書をcommitし、clean sourceを固定する。その後のpreflightでGit・環境・必要cache／image・実効制限・cleanup手順を確認してから、対象test作成と隔離検証を開始する。

実担当・実platform／publication供給元・実provider・証拠保管の耐久性／ACL・実時計差・運用TTL等はOPENのままAを検証可能。ただし、それらの実運用受入をAのPASSで認定しない。

**停止条件：** main／Port／schema／grant定義／依存／通常構成変更が必要、想定外の対象競合・肯定条件不足、認可／Audit迂回、通常無効時のfixture登録、既存回帰失敗、秘密出力、資源・時間・作業量上限到達、cleanup不成立。原因・必要差分を示して再reviewへ戻る。D07の隔離DBに限った既存Audit権限の一時REVOKE／復元は、故障注入として上記採用対象に含める。

**戻し方：** 作業を止め、当該test context・Executor・使い捨てcontainerを終了する。main／schemaは変更しないので、未commitのtest差分の扱いをOwnerと確認する。無断reset／delete、down migration、履歴改変、保存済みpermit／消費／Auditの削除は行わない。test内resetは使い捨てDBだけ。

## 8. Owner判断欄と次の工程

**未判断。** 接続契約案の段階A・本票§1〜7のtest限定範囲、5 class／48 invocation候補、回帰集合、資源・時間・作業量、想定未達、停止／戻し条件、必要正本整合、文書commit・clean source固定後のpreflightと、成立時のtest作成／Maven・隔離Docker検証について「承認／条件変更／保留」を記録する。

本票はレビュー可能な開始条件を具体化した段階。個別承認後は同じ範囲の開始許可を再要求しない。実運用Adapter、受付／worker／送信、schema／Modulith、Framework／remoteへの拡張は別判断。

今回の変更は開発文書のみ。code／test・SQL／環境、Maven／Docker、git add／commit／pushは実行していない。

## 9. Owner承認・条件付き限定開始（2026-10-08）

Ownerは、本票の到達点・変更path・証拠の扱い・48件の検証・回帰と上限・開始／停止条件のレビュー整理を確認し、「レビュー整理を確認し、内容承認いたします。」と明示した。変更条件の指定はなく、接続契約案の段階Aと本票§1〜7のtest限定範囲、5 class／48 invocation候補、回帰集合、資源・時間・作業量、想定未達、停止／戻し条件を提示内容で採用する。§8の未判断記載は承認前の履歴として保持する。

承認対象はtest所有の対象・停止制御・provider観測・証拠台帳と現行Portへの接続検証、既存Harnessの必要最小変更、当該文書／Evidence。承認文書commit・clean source固定後のpreflightと、成立時の対象test作成／Maven・使い捨て隔離Docker検証を条件付きで承認する。D07の隔離DB内Audit権限一時REVOKE／finally復元も限定故障注入として含む。必要正本整合はAGENTS.mdへ本限定条件・承認導線を反映する。

本番main／Port／schema／grant定義／依存／通常構成は変更しない。D11／D12の既知未達は観測・記録の対象であり、解消した保証にはしない。実運用Adapter、停止操作、provider通信、受付／worker／送信、publication／Modulith、Framework／CI／remote、Phase 4全体・DoD・正式受渡しへ承認を拡張しない。

Agentは承認記録・正本導線を反映するだけでgit add／commit／pushを行わない。Owner操作またはコミット操作への事前確認で文書をcommitするまでtest作成・検証を開始しない。source固定後は同じ承認範囲の開始許可を再要求せず、preflight・環境の権限付き実行手順と本票の停止条件に従う。実装・検証結果のOwner受入は別途判断する。

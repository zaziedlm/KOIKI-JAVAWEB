# S1 R0-B：必要最小限の検証計画（現行、2026-10-10）

**現行入口。Rules107成立済み／通常root未成立。** 以下の集合・順序だけを現在の作業対象とする。過去の件数・反復停止履歴を進捗に加算しない。

[現行結果表](../../../../architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md)。過去の承認・訂正・監視履歴は[アーカイブ](../../README.md)に分離した。旧節へのリンクはアーカイブ側で参照する。

## 必要な集合と区切り

### 1 今回の目的とOwner指示

目的はLevel選択に対応したRule28／29と旧API互換性を検証し、Referenceを保護した配布artifactが成立することを確認すること。監視ツールの一般的な完成・PID世代／event遅延の網羅をR0の目的にしない。

Ownerは「過去作業、成果にとらわれずに、必要なことに集中」「区切りをもった見直し」と指示し、以下の7集合へフォーカスして整理することを明示した。最新指示により監視の網羅拡張を止める。今回の作業は文書と履歴の分離だけ。改修・test・実環境操作は行わない。

### 2 固定する集合と成果

| 集合 | 必要性・現在位置 | 完了の証拠／扱い |
|---|---|---|
| Rules新規40＋既存67＝107件 | R0の中心、成立済み | 固定sourceと107件XML・違反検出／公開契約の保存結果を維持。source／検証入力が変わった場合だけ影響を再評価。単独107件を習慣的に再実行しない |
| PL2既存規約4件 | 旧候補との整合確認、4件PASS | Rule28And29CandidateTestの4件だけ。候補の既知の検出限界も保持、正式Rulesの追加caseやModulith runtime実験へ広げない |
| package・旧Consumer互換性 | 配布artifact／旧API利用確認、PACKAGE_LEGACY_PASS | Rules JAR／通常Reference package、新JARを先頭にした旧Consumer bytecodeの実行成立。旧Consumerを再compileしない。source／main依存不変・fixture非混入・JAR hash／descriptor／実classpathを記録 |
| Reference E2E 1件 | Reference保護、REFERENCE_E2E_ONE_PASS | PackagedReferenceCriticalJourneyTestの既存1 journeyを新packageで実行。Security／Audit／DB／logの既存assertionを維持。画面／認証／業務caseを新規追加しない |
| 通常root297件＋予定skip92 | 最終統合確認、未成立 | 同じsource／package epochを入力にclean verify 1 sequence、今回fresh XMLで計389 invocation候補・実行297・skip92（S1 64＋B2 28）を照合。旧XML・別epoch途中成功を足さない。failure／error0とcleanup／資源条件成立を要求 |
| 監視pure74例と今後の追加例 | 補助証拠、R0本体に加算しない | 現pure結果・失敗履歴を凍結。新case追加／監視の一般化を停止。pure PASSを実監視／通常root PASSの代用にしない |
| S1／B2専用92件 | 今回は通常buildからの分離確認 | 上記rootの予定skip集合とtest source不変を照合。専用processの再現・専用92件再検証をしない |

### 3 区切りを持つ実行順

| 段階 | 内容 | 区切り・次段階へ進む条件 |
|---|---|---|
| 0 実行準備 | source／artifact epochと既存107件の整合、必要な安全な実行方法・予算を確認 | 必要な資源／所有物の識別／cleanupが成立できる手順を先に選ぶ。既知失敗のtmp runnerをそのまま再使用しない。方法が用意できなければ未実行として区切る |
| 1 PL2 | 既存4件だけ | 今回XML4件・failure／error0を保存し、この段階を固定 |
| 2 package／旧Consumer | Rules／Reference packageと旧bytecode実行 | 新artifact hash・classpath・互換性・fixture非混入を記録し、この段階を固定 |
| 3 Reference E2E | 段階2のpackageで既存journey1件 | XML1件・業務／Security／Audit／DB／log・cleanup成立を保存し、Reference保護を固定 |
| 4 通常root | 全体の最終統合・予定skip92確認 | fresh全体集合・資源／cleanupを照合。成立した場合だけR0-B検証全体を完了とし、Owner最終受入へ提出 |

本体の独立した検証を先に固定し、通常rootを最終統合確認とする。段階2は既存依存関係が必要とするcompile／packageを含むが、専用test・無関係な回帰testは追加しない。段階4が未成立でも段階1〜3の成立証拠は保持する。ただし、その保持をR0全体完了・Reference Level2開始・正式採用へ読み替えない。

各段階の初回は1回だけ。failure／UNKNOWN／監視不成立・予算不足では当該段階を停止・保全し、そこで結果を区切る。自動でcaseを増やしrootを反復しない。必要な原因整理を行うが、網羅拡張を再開する指示にはしない。段階間でsource／artifact入力が変わった場合だけ、影響する成立証拠を再評価する。

### 4 検証環境は必要最小限の実行条件へ戻す

必要な条件は、今回の実行source・起動したprocess／containerの所有・資源上限・タイムアウト・cleanup・保護baselineの保持を説明できること。あらゆるPID再利用や短命eventを分類できる汎用監視は今回の成果要求にしない。

既存の成立済み限定検証手順を再利用できるかを、用途・資源設定・Ownership・残予算の読取照合で先に判断する。再利用可能と未確認の時点で記載せず、失敗した現tmp監視を無効化するだけの実行もしない。これを新しい長期監視開発へ広げない。段階0で適用手順を選べなければ、次のroot commandやpure追加案を出す代わりに「実行方法未成立」を残課題として固定する。

上限はJVM4／PG1、通常root client14／Ryuk2、他phase client8／Ryuk1、memory8GiB／disk10GiB、raw100MiB／run2GiB、既存child heap・pool／assertion・Securityを維持する。新provider・OS権限変更・dependency取得・Reference／POM／SQL／CI等の実装変更で成立を先行しない。

### 5 残予算と完了判断

停止診断・途中XML・監視74例・過去の訂正履歴はアーカイブに分離した。通常root未成立、最終cleanup確認済み、予算は下記へ引き継ぐ。

累計1987秒、残1613秒（26分53秒）、cleanup300秒予約。予算はリセットしない。残予算の配分は段階2＋3合計最大480秒（段階2最大300秒、段階3最大180秒）、段階4最大600秒、cleanup／最終保全300秒、準備等233秒。計1613秒で既存の各上限を増やさない。実測・準備負担から不足が判明した段階は未実行として区切り、時間不足を理由に件数／assertion／監視条件を減らさない。計画・文章整理はtest実行ではなく、追加実行許可を消費したとは扱わない。

R0の完了判定は「規約107」「PL2 4」「package／旧binary互換性」「Reference E2E 1」「root全体／skip92」「資源／cleanup／source・artifact整合」の成立表で示す。監視74件の成功率や過去root途中成功の累積を進捗率に使わない。root内のRules107と単独107も二重加算しない。最終結果の受入はOwner判断。R1／実非同期／DoD・Gate／Phase4全体・commit／remoteの開始判断を兼ねない。

### 6 網羅拡張を行うタイミング

| 拡張対象 | 開始の契機 | 今回の扱い |
|---|---|---|
| Rulesの新しい反例 | 0／1／2の採用契約に具体的な検出抜けが確認されたとき | 根拠・期待違反・影響を固定した変更単位で扱う。予測だけでcaseを追加しない |
| Reference非同期／復旧／provider | R1以降の個別実装開始判断と、追加する機能の契約が揃ったとき | 現R0には追加しない |
| S1／B2専用検証 | 当該source／protocol／専用実行条件が変更されたとき | 今回は分離確認だけ |
| 一般的な監視のPID世代／event遅延網羅 | 共通検証ツールとして再利用する要件・保証範囲・専用予算を決めるとき | 別作業へ分離、現在は凍結 |

段階1以降は未実行。段階0は下記の読取専用preflightまで準備済み。build／testの安全な実行方法は未成立で、監視case追加や旧runner再開へ戻らない。

### 7 最小実行準備（2026-10-10）

Ownerの「必要最小限の実行準備へ進む」指示により、入力整合の確認と読取専用preflightだけを準備した。新規case・監視pure・Java／Maven／container起動は0。

固定snapshot1156ファイルと元source（文書4件の承認済み差分を除く）はhash一致。Rules保存XML13ファイルは107件、failure／error／skip0。rules／tooling／E2E classpathは31／129／46 entryが存在し、旧Consumer bytecode・JDK／Maven入力もhash台帳へ保存した。確認は再実行PASSを意味しない。初回のWindows長いpath読取を訂正した分も含め2秒計上し、累計1932秒・残1668秒となった。

| 手順 | 今回の選定結果 |
|---|---|
| 過去のPL2限定手順 | 4件選択と証拠保存の形は参照可能。固定source・timeoutと上限が異なり、script全体を再使用しない |
| 過去のroot／E2E手順 | 旧source／artifact、旧上限やcache変更を含むためそのまま使用しない |
| 現tmp event監視runner | 使用しない。追加pure／launch probeも行わない |
| 新しい読取専用preflight | 準備済み。環境・入力・予算だけを照合し、結果保存後に終了。build／test起動方法の成立は別欄で未成立を維持 |

`tmp/r0-rules-9c41aa4-20261009-first/minimal-preparation-20261010/` に `static-readiness.json`／`readiness-inputs.json` と `Start-Readiness-Owner.ps1` を配置した。scriptは同一user管理者PowerShell7、HEAD／branch／snapshot／保存入力、保護JavaのPID＋creation、Docker Server応答／稼働container0、memory／disk／raw／run容量を確認する。event登録・権限変更・build／test／container操作はしない。Docker読取commandは各10秒、全体75秒上限（同期読取処理は完了時に超過を判定）、消費時間を既存累積台帳へ加算する。初回attemptを保持し、自動再実行はしない。

PowerShell parserによる構文確認は成立。Owner環境での実行は未実施。出力 `READINESS_ONLY_PASS` は環境読取成立だけで、段階1開始・本体PASSやroot監視成立を意味しない。停止・baseline差分・予算不足時は結果を保全して区切る。次に読むものはこのpreflight結果だけで、旧監視履歴を再開入口にしない。

### 8 成果固定commitのOwner承認（2026-10-10）

OwnerはGit点検結果に同意し、「指摘の方向で、2点の直しを含めて、ローカルコミットを進めてください」と明示した。規約票§9.5の旧停止結果をアーカイブ参照へ置換し、AGENTS.md等の承認参照をアーカイブの所在へ接続する。対象はRules main5＋test／fixture11、現行／承認文書4＋アーカイブ4＝24ファイル、現在branchへのlocal commit1回。必要なcode／testは破棄しない。tmp script／raw証拠はGit対象外として保持する。

親HEADは `e8c81fe46fcc1f3de19c66fb6c5eea180831b9b7`。成果固定はRules107成立済み・通常root未成立の中間状態を保存するもので、R0結果受入・remoteを含まない。新HEADはcommit後のGitと端末内 `minimal-preparation-20261010/local-commit-result.json` で確認する。preflightの確認対象HEADだけを新HEADへ更新し、過去のRules source manifest・XML・artifact epochを上書きしない。

### 9 現在の区切り：PL2前提package停止（2026-10-10）

成果固定commit `c11dbbceb0241f1257f9bfc153105d8f55125c1c` 後、Owner読取preflightは成立。PL2限定実行はWindows Job所属による所有／cleanupを使い、Rules targetの不完全状態を解消するため前提offline Rules package／隔離m2 installから開始したが、compile中にResource ceiling/floorで停止（6秒）。PL24件・Rules107再実行・S1／B2専用・Reference／E2E・rootはいずれも未起動。Rules package／installも成立しておらず、元m2とrun/m2の保存入力は不変。

保存済み3sampleはmemory／disk／job process数が閾値内。停止値の保存より先にthrowする診断不足があり、失敗項目はUNKNOWN。Native limitや実環境の原因を推定して変更しない。source1156・元code／保存入力hash照合、Owner cleanup成立と既証拠hashを `minimal-preparation-20261010/pl2-stop-preservation.json` へ保全、1秒計上。累計1943秒、残1657秒、cleanup300秒予約。今回で区切り、上限変更・case追加・再実行なし。

次に必要な修正を「停止時memory／disk／job process数・判定名の保存だけ」として非実行メモに整理した。実行方法全体の成立、追加実行許可、根本原因の確定を意味しない。原script／helperは変更せず保存し、旧監視74例へ戻らない。

### 10 PL2限定訂正・再実行1回のOwner指示（2026-10-10）

Ownerは「再実行へ進めます」と明示した。対象は停止時memory／disk／job process数・判定名の保存訂正だけ。前回script／helper／証拠は保持し、`Start-PL2-Rerun1-Owner.ps1` と別出力 `pl2-rerun1/` を使用する。native helper・数値上限・Rules source・PL2既存4件・assertionは不変。新case／launch probe・追加root・Reference／S1／B2専用testは実行しない。

段階1は初回6秒を控除した174秒以内、既存累積予算・cleanup300秒予約を維持。Git確認は固定HEADとRules source維持を要求し、結果記録で生じた現行文書2件の差分だけをhash固定して許容する。追加commitは行わない。preflightを再実行せず、再実行script内でsource／baseline／memory／disk／container0を確認する。結果保全後に区切り、自動で2回目へ進まない。

再実行準備1秒を計上済み、現在累計1944秒・残1656秒（27分36秒）、cleanup300秒予約。新scriptは構文確認済み、Owner端末での再実行は未実施。

### 11 現在の区切り：全process上限の誤適用（2026-10-10）

再実行1回は前提Rules compile中、5秒でOWNED_PROCESS_LIMITにより停止。記録値はmemory17479831552 bytes／disk50102099968 bytes（各下限内）、job ActiveProcesses6／独自全process上限4。Owner cleanup成立、Rules source・旧入力／証拠hash不変、PL24件未起動・Rules package未成立。再実行と訂正draft保全2秒を追加し、現在累計1951秒・残1649秒、cleanup300秒予約。

runnerが承認済みJVM4をcmd等も含む全process4へ置き換えた設計誤りを確認した。カウンタoffset40はWindowsのActiveProcessesで、累積TotalProcessesの誤読ではない。ただしlimit違反により終了中のprocessも一時的に含むため、6からJVM6や生存process内訳を推定しない。[Microsoft定義](https://learn.microsoft.com/en-us/windows/win32/api/winnt/ns-winnt-jobobject_basic_accounting_information)。当該実行のJVM数は未記録で、JVM4超過は未立証。

限定訂正draftはJobを所有・一括停止に使用し、jobに属するprocess handleの生存／image名を照合してjava／javaw／javac／jcmdだけをJVM4として判定する。全process4の独自制限を外し、承認済みJVM4・その他上限は維持する。`OwnedJob-JvmDraft.cs` を別名で準備・C# compile確認済み。旧helper／script／失敗証拠は保持。実process起動・新case／probe・追加再実行は0。draftのruntime成立、次回再実行開始を認定しない。

### 12 JVM数判定訂正・再実行2のOwner指示（2026-10-10）

Ownerは「再実行に進めます」と明示した。全process4の誤適用を取り除き、Job所属・生存handleの実行imageからJVM4を判定する限定訂正を採用する。旧helper／script／証拠は保持し、準備済み `OwnedJob-JvmDraft.cs` と新script `Start-PL2-Rerun2-Owner.ps1`、別出力 `pl2-rerun2/` を使う。JVM数と内訳・全process参考値・停止判定項目を保存する。Jobの所有／一括終了、memory／disk／容量／container0、既存4件とassertion、snapshot・隔離m2を維持する。

段階1残枠169秒（初回6秒＋再実行1の5秒を控除）、累積予算・cleanup300秒予約を維持。追加case／probe・Rules107再検証・Reference／root／S1／B2専用実行はしない。Owner同一user管理者PowerShell7で1回だけ実行し、停止時に再実行3を自動開始しない。JVM観測は有限snapshotであり、観測間の全時点保証や汎用監視完成を主張しない。

再実行2準備1秒を計上し、現在累計1952秒・残1648秒（27分28秒）、cleanup300秒予約。script構文と旧証拠hashを確認済み、Owner実行前。

### 13 現在の区切り：起動前baseline未成立（2026-10-10）

再実行2はgateでJava cleanup/baseline not establishedにより1秒で停止。Maven／Java／Rules package／PL2 testは今回起動していない。Cleanup=falseは期待Java件数／identityとの一致を確認できなかった状態で、今回所有processの残留を証明しない。停止時のCIM一覧は未保存のため当該差分はUNKNOWN。準備source／入力hash不変を `pl2-rerun2-stop-preservation.json` に保全、1秒計上。現在累計1954秒・残1646秒、cleanup300秒予約。

Agent非管理者tokenのCIM読取はアクセス拒否で、空のJava一覧とは扱わない。別の通常.NET読取では既知PID24852／10280の2件が見えるが、停止時CIM状態・baseline再採用の証拠ではない。次の必要最小操作は `Read-Baseline-Owner.ps1` による管理者端末の読取1回だけ。期待値・現在CIMのPID／親PID／名前／creation・比較を保存し、既存baselineは更新しない。build／test／process停止・event登録・新case／追加再実行を行わない。

### 14 最新baseline一致・確認値保存の限定準備（2026-10-10）

Owner読取はOBSERVATION_ONLY。期待2／実2、PID24852・10280の名前／creation一致、親5292、未知PID0。baselineの変更・採用・process停止なし。現在保存された一覧に旧件数比較を適用すると不一致=falseで、前回gate不一致の原因は未確定のまま。現在一致を停止時点の一致へ遡及しない。

次に用意した `Start-PL2-Rerun3-Owner.ps1` はbaseline件数／identity判定前に期待・実際・読取失敗・比較値を別JSONへ保存する限定訂正。期待baseline・JVM4・Job所有・既存4件は維持し、旧script／証拠は保持する。観測間のJava変動を自動無視・再試行・baseline更新しない。CIM確認は10秒timeout、段階1残枠168秒、別出力 `pl2-rerun3/`、1回で区切る。root／Reference／専用92件・新caseは対象外。

Owner読取1秒と保存・準備1秒を計上、累計1956秒・残1644秒（27分24秒）、cleanup300秒予約。script構文確認済み・実行前。現在のbaseline一致はPL24件／Rules packageの成立結果を意味しない。

### 15 PL2成立固定・package／旧Consumer段階の準備（2026-10-10）

PL2既存4件はfresh XML4／failure・error・skip0、Owner cleanup成立、JVM最大観測2で成立。新Rules JAR hashはDED17E83809F2BF73D743F67792702B559EAF59FBF4A4FAAF9DE3FD82C506918、main class26／fixture・test class0。入力JARをPL2証拠側へ別コピーし、XML・log・baseline・資源・summaryのhashを `pl2-four-fixed-evidence.json` に固定した。Rules107を再実行・二重加算していない。

次は `Start-Package-Owner.ps1` で通常Referenceと必要upstreamのoffline clean package（test compile／実行skip）を行い、Rules JAR／Reference main内容・nested KOIKI JARとreactor JARのhash、test／fixture依存非混入、公開descriptor3 signatureを確認する。旧Consumer bytecodeは再compileせず、新Rules JAR先頭・旧JAR非混入classpathで実行する。必要な既存metadata/simple fixture2 sourceだけを隔離出力へcompileする。POM／main／test sourceは変更しない。

Reference packageとRules packageは新artifact epochとして記録し、PL2入力JARを上書き・過去結果へ読み替えない。package段階300秒、次のE2E180秒、合計480秒以内・JVM4／container0で区切る。Reference E2E／root・S1／B2専用testは今回起動しない。初回は1回のみ、失敗時自動再実行なし。

PL2実行29秒、結果固定・package準備2秒（生成scriptの訂正を含む）を計上し、現在累計1987秒・残1613秒（26分53秒）、cleanup300秒予約。script構文確認済み、Owner package実行前。Git差分は結果文書2件のみ、追加commit／remote0。

### 16 package停止の区切りと容量走査だけの限定訂正（2026-10-10）

Owner初回packageは32秒で停止、cleanup成立。logはReference cleanによるtarget削除で終わり、runnerの容量用再帰走査がtarget/test-classes消失をDirectoryNotFoundとして停止した。Maven終了結果・新Reference artifact検査・旧Consumer実行は未成立。main／POM／test sourceの失敗とは認定しない。snapshot1156と元source（承認文書除外）、PL2固定証拠hashを照合し、初回script／log／summaryを上書きせずhash保全した。

同種の限定訂正はOwnerの§26継続許可の範囲で準備する。`Start-Package-Rerun1-Owner.ps1` は、隔離sourceのmodule直下targetでDirectoryNotFoundを確認した場合だけ容量の全走査を1回やり直す。部分走査値を採用せず、再度失敗・権限／他IOエラー・target外消失では停止する。失敗path・判定・再走査有無を保存する。容量・JVM4・memory／disk・所有／cleanupの数値は維持。新case・pure／probe・Rules107／PL24件再実行・専用92件・E2E／root起動を追加しない。

package初回32秒を控除し再実行枠268秒、package＋E2E残448秒（E2E180秒予約）。別script／出力で1回だけ区切り、自動build再実行はしない。結果保全・準備2秒（入力epoch照合訂正を含む）を計上、現在累計2021秒・残1579秒（26分19秒）、cleanup300秒予約。Owner再実行前。Git変更は現行文書2件のみ、追加commit／remoteなし。

初回Reference reactorはRules JARも再packageしており、新hash49925B073772E5AB9A04A2F6769272BAA01D9B30A7965C1D7465E681DD6303B3。PL2入力DED17…とは別epochとして保存し、全26 classの名前・byte一致を確認した。PL2固定JAR／XMLは不変。訂正準備中の入力hash照合はこの想定済み再packageを検出して停止したため、元hashを上書きせず新入力台帳にPriorEpochSha256と新hashを明示する。build再実行はしていない。

### 17 package成功の保存と未実施互換性だけの再開準備（2026-10-10）

再実行1は40秒でSTOPPED。ただしMavenは13 module全SUCCESS／Exit0、JobEmpty=true、最終資源sampleも所有process0／JVM0。直後のbaseline一覧は保護2件のidentity一致＋jcmd.exe PID20104／親5292の追加1件、cleanup一覧では保護2件へ戻りCleanup=true。jcmdの起動目的・所有者は未確定、同じ親PIDだけで除外・baseline再採用をしない。初回の容量走査競合は今回の停止理由ではない。

成功したpackageを再実行しない。source1156とPL2固定証拠を照合し、今回log／Exit／baseline／資源／summaryをhash保全。生成Rules JARの26 classはPL2固定入力とbyte一致、Reference main全entryとnested KOIKI JARのhash一致を読取確認し、新artifact hashを別manifestへ固定した。旧Consumerと公開descriptorは未実行で、段階全体PASSへ更新しない。

`Start-Package-Resume1-Owner.ps1` は保存済みExit0／JobEmpty／cleanupと固定artifact・source入力、期待Java baseline2件を要求し、artifact検査・fixture2 sourceのcompile・既存Consumer bytecode実行・descriptor確認だけを行う。Maven reactor再起動なし。未知Javaの無視・baseline更新・process停止・新case・pure拡張なし。Job所属による所有／JVM4と既存上限・cleanupを維持する。

package累計72秒、再開上限228秒、package＋E2E残408秒（E2E180秒予約）。保存・静的artifact照合・準備2秒（Starter所在の静的照合訂正を含む）を計上、現在累計2063秒・残1537秒（25分37秒）、cleanup300秒予約。Owner再開前。Rules107とPL24件は成立維持、Reference E2E・通常root・専用92件は今回未起動。追加commit／remoteなし。

### 18 旧Consumer用fixtureの引数構築訂正（2026-10-10）

再開1はfixture-compileで3秒停止、javac Exit2／所有Job空／cleanup成立。保存されたArgumentsは`-J-Xmx768m`、`@`、args pathの3件。logは「@は無効なフラグ」。Agent作成scriptのPowerShell array内結合式に括弧がなく、引数ファイル指定を2引数へ分割した誤りであり、fixture sourceの診断ではない。文字列だけの確認で旧3引数→訂正後2引数を確認した（Java起動なし）。

`Start-Package-Resume2-Owner.ps1` ではjavac側を`@('-J-Xmx768m',('@'+$argsPath))`、java側も結合式の括弧を明示する。旧script／compile log／args／Exit／baselineをhash保全、snapshot1156・PL2保存証拠・固定artifact入力を照合済み。旧Consumer bytecodeを再compileせず、fixture2 source・Consumer実行・descriptorの未実施分だけを同じ上限で行う。reactor再build・case追加・baseline変更・専用92件／E2E／root起動なし。

package累計75秒、再開上限225秒、package＋E2E残405秒（E2E180秒予約）。保存・限定訂正準備1秒を計上、累計2067秒・残1533秒（25分33秒）、cleanup300秒予約。Owner実行前。段階全体PASS・旧Consumer PASSをまだ認定しない。追加commit／remoteなし。

### 19 package・旧Consumer成立固定とReference E2E 1件の準備（2026-10-10）

Owner再開2はPACKAGE_LEGACY_PASS、7秒／cleanup成立。旧Consumer bytecode不変、実行Exit0、公開descriptor確認Exit0、test／fixture依存非混入。Rules JAR F9544EA78132BB49E422CC28F70047BED6BDE7370C6C56EA33D1F0995DD1AB04、Reference JAR 313C3F72FF04BDA3D96D3746BFEB006727D468A7D431E6CAE4F30ECEB08B0804をhash固定。保存log／args／Exit／manifest／baseline／資源／fixture classを別manifestへ保存、source1156・旧Consumer・固定入力を照合済み。Rules107／PL24件を再実行していない。

次は`Start-E2E-Owner.ps1`で既存PackagedReferenceCriticalJourneyTestのjourney1件だけ。offline専用module clean test（testの通常compileを含む）、固定Reference JARを用い、既存業務／Security／Audit／DB／log・child heap/pool assertionを維持する。JVM4／PG1／Ryuk1／client8、PG memory1GiB／1CPU・max_connections16と既存child heap768MiB／pool4・idle1、memory／disk／容量上限を維持。接続数は専用監視SELECTも含む有限sampleで判定し、全時点の連続保証とはしない。

起動前container0／保護Java2件、起動後Job所属のJVM数と隔離containerのcreation／session pair／IDを照合する。container所有判定の既存関数だけを再利用し、旧event/PID監視を起動しない。Docker各操作10秒、PG起動待ち／session照合の既存5秒猶予を維持、未知containerは停止しない。cleanupは当該Jobとidentity確認済みcontainerに限り、最大30秒を予約cleanup枠内で行う。新case／pure・download・reactor再build・専用92件・root起動なし。

E2E180秒上限、package＋E2E既消費82秒・残398秒。成立XML1件・assertion・artifact不変・cleanup確認後だけ次のroot準備へ進む。失敗時自動再実行なし。固定／準備1秒計上、累計2075秒・残1525秒（25分25秒）、cleanup300秒予約。Owner E2E実行前、R0全体未成立・最終受入前。追加commit／remoteなし。

### 20 Reference E2E成立固定・最終通常root 1回の準備（2026-10-10）

Owner E2EはREFERENCE_E2E_ONE_PASS、31秒、fresh XML1／failure・error・skip0、既存assertion／固定Reference hash不変、cleanup成立。XML hash1D5B002EE797E32217CBBF2EF40FF5C80FC84009717D594D05CDF1696E8FA6CC。XML・sanitized child資源log・Maven結果・baseline・Job／PG／Ryuk／接続sampleをhash固定した。package／E2E成立JAR2件を別保存し、root cleanで上書きされるtargetと区別する。

`Start-Root-Owner.ps1` は同snapshotのoffline通常root clean verifyを1回だけ実行する。特別なS1／B2／E2E有効化propertyやtest選択を渡さず、root15 moduleのfresh XMLだけを保存し、389 invocation＝297実行＋指定92skip（S1 7 class64／B2 3 class28）、failure・error0、他skip0を照合する。旧XML・build-supportのPL2／E2E専用XML・過去の部分結果は加算しない。Rules107はrootにも含まれるが単独結果と二重加算しない。

E2Eで成立したJob所有／JVM観測・container session／creation／ID照合を再利用し、rootの承認済みPG1／Ryuk2／client14・JVM4、memory／disk／容量を適用する。容量走査のDirectoryNotFound全走査追加1回は固定root moduleのtargetにだけ限定。root600秒、Docker各10秒・cleanup30秒を予約300秒内で実施。未知Java／containerを無視・停止せず、baseline期待2件を更新しない。pure・launch probe・新case・専用92件実行・download・実装変更・remoteなし。

rootが作るJARは新epochとしてhash保存し、旧E2E入力と同じhashだと推定・読み替えしない。root成立後のsource／保存証拠／epoch照合を経て最終結果をOwnerへ提出する。失敗では未成立のまま区切り、自動再実行なし。保存・準備4秒計上、現在累計2110秒・残1490秒（24分50秒）、cleanup300秒予約。Owner root実行前、最終受入前。

### 21 通常root停止・cleanup確認を先行する区切り（2026-10-10）

Owner通常rootは208秒でReady database connection observation failedにより停止。fresh完全集合／package完了は未成立、Cleanup=false。保存済みsample最大はJVM3／PG1／Ryuk2／client11で上限内だが、失敗pollのSQL Exit／stderrとcleanup側の例外を保存しておらず、停止原因の細部はUNKNOWN。ログ末尾はMasterReadModelPostgreSqlIntegrationTestのPG起動直後。起動途中のDBと観測の競合は候補であり、確定しない。

直前の所有sampleにはRyuk4b86b6c…がsession pair待ち。root Job内java20588の観測と対応するlogには当該Ryuk起動IDがあるが、未登録IDを自動停止しない。cleanup未成立を単なる記録問題としてPASSへ訂正しない。まず現在のOS状態を読取確認する。旧helper／script／停止log・sample・container台帳・summaryをhash保全し、snapshot／元source1156（文書除外）とRules107／PL24／package／E2E独立証拠・別保存JAR不変を確認した。

gate後の途中XMLは187 invocation／実行187／skip0、failure0／error0として別dirへ保存した。完全root集合でなく本体完了へ加算しない。通常root追加起動0、新case／pure追加0。

`Read-Root-Cleanup-Owner.ps1` は管理者PowerShell7でJava PID／名前／親／creationを期待2件と比較し、現在running container IDと保存台帳へのidentity一致を読むだけ。最大60秒／Docker各10秒、inspect最大3件。SQL・build・event・process／container停止・baseline変更なし。結果がCLEANUP_OBSERVED_PASSでも歴史root summaryのCleanup=falseを上書きせず、現在一致の別証拠として保持する。

保全・読取手順準備1秒を計上、累計2319秒・残1281秒（21分21秒）、cleanup300秒予約。root600秒枠の既消費208秒／残392秒を維持するが、追加実行は未準備・未起動。cleanupと停止診断を確認するまで再実行へ進まない。追加commit／remoteなし。

### 22 現在cleanup成立・root監視だけの限定訂正準備（2026-10-10）

Owner読取はCLEANUP_OBSERVED_PASS、保護Java2件の名前／PID／creation一致、稼働container0。baseline採用・停止操作なし。2026-10-09T22:57:08Z時点の別証拠としてhash固定し、歴史rootのCleanup=falseを上書きしない。保存独立結果・別保存JAR・root停止証拠／source1156は不変。

旧pg_isreadyにはhost指定がなく、初期化用socket serverを起動完了と観測する可能性がある。[PostgreSQL公式entrypoint](https://raw.githubusercontent.com/docker-library/postgres/master/docker-entrypoint.sh)はtemp serverをlisten_addresses空で起動し、DB初期化後に終了・通常serverへ移る。[Testcontainers 2.0.5 PostgreSQL source](https://raw.githubusercontent.com/testcontainers/testcontainers-java/2.0.5/modules/postgresql/src/main/java/org/testcontainers/postgresql/PostgreSQLContainer.java)もready log2回を待つ。これは方式上の不整合の根拠であり、失敗pollのstderrがない今回の原因を確定するものではない。

`Root-Containers-Rerun1.ps1` はpg_isreadyだけを127.0.0.1 TCPへ限定、SQL／認証／DB／業務sourceを変えない。SQL失敗はExit／2048文字以内のcredential除去済みstderr／stdoutをthrow前に保存し、接続数が読めない状態をPASSにしない。停止中container退役の既存確認だけを維持し、実行中SQL失敗の自動無視・再試行は追加しない。

旧採用済みGet-OwnedLoggedIdsだけを再利用し、当該Jobに所属して生存handleで観測されたJava PIDからのTC container起動log IDを記録する。既存creation／image／name／session policyと組み合わせ、session pair待ちRyukのcleanupでも所有を確認する。未知IDは停止しない。Job所有判定・baseline期待値・JVM4／PG1／Ryuk2／client14・容量／memory／diskの上限は不変。cleanup失敗の別診断を保存し、container cleanupが失敗してもJava baselineの独立確認を行う。

Ownerの同種限定訂正継続許可内で`Start-Root-Rerun1-Owner.ps1`を別script／別出力へ準備した。残392秒以内でroot clean verify追加1回だけ、fresh297＋skip92／他skip0を照合。旧partial187件を足さず、case／pure／launch／専用92件・download・source／POM変更なし。失敗時に自動で次を開始しない。読み取り1秒と準備2秒計上後、累計2322秒・残1278秒（21分18秒）、cleanup300秒予約。Owner再実行前・最終受入前。追加commit／remoteなし。

新root scriptでは旧資源sampleのContainers=0固定欄を除去し、container数は実数を保存するcontainer-resources.jsonで照合する。歴史sampleは上書きしない。

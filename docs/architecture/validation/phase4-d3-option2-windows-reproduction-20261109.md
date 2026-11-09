# D3方式2：Windows自動再現・初回停止記録（2026-11-09）

**最新判定（2026-11-10 JST）：Windows S5-V01〜06の限定技術PASS。** 期限切れ補助Cookieだけを除外したwin06の実測・境界は§13.1を参照。以下のFAIL／HOLD・未実施表示は各実行時点の履歴で、win01〜05とdiag01の証拠を保持する。手動確認・結果Owner受入・D3／Phase4全体の認定は別に残る。

## 1. 初回からwin05までの判定と実行範囲

**後続Owner指示：** 停止前Browser接続の静穏化と停止診断をToolingへ追加し、新規projectで同じS5集合1回の再実行を指示された。初回FAILを保持し、§8へ後続実行を記録する。下記の追加実行未承認・判断待ちはこの後続指示前の記録である。

**最新結果：** §8の`win02`もfrontend SIGTERM41.066秒・exit137でFAIL。3画面のblank／networkidle・Cookie保持を確認したが停止は成立しなかった。SIGTERM後に待受が消失し、port3000のESTABLISHED12件が35秒時点まで残る診断を取得。今回の再実行1回を終了し、cleanup・secret削除・export／checksum照合が成功。3回目の動的実行は未実施。

**後続限定調査：** §9の`diag01`で停止所有元・待機箇所を観測した。HTTP requestイベント未発火の13 socketがNext PID1のFDとして残り、Windows側ではChromeが13接続を保持。APIRequestContext解放後も変化なし。NextはHTTP server.closeのcallback完了前で待機し、後段のnextServer.closeへ進んでいない。停止自体は41.307秒／137でFAIL、S5全体の追加実行ではない。cleanup・秘密削除・export成功。

**後続Owner指示：** Browser自体の終了、Cookieのメモリ保持と再起動後復元によるharness修正へ進める指示を受けた。§10へ修正・新規projectでの確認を記録する。以下の当該修正未実施・判断待ちはこの指示前の記録とする。

**win03の最新結果：** Browser終了後のfrontend停止は0.413秒／exit143・強制終了なしで成立し、同volume／同鍵再起動後のDB／Audit／Session暗号文等のsnapshot不変も成立した。一方、Cookieを新Browserへ復元した後の値・属性完全一致検査で停止したため、S5全体はFAIL／後続HOLD。詳細は§10.1。判定を緩めず差分観測の準備まで行い、4回目の通常S5は実行していない。

**win04の追加S5結果：** §11のOwner指示による通常S5集合1回もCookie復元検査でFAIL。AはCookie6→5件、Bは5→4件で各1件欠落、3つ目は5件一致。復元後に残ったCookieの値・属性変更fieldは0、unexpectedも0。frontend0.413秒／143・同volume／同鍵snapshot一致は再確認した。欠落が期限切れか別の非受入かは未確定。専用cleanup・秘密削除・export成功。以下のwin04未実施表示は実行前の履歴。

**win05でCookie欠落の原因を特定：** §12の1回確認で、欠落対象はいずれも`IDP_AUTH_SESSION_HASH`（Keycloakの短期認証補助Cookie）と判明した。取得時は有効、注入前・読取後は期限切れ。必須BFF Session3件は存在・同一ID・全属性一致・有効性が全てtrue。期限切れ補助Cookieまで完全一致対象にしたharness期待値が今回のFAIL原因である。frontend0.412秒／143・snapshot一致・cleanup／秘密削除／export成功。期限条件を加えた比較修正とS5後段の確認はまだ行っていない。

**Windows初回S5自動再現はFAIL、後続はHOLD。** frontendのSIGTERM停止が猶予40秒以内に終わらず、41.313秒・exit137（強制終了、OOMなし）で停止した。専用projectのcleanup・生成secret／realm削除と証拠exportは成功した。CloudのS5限定技術PASSを上書きしない。

OwnerのWindows検証開始指示により、[Cloud S5結果§10](phase4-d3-option2-s5-results-20261109.md#10-windows再現への引継ぎ)に従い、同sourceの新規buildと新規projectでS5-V01〜06の集合を1回実行した。実行中に観測できた正常結果と、checkpoint確定前の失敗を分ける。手動環境起動・Owner打鍵・commit／remoteは今回行っていない。

## 2. source・環境・生成物

| 項目 | Windowsで確認した値 |
| --- | --- |
| branch／固定source | `docs/d3-option2-implementation-conditions-20261109`／`bf3eca651cdece055e7b4ee36d816c92ee0fee78`。local HEAD／origin tracking／GitHub ls-remoteが一致、開始時clean |
| 実行差分 | Toolingの`cleanup.py`だけ、Windowsで読取専用secretの属性を解除して削除する6行追加。Framework／Reference／依存・lock／test assertionは変更なし |
| JDK／Maven | Temurin21.0.13+9、java／javac／javapをJDK21へ統一、Wrapper3.9.16。MAVEN_BASEDIRをRepository rootへ設定 |
| Node／npm | プロセス内PATHに既存専用`.tmp/d3-tools/pinned`を設定し24.19.0／11.9.0を確認。通常PATHの22.20.0／10.9.3やsystem PATHは変更なし |
| その他 | Python3.13.3、Docker Server29.5.3 Linux amd64／6 CPU・16,771,686,400 bytes、Compose5.1.4、Buildx0.34.1 |
| Browser | Playwright1.62.0、Windows Chromium151.0.7922.34／revision1234を新規取得。専用cache `.tmp/d3-sso-windows-browsers-20261109/` |
| port／容量 | 開始時13000／18080未使用、C空き約53.4 GB。既存の`osskk_postgres_unified`は操作せず、終了時もhealthy |
| JAR SHA-256 | `b19b4b66812f1fd761ee15eb20499d6b3e6a8032d62275b9bc36b837dd5ebc96`。Cloudとbyte一致は要求せずWindows新規生成物として識別 |
| frontend context SHA-256 | `200033515af4df69e8beb7b38769357d77cf0f54edbadf82aedb9baa3283d59b` |
| project／work | `d3-sso-s5-20261109-win01`／`.tmp/d3-sso-s5-20261109-win01/` |
| 補助Evidence | `.tmp/d3-option2-windows-evidence-20261109-01/`。build／依存／単体／guard・image取得／build・実行log、環境・初回結果照合 |

S5 manifest内のauthorization文字列・`java_unchanged`／直接回帰0・Next build3回等の説明はCloud runner由来の固定metadataであり、今回Windowsの承認・実行回数を表す値ではない。Windowsの実際の指示・新規Java build／直接回帰23件・Next build1回は本書と補助Evidenceを優先する。manifestは実行時点のまま保全し、後から書き換えていない。

## 3. 準備・buildと最小Windows対応

| 集合 | 結果 |
| --- | --- |
| 依存 | 専用lockの`npm ci --ignore-scripts --no-audit --no-fund`成功、50 package。依存版／lock変更なし。今回auditは実行していない |
| Java | `build-reference.py --stage S2`成功。指定4 class／23件、failure0／error0／skip0、d3-container、SPA／async JDBC非同梱確認 |
| Node境界 | 固定Nodeによる39件PASS／skip0 |
| 共有guard | 元sourceで5件PASS、cleanup対応後に同じ5件PASS |
| Next | Windows hostのNext build／TypeScript成功、standalone生成。build JVM／Next buildとruntimeは非並走 |
| image | Windows新規JAR・standaloneから専用backend／frontend image build成功。Keycloak／Nodeは固定digestで取得 |

通常sandboxではDocker named pipe、npm cache／未cache依存、Node test子process（spawn EPERM）、Python test用Temp（WinError5）が拒否された。環境の権限付き実行手順で同じ範囲を実行し成功した。最初のnpm呼出しはcwdに対する専用Nodeパス指定が誤っており、正しい相対pathに直して版を再確認した。失敗logと指定版での成功logは別名で保持する。これらをアプリ機能FAILや正常test実行39件と混同しない。

実行前にWindowsのread-only secretを`unlink`できない問題を静的点検で確認した。`cleanup.py`で、所有確認・container／volume／network消去・残留0確認後、通常fileかつ非symlinkの生成secretだけについて、Windows時に書込属性を戻してから削除する。Linuxの処理・DB／認証処理は変更しない。今回の実cleanupで10 secretと生成realmの削除が成立した。

## 4. 初回実測と未完了

自動runnerは318.775秒でFAIL。`results.json`のcheckpoint確定欄は空で、下記は`observations`に残った途中観測である。S5-V01〜04の最終PASSとして繰り上げない。

| 領域 | 途中観測 |
| --- | --- |
| 起動／migration | 3 DB・realm／backend／helper起動、BFF V1／V2、V2同checksum再適用、seed1回。寿命60／rotation true／MaxReuse0・SSO／Client期限整合 |
| Java JWT期限 | 実行JAR内oauth2-jose7.1.1を取り出しskew60秒をbytecode照合。実JWTの期限前Java404→exp＋65秒で401、refresh0。host／Java／DB UTC差2秒以内 |
| rotation | 専用IdP Sessionでrefresh後の旧token拒否、helperメモリ上token破棄 |
| 並行refresh | 並行2要求200、Keycloak refresh event差分1、token暗号文更新・version差分4、pending=false、旧token再送0 |
| logout競合 | 実refresh成功が戻ってもSession row0、Cookie削除、Session復活なし |
| 依存停止 | IdP stop1.408秒／143、backend stop0.676秒／143、DB stop0.540秒／0、各OOMなし。BrowserとrunnerのDB回復確認を経てrestart-readyへ到達 |
| frontend停止 | SIGTERMで41.313秒・exit137、強制終了あり・OOMなし。runner assertionにより停止 |
| 後段 | 同volume再起動後の保持／復号・pending拒否、最終2300円作成／詳細／提出、最終A→B SSO／SSO logout後B refresh集合、S5-V06は完了していない |

frontend logにはNext起動・Readyが残るが、停止遅延原因を示す追加診断はない。Cloud結果のfrontend0.122秒／143とは異なる断面である。40秒を延長したりexit137を許容して合格にはしていない。

## 5. 診断で分かった範囲と次の案

採用Nextの`node_modules/next/dist/server/lib/start-server.js`はSIGTERMでHTTP server.closeの完了等を待って終了する。現在のBrowser harnessはrestart-readyで3 contextのpageをBFF画面のまま保持しており、DOM表示完了とfile rendezvousだけではserver上の全接続・RSC処理が静穏であることまで観測していない。

これは停止遅延の候補であり、残るHTTP接続が原因と確定した結果ではない。Windows build生成物、Browser／RSC接続、Next停止処理、DB poolを切り分ける必要がある。Framework／Reference改修の根拠は現時点でない。

次の最小案は、Toolingの`browser-s5.mjs`／`verify-s5.py`に静穏条件と停止診断を追加し、Cookie／Sessionをメモリ内に保持したまま停止前に各pageをabout:blankへ遷移して要求を収束させること、必要な接続数／停止phaseを秘密値なしで観測すること。その後に新規projectで同じS5集合を1回確認する案とする。処理中drain成立を目的に変更せず、40秒／exit0・143の合格条件を維持する。

**この停止診断・harness修正と動的再実行は未実施。** [段階別計画§8](../../development/phase4-d3-option2-staged-task-plan-20261109.md#8-段階間の再現実行枠停止)の各集合初回1回・失敗時の診断保全／訂正対象・再実行集合提示に従い、今回は初回結果を提示する。Cloud時の修正後再実行許可をWindowsの追加実行へ流用しない。

## 6. cleanup・export・保持

所有label／service／working_dir／volume／networkを照合したcleanupで、常設4 container・一時helper・専用DB volume／networkの残留0を確認。生成secret10 file・realm削除成功。既存project／volumeへの操作、Docker全体pruneは行っていない。

Windows生成のallowlist archiveは`.tmp/d3-sso-exports/d3-sso-s5-20261109-win01-evidence.zip`。

- bytes：24,248
- SHA-256：`a9134fc0c35d7e0e27318c4f78656c6c7af0a9e10a22e2e8e825226b8088f11b`
- 7 entries：manifest／execution／results／cleanup／runtime-redacted.log／helper.json／archive-manifest。Browserは中断前に最終結果JSONを書いていないためbrowser.jsonなし。
- WindowsでZIP全体hashと内部entry checksumを照合済み。これはWindows生成物の照合であり、Cloud ZIP受領を意味しない。

実行source62 fileをmanifest hashと照合し、当該workの`source-snapshot/`へ保全した。JAR／image／build cache・redacted Evidenceを保持する。Cloud失敗／成功ZIPのdownload・受領checksum照合は未確認OPENのまま。

## 7. 次の判断と保証限界

次は初回失敗・cleanup・Tooling差分の確認と、§5の有限診断／必要最小修正・新規projectでの同一集合1回再実行の判断。手動確認環境の起動保持はまだ行わない。commit／push／PR／merge・Owner結果受入も今回未実施。

Cloud S1〜S5の既存限定結果、方式1保持資源、E1 V10・既存非同期S1の未達／人手HOLD・C2 H07・D1結果受入未記録、D3／Phase4全体の状態を維持する。Windowsの途中観測を全KS再実行・Windows再現完了や本番停止成立へ一般化しない。

## 8. Owner指示後の静穏化・停止診断と同一集合1回

Ownerは「停止前のBrowser接続の静穏化と停止診断をToolingへ追加し、新規projectで同じS5集合を1回再実行してください」と指示した。

`browser-s5.mjs`は停止前の3 pageをabout:blankへ遷移しnetworkidleを待ち、3 BrowserContextのCookieがメモリ内で不変であることを照合してからrestart-readyを通知する。Cookie値・URL・payloadは記録せず、page数・blank／idle／Cookie不変の真偽だけを記録する。

`verify-s5.py`はその静穏条件を検査し、frontend停止の前と、終了待機が続く場合はSIGTERM後2／10／25／35秒にPID1のName／State／Threads／signal maskとport3000のTCP state別件数を有限採取する。実接続先アドレス、環境変数、Cookie／token／要求本文は採取しない。stop結果と診断をassertion前にresultsへ保存し、失敗しても消さない。40秒・exit0／143・OOMなしの条件は維持し、exit137を合格にはしない。

再実行projectは`d3-sso-s5-20261109-win02`。Java／frontend application・依存／lock・生成物を変更していないため、新規buildは反復せず、win01のJAR／image label／frontend context hashを照合して新しい専用tagで使う。DB／secret／realm／workは新規生成する。動的集合はS5-V01〜06を1回、同じbuild30分・動的60分／cleanup10分予約・所有資源条件で実施する。実行結果は追記する。

### 8.1 実行結果

`win02`は315.961秒でFAIL。Node境界39件／skip0・共有guard5件、Browser harness／Python／診断JSの構文確認が成功した後、同じS5動的集合を新規projectで1回実行した。Java／frontend buildは反復せず、JAR hash・frontend context hashとimage labelがwin01と同じことを確認した。更新したTooling source62 fileもmanifestと対応するsnapshotへ保全した。

| 観測 | win02実測 |
| --- | --- |
| Browser静穏条件 | 3 pageすべてabout:blank、networkidle待機成功、3 contextのCookieはメモリ内の比較で不変 |
| frontend停止 | 41.066秒／exit137、強制killあり、OOMなし。40秒・exit0／143条件不成立 |
| 停止前PID1 | Name=`next-server (v`、State=`S (sleeping)`、Threads11、SigIgn／SigCgtを保持。shellがPID1のままという断面ではない |
| port3000停止前 | LISTEN（0A）1、ESTABLISHED（01）15、TIME_WAIT（06）9、FIN_WAIT2（05）1 |
| SIGTERM後2秒 | LISTENなし、ESTABLISHED12、TIME_WAIT10、FIN_WAIT2 3 |
| SIGTERM後10秒 | LISTENなし、ESTABLISHED12、TIME_WAIT10、FIN_WAIT2 2 |
| SIGTERM後25／35秒 | LISTENなし、ESTABLISHED12、TIME_WAIT6、FIN_WAIT2 2。PID1は引き続きsleeping／Threads11 |
| その他の故障停止 | Keycloak1.336秒／143、backend0.640秒／143、DB0.485秒／0、各OOMなし |
| 結果の位置づけ | checkpoint確定欄は今回も空。前半の途中観測を全S5 PASSにせず、再起動保持・最終業務／SSO集合は未完了 |

この診断はTCP namespaceのstate集計であり、各socketのowner PID・相手・HTTP処理状態を特定したものではない。画面遷移の静穏化だけでは接続が収束しないことは実測できたが、12件がPlaywrightのAPIRequestContext、Browser、Docker転送、server側のどの待機に属するかは未確定。SIGTERM後に待受が消える観測はsignal未達／shellだけが受信したという説明とは合わないが、Nextのどのawaitで止まっているかまでは断定しない。

### 8.2 cleanup・証拠・次の調査範囲

win02のcontainer／DB volume／network残留0、生成secret10 file／realm削除が成立。既存`osskk_postgres_unified`はhealthyを維持し、方式1の保護対象を操作していない。

Windows生成archiveは`.tmp/d3-sso-exports/d3-sso-s5-20261109-win02-evidence.zip`、24,805 bytes、SHA-256 `8c435486936356200d2515d1dfeacfe1fec5e44cb76c278005c410cf11c3c801`。初回と別名の7 entriesをexportし、ZIP全体hash／entry checksum・source snapshot62 fileのhashを照合した。診断はresults.json内に含まれる。Cloud ZIPの受領確認は今回も行っていない。

今回許可された静穏化・診断追加と新規projectの同一集合1回は完了した。次の調査候補は、残る接続の所有元・HTTP request完了状態と、Next停止処理の待機phaseの有限観測である。必要ならPlaywright APIRequestContextの接続解放を、Cookie／Sessionのメモリ保持・再起動後の同一Session確認を維持した方法で別途検討する。現時点では当該修正・追加診断実行・3回目のS5実行へ進めていない。判定緩和、処理中drain成立への拡張、Framework／Reference改変、手動環境起動、commit／remoteは行っていない。

## 9. Owner指示後の接続所有元・停止待機箇所の限定調査

Ownerは残存接続の所有元とNext停止処理の待機箇所を次の調査対象として進めるよう指示した。win01／02のFAILとsource／Evidenceを保持し、`d3-sso-s5-20261109-diag01`の新規projectで診断1回を実施する。build30分・動的60分（cleanup10分予約）、同じ資源上限を使い、既存方式1／他projectを操作しない。停止時点に至る既存S5前半の再現を使うが、停止後の保持・最終業務／SSO集合は省略し、S5全体の追加PASS試行とは扱わない。

診断専用`prepare.py --stop-diagnostics`／`verify-s5.py --stop-diagnostics`を明示選択する。通常S5の選択・起動を変えず、Compose overrideで`stop-observer.cjs`を読取専用mountし、Nodeのrequire preloaderとして観測する。Next／node_modules／Framework／Referenceをpatchせず、生成済みJava／frontend imageはhashを照合して再利用する。

- HTTP socketをNode側で追跡し、process所有FDの有無、要求数・active／finish／close数と、harnessが付ける固定分類`page`／`api`／`api-rsc`だけを記録する。URL・query・Cookie・token・payload・接続先アドレスを記録しない。
- Nextが呼ぶHTTP server.closeの呼出し／callback／server close eventと、SIGTERM時点・2／10／25／35秒後の集計を記録する。元の要求処理・close callbackをそのまま呼び、強制socket closeやtimeout延長を行わない。
- 停止10秒後も待機する場合、harnessの3 BrowserContextについてAPIRequestContextだけをdisposeし、Browser Cookieがメモリ内で不変であることを確認する。解放指示時刻と前後のsocket／close完了を照合する。これは診断上の制御で、修正済みの本番停止保証ではない。
- 正常停止した場合も結果は`DIAGNOSTIC_COMPLETE`で、S5 checkpoint PASSは付けない。強制終了はFAILのまま保持し、いずれも専用資源cleanup・秘密削除・exportを行う。

Node24の[HTTP close公式資料](https://nodejs.org/docs/latest-v24.x/api/http.html#serverclosecallback)ではidle connectionをcloseする契約のため、単にkeep-aliveがあるという推測で結論にせず、HTTP request／response完了状態も実測する。観測preloaderには、HTTP応答・close callbackを維持し、疑似secretが出力されないことの専用testを付ける。

実行結果は追記する。追加のS5全体再実行・一般的な終了機構改修・手動環境起動・commit／remoteはこの調査に含めない。

### 9.1 所有元と待機箇所の実測

新規`diag01`の診断1回は321.363秒で終了。frontend停止は41.307秒／exit137（OOMなし）で、runner結果はFAIL。S5のcheckpoint PASSは付与していない。専用observer test1件・既存Node39件／skip0・共有guard5件と構文確認は成功。診断用overrideとpreloaderは明示flag時だけ有効で、通常image・起動設定・Java／frontend生成物のhashは維持した。

| 時点／対象 | 実測 |
| --- | --- |
| SIGTERM直前／HTTP close呼出し | Next PID1がsocket16件を所有。HTTP active要求0。3 socketはHTTP要求・response finish／closeを観測済み、残る13 socketはconnectionのみでrequestイベント0 |
| SIGTERM後2／10／25／35秒 | 既にHTTP要求を扱った3 socketは消失。requestイベント0・active0・finish0の13 socketだけ残り、全てNext process所有FDであることを確認 |
| Windows側 | port13000へのclient接続をprocess別・state別に16回採取。開始時node1＋Chrome15のEstablished、停止待機中は`chrome-headless-shell`のEstablished13件が継続。終了後は消失 |
| API接続解放対照 | SIGTERM後10.087秒に3 contextのAPIRequestContextをdispose。Browser Cookieはメモリ内で不変。13 socket残留・停止待機は変化しなかった |
| Next停止phase | `SIGTERM_observed`→`http_close_called`を記録。35秒後まで`http_close_callback`もHTTP server close eventも発生せず、40秒でDocker強制終了 |

**今回の待機箇所は、NextがHTTP server.closeのcallbackを待つ箇所まで特定できた。** 採用版sourceはそのcallback完了後にnextServer.close等をawaitする順なので、今回の停止はその後段に到達する前である。観測済みHTTP要求は完了しており、未完了の業務要求やDB pool終了待ちを直接の待機箇所とする断面ではない。

所有元については、サーバ側Next PID1の13 FDとWindows側ChromeのEstablished13件の継続、API接続解放の無効果が整合する。残留はAPIRequestContextではなくChrome側の接続保持へ絞れる。ただし、接続先アドレス／4-tupleを保存して1本ずつ対応付けたものではない。requestイベント未発火は確認したが、HTTP headerの部分受信やChromeのpreconnect機能のどれが生成したかまでは観測していないため、「Chromeの特定機能の不具合」とは断定しない。

画面のblank／networkidleはBrowser processの接続pool解放を保証せず、APIRequestContextのdisposeもChrome自身のsocketを解放する操作ではない。このため、今回試した2つの静穏化だけでは停止待機を解消できなかったと評価する。Node24公式の一般的なidle connection説明を、requestイベント未発火socketも今回終了できたという実証へ読み替えない。

### 9.2 証拠・cleanupと次の対処候補

diag01の専用container／DB volume／network残留0、生成secret10 file・realm削除成功。既存`osskk_postgres_unified`はhealthyを維持した。実行source65 fileをhash照合してsnapshotへ保持した。結果文書・README追記は実行後の記録として区別する。

- 主archive：`.tmp/d3-sso-exports/d3-sso-s5-20261109-diag01-evidence.zip`、26,962 bytes、7 entries、SHA-256 `c351040905e2317158c66597047ff9747e01bcf809fe23cf67970fcf84b20870`。HTTP observerはresults／runtime-redacted.logに含む。entry checksum／全体hash照合成功。
- Windows側補助証拠：`.tmp/d3-option2-windows-evidence-20261109-01/diag01-windows-socket-owners.json`、16 samples、SHA-256 `21fd37df9eaadd79b498078e24ec2d5574cdf461b5006fe8337ed4ca4974e531`。保存fieldはutc・process名／PID・state・件数だけ。主archiveには含めず補助証拠として保持。
- 診断summaryとresults／cleanup／executionの保全先：同補助Evidence内の`diag01-evidence-check.json`、`diag01-results.json`等。Cloud ZIP受領とは別である。

次のharness修正候補は、静穏時停止前にBrowser process／contextを明示終了し、CookieをNode harnessのメモリ内だけに保持して、app再起動後の新BrowserContextへ復元する方法。これでBrowser側socket解放と同一BFF Sessionの保持を両立できるかを有限確認する。storageState／Cookieをfileへ書かず、DB／Audit／暗号鍵・同一Session IDの照合条件も維持する。

これは静穏時の検証harnessを整える候補であり、接続したままの実Browserや処理中要求が存在する本番での終了・drainを直す一般対策ではない。本番向けには未使用／未完了接続と処理中業務要求を区別した受付終了・猶予・接続解放方針を別途設計する。Next／Nodeのlibrary直接patch、全接続の即時強制破棄、stop猶予の延長で今回の判定を通す修正は行っていない。

**今回の依頼である所有元・待機箇所の限定調査は終了。** 上記harness対処・S5全体の再確認は未実施であり、調査結果・差分確認後の次の判断とする。手動環境起動・commit／remoteは未実施。

## 10. Browser終了・Cookieメモリ復元によるharness修正

Ownerは「停止前にBrowser自体を終了し、Cookieをメモリ保持して再起動後に復元するharness修正へ進めます」と指示した。ToolingのBrowser harness／runnerだけを修正し、新規`d3-sso-s5-20261109-win03`で同じS5-V01〜06を1回確認する。build30分・動的60分（cleanup10分予約）・既存資源／停止／cleanup上限を維持する。Framework／Reference／Next本体・依存／lock・業務DDL・frontend applicationは変更しない。

通常S5では3 contextのCookieをNode harness内で取得し、Browser.close完了・切断確認後にrestart-readyを通知する。Cookie／storageState／tokenはfileへ保存しない。runnerが同volume・同鍵・DB／Audit／schema／realm保持を照合してから、新しいBrowserと3 contextへCookieを復元する。値・domain／path・期限・HttpOnly／Secure／SameSite等の属性と同一BFF Session IDをメモリ内で比較し、比較用配列は照合後に空にする。

復元後は新しいrendezvousを置き、まだHTTP要求を出していない時点でDB snapshot・IdP refresh eventが変わらないことをrunnerから再照合する。その後に従来のpending拒否・復号・本人SSR／最終業務／独立SSOを続ける。requestイベント未発火のBrowser接続を停止前に残さない静穏時testであり、接続したままの実Browser／処理中drainの保証にはしない。

診断用flagの旧blank／API解放対照は別モードとして維持し、通常再現ではdiagnostic override／preloaderを有効にしない。40秒／exit0・143、業務POST自動再送なし、同一Session・DB／Audit／鍵の判定は変更しない。JAR／frontend生成物はhashを照合して専用tagで再利用、DB／secret／realmは新規作成する。結果は追記する。

### 10.1 win03の実測と残るCookie検査

通常S5を新規projectで1回実行し、312.689秒でFAIL。Node境界39件／skip0・共有guard5件・構文確認は成功。Java／Nextの再buildはしていない。JAR／frontend contextとimage labelは前回と同hashで、今回のTooling source65 fileはsnapshotへ保全した。

| 確認 | win03結果 |
| --- | --- |
| 停止前 | Browser.close完了・切断確認。3 contextのCookieはNodeメモリだけに保持、Cookie file作成なし |
| frontend停止 | **0.413秒／exit143、強制killなし・OOMなし**。前回までの40秒超過はこの静穏条件で再現しなかった |
| backend／Keycloak停止 | 0.758秒／143、1.389秒／143。各強制kill・OOMなし |
| frontend停止前socket | LISTEN1、ESTABLISHED1。Chrome由来の未使用13接続を残した前回とは異なる |
| 同volume／同鍵再起動 | 業務値・Audit数・Session hash／暗号文／pending・BFF V1／V2 checksum・realm IDのsnapshotと鍵hashが停止前後で一致。再seed／migrationなし |
| 新Browserへの復元 | addCookies後のCookie値・属性完全一致検査で停止。同一Session ID確認・復元後DB照合のrendezvous前に終了 |
| 未完了 | 復元Cookieでのpending拒否／本人SSR・最終業務作成／提出・独立SSO・S5-V01〜06の最終確定 |

これにより、**Browser接続を終了する静穏時harness修正はfrontend停止を改善した**と評価する。接続したままのBrowser／本番drainに対する一般対策にはしない。Cookie復元検査のFAILは別に保持し、停止成功だけでS5 PASSにはしない。

今回の比較logは一致／不一致だけで、実Cookieの値や属性内訳を保存していないため、どのfieldが違ったかは未特定。原因を期限精度やSameSite等と推測して許容値を増やす変更はしていない。

切り分けとして、同じWindows Chromiumで実credentialを使わない次の限定probeを各1回実施した。addCookies由来の合成Cookie2件、HTTP Set-Cookie／localhostの1件、HTTP Set-Cookie／idp.localhostの1件は、Browser終了→新Browserへのメモリ復元で差分なし。一般的なCookie往復では再現せず、実認証後のCookie集合のどの差分かはなお確認が必要である。container／業務要求は使っていない。

実行後、harnessにcontext別の期待／実際のCookie件数、missing／unexpected件数、変更field名だけをobsへ残す処理を追加した。Cookie名・値・domain／path・時刻は出さず、完全一致・同一Session IDの判定は維持する。この追加観測は構文／静的点検のみで、実Cookie集合での追加実行は未実施。win03の実行source snapshotと、この実行後差分は区別する。

### 10.2 cleanup・export・次の作業

win03の専用container／volume／network残留0、10 secret／realm削除成功。既存`osskk_postgres_unified`はhealthyを維持した。Windows生成archiveは`.tmp/d3-sso-exports/d3-sso-s5-20261109-win03-evidence.zip`、29,028 bytes、8 entries、SHA-256 `fcb8ac44718cd5382dac06a8316999728dafcf3e3f9bd23a755d26a9d25c37ac`。全体hash・entry checksum・source snapshot65 fileを照合済み。初回／win02／diag01の証拠は上書きしない。Cloud ZIP受領は未確認のまま。

補助Evidenceの`win03-evidence-check.json`、`win03-results.json`／`browser.json`等と、`cookie-roundtrip*.log`へ追跡する。次は追加した秘密非露出の差分観測で、実認証後のCookie件数／fieldの違いを特定し、必要最小差分を判断する。Cookie属性・期限・同一Session IDの検査を無条件に省略しない。追加の通常S5集合・手動環境起動・commit／remoteは今回先行していない。

## 11. Cookie差分観測付き追加S5（win04）

Ownerの「追加S5実行へ進めましょう」により、追加済みの件数・変更field名観測を使い、新規`d3-sso-s5-20261109-win04`で通常S5集合1回を実行する。完全一致・同一Session ID・40秒／exit0・143の条件は維持する。Java／frontend application・生成物・依存／lockは変更せずhash照合で再利用、work／DB／secret／realmは新規。build30分・動的60分（cleanup10分予約）・従来の資源／cleanup条件を維持する。

今回の目的は実Cookie復元の比較差分を秘密値なしで特定し、通過する場合はそのまま最終業務／SSOまで確認すること。差分があれば値を出力したり比較を緩和せず結果を記録する。実行結果は追記する。手動環境起動・commit／remoteは含めない。

### 11.1 win04実測

新規projectで通常S5を1回実行し、316.882秒でFAIL。前回追加した件数・field名観測と構文確認だけを入力差分とし、Java／Next build・Node39件・guard5件は今回反復していない。JAR／frontend context・image label照合、source65 fileのsnapshot保全は成立した。

| context | 期待Cookie数 | 復元後 | missing | unexpected | 残存Cookieの変更field |
| --- | --- | --- | --- | --- | --- |
| 0：申請者A | 6 | 5 | 1 | 0 | なし |
| 1：申請者B | 5 | 4 | 1 | 0 | なし |
| 2：pending拒否用独立context | 5 | 5 | 0 | 0 | なし |

照合keyはCookieのname／domain／pathで、内部比較だけに使用し証拠へ値を出していない。残ったCookieについてvalue／expires／HttpOnly／Secure／SameSite／partitionKeyの変更は観測されなかった。したがって今回の差分は、既存Cookieの書換えではなく2 contextでの各1件の欠落まで絞れた。ただし、missing対象の役割・期限状態を記録していないため、自然失効か復元時の非受入かは未特定。

frontend停止0.413秒／143、backend0.524秒／143、Keycloak1.498秒／143、全て強制kill・OOMなし。同volume／同鍵再起動後の業務／Audit／Session hash・暗号文／schema／realmのsnapshotは一致した。Browser復元後rendezvous前で止まったため、同一Session IDの最終照合・pending拒否・最終業務／SSO集合とS5 checkpoint確定は未完了。停止改善の再確認をS5全体PASSにしない。

### 11.2 保全と次の確認対象

専用container／DB volume／network残留0、10 secret／realm削除成功。既存`osskk_postgres_unified`はhealthyを維持し、方式1資源を操作していない。

Windows archiveは`.tmp/d3-sso-exports/d3-sso-s5-20261109-win04-evidence.zip`、28,383 bytes、8 entries、SHA-256 `c4f57d7977525628673b8a66810e7acc9099872f1e9c1f7927743b3687472222`。ZIP全体・entry checksum・source snapshot65 fileを照合した。補助Evidenceは`win04-evidence-check.json`、`win04-results.json`／`browser.json`等に保持。過去のFAIL・Cloud結果を上書きせず、Cloud ZIP受領未確認を維持する。

次の確認対象は、missing Cookieの匿名役割分類（BFF SessionかIdP側等）と、取得時／復元時点の期限内・期限切れの区別、および必須BFF Session Cookie3件の一致を個別に確認することである。Cookie値・scope・生の時刻を出力せず、役割と期限判定の真偽・件数だけを追加する案とする。期限切れCookieの欠落と必須Sessionの欠落を混同せず、原因確認前にmissingを一律許容したりCookie完全一致を無効にしない。

今回指示された追加S5集合1回は終了した。上記の追加観測・再実行、手動環境起動・commit／remoteは未実施。

## 12. 欠落Cookieの役割・期限状態確認（win05）

Ownerは欠落Cookieの役割と期限状態の確認へ進めるよう指示した。Toolingの観測だけを追加し、新規`d3-sso-s5-20261109-win05`で1回確認する。既存のS5前半・停止／再起動の再現を使い、復元検査に通過すれば後段へ進むが、完全一致・必須Session ID・40秒／exit0・143の条件は変更しない。build30分・動的60分（cleanup10分予約）・従来資源上限を維持する。

missing対象は固定の匿名役割（BFF_SESSION／BFF_AUTH_FLOW／BFF_LOGOUT／IDP_AUTH_SESSION／IDP_AUTH_RESTART／IDP_IDENTITY／IDP_SSO_SESSION／IDP_OTHER等）へ分類し、取得時・注入直前・読取後のsession／valid／expired／unknownと、取得時残存時間のbucketだけを記録する。Cookie名・値・domain／path・失効の生時刻は記録しない。Secure／HttpOnly／SameSiteの設定値は診断属性として保持する。

3つの必須BFF Sessionは集合の完全一致検査とは別に、存在・同一Session ID・全属性一致・復元時に期限切れでないことを真偽で先に観測する。これで一時IdP Cookieの欠落と、必須BFF Sessionの欠落を区別する。失効を確認する前にmissingを許容したり、寿命を延長する変更は行わない。

Java／frontend生成物・依存／lockは変更せずhash照合で再利用、DB／secret／realmは新規。結果は追記する。手動環境起動・commit／remoteは含めない。

### 12.1 役割・期限と必須Sessionの実測

新規win05で1回確認し、313.254秒でCookie集合の完全一致検査によりFAIL。今回の調査目的である欠落対象の役割・期限状態と、必須BFF Sessionの保持は確認できた。比較を通過させる変更はしておらず、S5 checkpoint確定／後段は未完了のままとする。

| context | 欠落の匿名役割 | 取得時 | 取得時の残存bucket | 注入直前 | 復元後読取時 |
| --- | --- | --- | --- | --- | --- |
| 0：A | IDP_AUTH_SESSION_HASH 1件 | valid | 5〜30秒 | expired | expired |
| 1：B | IDP_AUTH_SESSION_HASH 1件 | valid | 0〜5秒 | expired | expired |
| 2：独立context | 欠落0件 | — | — | — | — |

分類は固定名`KC_AUTH_SESSION_HASH`だけを`IDP_AUTH_SESSION_HASH`へ対応付ける。採用Keycloak26.8.1の[CookieType source](https://github.com/keycloak/keycloak/blob/26.8.1/server-spi-private/src/main/java/org/keycloak/cookie/CookieType.java)で、この認証session hash用CookieがFEDERATION_JS・既定MaxAge60秒と定義されていることを確認した。これはBFFの`D3_A_SESSION`やIdPの`KEYCLOAK_SESSION`とは別のCookieである。実行前にこの分類を補足し、旧準備manifestを別名保存したうえで実行source hash／snapshotを確定した。runtime開始後のsource／manifest書換えはしていない。

| 必須BFF Session | 存在 | 同一Session ID | 全属性一致 | 復元時に有効 |
| --- | --- | --- | --- | --- |
| context0 | true | true | true | true |
| context1 | true | true | true | true |
| context2 | true | true | true | true |

残存Cookieの変更field0・unexpected0も維持した。欠落がBFF認証Sessionの消失ではなく、取得から再注入までに期限切れになったIdP補助Cookieであることを今回の実測で区別できた。

### 12.2 停止・保全と次の比較修正案

frontend0.412秒／143、backend0.654秒／143、Keycloak1.502秒／143、強制kill／OOMなし。同volume／同鍵で業務／Audit／Session暗号文等のsnapshot不変も成立。完全一致検査で止まったため、その後のpending拒否・本人SSR／最終業務／SSO・S5全体PASSはまだ確認していない。

専用container／DB volume／network残留0、10 secret／realm削除成功。既存`osskk_postgres_unified`はhealthyを維持した。archiveは`.tmp/d3-sso-exports/d3-sso-s5-20261109-win05-evidence.zip`、29,750 bytes、8 entries、SHA-256 `567aa455a24f35e1a52f2692970eb9dc389066daeaa9da59746dccfd3879fa13`。全体hash・entry checksum・source snapshot65 file照合成功。補助Evidenceは`win05-evidence-check.json`／`win05-browser.json`等。Cloud証拠受領確認とは別で、過去FAILを上書きしない。

次の最小修正案は、**注入時点で期限切れのIDP_AUTH_SESSION_HASHだけを復元・完全一致の期待集合から除外する**こと。再作成や寿命延長はせず、期限切れCookieが復活しないことを確認する。まだ有効な同Cookieとその他のCookieは値・属性の厳密比較を維持し、必須BFF Session3件の存在・同一ID・全属性・有効性は例外なく要求する。missingの一律許容やIdP SSO Cookie全般の除外には広げない。

今回の確認は終了し、この比較修正と新規projectでのS5全体再確認は未実施。Ownerの次の判断を待つ。手動環境起動・commit／remoteは未実施。

## 13. 期限切れ補助Cookieの期待集合訂正（win06）

Ownerは、期限切れになった当該補助Cookieだけを期待集合から除外するharness修正へ進めるよう指示した。Toolingの選択policyを`cookie-replay.mjs`へ分離し、新規win06で通常S5集合1回を確認する。従来のbuild30分・動的60分（cleanup10分予約）・資源上限を維持する。

除外条件は、名前が`KC_AUTH_SESSION_HASH`、domainが固定IdP、pathが当該realm、expiresが有限・非負かつ注入時点以下の全条件一致だけとする。期限がsession／unknown、有効期間内、他Cookie名／domain／realmは例外に含めない。除外したCookieをaddCookiesへ渡さず、復元後に存在しないことを検査する。残りは値・属性の完全一致、必須BFF Session3件は存在・同一ID・全属性・有効性を引き続き要求する。

境界test5件で、期限境界、有効／session Cookieの保持、必須Session・他IdP Cookieの保持、scope／不正expiresの非除外、不正clock拒否を確認する。既存Node39件／guard5件も確認する。通常sandboxのtest子process spawn EPERMは権限付き同範囲の成功と区別して保持する。

Java／frontend application・生成物・依存／lock・Framework／Referenceは変更せず、生成物hash照合で再利用する。DB／secret／realmは新規。実行結果は追記する。手動環境起動・commit／remoteは含めない。


### 13.1 win06：Windows S5限定技術PASS（2026-11-10）

新規project `d3-sso-s5-20261109-win06`で通常S5集合を1回実行し、**S5-V01〜06全件PASS**。開始は2026-11-09 23:58:53.897 JST、終了は2026-11-10 00:04:15.945 JST、経過322.048秒。日付をまたいだがproject名と証拠識別は変更していない。

境界test5件、既存Node39件、共有guard5件は全件成功。境界testは通常sandboxの子process spawn EPERM後、権限付き同範囲で成功した。初回Windowsで生成したJava／frontend生成物をhash・image label照合で再利用し、今回Java23件・Next／image buildは反復していない。実行時source snapshot67 fileと照合を保持し、この結果・入口の文書追記は実行後の差分とする。

| context | 取得時Cookie数 | 復元期待数 | 期限切れ補助Cookieの除外 | 除外Cookieの非復活 |
| --- | --- | --- | --- | --- |
| 0：A | 6 | 5 | 1 | true |
| 1：B | 5 | 4 | 1 | true |
| 2：独立context | 5 | 5 | 0 | true |

除外は§13の固定名・固定IdP／realm・注入時期限切れ条件だけに限定した。復元対象の値・全属性は完全一致し、必須BFF Session3件は存在・同一Session ID・全属性一致・復元時有効が全てtrue。元の取得集合との診断比較に残るmissing2件は、上記の期限切れ除外2件に対応する。Cookieはメモリだけで保持し、Cookie／storageState fileの保存・寿命延長・期限切れCookieの再作成はしていない。

| 停止対象 | SIGTERMから終了 | exit | 強制kill／OOM |
| --- | --- | --- | --- |
| frontend | 0.412秒 | 143 | なし |
| backend | 0.662秒 | 143 | なし |
| Keycloak | 1.465秒 | 143 | なし |

従来の40秒／exit0・143条件を維持して停止成功。同volume／同鍵再起動後の業務・Audit・Session暗号文等のsnapshotは一致し、Browser復元後のHTTP要求前にもDB／IdP eventの不変を照合した。再seed・migration再実行による作り直しはしていない。

S5-V01〜06では、並行refresh・logout競合、利用者分離・実JWT期限拒否、依存停止回復、静穏時停止／同volume・同鍵保持、復元後のpending拒否・本人SSR・最終業務・独立SSOを確認した。最終業務は申請2件／明細2件、保持確認用draft1,200円と最終提出2,300円、提出成功Audit1件。失敗POSTの自動再試行なし。SSO logout後に残るBのJWT200→refresh401→silent `login_required`、refreshエラーevent1件・B Session削除も成立した。

専用container／DB volume／network残留0、生成した10 secret／realm削除成功。既存`osskk_postgres_unified`はhealthyで、方式1保持資源を操作していない。Windows archiveは`.tmp/d3-sso-exports/d3-sso-s5-20261109-win06-evidence.zip`、31,496 bytes、8 entries、SHA-256 `180a71da415108394aea45e64b20894ae64468632998732c6857b7c73203e155`。ZIP全体hash・全entry checksum・実行source67 fileの照合成功。補助Evidenceは`.tmp/d3-option2-windows-evidence-20261109-01/`の`win06-evidence-check.json`、`win06-results.json`、Browser／cleanup／execution記録と各test logへ追跡する。

今回のPASSはWindowsのS5-V01〜06に限定する。KS01〜10全段階・S1〜S4の全負例・通常rootを今回全て反復した意味ではなく、Browser終了済みの静穏時停止を処理中drainの実証へ拡張しない。Cloud ZIP受領確認はOPENのまま。Framework／Reference／依存／lockの変更、手動環境起動・Owner手動確認／結果受入、commit／pushは未実施。次は結果・差分の確認とOwnerの受入／成果固定・手動確認方針の判断であり、追加動的実行を自動で先行しない。


## 14. 成果固定・Cloud再検証への引継ぎ（2026-11-10）

OwnerはここまでのWindows Tooling・検証文書を、Cloud再検証引継ぎを含めてcommit・同作業branchへpushするよう指示した。[引継ぎ](../../development/phase4-d3-option2-cloud-harness-reverification-handoff-20261110.md)に開始条件・単体集合・通常S5全6群1回・資材再生成・証拠返却を具体化した。旧Cloud PASSは修正前harnessのため、修正後のLinux回帰確認は未実施として残す。

この成果固定操作はWindows結果のOwner最終受入やCloud実行を意味しない。手動環境起動・PR／merge・tag／Releaseも含めない。commit SHAとpush後local／remote一致は完了報告で示す。上記各節のcommit／remote未実施表示は、この指示前の実行履歴として保持する。

# D3方式2：Windows修正後harnessのCloud再検証結果（2026-11-10）

## 1. 判定・対象

**修正後harnessのCloud限定回帰PASS。** 新規project `d3-sso-s5-20261110-cloud01`で通常S5-V01〜06が全件PASSし、cleanup・生成secret／realm削除・redacted証拠exportが成立した。動的集合は指示どおり1回、失敗・追加再実行なし。

Ownerの「再検証へ進めましょう」により、[引継ぎ§2〜5](../../development/phase4-d3-option2-cloud-harness-reverification-handoff-20261110.md)のCloud限定回帰確認を実施した。対象はWindowsで加えたBrowser終了・Cookieメモリ復元・期限切れ補助Cookieの選択policy・停止診断の分離・Windows secret削除対応のLinux回帰である。旧Cloud／Windows結果とは別の証拠として記録する。

S1〜S4の全負例・通常root全件・停止診断projectは反復していない。Framework／Reference／POM／依存／lock・認証方式・本番設定・harness sourceは変更していない。今回のCloud結果をWindows手動確認やD3／Phase4全体の受入へ拡張しない。

## 2. source・環境・生成物

branchは`docs/d3-option2-implementation-conditions-20261109`、開始commitは`5ac06e361cb69878dbeb7c765173a7c82849bd91`。開始時cleanを確認し、source snapshot67 fileをmanifest hashと照合して保持した。以前の未コミットS5証拠受領記録はstashと`.tmp/d3-cloud-handoff-sync-20261110/`のpatchへ保全した。実行後の結果・入口追記を実行時sourceと区別する。

Linux amd64、cgroup実効4 CPU／16 GiB、Docker28.4.0／Compose2.40.3／Buildx0.31.0、JDK21.0.12.1／Python3.12.14、Node24.19.0／npm11.9.0を再確認した。開始時container0・port13000／18080未使用、既存匿名volume2件を記録した。Cloud runtime policyは現executorでcurrent／enforced、既存のCloud専用CA／Maven settings・toolchainsを照合して利用し、TLS検証を無効化していない。

専用lockから`npm ci --ignore-scripts`を1回実行し、Chromiumを今回専用cacheへ新規install、151.0.7922.34のheadless起動を確認した。Next16.4.5を1回buildし、Javaは共通`build-reference.py --stage S2`で23件を含めてclean packageした。backend／frontendも生成contextから各1回、逐次buildした。旧imageのretagによる代用はしていない。

生成workの開始manifestだけを実行前に補正した。prepareの旧S5記述（直接回帰0、Next build3回等）を今回の23／5／1／39／5件・Next build1回・各image build1回・通常S5 runtime1回へ置き換え、元manifestのhashと補正理由を保持した。コード・recipeは変えず、補正後manifestを確定してから実行した。

| source→artifact | 今回の値 |
| --- | --- |
| Java JAR SHA-256 | `97fa0a995611379453125d6052950bd7b373481e619697eab1e884cf51f5c9d8` |
| backend image | `sha256:b85c1f3d909f52263e94a4b6a381e74beffa6695a74c1c016a5943bb85f4c739` |
| frontend context SHA-256 | `f267d4cf68aec20e4c29e4533b2c025dc7be3fa9a717d1614a05f890a26ecadc` |
| frontend image | `sha256:2a227229f7b853801b20f89346ff1bd587c7027e203ff464000550ec90093781` |
| build／動的枠 | build 203.6秒／上限30分、動的 370.731秒／上限60分（cleanup10分予約） |
| 動的開始 | 2026-11-10 00:34:25 JST |

常設4 container計4 CPU／5 GiB、PG接続32。helperはfrontendと非並走、Browser worker1／同時context最大3を維持した。新しいBrowser cache・JAR・contextを用い、OS間のbyte一致を成功条件にはしていない。

## 3. 検証結果

| 集合 | 結果／skip |
| --- | --- |
| Cookie選択policy境界 | 5 PASS／0 |
| 停止observer専用単体 | 1 PASS／0 |
| frontend既存境界 | 39 PASS／0 |
| 共有所有物guard | 5 PASS／0 |
| Java選択直接回帰 | 23 PASS／0（4 class、共通build入口） |
| npm／Chromium／Next／image | npm ci・新規Chromium準備・Next build各1回、backend／frontend image各1回成功 |
| 通常S5 | S5-V01〜06を新規projectで1回、全PASS |

| ID | Cloud実測 |
| --- | --- |
| S5-V01 | 並行2要求200×2、実refresh event差分1・旧token再送0、暗号文変更・version差分4・pending=false。独立Sessionでrotation後の旧token拒否。実refresh成功とlogout競合でSession復活なし。結果不明は単体stub |
| S5-V02 | 2利用者SSR／RSC／API／Cookie／cache分離、本人200／他人404。helper内に未更新の実JWTを保持し、Javaへ直接送り期限前404→exp＋実効skew60＋5秒後401。JWT時刻とUTC時計・実行JAR内のSpring Security7.1.1を照合 |
| S5-V03 | IdP停止でlogin503・refresh401、別の既存JWT経路は404まで到達。backend停止中の業務POST1回は503、業務／Audit不変・再送なし。各依存回復後ready |
| S5-V04 | DB停止でready503／live200／API503、回復後ready／本人API200／新規IdP login成立。自動restart0・OOM0、更新重複なし |
| S5-V05 | Browser.close完了→3サービスSIGTERM→同volume／同鍵snapshot一致→新Browser／3 contextへCookie復元。復元後HTTP前にDB／IdP event不変。pending専用fixtureだけ401／Session削除・交換0、通常2利用者の復号／SSR成功 |
| S5-V06 | 最終artifactでBrowser作成201→本人詳細200→提出204→reloadとA→B独立SSO。申請2件／明細2件（保持draft1200円・最終提出2300円）／提出成功Audit1件。SSO logout後のB既存JWT200→refresh401→silent login_required。client bundle7件のJWT非露出、cleanup・secret削除 |

通常S5で`--stop-diagnostics`は指定せず、observer注入Compose／HTTP monkeypatchは有効にしていない。専用observerの確認は上記単体1件だけ。通常runnerのPID状態・socket集計は値を限定した読取観測で、Cookie・URL・環境値は保存しない。

## 4. Browser終了・Cookie復元・停止

停止前に3 contextのCookieをNode harnessメモリへ取得し、Browser.close完了／disconnectedを確認した。Node／Java／Keycloakを停止・同volume／同鍵で再起動し、新Browserの3 contextへ戻した。Cookie／storageStateを返却用fileへ保存していない。

`KC_AUTH_SESSION_HASH`、固定IdP `idp.localhost`、固定realm path、注入時点で期限切れという条件をすべて満たす補助Cookieだけを除外する。有効な補助Cookie・session／unknown期限・その他Cookieは厳密比較を維持し、期限延長や期限切れCookieの再作成は行わない。LinuxとWindowsで除外件数が同じことは合格条件にしない。

| context | 取得Cookie数 | 復元期待数／実数 | 期限切れ補助Cookie除外 | 除外Cookie非復活 |
| --- | --- | --- | --- | --- |
| 0 | 6 | 5／5 | 1 | true |
| 1 | 5 | 4／4 | 1 | true |
| 2 | 5 | 5／5 | 0 | true |

A／Bの除外各1件は、取得時valid・注入前expiredのIDP_AUTH_SESSION_HASHで、Windowsと同じ選択条件に合致した。Aの取得時残存bucketは30〜120秒、Bは0〜5秒。元集合との比較ではこの2件だけmissing、unexpected0・残存Cookieの変更field0。今回たまたまWindowsと同じ除外件数だったが、件数を固定した判定ではない。

| 必須BFF Session | 存在 | 同一Session ID | 全属性一致 | 復元時有効 |
| --- | --- | --- | --- | --- |
| context0 | true | true | true | true |
| context1 | true | true | true | true |
| context2 | true | true | true | true |

復元対象Cookieのvalue・expires・HttpOnly・Secure・SameSite・partitionKey等の完全一致を保持し、3件のBFF Sessionを個別にも確認した。復元後HTTP前の業務／Audit／Session暗号文／schema／realmとIdP refresh eventは不変で、再認証によるSessionの作り直しを保持成功とは扱っていない。

| 停止対象 | Cloud秒数／exit | Windows win06秒数／exit | 強制kill／OOM |
| --- | --- | --- | --- |
| frontend | 0.201／143 | 0.412／143 | なし／なし |
| backend | 0.446／143 | 0.662／143 | なし／なし |
| keycloak | 1.199／143 | 1.465／143 | なし／なし |

40秒以内・exit0または143の条件を全て維持した。停止秒数は負荷性能比較や処理中drainの証明にはしない。

## 5. cleanup・export・証拠の状態

専用container／一時helper／DB volume／networkは残留0、生成secret10 file・realm削除済み。以前の匿名volume2件・旧Cloud／Windowsのwork／archive・image等は操作していない。今回のimage・JAR・cache・source snapshot・redacted Evidenceは保持する。

| 新Cloud archive | 内容 |
| --- | --- |
| file | `.tmp/d3-sso-exports/d3-sso-s5-20261110-cloud01-evidence.zip` |
| bytes | 31,633 |
| SHA-256 | `098387dfd60de40ec0cb80b03cf812218c30b96e24254f2c751d5f29541ca1e6` |
| entries | manifest／execution／results／cleanup／runtime-redacted.log／browser.json／helper.json＋archive manifestの8件 |
| 照合 | JSON schema、全entry checksum、全体SHA-256、source snapshot67 file・実行source同一、所有物残留0を確認 |
| 持帰り | export済み。この新ZIPのWindows取得／SHA-256照合は未確認 |

secret／生成realm原本・token／code付きURL／Cookie値／storageState／trace／HAR・JAR／image／DB dumpはZIPへ含めない。補助Evidenceは`.tmp/d3-option2-cloud-reverify-20261110-01/`の開始条件・npm／Browser準備・各test／Next／Java build・final-reviewと、実行workのimage build log・source snapshotに保持する。結果追記時もarchiveは変更していない。

最終logはbackend INFO121／WARN9／ERROR0、Keycloak ERROR0、BFFは起動・Readyのみ。DB ERROR2件は初期製品probeのmigration_model／databasechangeloglock不存在で、その後全journeyが成立した。生成secret値／JWTのlog露出、D3_STOP_DIAGによるobserver log、Session属性重複キー例外は検出されていない。

**以前のS5証拠受領の整合：** このセッションでOwnerは旧Cloud最終PASSの`d3-sso-s5-20261109-02-evidence.zip`をダウンロードし、SHA-256相違なしと報告済みである。対象は29,577 bytes／SHA-256 `9e38be2845c55f5b706273fada15bf23e260a24da78d876d18bc2217ef07e7cb`。旧Cloud原本のhashも再確認した。Windows引継ぎ文書に残る従来Cloud ZIP受領OPENは、この旧最終PASS ZIPに限りOwner報告で解消済みとする。新Cloud ZIP・旧FAIL ZIP・他段階のZIP受領へは拡張しない。元の受領記録patch／stashも保全し、この新規exportを受領確認の根拠にはしていない。

## 6. 限界・次の判断

停止はBrowserを終了した静穏時の確認であり、処理中業務POSTや長時間requestのdrain実証ではない。交換結果不明の境界は単体stub、pending復帰拒否はtest-owned SQL fixtureであり、実network応答喪失・process crash全般の保証へ拡張しない。同一Origin／Next process内のlocal HTTP構成で、別Origin企業SSO・backchannel logout・本番TLS・鍵rotation・負荷／長期安定は対象外である。

旧CloudのS1〜S5断面・Windows win01〜06と診断履歴を保持し、本結果を修正前artifactの証拠へ遡及適用しない。Windows win06との比較は同じ判定条件に対するOS別の限定結果であり、image／JAR byte一致や停止秒数・Cookie除外件数の一致を要求しない。

今回の結果commit／push、PR／merge・tag／Release、手動環境起動・Owner打鍵・結果全体受入は実施していない。

## 7. 結果文書の成果固定指示（2026-11-10）

Ownerは「結果文書など3件をコミットして、pushまで進めましょう」と指示した。本結果・Cloud再検証引継ぎ・AGENTSの文書3件をlocal commitに固定し、同じ作業branchへpushする。実際のSHAとpush後のlocal／remote一致は操作後の報告とGit履歴で確認する。上記の結果commit／push未実施表示はこの後続指示前の履歴であり、PASSの検証範囲・旧FAIL履歴・証拠の保持条件は変更しない。新Cloud ZIPのWindows受領・手動打鍵・結果全体受入、PR／merge・tag／Release・D3／Phase4全体の認定へは拡張しない。

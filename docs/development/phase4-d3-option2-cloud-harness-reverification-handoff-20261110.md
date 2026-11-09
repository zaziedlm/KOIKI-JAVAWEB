# D3方式2：Windows修正後harnessのCloud再検証引継ぎ（2026-11-10）

## 1. 目的と今回の指示

OwnerはWindows検証までの成果と本引継ぎをcommit・作業branchへpushし、Codex Cloudで点検する方針を示した。対象branchは`docs/d3-option2-implementation-conditions-20261109`。開始入力はpush完了報告の実commit SHAとする。本書へ自己commitのSHAを埋め込まず、Cloud開始時のmanifestに記録する。

旧CloudのS5 PASSは修正前harnessの証拠である。Windowsでは修正後の新規win06でS5-V01〜06が全件PASSしたが、修正後harnessのLinux実行はまだ確認していない。今回のCloud作業はこの回帰確認であり、S1からの実装やWindows検証のやり直しではない。

[Windows結果§13.1](../architecture/validation/phase4-d3-option2-windows-reproduction-20261109.md#131-win06windows-s5限定技術pass2026-11-10)、[元Cloud S5結果](../architecture/validation/phase4-d3-option2-s5-results-20261109.md)、[段階別計画§11](phase4-d3-option2-staged-task-plan-20261109.md#11-cloud実装自動検証windows再現手動確認)、[実行README](../../build-support/container-sso-verification/README.md)を併読する。

## 2. 点検する差分と維持する条件

| 差分 | Cloudでの確認 |
| --- | --- |
| 通常S5の停止前にBrowser自体を終了 | 新Browser・3 contextへの復元までLinuxで成立すること |
| CookieをNodeメモリだけで保持 | Cookie／storageState fileなし、復元後のHTTP要求前にDB／IdP event不変 |
| 期限切れ補助Cookieの選択policy | `KC_AUTH_SESSION_HASH`かつ固定IdP／realmかつ注入時期限切れだけを除外。除外Cookieの非復活 |
| 残りのCookie比較 | 値・全属性完全一致、必須BFF Session3件の存在・同一ID・全属性・有効性を維持 |
| 停止診断・observer | 通常S5ではobserverを有効にしない。専用単体testのみ確認 |
| Windows read-only secret削除対応 | Linuxで既存の所有権確認・cleanup・secret削除が成立すること |

有効な補助Cookie、session／unknown期限、他のCookie名／domain／realmは除外しない。Linuxの処理が速く、期限切れ除外件数が0になっても、それ自体はFAILではない。有効Cookieは厳密比較し、除外境界は単体testで確認する。件数をWindowsのA／B各1件へ固定しない。

Framework／Reference／POM／依存／lock・認証方式・本番設定は変更していない。40秒以内・exit0または143・強制kill／OOMなしの停止条件を維持する。Browser終了済みの静穏時停止であり、処理中drainを実証した扱いにしない。

## 3. Cloud開始条件と資材再生成

1. Cloud環境のruntime guidanceを読み、指定branch・報告SHA・`git status --short`のcleanを照合する。旧commitでは開始しない。
2. 計画§11.2に従い、Docker Linux／Compose／Buildx・amd64・資源・loopback13000／18080・固定issuerの接続、JDK21（`javap`含む）・Maven Wrapper・Python・Node24.19.0／npm11.9.0・CA／proxyを確認して記録する。
3. Windowsの`.tmp`／JAR／image／DB／鍵／node_modules／Browser cacheは移植しない。Cloudの既存資材も残存を前提にせず、今回のsourceから再生成する。
4. 専用frontendでlockから`npm ci --ignore-scripts`を行い、固定PlaywrightのChromiumとLinux共有libraryを準備する。計画§11.3とREADMEのLinux手順でNext standaloneをbuildする。
5. Javaは`build-reference.py --stage S2 --output <新規出力先>`を使用して生成し、4 class／23件の選択直接回帰を記録する。これは新しいCloudで入力JARを準備する確認であり、Framework全体の再検証ではない。Maven settings／toolchains等は実環境で確定する。
6. JAR→backend image、frontend context→imageのhash／label・source snapshotを記録する。WindowsとのJAR byte一致は要求しない。

## 4. 実行集合

Repository rootからの単体確認例：

```sh
node --test build-support/container-sso-verification/test-cookie-replay.mjs
node --test build-support/container-sso-verification/test-stop-observer.mjs
python3 -m unittest discover -s build-support/container-sso-verification -p test_s1.py -v
```

frontendディレクトリでは次を行う。test39件／skip0を確認してからNext buildへ進む。

```sh
node --experimental-strip-types --test test/*.test.mjs
NODE_OPTIONS=--max-old-space-size=2048 NEXT_TELEMETRY_DISABLED=1 node node_modules/next/dist/bin/next build
```

新規project例は`d3-sso-s5-20261110-cloud01`、workは`.tmp/d3-sso-s5-20261110-cloud01`。実行日や既存資源に応じて未使用名にする。`prepare.py --stage S5 --jar <今回生成JAR> --work-dir <新規work> --project <新規project>`で準備する。

生成workの`compose.env`とrecipeの`compose.yaml`→`compose-s2.yaml`→`compose-s3.yaml`→`compose-s4.yaml`→`compose-s5.yaml`を順に指定し、backend／frontend imageをbuildする。旧projectのimageを未照合でretagしない。手順・生成contextはREADMEのS2〜S5節に従う。通常集合では`--stop-diagnostics`を指定しない。

```sh
python3 build-support/container-sso-verification/verify-s5.py --work-dir <新規work>
python3 build-support/container-sso-verification/export-evidence.py \
  --work-dir <新規work> --output-dir .tmp/d3-sso-exports
```

実行範囲は境界5件・observer1件・Node39件・guard5件、および新規projectで通常S5-V01〜06を1回とする。S1〜S4全負例・通常root全件・停止診断projectの再実行は今回の集合に含めない。build30分・動的60分（cleanup10分予約）とS5の資源上限（runtime4 CPU／5 GiB、probe0.5 CPUでfrontendと排他、PG接続32、Browser worker1／同時context最大3）を維持する。不成立・FAIL時は秘密値を出さずcleanup・exportと原因を記録し、原因未確認の自動再試行や集合拡張をしない。

## 5. 判定と証拠の返却

S5-V01〜06の各判定、実際のtest件数／skip、停止時間／exit／OOM、Browser終了・復元、Cookie選択件数と非復活、必須Session3件、同volume／同鍵snapshot保持、pending拒否、最終業務／Audit・SSO、cleanup・secret削除を記録する。交換結果不明の単体stub、pendingのSQL fixtureと実IdP結果を分ける。

結果は新しいCloud再検証結果文書へ記録し、本書・AGENTSから参照する。旧Cloud／Windows結果とFAIL履歴を上書きしない。開始commit SHA・dirty状態・実行source hash・生成物hashを対応付ける。未実施は未実施として残す。

allowlistのredacted archiveだけをexportし、名前・bytes・SHA-256・entry一覧・checksum照合結果・取得経路を報告する。secret／realm原本・Cookie値・token・code付きURL・storageState・trace／HARをGitや返却ZIPへ追加しない。実行workのsource snapshotは変更せず、結果文書の実行後追記と区別する。

Cloud内でのexportと、Windowsでの実取得／SHA-256照合は別の状態にする。従来Cloud ZIP受領OPENを今回の新規exportだけで解消しない。

## 6. 次のCloudタスクへの依頼文

> 作業branch `docs/d3-option2-implementation-conditions-20261109`のpush完了報告にあるcommit SHAを確認し、本引継ぎ§2〜5に従ってWindows修正後harnessのCloud限定回帰確認を進めてください。新規Cloud資材・project／DB／secretで、単体集合と通常S5-V01〜06を1回実行し、cleanup・redacted証拠exportまで記録してください。旧Cloud／Windows証拠・既存projectを変更せず、FAIL時は原因と必要差分を報告してください。手動環境起動・PR／merge・tag／Release・追加再実行は先行しないでください。

今回のWindows上のcommit・pushにはCloudタスクの起動／実行を含めない。Cloudの結果commit／pushはその時点のOwner指示に従う。Windows手動確認・結果Owner受入・D3／Phase4全体の認定は別判断として残す。

## 7. Cloud再検証の完了記録（2026-11-10）

Ownerの「再検証へ進めましょう」により、開始SHA `5ac06e361cb69878dbeb7c765173a7c82849bd91`・cleanからCloud資材を新規生成し、新規`d3-sso-s5-20261110-cloud01`で指示された通常S5集合1回を実施した。境界5／observer1／Node39／guard5／Java23件とS5-V01〜06が全件PASS、Browser終了・新3 contextへのCookie完全一致／必須Session3件・期限切れ補助Cookieの非復活・HTTP前のsnapshot不変が成立した。frontend0.201秒／143、backend0.446秒／143、Keycloak1.199秒／143、強制kill／OOMなし。資源残留0・10 secret／realm削除・export／checksum・source67 file照合まで成立。[Cloud再検証結果](../architecture/validation/phase4-d3-option2-cloud-harness-reverification-results-20261110.md)を参照する。

開始manifestの旧固定build回数等は今回の実測集合へ実行前に補正し、元manifest hashと理由を保持した。harness本体・Framework／Reference／依存／lockは変更していない。旧Cloud最終PASS ZIPの受領・SHA一致は前ターンOwner報告に基づき確認済み、新Cloud ZIPの受領は未確認として別管理する。上記各節のCloud未実施・従来ZIP受領OPEN表示は当該報告前の履歴として読む。結果commit／remote・手動環境・Owner結果受入・D3全体の認定は先行していない。

## 8. Cloud再検証結果の成果固定指示（2026-11-10）

Ownerは「結果文書など3件をコミットして、pushまで進めましょう」と指示した。Cloud再検証結果・本引継ぎ・AGENTSの文書3件をcommitし、`docs/d3-option2-implementation-conditions-20261109`へpushする。実SHA・local／remote一致は操作後の報告とGit履歴で確認する。上記の結果commit／remote未実施表示は後続指示前の履歴として保持する。新Cloud ZIPのWindows受領・手動環境・Owner結果全体受入、PR／merge・tag／Releaseは今回の成果固定に含めない。

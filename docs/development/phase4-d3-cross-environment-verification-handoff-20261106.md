# D3方式1：別端末・Codex Cloud再検証への引継ぎ（2026-11-06）

## 1. 目的・状態と作業入口

Container改善を別PC・Codex Cloudで再検証する際の入力を保存する。環境固有の準備不足とアプリ不具合を分け、同じsourceから新しい専用環境・Evidenceを作るための引継ぎである。

本書作成時点では別環境の再検証は未実施。改善sourceは計画commit `3b095a8`に対する未commit差分であり、改善成果commitの存在を前提にしない。成果固定・受渡し後、次回作業者が**改善を含む実際のcommit SHA**を確認して使用する。branch名だけで対象を識別せず、未反映なら旧版で再検証を始めない。

| 入口 | 用途 |
| --- | --- |
| [改善計画](phase4-d3-container-improvement-plan-20261106.md) | 要件、Ownership、変更・検証境界、対応外事項 |
| [改善結果](../architecture/validation/phase4-d3-container-improvement-results-20261106.md) | このPCでのlayer測定・直接回帰25件・自動7 checkpoint、手動報告・DB／ログ点検 |
| [実行資材・README](../../build-support/container-verification/integrated/README.md) | build／prepare／Compose／verify／手動環境の操作 |
| [環境構築手順](application-team-development-environment-guide.md) | JDK／Proxy CA等の準備・診断 |

対象はReference／Toolingの方式1（SPA同梱Java app＋PostgreSQL）。今回の設定値は実案件の最適値や本番標準ではない。再検証成功を正式support・全DoD・D3／Phase 4全体の認定へ拡張しない。

## 2. 移さないもの・その環境で用意するもの

- このPCの`.tmp`、Maven／npm cache、Windows用classpath、browser binary、絶対pathを再開条件にしない。
- 既存secret・Session・DB volumeを複製せず、新しいwork／project名・空きport・secretを用意する。認証情報や社内CAをCloudへ一式コピーしない。
- 原則として固定sourceからJARを再生成する。Node／npmとprofileは同じにし、JDK・OS・依存取得条件を記録する。
- **その環境で生成した元JAR hashと、そのJARでbuildしたimage labelを照合する。** このPCのJAR hash・image IDとの一致は要求しない。再現可能buildはA4として対応外であり、hash差だけで失敗としない。
- このPCの手動project `d3-improved-manual-20261106`は稼働保持中。旧`d3-manual-local-20261106`はcontainer整理済みでDB volume等を保持している。どちらも別環境検証から操作・cleanupしない。将来の現状はAGENTSとOwner指示を優先する。

## 3. 最初に確認する環境条件

| 項目 | 必要な確認・差分の扱い |
| --- | --- |
| source | 改善成果commit、checkoutのclean／採用差分を記録。実行したDockerfile／Compose／script／専用profileを対応づける |
| platform | 現在の実証はLinux／amd64 container。ARM hostでは固定digestの対応platform・emulationを確認し、自動で別imageへ差し替えない。native ARM成立とは数えない |
| Docker／Compose | daemonへの接続、Compose利用、image取得、volume／network／port作成が可能か。`start_interval`はEngine25以降・Compose2.20.2以降が必要。このPCの確認版は29.5.3／5.3.1 |
| JDK／Maven | JDK21。java／javac／Maven使用JDKが同じ配布断面か確認。Mavenはrepo Wrapperを使用し、端末既定版に依存しない |
| Node／npm | frontendの`package.json`が正本。現行指定はNode24.19.0／npm11.9.0。Node同梱npmが一致するとは限らず、専用toolchainで確認する |
| Python | Python3の実行実体を確認。このPCのpyenv設定・Windows pathを転記しない |
| browser | 既存E2E ToolingのPlaywright版に対応するChromiumをそのOSへ取得。Linuxでは必要な共有library／browser実行権限も確認。初回Playwright起動に追加downloadが生じ得る |
| network／CA | Maven、npm、image registry、browser配布先へのアクセスを確認。社内Proxy使用時はOS trustと使用JDKのtrustを分けて確認。証明書検証を無効化しない |
| 資源 | app1536 MiB／CPU1／PID160、DB1024 MiB／CPU1／最大16接続の現行設定に加え、host build JVM・browser・Docker overheadを確保。buildと検証runtimeは並走させない |
| Linux観測 | `verify.py`はcontainer内の`/proc/1/status`とcgroup v2の`/sys/fs/cgroup/pids.current`を読む。利用不能なら環境差として記録し、assertを黙って削除して同一PASSにしない |
| file／mount | shell scriptのLF、実行・読取権限、host→container bind mountとsecretの読取りを確認。非root／read-only条件を回避するために権限設定を緩めない |

Codex Cloudでは、その環境で提供される起動時ガイダンス、network／Proxy／CA・Docker利用条件を先に確認する。Dockerやbrowserが利用可能であるとは仮定しない。権限エラーとEngine停止を区別し、実行環境の承認手順を守る。

Windows用`import-corporate-root-ca.ps1`はWindows証明書ストアを使う。Linux／Cloudへそのまま実行せず、環境で許されたCA供給方式を使う。JDKを再取得・置換した場合はCA設定も再確認する。

## 4. 次回の実施順序

1. 対象commitと環境条件を確認し、実施する集合・回数・work／project名・port・資源・cleanupを開始時に記録する。本書の保存だけでは別環境の実行を開始せず、次回の指示に合わせて範囲を具体化する。旧検証の予算・承認を自動流用しない。
2. 指定Node／npm・JDK21で`b1-session-spa,d3-container`のJARを生成する。通常packageだけでSPAが入ると考えない。既存直接回帰は3 class／25件を対象とし、root全件へ広げない。
3. 新しいworkで`prepare.py --require-reference-spa`を実行する。SPA asset hash・async JDBC非同梱、元JAR識別、新規secret作成を確認する。
4. Compose `config --quiet`、image buildを行う。旧版workのenvには`D3_JAR_SHA256`がないため、使い回さない。
5. 同じ環境で既存E2E Toolingからclasspathを生成し、browserを準備して`verify.py`を1回実行する。Windowsの`;`区切りclasspathやbrowser cacheをLinuxへコピーしない。
6. 既存7 checkpoint（DC01／02／03／04／06／07／08）、設定観測、ログ・cleanupを確認する。DC01はimage labelによる元JAR識別であり、元fat JARのbyte一致ではない。DC05のsecret欠落負例は自動で追加しない。
7. 手動打鍵が指示された場合だけ、別の新規work／projectで`start-manual.py`を実行する。自動検証projectはDBまでcleanupされるため、手動確認に転用しない。終了時の停止・データ保持／削除方針を明示する。
8. 環境別の結果文書を新規作成し、今回のWindows結果を上書きしない。失敗時は同workの証拠を保持し、原因・必要差分を整理してから再実行範囲を判断する。

WindowsのJAR生成は既存`build-reference.ps1 -NodeDirectory ... -RunDirectTests`を使える。Linux／Cloudでは同helperのWindows実行ファイル名を転用せず、指定Node／npmをその実行shellのPATHへ設定してから、Repository rootで以下と同等のMavenを使う。

```sh
./mvnw -B -ntp -pl koiki-reference-app -am \
  -P b1-session-spa,d3-container clean package \
  '-Dtest=ReferenceContainerSecurityConfigurationTest,ReferenceSessionApiSecurityTest,ReferenceSecurityConfigurationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false '-DargLine=-Xmx768m'
```

Wrapperの実行権限やshell、node／npmの検出方法はcheckout環境で確認する。既存frontendのversion guardや依存lockを緩めず、不足依存の取得を全suiteの追加実行と混同しない。

## 5. 比較するもの・しないもの

| 観点 | 判定方法 |
| --- | --- |
| 機能 | 7 checkpointとDB／Audit・browser・cleanupの成立を環境別に確認。skip・観測不能を成功扱いしない |
| layer | 元JARでの同一再buildのcache利用、application変更時の依存layer再利用を確認する場合は測定範囲を先に定める。このPCの5 invocation比較を必須の全環境再試験にしない |
| 分類 | Boot標準分類を記録する。このPCのReactor JARでは自作moduleはapplication区分、loader／snapshot区分は空。全てのSNAPSHOTが専用区分に入る前提を置かない |
| image識別 | 元JAR hash labelと起動imageの対応を確認。同じfilesystem layerでもattestation等でmanifest IDが変わり得る |
| 時間・容量 | build時間・Startedログ・Compose待機時間を分ける。PC性能・cache・初回DB準備の違いを含むため厳密な速度比較にしない。history容量を圧縮転送量の実測としない |
| Session既知事象 | 重複保存例外の再発有無と時刻を記録。未再発を解消とせず、今回の検証中にSQL／request cacheを修正しない |

CloudでDockerが使えない場合、JAR生成／静的確認の成立とContainer検証の未実施を分ける。browserだけが使えなければ、browser journeyは未実施であり7 checkpoint全体PASSとしない。制約回避のために別の基盤を新設する場合は、今回の再検証範囲へ自動で追加しない。

手動browserで別PCの`127.0.0.1`を開く場合は、そのPCでのlocal実行を確認する。CloudのloopbackへユーザーPCから直接は接続できない。Cloudでの打鍵は、認証・Cookie／origin・アクセス範囲を含む転送／preview方式を別途定める。公開portやSecure Cookie設定を黙って変更せず、公開・転送が不要ならCloudでは自動browser検証までとする。

## 6. 結果記録と後始末

次回結果には以下を残す。secret値・認証Cookie・private keyは記録しない。

| 記録項目 | 内容 |
| --- | --- |
| 対象source | commit SHA、採用差分、主要資材hash、実行したcommand／profile |
| 環境 | host OS／CPU architecture、container platform、Docker／Compose／JDK／Maven／Node／npm／Python／Playwright／browser版、cgroup観測可否 |
| artifact | 元JAR hash、image ID／label、build context識別。外部提供JARを使う例外では生成元を明示 |
| 検証 | 件数・checkpoint・PASS／FAIL／未実施、初回失敗と訂正、時間の測定範囲、WARN／ERROR・既知Session事象 |
| 手動確認 | Ownerの画面操作報告とDB／Audit点検を分ける。DB単独ではSPA／MVCの操作経路を識別できない |
| cleanup | 今回のproject labelのcontainer／volume／network残留、保持した資源・理由、停止状態 |
| 限界 | 環境制約・未実証条件、結果受入の有無。A4／B5／B6、Session対応等の対応外を維持 |

自動runnerのcleanupは今回専用projectだけを対象とする。既存手動project・他案件・Docker全体へのpruneや一括削除は行わない。ignored Evidenceは必要な調査が終わるまで識別して保持し、secret削除は対象workと保持要否を確認して行う。

次回の開始時に未確定なのは、改善成果commit、対象PC／Cloudの実際の機能・資源、手動打鍵の実施有無、具体的な実行集合である。本書作成では追加検証・環境操作・commit／remoteを実行していない。

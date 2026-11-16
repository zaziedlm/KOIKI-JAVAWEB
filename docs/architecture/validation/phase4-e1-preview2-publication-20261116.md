# E1 preview.2：固定断面からの最終ZIP生成・公開・実取得結果（2026-11-16）

状態：**GitHub prerelease公開済み／最終ZIP生成・登録・公開後実取得・独立ConsumerまでPASS**。結果全体のOwner受入、実案件側の採用・正式support・Phase4全体の認定は別判断。

## 1. 指示・生成元・タグの区別

Ownerの「最新のフレームワークJAR ZIP生成、それにpreview.2の配布版タグ付け／Releaseへ進めましょう」に基づき、認証・書込権限を確認したこのWindowsローカルPCで実行した。

| 項目 | 固定内容 |
| --- | --- |
| Source commit | `557fbfc59ddbf1275b1ff53c95b68d9e21739dfa`（PR #63 merge） |
| 資産断面のcheckpoint tag | `checkpoint/v0.1.9-phase4-development-baseline` |
| 配布版tag | `framework-0.1.0-preview.2`（今回新規作成した注釈付きtag） |
| 配布版tag object | `1aab0e4aae1970222662a37d3752aaed33b7e4a2` |
| local／remote peeled target | いずれも上記557fbfc、完全一致 |
| 開始時のcheckout | main／origin/main／checkpointのcommit一致、working tree clean |
| 最終manifest | `sourceDirty=false`、schema2 |

資産断面tagと配布版tagは役割を分け、同じ生成元commitを参照する。既存の2026-11-12の`sourceDirty=true`技術候補は公開に使用していない。checkpointや公開済みpreview.1のtag／Release／assetを変更していない。

生成元commitのmerge後CIは、checkpoint固定時に通常CI7／7、Java Runtime2／2、Virtual Threads1／1の成功を確認済み。今回のZIP生成に合わせて同じCIを再実行したとは扱わない。

## 2. 公開asset

- [GitHub prerelease](https://github.com/zaziedlm/KOIKI-JAVAWEB/releases/tag/framework-0.1.0-preview.2)
- [koiki-framework-0.1.0-preview.2.zip](https://github.com/zaziedlm/KOIKI-JAVAWEB/releases/download/framework-0.1.0-preview.2/koiki-framework-0.1.0-preview.2.zip)
- Release ID：`446717760`、asset ID：`753887340`。
- `draft=false`／`prerelease=true`、ZIP size：`816761 bytes`。
- ZIP SHA-256：`7858dc3719ed9f8f0fab164e6dfa2e6015893dd971b0d7bd6a48a37136aab08e`。
- 同梱installer SHA-256：`8ed85c91d1ab55027c374f70fdd67aa307de0c26c4f78b6c79d61ba2cb4bed4a`。

本体12 JAR・14 POM・sources11 JAR・Javadoc5 JARの14座標／42 payload。manifest files44、bundle実file46。Reference／SPA／検証fixture・第三者JARは配布対象に含めない。sourceはinternal実装も含むが、Javadocは公開packageのある5座標だけで、internalに互換性を約束しない。

`koiki/fragments :: header`削除への移行とSession logoutの評価順是正、schema2の版固有installer使用はRelease notesへ記載した。[実装・前段証拠](phase4-e1-sources-javadoc-results-20261112.md)は過去の技術候補と今回の最終artifactを分けて参照する。

## 3. 生成・公開前検証

環境：Temurin21.0.13+9、Maven Wrapper3.9.16、Python3.13.3、PowerShell7、ローカルDocker。Source Plugin3.4.0／Javadoc Plugin3.12.0を固定断面のTooling設定どおり使用した。Maven process heap768 MiB、候補test fork heap512 MiB。生成は既存producer cacheでoffline実行し、新しいrecipientの初回登録・Consumer依存取得は通常Maven repositoryを利用した。

| 段階（JST） | 結果 |
| --- | --- |
| 14:54:34〜14:55:34 最終候補生成 | `create-candidate.py --version 0.1.0-preview.2`を新規領域で1回。Framework111件PASS、failure／error／skip0 |
| classifier内容・本体影響 | sources11の全Java内容一致、Javadoc5の公開型HTML／internal除外・local絶対path等の検査成功。本体12 JARの追加goal前後SHA完全一致 |
| source照合 | manifestのsource commit／clean状態を確認。生成時inventory1511 fileを公開前checkoutとhash照合して一致 |
| 14:57:05〜14:57:28 空local recipient登録 | 同梱installerで14座標／42 payload全hash一致、schema2 marker files42、lockなし |
| 14:57:57〜14:58:13 準備script | 既存の有限16確認が全PASS。改ざん・欠落・未知classifier・schema1・path traversal・download失敗・同一再利用・登録済み衝突・欠落復旧。offlineで実行 |
| 14:59:44〜15:00:29 公開前Consumer | 新しい独立Consumer1 test／8 checkpoint PASS、Boot JARへのclassifier混入0、専用container／匿名volume cleanup PASS |

一時runnerによる最初の登録呼出しは、PowerShell scriptへの配列引数の渡し方に誤りがあり、installer起動前・recipient作成前に停止した。一時runnerだけを名前付きhashtable splatへ訂正してから、上記の登録1回を実施した。生成済みZIP・同梱script・固定済みsourceは修正していない。`local-prepare-invocation-01.json`にこの履歴を保持した。候補の再生成・ZIP／manifestの手修正は行っていない。

## 4. タグ・Release作成と公開後確認

公開前検証後、生成元557fbfcへ配布版の注釈付きtagを作成・pushした。注釈にcheckpointとの区別、ZIP SHA、clean source、同梱内容と検証範囲を記載した。その既存tagを`--verify-tag`で照合し、`gh release create --prerelease`へ検証済みの同一ZIPと確定notesを渡した。公開処理は1回で成功し、tag／assetの上書き・削除は行っていない。

| 公開後確認 | 結果 |
| --- | --- |
| GitHub asset digest | `sha256:7858dc37…36aab08e`、ローカル最終ZIPの全64桁SHAと一致 |
| 16:23:42〜16:24:06 実取得・空remote recipient登録 | 同梱scriptの`-Repository zaziedlm/KOIKI-JAVAWEB`で公開ZIPを実取得し、outer SHA確認後に14座標／42 payload登録。全hash・marker一致、lockなし |
| 16:48:09〜16:48:55 公開後Consumer | 公開ZIPから登録したrepositoryを明示。独立Consumer1 test／8 checkpoint PASS、Boot JARへのclassifier混入0、専用container／匿名volume cleanup PASS |
| 16:51:16 最終読取 | Releaseはprerelease・非draft、asset digest／size一致、local／remote tag objectとpeeled target一致。両recipientの42 hashと両ConsumerのXML・8 checkpointを再照合 |
| 既存資源保護 | E1専用container残留0。開始時から存在したcontainerのID／名前／image／状態の変更0。ヘッダー用DBは開始時から停止状態で、起動・停止・再seedしていない |

Consumerの8 checkpointは、Customer／Identity／Audit Entity共存、Public service bean、Customer永続化、同schemaの別migration history、非superuser接続、実Web default deny、同DB再起動でmigration no-op／data保持、明示EntityScanが必要な境界である。公開前後の2 invocationを独立に記録し、内部8 checkpointを8 JUnit testと数えない。

公開後の成功結果はGitHub Release notesへ追記した。Release notesの更新で公開ZIPのbytesやhashは変更していない。

## 5. Evidenceと後続

端末内で保持する資材：

- `.tmp/e1-preview2-release-20261116/`：段階別runnerとlog／summary、Framework111件集計、公開用notes・tag注釈、release metadata、remote tag参照、最終`publication-result.json`。
- `%TEMP%/koiki-e1-preview2-20261116-final01/`：正規ZIP・SHA256SUMS・bundle、source snapshot、source-inventory、generated-poms、main-jar-comparison、生成log／Surefire XML。
- `%TEMP%/koiki-e1-preview2-20261116-recipient-local01`／`recipient-remote01`：各42 payloadと完了marker。
- `%TEMP%/koiki-e1-preview2-20261116-consumer-local01`／`consumer-remote01`：各Consumer、XML・log・dependency tree・DB client sample・cleanup result。

公開用バイナリ・Tempのraw EvidenceはGitへ含めず、後続確認まで保全する。本書・README・AGENTS・Release notes sourceの公開後追記は、生成・公開・最終照合の後に行った文書差分である。公開完了時点では未コミットだったが、2026-11-16にOwnerから、この4文書だけを独立branchでcommit・pushし、main向けPRと必要CIを経てmergeする指示を受けた。配布tagと資産checkpointは557fbfcのまま保持し、この文書差分へ付け替えない。ZIPの再生成・asset更新も行わない。

今回成立したのは、固定版preview.2のZIP生成・公開・実取得・Maven登録・独立Consumerによる限定配布技術確認。前段のOwner IDE確認は過去の技術候補の`IdentityQuery`での確認として保持し、新しい最終ZIPでのIDE再確認や別PC／実案件CIまで確認済みとはしない。必要に応じて案件側でpreview.2取込み・IDE／CI／containerを検証する。

O-1、正式support、全V10、S1・D3・DoD・Phase4全体の認定、結果全体のOwner受入は別判断。公開記録4文書のcommit／PR／merge結果はGit履歴へ追跡する。

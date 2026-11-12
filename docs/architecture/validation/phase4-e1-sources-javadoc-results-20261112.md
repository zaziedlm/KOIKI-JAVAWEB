# E1 preview.2：sources／Javadoc JAR同梱のローカル結果（2026-11-12）

状態：**候補生成・空recipient登録・16確認・独立Consumer PASS／OwnerのIDE表示確認済み／未公開**。

## 1. 判断・所有・source

Ownerは別branchの引継ぎ案を現行branchへまとめる進め方とA1＋B1を了承した。[判断記録](../../development/phase4-e1-sources-javadoc-handoff-20261112.md#現行判断利用側是正branchへ統合2026-11-12)を正本とする。

- branch：`claude/eager-hawking-2pyla8`、開始commit：`d0b39bd811cf3262da6e114abe8f2891fd6c673f`（開始時clean、originより文書commit1件先行）。
- `feat/e1-sources-jar-20261112`の`e718cc8`は引継ぎ文書とAGENTSだけだった。本書の前段で文書を取り込み、現行branch／是正済み前提に整えた。merge／cherry-pick／commitは行っていない。
- 今回の追加変更は配布Tooling・そのConsumer／verification・文書。Framework機能・Reference source・Parent・依存baseline・migration・CIを追加変更していない。
- Java21 `Temurin 21.0.13+9-LTS`、Maven Wrapper3.9.16、Python3.13.3、Windows PowerShell7で実行。Maven process heap768 MiB、候補test fork heap512 MiB。
- Source Plugin3.4.0／Javadoc Plugin3.12.0は[Maven公式一覧](https://maven.apache.org/plugins/)で確認し、`classifier-inventory.json`に固定した。doclintは`all,-missing`、外部doc linkの自動検出を無効化。Parentにpluginを継承させない。
- GitHub読取で`zaziedlm`の認証、public `zaziedlm/KOIKI-JAVAWEB`のADMIN権限を確認。preview.2 ReleaseはHTTP404、remote tag一覧の同名tagは0件。tag／Release／assetの書込操作はしていない。

## 2. 実装

| 対象 | 内容 |
| --- | --- |
| `create-candidate.py` | stagingの通常`clean install`後、sources11／Javadoc5のmoduleを選択してCLI goalを実行。producer cacheに本体を登録して固定版のmodule間依存を解決する。main12 JARの追加goal前後SHA一致、sourcesの全Java内容一致、public型HTML存在、internal doc除外、local絶対path・秘密鍵marker非混入を検査 |
| `classifier-inventory.json` | classifier対象と生成plugin・doclintを固定。A1はsources11、Javadoc5 |
| manifest | schema2。classifierは本体／POMがnull、sources／javadocはその文字列。payload42、manifest files44、bundle全46 file |
| `prepare-koiki.ps1` | 独立した固定一覧、artifactとglobal manifestのpath／classifier／size／hash整合、全file列挙を検査。14座標を各1 invocationで登録し、sources／javadocも完了判定・衝突検出・事後hash照合・marker42件へ含める。schema1は版固有scriptへの案内付きで拒否 |
| `verify-preparation.py` | classifierの改ざん・欠落・未知classifier・schema違い・登録済み衝突・欠落復旧を追加。再登録を含むMavenを明示offlineにして16確認を実行 |
| Consumer | Parent版をpreview.2へ更新。既存1 test／8 checkpointを継続し、Boot JARの`BOOT-INF/lib`にclassifier JARがないことを追加確認 |
| Consumer runner cleanup | 今回生成したcontainerの匿名volume名を取得し、当該containerだけを`rm -f -v`、containerとvolumeの消去を照合。DB memory1 GiB／CPU1／max_connections16。既存の手動環境は対象外 |

sourcesはinternalも含む実装参照用だが、Javadocはpublic packageを持つ5座標だけ。sourceのない`koiki-testing`へplaceholderを作らず、Javadocの記述内容も今回増やしていない。

## 3. 新規候補と登録結果

ローカル候補の生成は**1回で成功**した。ZIP／manifestの手修正はしていない。

| 項目 | 結果 |
| --- | --- |
| 候補生成 | 19:51:24〜19:52:27 JST、exit0 |
| staging Framework test | 111件PASS、failure／error／skip0 |
| 生成classifier | sources11、Javadoc5、全内容検査成功 |
| 本体JARへの影響 | 12 JARすべて生成goal前後SHA一致 |
| 候補座標／file | 14座標、POM14＋main JAR12＋sources11＋Javadoc5＝payload42、manifest files44、bundle実file46 |
| ZIP SHA-256 | `892a0d001fd3bfeafa4f86053e93854f90a3d8c325baed4fc9b2179ee82eaca6` |
| ZIP | `%TEMP%/koiki-e1-preview2-20261112-candidate01/koiki-framework-0.1.0-preview.2.zip` |
| 新規recipient | `%TEMP%/koiki-e1-preview2-20261112-recipient01`（未存在から開始） |
| 登録終了 | 19:57:25 JST、14座標／42 payload全hash一致、marker schema2・files42、lock残留なし |

**候補は`sourceDirty=true`である。** 生成時のcommitは上記d0b39bd、未コミット実装を含むsource全file hashを`source-inventory.json`で固定した。manifestの同inventory SHAは`dcfc8d7e0a00f05ab987d5cbd17b60feb6694d905653eb40fa50e5a20f0f9efa`。生成後のverification runnerのoffline強制・volume cleanup追加と結果文書は、この候補のsource snapshotより後の変更である。配布installerそのものは候補同梱版と実行時sourceのSHA一致を確認して検証した。

この候補はローカル技術成立の証拠であり、公開対象のclean commitに基づく最終候補と同一視しない。成果固定後に新しい候補を生成し、必要な再照合を行う。既存ZIPやmanifestを書き換えてsourceDirtyを消さない。preview.1の公開済みassetは操作していない。

## 4. 準備scriptの16確認

19:58:48 JSTにexit0、次の16件が全PASS。拒否ケースのexit1は期待どおりの拒否であり、harnessの失敗ではない。偽ghによるdownload失敗を含め、network呼出しなしで実行した。

1. ZIP全体hash不一致の拒否
2. 本体JAR改ざんの拒否
3. sources改ざんの拒否
4. sources欠落の拒否
5. Javadoc改ざんの拒否
6. Javadoc欠落の拒否
7. 想定外classifierの拒否（件数を保ちmanifestも置換したZIP）
8. schema1の拒否
9. archive path traversalの拒否
10. download失敗時の登録前停止
11. 同一ZIPの再利用
12. 登録済みPOM衝突の拒否
13. 登録済みsources衝突の拒否
14. 登録済みsources欠落の復旧
15. 登録済みJavadoc衝突の拒否
16. 登録済みJavadoc欠落の復旧

改ざん・欠落を試すZIPは派生コピーだけで、正規候補は不変。登録済みpayloadの意図的変更は隔離recipientだけで行い、各`finally`で復元する。成功markerが衝突拒否時に変わらないこと、欠落の復旧後も同じ内容であることを確認した。

## 5. 独立Consumer

`%TEMP%/koiki-e1-preview2-20261112-consumer01`へConsumerをコピーし、上記recipientを明示して通常の`clean verify`とdependency treeを実行した。22:12:51 JSTにrunner exit0、ConsumerのMavenはBUILD SUCCESS。

- JUnit1件／内部8 checkpoint PASS：Customer／Identity／Audit Entity共存、Public service bean、Customer永続化、同schemaでFramework／Customer別history、非superuser、実HTTP default deny、同DB再起動でmigration no-op／data保持、明示EntityScanが必要な境界。
- Boot JAR内のsources／Javadoc JARは**0件**。
- 専用container `koiki-e1-e363593fd5ee`とその匿名volumeのcleanupはPASS、残留volume0。
- 実案件repository・実案件DBの変更、recipient CIでの確認、IDE実表示はこの自動検証では行っていない。

今回のTooling差分後に通常root全件は反復していない。Framework／Reference／Parentは前段の検証後に追加変更しておらず、[Windows自動検証のroot結果](phase4-consumer-feedback-windows-verification-20261112.md#5-owner承認後のclean再実行後続検証)はそのsource epochの証拠として保持する。今回の候補に対する成立は本書の111件・16確認・独立Consumerで記録する。

## 6. IDE確認と証拠の保持

VS Code向け独立workspaceを次へ準備した。

```text
%TEMP%/koiki-e1-preview2-20261112-consumer01/ide/koiki-preview2-consumer.code-workspace
```

workspace専用`settings.xml`で今回のrecipientを指定し、普段のglobal設定・system PATHは変更していない。別ウィンドウで開いてMaven import後、`DistributionConsumerTest.java`の`IdentityQuery`をF12で開き、decompile表示ではなくJava sourceが開くこと、型hoverでJavadocが表示されることを確認する手順を案内した。データベースはcleanup済みなので、IDEでtestを再実行する必要はない。

2026-11-12にOwnerから「`IdentityQuery` をF12で開くとJava sourceが表示され、型へのhoverでJavadocが見えました」との報告を受けた。今回の独立Consumerによるsource／Javadoc表示のOwner確認として記録する。確認対象は`IdentityQuery`の型であり、全型・別IDE・別PCの確認へは広げない。

22:24:26 JSTの最終読取でrecipientの42 payload全hash一致、正規ZIPのSHA不変、installer lockなし、E1検証container残留0を確認した。保持中のヘッダー手動環境はJava PID26008生存、専用DB running／healthy・restart0だった。記録は`.tmp/e1-sources-20261112/final-inspection.json`に保持する。

保持する端末内Evidence：

- `.tmp/e1-sources-20261112/`：各runner、candidate／prepare／16確認／Consumerのlog・summary。
- `%TEMP%/koiki-e1-preview2-20261112-candidate01/`：ZIP、bundle、source snapshot、source-inventory／generated-poms、main-jar-comparisonと各build log・111件のSurefire XML。
- `%TEMP%/koiki-e1-preview2-20261112-recipient01/`：隔離recipient、42 payloadとmarker。
- `%TEMP%/koiki-e1-preview2-20261112-consumer01/`：ConsumerとSurefire XML、dependency tree、DB client sample、cleanup result、IDE workspace。

Temp領域の候補・raw EvidenceはGitで転送されないため、成果固定や後続検証までcleanupしない。[Release notes案](../../development/phase4-e1-preview2-release-notes-20261112.md)は未公開の草案である。

残件：結果全体のOwner受入、成果commit／remote、clean sourceからの最終候補・CI・公開前照合、Owner明示指示後の公開とremote再取得確認。今回の限定成立を正式support・全V10／Phase4完了へ拡張しない。ヘッダー手動確認環境は稼働保持する。

## 7. Ownerのソース断面固定指示とCI集計の追随

Ownerはフレームワーク資産の断面固定とZIP配布版のtag／Releaseを分ける方針を確認した。資産の断面はmainへのmerge・CI確認後に別のcheckpoint tagで固定し、その断面から最終ZIPを生成・検証した後、配布版の`framework-0.1.0-preview.2`tagとGitHub prereleaseへ進む。両tagが同じ生成元commitを指すことは可能だが、役割は分ける。checkpoint tag名は未確定。

現行の開始指示は、現在の変更のcommit・push、main向けPR作成とCI確認まで。merge・checkpoint tag・ZIP生成・配布Releaseは後続段階とし、今回先行実施しない。以前の「ソース固定にframework-0.1.0-preview.2を使う」という説明は、この役割分離に訂正する。

成果固定前のローカル点検で、既存`verify-p2-b3-session-core.ps1`の期待件数がSession context5件／累計72件のまま残っていることを発見した。今回追加済み2件に合わせて7件／74件と成功messageを更新した。testのskipやassertion緩和ではなく、実テストの増分への追随である。前段でSession7件は実行済みPASSだが、この累積harness全体の再実行結果はPRのCIで確認する。候補snapshotより後の検証Tooling差分として扱う。

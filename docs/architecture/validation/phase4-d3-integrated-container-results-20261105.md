# D3：方式1 SPA同梱Java＋PostgreSQL Containerの限定結果（2026-11-05）

## 1. 対象・判断

Ownerは設計文書を`ded2698`へcommitし、方式1の構築へ進むよう指示した。branchは`docs/d3-01-container-contract-20261105`、開始時はclean。今回の成果は未commitであり、`ded2698`だけに実装が含まれるとは扱わない。

**限定技術結果：image build・2 Container起動・実SPA journey・DB／Audit・再起動／障害回復・cleanupが成立。結果Owner受入は未記録。** D3全体／全DoD／正式提供・本番運用の完了にはしない。

実装は[資材README](../../../build-support/container-verification/integrated/README.md)、設計は[D3-01](../../development/phase4-d3-01-container-contract-20261105.md)。方式2のNext.js／Keycloakは今回作成・起動していない。

## 2. 実装と所有

| 対象 | 差分 |
| --- | --- |
| Tooling | `build-support/container-verification/integrated/`にruntime Dockerfile・Compose・probe、専用DB role初期化、JAR準備／hash照合・secret生成、Reference build補助、fixture SQL、Java Playwright runner、有限検証・cleanup runnerと手順を追加 |
| Reference | Maven `d3-container` profileだけに既存Observability Starter依存を追加。runtime同名profileでready／live・pool／shutdownを設定。Securityは同profileだけで有効にし、匿名GETの2 probeだけを許可 |
| 維持 | 通常Security／Session API／CSRF、Framework Public API／Starter本体／migration、Root Reactor／CI、E1の公開tag／ZIPを変更しない |

Javaのruntime recipeは明示`app.jar`をCOPYする方式で、Framework repoのsource／親POMをDocker build contextへ入れない。Reference固有のbuild・profile・seed／検証は分離した。案件では案件自身のJAR生成と設定／healthへ置換する。別案件Consumerによる独立build／起動の実証は今回未実施。

## 3. 環境・artifact

| 項目 | 実値・識別 |
| --- | --- |
| host／Docker | Windows PowerShell、Rancher Desktop経由、Docker Server `29.5.3`、Compose `v5.1.4`、Linux／x86_64、6 CPU・約16 GiB |
| build JDK／runtime | managed JDK21でbuild。Container内はTemurin `21.0.13+9`を実取得・確認 |
| frontend | Node `24.19.0`／npm `11.9.0`をworkspace内へ用意。既定Node `22.20.0`／npm `10.9.3`を変更しない |
| Java base | `eclipse-temurin:21-jre-noble@sha256:0eda5830ed7b669abc246a8dc520ea8a23cab06f9a8d4b6964109e2f2394e46f` |
| PostgreSQL | `postgres:17-alpine@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73` |
| JAR SHA-256 | `a6fa0a155060ac7740b60e872d4aef81dc8791641476a96c06f2abe0037c4b50` |
| 最終project／app image ID | `d3-integrated-02`／`sha256:83a281833a21b9ff34a8b90f0405e9a9d5bffa30d4f165d1e523c872151ea106` |
| app設定 | UID／GID `10001:10001`、read-only root、専用`/tmp`、heap768 MiB、Container1.5 GiB／CPU1。Java PID1・実UID設定・read-only・memory・image内JAR hashをinspect／execで照合 |
| DB設定 | Container1 GiB／CPU1、`max_connections=16`、app role`koiki`は非superuser。PG管理passwordとapp passwordを分離 |
| 公開・secret | `127.0.0.1:18081`だけをappへ公開。DB portは非公開。DB password／HMACをsecret file＋config treeで供給。loopback HTTP・Secure Cookie無効の限定条件 |

Mavenは`-pl koiki-reference-app -am -P b1-session-spa,d3-container`でpackageし、既存SPAのbuild recordとJAR中asset hashを照合した。Source epochは`ded2698`＋今回の未commit差分。実行時点の実装sourceの16 file hashを`.tmp/d3-integrated-source.json`へ保存した（結果文書等は別）。結果確認後、runnerへ既存証拠／同名project資源のpreflight拒否guardとREADME補足を追加した。最終inventoryは`.tmp/d3-integrated-source-final.json`に別保存し、guard追加後は構文確認だけでContainer検証を再実行していない。実行時と最終sourceの差分を区別する。

## 4. 実行した確認

| 確認 | 結果・範囲 |
| --- | --- |
| JAR生成・直接回帰 | Maven success。新規probe Security2件＋既存Session API Security22件＋既存browser Security設定1件＝25件、failure／error／skip 0。frontend build・test compile・repackage成功 |
| 通常profileの保護 | `d3-container` Maven profileなしで新規Security2件を選択実行し成功。通常構成でprobe chain未登録・未定義経路拒否を確認。全通常rootは未実行 |
| DC01 | 非root／read-only／PID1、image内JAR hash一致、SPA asset hash・async JDBC非同梱を確認 |
| DC02 | 専用DBのKOIKI→Reference migration、health UP、Container healthy・起動成功 |
| DC04 | live／ready匿名GET成功、health root／env／未認証SPA APIは401、probe POSTは401で拒否。probe応答は`status`だけでdetails／componentsなし。app DB role非superuser |
| DC03 | 実Chromiumでlogin→SPA→1200円の下書き作成201→CSRF欠落の提出403→正規提出204→reload→logout→API401。Session HttpOnly・JSから非参照、page errorなし。画面PNGを目視確認 |
| DB／Audit突合 | 申請`4b494ea2-3e62-4dfb-93d8-dd613f890cd5`は`SUBMITTED:1200`。`SUBMIT_EXPENSE` Business Audit1件、async publication schema0 |
| DC07 | 同image・同DBでappをrestartしreadyへ復帰。申請値・Audit・両migration history件数を維持。処理中HTTP要求のdrain／終了時間上限を全件実証したものではない |
| DC06 | DB停止中ready503・live200、DB再開後ready200。Audit1件のままで業務再送なし |
| DC08 | 検証runnerのfinallyで当該projectだけをdown／volume削除。container・volume・network残留0を確認し、最後に再照合。local image／ignored証拠・secret fileは保持 |
| DC05限定負例 | networkなし・secret mountなしの単発appはexit1、必須`configtree:/run/secrets/`不存在で起動拒否。`--rm`で終了。個別password／HMAC欠落・不正値の全組合せは未実施 |

有限runnerの7 checkpoint群がPASSし、別途DC05の上記1負例が成立した。25＋通常profile2件は実行回数の別記録であり、重複する2件を新規の異なる27件とは数えない。

## 5. 初回失敗・訂正の保持

1. Node24.19.0の同梱npmは11.17.0で既存frontend指定11.9.0と異なり、最初のpackageはfrontend version guardで停止。npm11.9.0を追加取得したがWindows shimをpackage内binから直接使う配置が不正で再度停止した。workspace内に正規の`node.exe`＋`npm.cmd`＋`node_modules/npm`配置を作り、指定版確認後のpackageが成功した。既存frontendの版制約は緩めていない。
2. 準備側がSPA格納先を`BOOT-INF/classes/META-INF/resources/spa`と誤って期待して停止。実JARと既存`SessionSpaArtifact`に従いrootの`META-INF/resources/spa`へ訂正し、埋込build record／asset hashを追加照合。生成JAR自体の欠落ではなかった。
3. 新規browser補助の`APIResponse`をAutoCloseableとしてcompileした誤りは、既存APIの`dispose()`へ訂正した。JAR／production変更なし。
4. 初回project `d3-integrated-01`はstartup・artifact確認後、probe POSTの期待403に対して実401で停止。単独filter-chain testのCSRF403と実サーバーのerror dispatch後401を分け、検証側だけを401へ訂正した。拒否自体は成立しておりSecurityを緩めていない。初回をcleanupし、同JAR・別project／新DBの`d3-integrated-02`で後続journeyを含む検証を実行し成功した。

初回package logは`.tmp/d3-build-initial.log`、shim失敗は`.tmp/d3-build-corrected.log`、訂正後成功は`.tmp/d3-build-pinned.log`。初回Container結果は`.tmp/d3-integrated-01/`、最終結果は`.tmp/d3-integrated-02/`、通常profileは`.tmp/d3-default-profile.log`、secret負例は`.tmp/d3-missing-secret.log`に保持。binary／raw log／secretはGitへ格納しない。

## 6. 残る範囲・次の判断

方式1のlocal Container技術成立の結果を確認し、差分・手順のOwner review／結果受入と成果固定を判断する。追加動的実行を自動反復しない。

未実施はHTTPS／Secure Cookie／信頼proxy、処理中要求のgraceful drain・強制停止、個別secret負例の追加、案件repoでの独立Consumer build／起動、全通常root／CI、ECS・registry公開。方式2のNext.js／Keycloak SSOは計画のまま。

E1の既存公開版・V09、V10未完了、S1未達／人手HOLD・C2 H07・D1結果受入未記録、正式support／全DoD／Phase4全体の未完了を維持する。今回commit／push／PR／merge／tag／Releaseは実行していない。

## 7. ローカルブラウザ手動確認用の別環境（後続Owner指示）

OwnerはローカルPCのbrowserからの手動確認を希望した。`start-manual.py`と承認者fixtureを追加し、新しい専用project `d3-manual-20261105`をbuild・起動して保持した。同じJAR hashをimage中と照合し、HTTPで申請者のlogin／SPA options、承認者のlogin／承認一覧200を確認した。申請データはまだ作成しておらず、Ownerの手動browser結果・受入は未記録。

接続先は`http://127.0.0.1:18081/login`／`/spa/`。利用者は`d3-applicant@example.test`と`d3-approver@example.test`。生成passwordの参照は`.tmp/d3-manual-20261105/secrets/login-password`、稼働記録は同workの`manual-ready.json`／`manual-status.jsonl`。password本文は本書へ記載しない。

この別projectのapp／DB／volume／secretは手動確認用に保持し、自動検証時のcleanup済み記録と区別する。停止・データ保持再開・最終削除のcommandは[資材READMEの手動確認節](../../../build-support/container-verification/integrated/README.md#ローカルブラウザで手動確認する場合)を参照する。

## 8. 承認者のホーム導線訂正（手動確認feedback）

Ownerから承認一覧を確認できないとの指摘があり、稼働DBを読取照合した。承認者の`EXPENSE:APPROVE`／部門scopeは正しく、独立Sessionで一覧・詳細200と提出済み1件を確認した。一方、共通headerの「経費申請」は申請者用`/expenses`で拒否され、ホーム本文にも承認用リンクがないことを確認した。

Referenceの`HomeController`／`home.html`だけに権限別の入口を追加し、承認者には「承認対象の経費申請」、申請者には自身の申請と同梱SPAを案内する。URL側の認可は維持し、Framework共通fragment・権限・申請値を変更しない。既存SPA配置testのhomeアクセスfixtureを申請者Authentication付きへ合わせた。

初回incremental buildは旧fixtureのリンク期待FAILと、既存test-classのpackage不整合25 ERRORを記録（`.tmp/d3-home-navigation-build.log`）。fixture訂正・clean再compile後はSPA配置2件・probe Security2件・MVC URL Security25件の計29件、failure／error／skip0でpackage成功（`.tmp/d3-home-navigation-clean.log`）。新規の別29件を追加したという意味ではない。

手動用appだけをbuild・recreateし、同じDB containerとvolumeを保持した。新JAR SHA-256は`ceb99d5abb727aa8a92702cbbd6f639d90c27d23da006201048c9c4b18a24302`。更新前のartifact／handoff記録を同workの`*-before-home-navigation.json`へ保存し、現行`manual-ready.json`を追随した。以前の自動検証JARの結果を新JAR全体の再検証として扱わない。

実Chromiumで承認者login→ホーム本文の承認リンク→提出済み一覧を確認し、`approver-home.png`／`approvals.png`を同workへ保存。申請`fdead68b-7143-4ced-a98c-89cf90cad087`は更新前後とも`SUBMITTED`、承認・却下等は未操作。手動環境は起動保持し、Ownerの後続確認・結果受入は別管理する。

## 9. MVC選択肢のUUID不整合の訂正（手動確認feedback）

OwnerからMVC作成時の部門・経費科目の正規表現エラーが報告された。原因はTooling seedのmaster UUIDがversion／variantに適合しない`d3000000-0000-0000-0000-...`だったこと。SPAのUUID形式条件とMVCのversion／variant条件の差により、自動SPA journeyだけでは検出できていなかった。MVCの入力検証・production codeは変更せず、seedの部門・科目IDと参照を`d3000000-0000-4000-8000-...004／005`へ訂正した。

稼働中の手動DBを`.tmp/d3-manual-20261105/db-before-uuid-repair.sql`へ退避後、Toolingの一回用`repair-manual-master-ids.sql`で既知master・利用者割当・承認scope・申請／明細の参照を単一transactionで更新した。申請／明細は当該master ID以外の全列、Auditは全列を訂正前後で双方向EXCEPT照合し、不一致ならrollbackする。対象はこの旧D3 fixtureに限定し、Framework migrationへ追加しない。

初回はguard内のUUID文字列のsegment不足で変更前に停止・rollback。誤記訂正後、既存申請2件／明細2件・Audit15行の不変照合を含めcommitし、appだけをrestartして選択肢cacheを更新した。実行記録は同workの`uuid-repair.log`。DB container／volumeと認証利用者は維持する。

実ChromiumでMVCフォームの選択→1円の下書き保存POST302→詳細表示を確認。確認用下書き`54391d21-b0b7-4c56-b22b-f02ad76bf2a1`（内容`MVC UUID repair verification`）を1件追加し、`mvc-create-after-uuid-repair.png`へ画面を保存した。訂正前の既存2件は`APPROVED`／`SUBMITTED`の状態を保持。開いていたフォームは旧IDを持つため再読込が必要。JARは§8の同一hashで、image再build・追加Maven実行は行っていない。

## 10. 手動確認feedbackと成果固定・統合の指示

OwnerはUUID訂正後に「問題解消しました」と確認した。これは当該MVC作成エラーの解消確認として記録し、全検証結果・未実施範囲の受入へ拡張しない。UUID訂正はTooling seed／検証DBの問題でReference code・JAR変更なし、承認入口の訂正はReference appの再生成・image更新だけでFramework配布JAR群の再生成／再公開は不要、という境界を確認した。

Ownerは現在の検証確認段階でcommit・作業branch push・main向けPR／mergeを進める方針を示した。今回の実装・設計／結果文書を成果固定し、差分レビューと既存required checks成功後にmergeする。CI workflowへContainer build／起動は追加しない。新規probe testとReferenceのホーム／fixtureは既存通常CIの対象で、local限定結果だけでCI成功を代用しない。tag／Release／image公開は今回含めず、手動環境・DB・ignored証拠は保持する。

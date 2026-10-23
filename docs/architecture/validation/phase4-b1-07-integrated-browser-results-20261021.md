# B1-07：通常／同梱構成の合流回帰と実JAR browser統合実演

実行日：2026-10-21。時刻はUTC。

状態：**Cloud側の実装・統合検証完了**。最終通常／同梱構成は各422件実行PASS＋99 skip。同じ最終同梱JARの新旧browser journey計2件PASS。ローカルPCの人による手動確認・B1-08のOwner受入は未実施。

## 1. 指示・sourceと変更

OwnerのB1-07開始指示に基づく。branchは`docs/phase4-review-integrated-plan-20261020`、B1-06成果`2b0ed5b`・clean treeを開始点とする。今回の成果commit・push・PR作成は行っていない。

| 所有／対象 | 対応 |
| --- | --- |
| Tooling：[新規Session SPA journey](../../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceSessionSpaJourneyTest.java) | 実JAR／隔離PostgreSQL／ChromiumでCookie・CSRF・SPA／MVC・競合・logout／失効・DB／Audit／logを突合。明示flagなしはskip |
| Tooling：[artifact照合](../../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/SessionSpaArtifact.java) | 起動前に指定JAR SHA-256、private build記録、profile、全frontend入力・lock、asset bytesを照合 |
| Tooling：[既存Critical Journey](../../../build-support/reference-e2e-verification/src/test/java/org/koikifw/buildsupport/referencee2e/PackagedReferenceCriticalJourneyTest.java) | B1明示選択時だけ同じartifact照合を追加。起動・seed・終了helperをpackage内で再利用。既存のBearer→MVC承認の操作・assertionを維持 |
| Reference：[ExpenseDraftForm](../../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/inbound/web/ExpenseDraftForm.java) | 実演で判明したMVC利用日表示の不具合を、`@DateTimeFormat(iso=ISO.DATE)`だけで訂正 |
| Reference：[MVC回帰](../../../koiki-reference-app/src/test/java/org/koikifw/reference/web/ReferenceBusinessUrlSecurityTest.java) | en-US／ja-JPで編集画面のISO初期値とISO POST bindingを2 invocationで検証 |
| Reference：[ExpenseApplicationService](../../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/application/ExpenseApplicationService.java) | 同じIDの明細instanceを置き換えた編集後、既存Repositoryのsaveを通してからflushする |
| Reference：[実DB回帰](../../../koiki-reference-app/src/test/java/org/koikifw/reference/expense/ExpensePostgreSqlIntegrationTest.java) | 同じ明細IDでの編集、ID／件数の維持、金額／内容・version+1・Audit追加なしを1件追加 |

Framework・Security設定・Domain model・Repository契約・migration・依存・frontend入力／lockは変更していない。Reference Applicationの変更は編集時saveだけ。UI故障注入はToolingのbrowser応答保留に限定し、製品側へfixture endpointを追加していない。

## 2. 実演途中の停止と局所訂正

1. 初回新規journeyはlogin／Cookie確認後、storage全体が空という検査でFAIL。実JAR内HTMX2.0.10は`sessionStorage`へ`htmx-current-path-for-history`を保存する。既存MVCのpath記録を考慮し、SPAがstorageを変更せず、認証情報・業務データを保存しないことを検査する条件へ訂正した。
2. 次の実演はSPA作成・大整数・CSRF／DTO拒否を通過し、MVC編集後のredirect待機でtimeout。秘密値を含まないHTML validity観測を追加したところ、`usageDate`のdate入力がvalueMissing=trueと分かった。
3. MVC FormのLocalDateがlocale依存の表示になり、HTML dateのISO値と一致しない。追加回帰2件は修正前に両localeでFAILを再現した。Spring標準のISO指定を追加し、テスト側で利用日を埋め直す回避をせず検証する。
4. 製品sourceが変わったため、修正前の通常／同梱全件成功・JARは保持し、修正後の通常／同梱構成を各1回再検証する。無変更でのroot反復は行っていない。新旧journeyの最終結果は修正後の同じ同梱JARだけに紐付ける。
5. 日付訂正後の実演ではMVC編集POSTが409になり、HTTPを外した同じ明細IDの実DB回帰1件でも`CONFLICT`を再現した。内容を新しい明細instanceへ置き換えた後にflushする経路を、既存Repositoryの`save(request)`を通す最小変更で訂正。局所1件はPASSし、明細IDを変更しない更新とversion進行を確認した。
6. 日付のみの通常／同梱成功は別epochで保持し、両訂正を含む最終sourceで各構成を1回再確認する。追加是正に伴い合流実行の時間見積りを75分へ更新し、cleanup10分の予約と資源条件は維持する。

7. 最終JARの新規実演はMVC編集・409再取得／明示submit・DB／Audit・遅延GETとlogoutまで通過し、scope外submitを403としたfixture期待で停止した。既存設計§5.5／実DB回帰はread／mutationとも404で存在を秘匿するため、期待を404へ訂正する。CSRFなし匿名POSTも認証より先に403となる既存条件へ合わせ、製品側のSecurityや応答仕様は変えない。

8. scope期待訂正後は失効確認で停止した。fixtureが`expiry_time`のみを更新していたが、JDBC Sessionの`isExpired`は最終アクセス時刻と有効期間からも再構成する。既存B1-05の実DB回帰と同じ`last_access_time=0, expiry_time=0`へ訂正し、製品側のSession実装は変更しない。

## 3. build・XML・artifact

| 集合 | 実行 | skip | failure／error | 状態 |
| --- | ---: | ---: | ---: | --- |
| 修正前 通常root clean verify | 419 | 99 | 0／0 | SUCCESS、6分41秒 |
| 修正前 同梱profile clean verify | 419 | 99 | 0／0 | SUCCESS、6分29秒 |
| 日付訂正後 通常root clean verify | 421 | 99 | 0／0 | SUCCESS、6分36秒 |
| 日付訂正後 同梱profile clean verify | 421 | 99 | 0／0 | SUCCESS、6分20秒 |
| 両訂正後 最終通常root clean verify | 422 | 99 | 0／0 | SUCCESS、6分34秒 |
| 両訂正後 最終同梱profile clean verify | 422 | 99 | 0／0 | SUCCESS、6分49秒 |
| 最終 新規Session SPA journey | 1 | 0 | 0／0 | PASS、29.05秒 |
| 最終 既存Critical Journey | 1 | 0 | 0／0 | PASS、25.93秒 |

各buildはfresh XMLを集計し、次のclean前にXML・JARを別directoryへ退避する。99 skipは専用検証の通常分離であり、PASSへ加算しない。修正前／後とprofile違いの集合を合算して異なるtest数としない。新規journeyの明示flagなし1 skipは別記録に保存し、実演成功へ加算しない。

最終通常rootの内訳はArchitecture Contract4＋Rules107＋Reference311＝422実行。ReferenceのMVC／Security class25件には追加locale2件を含む。通常JARにはSPA／build記録がない。

JAR SHA-256：

- 修正前 通常：`7e5488e26cf89b95e88437b1bd20b948d85843d6dde2f9e78d6b91c4c5d14114`
- 修正前 SPA：`175dc7360df4984bca5a1dd2cb3c476f07d8951e59b4d2efc750156c468e5d39`
- 日付訂正後 通常：`e03f3646f20ca16f1427881a537cfee22b6a1a5e2542858cd94b28b77772c475`
- 日付訂正後 SPA：`47e57e878af022916e35f0265dcf417c608fc868fb1727e63f5bd246a49d90e4`
- 最終 通常：`78e329e4b439f7a4b970a12253a8a5cafd341cf206b894bec538d0091b797d0e`
- 最終 SPA：`32e1c955c8458f052266c9ee0a0da59cdfa9e2d595e84dc0a46d26f8bf34e963`

frontend入力15 file、同梱index・JS・CSS3 fileのhashをbuild記録と照合する。META-INFはBoot JAR rootに置かれ、`META-INF/koiki-reference-spa-build.json`は静的配信外とする。source map・node_modules・frontend source・公開manifestを同梱しない。

## 4. browser確認の集合と制約

新規`crossesSessionSpaMvcBoundariesAndDiscardsExpiredViews`は1 method／1 invocation。内部checkpointを別件数に加算しない。

- 実login、SESSIONのHttpOnly／Lax／Path=/・HTTP fixtureのSecure=false、XSRF cookieのJS可読、POST headerとCookieの対応。
- SPA作成、Stringの金額／version／totalElements、2^53超の整数精度、text escape、直接詳細／reload、storage非保存。
- CSRF欠落・誤値・MVC token拒否、DTO400・業務422、MVC参照／編集→SPA409→最新GET→明示submit・連打抑止。
- 未知path／API version／不足asset／private記録の404、scope外のread／mutationはいずれも404、権限不足403。
- 実GETの取得済み応答をbrowser側で保留→logout→遅延応答で画面が戻らないこと、Cookie削除、旧Session拒否、戻る／reload。
- DB expiry→401・表示破棄、別利用者／権限で再login、実navigationとhistory restoreで未保存入力を復元しないこと。
- DB state／version／明細合計、Business Auditのsubmit1件とactor、Security login Audit、sanitized log、所有資源の終了。

既存Critical JourneyはBearer作成・提出→Session MVC承認／HTMX検索→logout→Bearer再参照の1 invocation。新規と同じ最終同梱JARを使い、間で再packageしない。Cloudはheadless Chromium。最終JARのdesktop1280／mobile390のscreenshotを目視し、大整数・明細の文字表示と配置を確認した。ただし、人の実操作・OSのtab visibilityやWindows build確認の代用にはしない。

## 5. 環境・資源・実行・証拠

保存activationによるJDK21.0.12.1、Wrapper Maven3.9.16、Node24.19.0／npm11.9.0、Playwright Java1.62.0、PostgreSQL17 imageを使用。Cloudはcgroup memory8 GiB／CPU2。設定済みproxy・CA・checksum／npm integrityを維持し、依存version・権限設定の回避変更はない。

buildはMaven／test heap768 MiB、fork1／reuseForks=false、JUnit／reactor並列なし、通常DB専用selectorでDB memory1 GiB／CPU1／max_connections16。既存pool既定最大10を維持し、専用test有効化flagを与えない。browserはMaven／test／application heap768 MiB、application pool4／idle1、DB同制限、Chromium1 context・issuer1・子process1、新旧直列。container inspect／SHOW・JVM／Hikari実効値を照合する。

通常／同梱共通引数：

```bash
source /workspace/cloud-setup/activate.sh
export MAVEN_OPTS="$MAVEN_OPTS -Xmx768m"
./mvnw -o -B -ntp -DargLine=-Xmx768m -DforkCount=1 -DreuseForks=false \
  -Djunit.jupiter.execution.parallel.enabled=false \
  -Dkoiki.reference.normal-root.db-limits.enabled=true clean verify
# 同梱構成は上記へ -pl koiki-reference-app -am -Pb1-session-spa を追加
```

browserは[Tooling README](../../../build-support/reference-e2e-verification/README.md#b1-07-session-spa-packaged-journey)の引数でclassごとに実行し、固定SHA-256を渡す。sandboxのDocker／browser制限には承認済みの権限付き実行を使用する。

rawは`/workspace/cloud-setup/logs/phase4-b1-07-20261021/`に保存する。修正前は`normal/`／`spa/`、日付訂正後は`normal-corrected/`／`spa-corrected/`、最終は`normal-final/`／`spa-final/`。途中停止のXML・sourceと修正前再現を別保存し、最終source hash／patch・XML／artifact・資源sample／cleanupを対応付ける。端末内rawやtargetのPNGはGit成果物へ含めない。

最終2 journeyは両方でSHA-256 `32e1c955c8458f052266c9ee0a0da59cdfa9e2d595e84dc0a46d26f8bf34e963`を起動前照合し、終了後もJAR bytes不変を確認した。途中で再packageしていない。新規の検査済みprocess log・checkpoint・cleanup・PNGは`session-spa-final/evidence/`、既存のheap／pool診断は`critical-final/resource-evidence/`へ退避した。途中停止の結果を最終PASSへ加算していない。

資源は約2秒間隔の観測でDB同時最大1、Ryuk最大2、cgroup memory最大2743 MiB、DB client最大13、最終browser集合のclient最大4。これはsampleの最大であり、全時点の連続上限証明ではない。DBの設定上限16、通常接続予算14／browser8を維持した。各集合後のDocker残存0と、8回の実JAR起動で所有したapplication PID全停止を最終確認。新旧の成功test内でbrowser／context・issuer／application port・DB・temp logの終了／解放をassertし、resource monitorも終了した。

最終buildは10:45:13 UTC、新規journeyは10:52台、既存は10:53:43 UTCに終了し、75分の更新見積り内で検証を終えた。cleanupとEvidence整理はその後に別予約枠で行った。

## 6. 次の判断

[ローカルPCの実ブラウザー確認計画](../../development/phase4-b1-07-local-browser-check-plan-20261021.md)を用意した。Cloud成果の固定・push後に同branchをpullし、Windows build・使い捨てdemo seed・人の操作・cleanupを別に記録する。現時点で手動確認を実施済みとしない。DoD 4-8／4-9のOwner受入はB1-08で判断する。

# S1 R0-B：文書source固定・preflight停止記録（2026-10-09）

**状態：SOURCE FIXED / PREFLIGHT STOPPED — 接続上限と既存回帰実績の不整合。** code作成・test実行は未開始。source固定local commitはOwner承認に従って1回実施済み。

## 1 文書commitの実行結果

Ownerは「local commit 1回で固定してよい」と明示した。[commitレビュー](../../development/phase4-s1-r0-rules-document-source-fixed-commit-review-20261009.md)の14 pathとworktree集合・branch／親HEADを照合し、対象だけをstageして実行した。

- branch：`feature/phase4-s1-reference-foundation`
- 親HEAD：`fe93b5da76a85fd6a4c41409c725c35661fac007`
- 新HEAD：`9c41aa4b3fc93cd0c6afb2c767171a771e42616b`
- message：`docs: approve S1 R0 event-level rules contract`
- 対象：文書14 file、551 insertion／2 deletion。commit直後worktree clean。
- code／POMの親commitとの差分0。remote操作0。

本票と訂正レビューはその後のpreflight記録として追加する未commit文書であり、上記commitへamend・追加commitしていない。

## 2 preflightで確認したこと

JDKはTemurin `21.0.12.1`、Maven `3.9.16`。ParentのJDK `[21,22)`／Maven `[3.9.16,3.10.0)`条件に適合。wrapper配布物は既存cacheに存在し、version確認だけを実施した。dependency取得・test／verify／packageは実行していない。

Rules testのannotation検索は既存67 method候補。予定新規40を加えた107候補はmodule120件の上限内だが、invocation確定・fresh XMLは未実施。既存B2専用m2にはRules JAR、Modulith API `2.1.1`、Surefire JUnit provider `3.5.6`がある。Playwright `1.62.0`は同m2にはなく、元の`.m2/repository`には存在する。browser cacheも存在する。依存closure／新専用classpathの完全成立は未確認で、限定取得はしていない。

最終read-only baselineではDocker Server `29.5.3`、container0。既存Java PID `24852`／`10280`（親PID `5292`）を保持し、今回所有Java／container0。空きmemory `17,716,867,072` bytes、disk `47,662,260,224` bytesで開始条件を満たす。これら既存Javaを今回の検証用と扱わず、停止しない。

Rules／Reference／Tooling／E2E sourceとPOMの保護hash台帳417 fileを保存した。旧tmpのsource／raw／cacheを変更せず、copy・生成・baseline Consumer compileは未実施。

## 3 停止した理由

[承認済み規約票§7.2](../../development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md)は今回所有DB接続8を上限とする。一方、同日の受入済み通常rootの`root-separation-resources.json`ではclient connection最大11、8超のsample14件を確認した。sample例は2026-10-09 09:24:35.899 JST、PostgreSQL1／Ryuk1／JVM2／client connection11で、観測不可sampleではない。

これは旧rootの実測であり、今回rootが既に上限超過した結果ではない。今回rootは未実行。新sourceで接続8以内に収まる証拠がなく、既存test／pool変更も除外なので、資源条件の整合前にcode・testを先行しなかった。開始票作成時に既存回帰の接続実績照合が不足していた。

旧rootの最大はJVM3／PostgreSQL1／Ryuk2／接続11。Ryuk2は今回root限定上限に収まるが、接続11は8に収まらない。計測対象からobserverや管理接続を引いて数値を小さくしない。

## 4 予算・証拠・再開条件

初回preflight開始は2026-10-09 15:48:51 JST、停止集約まで実測227秒。source hash・最終確認等の管理予備13秒を保守的に加算して240秒（4分）を計上する。文書編集・Owner／実行環境承認待ちは除外。総60分枠の残56分、preflight10分枠の残6分、cleanup予約5分を維持する。未消費を追加実行許可にしない。

端末内raw：`tmp/r0-rules-9c41aa4-20261009-first/evidence/`。
`commit-preflight-result.json`、`environment-baseline.json`、`protected-source-manifest.json`にcommit identity、停止理由、予算・baselineを保存した。元のroot sampleは`tmp/b2-normal-build-8b03b9f-20261009-first/evidence/root-separation-resources.json`で保持する。Gitだけではこれら端末内rawを別端末へ移さない。

再開は[root接続上限限定訂正レビュー](../../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)のOwner判断と、採用した訂正文書の別source固定後だけ。規約code／Public APIは未変更、test0、起動container0、追加再実行0。R0-C受入・R1／runtime開始・remoteは未成立のまま保持する。

# S1 R0-B：通常rootの接続上限限定訂正Ownerレビュー（2026-10-09）

**状態：OWNER APPROVED（2026-10-09）、訂正文書commit実行前。** source固定commit `9c41aa4`は完了。[preflight停止Evidence](../architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md)の実績不整合を解消する本案をOwner承認済み。§3の文書4件を1回固定し、残preflightへ戻る。

## 1 訂正案

通常root `clean verify`だけ、今回所有PostgreSQLのclient connection上限を**8→12**へ訂正する。他phaseの接続上限8、JVM4／PostgreSQL1、root限定Ryuk2／他phase Ryuk1、memory／disk／raw／file／件数・各集合1回・追加再実行0回、既存test／pool不変更を維持する。

根拠は、同日受入済みrootの観測最大11（8超14 sample）。12は今回の停止上限案であって、現在sourceの実測PASSや本番値ではない。今回rootで12超・観測不能・他上限超を見つければ停止する。observer／管理接続も数え、定義を狭めて上限内に見せない。

新provider／依存／agent取得、既存testのpool削減、検証追加、Reference code・POM・SQL変更は採用案に含めない。依存closure・classpath・旧Consumer bytecode等の残preflightは成立を確認してからcodeへ進む。不足取得が必要なら一覧を提示して停止する。

## 2 残予算と再開

総60分のうち240秒計上、残56分。preflight残6分、Rules15分／root15分／PL2候補5分／package・既存E2E10分／cleanup5分を維持する。追加再実行を増やさない。ここまでtestは0件、再開後の予定した初回集合だけを実行する。

訂正承認後に`AGENTS.md`と規約票§7.2のroot限定接続上限を反映し、承認sourceと保全済みbaselineの差分を確認する。scope／資源・依存・preflight条件不成立ではcodeへ進まない。

## 3 訂正文書のsource固定案（別承認）

先の承認による文書14件のlocal commit1回は既に完了した。本訂正で必要になる追加commitは別判断とし、先の許可を流用しない。

承認後の文書4件だけを現在branchへ**追加local commit1回**で固定する案：

1. `AGENTS.md`：root接続だけ12へ訂正、他条件維持。
2. `docs/development/phase4-s1-r0-rule28-29-level-selection-owner-review-20261009.md`：§7.2のroot限定接続訂正と実際のOwner承認記録。
3. `docs/architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md`：完了commitとpreflight停止のEvidence。
4. `docs/development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md`：本訂正案・採否・固定対象。

全体の文書対象は既存14＋新規2＝16で、規約票の上限内。codeはstageしない。message案は`docs: correct R0 root connection ceiling`。親`9c41aa4b3fc93cd0c6afb2c767171a771e42616b`・対象4件・新HEAD・cleanを確認し、そのsourceで承認済み残preflightへ戻る。remoteは行わない。

## 4 判断欄

Ownerは本票を指定して「確認、承認いたします」と明示した。root限定接続12、他上限・残予算維持の訂正採用、および§3文書4件の追加local commit1回と残preflight再開を承認済み。規約結果受入・成果commit・R1／Reference runtime・remoteの承認は含まない。commit後のSHA・clean・preflight実測はrun Evidenceへ記録し、本票の追加commitを発生させない。

## 5 再開後のpreflight時間計上・限定訂正（OWNER APPROVED）

§1〜4の接続上限訂正と4件commitは実行済み（`671fd17`）。本節はOwnerの個別承認を受けた追加訂正である。[Evidence§5](../architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md)の技術条件は成立したが、時間guardのUTC二重変換とOwner待ちの分離計測不足により、preflight枠の成立が確認できずcode前で停止した。

### 5.1 時間枠だけの訂正

preflight上限を10→15分とする案。総60分は増やさず、既計上810秒・残46分30秒、cleanup5分予約を維持する。現段階の技術条件確認は成立しているため、未済のsource固定後差分確認だけを行い、新たなclasspath探索・依存取得・既実施準備の反復をしない。既存依存補完の失敗2回を保全し、予定のvalidation各集合はまだ0回で、実行はそれぞれ初回だけ。

Rules15分・root15分・PL2候補5分・package／既存E2E10分の個別上限を維持するが、総残予算が優先する。各枠の最大値を使い切れる許可とはせず、cleanup予約を残せない場合は停止する。接続root12／他8、他資源・file／case・raw・追加再実行0回・既存test不変更・remote除外を維持する。

時間guardはraw timestampをDateTimeOffsetで扱い、今後の各実行はprocessの実行開始・終了／実効timeoutを計測し、Owner待ちと文書作成を混入させない。タイマ訂正はrun所有の一時harnessだけで、Framework code／依存を変更しない。9時間多い元recordを消さない。

### 5.2 訂正source固定案

本節の採用時に、§3と同じ文書4件（AGENTS、規約票、本票、preflight Evidence）だけへ時間条件・実際の承認記録を反映し、現在branchへ追加local commit1回で固定する案。親は`671fd171a7c2ce70eff571995a5f20b3c598c1ae`。先のcommit許可は実行済みなので、今回の追加固定は別判断とする。全体文書対象16 fileを維持する。

Ownerには、preflight15分・総60分／残46分30秒・他上限維持、上記文書4件の追加固定と承認済み初回validationへ向けた条件付きcode作成を判断対象として提示する。Ownerは「この限定訂正で進めてよいです」と明示した。preflight15分、総60分・既計上810秒／残46分30秒・cleanup300秒予約、他上限維持と文書4件の追加local commit1回、source固定後の差分確認および成立時の承認済みcode／初回validationを承認済み。成果commit・結果受入・R1／remoteは含まない。

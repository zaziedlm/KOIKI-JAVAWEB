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

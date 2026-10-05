# S1初回局所検証後の後続タスク（2026-10-05）

**状態:** 後続順序はDRAFT。4項目の方針候補継続・残条件具体化はOWNER APPROVED（2026-10-05、[方式票§8.3](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#83-owner方針レビュー承認記録2026-10-05)）。追加検証／正式実装の開始承認ではない。
**確認済みbaseline:** `docs/daily-development-workflow` / `5252124`。整理開始時の作業treeはclean。preflight・初回L1〜4、Evidence・方式票への結果反映をcommit済み。
**現在の入口:** [方式票§7・8](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#7-局所結果の方式票への反映2026-10-05)。候補・初回開始の承認は同票§1.1、検証結果は[局所Evidence](../architecture/validation/phase4-s1-minimum-local-verification-20261005.md)を正本とする。本書は順序と残タスクの案内であり、既存CP／Gateを改訂しない。

## 1. 完了した範囲と残る境界

| 項目 | 現在の状態 |
|---|---|
| 初回ST-B | L1保存11、L2競合9、L3executor相関・対象選別8、L4mode12の計40 invocationが順次実行でLOCAL PASS。方式票へ結果反映済み |
| 最小構成 | 許可＋append-only消費の2種類を局所候補として了解済み。将来管理機能との分離、人の総合判断を維持 |
| Owner方針レビュー | 方式票§8のTier・認可・Audit・正式Reference接続の4項目継続と残条件具体化は承認済み。正式採用・追加開始判断は残る |
| 追加検証／正式開始 | Aの作成・実行・契約上限はOWNER APPROVED（2026-10-05、追加契約§6.1）。B／C・ST-C〜E、正式Tier・Public API／Rule／migration・依存の採用、DoD／Gateは未成立 |

L2の結果不明はtest入力、L4の復旧runnerはID準備まで。別process再起動後の不明保全・実復旧実行・実JPA／認証／Audit／Reference起動・実trace／provider／OS crashは未実証である。初回PASSを全test回帰や通知配信完遂の証明にしない。

## 2. 後続の順序・成果物・開始条件

Owner一人＋Codexで順次進める。下表の順番号は作業整理用であり、新しいGate番号ではない。

| 順 | タスク・対応 | 成果物／出口 | 開始条件・現在の状態 |
|---|---|---|---|
| 1 | **Owner方針レビュー**：方式票§8の4論点 | 4項目の候補継続・残条件具体化を承認し、方式票§8.3へ記録 | **完了／OWNER APPROVED（2026-10-05）。** 正式Tier確定・包括的実装開始は含めない |
| 2 | **正式接続条件の具体化**：B2／B3／B4、CH-01〜04 | [責務・認可／失効・Audit・mode／DB権限案](phase4-s1-responsibility-authorization-audit-mode-draft-20261005.md)を作成、Owner確認済み | **契約入力として受領（2026-10-05）。** 物理名／route／権限コード・具体方式の採用は残条件 |
| 3 | **追加局所検証契約・再見積**：ST-B追加、CH-08、J7 | [追加契約案](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md)でA：JPA、B：Identity／Audit、C：Web／modeを順次単位に分け、test・command・上限・技術量36〜72時間の低確度案を提示 | **契約案作成済み／個別開始判断待ち。** 最初はAの作成・実行だけを推奨。B／C依存・前置条件は後続review。初回工数枠は流用しない |
| 4 | **承認された追加局所検証**：ST-B追加 | まずAの実JPA SQL／lock／権限適合を検証。不成立は2／3へ戻す。B／Cの実Audit・認可・起動境界は別判断 | **Aのみ作成・実行・上限を承認済み（追加契約§6.1）。本日は記録まで、次回preflightから。** B／Cと実Reference変更は未承認 |
| 5 | **正式開始判断と基盤実装**：ST-C、CH-01〜04／07、J1／J2／J5／J8 | 対象・Tier／保存／認可／Audit／起動／migration／依存・Ruleの前置review、Gate正本差分、開始範囲を整合。承認後に基盤実装・fresh／upgrade・既存Web回帰のEvidence | 追加検証の必要証拠と残条件を反映し、正式開始Gate／CPの判断を得てから実装。CH-07のFramework契約変更は独立review。未解決条件下で送信を許可しない |
| 6 | **通知・実観測・実演環境**：ST-D、CH-05／06／08、A2／D1、J3／J6／J7 | 承認event→通知、stable key／stub受理、実metric／trace／alert、process停止・採取／flush、有限候補・保持／cleanup、固定artifact・環境台帳 | A1基盤とA2／D1・依存review、個別開始承認後。Customer実provider／SLAへ保証を広げない |
| 7 | **統合復旧・人による判断の実演**：ST-E、E01〜21／V5〜7 | 正常復旧、競合・停止／lock喪失、受理／commit不明、Audit失敗、再起動引継ぎ、保留／調査／最終解決をrunbookと証拠で突合。不足枝を追跡 | 5／6の条件と個別実演上限が成立後。case了解は網羅PASSではない。未知状態を自動解除しない |
| 8 | **DoD・CP／Gateの最終判断材料**：J4／J8 | DoD 4-2の時間起点／終点・条件、4-3の受理一意、適用範囲／未達・保持をまとめ、既存CP／Gateへ提出 | 時間条件は実演前に決め、結果を後付けで合格化しない。拒否／保留が成功しただけで配信完遂DoDをPASSにしない |

5のGate／正本整合の文書準備は2〜4と順次進めてよいが、正式実装の開始は前置判断が成立してからとする。追加Tooling PASSが正式Referenceの回帰やGate判定を代替することはない。

## 3. Owner方針レビューの対象と承認結果

以下の4項目の候補継続と残条件具体化をOwnerが承認した（2026-10-05）。正式採用・追加開始は後続の証拠と個別判断に委ねる。判定の正本は方式票§8.3。

1. **Tier:** 2種類記録と少数の不変条件に限定したRICH候補を継続するか。JPA／制限roleの成立を正式採用の条件に残す。
2. **認可:** Web本人認証で許可を発行し、non-Webで真正な許可と現在権限を確認する方向を継続するか。対象scope・失効窓・拒否条件を次に具体化する。
3. **Audit:** 業務操作の原子的記録と認可拒否等のSecurity記録を分け、既存Recorderに接続する方向を継続するか。actor・イベント分類と実transactionの検証を残す。
4. **正式Reference接続:** 通常Webを維持し、確認／復旧を必要Beanだけで組み立てる方向を継続するか。scan／Security／migration・DB副作用の証拠を開始条件へ渡す。

このレビューでは候補を進める方向を確認する。具体条件が未成立なら条件を記録して具体化へ戻し、正式採用／開始は対応する票で判断する。人がDB・process・provider・log／Auditを総合判断する運用前提を維持する。

## 4. 上限・コミット・別系統の扱い

- 追加作業の工数・資源・日程は未見積。初期176〜344時間、拡張200〜392時間、初回局所16〜32時間を現在の残量／予算として使わない。Ownerレビュー稼働・環境待ちも分けて記録する。
- 次の文書コミット候補は「Owner判定＋接続条件＋追加検証契約」が揃った時点。以後は承認された検証／実装単位ごとにcodeとEvidenceを対応付ける。コミットは採用／Gate承認を意味しない。
- **コミットと現在の区切り:** 方針承認・接続条件案・追加契約は`f145e37`にcommit済み。Aの作成・実行・上限を追加契約§6.1に承認記録。本日は記録まで。B／Cの残条件を先に全て閉じる必要はない。
- 他package・S2、Customer実案件の受入、依存関係制御、日常開発方式の実装は別判断。S1の後続へ自動追加しない。
- remote push／PR／merge、CI変更、publishは個別Owner承認の別操作として扱う。

**次回の再開点:** commit／branch／statusと[追加契約§6.1](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md#61-a単位のowner開始判定記録2026-10-05)を確認し、Aのpreflight→必須枝対応／証拠固定→fixture作成・単独実行へ進む。B／Cや正式Referenceへ拡張しない。本日は実作業未開始。

経過・具体的な再開順・停止条件は[次回用引継ぎ](phase4-s1-additional-a-next-session-handoff-20261005.md)へ集約した。

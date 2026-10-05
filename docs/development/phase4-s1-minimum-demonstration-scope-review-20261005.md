# S1実証の最小構成と将来の運用管理機能の分離（2026-10-05）

> **現在の参照先（2026-10-05）:** 最小構成と将来管理機能の分離方針を維持する。その後、2種類記録・目的別起動等の局所候補、狭いRICHのTier候補継続、初回ST-B作成／実行が方式票でOwner了解となった。正式Tier・全S1実装は後続判断。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**方針:** OWNER AGREED（2026-10-05）。Ownerは、S1実証に不可欠な最小構成と将来の運用管理機能を分け直し、notificationのTierは最小構成を確認してから判断する順序に同意した。
**具体方式:** DRAFT / 再review。最小方式・Tier・DDL / dependency・数値・Gate / 実装開始は未決定。
**Ownership:** Framework側の設計資料。Referenceの通知・復旧実行、Toolingの実証支援、人の判断を分担する。Owner一人＋Codexで順次進める。
**入力:** [前段の保存・依存・環境案](phase4-s1-request-storage-dependencies-environment-draft-20261005.md)、[RV票・preflight](phase4-s1-preimplementation-review-preflight-estimate-draft-20261005.md)、[安全条件](phase4-s1-safety-and-recovery-conditions-20261005.md)、[case計画](phase4-s1-dod-demonstration-plan-20261005.md)。

## 1. 見直す理由と合意範囲

前段案は安全条件の具体化から、要求受付・状態遷移・対象実行権・判断履歴をnotificationへ集約する方向へ広がった。これら一式を実装する必要性は、S1実証に必要な安全性とは別に判断すべきだった。Tier 2という設計手法自体より、対象機能を厚くしたことを見直す。

今回の合意は機能範囲と判断順序の修正であり、安全条件の免除、Tier 1の確定、復旧の無認可実行、DoD達成やGate通過の承認ではない。人の総合判断を前提とし、DB状態だけから再送許可を自動算出しない。

## 2. 初回S1で残すもの・後続へ分けるもの

| 項目 | 初回S1で必要な能力 | 将来の運用管理機能として分けるもの |
|---|---|---|
| 通常通知 | 承認commitとpublicationの整合、async受信、安定keyでのprovider送信、通知結果の記録 | 汎用通知サービス、宛先管理、通知種別拡張 |
| 復旧許可 | 信頼できる主体・現在権限 / scope、対象1件・実行条件の確認。不成立なら送信しない | 復旧申請の受付画面・承認フロー・要求一覧管理 |
| 停止・排他 | 通常processの停止 / drainと再起動制御、復旧者競合の排除、lock喪失窓とprovider冪等性の確認 | 常設の実行権 / lease管理サービス、自動引継ぎ・復旧orchestrator |
| 試行と結果 | 対象publicationの今回の試行を区別し、送信前の許可条件・送信後結果 / 不明を追跡できる最小記録 | 要求・試行・実行権・判断履歴の4種recordを備えた管理台帳一式 |
| 人の判断 | Ownerがprocess・DB・provider・Audit / logを照合し、再送 / 保留・調査・連絡を記録 | 常設担当割当、代行workflow、管理dashboard |
| Audit・観測 | 既存Audit契約、実metric / trace・logの突合、隔離環境でのalert受信と結果確認 | 共通運用portal、汎用観測Starter、外部通知サービス統合 |

右列の管理方式が左列を成立させる唯一の実装であるとは扱わない。ただし、簡素な方式で必須能力を満たせないと判明した場合は、必要部分だけを理由・差分とともに再reviewする。安全な拒否や不明時保留を削って小さくしない。

## 3. 最小構成の配置案と残る核心

- **Framework:** 現行Public契約を利用する。Level 2規約の明示選択・互換など元S1に必要な変更は独立reviewを維持する。業務復旧管理API / Starterを新設する前提を外す。
- **Reference:** 通常notificationと、認可・対象確認・排他・結果確認を備える専用non-Web復旧実行経路を最小候補とする。常設要求管理や承認画面を必須としない。
- **Tooling:** 停止証拠・provider stub・故障注入・観測採取・証拠整理を担う。Toolingから無認可でReferenceの再送処理を呼ぶ抜け道を作らない。
- **人:** 再送 / 保留の判断と理由・証拠を残す。文書 / Evidenceの判断記録を初回候補とするが、必要DB Auditや送信前commitの代替にはしない。

**non-Web認可は依然として残る核心である。** 管理台帳を後続へ分けても、CLIのuser ID自己申告は許可証明にならない。前段の「認証済みWeb要求→IdentityQuery再確認」は成立候補の一つとして残すが、4種recordや状態管理一式の採用とは分離する。必要なら、認証済みの許可内容を1件だけ安全に引き渡す最小構造を比較する。許可の真正性・対象拘束・有効性・競合 / 再使用防止が必要な範囲を見定め、保存方式を決める。

操作記録をfileに移すだけで単発性・DB改変保護・副作用一意を保証したとはしない。最小構成でも実行前の条件、当該試行、crash後の不明を区別する情報が必要であり、具体的な型・table数はその能力から決める。既存Modulith / 通知log / Auditを利用できる部分と、Application追加記録が必要な部分を先に照合する。

## 4. Tier・見積・環境・caseの扱い

| 対象 | 今回の修正 |
|---|---|
| notification Tier | Tier 2見直しを推奨の前提から外す。Tier 1開始案も確定しない。最小機能に残る業務状態 / 不変条件を確認し、必要ならTier 2を選ぶ。DB排他処理があることだけでTierを決めない |
| 保存構造 | 前段4種recordは拡張方式の比較材料として保持。初回S1の必須DDLとは扱わない |
| 作業量 | 追加24〜48時間・比較総量200〜392時間は拡張方式の参考値へ戻し、現在の最小案へ適用しない。元176〜344時間も新方式の予算 / 確定残量ではない。最小構成後に再算定 |
| 環境上限 | 4 JVM・接続 / memory等は競合実証の仮値として保持。要求table / claim / 期限30分等の拡張方式固有値は採用前提から外す。実測・資源 / 数値判断は未実施 |
| E01〜21 | 安全性・観測・人の判断という目的を維持する。要求管理一式を前提とした枝は最小方式へ対応付け直す。枝を削除したり、未実演をPASSとしない |
| RV-1〜7 | 未判定。特にRV-2 / RV-4の保存・Tier案を再reviewし、その後にDDL / 依存固定と開始経路を確認 |

## 5. 次の作業順と出口

**後続照合:** [non-Web許可引渡し・対象試行識別案](phase4-s1-minimum-permission-and-attempt-review-20261005.md)で、1件の許可record＋既存publication / 通知log / Auditを優先比較し、Modulith公開APIの署名と実行相関の不足を確認した。許可DB保護・実executor相関・確認終了の起動条件は未成立で、Tier判断を先行しない。

1. 必須能力ごとに既存契約・sourceで成立する部分と追加が必要な部分を照合する。最優先はnon-Webへの信頼できる許可の引渡し、対象試行の識別、不明時の記録である。
2. 最小候補を比較し、必要なApplication記録・認可 / Audit・排他とTooling支援を示す。候補の成立条件・満たせないcaseをOwnerへ提示する。
3. この最小構成に対してnotificationのTier、保存構造・依存、工数 / 環境上限を順番に判断する。
4. RV票・case対応・Gate材料へ反映し、個別開始範囲が成立してから実装 / preflightへ進む。

今回の変更は合意方針と計画資料の修正に限定する。code・Public API・POM・migration・環境起動・remoteは変更していない。

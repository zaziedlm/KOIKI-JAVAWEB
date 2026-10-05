# S1最小候補・初回局所検証開始の引継ぎ（2026-10-05）

**目的:** S1の文書検討から、承認済み初回Tooling検証へ進む入口を一本化する。
**状態:** 初回ST-Bのfixture作成・実行はOWNER APPROVED（2026-10-05）。preflightと初回L1〜4はLOCAL PASS（11 / 9 / 8 / 12 tests、各failures / errors / skipped 0）。[Evidence](../architecture/validation/phase4-s1-minimum-local-verification-20261005.md)を参照。初回4 classの局所契約は揃った。正式Reference実装・Tier確定・DoD / Gateは後続判断。
**作業位置:** `docs/daily-development-workflow`。整理開始時HEADは`e75a70a`。本書を含む文書コミット後、再開時に実際のHEADと作業treeを確認する。

## 1. 最初に読む文書と適用範囲

1. Repositoryの`AGENTS.md`と[project overview Skill](../agent/skills/koiki-project-overview/SKILL.md)を読む。業務機能を扱う場合は[business feature Skill](../agent/skills/koiki-business-feature-work/SKILL.md)も読む。
2. [方式採用票・初回局所検証契約](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)を、B1〜6のOwner判定、初回test / path / command / 資源上限の正本として読む。
3. [変更一覧・個別開始範囲](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)をCH / STの対応表として読む。今回承認されたのはST-Bの初回範囲だけで、ST-C〜Eの正式開始へ拡張しない。
4. 必要な論点だけ、[最小構成と将来管理機能の分離](phase4-s1-minimum-demonstration-scope-review-20261005.md)、[成立性確認](phase4-s1-minimum-boundary-feasibility-review-20261005.md)、[Tier・保存／権限・検証順](phase4-s1-minimum-tier-storage-verification-review-20261005.md)へ戻る。

本書は再開の案内であり、上位設計・ADR・既存Gateを改訂しない。承認範囲は方式採用票を優先する。文書中の「次は」「未判定」は作成時点の記述もあるため、現在の開始権限を単独で判断する根拠にしない。

## 2. 現在の最小候補と承認境界

| 項目 | 現在の扱い |
|---|---|
| 保存 | 許可＋append-only消費の2種類を局所検証候補として了解。許可ID一意、対象不変、競合消費、未解決対象への次許可禁止を確認する |
| Tier | 狭いTier 2 RICH / JPA共有モデルを優先review候補として継続。正式確定ではなく、局所JDBC fixtureがJPA / Tier採用を証明するものでもない |
| 人と仕組み | 人がDB・process・provider・log / Auditを総合判断する。仕組みは許可の真正性・対象拘束・一意消費を守り、不明を自動解除・再送可へ変換しない |
| 認可 / Audit | 認証済みWebで許可し、non-Webで真正な許可と現在権限を確認する方向性を了解。permission / scope・失効・Audit分類 / actor・既存Bean接続は残条件 |
| 起動 / 相関 | 目的別modeと実executorへの不変operation伝播を局所検証する。確認用modeは送信機能を起動しない |
| 再送 | JDBC / UPDATE、kkref履歴維持、有限collectionとpredicateによる対象選別を候補として検証。呼出し復帰だけを対象処理・送信完了の証拠にしない |
| 開始許可 | B6により、既存非配布Tooling配下の初回4 test・専用support / resourcesの作成と実行を承認済み |
| 未承認 / 未検証 | 正式Reference code / POM / migration、Framework Public API / Rule、正式Tier、実Web認証・実trace・OS crash・独立provider、DoD / Gate、release / remote |

Owner一人＋Codexで順次・同期的に進める。サブエージェントの利用や追加担当者の配置を前提にしない。21 caseの了解は網羅性・実演PASSの認定ではない。

## 3. 旧資料の読み分け

| 旧記述 | 現在の読み方 |
|---|---|
| [10月2日の引継ぎ](phase4-s1-next-session-handoff-20261002.md)の「作業7を具体化」「実行条件未承認」 | 当時の記録。現在の再開は本書と承認済み方式票から行う |
| [初期scope mapping](phase4-s1-a1-a2-d1-scope-mapping-20261005.md)のnotification Tier 1第一候補 | 当初候補。現在は最小不変条件を踏まえた狭いRICH候補のreview段階。Tier 1 / 2とも正式確定とはしない |
| [保存・依存・環境の拡張案](phase4-s1-request-storage-dependencies-environment-draft-20261005.md)の4種類記録・4 JVM・200〜392時間 | 将来管理機能・拡張方式の比較材料。初回fixtureの必須実装・環境・予算へ転用しない |
| [元WBS](phase4-s1-execution-resources-and-sequence-draft-20261005.md)の176〜344時間 | 当時の低確度概算。現在の確定残量・予算ではない |
| 1種類の許可record＋trigger等の比較 | 代案の検討履歴。初回優先候補は許可＋append-only消費の2種類 |
| 旧RV票・PF案やGate判断案 | 正式統合の残論点として保持。初回局所検証の開始契約はB6と方式票§2〜5を読む |

安全条件とDoDの目的は維持する。過去の本文は理由・代案の履歴として残し、古い機能一覧から将来管理機能を再び必須化しない。

## 4. 次に行う作業

**現在の再開点:** 下記の初回L1〜4とEvidence記録は実施済み。[方式票§7・8](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#7-局所結果の方式票への反映2026-10-05)へ結果を反映済み。次はTier・認可／Audit・正式Reference接続の4論点をOwner reviewし、具体化と追加個別開始票へつなぐ。L2の結果不明はtest入力、L4の復旧runnerはID準備までで、再起動後の不明保全・実復旧実行は残条件。局所PASSを本人認証・正式Audit・実OS crash・Gateへ拡張しない。

1. branch / HEAD / statusと承認票を確認し、初回の対象pathを固定する。
2. Java 21・Wrapper / artifact・Docker接続・端末資源をpreflightする。Maven / Dockerの権限エラーはAGENTS.mdと実行環境の承認手順に従う。
3. L1 `S1PermitStorageTest`から、test専用DDL / role・独立context・必須枝一覧を作成する。既存Source / V1 / V2 / POMは初回変更対象に含めない。
4. 方式票の実効設定・資源上限を確認してL1を実行し、L2競合、L3実executor相関、L4mode組立へ順次進む。L1 / L2不成立のまま送信検証へ進めない。
5. 実行ごとのXML・DB / role / probe・環境条件を保存し、実行日付のvalidation文書へPASS / FAIL / BLOCKED・未検証範囲を記録する。

局所枠は1 test fork / 1 PostgreSQL・有限20 publication。作成16〜32標準時間は低確度の再判断枠で、8時間時点に見直す。詳細のmemory・接続・待機・保存・再試行上限は方式票§5に従う。追加依存・別process・Web / tracer、上限超過が必要なら原因と変更票を示して再判断する。

## 5. 今回のコミット単位

S1方式のsource照合、運用・認可 / Audit・観測条件、拡張案から最小案への絞り込み、Owner承認、現在と履歴の導線を一つの文書コミットにまとめる。内容は`docs/development/`に限定し、実装・test作成 / 実行・環境変更・remoteを含めない。

推奨message: `docs: consolidate S1 minimum scope and approved local verification contract`。
新しいcommit SHAは本文へ先取りせず、再開時にGit履歴から確認する。文書の差分・リンク・承認表示を確認してからcommitする。

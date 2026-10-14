# S1最小候補：Tier・保存／権限・検証手順の整理（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書は候補の設計根拠。狭いRICH候補の継続と初回局所作成／実行は後続方式票でOwner了解済み。正式Tier・DDL採用・全V段階の開始承認ではない。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / blocking review入力。最小構成の分離方針はOwner了解。2種類record・Tier・DDL / 権限・起動 / executor構成・検証実行・開始Gateは未採用。
**Ownership:** Reference `notification`の通知・最小復旧許可、`expense`の承認event。Toolingはstub・故障注入・観測 / Evidence。Framework共通管理機能を追加しない。
**確認baseline:** `docs/daily-development-workflow` / `e75a70a`＋未commit文書。今回は既存source・規約・文書の照合と設計整理。実装・DB / process起動は行っていない。
**入力:** [成立性確認](phase4-s1-minimum-boundary-feasibility-review-20261005.md)、[DB / executor / mode](phase4-s1-permission-db-executor-startup-review-20261005.md)、[最小構成方針](phase4-s1-minimum-demonstration-scope-review-20261005.md)、[E01〜21](phase4-s1-dod-demonstration-plan-20261005.md)、[RV / PF](phase4-s1-preimplementation-review-preflight-estimate-draft-20261005.md)。

## 1. 今回扱う最小構成

通知、認証済み経路からの対象1件許可、non-Web単発復旧、対象試行とprovider / Auditの照合、結果確認を扱う。追加保存は許可とappend-only消費の2種類を比較基準とする。画面付き要求管理、承認workflow、常設実行権 / lease、自動復旧、汎用通知サービスは初回範囲へ戻さない。

人は停止・受理不明・証拠の整合を判断する。Applicationはその判断を受けても、現在権限・期限・対象・競合・記録commitが成立しなければ送信しない。人の判断をboolean状態へ置き換えて自動再送しない。

## 2. notificationのTier判断票

[KOIKI業務Skill](../agent/skills/koiki-business-feature-work/SKILL.md)ではTier 1を開始点とし、複数Entityにまたがる不変条件や複数Use Caseで使う業務ルールがある場合はTier 2を検討する。[Rule 14 source](../../koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java)もSIMPLE moduleへRich Domain packageを置くことを拒否する。table数・非同期・lock・retryがあることだけではTierを決めない。

| 最小候補に残るルール | 性質 | Tierへの影響 |
|---|---|---|
| 同じ許可を一度しか使用しない | 業務上の使用条件。DB一意挿入は技術的実現手段 | DomainまたはApplicationで明示する。単独ではTier 2確定としない |
| 未確認終了の対象へ次の許可を発行しない | 許可間の業務条件。発行と確認で同じ意味を保つ | 共通する業務ルールとしてTier 2検討材料 |
| 消費と取消 / 確認終了を矛盾させない | 許可と消費記録をまたぐ不変条件 | 現2種類候補を業務モデルとして扱う場合、Tier 2検討条件に該当 |
| DB排他・executor相関・flush / timeout | 技術的処理調整 | Tier選択の直接理由にしない。Adapter / Configuration / Applicationに配置 |
| provider不明時の再送 / 保留判断 | 人による総合判断と、Applicationの安全な実行条件 | 複雑な自動判断Domainを作らない。人の判断理由と必要証拠を記録 |

**今回の推奨review:** この最小候補をnotificationの業務責務として保持するなら、狭いTier 2 RICH / JPA共有モデルを優先して確認する。前段の運用管理一式を作るTier 2案とは機能範囲が異なる。必要な許可の操作条件だけをModelへ置き、専用Domain Serviceや将来用抽象を増やさない。

Tier 1を残すには、上記のどこまでが技術記録で、業務上の不変条件をApplicationに単純に集約できるかを説明する。2種類を別々のCRUDとして実装して意味を分散させたり、Tier 1に合わせて再使用 / 未解決対象の安全条件を削ったりしない。Tier 2を避けるためにFramework / Toolingへ業務ルールを移すこともしない。

**判定:** 未判定。Ownerの分離方針了解をTier 2採用へ転用しない。notification module内のTierは統一し、expenseの既存Tier 2・masterの既存Tier 1は変更しない。型名・aggregate境界・module宣言はこの票の採用とモデルreview後に確定する。

## 3. 保存構造・transaction・権限の具体案

### 3.1 論理schemaと制約

| 論理record | field群 / 型の候補 | 制約・保存上の意味 |
|---|---|---|
| 許可 | 許可ID UUID、環境ID、publication UUID、event型 / 不変ID、listener識別、status / attempts / 最終再送時刻snapshot、発行者user ID、理由code・証拠参照、発行 / 期限timestamptz、確認者 / 確認時刻 / 結果参照、技術version bigint | 許可ID PK。必須項目NOT NULL、期限は発行後。確認未終了の環境＋publicationへpartial unique indexを候補。最終再送時刻・確認項目は未成立ならnull |
| 消費 | 許可ID UUID、operation UUID、worker世代、認可確認 / 消費時刻timestamptz | 許可ID PK兼許可recordへの同owner FK、operation UNIQUE、必須項目NOT NULL。再消費はINSERT競合で拒否、INSERT-only |

Reference-owned追加migrationで作り、kkref履歴と適用済みV1〜V3を維持する。publicationはModulith JDBC / UPDATEの別保存、通知logは通常通知の既存設計に従う追加対象。許可からFramework Identity / Auditやregistry tableへのFKを先行追加しない。user ID等はPublic契約の実際の保存表現へ照合し、長さ・digest / ID方式・migration番号・物理名はDDL reviewで決める。通知payload・credential・PIIを許可へ複製しない。

確認終了は「人が結果を解決済みと確認した」事実。通知成功のBooleanとはしない。未消費の取消、送信前拒否 / 未送信確認、配信整合確認をAudit / 結果参照で区別する。消費後crash・受理不明・commit不明は自動確認終了せず、次許可を保留する。

### 3.2 row lockと権限の修正

前段は復旧roleの許可SELECT-onlyと許可row lockを併記した。しかしPostgreSQLの`SELECT FOR UPDATE / SHARE`はSELECTに加えて少なくとも一列のUPDATE権限を必要とする。[PostgreSQL 17公式権限](https://www.postgresql.org/docs/17/ddl-priv.html)

row lockを使う候補では、復旧roleへ**許可の技術version列だけUPDATE権限**を追加し、対象・発行者・理由・期限・確認終了のUPDATEは禁止する。JPA pessimistic lockとversionの実効SQLを確認する。version列の変更権限だけで消費を取り消せることはなく、消費recordの一意性・INSERT-onlyが単発性を守る。versionを本人認証や停止証明へ使わない。

| role | 許可 | 消費 | 周辺資源の条件 |
|---|---|---|---|
| migration / owner | DDL / grant、review済みupgrade | 同左 | runtimeへcredential / owner継承を渡さない |
| 認証済み受付 / 確認Web | SELECT、発行field INSERT、確認field / versionだけUPDATE | 照合用SELECT、INSERT / UPDATE / DELETEなし | 正当なIdentity認証・Session・Audit書込、registryは読取だけ。送信機能なし |
| 復旧non-Web | SELECT＋versionだけUPDATE（row lock用） | SELECT / INSERTのみ | IdentityQuery読取、必要Audit / 通知log / registryの管理更新。許可発行 / 確認終了不可 |
| Tooling閲覧 | 必要な安全項目SELECT | 同左 | 故障注入の管理権限は別credential、通常実行へ流用しない |

table-level権限・PUBLIC・role継承・sequence・schema USAGEを含め点検する。JPAが対象fieldまでUPDATEするSQLを出すなら改変権限を広げず、mapping / 限定Repository操作を直す。純SELECT roleを維持する代案なら、全競合経路で共通のtransaction advisory lock等が必要になるため別reviewとし、今回両方式を混在させない。

### 3.3 transaction境界と保護の限界

発行は認証・認可後に許可と必要Auditをcommitする。消費と確認終了は同じ許可rowを先にlockし、最新の条件を読み、消費INSERTまたは確認終了更新と必要Auditをcommitする。外部送信中に業務row lockを保持しない。復旧者のsession lockと人の通常process停止確認は別条件として維持する。

消費commitが成立した後でだけ標準再送APIへ進む。新しいIdentityQuery照会・期限・snapshot / 停止証拠は送信直前にも照合し、失効後送信や照会鮮度の窓を保証範囲へ記録する。flush成功をcommit成功としない。commit結果不明は保留し、同許可を別operationでやり直さない。

確認roleによる早期終了・発行者偽造の防止は、信頼された認証済みApplication経路とcredential管理の範囲である。消費のDB巻戻し防止と、人の判断の正当性を同一の保証にしない。runtime / migration分離・事前schema確認・既存二階層Flyway順の維持は別reviewで閉じる。

## 4. 検証手順案と停止条件

全段階**未実施**。各段階は個別開始範囲・blocking review・preflightが成立した後に行う。Owner一人＋Codexで順番に進め、独立processの並走は競合case内だけに限定する。以下の合格は段階の技術条件で、OwnerのDoD最終判定とは区別する。

| 順 / 対応 | 手順・操作 | 必須観測と合格条件 / 停止 |
|---|---|---|
| V0：設計・PF-1〜3 | Tier票、DDL / role表、Audit分類、mode / executor、依存・artifactをreview。隔離資源と既存差分を固定 | 未解決の権限 / 対象選定・Gate条件があれば実装 / 実演開始にしない。source・実効依存・checksum・設定をEvidenceへ |
| V1：DB-1 / 2、E01〜03 / E12 / E16 | freshと既存履歴upgrade、用途別roleでJPA / 認証 / Audit起動。許可発行、消費INSERTとcommit / rollback、対象改変・消費UPDATE / DELETEを試す | 消費commit前のprovider送信0、rollbackなら消費も必要Auditも不成立、対象 / actor変更・消費取消し拒否。最小roleで動かない場合はREWORK |
| V2：DB-2、E13 / E16 | barrierで同許可2worker、別許可同対象、取消 / 確認終了と消費の競合を同期。消費commit直後crash・commit応答喪失を別枝にする | 消費recordは許可当たり1、負けたworker送信0、未確認対象へ別許可不可。不明時に消費を消さず人の照合へ。件数だけで旧worker停止としない |
| V3：ST-1、E03 / E12 / E16 | 通常 / 確認Web / non-Webを別processで起動。Bean / route / Web port・設定を採取し、確認Webで未認証・権限外・CSRF拒否と正当確認を試す | non-Webで不要Web Securityなし、確認Webに送信Bean・通常更新routeなし。startup / HTTP後のprovider / registry不変。既存通常Security回帰FAILなら停止 |
| V4：EX-1 / 2、E13 / E20 | 少数の対象外FAILEDと同event別publicationを置き、predicate版で厳密選定。options版はbatch前filterの負例を別途確認。実executor / proxyとoperation伝播を採取 | 対象だけの実listener相関、未選定を保留、欠落 / 別operation送信0、thread再利用時非漏えい、実span / metric受領。markerだけなら未成立 |
| V5：E01〜10 / E15 / E20 | 通常通知成立後、PUBLISHED / PROCESSING / 受理後等の停止位置をToolingで同期。Ownerが停止・providerを照合し許可、復旧と結果確認Webを順次実演 | 同じ通知keyで受理1件と当該publication・通知log / Auditの整合。安全な拒否だけで配信完遂caseの合格にしない。元worker終了と復旧時間を採取 |
| V6：E11〜17 / E21 | 旧worker生存、lock喪失、権限失効、Audit / provider / sink失敗、timeoutを別実演に分ける | 危険な再送0、保留理由・最後のsnapshot・旧worker / 受理不明と次調査を保存。回復後の確認 / 再開または連絡まで人が記録 |
| V7：E18 / E19・W10 | 保持 / パージの個別条件review後に単独・競合を確認。case別Evidence・不足枝と適用条件を整理 | 未完了publication・未確認許可 / 消費・必要証拠を削除しない。DB / log / Audit / trace / provider・人の判断を突合してOwnerの判定へ |

V1〜4は局所契約の検証、V5〜7は固定Reference artifactの実演として重複を区別する。SQL権限拒否はSQLSTATE等とtransaction rollbackを採取し、例外文字列だけを根拠にしない。競合はbarrierとprocess世代で再現し、任意sleepや送信件数のみで排他成立としない。

source SHA・JAR / image / 設定identity、時刻・時計差、許可 / operation / publication / listener・通知key、role（secret非表示）、前後snapshot、Audit / log / 実metric / trace、Owner判断・追加枝を記録する。必要な故障注入はTooling所有に限定し、Referenceへ恒久failure switchを追加しない。

新fixture名・実行command・Evidence出力先は変更対象と実行範囲が決まってから確定する。既存PL2 Toolingの実行許可を新Reference実装 / container全体へ自動で広げない。環境数値・待機時間・回数は仮値を実測・reviewして設定し、DoDの許容復旧時間と混同しない。

## 5. review票への戻しと次の出口

**統合先:** [採用候補・残条件・変更一覧と個別開始範囲](phase4-s1-minimum-change-and-start-scope-draft-20261005.md)へK1〜6・CH-01〜08・ST-A〜Eとしてまとめた。Tooling局所検証と正式実装を区別し、本資料のTier・DDL / 権限・全検証の未判定 / 未実施状態は維持する。

| 判断票 | 今回用意した具体入力 | 判定 |
|---|---|---|
| Tier | 最小候補に残る不変条件と、狭いRICH / JPA共有モデルの優先review。SIMPLE維持なら配置・単純性を説明 | 未判定 |
| RV-2 / RV-4 | 2種類record・制約 / row lock・修正権限表、JPA / migration適合 | 未判定 |
| RV-1 / RV-5 | 認証済み発行・現在権限 / scope、必要Auditのcommit・結果不明保全 | permission / scope・分類未確定 |
| RV-3 / RV-6 | 目的別Bean境界、predicate / options使い分け、実executor・context / trace | 実動未確認 |
| RV-7 | V0〜7と元E01〜21の対応、DoD完遂と拒否 / 保留の区別 | Gate・開始 / 合格数値未確定 |

次はこのTier判断・保存 / 権限案・検証順をreviewし、採用候補と残条件を絞って実装前の変更一覧と個別開始範囲へまとめる。工数は2種類記録・起動境界 / 相関の採用条件が揃ってから再算定し、前段の拡張方式200〜392時間を流用しない。今回code・POM・migration SQL・DB role・test実行・環境起動・remoteは変更していない。

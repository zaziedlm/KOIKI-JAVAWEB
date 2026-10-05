# S1最小候補：採用候補・残条件・変更一覧と個別開始範囲（2026-10-05）

**状態:** DRAFT / 統合票。下記はレビュー提示時の候補・残条件。2026-10-05に局所検証候補とST-B初回fixture作成・実行がOwner承認済みとなった。現在の個別判定は[方式採用票§1.1](phase4-s1-method-ballot-and-local-verification-contract-20261005.md#11-owner判定記録2026-10-05)を正本とする。正式Tier・Reference実装・Gateは後続判断。
**合意済み方針:** S1実証に必要な最小構成と将来の運用管理機能を分離し、Tierは最小構成後に判断する。人の総合判断とOwner一人＋Codexの順次協働を維持する。
**baseline:** `docs/daily-development-workflow` / `e75a70a`＋未commit設計文書。文書・source照合のみ。新しいtest / DB / process実行は未実施。
**入力:** [Tier・保存 / 権限・V0〜7](phase4-s1-minimum-tier-storage-verification-review-20261005.md)、[成立性確認](phase4-s1-minimum-boundary-feasibility-review-20261005.md)、[初回対象・Gate / J1〜8](phase4-initial-scope-gate-and-open-decisions-draft-20261005.md)、[RV票](phase4-s1-preimplementation-review-preflight-estimate-draft-20261005.md)。

## 1. 一つに絞る推奨候補

認証済みの限定Web経路が対象1件の許可を発行し、専用non-Web processが現在権限・停止証拠・対象snapshotを確認して一意な消費記録をcommit、その後に標準APIで対象publicationだけを復旧する。結果確認は送信機能を持たない限定Webと既存Audit / 通知log・Tooling Evidenceを使い、人が解決 / 保留を判断する。

| 判断ID | 絞り込む候補 | 残条件 / 判定 |
|---|---|---|
| K1 最小保存 | Reference-owned許可＋append-only消費の2種類、JPA。消費は許可ID一意INSERT。未確認対象の次許可を禁止 | field / 制約・row lockとversion列権限・確認終了・fresh / upgrade review。未判定 |
| K2 Tier | notificationに残る不変条件を狭いRICH / JPA共有モデルへ集約する優先候補 | aggregate境界・発行 / 消費 / 確認の責務を確認。管理workflowを増やさない。SIMPLE代案の条件は前段票を保持。未判定 |
| K3 主体 / Audit | Web本人認証＋現在permission / scope、non-Webは真正な許可とIdentityQuery再確認。依頼者とworkerを区別 | permission code・scope / 失効適用時点、Business / Security分類・actor / resource、必要commit。未判定 |
| K4 起動 / 相関 | 同一JARで通常・限定Web・単発non-Webを分離。実executorへ不変operation context＋標準traceを伝播 | Web Security条件・scan / Bean / route境界、実proxy・decorator・flush。未実証 |
| K5 publication / 通知 | Modulith JDBC / UPDATE、kkref履歴維持。有限の隔離データでpredicate版を優先比較し、同じ通知keyでstub送信 | 対象状態の適用範囲・候補取得量・listener識別 / 今回試行、実効依存・Rule / Level互換、provider key契約。未判定 |
| K6 実施 / Gate | 局所検証を先に行い、その証拠とreviewを経てReference実装・E01〜21実演へ進む案 | Tooling局所開始と正式開始を個別判断。J1 / J4 / J7 / J8、OR・CP-F0 / P4-F / blocking review整合。未判定 |

1 record＋trigger、署名token / file、直接nonHTTP認証、options版再送は代案として保持し、初回同時実装しない。優先候補が成立しない場合に理由と変更量を示して戻る。4種管理台帳、汎用復旧Starter、承認画面、常設lease / 自動復旧は初回対象から外す。

## 2. 変更一覧

下表は変更対象の一覧であり、code / migrationの生成指示ではない。新規型・package末尾・property / route名・DDL番号・正式API signatureは各review後に固定する。

| ID / Owner | 対象path・変更内容 | 前置条件と必要証拠 |
|---|---|---|
| CH-01 Reference | `koiki-reference-app/.../notification/`新規業務package候補。通知、許可発行 / 消費 / 確認Use Case、2種類recordとRepository / Adapter | K1〜3 / A1・A2 review。Domain / Application契約、JPA実DB、現在権限・Audit・競合・不明保全 |
| CH-02 Reference | `koiki-reference-app/src/main/resources/db/migration/kkref/`追加migration候補、用途別role / grantの環境手順 | K1、既存履歴維持・二階層順、fresh / upgrade、role継承 / 列権限・消費取消し拒否。role作成を自動でmigrationへ混在させるとは決めない |
| CH-03 Reference | `ReferenceApplication.java`、`identity/configuration/ReferenceSecurityConfiguration.java`等と必要なscan / Configuration。通常・限定Web・non-Web境界 | K3 / K4。通常回帰、非Web HttpSecurity依存なし、確認routeの本人認証・権限 / CSRF・default deny、送信Bean不在 |
| CH-04 Reference | 復旧runner・標準再送接続・対象読取Adapter、Application-ownedcontext / executor構成 | K1 / K3〜5。消費commit後のみ送信、厳密対象・未選定、当該試行・worker / provider / Audit相関、context非漏えい |
| CH-05 Reference | `expense`の承認event発行と`notification`の受信 / provider Adapter / 通知log | A1基盤の証拠とA2 review後。承認・Audit / publication整合、同期veto回帰、stable key・外部受理不明 |
| CH-06 Reference / Tooling | Reference POM・観測設定の必要最小追加、非配布collector / stub / 採取・alert / 故障注入 | K5 / D1・依存review、effective POM / tree・固定version / image、実metric / trace・終了採取・key / 通信境界 |
| CH-07 Framework / Reference test | `koiki-archunit-rules/`の明示Level選択候補とReference architecture test | Public API / 互換・scopeの独立review、現行Level 0 / 1拒否維持、Level 2正負例・必要互換チェック。Rule 28単純除外をしない |
| CH-08 Tooling / docs | `build-support/`内の非配布局所fixture・実演harnessと`docs/architecture/validation/`のEvidence、runbook / case対応 | 個別開始範囲・資源台帳・V0〜7。既存fixtureの知見を利用し、正式成果物へコピー / 自動昇格しない |

Reference・Toolingは正式Framework配布物にしない。BOM管理値は採用承認ではなく、Customer固有実装や実provider接続は追加しない。CH-07のFramework契約変更をCH-01の業務実装了解へ含めない。

## 3. 個別開始範囲案

以下のST番号は本資料の開始範囲ラベルで、既存CP / Gateを新設・改訂する番号ではない。各開始判断には対象commit / path・実行内容・資源・上限・停止条件・Evidence先を付ける。

| 範囲 | 許可対象として提示する作業 | 現在の状態・次へ進む条件 |
|---|---|---|
| ST-A 文書 / read-only | K票・変更一覧・局所検証契約、既存source / API / POM宣言の照合とリンク整備 | **今回の指示範囲で実施。** code / POM / migration・環境起動へ広げない |
| ST-B Tooling局所検証 | 非配布fixtureで2種類recordの権限 / transaction / 競合、標準再送とexecutor相関、mode契約の検証。V1〜4に対応 | **未開始・個別判断待ち。** まず具体的test対象・変更pathと実行command / Docker資源・必要権限・上限を確定。Framework API / Reference production変更を含めない |
| ST-C 正式規約 / 保存・起動基盤 | CH-01〜04 / CH-07のreview済み部分をReference / Frameworkへ実装し、局所回帰・fresh / upgradeを行う | **未開始。** 対象 / Tier採用、必要な正式開始GateとCP・Public API / DDL / Security blocking review成立、ST-B不足の解消。復旧入口は必要条件未成立なら送信拒否 |
| ST-D A2・実観測 / harness | CH-05 / 06 / 08の承認範囲。固定JARをpackageし通常通知・観測を成立させる | **未開始。** A1の安全条件 / EvidenceとA2・D1 review、依存 / stub・sink・環境を固定 |
| ST-E 統合復旧・運用 / 保持実演 | V5〜7、E01〜21・不足枝、人の判断、runbook・DoD判定材料 | **未開始。** ST-C / Dの局所条件、隔離資源・時間 / 回数 / retention等の個別上限、Owner確認稼働、故障注入方法が成立 |

ST-BのPASSはToolingに限定し、Reference実動やP4-F通過とはしない。mode契約fixtureは候補検証であり、正式ReferenceのSecurity / root scan回帰はST-Cで必要。局所fixture用DDLはTooling-ownedで、review済み正式migrationへ自動昇格しない。

個別開始を切る場合も全体安全条件を削らない。保存だけ完成していても、認可・起動境界・Audit・相関が不足した復旧processへ送信を許可しない。実演順はOwner＋Codexで同期的に進め、並走は明示した競合case内に限定する。

## 4. 残条件を開始前 / 実証 / 最終判断へ分類

| 時点 | 閉じる条件 | 残る場合の扱い |
|---|---|---|
| ST-B前 | K候補の検証対象、具体fixture / 資源 / command・権限、branch / 固定sourceとEvidence、局所上限 | 文書とread-only調査まで。既存PL2許可を新fixture全体へ自動適用しない |
| ST-C前 | Tier / 2種類保存・role / lock、permission / scope / 失効、Audit分類・actor、正式Rule / Level・migration / 依存review、J1 / J8開始経路 | 正式実装を先行しない。条件付き採用の必須条件が未成立なら該当範囲停止 |
| ST-D / E前 | A1・A2 / D1の実契約、有限候補上限、provider key / payload / 保持、終了 / flush・alert、端末 / 環境実測と再見積 | 実演を成立扱いにしない。拡張方式200〜392時間・仮環境値を現在予算へ流用しない |
| 実証中 | DB競合 / commit不明、実executor相関・非漏えい、目的別Bean / route、停止 / lock喪失・provider不明・記録失敗の挙動 | FAIL / BLOCKED / 保留を記録し、原因・差分をreviewへ戻す。DB状態だけで再送しない |
| 最終Owner判断 | J4のDoD 4-2 / 4-3解釈・時間条件、E01〜21不足枝、保持・適用条件、CP統合 / 未達 | 拒否 / 保留の成功だけで配信完遂DoDをPASSにしない。production / 実案件 / 配布へ保証を広げない |

permission code、Audit分類、正式Level選択、migration / role物理名、fixture / 実行command、tracer / image固定、環境上限、再見積は未確定のまま残る。全体を一括承認しないために分類したもので、残条件を取り除いた記録ではない。

## 5. Owner判定に提示する単位と直近作業

**具体的な票・初回契約:** [方式採用票・初回局所検証契約](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)にB1〜6とST-B初回の予定4 test・実行command・資源上限をまとめた。初回は既存Tooling / JDBCを利用するtest-only範囲で、正式Web / Security・実trace / 別JVM / 独立providerは後続。2026-10-05に局所候補と初回作成・実行をOwner承認。preflightと初回L1〜4はLOCAL PASS（11 / 9 / 8 / 12 tests、各failures / errors / skipped 0）。次は正式接続・残条件のreview。

1. **方式採用票:** K1〜5の候補・条件・代案への戻り条件を個別に判定する。K2のTierとCH-07のFramework契約変更は独立して確認する。
2. **局所開始票:** ST-Bのfixture変更一覧・必要test / command・資源台帳 / 上限・Evidence先を具体化し、許可対象をreviewできる形にする。今回の資料だけでは作成 / 実行を開始しない。
3. **正式開始票:** J1 / J8・OR・CP-F0 / P4-Fと各blocking reviewを整合し、ST-C以降の対象・停止点・再見積を提示する。Gateを迂回するための小分けにはしない。

採用候補と残条件を絞り、変更一覧・個別開始範囲を統合した。後続の方式採用票レビューにより、局所候補とST-B初回作成・実行は承認済みとなった。正式方式は未採用。ST-B初回のpreflight / L1〜4はLOCAL PASS、ST-C以降は未実施。次は局所結果とTier／認可／Audit・正式Reference接続・追加範囲をreviewする。承認記録時点でcode・POM・migration・DB権限・環境実行・remoteは変更していない。

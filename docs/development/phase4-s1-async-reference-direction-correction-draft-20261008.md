# Phase 4 S1：Reference非同期経路の早期組込みへの軌道修正案（2026-10-08）

状態：`DIRECTION OWNER ADOPTED（2026-10-09、§9）`。B1／B2受入後の点検結果と、Ownerが採用した軌道修正の方向を記録する。本書は進め方の修正であり、Reference Level 2 runtime・依存追加・code／migration・検証の開始承認ではない。B1／B2の受入結果は変更しない。

**入力：** [B1／B2振り返り](phase4-s1-b1-b2-purpose-retrospective-20261008.md)、[S1完遂方針§3](phase4-s1-completion-direction-decision-20261002.md#3-s1で完遂を目指すもの)、[DoD実演計画§3](phase4-s1-dod-demonstration-plan-20261005.md#3-caseとdodの対応)、[段階B契約](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)、[PL2 Evidence](../architecture/validation/phase4-pl2-level2-verification.md)。
**確認source：** `feature/phase4-s1-reference-foundation` / `2493093`。source・文書の読取点検のみで、Maven／Docker検証は実行していない。

## 1. 点検で確認した事実

| 観点 | 確認内容 |
|---|---|
| Referenceの非同期実装 | `koiki-reference-app`にSpring Modulith依存、listener、event publicationはない。S1以降のReference main追加（約1,270行）は、permitの保存・認可・Auditと対象読取の制御面だけである |
| B1／B2の対象 | Toolingの`event_publication`を、Tooling所有のview（`b1_read.target`／`b2_read.frozen_target`）経由で読んでいる。S1出口のexpense→notification publicationはReferenceにまだ存在しない |
| 既存の非同期Evidence | PL2 Tooling（`ProcessCrashRecoveryIT`）は、再起動時の再公開、送信前後の停止、稼働中listenerとの並走、advisory lock喪失後の並走、停止確認付き復旧を実プロセスで示した。送信後停止の重複抑止はstubの冪等key（`ON CONFLICT`）に依存する |
| permitの役割 | permitはDB上で復旧許可を一意化し、認可・Auditを残す。旧プロセスや並走workerの送信そのものは止めない（段階B契約B-C10・D12と整合） |
| B2の保護方式 | 子JVM全停止、書込role NOLOGIN、接続0件確認、60秒受付窓による全停止型である。保護scopeはThreadLocalで呼出しthreadに限られ、非同期の送信（TaskExecutor／別process）へは持ち越せない |

## 2. 判断：何を修正するか

B1／B2はS1の安全基盤として妥当だが、DoD 4-2（停止後の配信完遂）と4-3（重複抑止）の中心実証には未到達である。permit側（consume／close）の深掘りを先行すると、次の問題が残る。

- 復旧対象の契約が、Tooling viewという仮の供給元に依存したまま固まる。
- Reference内でModulith内部tableをどう読むかという設計問題が、後回しのまま残る（他module内部・Modulith内部tableの直接参照を既定にしない方針との整合）。
- 重複抑止の主機構（送信境界の冪等key）の実証が進まない。

そこで次のように進め方を修正する。

> Reference appへ最小の非同期経路を早期に組み込み、正常系から異常系・停止後回復へ段階的に実証する。permit・認可・Audit基盤は、その経路の復旧操作（DoD 4-4）へ後から接続する。

## 3. DoDと担う機構の対応

| DoD | 主に担う機構 | permit基盤の位置づけ |
|---|---|---|
| 4-1 業務確定と通知失敗の分離 | Referenceのevent publication（承認transactionとの分離）、listener失敗時の承認保持 | 関与しない |
| 4-2 停止後の配信完遂 | publication registryの未完了検出と再送経路、停止確認 | 手動復旧を採る場合の実行許可 |
| 4-3 冪等性・重複抑止 | **論理通知ごとの固定key＋provider側の重複拒否**、同key異内容の拒否 | 重複抑止の主機構にはしない |
| 4-4 監視・認可／Audit付き再送 | FAILED／滞留の観測、運用者の照合 | **主な担い手**（issue／consume／close・Audit） |
| 4-5 保持・パージ | 完了publicationの単一実行パージ | 関与しない |
| 4-12 非同期追跡 | event／publication／通知key／Audit／traceの相関 | permit IDを相関項目の1つとして提供 |

## 4. 段階案

各段階の出口で結果をOwnerへ示し、次段階の開始を判断する。件数・資源・時間の上限は各段階の開始判断で決める。

| 段階 | 内容 | 主なcase | 出口 |
|---|---|---|---|
| R0 開始判断 | Reference限定のLevel 2 runtime採用、依存、publication schema／migration、ADR要否、Ownershipを決める | — | Owner承認記録。AGENTS／Skillの「Level 2開始制限」の限定解除差分を同時に提示 |
| R1 正常経路 | expense承認→値だけのevent→notification `@ApplicationModuleListener`→provider stub。論理通知keyを固定し、provider側で同key重複を拒否 | E01、E08の正常枝 | package済みReferenceで承認・publication・stub受理1件・Auditが整合 |
| R2 失敗分離 | provider失敗、承認rollback・拒否時に通知が出ないこと | E02、E03 | 承認保持と失敗の可視化、誤配信なし |
| R3 停止後回復 | 保存後／送信前／受理直後／log保存後の停止と復旧 | E04〜E07、E09、E10 | 受理総数1件で完了整合。不明時はHOLD |
| R4 復旧操作の統制 | 既存permit基盤をR3の復旧経路へ接続し、認可・Audit付き再送と負例を実証 | E11〜E17 | DoD 4-4候補。B-C01〜B-C10・D11／D12の未達を明示 |
| R5 保持・追跡 | パージと非同期追跡 | E18〜E21 | DoD 4-5／4-12候補 |

R1でkeyとprovider側の重複拒否を作り込んでおけば、R3の異常系がそのまま検証できる。R3に入る前に§5の保護モデルを決める。

## 5. R3前に明示判断する事項：復旧時の保護モデル

| モデル | 内容 | B2方式との関係 |
|---|---|---|
| 全停止メンテナンス型 | 関連processを全停止・drainしてから照合・復旧し、再開する | B2の凍結方式を延長できる。許容復旧時間とWeb停止影響をDoD 4-2の解釈として決める |
| 稼働中個別復旧型 | 他の通知を処理しながら対象1件だけを復旧する | 送信境界の冪等keyと、旧workerを拒否するfencing（世代照合等）が必要。B2方式はこの前提にならない |

どちらを採るかで、R3・R4の設計と検証が変わる。B2の実装を理由に全停止型を既定扱いしない。

## 6. 維持する境界

- 非同期runtime・依存はReference（`koiki-reference-app`）に限定する。Framework Starter、Public API、Rules、正式配布物への昇格はS2判断とし、本修正に含めない。
- Phase 2 Security、Identity、Business／Security Audit、既存の同期veto・migration方針を弱めない。
- B1／B2の受入結果と、permit基盤の「通常無効・未接続拒否」を維持する。[JdbcProtectedRecoveryTargetAdapter](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/adapter/outbound/operational/JdbcProtectedRecoveryTargetAdapter.java)のTooling固有protocolは、Reference publicationへの対象接続時に置き換える暫定物として扱う。
- PL2 ToolingのPASSを、Reference DoD PASSへ転記しない。
- remote push／PR／merge、workflow dispatch、snapshot publishは個別承認による。

## 7. reviewの粒度

これまでは段階ごとに、開始票・preflight・訂正承認・固定manifestを詳細に作成してきた。R1以降は次を軽量化の候補としてOwnerと相談する。

- 開始判断は、対象path、検証上限（件数・時間・資源）、停止条件、cleanupの1枚に絞る。
- 回帰は影響範囲に応じて選択し、全件再実行は段階出口だけにする。
- 固定manifestは段階出口（DoD候補提出時）だけに作成する。

安全条件（認可・Audit・秘密非出力・隔離環境）の確認は省略しない。

## 8. Ownerに判断を求める事項

1. 本書§2の軌道修正（Reference非同期経路の早期組込みを、consume／close深掘りより優先）の採否
2. §3の役割分担（4-3は冪等key主軸、permitは4-4主軸）の採否
3. R0開始判断資料の作成着手の可否（実装開始ではない）
4. §5の保護モデルを決める時期（R3開始前を提案）
5. §7のreview軽量化の範囲

本書の作成はworking treeの文書追加だけで、code／test／SQL／POMの変更、Maven／Docker実行、git add／commit／pushは行っていない。

## 9. Ownerによる方針採用（2026-10-09）

Ownerは本書の提案を受け、次の意向を示した。

> 提案を、次回作業からに盛り込み、早期に reference app に、非同期機能、処理を組み込みしながら、正常系、異常系回復に取り組む方向へ起動修正させようと思います

続けて、振り返りと作業引継ぎ文書へ方針見直しを反映し、commit・pushしたうえで次回作業開始へ引き継ぐ方針を指示した。§8の各項目は次のとおり扱う。

| §8の項目 | 扱い |
|---|---|
| 1. 軌道修正の優先順位 | 採用。次回作業の方針とする |
| 2. DoDと機構の役割分担 | 採用。目的対応表の入力とする |
| 3. R0開始判断資料の作成着手 | 採用。次回作業で作成する。R0の開始承認ではない |
| 4. 保護モデルの決定時期 | R3開始前の判断事項として対応表とR0資料に明記する。選択は未決定 |
| 5. review軽量化の範囲 | R0資料で具体案を示し、Ownerが判断する。未決定 |

Reference Level 2 runtime・依存・code／SQL／migration・Maven／Docker検証、AGENTS／Skillの開始制限の変更は、R0資料に対するOwnerの個別判断まで行わない。remote操作は今回指示された当該branchへの通常pushに限り、PR・merge・workflow dispatch・publishを含めない。

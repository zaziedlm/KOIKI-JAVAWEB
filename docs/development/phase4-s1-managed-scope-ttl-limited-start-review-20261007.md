# S1後段：管理設定scope／TTL供給の限定実装開始票案（2026-10-07）

**状態：OWNER APPROVED / source固定・preflight条件付き限定開始（2026-10-07、§8）。** [供給案§8](phase4-s1-managed-scope-ttl-configuration-proposal-20261007.md#8-owner採用承認2026-10-07)に基づく対象・設定・検証・上限を採用済み。文書commit・clean source固定・preflight成立前にcode／test作成・検証を先行しない。

**source：** `feature/phase4-s1-reference-foundation`／`a18a76d`＋後段文書。初回基盤は[検証記録§13](../architecture/validation/phase4-s1-reference-foundation-20261007.md#13-初回限定基盤の差分レビューlocal検証結果のowner承認2026-10-07)でOwner承認済み。実装前にOwner操作または事前確認で文書をcommitし、clean sourceを固定する。

**位置：** 既存Gate P4-Fの初回限定受入後に残したD3／D4のReference運用供給部分。新Gateは設置しない。初回開始承認を後段へ流用せず、本票の個別開始判断を必要とする。実装管理・受入はArchitecture Owner、支援はCodex、サブエージェントなし。

## 1. 限定範囲と到達点

到達点は、管理された明示設定からscope／TTLを供給でき、通常無効・未接続拒否・初回保存／認可／Auditの契約を保つこと。肯定側Application検証は、現在対象・停止／provider証拠をtest所有の有限入力に限定する。正式運用発行・消費・閉鎖や再送の受付は有効化しない。

| 対象候補（すべてReference `notification`内） | 変更の責務 |
|---|---|
| `adapter/outbound/configuration/ManagedRecoveryConfigurationLoader` | 外部UTF-8 JSONを起動時1回読取、64 KiB上限、未知／重複key、schema・tuple・期間・値・manifest一致の検査 |
| 同packageの不変snapshotと`ManagedRecoveryScopeAdapter`／`ManagedRecoveryTtlPolicyAdapter` | 64 grant上限、完全一致scope、期間外拒否、TTL5分／最大10分・設定期間最大30分、残期間不足拒否 |
| `configuration/ManagedRecoveryConfiguration` | foundation有効かつ明示modeの時だけ登録。専用parserを使い、共有ObjectMapperや既存設定を変更しない |
| `application/port/outbound/RecoveryTtlPolicyPort`／`RecoveryPermitService.issue` | 発行時刻取得後の設定期間・期限再検査。既存expiresAt不変とAudit rollback維持 |
| `NotificationFoundationConfiguration` | 管理mode未指定では既存scope／TTL既定拒否を維持。明示modeでBean選択を確実にする |
| notification test・referenceacceptance test harness | 下記新規5 class／40 case候補と初回test有限policyの再検査対応 |
| 開発文書・検証Evidence | 非機密起動manifest、ACL・失効／戻し手順、source／資源／cleanupと結果記録 |

新table／migration／grant変更、ユーザー／permission seed、Web／CLI・worker本人認証／委譲、sender／listener／runner、停止／provider／publication Adapter、Modulith runtime、POM／Framework API／Rules／Security／CI／remoteは対象外。正式設定ファイルや実user IDをJAR・Gitへ含めない。

## 2. 設定選択と検証の具体案

既存 `koiki.reference.notification.foundation.enabled` は維持。追加名は本票の採用候補で、まだsourceにない。

| property候補 | 契約 |
|---|---|
| `koiki.reference.notification.managed-config.mode` | 未指定=`disabled`。`disabled`／`file`以外はfoundation有効時に拒否 |
| 同prefixの `path` | file modeのローカル絶対path。通常無効時は参照しない。URL・classpath・ディレクトリ・symlink等による対象すり替えを許さない方式を検証 |
| 同prefixの `expected-sha256`／`expected-revision`／`environment-id` | 管理者の起動manifestから指定。必須かつ読込内容と一致。snapshot本体を環境変数等で部分上書きしない |

foundation=falseでは管理設定を読まず追加Beanを登録しない。foundation=true＋disabledは現行のUNAVAILABLE／empty。true＋fileの欠落・読取不能・不正は起動失敗とし、他設定へfallbackしない。

ファイルを1回開き、上限付きで得た同一bytesをhashとparseへ使用する。検査後に別内容を再読込しない。厳格な重複key検出は既存Jackson系依存で実現できるか最初に確認する。現Referenceに `tools.jackson.databind.ObjectMapper` の利用はあるが、本用途の実効version・parser APIは未確認。追加依存／downloadが必要なら開始範囲を広げず停止して差分を提示する。

manifestとsnapshotのrevision・hash・environment一致は必要条件。ACL／信頼できる配備経路の成立はpreflightの運用証拠で確認し、hashのみで真正性PASSとしない。秘密をlogや起動commandへ載せず、設定全体・割当user一覧をlogへ出さない。

## 3. 発行直前の期間検査とtransaction

現行 `durationFor(target)` の呼出しとClock取得は別時点なので、期間検査を明示追加する。内部Reference Portの案は `allowsIssuanceAt(target, issuedAt, expiresAt)`。既定はfalseとし、明示policyだけが肯定を返す。test有限policyも明示実装へ更新する。Framework Public APIの変更ではない。

発行順は、現在本人／能力／scope→現在対象→TTL取得→同一ClockからissuedAt→expiresAt計算→policyの発行直前検査→Domain生成→INSERT／flush→Business Audit。再検査はsnapshotのenvironment一致、`validFrom <= issuedAt < validUntil`、正TTL・最大10分、`expiresAt <= validUntil` を検査する。期間跨ぎ・overflow・確認不能は保存前に拒否する。DB精度への切捨てが期限延長をしないことも維持する。

設定は起動後不変とし、permit row lock中のfile／platform／provider I/Oを追加しない。時刻異常検知・分散時計・送信直前再確認は本Adapterだけで保証しない。消費／閉鎖は設定期間外ならscopeで拒否し、target／evidence未接続時は従来どおり拒否する。

## 4. 検証集合案：新規5 class／40 case

case IDは計画用。実装前にmethod対応を記録し、parameterized invocationも含め40件と照合する。肯定側の証拠はtest所有で、運用接続の受入へ昇格しない。

| class候補／case | 内容 |
|---|---|
| `ManagedRecoveryConfigurationLoaderTest`／L01〜L12（12） | L01正常、L02未知schema、L03未知field、L04重複key、L05不正UUID、L06不明能力、L07tuple重複、L08不正期間、L09容量超過、L10grant数超過、L11欠落／読取不能、L12hash／revision／environment不一致 |
| `ManagedRecoveryScopeAdapterTest`／S01〜S08（8） | S01四能力完全一致、S02別user、S03別能力、S04別publication、S05別environment、S06開始直前、S07開始一致、S08終了一致／以後 |
| `ManagedRecoveryTtlPolicyAdapterTest`／T01〜T08（8） | T01採用5分、T02最大10分一致、T03最大超過、T04ゼロ／負／欠落、T05残期間不足、T06expiresAtと設定期限一致、T07発行直前期間跨ぎ、T08overflow／不正計算の拒否 |
| `ManagedRecoveryConfigurationRegistrationTest`／R01〜R06（6） | R01通常無効・file読取0、R02有効disabledの既定拒否、R03file modeのAdapter各1登録、R04不正mode／設定の起動拒否、R05target／evidence未接続拒否、R06既存Bean／Entity維持・新受付／runner不在 |
| `ManagedRecoveryConfigurationDbTest`／D01〜D06（6） | D01実Identity／Audit発行、D02現在Identity失効、D03scope外の保存0・Security Audit、D04Audit失敗rollback、D05期間跨ぎの保存0、D06旧context終了・改版再起動で失効反映／既存permit期限・消費保持 |

複数負例を1case内でassertする項目は枝ごとに確認結果が判別できるよう記録する。必要な追加caseが出たらcoverageを減らさず対応表を更新し、対象／資源上限への影響をOwner判断へ戻す。

初回6 class／55件は有限policy修正後も同じ規則・枝を維持して再検証。既存25 class／99件とpackage済みReference E2E1件は初回baselineと同じ集合・条件で比較する。Architecture2件は既存99件内、重複加算しない。

## 5. 実行資源・作業量・停止条件の採用候補

初回値を自動流用せず、本後段に再提出する。

| 項目 | 本票の候補条件 |
|---|---|
| 開始資源 | available memory8 GiB・disk10 GiB以上、JDK21／Maven／既存artifact／Chromium／Docker到達をpreflightで確認 |
| process／DB | 各JVM heap768 MiB、fork1／reuseForks=false・並列無効。使い捨てPG17同時1、memory1 GiB／CPU1／max_connections16 |
| pool／DB操作 | 既存pool4／idle1、新規用途別pool2／idle0＋管理2、同時connection総計8以内。lock／statement／Spring transaction各10秒、任意JVM処理の強制停止とは区別 |
| 時間 | 新規class各10分、初回55件再検証合計60分、既存25 class＋E2E合計60分、全検証実経過180分以内。cleanupも含む |
| 記録／再実行 | 今回runのraw合計1 GiB以内、同原因・同条件rerun最大1回。失敗・過去証拠を保持し、無条件反復しない |
| 作業量 | code／test／Evidenceは12〜20標準時間候補、10時間相当で確認・20時間相当で停止／再評価。Owner review／環境承認待ちは別枠。見積りは未採用 |
| cleanup | class単位に当該DB／Ryuk／JVM終了確認後だけ次へ。E2Eのbrowser／issuer／child／port／一時log終了も確認。無関係process／既存containerを操作しない |

停止点：既存回帰失敗、通常無効時の設定読取／登録、設定不正の黙示fallback、確認不能で許可、期間延長、秘密出力、ACL不成立、実効資源不足、cleanup不成立、対象外依存／schema／受付等が必要、時間・量上限到達。拒否された権限操作を迂回せず、環境の承認手順を使う。

## 6. OPEN入力と段階別の成立条件

| 入力 | code／隔離test作成前 | 管理設定による実演process起動前 |
|---|---|---|
| 本票・必要正本の整合 | Owner開始判断でscope／TTL単独を明示し、必要ならAGENTS／計画の限定条件改訂を承認・反映 | 同じ承認対象を再確認 |
| source | Owner操作／確認で文書commit・clean固定、開始preflight成立 | 実装commit／JAR／設定版の固定 |
| 管理者・path・ACL | 隔離testの一時path／有限UUIDはtest所有。実担当・実pathはOPENのまま可能 | 実担当・配備先・書込制限・信頼manifest必須 |
| 時計 | test可変Clockで境界を実証。同一端末の時刻前提・異常時停止を明記 | 同期確認手段・許容差／異常判定を採用し、preflightで確認 |
| 対象／本人認証／証拠 | test有限入力だけで検証。productionの未接続拒否を維持 | 現在対象・停止／provider証拠・必要受付／主体の別reviewが成立するまで該当操作不可 |

実設定を配備してApplication操作する段階は本票の自動次工程ではない。D06の再起動・失効検証は隔離test contextの終了／再作成であり、実運用processのdrain・強制終了や即時失効の保証を代替しない。

## 7. 戻し方・承認欄

明示modeをdisabledへ戻して対象processを再起動すると、scope／TTLは既定拒否へ戻る。通常foundation無効も維持する。戻し操作は管理された改版・終了確認を経る。既存permit／消費／Auditを削除せず、down migration・既存履歴改変を使わない。

**Owner判断欄（未判断）：** 対象code／Port再検査／設定名、5 class／40 caseと初回55件・既存99件／E2E、資源・検証時間・作業量・停止条件、必要正本改訂、文書commit後clean固定・preflightを条件とするcode／test作成・隔離検証の個別開始について「承認／条件変更／保留」を記録する。実設定による運用操作、停止／provider接続、通知／復旧／Level2は承認対象に加えない。

今回の開始票具体化はdocsのみ。code／SQL／実設定・環境、Maven／Docker、git add／commit／pushは実行していない。

## 8. Owner承認・条件付き限定開始（2026-10-07）

Ownerは本票を確認し、「設定選択が明白であるので、妥当と判断し、これを承認します」と明示した。変更条件の指定はなく、§1〜7の対象code／Port再検査／設定選択、5 class／40 case候補と初回55件・既存99件／E2E、資源・時間・作業量・停止／戻し条件を提示内容で採用する。§7の未判断記載は承認前の履歴とする。

承認対象はReference-ownedのscope／TTL設定供給、必要なApplication発行直前再検査・test有限policy対応、文書・検証Evidenceと、採用済み隔離testからの検証。文書commit・clean source固定後のpreflight、および成立時の本票対象code／test作成とMaven／隔離Docker検証を条件付きで承認する。必要正本整合はAGENTS.mdへ本限定条件・承認導線を反映する。初回承認の包括流用ではなく、本票の個別承認に基づく。

実担当・実path・実UUID・時計監視／許容差等は§6の段階別OPENを維持する。隔離testでの有限入力を正式配備・運用操作へ昇格しない。停止／provider／publication接続、受付／worker認証、通知／復旧／Level2、Framework変更・依存追加、実設定配備・remoteへ承認を拡張しない。

承認時点では後段文書3件は未commit。Agentは承認記録と正本導線を反映するだけでgit add／commit／pushを行わず、Owner操作または事前確認でcommitされるまで実装・検証を開始しない。source固定後に本票preflightと必要artifact／実効設定を確認し、条件不足・安全条件不成立・対象外差分が必要なら停止して原因と必要差分を提示する。同じ承認範囲の開始許可を再要求しない。

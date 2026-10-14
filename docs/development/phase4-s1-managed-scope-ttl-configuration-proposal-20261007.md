# S1限定実演：管理された明示設定によるscope／TTL供給案（2026-10-07）

**状態：OWNER APPROVED / 限定実演向け供給案を採用（2026-10-07、§8）。** 方式・scope粒度・失効手順・数値候補を限定実演条件として採用済み。時計監視手段・許容差、実担当・配備先・実UUIDはOPEN。設定名・実装／実行開始は別判断。

**source：** `feature/phase4-s1-reference-foundation`／`a18a76d`。先行[運用接続レビュー案](phase4-s1-operational-port-integration-review-draft-20261007.md)は未commit文書。今回もdocsのみ。

**正本入力：** [初回採用判断D3・D4](phase4-s1-reference-foundation-adoption-decisions-20261006.md)、[Owner承認済み初回検証](../architecture/validation/phase4-s1-reference-foundation-20261007.md)、[運用接続レビュー案§2・3・7](phase4-s1-operational-port-integration-review-draft-20261007.md)。OwnershipはReference `notification`。設定管理・配備は実演の運用担当、採用・受入はArchitecture Owner。担当者の実名は未指定。

## 1. 推奨方式と適用範囲

**起動時に、管理者が配備した外部設定を1回だけ検証・読込し、不変snapshotとしてscope／TTL Portへ供給する案**を推奨する。実演中のhot reloadは作らず、変更・失効は対象processの停止と設定改版・再起動で反映する。

設定は起動manifestで承認版・SHA-256を固定し、配備者以外の書込を禁止、runtimeは読取だけとする。hashは内容一致の証拠であり、承認者や配備元の真正性の代替ではない。承認されたmanifestとファイルを一緒に任意改変できる権限をruntimeへ与えない。

publication UUIDを明示列挙し、user UUID・能力・environment・publicationの完全一致だけを許可する。ワイルドカード、全対象許可、group／role由来の暗黙scope、既存expense部門scopeの流用は追加しない。現在のIdentity能力確認は引き続きPublic IdentityQueryで別に行う。

| 比較 | 起動固定の管理設定（推奨） | 永続割当台帳 |
|---|---|---|
| 今回の適合 | 対象・担当・実演時間が有限で、起動／停止を管理できる | 継続稼働中の頻繁な割当／失効が必要な場合 |
| 追加物 | Reference設定読取・scope／TTL Adapterと検証。新tableなし | schema・migration・grant、管理操作と認可／Auditが必要 |
| 失効 | 稼働中snapshotには反映されない。process停止・再起動が必要 | 現在値照会、transaction／cacheの失効窓を設計可能 |
| 採用限界 | 即時オンライン失効、複数nodeの自動同期、実案件運用は保証しない | 初回2tableの範囲外。独立review・開始判断が必要 |

停止による失効を許容できない実演条件なら、推奨案をREWORKとし、台帳等へ戻す。静的設定を「毎回最新の運用割当」と説明しない。

## 2. 設定の単位と内容

1実演environment・1設定版を単位とする外部UTF-8 JSON案。専用設定として読み、通常のSpring property mergeや環境変数による割当／TTLの部分上書きを許さない。既存ライブラリでのparser選択・重複key検出は実装前に確認し、新依存を前提にしない。

| フィールド候補 | 意味・検査 |
|---|---|
| `schemaVersion` | 対応formatを1に限定する案。不明version拒否 |
| `revision` | 管理者が発行する設定版。承認manifestと一致。過去版への戻しも別承認 |
| `environmentId` | 起動manifestが指定した実演環境と完全一致。自己申告だけで環境を信頼しない |
| `validFrom`／`validUntil` | UTC Instant、from < until。使用時も `from <= now < until` を確認 |
| `grants[]` | `userId` UUID、`capability`、`publicationId` UUIDの完全一致tuple。environmentは上記1環境を継承 |
| `permitTtl` | ISO-8601 Duration。正数、レビュー済み上限以下。受付入力で変更しない |

形式の説明用例。UUID・環境・日時・TTLを実演の正式設定へ転用しない。

```json
{
  "schemaVersion": 1,
  "revision": "demo-review-example",
  "environmentId": "reference-demo-example",
  "validFrom": "2026-10-07T00:00:00Z",
  "validUntil": "2026-10-07T00:30:00Z",
  "permitTtl": "PT5M",
  "grants": [
    {
      "userId": "10000000-0000-0000-0000-000000000001",
      "capability": "NOTIFICATION:PERMIT:READ",
      "publicationId": "20000000-0000-0000-0000-000000000001"
    }
  ]
}
```

能力は既存4code（ISSUE／READ／EXECUTE／CLOSE）のみ。必要な操作だけ個別割当し、READだけの割当から更新を派生させない。userIdの掲載は本人認証の代わりにならず、permission seedやユーザー作成を本案で開始しない。

未知field・JSON key重複・tuple重複・不正UUID・空文字・不明能力・期間不正は起動時拒否。入力上限候補は64 KiB／64 grantで、超過は切捨てず拒否する。上限の採用もOwner判断に含める。設定内容全体やuser一覧をlogへ出さず、revision・hash・検証結果だけを記録する。

## 3. TTL・設定期限・時計

限定実演用の**提案値は発行TTL 5分、許容最大TTL 10分、設定有効期間最大30分**。短い一連の操作を想定した候補で、実測やprovider保証に基づく正式運用値ではない。採用判断で変更可能。実演所要時間が収まらない場合は原因を確認して再判断し、無期限化しない。

最大TTL・最大設定期間は設定自身が任意に引き上げられない、レビュー済み実装側の上限とする案。policyは `0 < permitTtl <= 最大TTL` と、`now + permitTtl <= validUntil` を満たす場合だけDurationを返す。残り期間不足は発行拒否とし、暗黙の短縮や期限延長をしない。

発行済みpermitのexpiresAtは不変。設定期限が先に到来した場合、scopeはUNAVAILABLEとなり、permitが未失効でも操作を拒否する。設定変更で既存permitを伸長せず、消費／未閉鎖記録を保持する。

Clockの参照は既存ApplicationとAdapterで同じ注入Clockを使う案。Scope確認から発行時刻取得までに設定期限を跨ぐ可能性があるため、発行直前に設定期間と計算後expiresAtを再検査する必要がある。現行 `durationFor(target)` のままで足りるか、Applicationに検証を追加するかを開始票に明示する。

限定実演は同一端末・同一実行環境のUTC時計を前提候補とする。時刻同期の健全性をpreflightで記録し、実演中に逆行／時計変更を認識したら停止する。許容時計差の数値と監視手段はOPENで、この案だけで分散processの同期・安全な時刻判定を保証しない。provider／停止証拠の期限検査は別接続で必要。

## 4. Portの判定と条件付き登録

| 状況 | Scope Port | TTL Port／起動 |
|---|---|---|
| 有効snapshot・environment一致・完全一致grant | ALLOWED（Identity能力確認は別途必須） | 期間・上限成立ならTTLを返す |
| 有効snapshotだがgrantなし／別environment | OUTSIDE | 別environmentやscope外へpolicyを流用しない |
| 設定期間外／明示停止状態 | UNAVAILABLE | empty、必要操作拒否 |
| 設定供給mode未指定 | 既存UNAVAILABLE Adapterを維持 | 既存emptyを維持。通常起動は影響させない |
| 設定供給mode明示・file欠落／不正／manifest不一致 | 成立しない | 追加構成の起動失敗。黙って別設定／既定値へ戻さない |

設定供給の明示選択に対応するproperty名・mode名は開始票で採用する。foundation.enabled=falseの通常構成では外部設定を読まず、追加Adapterを登録しない。foundation.enabled=trueだけで設定供給を暗黙有効化しない。

起動後はsnapshotを不変とするため、file削除／変更は稼働中の失効手段にならない。運用手順で改版前停止を必須とし、変更後hashが一致しただけで旧processの権限が消えたと認定しない。

現在対象と証拠のPortは引き続き未接続拒否。scope／TTL供給が成立しても運用発行・消費・閉鎖の全条件成立や送信可にはならない。

## 5. 配備・改版・失効の手順案

1. 管理者が実演環境・認証済みuser ID・対象publication・必要能力・実演時間を確認し、設定版と承認manifestを作成する。担当者・取得元と判断理由を記録する。
2. runtime外の管理経路で設定を配備し、読取／書込ACL、承認revision・SHA-256・有効期間を確認する。設定にpassword／HMAC／provider credentialを含めない。
3. 対象processと起動manifestを結び、設定を検証して起動する。全関連processで同じ承認版が使われることを確認する。限定実演では管理対象processを明示列挙し、未把握processがあれば開始しない。
4. grant変更・失効時は新規操作受付を停止し、対象processを停止・drainして終了を確認する。旧snapshotの終了前に新割当を有効と説明しない。進行中transactionの確定結果を照合し、不明なら保留する。
5. 設定改版・新manifestの確認後、必要なら再起動する。戻す場合も同じ手順。permit／消費を削除したり、未消費に戻したりしない。

失効完了の基準は全対象processの終了確認、または設定期限後に全必要操作が拒否されることの確認。停止要求から終了までの失効所要時間は未測定であり、即時失効とは呼ばない。停止・drainのtimeout／強制終了条件は後段開始票で採用する。

このprocess停止は設定snapshotの失効手段であり、元通知listenerの停止真正性やprovider受理の証明ではない。送信中の外部副作用を取消す保証へ拡張しない。

## 6. 検証ケース候補

| 集合 | 確認する枝 |
|---|---|
| 設定読取 | 正常、不明version／field、key／tuple重複、不正UUID／能力、上限超過、欠落・読取拒否、revision／hash／environment不一致 |
| scope | 4能力個別、user／publication／environment差異、期間直前／一致／直後、Identity無効／失効、未割当でも存在情報非露出 |
| TTL | 正常5分候補、上限10分候補の一致／超過、0／負／欠落、残期間不足、発行直前の設定期限跨ぎ、DB精度境界 |
| snapshot | 起動後のfile改変で権限が自動変更されないこと、旧process終了→改版→再起動で失効／新割当反映、設定期限後拒否、既存permit期限不変 |
| 登録 | 通常無効でfile参照／Adapter登録なし、mode未指定で既定拒否、明示modeの不正設定は起動拒否、target／evidence未接続拒否維持 |
| 実DB／既存保護 | 実Identity／Audit、scope外で保存なし、拒否記録、発行失敗rollback、記録保持、既存25 class／package済みE2Eとの比較 |

後段case数・class対応・資源／時間／接続／cleanup・停止・再実行上限は限定開始票で採用する。本候補を実行済みとせず、初回55件のPASSや予算を流用しない。設定読取は起動時のみとして、permit lock中にfile I/Oや外部サービス通信を加えない。

## 7. 採用判断と次の作業

| 判断項目 | 推奨案 | 現在 |
|---|---|---|
| 方式 | 外部管理設定・起動固定snapshot・hot reloadなし | 未採用 |
| scope粒度 | user×能力×environment×publication完全一致、明示列挙 | 未採用 |
| 変更／失効 | 全対象process停止・改版・再起動、実行中結果照合 | 未採用 |
| 数値 | TTL5分／最大10分／設定期間最大30分、64 KiB／64 grant | 限定実演の提案値、未採用 |
| 時刻 | 同一環境UTC、発行直前再検査、時刻異常時停止 | 未採用。監視手段・許容差OPEN |
| 管理 | 承認manifestとACL、改版理由・終了確認の記録 | 未採用。実担当・配備先・実UUID OPEN |

次はOwnerが方式・粒度・失効・数値候補を採用／条件変更／保留として判断する。採用後、scope／TTL Adapterだけの先行実装可否を必要正本・既存CP／Gateと照合し、対象path・設定選択・発行再検査・検証／上限・戻し方を限定開始票へまとめる。停止／provider証拠・受付／worker認証／sender／listener／runner・Modulith接続は別reviewを維持する。

本書の作成ではcode／SQL／実設定・環境を変更せず、Maven／Docker、git add／commit／pushを実行していない。

## 8. Owner採用承認（2026-10-07）

Ownerは骨子・採用と実装開始の区分を確認し、「採用案の承認し、限定実装開始票の具体化へ進めます」と明示した。§7の推奨案を変更条件なしで限定実演向けに採用する。§7の未採用記載は提出時の履歴とし、現在の採用状態は本節を正本とする。

起動固定snapshot、完全一致scope、停止・改版・再起動による失効、TTL5分／最大10分／設定期間最大30分、64 KiB／64 grant、同一環境UTCと発行直前再検査、管理manifest／ACL・改版理由・終了確認を採用対象とする。実案件の正式運用値・即時オンライン失効・停止／provider証拠の保証へ拡張しない。OPEN入力は承認により取得済みとは扱わない。

次の提出物は[scope／TTL供給の限定実装開始票案](phase4-s1-managed-scope-ttl-limited-start-review-20261007.md)。本採用は開始票の具体化を承認し、code／test作成・Maven／Docker実行、commit／remoteは別判断とする。

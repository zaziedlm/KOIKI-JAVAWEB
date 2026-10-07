# S1段階B：Tooling供給元・限定環境・管理責任の具体化（2026-10-08）

**状態：供給元の方向はOWNER SELECTED、接続方式・補強差分・実行開始は未判断。** Ownerは、Repository内で整備済みのToolingを今回の供給元として採用する意向を示した。本書はその選択を、Reference限定実演の接続対象として具体化する。実案件のシステム連携・外部provider選定は今回の開始条件にしない。

**確認source：** `feature/phase4-s1-reference-foundation` / `08f47402e470d72dc8538452f5766331bb4dc323`。本確認時点には段階B承認文書の未commit差分2件がある。Maven／Docker／process起動・外部通信は行わず、現行sourceを読取り確認した。環境の到達性・cache・空き資源は今回再検証していない。

**入力：** [段階B契約・限定範囲§9](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md#9-owner承認網羅性懸念の保持2026-10-08)、[段階Aの結果](../architecture/validation/phase4-s1-operational-evidence-harness-20261007.md)。契約の網羅性懸念、D11／D12の未達と個別開始判断を保持する。

## 1. 「実運用供給元」の適用範囲を明確にする

本段階Bで接続するのは、**Tooling所有の隔離process・DB・provider stubから、実際に値を取得するReference限定実演の供給元**。段階Aの同一process内modelから、実process・DB境界と取得失敗・保持を検証する接続へ進む候補である。実案件の本番基盤・外部providerの契約取得は後続の別対象とする。

Toolingは非配布・Root Reactor外の所有境界を維持する。既存のsource・SQL・停止確認fileをそのままReference本番source／migrationへ移すことや、既存fixtureのPASSを接続後の安全保証に読み替えることはしない。

Ownerの採用発言は次のとおり。

> リポ内にここまで整備したTooling実装があり、これを 実運用供給元として扱えるということであれば、これを採用します。未定としたのは、実際のシステム連携、機能連携を想像したものでした。

これを受け、Toolingを供給元に使う方向を採用する。現在のToolingは接続素材として利用できるが、§2の不足があるため「そのままでB-C01〜B-C10を満たす」「肯定Portを登録してよい」とは判定しない。具体方式・補強と限定開始票を次にreviewする。

## 2. 接続する既存資産と補強事項

| 接続面／契約ID | 採用する既存資産・sourceの事実 | 接続前に具体化する補強 |
|---|---|---|
| publication／B-C01 | [PL2 migration](../../build-support/phase4-level2-verification/src/main/resources/db/migration/V1__probe_schema.sql)にTooling所有`event_publication`。[ProbeApplication](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ProbeApplication.java)と[NotificationProbe](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/NotificationProbe.java)が通常process・async listenerを構成 | environment・publication・event・listener・attemptの厳密対応、revision／適格性／観測時間・対象変更制御。JSON LIKEやevent件数を最終照合にしない。provider所有の読取契約を設計 |
| 停止／B-C02・B-C10 | [ProcessCrashRecoveryIT](../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/ProcessCrashRecoveryIT.java)が子JVMを起動・終了待機。[ExclusiveRecoveryProbe](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ExclusiveRecoveryProbe.java)は停止確認fileとadvisory lockを利用 | 起動した全processの台帳・起動世代、終了／drainの区別、再起動・移譲抑止の管理期間、制御失効。停止fileはoperator assertionで停止証明ではない。lock喪失pollingの窓も保持 |
| provider／B-C03・B-C04 | [ProviderStub](../../build-support/phase4-level2-verification/src/main/java/org/koikifw/buildsupport/phase4/ProviderStub.java)は別transactionで`probe_provider_send`へ保存。event IDをkeyとする一意登録・冪等性切替を持つ | 現行はsendのみで、受理分類・照会契約・payload／宛先binding・key保持・遅延要求の契約はない。B-1では送信済みfixtureを限定読取する方式を設計し、sendを読取操作から呼ばない。必要補強はTooling内の差分としてreview |
| 証拠保管／B-C05・B-C07 | [OperationalRecoveryTestSupport](../../koiki-reference-app/src/test/java/org/koikifw/referenceacceptance/notification/OperationalRecoveryTestSupport.java)に一意key・本文／sealed原本・失効・provider受理のprocess内map | mapは耐久保管先にできない。Tooling所有の独立保管先を追加する候補。DB／file方式・一意性・ACL・改変防止・失効・保持／復元を比較し、再起動後追跡を実証。既存`target/` rawだけを証拠正本にしない |
| Identity／scope／TTL／B-C06 | Referenceに実Identity・保存／Audit、管理snapshotのscope／TTLがある。既定target／evidenceはempty | 既存本人・能力・scopeと通常無効を維持。管理設定の有限値を動的対象・停止証拠へ流用しない。人の発行・閉鎖とTooling採取主体を分ける |

既存PL2の[README](../../build-support/phase4-level2-verification/README.md)は、通常listenerとの重複実行、lock喪失後の旧JVM継続、停止fileとpollingの限界を明記している。接続時にもこれらを失敗／HOLD条件または未達観測として扱い、肯定testのために省かない。

## 3. 限定環境の候補と隔離単位

| 項目 | 具体化した候補 | 確定・実証を残す項目 |
|---|---|---|
| 端末 | 現在のWindows端末、Repository `C:\Users\kataoka\Desktop\KOIKI-JAVA\KOIKI-JAVAWEB` | 個別開始票のclean source・端末preflight。過去のDocker到達を現在の成立としない |
| 実行対象 | Referenceと`build-support/phase4-level2-verification/`の非配布Tooling | 必要なprofile・登録方式・接続path・class名。既存PL2全suiteを包括的に再実行しない |
| DB | 既存検証の`postgres:17-alpine`を用いる使い捨て隔離DB候補 | Reference保存・Tooling publication／provider・独立証拠保管のDB／schema分離、role／grant、同時container数・connection予算。既存資源上限へ収まるか開始票で再計算 |
| 通信 | 同端末内の限定接続。HTTP方式ならloopback・動的portを候補とする | JDBC／HTTP等の選択、供給元の認証・最小権限、接続timeout・回数、port cleanup。固定URL・credentialは未指定 |
| 対象 | 1環境・1論理通知を初回候補とし、publication／listener／attemptを明示 | 実演時に生成するIDと論理通知keyのbinding、通常経路からの重複・別publication枝。既存test UUIDを本番設定へ転載しない |
| 操作 | B-1の読取先行。採用範囲は段階B§9承認済み | dataset準備・process停止・証拠保存は読取とは別の副作用として開始票に明示。issue／consume／close、復旧resubmit、送信は初回読取に含めない |

Toolingでpublicationを作るためのfixture起動・イベント発行やprovider stubへの送信は準備操作にも副作用がある。既存実装があるという理由だけで実行せず、fixture準備・終了手順と許可対象を限定開始票に含める。ReferenceへのModulith runtime／publication schema追加は、このTooling供給元選択には含まれない。

## 4. 管理責任の具体化案

現在のOwner＋Agentで進める開発形態を踏まえ、次の分担を提示する。役割の所有境界と、OS／DBの実操作権限を区別する。実担当の兼務・操作委任は開始票で確認する。

| 役割 | 担当案／現在確認できること | 判断・作業責任 |
|---|---|---|
| 方式・範囲・結果の判断 | Architecture Owner（今回の承認者） | 残存リスク、追加差分、開始条件、結果受入。網羅性を保証済みにしない |
| 限定環境・停止／再開の管理 | Owner兼務を候補。実操作の担当確定は未成立 | 全process把握、再起動抑止期間、停止証拠、復帰・cleanup。通常運用や無関係containerを操作しない |
| Tooling供給契約・補強実装 | Tooling-owned。Agentは個別開始承認後に実装・検証を支援 | 信頼元・世代・一意性・取得不能時拒否、fixture隔離、sanitized Evidence。無承認の供給元操作を行わない |
| 証拠保管・参照権限 | Tooling-owned、管理担当は環境担当との兼務候補 | 採取／本文登録／失効／閲覧のrole分離、保持・復元、改変検知。ローカル管理者の能力も限界として記録 |
| UNKNOWN時の人の照合 | Ownerによる限定実演の判断を候補 | process・DB・provider・Audit照合、HOLD・調査・理由・次処置。Agentが配送済み／再送可を推測しない |

新しい社内部署・platform管理者・外部provider担当の実在を仮定しない。Repository内のTooling所有者という設計上の区分だけで、担当者の認証や停止制御権限が成立したとは扱わない。

## 5. B-1の接続方式を具体化する順序

1. Tooling側のpublication・provider観測に対し、正式な供給元読取契約と厳密な識別を設計する。既存内部table／SQLへReferenceが無条件に直結する方式を既定にしない。
2. 同端末のprocess台帳・世代・停止確認と、再起動／移譲を制御する期間・担当を具体化する。B-1読取で観測する内容と、B-2肯定に必要な保護を分ける。
3. 独立証拠保管をDB／file候補から比較する。一意登録・再起動後追跡・ACL・失効照会・保管失敗の拒否が成立する方式を採用する。
4. 既存資産の変更path・新規契約test・fixture準備副作用・検証上限・cleanupを限定開始票へ落とし込み、必要正本整合と開始承認を判断する。

ReferenceからToolingのJava実装／Maven artifactへ依存させたり、Root ReactorへToolingを入れたりしない。接続境界のJava型・protocol・property・schemaは今回先行固定しない。供給元選択は済んでおり、同じ方向の採用承認を繰り返し要求しない。補強方式と実行範囲の判断を次の対象とする。

## 6. 未達・追加洗い出しと今回の差分

実案件のprovider・本番platform・分散環境の保証は対象外として保持する。今回の限定実演については、供給元の選択は進んだが、厳密target読取・停止継続・provider照会／binding・独立証拠保管・時計／I/O数値・実操作担当・D11／D12対応方式はまだ未達。契約B-C01〜B-C10のどこが実証でき、どこが残るかを検証票で追跡する。

今後判明する不足は根拠source・影響契約ID・拒否条件・追加case・残存リスクとともに記録し、方式・権限・schema／依存・肯定条件・上限の変更が必要なら実行前にreviewへ戻す。

今回の変更は供給元選択・限定環境・責任分担の文書整理のみ。code／test／SQL、Maven／Docker・process・外部通信、git add／commit／pushは操作していない。

## 7. B-1方式・限定開始票の具体化（2026-10-08）

Ownerの「B-1読取接続方式と限定開始票の具体化 へ進めましょう」を受け、[B-1読取接続方式・限定開始票案](phase4-s1-stage-b1-read-connection-limited-start-review-20261008.md)を作成した。供給元所有ビュー＋最小権限JDBC、同DB別schemaの証拠保管、test専用collector／process台帳／reader、新規6 class／52件、fixture準備副作用と個別上限を提出する。

供給元の採用方向は維持するが、具体方式・補強差分・操作委任・実行開始は同開始票で判断する。Reference本体へのAdapter／肯定Port登録はB-2とし、今回の具体化指示で実装・通信・停止操作を開始しない。

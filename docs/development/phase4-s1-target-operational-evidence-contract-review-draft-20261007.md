# S1後段：現在対象・停止／provider証拠の接続契約と限定範囲案（2026-10-07）

**状態：段階Aの方式・限定範囲はOWNER APPROVED（2026-10-08、§12）。** 作成時は接続契約と限定実装範囲の整理であり、後続の限定開始票でtest所有ハーネスの条件付き開始を承認した。段階Bの実運用接続は未承認。

**確認source：** `feature/phase4-s1-reference-foundation` / `6cf029e09d9c1a73550f0ef8e3f8a5996c67390d`。作成前clean。scope／TTL供給の実装・検証は[検証記録§7](../architecture/validation/phase4-s1-managed-scope-ttl-20261007.md#7-実装検証結果のowner承認2026-10-07)でOwner承認済み、Ownerがcommitした。今回の書類作成ではMaven／Docker・運用操作を実行しない。

**位置／Ownership：** Phase 4 S1の個別限定経路の後段設計。接続契約はReference `notification`（既存Tier 2）、有限供給元・provider stub・障害注入はtest／非配布Tooling。Framework、Customer、expense／masterの責務を移さない。既存JPA共有モデル・許可／append-only消費・認可／Auditを維持し、Web／CLI受付や送信処理は含めない。

**入力：** [運用接続レビューO-03〜O-08](phase4-s1-operational-port-integration-review-draft-20261007.md)、[安全条件S-01〜S-11](phase4-s1-safety-and-recovery-conditions-20261005.md)、[初回採用判断D3〜D6](phase4-s1-reference-foundation-adoption-decisions-20261006.md)。本書の判断ID E-01〜E-08は追跡用で、runtime enum・新Gate・propertyではない。

## 1. 結論案と二段階の範囲

scope／TTLの起動固定設定を、そのまま「現在対象」や「元listener停止」の供給元には流用しない。これらは使用までに変化し得るため、取得時点だけでなく使用時点での鮮度・失効・再起動／移譲抑止を確認する契約が必要になる。

推奨する次の単位は、**接続契約を採用した後、test所有の可変供給元と証拠保管ハーネスで、既存Applicationへの照合接続を検証すること**。実供給元が未取得のまま、本番Adapterに有限値や停止booleanを組み込まない。

| 段階 | 対象 | 到達点と限界 |
|---|---|---|
| A：限定接続検証候補 | test所有の現在対象、停止／provider観測、証拠台帳とAdapterを既存Portへ接続。実Identity／JPA／Auditとの照合を実DBで検証 | 対象・期限・世代・失効・保持の契約を実証する。実publication／platform／provider停止・受理の保証にはしない。本番の未接続拒否は維持 |
| B：実運用Adapter接続 | 実在する取得元の仕様・管理責任・認証・保管・timeout等を取得してReference Adapterを設計 | Aの結果と実供給元を入力に別の開始票を作る。publication schema／Modulith runtime、運用配備、worker、送信の開始はさらに対象を明示して判断 |

Aは既存有限fixtureの正例を増やすだけではなく、証拠の取得後失効・参照の差替え・保存障害・対象競合を追加する候補。A／Bとも本書だけで実装開始しない。

## 2. 現行コードの契約と不足

| 現行source | 実装されている確認 | 接続で追加する保証 |
|---|---|---|
| [RecoveryTargetPort](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/port/outbound/RecoveryTargetPort.java)／[RecoveryTarget](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/query/RecoveryTarget.java) | environment・publication・event・listener・expectedAttemptの5項目snapshot | 供給元の所有者、適格状態、revision、観測時刻、失効・削除・競合、使用時までの有効性。現行recordにこれらのfieldはない |
| [RecoveryEvidencePort](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/port/outbound/RecoveryEvidencePort.java) | consumptionはpermit・target・operation・worker世代・evidenceRef、closureはpermit・target・confirmedBy・resultRef | 証拠の真正性、停止制御の継続、provider分類・通知keyとの対応、時刻・失効・保管・参照の不変性 |
| [RecoveryPermitService](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java) | issue／consume／closeで現在対象を照合。consumeは発行者本人・証拠tuple・期限・未消費を確認。closeは証拠tuple照合。消費／閉鎖はpermit lock後にPortを呼ぶ | 外部採取をlock中に直結しない設計、使用中の対象・証拠変化への対応。現在のpermit lockはpublication／通常listenerを保護しない |
| [RecoveryConsumption](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/domain/model/RecoveryConsumption.java)／Business Audit | 消費のpermit・operation・worker世代・時刻を保存、Audit同commit | evidenceRefは保存していない。成功した消費から採用した証拠へ一意に辿る仕組みが必要 |
| [RecoveryPermit.close](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/domain/model/RecoveryPermit.java) | 期限後も人の閉鎖を記録できる。閉鎖は配送成功を意味しない | 未解決対象の保留と次許可の抑止、古いattemptの閉鎖条件を運用側で定義 |

Portが値を返したという事実だけを真正性の証明にしない。Adapterが信頼元から検査を完了した場合だけ、現行の肯定値を返す方針をレビューする。結果のUNKNOWN／HOLD等は本書の運用分類であり、未実装のJava enumではない。

## 3. E-01：現在対象の供給契約案

| 項目 | 契約候補 |
|---|---|
| 所有・取得 | publicationの正式所有者が供給する値を、notification Outbound Adapterから取得。対象Entity／内部Repositoryを他moduleへ公開せず、Modulith内部schemaの直接読取も既定にしない |
| 識別 | 5項目に加え、供給元revision／更新世代、payloadの同一性を判定できる非秘密の識別、観測・有効期限を供給元内部で管理。field名・型は未固定 |
| 適格性 | 状態別の条件を明示。FAILED・stale・経過時間だけでは再送可にしない。現在の対象一致は停止・providerの独立条件を代替しない |
| 鮮度 | 操作時にrevision・失効・削除を再照合。取得不能・古いcache・別環境／別publication／別listener／別attemptは肯定値を返さない |
| 競合 | 照合直後の変更を防止する供給元側の予約／lock／fencing、または停止管理下の不変期間を設計し、変更可能な窓と保護範囲を記録。単なる再読取だけで原子性を保証しない |
| 利用区分 | issueは許可発行対象の適格性、consumeはその時点の実行条件、closeはその許可の解決条件を照合。操作ごとの条件を同じ5項目照合へ押し込めない |

実Referenceにpublication runtime／供給元はないため、実API・table・status一覧を現時点で固定しない。Aでは可変のtest供給元でrevision変更・削除を模擬し、競合がある場合の拒否を示す。実供給元の原子的保護はBの必須残条件。

## 4. E-02：元listener停止と継続制御の契約案

限定実演では、関連する全通常processを停止・drainし、確認から実行終了まで再起動・task移譲を管理する方式を第一候補にする。範囲・可用性影響・再開順を採用前に確認する。対象workerだけを止められる実基盤が得られた場合は比較して選ぶ。

証拠には対象tuple、元process／containerと起動世代、deploy revision、採取主体・信頼元、観測時刻・有効期限、drain／終了結果、別instanceへの移譲状況、再起動抑止の管理世代を結び付ける。PIDだけ、任意boolean、FAILED、復旧worker lockだけを停止証明にしない。

再起動、移譲、制御喪失、別世代、期限到達、確認不能では停止証拠を失効させる。証拠がimmutableでも、その証拠を今使用してよいかは変わるため、失効情報を別に照合する。再起動抑止のleaseを採用する場合、時計差・失効検知窓・制御主体と旧workerを止める仕組みまで必要になる。lease文字列だけをfencing扱いしない。

Aは停止制御の可変modelと拒否を検証するだけで、OS／platformの停止保証は未達。既存containerの停止や設定変更は行わない。

## 5. E-03：provider受理・通知識別の契約案

| 運用分類候補 | 必要な判断と操作 |
|---|---|
| 受理済みを確認 | 安定通知key・payload識別・受理IDを照合。追加送信のための消費証拠には変換しない。配送完了の証明とは区別し、人の結果照合へ進む |
| 未受理／確実な拒否を確認 | providerが定める時点・key・要求範囲で確認。他の遅延要求が後から受理されない条件、停止、重複抑止を併せて確認した場合だけ再要求候補 |
| 受理状態UNKNOWN | 未送信へ変換しない。既定はHOLD・調査／エスカレーション。安全な同key再要求が独立に契約・実証された場合だけ、その別分岐を後続で採用可能 |
| 取得不能／矛盾／key失効 | 追加送信を保留。DB rollback、消費有無、通知log不在、timeoutから未受理を推定しない |

通知keyは同じ論理通知の初回・再送で同じ値を維持し、operation／retry IDを毎回のprovider keyにしない。通知種別・宛先・payload同一性、key保持期間、同key異payload、並行要求、期限後の応答を契約に含める。実値・識別の組成・保持期間はOPEN。

Aのstubは受理前／後の観測、応答喪失、同key同payloadと異payload、保持期限を模擬する。既存Toolingのevent ID一意受理を実provider契約へ自動昇格しない。senderを接続しないため、実ネットワーク配送や送信中fencingのPASSは主張しない。

## 6. E-04／E-05：証拠保管・消費・閉鎖の契約案

証拠は参照先でimmutableに保管し、採取主体・対象・permit・operation・復旧worker世代・停止制御世代・provider観測・観測時刻／有効期限と、判断理由を対応付ける。証拠本文の秘密・payload・provider credentialをAudit／エラーへ露出しない。受付から渡された参照やuser IDだけで信頼元・本人を認定しない。

**消費証拠追跡の推奨候補：** 今回の2 tableを変更せず、独立保管先で`environment + permitId + operationId + workerGeneration`から採用証拠を一意に解決する。証拠を先に確実に保管し、同じkeyへの本文差替えを拒否する。参照が取れない／曖昧／保持不足／改変／失効なら消費を認めない。

証拠先行保存後にDBがrollbackした場合は、未使用証拠として区別し、消費成功を推定しない。DB commit結果不明なら消費を照会・照合し、追加送信しない。別保管先との分散transactionを暗黙に仮定しない。保管先の一意性・保持・改変防止を実現できなければ、Reference記録／migration追加案を独立reviewへ戻す。Aのtest台帳はprocess内の契約検証であり、実保管の耐久性を証明しない。

**閉鎖：** confirmedByは実認証主体と現在のCLOSE能力／scopeに一致させる。resultRefの先で対象・停止／provider照合・判断理由・次の処置を辿れることを要求する。今回の候補では未解決UNKNOWNを閉鎖肯定にしない。未解決のまま次許可を発行できる運用は認めない。

**残る競合（2026-10-08訂正）：** 現行closeも`requireCurrentTarget`を呼ぶため、attempt更新・publication削除後の古い許可は閉じられない。live対象と許可時の歴史対象を区別し、旧許可の解決を照合する別契約／操作区分が必要かを判断する。同じenvironment・publicationの未閉鎖permitはV4の部分unique indexで1件に制限され、消費・期限経過だけでは次許可を発行できない。別publicationでも同じ論理通知となる場合の横断抑止は、この制約だけでは保証しない。Aでは既存保証と現行拒否・未達を分けて記録し、対象比較の緩和や新schemaを先行しない。承認時に同一対象の抑止を未実装と説明した誤りとD11訂正は[開始票§10](phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#10-preflightでのd11前提訂正2026-10-08owner確認待ち)に記録する。

## 7. E-06：transactionと使用時の再照合

1. transaction外で信頼元から証拠を採取し、保管・停止制御を成立させる。遅いplatform／provider通信、file解析をpermit row lockの下で行わない。
2. 現行Applicationの短いtransactionでは実Identity／scope、現在対象、保存済み証拠の対象・期限・失効・制御世代を照合する。既存permit lock、消費一度性、Business Audit同commitを維持する。
3. 後段sender接続時には消費commitを確認し、送信直前にも権限・対象・停止制御・provider条件を再確認する。消費後に条件が失われれば送信せず消費を保持する。照合後から送信中までの変化は停止制御／provider契約／必要なfencingで別途扱う。
4. 外部副作用後のDB／Audit記録失敗は成功とも未送信とも断定せず、照合へ戻す。

現行Serviceの10秒transaction timeoutは外部I/O・全JVMの強制終了保証ではない。Bで保存済み証拠取得がremoteになるなら、明示timeout・取消・接続数・lock保持時間・障害時rollbackを検証する。worker認証／委譲は未接続で、Aも既存の「発行者本人による消費」を維持する。

## 8. E-07：次の限定実装範囲候補

| 対象 | 候補差分 | 今回は含めないもの |
|---|---|---|
| test供給元 | `src/test/java/.../referenceacceptance/notification/`に可変現在対象、immutable証拠＋失効台帳、停止制御model、provider観測stub | 実publication台帳、Modulith内部読取、platform操作、本番有限値 |
| 契約test | `src/test/java/.../reference/notification/`に下表の拒否・競合・追跡test。実DBは既存隔離Harnessを必要最小限拡張 | test UUID／故障注入の本番登録、Framework test artifactへの昇格 |
| 本番source | Aの第一候補では変更なし。現行Portへのtest Adapter接続で契約適合／不足を識別 | 未採用のPort拡張、close比較緩和、schema／grant・API／依存・通常properties変更 |
| 文書 | 接続契約、開始票、local Evidenceと未達の追跡 | 実運用PASS、Phase 4全体／DoD／正式受渡し認定 |

契約を現行Portだけで安全に表現できないことが判明したら、testの肯定を作るために本番契約を変更せず、差分案をOwner reviewへ戻す。Aの成功後も本番target／evidenceは未接続拒否のまま。

## 9. 検証coverage候補と停止点

| 追跡ID | 必須枝の候補 | 判定の要点 |
|---|---|---|
| C01／対象 | 全5項目一致、同event別publication、別listener／attempt／環境、削除、revision変更、古い観測、供給障害、照合中変更 | 更新を拒否。現在値一致だけで実競合抑止をPASSにしない |
| C02／停止 | 生存、別process世代、改変、期限一致、再起動／移譲、制御喪失、復旧lockのみ | 消費肯定を返さない。元listener停止保証はAでは未達 |
| C03／provider | 受理済み、確実な拒否、受理後応答喪失、照会不能、DBとの矛盾、同key異payload、key期限 | UNKNOWNを保持。既定HOLD、同key安全再要求は別採用 |
| C04／証拠 | permit／operation／worker／actor差異、参照不能、本文差替え、失効、先行保存失敗、同key再利用、後日一意参照 | 消費拒否／DB rollback、未使用証拠との区別、証拠追跡 |
| C05／閉鎖 | 照合済み結果、未解決UNKNOWN、確認者差異、古いattempt／削除対象、次permit競合 | 未解決閉鎖拒否。歴史対象閉鎖／複数permit保護の未達を記録 |
| C06／transaction | 同時消費、Audit失敗、証拠確認中失効、消費commit不明のmodel、消費後条件喪失 | append-only保持、成功・送信を推定しない。送信直前の本番保証は未達 |
| C07／既存保護 | 通常無効・未接続拒否、scope／TTL、初回基盤、既存Identity／master／expense／Security、固定JAR E2E | 本番JARへfixture混入なし、既存経路・権限不変 |

この表はcoverage候補で、class数・invocation数・完全性・PASSの認定ではない。開始票で具体methodへ対応付け、新規集合・scope／TTL40件・初回55件・既存99件・E2Eを区別し、重複計数しない。

対象の供給元変更、schema／dependency／本番Port差分の必要性、肯定条件を証明できない競合、権限障害、検証資源／時間上限到達、cleanup失敗では停止して差分を提示する。失敗時に任意booleanや既存設定へのfallbackで続行しない。

## 10. E-08：採用前の判断と開始票への入力

| 判断 | 推奨／OPEN |
|---|---|
| 次の単位 | Aのtest所有限定接続検証を先行候補とし、Bの実供給元接続と分ける。Owner採用は未成立 |
| 停止方式 | 限定実演の関連process停止・drain＋再起動／移譲制御を第一候補。実範囲・責任者・証拠取得元はOPEN |
| provider不明時 | 既定HOLD。同key安全再要求の分岐は契約取得・実証後に別採用。実providerはOPEN |
| 証拠保存 | 一意tupleから独立immutable保管へ辿る候補。保持期間、管理者、改変検知、失効照合、耐久性はOPEN |
| 現行契約の不足 | 古い対象の閉鎖、同一通知の次permit抑止、対象／証拠の原子的保護は接続前に判断。Aで現行不足を可視化しBを開始しない |
| 数値・供給元 | 証拠最大寿命、時計差、provider key保持、I/O timeout、実担当・API／schema／platform／保管先は未固定 |

採用後に、限定開始票へ対象path・test集合・main差分の有無・資源／時間／作業量・再実行／raw上限・cleanup・停止／戻し方、必要正本改訂とsource固定／preflightを具体化する。前回scope／TTL開始票の上限を包括的な実行許可として流用しない。

本書は文書だけの整理。git add／commit／pushはAgentが行っていない。次の提出物は、Ownerによる方式・境界の採用判断を反映した限定開始票案。

## 11. Owner確認と次の具体化（2026-10-07）

Ownerは本書を確認し、「次へ進めましょう」と指示した。この確認・継続指示を受け、[段階Aの対象・運用証拠ハーネス限定検証開始票案](phase4-s1-operational-evidence-harness-limited-start-review-20261007.md)を作成した。方式・範囲と具体test集合・検証上限を同票で一括判断できるようにした。本書作成時の未判断記載は履歴として保持し、code／test・環境実行の個別開始承認は同票で別途記録する。実運用接続・送信や実停止操作へ今回の継続指示を拡張しない。

## 12. 段階Aの採用・限定開始承認への対応（2026-10-08）

Ownerは[限定開始票§9](phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#9-owner承認条件付き限定開始2026-10-08)で、段階Aのtest所有ハーネスによる方式・限定範囲と、具体test集合・検証上限・開始条件を承認した。§10等の未判断記載は作成時の履歴とし、今回採用されたtest内の契約と実運用供給元の未達を区別する。段階B、実停止方式／provider／証拠保管の運用採用、歴史対象閉鎖・次permit抑止・原子的保護の解決は未成立。実装・環境実行は同票の文書commit・clean source固定・preflight条件に従う。

**D11訂正の承認（2026-10-08）：** preflightで確認した同一environment・publicationの未閉鎖許可一意制約を§6へ反映し、[開始票§11](phase4-s1-operational-evidence-harness-limited-start-review-20261007.md#11-d11訂正のowner承認2026-10-08)でOwner承認済み。既存の次許可拒否保証を未達と扱わず、歴史対象閉鎖・別publication間の同じ論理通知の横断抑止・照合後競合を区別する。段階Aの対象・48件・上限を維持し、訂正文書commit後のclean source再固定・source／環境差分確認を経て進める。

## 13. 段階A結果の承認・commit固定と段階B追加整理（2026-10-08）

段階Aのtest所有ハーネスと検証結果は[検証記録§10](../architecture/validation/phase4-s1-operational-evidence-harness-20261007.md#10-実装検証結果のowner承認2026-10-08)で`COMPLETE / OWNER APPROVED`。Owner操作により`08f47402e470d72dc8538452f5766331bb4dc323`へ7ファイルをcommitし、clean sourceを確認した。

Ownerの「段階B（実運用接続）の追加契約と限定範囲の整理 へ進めましょう」を受け、[段階Bの追加契約・限定範囲案](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)を作成した。供給元確認・限定読取・肯定操作接続を分け、D11／D12の未達と実供給元のOPEN項目を開始条件へ対応付ける。今回の継続指示は文書整理の範囲で、段階Bの方式採用・Adapter作成・外部通信・実停止・送信の開始承認ではない。

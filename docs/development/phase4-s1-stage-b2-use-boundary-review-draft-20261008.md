# Phase 4 S1 段階B-2 使用境界・操作別接続 review案（2026-10-08）

状態：`OWNER APPROVED — 方式・初回操作範囲（§9）`。B1は `COMPLETE / OWNER APPROVED`。本書はB2の方式・初回操作範囲を判断する資料であり、実装・検証の開始票ではない。

## 1. 今回の作業位置と入力

Ownerの「エビデンス確認しました。次へ進めましょう」を受け、B1受入結果を入力としてB2の設計reviewを具体化した。

- [B1結果受入§24](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#24-b1完了記録のowner承認2026-10-08)：新規52件、選択PL2回帰6件、Reference関連143件、既存99件＋E2E1件の限定結果を受入済み。
- [段階B契約§3〜§4・§7](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)：B-C01〜B-C10、D11／D12、操作別開始と未達保持の採用済み方針。
- [供給元選択](phase4-s1-stage-b-tooling-source-selection-20261008.md)：Toolingの隔離process・publication DB・provider stubを接続素材とする方向は採用済み。
- 本review作成時のsource：branch `feature/phase4-s1-reference-foundation`、HEAD `23379b352bfb9ef261789290a3acf40a6805c7e6`、B1受入sourceは未commit差分だった。後続のlocal commit固定は限定開始票§12に記録。検証script／rawは端末内tmp保管。

Ownershipは、接続先がReference notification（Tier 2 RICH／JPA共有モデル）、供給元と検証制御が非配布Tooling。Framework／Customer／他Reference moduleの責務へ移さない。通常無効・未接続拒否、既存Identity／scope／TTL／Auditを維持する。

## 2. sourceから確認した接続上の不足

| 現source | 確認した動作 | B2で補うべき境界 |
|---|---|---|
| [RecoveryTargetPort](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/port/outbound/RecoveryTargetPort.java) | `Optional<RecoveryTarget>`で現在tupleを返す | revision・観測期限・保護期間・保護所有者を型として返さない。tuple一致だけでは使用中の不変性を示さない |
| [RecoveryEvidencePort](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/port/outbound/RecoveryEvidencePort.java) | consume／close用の識別子と参照を返す | Serviceが証拠の寿命・失効世代・停止継続を独立検査する契約ではない。Adapterが一度確認するだけではD12が残る |
| [RecoveryPermitService](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/application/RecoveryPermitService.java) | issueは現在対象確認後に保存・Audit。consume／closeはpermit lock取得後にPortを呼び、transaction完了まで処理する | issueにも対象確認→保存の間隔がある。consume／closeはlock中に供給元I/Oが入り得る。transaction timeout10秒は外部I/O全停止を保証しない |
| [NotificationFoundationConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/notification/configuration/NotificationFoundationConfiguration.java) | 未接続target／evidenceはempty。条件付き構成 | 肯定Adapterの登録条件・接続設定・秘密供給・失敗時の登録拒否は別reviewが必要 |
| [B1ReadContract](../../build-support/phase4-level2-verification/src/test/java/org/koikifw/buildsupport/phase4/b1fixture/B1ReadContract.java) | revision／binding／provider分類／停止観測／寿命を含むtest-only snapshot | 明記どおり観測契約。Referenceの許可契約へそのまま変換・転載できない。I05の照合後変更は未保護 |

上記は現在のsourceに基づく設計判断。B1の失敗や受入取消ではなく、B1から肯定操作へ進むための追加条件である。

## 3. 操作ごとの採用候補

**推奨：初回は使用境界の実証とissue／readの接続に限定し、consume／closeの肯定接続はその結果を入力に個別判断する。** issueも保存を伴うため、使用中の保護成立前には実行しない。issue成功を再送可・実行許可の成立と扱わない。

| 操作 | 初回候補 | 肯定に必要な条件／残る対象 |
|---|---|---|
| issue | 保護された現在対象を既存Identity・現在ISSUE能力・scope・TTLで保存し、同transactionのBusiness Auditを確認 | 保護開始前にsourceを確認し、最終確認からcommit／rollbackの確定まで対象・bindingが変わらない。保存済みpermitは後続consumeの安全性を保証しない |
| read | 実READ能力・scopeでReferenceの保存結果を照会 | provider結果・配送位置・証拠の健全性をread結果から推定しない |
| consume | 初回は既定empty evidenceによる拒否を維持 | 停止／再起動抑止・provider／key／payload／宛先・証拠失効・現在EXECUTE能力・発行者本人・使用中保護がすべて必要。証拠採取／保存はpermit lock前。採用前は実消費なし |
| close | 初回は既定empty evidenceによる拒否を維持 | 実CLOSE能力・scopeと人の照合、歴史対象／resultRef／provider分類の意味を別判断。D11の現在対象比較を緩和しない |

初回issueの未閉鎖permitは使い捨てDBのtestデータとして保持し、最後に当該containerを破棄する候補。cleanupのために未承認close／任意UPDATEを行わない。この破棄は本番permitの解決手順でも証拠の長期保管方式でもない。

## 4. 使用中保護の比較と推奨案

| 方式 | 評価 | 採用の扱い |
|---|---|---|
| 読取直後に再読取、短TTL、permit lockだけ | 最終確認直後の変更・再起動を止めない。Referenceのpermit lockはToolingのpublication／制御状態を保護しない | 肯定接続の根拠にしない |
| 隔離fixture全体の不変期間を限定環境管理者が制御 | 関連子processを終了し、再起動入口と通常のsource変更入口を閉じた期間内にReference操作を完了する | 初回推奨候補。管理者・同OS利用者への防御や分散fencingは保証外。成立を実証できなければ肯定しない |
| supplier予約／世代＋使用側fencing | 変更を許す実環境の候補。使用先も古い世代を拒否し、保護喪失とcommitの競合を定義する必要がある | 後続比較対象。Port／Service／記録／schema差分の独立reviewが必要 |

推奨候補は単なる「変更しない予定」では成立しない。開始票には次の一連の手順と実装箇所を固定する。

1. 当該runのprocess台帳と起動入口を列挙する。通常listenerを含む全関連子processの終了を待ち、残留・未登録process・終了不明はHOLDとする。
2. 保護期間中の再起動・再配置・source更新を行える主体と経路を列挙する。fixture coordinatorの単独所有、通常writerの権限制限、既存接続／実行中transactionの終了確認を組み合わせる候補とする。REVOKEだけで進行中処理が停止済みとは扱わない。
3. 管理用接続はfixture準備・境界外の失敗注入・cleanupだけに限定する。肯定区間中の管理者変更は保証外として明示し、Agent自身の操作をcoordinator内へ閉じる。排他lockだけで管理者や任意SQLを防いだとは扱わない。
4. 保護成立後に対象tuple・revision・source hash・binding・停止世代・各期限を再検査する。ここからReference transaction完了まで保護する。保護終了をServiceメソッドの本体終了と混同しない。
5. commit／rollbackの確定後に保護を解放する。commit結果不明はHOLDとして実permit／Auditを照会し、自動再issueしない。監視で境界違反を後から検出してもcommit済み操作をrollbackできる保証はない。

保護期限に達しただけで進行中transactionの保護を自動解放しない。証拠／制御の期限切れを保存前拒否として扱えるか、最悪の処理・取消・結果照会時間を含めて保護を継続できるかを具体設計で確認する。既存transaction timeoutや初回のTTL再検査だけで、commit時点までの期限検査が成立したとは扱わない。成立にService／Port差分が必要なら、その差分を先に提出する。

保護を単一coordinatorで実現できる範囲と、Reference側に新しいPort／transaction連携が必要な範囲を開始票作成時に判定する。**使用前の検査だけで保護成立としない。成立不能ならB1読取に留まる。** consumeへ進む際にはprovider遅延要求・再起動入口まで含む保護を別途実証する。

## 5. 接続差分とOwnershipの候補

| 対象 | 提案する範囲 | 開始票で具体化する事項 |
|---|---|---|
| Reference notification Outbound Adapter | supplier-owned読取protocolを解釈し、自moduleのRecoveryTargetPortへ変換。拒否時empty／HOLD | path／class名、field／型・版、鮮度・revision・適格状態、例外・timeout。Tooling Java型／artifactへの依存なし |
| Reference Configuration | 明示選択時だけAdapter登録、通常無効・欠落／不正設定拒否 | property／接続先制限／credential ENV供給／登録失敗。既存管理scope／TTL設定の意味を変えない |
| Reference Application／Port | 初回は既存Serviceの保存・認可・Audit契約維持を優先 | 保護が現契約の外で成立しない場合は必要差分を先にreview。Service変更不要と現時点で断定しない |
| Tooling test-only supplier／coordinator | B1読取・保管資産を参照し、必要最小限の保護と失敗注入を追加 | source変更／起動の入口、role／grantと進行中接続の扱い、保護所有・終了判定。B1既存assertionを弱めない |
| Reference acceptance test／Evidence | 実保存・実Identity・Audit・否定経路を隔離DBで確認 | cross-process接続、有限case、資源実効値、非混入、cleanup |

現時点で新しいJava契約・property・migration／grantを固定しない。ReferenceがTooling内部tableへ無条件直結する案、Tooling helperのReference mainへのコピー、POM／依存・Root Reactor変更は推奨しない。supplier-ownedビュー等の読取protocolを新用途へ適用する場合も、権限と意味の差分をreviewする。

## 6. 検証枝の候補

件数の完全性を認定する一覧ではない。class／method／invocation数と実効資源は個別開始票で確定する。

| 群 | 初回で洗い出す枝 |
|---|---|
| 保護の成立 | 全当該子終了、未登録／残留・世代差異・期限一致・管理所有不明は拒否。通常writer／起動入口の保護中操作が通らない。保護前変更は新状態で再確認 |
| 照合後変更 | 読取→Reference保存の間に通常更新／起動を試みても副作用が起きないこと。Source変更を観測できただけでPASSにしない。制御喪失注入はcommit前拒否が保証できる方式に限定 |
| issue／read | 正常保存とAudit、scope外・能力なし・無効Identity・TTL失効・tuple／revision／binding不一致・UNKNOWN／取得不能の拒否。対象の適格条件をFAILEDだけから導かない |
| transaction | Audit失敗時permit rollback、同一環境／publicationの競合、呼出し元transactionの拒否、commit不明HOLDと照会手順。既存一意制約・append-onlyを維持 |
| consume／close拒否 | evidence未接続のため肯定されず、消費／閉鎖記録が増えない。否定確認を肯定接続検証と計数しない |
| 回帰・artifact | 影響別にB1／選択PL2・Reference関連143・既存99＋package済みE2Eを選ぶ。Architectureは重複計数しない。fixture／秘密のJAR非混入、接続／child／container cleanup |

初回から外す枝は、肯定consume／close、人の歴史閉鎖、別publication横断抑止、外部provider・実送信・worker委譲・runner、Reference Level 2、分散fencing・backup／DR。本書で免除したのではなく、後続の独立判断に残す。

## 7. source固定・予算・preflightへ渡す条件

1. B1受入source・承認文書の固定を先行する。local commitはOwner操作または事前確認。現在の未commit差分・既存handoffを無断で整理／削除しない。
2. 端末内tmpのscript／rawの所在とhashはB1 Evidenceにある。長期・別端末再現には、必要手順のRepository化と証拠保管先の選択を別途具体化する。成功rawだけを残して過去失敗を消さない。
3. B2の開始票はclass／path／SQL／configuration、操作・副作用、role、DB／child同時数、heap／pool／connection・各timeout、集合別時間、標準作業量、raw上限、再実行上限、cleanup／停止条件を確定する。B1上限を新しい接続操作へ自動流用しない。
4. B1の累積約100分／残約140分、作業量約6〜7時間は暫定管理値。B2へ使える確定予算ではない。新しい作業の見積りと不確かさ・cleanup余裕を加え、実行開始前に保守的に再評価する。
5. 文書commit・clean source固定後にbranch／HEAD／hash、offline artifact／profile、Docker・空き資源・当該process／接続先・通常経路隔離を再確認する。環境の権限付き実行要求に従う。

境界違反、制御不明、既存回帰失敗、秘密出力、schema／依存変更の必要、資源・時間・raw上限超過、cleanup不成立では停止し、追加修正／追加実行を自動先行しない。

## 8. 今回のreview対象と次の成果物

今回の判断対象は次の2点。

1. 初回B2を「使用境界の実証＋issue／read」に限定し、consume／closeの肯定接続を後続判断とする操作分割。
2. 限定Tooling環境では「全当該process終了・再起動入口制御・通常writer制限による不変期間」を第一候補とし、成立不能なら肯定操作を開始しない方針。

採用後の成果物は、具体protocol／Adapter／configuration差分、保護手順・role／接続・transaction境界、case一覧と資源・再実行上限を固定したB2限定開始票。方式採用と実装開始を分け、具体差分が揃う前に肯定Adapterを作成・登録しない。

承認を分ける根拠は、[段階B契約§1・§7〜§9](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)の操作別開始・個別開始票の要求と、[B1受入§24](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#24-b1完了記録のowner承認2026-10-08)のB2除外である。Tooling供給元の選択とB1受入を再承認していただくものではない。

今回の作成はreview文書のみ。code／test／SQL・Maven／Docker・process操作・git add／commit／pushを実行していない。Phase 4全体・正式受渡し／DoD・remote開始には拡張しない。

## 9. 方式・初回操作範囲のOwner承認（2026-10-08）

Ownerは§8を確認し「確認、承認します。具体差分と検証上限を限定開始票へ落とし込みへ進めましょう」と明示した。初回B2の使用境界実証＋issue／read、全当該process終了・再起動入口制御・通常writer制限による不変期間を第一候補とする2点を採用する。consume／close肯定は後続判断、保護成立不能時は肯定操作を開始しない条件を保持する。

本承認に従い、[B2限定開始票案](phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md)へ具体path・供給protocol・保護とtransaction境界・有限case・予算・cleanupを落とし込む。今回の承認は方式採用と開始票作成の指示であり、具体code／SQL・隔離process／DB・肯定操作の開始承認ではない。同じ2点の方式採用を再要求しない。

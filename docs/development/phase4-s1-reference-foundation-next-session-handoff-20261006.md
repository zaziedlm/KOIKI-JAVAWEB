# S1 Reference基盤：別端末・次回実装開始への引継ぎ（2026-10-06）

**作業区切り:** 正式開始の承認・計画／Agent guidance反映をcommit済み。preflight・実装・正式検証は未開始。Ownerの指示により、再開は別タイミング・別端末で行う。
**確認済みsource:** branch `feature/phase4-s1-reference-foundation`、commit `371f0245af0f1c4b58f93fbd6287961a502d9b12`（`371f024`）、message `docs: approve S1 Reference foundation limited start and verification contract`。本引継ぎ作成前のworktreeはclean。
**今回行ったこと:** Gitと既存文書の確認、引継ぎ・導線の文書化だけ。別端末の環境・cache・資源は未確認。

## 1. 再開時の入口と承認状態

最初にRepositoryのAGENTS.md、project-overview／business-feature-work正本Skillを読む。その後は次の順で確認する。

| 順 | 文書 | 用途 |
|---|---|---|
| 1 | [初回実行資料§6〜8](phase4-s1-reference-foundation-execution-review-20261006.md#6-初回実行資料のowner採用2026-10-06) | 手順採用・Gate設置／APPROVE LIMITED START・条件付き正式開始の最終承認 |
| 2 | [正式開始票](phase4-s1-reference-foundation-formal-start-review-20261006.md) | 作成対象、責務、登録、migration、検証・上限 |
| 3 | [実行資料§2〜5](phase4-s1-reference-foundation-execution-review-20261006.md#2-新規6-class55-invocationのmethod対応案) | 55件method対応、class単位実行、接続配分、cleanup・F-1〜F-5 |
| 4 | [正本改訂・対話採用記録§6・9](phase4-s1-reference-foundation-canonical-delta-and-start-conditions-20261006.md) | 採用理由・対話履歴・正本反映と後段境界 |

実行資料§8が最終状態である。それ以前の「Gate未成立」「実行開始未判断」「文書未commit」等は各審査時点の履歴であり、再承認事項ではない。`371f024`で文書commit・clean確認は成立したが、別端末のsource確認とpreflightは必要。

Owner承認は端末変更後も同じ対象・条件に適用する。初回code・test・Reference-owned V4・隔離grant setup・検証専用設定の作成とMaven／隔離Docker検証は、source固定・preflight成立後に承認済みとして進める。AGENTSのDocker対象にはReference／E2Eも追加済み。実行環境が権限付き実行を求めた場合は、その承認手順に従う。

## 2. 別端末で最初に確認するsourceと受渡し

1. 対象branchと`371f024`を含む履歴、必要なtracked／新規文書が揃っていることを確認する。本引継ぎの追加commitも渡す場合は、そのcommit IDを再開記録へ追加する。
2. `git status --short`、`git diff --check`で未説明差分がないことを確認し、実際に使うbranch・完全commit ID・OS／端末区分をEvidenceへ記録する。後続commitで再開する場合は`371f024`との差分を確認する。
3. source移送はOwnerが選んだ方式で行う。既存remoteにこのbranch／commitがあるとは仮定しない。今回の開始承認はpush／PR／merge／publishを含まない。
4. `.m2`、Docker image、Chromium、JDK trust store、settings.xml／toolchains.xml、環境変数をGitのsourceと分けて確認する。前端末のpath・local cache・credentialを前提にしない。秘密をRepositoryや引継ぎ文書へ複製しない。

引継ぎ文書と導線は、この作業区切りの追加文書差分としてcommitしてから受け渡す。sourceが揃わない場合、実装を始めず不足差分を確認する。

## 3. 別端末preflightの確認範囲

preflight結果は`docs/architecture/validation/phase4-s1-reference-foundation-<再開日>.md`へ記録する。未実施をPASSにせず、項目ごとの成立／不足と根拠を残す。

| 面 | 確認すること | 不足時の扱い |
|---|---|---|
| source／build | clean source、Root／Reference／独立E2E POM、reactor順序、JDK21 build・JAVA_HOME／toolchains、Wrapper3.3.4／Maven3.9.16 | 端末・source差分を記録。既存Wrapperをbootstrapで再生成して解決しない |
| artifact | Referenceの`-pl koiki-reference-app -am`と独立E2Eのoffline goalで必要な依存・plugin・processor・Parent／BOM座標の有無・source整合 | 不足座標と取得／install案を提示して停止。前端末のBOM-only install許可やTooling準備結果を流用しない |
| Docker | Engine Server応答、使用context／接続先、既存postgres:17-alpine／必要Testcontainers image、使い捨てDB起動の権限、既存container／port | named pipe等の拒否はAGENTSの権限付き手順へ。image取得が必要なら対処差分を提示 |
| browser | E2E POMのPlaywright1.62.0と使用可能な既存Chromium、対象OSのbrowser／native資材 | 前端末のWindows用cacheから別OSの成立を推定しない。Chromium installは今回の開始承認に含まない |
| 資源 | available memory8 GiB／disk10 GiB以上、DB memory1 GiB／CPU1を適用できるDocker設定、JVM heap768 MiBの指定・native／browser余裕 | 空き資源不足・制限不適合で停止。memory4 GiBの旧案へ戻さない |
| 実行条件 | 既存25 class集合、新規6 class／55件の予定対応、classごとの呼出しとDB停止確認、実効pool・connection・timeoutの確認方法 | 未作成の資源設定propertyを指定しただけで制限適用済みにしない |
| 接続・秘密 | local Session／Bearer E2Eの必要設定、source HMAC key等の検証中生成、proxy／証明書／settingsの機密を出力しない方法 | 秘密値をcommand line・report・log／Evidenceへ出さない |
| 証拠・終了 | run別target保管先、XML更新時刻／対象class、sanitized log、当該runのcontainer ID／process／port、上限監視とcleanup | 全container／Javaの一括停止や、必要証拠削除で続行しない |

[artifact整備案](framework-local-artifact-readiness-options-20261006.md)と[前端末の最小実証](../architecture/validation/framework-local-artifact-readiness-20261006.md)は調査入力である。旧Toolingのoffline成功をReference／E2Eの別端末成立へ転記しない。初回Wrapper起動も取得を伴い得るため、既存配布cacheを先に確認する。

preflightの環境・source・offline準備確認が成立してから、承認済み検証専用設定を作成する。その設定の実効値・cleanup成立は作成後に確認する。preflight段階で未実装の制限を「実証済み」とせず、失敗すればそこで停止する。

## 4. 初回実装の順序と最初の確認ポイント

1. **検証専用設定を先に作成する。** 既存Reference DB fixture・独立E2Eに明示選択式の制限を追加する。Parent／BOM／production properties／CIを変更しない。未設定／falseで従来動作、有効時のDB・pool・子JVM制限を確認する。
2. **業務code追加前のbaselineを取得する。** 既存25 classをclassごとに実行し、DB停止確認後に次へ進む。package済みReference JARの主要操作E2Eを実行する。source／検証用変更／JAR hash・XML・資源・cleanupを記録する。
3. **通常無効構成とmigration互換を早い段階で確認する。** 初回notificationのmodel／条件付き登録・最小V4を承認範囲内で作成し、無効時のBean／Entity不在と新旧DB起動を確認する。成立しないままUse Caseや後段へ拡大しない。
4. **保存・認可・Auditを実接続する。** 初回Domain規則、JPA Adapter、Public IdentityQuery／Recorder、Spring管理transaction、用途別grantを実装する。運用scope／TTL／対象証拠が未接続なら操作拒否を維持する。
5. **55件と既存回帰を照合する。** 新規6 class／55件のXML対応、既存25 class＋E2E、通常無効・新旧DB、権限・競合・Audit／DB突合を実証し、結果・未達をOwner受入へ渡す。

基盤の最初の成立リスクは次のとおり。実装で推測を検証し、不成立は差分reviewへ戻す。

| 確認点 | 特に確認する条件 |
|---|---|
| JPA／Repository scan | root scanでも専用Configuration以外の追加Bean・Repositoryが生成されず、無効時metamodelに追加Entityが入らない。既存Identity／master／expense EntityScanを置換しない |
| V4／Flyway履歴 | V4を別locationから同じkkref履歴へ適用した後、通常locationでvalidation・無効起動が成立する。未実証であり、validation無効化・history削除で通さない |
| grant／lock／JPA UPDATE | 不変列へのSQL UPDATEを出さないmapping、閉鎖列／version権限、消費append-only、row lock／version／uniqueが実DBで一致する |
| Audit／接続順序 | Businessと保存が同commit、Audit失敗でrollback。Securityはpermit lockのtransaction終了後に記録し、別transactionの接続も予算内。記録失敗でも拒否維持 |
| 期限の精度 | issued／expiresのmicrosecond切捨て、調整後の不正期間拒否、再読取後も期限一致で拒否。fixture TTL値を正式運用値にしない |
| UNKNOWN／競合 | 消費削除・自動閉鎖・再送なし。期限切れでも未閉鎖対象の次許可を禁止。競合を成功扱いしない |
| testの真正性 | 新規testは実Query／Recorder／DB／transaction。有限scope・Clock・proofはtest限定。旧A／B／C fixture・ORM XML・Security優先chainをproductionへコピーしない |

最初の機能変更は既存Referenceを守る構成の成立に必要な範囲から行う。12標準時間相当の確認点までに、通常無効構成・登録／migrationの状況を提示する。40標準時間相当で未完なら停止して残作業・再見積を示す。

## 5. 採用済みの実行上限と対象外

- 新規6 class／55 invocation。既存25 class＋E2Eはbaseline／変更後で同じ条件・集合を使う。
- DB同時1、memory1 GiB／CPU1／max_connections16。Maven・test・E2E子Referenceの各heap768 MiB。
- 既存回帰／E2E pool最大4／minimum idle1。新規はpermit／consumer／reader各最大2・minimum idle0＋管理最大2、合計8以内。
- 新規DBのlock／SQL／transaction各10秒、新規DB class各10分。既存回帰＋E2Eはbaseline／変更後それぞれ合計60分。
- raw合計1 GiB以内、同じ原因・条件でrerun最大1回。上限到達は安全終了・cleanup・証拠保持後に再判断。
- 初回24〜40標準時間の概算、12時間相当確認／40時間相当停止。経過時間・AI実行時間の約束ではない。

Web／CLI入口・通知送信・listener・復旧runner・publication schema／store・Modulith runtime・Framework API／Rules／依存変更、実運用scope／TTL値・provider／停止真正性・実traceは後段。Phase 4全体開始・CP-F1／2全体完了・DoD／正式受渡し・remoteは認定しない。

## 6. 次回セッション開始文例

> branch feature/phase4-s1-reference-foundation、承認commit 371f024と本引継ぎの追加commitを確認し、clean sourceを固定してください。初回実行資料§8の承認済み範囲で別端末preflightを行い、成立後に検証専用設定→既存baseline→初回Reference保存・認可・Audit基盤の実装・検証へ進めてください。artifact／資源不足・安全条件不成立では停止して必要差分を提示してください。通知・復旧・Level 2接続・remoteは進めません。

再開時に記録する項目：実行source、OS／端末、環境・cache差分、preflight成立／不足、作成した検証設定の実効値、baseline取得状態、最初の登録／migration確認結果、消費した標準作業量の見立てと実経過時間、残量・次の停止点。

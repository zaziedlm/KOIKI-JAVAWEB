# S1最小方式：保護方式・相関経路・起動境界の成立性確認（2026-10-05）

> **現在の参照先（2026-10-05）:** 本書は成立性の調査根拠。候補了解・初回作成／実行は後続方式票で承認済み。実動PASS・正式Tier・Reference実装は未確定。 [最新引継ぎ](phase4-s1-local-verification-start-handoff-20261005.md)／[承認済み方式票](phase4-s1-method-ballot-and-local-verification-contract-20261005.md)。

**状態:** DRAFT / source上の成立性確認。方式採用・Tier・DDL / code・検証実行・Gateは未判定。
**Ownership:** Referenceの最小復旧構成とTooling検証。Frameworkの汎用復旧機能を追加しない。
**確認範囲:** `docs/daily-development-workflow` / `e75a70a`＋未commit文書。Repository source、ローカルModulith 2.1.1 JARの`javap -c -p`、PostgreSQL 17公式仕様をread-only確認。DB・ApplicationContext・実executorの動作は未実演。
**入力:** [DB保護・executor・確認起動案](phase4-s1-permission-db-executor-startup-review-20261005.md)、[許可 / 試行案](phase4-s1-minimum-permission-and-attempt-review-20261005.md)、[最小構成方針](phase4-s1-minimum-demonstration-scope-review-20261005.md)。

## 1. 結論と推奨比較基準

| 領域 | source / 仕様上の判断 | 今回の推奨比較基準 | 実動で残る確認 |
|---|---|---|---|
| DB-2 保護 | 列UPDATE権限だけでは消費巻戻しを防げない。一意挿入とappend-only権限で消費を分離できる | 許可＋消費記録の2種類を優先比較。1種類＋trigger案は代案へ | 競合・commit不明・role継承・JPA transaction / FK / Audit、未解決対象の次許可禁止 |
| EX-1 相関 | 調査した再送経路はregistryの逐次処理からtarget listenerへ進む。ここまでに別executor投入は見えない | 呼出しcontextを標準async executorでcaptureする案はsource上成立候補 | 実proxy / executor選択、実listener相関、対象外・例外時非漏えい |
| 再送選別 | options版はFAILED候補のDB抽出件数を絞った後にfilter。predicate版は未完了collectionを取得してfilter | API overloadを区別。predicate版を有限の隔離データで使う候補も比較し、options版のbatch=1を対象1件選別保証にしない | 対象外FAILEDが先にある場合、取得量、既存RESUBMITTEDによる上限抑止、未選定時の保留 |
| ST-1 起動 | Referenceはroot scan。Web Security配置にServlet条件がなく、modeだけでnon-Web化は完成しない | 同一artifactで通常 / 限定確認Web / 単発復旧のBean境界を明示 | 非WebのHttpSecurity依存解消、確認Webの認証・送信Bean不在・startup無送信 |

以上は「成立させる経路を確認した」という設計判断であり、現状のReferenceが既に動くという意味ではない。最小構成に必要な変更が残る。現段階で全条件PASSやTier決定には進めない。

## 2. DB保護：許可と消費を分ける案

**後続の権限修正:** [Tier・保存 / 権限・検証手順§3](phase4-s1-minimum-tier-storage-verification-review-20261005.md)で、許可rowの`SELECT FOR UPDATE`には列UPDATE権限も必要なことを確認した。下表の復旧role SELECT-onlyは読取だけの場合の案で、row lock候補では技術version列だけUPDATE権限を追加し、対象 / 発行者 / 確認終了は保護する。

[PostgreSQL 17の制約](https://www.postgresql.org/docs/17/ddl-constraints.html)と[権限](https://www.postgresql.org/docs/17/ddl-priv.html)を根拠に、消費事実をUPDATEではなく一意INSERTとして保存する候補を優先する。実行試行管理・承認workflow・常設実行権tableを新設する案ではない。

| 論理record | 保存情報と書込者 | 必要な保護 |
|---|---|---|
| 実行許可 | 不変の発行者・対象 / snapshot・理由 / 期限 / 証拠参照。認証済み発行経路がINSERT。確認者・確認時刻 / 結果参照だけ確認経路が追記 | 復旧roleはSELECTのみ。対象 / actorのUPDATE、DELETE / TRUNCATEなし。同環境＋publicationで確認未終了の許可は一つだけ |
| 消費事実 | 許可ID、operation UUID、worker世代、認可確認 / 消費時刻。復旧roleがINSERT | 許可IDにPK / UNIQUE、operation UUIDに一意制約、必要情報NOT NULL。runtimeにUPDATE / DELETE / TRUNCATEなし。許可への同owner FKを候補 |

消費recordをUPDATE / DELETEできないroleで再実行しても、同じ許可IDのINSERTは競合する。別のoperation IDへ変えても許可IDの一意性で拒否する。ただしowner / superuser・継承権限・別credentialを除外した条件下の保証で、DB管理者による消去耐性ではない。

確認終了項目は受付 / 確認roleが持つ限定UPDATE権限とApplicationの一度だけの条件更新を候補とする。消費記録を消さないため、確認終了を誤って戻しても既存許可を再消費できない。確認履歴の完全なDB改変防止まではこの方式の保証に含めず、Auditと権限付き確認経路で記録する。誤った早期確認終了は別許可を発行可能にするため、人の停止・副作用照合とApplication認可が重要である。runtime確認credentialは信頼された経路だけが使用する。

未確認終了許可の対象一意性はpartial unique index等を候補とする。期限切れでも自動解除せず、未消費の取消確認か、消費済みの副作用確認を経て閉じる。消費processは許可rowをlockして、期限・未確認終了・snapshot・未消費を確認し、消費INSERTと送信前Auditを同じtransactionでcommitする。確認終了経路も同じrowをlockし、競合して消費と取消を同時成立させない。SQL / DDL・lock順は未固定。

commit成功前は送信しない。commit結果不明ならINSERTをやり直して送信せず、保存された消費recordとworker / providerを照合する。SELECTで未消費を見てから独立transactionで送信する方式は採らない。JPAで明示flushしてもcommit成功の証明ではない。

**1種類＋triggerとの比較:** 2種類案は消費巻戻し防止を通常の一意制約・INSERT-onlyに寄せられる。1種類案は少ないtableで済むが、消費 / operationの旧値からの変更を拒否するtriggerとORM更新の整合が必要。保護の見通しを重視して2種類案を優先比較する。前段の4種類管理台帳は依然として初回必須にしない。具体採用は未判定。

## 3. 相関経路：調査した標準再送の流れ

ローカルModulith 2.1.1の内部classを調査対象として次を確認した。Reference実装から内部classへ依存する提案ではない。

1. options版の再送は`PersistentApplicationEventMulticaster.doResubmitIncompletePublications`からregistryの`processFailedPublications`へ委譲する。predicate版は別の`doResubmitUncompletedPublicationsOlderThan`から`processIncompletePublications`へ進み、未完了collectionを取得して同じ`processPublications`へ渡す。
2. `DefaultEventPublicationRegistry.processFailedPublications`は現在RESUBMITTED件数とmaxInFlightを確認し、minAge / batch件数によるFAILED候補をRepositoryから取得する。
3. `processPublications`は取得済みcollectionの通常streamにfilterを適用する。各対象の`markResubmitted`が成立した後、in-progressへ登録してConsumerを呼ぶ。
4. multicasterはtarget identifierに対応するlistenerを探し、`executeListenerWithCompletion`から`TransactionalApplicationListener.processEvent`を呼ぶ。listenerが存在しない場合は失敗を記録する経路がある。

調査範囲のコードは通常stream / 直接呼出しで、別executorへの投入は見えない。したがって呼出しthreadの不変operation contextをlistener proxyのasync投入まで保持する案には根拠がある。一方、`@ApplicationModuleListener`からのasync proxy・実executorへ届くところは未実証である。Bean / proxy構成や実行時のdecorator合成を別に確認する。

**対象選別の新しい注意点:** options版ではbatch / maxInFlightの制限はfilterより前に適用される。batch=1かつ対象外FAILEDが先に抽出されると、対象publicationはfilterへ渡らず何も再送されない可能性がある。既存RESUBMITTED件数による抑止もあり得る。この制限を既存probeのpredicate版へそのまま当てはめない。どちらもAPIのvoid返却を対象受付成功と扱わない。このリスクはbytecodeからの推論で、新しい実演結果ではない。

S1の有限な隔離データでは、既存probeと同じpredicate版＋厳密対象filterを優先比較できる。ただし未完了collectionの取得量を制御し、無制限データでの利用へ広げない。options版を採るなら明示minAge / batch / maxInFlightと対象が抽出範囲に入る条件をreviewする。対象が選ばれない場合は「未選定 / 保留」とし、無制限retryや再送前FAILEDによる成功判定を行わない。選定できなかった許可も消費事実は消さず、人が安全な未送信と対象状態を確認して次を判断する。必要なら公開APIによる対象選別の限界を方式比較へ戻す。任意PUBLISHED / PROCESSINGがoptions版のFAILED抽出経路で復旧可能とは説明しない。predicate版で取得される状態の実効範囲も検証し、クラッシュ後の状態整理は別の停止確認 / stale条件へ照合する。

相関は、再送呼出しthread→実executorのcapture→listener→通知Use Caseまで、不変contextのoperation / publication / event / listenerを照合する。MDCは補助表示、認可は保存された許可＋現在権限。標準tracerによる新しい実traceを別途合成する。既存requestId decoratorだけでoperation / traceが伝わるとはしない。

**EX-1の出口:** 対象外FAILED・同event別publicationを用意し、対象1件の選定と実listener相関、対象未選定、欠落 / 別operationの送信拒否、thread再利用時の掃除、例外・停止時の不明保存を確認する。対象試行はpublicationの前後・operation / worker記録・provider / Auditを突合し、attempt増加だけで完了としない。

## 4. 起動境界：現行sourceの不足と最小修正案

[ReferenceSecurityConfiguration](../../koiki-reference-app/src/main/java/org/koikifw/reference/identity/configuration/ReferenceSecurityConfiguration.java)は無条件Configurationで、HttpSecurityとWeb用logout customizerを要求するSecurityFilterChainを定義する。現状ではnon-Web root scanでも候補に入るため、Bean不足等を起こし得る。これはsourceリスクで、起動して再現した結果ではない。

同classのmatcherは通常login・identity / master / expenses等で、新しい許可 / 確認routeは未定義である。既存chainをimportするだけで新routeの必要permissionまで成立したとは扱わない。Framework default denyを維持し、限定routeを明示的に認可する。

[Reference main](../../koiki-reference-app/src/main/java/org/koikifw/reference/ReferenceApplication.java)はroot scan、[expense persistence構成](../../koiki-reference-app/src/main/java/org/koikifw/reference/expense/adapter/outbound/persistence/ExpensePersistenceConfiguration.java)は独自EntityScanを持つ。Component除外だけでEntity / Repository scanも限定したとしない。

| 境界 | 最小変更候補 | 起動・拒否で確かめる条件 |
|---|---|---|
| 共通入口 | 同一artifactで目的を一つに確定し、Web種別とApplication configurationを組み立てる。目的不明 / 矛盾は起動拒否 | 通常既定の互換、復旧ID欠落・Web / non-Web矛盾・複数modeを拒否 |
| Web Security | Reference Web構成にServlet条件とmode適用を設ける候補。通常chainと限定確認chainを目的別に構成 | 非WebでHttpSecurity依存Beanなし、確認Webで本人認証・CSRF / 権限・logout・default deny成立 |
| 業務Bean | 明示import / scan範囲かmode条件を比較し、通常Controller / 更新Use Case・event listener・provider Adapterを確認modeから外す | Bean一覧とroute一覧で送信経路不在。空画面やpermission拒否だけをBean分離の証拠にしない |
| Persistence / Framework | 確認に必要なIdentity・Session・Audit・許可 / 消費読取を保持し、Entity / Repository scan・migration権限を点検 | 最小roleで認証・確認commitができ、publication更新 / DDLは拒否。Framework Identity / Audit auto構成を不用意に全面除外しない |
| Modulith・executor | 確認modeではlistener / runner・auto再送・stale monitor・schedulerを無効。復旧modeでは対象listenerと必要executorだけ有効 | 起動前後・HTTP確認後にprovider件数 / publication status / attempts不変。復旧の意図した再送だけを観測 |

許可発行と結果確認は同じ限定Web構成を候補とし、用途ごとに別アプリartifactや常設運用serviceを増やさない。安全な結果確認は旧復旧worker停止確認後に行う。未解決の副作用は許可を閉じず保留する。確認後の通常Web再開は別操作として人が判断する。

既存Security設定変更とBean条件はReference-owned修正候補。無条件Configurationを消してnon-Webを通すだけの変更や、新しいprofileで既存通常routeを丸ごと公開する変更はしない。正式Public API変更をこの起動問題の既定解にしない。

## 5. 現時点のreview判定と次の出口

**後続review票:** [Tier・保存／権限・検証手順](phase4-s1-minimum-tier-storage-verification-review-20261005.md)へ最小候補の不変条件・Tier比較、論理schema / 修正権限、V0〜7を整理した。狭いTier 2は優先review候補で採用決定ではない。実動は未実施。

| 条件 | 今回の到達点 | 未成立の出口 |
|---|---|---|
| DB-1 / DB-2 | 2種類記録による通常制約・権限の保護方式を優先比較できる | 権限表・DDL / JPA transaction review、競合と巻戻し拒否の実DB証拠 |
| EX-1 / EX-2 | 標準再送のfilter / target経路に相関方式の根拠がある。候補抽出の限界を追加確認 | 実executor / proxy・decorator / tracer・対象選定 / 未選定・非漏えいの証拠 |
| ST-1 | Web Securityの条件不足とscan境界を特定し、変更箇所を整理できた | 通常回帰・non-Web起動・認証済み確認Web、Bean / route / 外部送信不変の証拠 |

この結果はsource / 仕様照合の到達点で、runtime PASS・Owner採用判定ではない。次の設計判断は2種類記録方式と目的別起動構成を最小候補に絞ること、その候補に対するTier・DDL / 権限・executorとcase検証手順をまとめることである。sourceで特定した不足があるため、Tierや工数を確定せず実証前のblocking reviewへ接続する。今回code・POM・migration・DB権限・環境起動・remoteは変更していない。

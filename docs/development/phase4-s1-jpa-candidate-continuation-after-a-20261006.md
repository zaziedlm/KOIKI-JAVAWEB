# A承認後の狭いJPA共有モデル候補：継続検討（2026-10-06）

**状態:** 候補検討継続とA18件・B31件・C38件の受入はOWNER APPROVED（§7）。[初回採用判断表D1〜D6](phase4-s1-reference-foundation-adoption-decisions-20261006.md#8-owner採用判断2026-10-06)の経路方針・条件区分・Audit／DDL設計もOwner採用済み。次は必要なJ1／J8提出物・正本差分準備。Gate／正本整合・正式開始票の残判断・実装開始は未成立。
**入力:** [A結果とOwner承認](../architecture/validation/phase4-s1-additional-a-jpa-20261006.md)、[責務／認可／Audit案](phase4-s1-responsibility-authorization-audit-mode-draft-20261005.md)、[追加検証契約](phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md)。正本の境界を変更せず、A後の残条件を具体化する。
**位置:** Tooling検証結果をReference-owned notification候補の検討へ戻す文書。正式Reference / Framework codeは変更しない。Owner一人＋Codexの順次協働。

## 1. Aから継続できる候補

許可とappend-only消費の2種類を維持する。permit / consumptionを同module内のJPA共有モデル候補とし、可否規則はDomain、認可・transaction・Audit・送信順序はApplication、SQL / lockはPersistence Adapterへ配置する。

| 論点 | Aで確認したこと | 検討を続ける範囲 |
|---|---|---|
| 発行 | 発行列だけのINSERTと不変値の再読取 | 不変target / actor / 期限を保持する許可モデル。actorの信頼性は認証／認可側で確かめる |
| 更新 | versionだけ／確認列＋versionだけのUPDATE | 不変列の更新禁止と限定更新を候補継続。正式モデルでも実SQL回帰を必要とする |
| 消費 | 一意INSERT、DBで改変拒否 | 消費を戻さず保持する。Javaの公開更新メソッドを設けない候補 |
| 排他 | permit row lock下の確認／消費、一意制約、rollback | Adapterでlockと保存、Domain / Applicationで許可条件を再確認 |
| 登録・構成 | test専用の明示XML登録と用途別接続 | Aのscan隔離・pool数・XML方式は検証構成。正式scan／Bean構成の採用はC／正式接続reviewへ残す |

Aの`corruptForNegativeTest`、固定時刻、操作文字列、raw EntityTransaction、専用DDL / roleは正式モデルへコピーする資材ではない。`@DynamicUpdate` / column mappingは成立性の証拠として扱い、正式実装の検証なしに全モデルへ規約化しない。

## 2. モデルへ置く可否規則と外側の条件

| 条件 | 配置候補 | 残る証拠 |
|---|---|---|
| 対象／発行者／発行期限の不変性 | 許可モデル＋DB column権限 | Aは保存境界を確認。正式の生成／復元経路は後続 |
| 未閉鎖・期限内・対象一致での消費 | モデルで値を比較し、Applicationが現在値と時刻を渡す | B2で拒否枝。期限は`now < expiresAt`を境界候補とし、正式TTL・時計差許容は未固定 |
| 一度だけの消費・未解決対象の次許可禁止 | Domain / Applicationで意図を表し、DB一意制約で競合時も保証 | A成立。消費済み／不明を自動解除しない条件は維持 |
| 確認終了の可否 | Domain / Applicationで消費と運用証拠を照合、Adapterでlock | Aは競合時の保留。provider結果／旧worker停止の正当性は別証拠 |
| 現在Identity／permission／scope | Applicationから自moduleのPort / Adapter経由でPublic IdentityQuery | B1／B2／B6。本人認証はC。scopeはIdentityへ業務属性を追加せずReference側で拘束 |
| Business Auditと保存の原子性 | ApplicationのSpring管理transactionと既存Public Recorder | B3／B4。消費が確定してから送信probeへ進む |
| 拒否のSecurity Audit | 拒否処理からPublic Recorder、保存transactionとは別境界 | B5。Audit障害でも拒否を解除せず、有限poolを確認 |

DomainへIdentityQuery、Audit Recorder、EntityManagerやprovider照会を直接持たせない。結果不明をAuditResultの新enumや第三の管理台帳で解決する案へ広げない。

## 3. B開始前に具体化する項目

既存sourceを再確認したところ、IdentityQueryは現在userのstatus / permissionCodes / versionを返し、認証や失効lockを提供しない。AuditActorのSYSTEMはFramework-owned背景処理用であり、Reference workerへ無条件に使わない。Business Recorder内部はMANDATORY、SecurityはREQUIRES_NEW。参照は設計確認のためで、fixtureから内部classを直接importしない。

**Aのraw EntityTransactionをそのままBへ渡しても、SpringのMANDATORY契約を満たした証拠にはならない。** Bでは同じ用途のDataSource / EntityManagerFactory / Spring管理JpaTransactionManagerへ接続し、Public Recorderと同じtransaction・flush / rollbackであることを実証する必要がある。Aの3用途contextを一つの複数DataSource構成へ束ねる実装は先行しない。

| 開始前の項目 | 提示する候補・必要入力 | 現在の判定 |
|---|---|---|
| 依存 | test限定`s1-contract` profile、既存Identity / Audit正式座標と推移依存。Identity POMはSecurity / Data JPA / Auditへ依存 | BOM単独修復をOwner承認後、offline実効POM／tree／resolve成功。既存JAR内容を照合しhash固定。non-Web B構成で実Bean成立 |
| schemaと登録 | 既存artifact内のIdentity / Audit migrationを隔離DBへ明示適用。testモデルとFrameworkのEntity登録を合成し、runtime Flyway無効・制限loginでvalidate | 適用順・grant・migrationの取得元とauto-configuration条件を開始票で具体化。正式SQLは複製しない |
| 認可入力 | 発行／閲覧／消費／確認終了の能力別test code、Reference側environment / target scope、fixture userの有限集合 | 検証用値と正式契約を区別した具体案が必要。Aのfixture-actorを認証済み主体と見なさない |
| actor／相関 | 発行／閉鎖は確認済みUSER、消費は真正な許可の発行者USERを優先候補、worker世代は消費記録。resourceはpermit ID候補 | 消費actorの意味とSecurity拒否actorの分類をOwner reviewへ提示する。AuditEventに任意metadataを追加しない |
| target／attempt | B2は現在target / attemptと許可の拘束の差異を拒否 | Aのpermitにattempt項目はない。照合入力の保全・取得元・必要model / SQL差分をreviewし、無根拠のtest入力で正式接続を閉じない |
| 失効窓 | 消費前の現在確認に加え、消費commit後・probe前に再確認し、失効時は消費保持／送信0／保留を優先候補 | B6の具体経路と観測点を決める。再確認後からI/Oまでの窓は残る |
| 回帰／上限 | A18件、初回L1〜4、依存追加で影響する既存context。単独B・fork1・DB1＋Ryuk、pool合計4等を元契約案と照合 | Bのmethod / invocation、資源上限・16〜32標準時間の再判断枠・停止条件は個別開始票で判断 |

Bの依存追加・artifact準備が必要なら、対象座標と取得／installの別を開始票に明記する。今回の環境整備実証を将来すべての依存追加の包括承認にしない。

## 4. 次の出口

Ownerは本書を確認し、B専用開始への進行を指示した。[B専用開始票](phase4-s1-additional-b-start-review-20261006.md)を作成し、B1〜B6の31 invocation・具体条件・依存／schema／grant・Spring transaction構成・回帰と上限を提示した。続いてOwnerはその具体条件で作成・実行・test限定POM変更と記載上限を承認した（同票§8）。profile追加後の[preflight](../architecture/validation/phase4-s1-additional-b-preflight-20261006.md)でcached BOM不整合を検出し、fixture作成前に停止した。BOM更新の必要差分を個別判断へ提示し、B開始承認を維持する。

OwnerのBOM単独修復承認後、依存不整合を解消して再開した。[B結果](../architecture/validation/phase4-s1-additional-b-identity-audit-20261006.md)は31件／回帰58件LOCAL PASS。現在Identityとpermission、期限／target、Business同transaction・Security独立transaction、消費commit後失効の消費保持／probe0を候補検討の入力にできる。B結果の受入は未判断。Cの本人認証／実registry／現在値取得元・停止／provider証拠、再確認からI/Oまでの失効窓、正式TTL／scope／配置は残る。

A承認は候補継続を支持する。B／Cと正式接続の残証拠が揃うまでは正式Tier、Reference採用、Framework契約変更、DoD／Gateを固定しない。

## 5. B受入後の継続（2026-10-06）

OwnerはB結果の4つのレビュー点を承認した。現在Identity／permission、期限／target、Business同transaction・Security独立transaction、消費後失効の消費保全を、B専用条件で成立した候補継続の証拠として受け入れる。上記§3・4のB開始前候補と受入待ちの記述は経過として保持する。

Domainへ可否規則、Applicationへ認可・transaction・Audit・実行順序、AdapterへSQL／lockという狭いJPA共有モデル候補を継続する。Bの単一fixture class構成やXML／専用roleを正式配置へコピーしない。

Cで追加するのは、認証されたFrameworkPrincipalからのactor取得、Web能力／scope／CSRF、実Sessionの保存、modeごとの更新禁止、実registry現在値／proxy・限定再送、context再起動時の不明保全。正式provider／停止証拠の真正性、I/Oまでの失効窓、TTL／scopeの正式値、正式scan／Reference回帰はC局所PASSでも残る。[C専用開始票](phase4-s1-additional-c-start-review-20261006.md)を次の開始判断対象とする。

## 6. C開始承認後の実行結果（2026-10-06）

OwnerはC専用開始票のfixture作成・実行・test限定POM差分と上限を承認した。[C Evidence](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md)は38件／同profile回帰89件LOCAL PASS。実認証された主体由来actor、能力／scope／CSRF、用途別DB roleと更新受付の分離、実registry snapshot／proxy経由の対象1件再送、commit／rollback双方のUNKNOWN保留を追加証拠とする。Cの結果受入は未判断。

次はCレビュー点4項目の受入判断後、A／B／Cが支持したモデル規則・Application認可／transaction／Audit／実行順序・AdapterのSQL／lock／registry版依存と、未取得の正式接続条件を整理する。C資材をそのまま正式Referenceへ昇格しない。provider／旧worker停止の真正性、再確認とI/Oの窓、TTL／scope／配置は引き続き残る。

## 7. C受入後の継続（2026-10-06）

OwnerはCの検証結果・受入レビュー点4項目を確認し、ここまでの結果と理解について問題なしとして承認した。[C Evidence§6](../architecture/validation/phase4-s1-additional-c-web-mode-20261006.md#6-owner受入承認2026-10-06)を正本とする。A18件・B31件・C38件の局所検証結果がすべてCOMPLETE / OWNER APPROVEDとなった。§6の受入未判断は承認前の履歴とする。

次の検討入力は、Aの保存・競合境界、Bの現在Identity／permission・Audit／失効保全、Cの実Web主体・mode分離・実registry限定再送・UNKNOWN引継ぎ。これらを使い、Domainの可否規則、Applicationの認可・transaction・Audit・実行順序、AdapterのSQL／lock／registry版依存と正式接続時の残条件を整理する。正式provider／旧worker停止の真正性、I/Oまでの失効窓、正式TTL／scope／配置／Reference回帰は引き続き残し、fixtureを正式成果物へ昇格しない。

## 8. 既存Reference保護を共通条件とする検討（2026-10-06）

Ownerは、現在のReferenceの機能・品質を阻害せず、安全側へ倒した機能反映・拡張とするよう指示した。責務、正式配置／接続条件、初回実装範囲／受入を連続する検討軸として扱う。[検討案](phase4-s1-reference-safe-integration-design-draft-20261006.md)へ現行sourceの登録／Security／migration条件と、3軸を結ぶ責務・接続・検証表をまとめた。

最初は通常起動から登録を外した保存・認可・Audit基盤を候補とし、Web受付、復旧／通常通知、expense承認連携を順次判断する。追加機能の既定無効だけでEntity／migration／依存の影響が消えると見なさず、通常構成の未適用／適用済みDB起動と既存回帰を受入条件にする。推奨順序・具体配置はDRAFTであり、正式開始承認へ読み替えない。

# KOIKI-JavaWeb-FW Reference Application 業務仕様

**版:** v0.1  
**初稿日:** 2026年8月17日  
**状態:** ACCEPTED  
**Ownership:** Reference  
**Architecture Owner:** Shuichi Kataoka  
**対象DoD:** Phase 0 DoD 0-6

## 1. 目的

本書は、KOIKI Reference Applicationが扱う「経費申請・承認」の業務仕様を、
Frameworkの設計規約や実装技術から独立して定義する。

Phase 0では、状態遷移、不変条件、権限matrix、代表flow、範囲外、および受入条件を
一貫してreviewできる状態にする。本書の確定はReference Applicationの実装完了を意味しない。

## 2. 位置づけと正本

### 2.1 Reference Applicationの役割

KOIKI Reference Applicationはデモではなく、次を担う。

- KOIKI Frameworkの正しい利用例
- integration test対象
- release時のsmoke test
- AI agentの参照実装

ReferenceはFrameworkの内部実装ではなく、業務語彙や業務規則をFrameworkへ持ち込まない。
Walking SkeletonのJava classやSQLもReferenceへ直接昇格させない。

### 2.2 参照する正本

| 正本 | 本書で使用する判断 |
|---|---|
| [グランドデザインv0.2 §26](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#26-reference-application) | 業務題材、module構成、Phase配置、実証範囲 |
| [ADR Register ADR-043](../architecture/adr/README.md) | 経費申請・承認、単一application、Phaseごとのmodule追加 |
| [Glossary v0.1](../standards/KOIKI-JavaWeb-FW_Glossary_v0.1.md) | Ownership、業務module、Tier、Application Use Case、Domain Model等の語義 |
| [Phase 0 DoD Closeout](../architecture/KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md) | DoD 0-6の不足と完了条件 |

本書は上記Architecture判断を業務仕様へ具体化する。競合が生じた場合は本書だけで
Architecture判断を変更せず、Governanceに従って正本側をreviewする。

## 3. 確定済みBaselineと未確定範囲

| 論点 | 現時点の判断 | 根拠 | 状態 |
|---|---|---|---|
| Referenceの役割 | 正規利用例、integration test、smoke test、AI参照実装 | §26.1、Glossary | 確定 |
| 業務題材 | 経費申請・承認 | ADR-043、§26.2 | 確定 |
| application構成 | 単一deployableのモジュラーモノリス | ADR-043、§26.3 | 確定 |
| 業務module | `identity`、`master`、`expense`、`notification`、`accounting` | §26.3 | 確定 |
| 中核の役割 | 申請者、承認者、経理 | §26.2 | 確定 |
| `expense`の構造 | Tier 2 RICH、JPA兼用方式 | §26.3、Glossary | Architectureとして確定 |
| 状態遷移 | `DRAFT`、`SUBMITTED`、`APPROVED`、`REJECTED`、`RETURNED`、`SETTLED`を使用 | §26.3、本書§9 | ACCEPTED |
| 業務不変条件 | 明細合計と申請額の一致、`DRAFT`だけ編集可能、自己承認禁止、未処理申請がある部門の廃止禁止 | §26.3、本書§8〜§10 | ACCEPTED |
| 権限 | UIに依存せずbackendで強制し、Role、Permission、resource所有権、部門scope、業務状態を組み合わせる | §3.4、§14.4、§26.2、本書§10 | ACCEPTED |
| ScopeとNon-goals | §11の代表Use Case群を業務scopeとし、v0.1で扱わない業務と後続Phaseで決める技術方式を分離する | 本書§4・§11 | ACCEPTED |
| 受入条件 | 業務flowと拒否経路を検証可能にする | DoD 0-6、本書§11・§14 | ACCEPTED |

## 4. ScopeとNon-goals

**Owner Review:** ACCEPTED（2026年8月17日、Shuichi Kataoka）

### 4.1 Scope

本書は、次の業務を対象とする。

- 代表scenarioに必要な最小限のユーザー、role、権限の管理
- 階層を持たない部門と経費科目の管理
- 経費申請の作成、編集、提出、承認、却下、差戻し、再提出、精算
- Actorと権限scopeに基づく経費申請の検索・参照
- 未処理申請を考慮した部門廃止
- 承認・却下結果の通知
- 精算済み申請からの仕訳生成と会計連携
- 未処理申請のremindと月次締め
- §11で定義する記録区分に従った記録

Phase 0では本書の業務仕様を確定する。Phase 2〜4で業務moduleと連携を段階的に実装し、
Phase 5でProduction BaselineとしてReference Applicationの完成を判定する。

### 4.2 v0.1 Non-goals

次はReference Application v0.1の業務scopeへ含めない。

- 本番利用可能な完全な経費精算productの構築
- 顧客固有の就業規則、旅費規程、税務・会計規則の網羅
- 組織階層、多段・並列・代理承認、動的workflow engine
- 申請の取消、取下げ、終端状態からの再開
- 不一致状態を永続化するDraftのautosave
- 多通貨、為替換算、税額計算
- 証憑のupload、保存、OCR、添付file管理
- 給与連携、銀行振込、実在する会計productとの接続、および完全な会計帳簿
- multi-tenancy
- Walking Skeleton実装の正式Referenceへの直接移植

外部通知と会計連携は、Reference内のsimulationまたはtest endpointまでを対象とする。

### 4.3 後続Phaseへ保留する技術方式

次は業務上のNon-goalsではなく、本書では技術方式を固定しない。グランドデザインおよび
該当Phaseで承認されたArchitecture判断に従い、設計・実装検証で具体化する。

- MVC、REST API、React SPAの提供形態
- session、Bearer JWT、Cookie等の認証方式
- CSRF、token失効、refresh rotation、logout
- Problem DetailsとAPI versioning
- Spring Modulith Level 2、event永続化、retry、監視
- 通知・会計連携の具体的な通信protocol
- Java API、DB schema等の詳細実装

## 5. Actorと責務

| Actor | 業務上の責務 | 確定状況 |
|---|---|---|
| 申請者 | 自身の経費申請を作成・編集・提出し、差戻し後に再編集する | §10 ACCEPTED |
| 承認者 | 許可された部門scopeの申請を確認し、承認・却下・差戻しする | §10 ACCEPTED |
| 経理 | 承認済み申請を精算し、会計連携結果を確認する | §10 ACCEPTED |
| identity管理者 | ユーザー、Role、Permissionを管理する | §10 ACCEPTED |
| master管理者 | 部門、経費科目を登録・更新・廃止する | §10 ACCEPTED |
| system job | 人間のRoleとは分離したservice actorとして、remind、月次締め、通知再送等の指定Use Caseだけを起動する | §9・§10 ACCEPTED |

## 6. 業務moduleとOwnership

| Module | Tier / 永続化 | 業務Ownership | 導入Phase |
|---|---|---|---:|
| `identity` | Tier 1 SIMPLE / JPA | ユーザー、role、権限の管理画面 | 2 |
| `master` | Tier 1 SIMPLE / JPA | 部門、経費科目 | 3 |
| `expense` | Tier 2 RICH / JPA兼用 | 経費申請・承認とその不変条件・状態遷移 | 3 |
| `notification` | Tier 1 SIMPLE / JPA | 承認結果の通知と通知記録 | 4 |
| `accounting` | Tier 2 RICH / MyBatis分離 | 仕訳生成と会計system連携 | 4 |

他moduleのApplication Use Case、Domain Model、Repository、Adapterを直接参照しない。
module間の業務契約は、識別子と値のみを持つDomain Eventを原則とする。

## 7. 中核業務概念

| 概念 | 意味 | Ownership | Review事項 |
|---|---|---|---|
| 経費申請（`ExpenseRequest`） | 申請者、初回提出時に固定する申請部門、正の申請額、1件以上の明細、状態を持つ一貫性境界 | `expense` | 部門snapshot、明細合計との一致、および必須項目を§4・§8・§10でACCEPTED。永続化の版管理は実装詳細 |
| 経費明細（`ExpenseLine`） | 有効な経費科目、提出日以前の利用日、必須の摘要・目的、および正の金額を持つ申請内の明細 | `expense` | §4 ACCEPTED。件数上限は必要性を実装時に検証する |
| 金額（`Money`） | 単一application通貨の金額計算と比較に用いるValue Object。受入scenarioでは日本円を使用し、入力額を税込金額として扱う | `expense` | 多通貨、為替換算、税額計算をv0.1 Non-goalsとして§4でACCEPTED |
| 申請状態（`ExpenseStatus`） | 申請の業務lifecycle | `expense` | §9 ACCEPTED |
| 部門 | 申請者所属と承認者の可視範囲を決めるmaster。申請には初回提出時の部門IDをsnapshotとして保持する | `master` | 単一階層の完全一致scope、異動時の扱い、廃止条件を§8・§10でACCEPTED |
| 経費科目 | 明細を分類するmaster。新規作成・編集では有効な科目だけを使用し、既存申請が保持する科目は廃止後も変更しない | `master` | 使用可能な科目による作成・編集を§4・§11でACCEPTED |
| 通知 | 承認・却下結果を宛先へ伝えた記録 | `notification` | 対象eventとOwnershipを§11でACCEPTED。耐久性、再送、重複防止の実装はPhase 4へ保留 |
| 仕訳 | 精算済み申請から生成する会計連携単位 | `accounting` | 起点eventとOwnershipを§11でACCEPTED。勘定規則、再送、外部IDの詳細はPhase 4へ保留 |

## 8. 業務不変条件

**Owner Review:** ACCEPTED（2026年8月17日、Shuichi Kataoka）

| ID | 不変条件 | 適用時点 | 状態 |
|---|---|---|---|
| INV-EXP-01 | 明細合計と申請額が一致する | 新規作成、申請額・明細の変更、および提出時 | ACCEPTED |
| INV-EXP-02 | 申請内容を編集できるのは`DRAFT`だけとし、`SUBMITTED`以降は`RETURNED`から`DRAFT`へ戻るまで編集できない | 編集操作時 | ACCEPTED |
| INV-EXP-03 | 申請者は、承認者Roleを兼務していても自身の申請を承認・却下・差戻しできない | 承認・却下・差戻し時 | ACCEPTED |
| INV-EXP-04 | 状態遷移表にない遷移は拒否する | すべての状態変更時 | ACCEPTED |
| INV-MST-01 | `DRAFT`、`SUBMITTED`、`RETURNED`、`APPROVED`の申請が属する部門は廃止できない | 部門廃止時 | ACCEPTED |
| INV-AUT-01 | backendの認可を通過しない操作は、UI表示状態にかかわらず拒否する | すべての保護対象操作 | ACCEPTED |

### 8.1 明細合計と申請額

- `DRAFT`の新規作成時に、明細合計と申請額が一致しなければ作成を拒否する。
- `DRAFT`の編集は申請額と全明細を一つの業務操作として扱い、変更後に一致しなければAggregateと永続化済み状態を変更しない。
- 提出時にも防御的に再検査し、不一致なら`SUBMITTED`へ遷移しない。
- 申請額または個別明細を外部から独立して変更できるpublic setterを設けない。
- 画面上の未入力・編集中状態は許容できるが、不一致のAggregateをDraftとして永続化するautosaveはv0.1の対象外とする。
- JPAが既存状態を復元するときは業務factoryによる再検査を行わず、application経由の更新、migration、およびtestでDB整合性を保証する。

Walking Skeletonは提出時の検査だけを実証した。正式ReferenceではTier 2の複数Entity不変条件を
明確に示すため、新規作成と編集にも保証範囲を拡張し、Phase 3で追加検証する。

### 8.2 未処理申請

`REJECTED`と`SETTLED`以外の非終端状態を未処理申請と定義する。

| 状態 | 部門廃止 | 理由 |
|---|---|---|
| `DRAFT` | 拒否 | 作成・提出へ進む可能性がある |
| `SUBMITTED` | 拒否 | 承認処理中である |
| `RETURNED` | 拒否 | 再編集・再提出へ進む可能性がある |
| `APPROVED` | 拒否 | 精算が完了していない |
| `REJECTED` | 許可 | 終端状態である |
| `SETTLED` | 許可 | 終端状態である |

`DRAFT`は現在保持する部門、初回提出以降の申請は§10.3で固定した申請部門snapshotで判定する。
取消・期限切れを将来導入して未処理の定義を変更する場合は、§9と本sectionを再reviewする。

## 9. 経費申請の状態遷移

**Owner Review:** ACCEPTED（2026年8月17日、Shuichi Kataoka）

### 9.1 §26から引き継ぐ状態

`DRAFT`、`SUBMITTED`、`APPROVED`、`REJECTED`、`RETURNED`、`SETTLED`を使用する。

§26.3の略記は、`REJECTED`または`RETURNED`からも`SETTLED`へ遷移できるように読めたため、
本書では許可する遷移を次のように分解して確定する。

### 9.2 許可する遷移

| ID | 遷移元 | 操作 | 遷移先 | Actor | 主なguard / 必須条件 | 状態 |
|---|---|---|---|---|---|---|
| TR-01 | 新規 | 作成 | `DRAFT` | 申請者 | 申請者と所属部門が有効、§7の必須項目、正の申請額、1件以上の正の明細、INV-EXP-01 | ACCEPTED |
| TR-02 | `DRAFT` | 提出 | `SUBMITTED` | 申請者 | INV-EXP-01、必須項目充足 | ACCEPTED |
| TR-03 | `SUBMITTED` | 承認 | `APPROVED` | 承認者 | INV-EXP-03、部門scope、楽観lock | ACCEPTED |
| TR-04 | `SUBMITTED` | 却下 | `REJECTED` | 承認者 | INV-EXP-03、理由必須、部門scope | ACCEPTED |
| TR-05 | `SUBMITTED` | 差戻し | `RETURNED` | 承認者 | INV-EXP-03、理由必須、部門scope | ACCEPTED |
| TR-06 | `RETURNED` | 再編集開始 | `DRAFT` | 申請者 | 自身の申請であること | ACCEPTED |
| TR-07 | `APPROVED` | 精算 | `SETTLED` | 経理、または認可された月次締めjob | 未精算、楽観lock | ACCEPTED |

`DRAFT`だけを編集可能状態とする。`RETURNED`は差戻しの事実を表し、申請者が再編集を開始すると
`DRAFT`へ遷移する。`REJECTED`と`SETTLED`は終端状態とし、`SETTLED`へ遷移できるのは
`APPROVED`だけとする。月次締めjobもTR-07と同じApplication Use Caseと状態遷移規則を使用する。

### 9.3 v0.1で許可しない遷移

- 申請取下げ・取消
- 承認取消・精算取消
- `REJECTED`からの再提出または再開
- `SETTLED`からの再開
- 多段承認

却下内容を流用する場合も、既存申請を再開せず別の新規申請として扱う。

## 10. 権限matrix

**Owner Review:** ACCEPTED（2026年8月17日、Shuichi Kataoka）

### 10.1 判定原則

- Roleは排他的なaccount種別ではなく、同一ユーザーが複数Roleを兼務できる。
- `C`は、そのRoleが条件付きで操作を許可することを表す。`—`は、そのRoleだけでは権限を付与しないことを表し、他Roleからの許可を否定しない。
- 自己承認禁止等の明示的な業務禁止は、Roleの組み合わせによる許可より優先する。
- Roleだけで許可せず、Permission、resource所有権、部門scope、業務状態を操作時に組み合わせる。
- identity管理者またはmaster管理者であることを理由に、経費申請への全権限を付与しない。
- 個別ユーザーに対する認可判断結果はcacheせず、操作時点のRoleとPermissionで判定する。

### 10.2 権限matrix

| 操作 | 申請者 | 承認者 | 経理 | identity管理者 | master管理者 | system job | 主な条件 |
|---|---:|---:|---:|---:|---:|---:|---|
| 自身の申請作成・編集 | C | — | — | — | — | — | `DRAFT`、resource所有権 |
| 自身の申請を再編集開始 | C | — | — | — | — | — | `RETURNED`、resource所有権、TR-06 |
| 自身の申請提出 | C | — | — | — | — | — | `DRAFT`、resource所有権、TR-02 |
| 自身の申請閲覧 | C | — | — | — | — | — | resource所有権。状態を問わない |
| 承認対象申請の閲覧 | — | C | — | — | — | — | 申請部門が担当scopeと一致し、`DRAFT`以外 |
| 承認・却下・差戻し | — | C | — | — | — | — | `SUBMITTED`、担当scope、自己申請ではない、TR-03〜TR-05 |
| 精算対象・精算済み申請の閲覧 | — | — | C | — | — | — | 全部門の`APPROVED`または`SETTLED` |
| 精算 | — | — | C | — | — | C | `APPROVED`、TR-07。system jobは月次締め専用Permission |
| ユーザー・Role・Permission管理 | — | — | — | C | — | — | identity管理Permission |
| 部門・経費科目管理 | — | — | — | — | C | — | master管理Permission、廃止時の不変条件 |

### 10.3 部門scopeと異動

- Reference Application v0.1では部門階層を導入せず、承認者へ割り当てた部門IDとの完全一致でscopeを判定する。
- 申請部門は初回提出時にsnapshotとして固定し、それ以降は申請者が異動しても変更しない。
- `RETURNED`から再提出する場合も、同じ申請部門を維持する。
- 部門を変更する必要がある場合は、既存申請を変更せず別の新規申請を作成する。
- 申請者は自身の申請を全状態で閲覧できる。承認者は担当部門の`DRAFT`以外を閲覧できる。経理は全部門の`APPROVED`と`SETTLED`を閲覧できる。

### 10.4 認可責務

| 責務 | 配置 |
|---|---|
| 保護経路への粗い入口制御 | URL認可 |
| Permission、resource所有権、部門scopeの確認 | Application Use Case |
| 状態遷移、編集可能状態、自己承認禁止 | `expense` Domain Model |
| 表示制御、request受付、応答整形 | Inbound Adapter。表示制御だけで認可を代替しない |

Spring Security annotation、認可失敗時のHTTP status、Session / JWT等の具体方式は、本sectionの
業務権限を変更しない実装詳細として後続Phaseで確定する。

## 11. 代表Use Case

**Owner Review:** ACCEPTED（2026年8月17日、Shuichi Kataoka）

### 11.1 共通規則

- Application Use Caseがtransaction境界、Permission、resource所有権、部門scope、処理順序を担う。
- `expense` Domain Modelが§8の不変条件と§9の状態遷移を担う。
- 認可拒否、不変条件違反、状態遷移違反、楽観lock競合では、対象の業務状態を変更しない。
- MVC、REST API、Batch等のInbound Adapterが異なっても同じApplication Use Caseを利用する。
- 正常flowだけでなく、各表の代表的な拒否flowを受入scenarioとして検証する。

### 11.2 Use Case一覧

| ID | Use Case | 主Actor | 所有Module | Phase | 状態 |
|---|---|---|---|---:|---|
| UC-ID-01 | ユーザー・Role・Permissionを管理する | identity管理者 | `identity` | 2 | ACCEPTED |
| UC-MST-01 | 部門・経費科目を登録・更新する | master管理者 | `master` | 3 | ACCEPTED |
| UC-MST-02 | 部門を廃止する | master管理者 | `master` | 3 | ACCEPTED |
| UC-EXP-01 | 経費申請を作成・編集する | 申請者 | `expense` | 3 | ACCEPTED |
| UC-EXP-02 | 経費申請を提出する | 申請者 | `expense` | 3 | ACCEPTED |
| UC-EXP-03 | 経費申請を承認する | 承認者 | `expense` | 3 | ACCEPTED |
| UC-EXP-04 | 経費申請を却下する | 承認者 | `expense` | 3 | ACCEPTED |
| UC-EXP-05 | 経費申請を差し戻し、再編集・再提出する | 承認者、申請者 | `expense` | 3 | ACCEPTED |
| UC-EXP-06 | 承認済み申請を精算する | 経理、system job | `expense` | 3・4 | ACCEPTED |
| UC-EXP-Q01 | 経費申請を検索・閲覧する | 申請者、承認者、経理 | `expense` | 3 | ACCEPTED |
| UC-NOT-01 | 承認・却下結果を通知する | system | `notification` | 4 | ACCEPTED |
| UC-ACC-01 | 精算済み申請から仕訳を生成・連携する | system、経理 | `accounting` | 4 | ACCEPTED |
| UC-BAT-01 | remind・月次締めを実行する | system job | `expense`、`notification` | 4 | ACCEPTED |

### 11.3 IdentityとMaster

| ID | 事前条件 | 正常flowと事後条件 | 代表的な拒否flow |
|---|---|---|---|
| UC-ID-01 | identity管理Permissionを持つ | ユーザーへRole・Permissionを付与または剥奪し、後続操作の認可へ反映する | 非管理者による変更、不正または存在しない対象を拒否する |
| UC-MST-01 | master管理Permissionを持つ | 部門・経費科目を登録・更新し、有効なmasterとして保存する | 重複、不正値、非管理者による変更を拒否する |
| UC-MST-02 | master管理Permissionを持ち、対象部門が有効 | `DepartmentDeactivating`を同期発行し、未処理申請がなければ部門を廃止する | §8.2の4状態が1件でもあればtransaction全体をrollbackする |

UC-MST-02の受信側は`expense`自身のRepositoryだけを使用する。master管理者にexpenseの閲覧Permissionを
付与せず、Eventは1回だけ処理する。`REJECTED`と`SETTLED`しか存在しない場合は廃止できる。

### 11.4 Expense更新Use Case

| ID | 事前条件 | 正常flowと事後条件 | 代表的な拒否flow |
|---|---|---|---|
| UC-EXP-01 | 有効な申請者と部門、使用可能な経費科目 | 正の申請額と1件以上の正の明細から、自身の`DRAFT`を作成または編集し、INV-EXP-01を維持する | 明細合計不一致、他人の申請、`DRAFT`以外、楽観lock競合を拒否する |
| UC-EXP-02 | 所有者本人の`DRAFT` | INV-EXP-01を再検査し、初回提出時に申請部門snapshotを固定して`SUBMITTED`へ遷移する | 不変条件違反、所有権違反、状態違反、楽観lock競合を拒否する |
| UC-EXP-03 | scope内にある他者の`SUBMITTED` | `APPROVED`へ遷移し、`ExpenseApproved`を発行する | 自己承認、scope外、状態違反、楽観lock競合を拒否する |
| UC-EXP-04 | scope内にある他者の`SUBMITTED`、却下理由あり | `REJECTED`へ遷移し、`ExpenseRejected`を発行する | 理由なし、自己申請、scope外、状態違反、楽観lock競合を拒否する |
| UC-EXP-05 | scope内にある他者の`SUBMITTED`、差戻し理由あり | `RETURNED`へ遷移する。所有者が`DRAFT`へ戻して編集し、同じ部門snapshotで再提出する | 理由なし、自己申請、scope外、所有権違反、状態違反、楽観lock競合を拒否する |
| UC-EXP-06 | `APPROVED`、経理または月次締め専用Permission | `SETTLED`へ遷移し、Phase 4では`ExpenseSettled`を発行する | 重複精算、状態違反、Permission違反、楽観lock競合を拒否する |

UC-EXP-01では未完成Draftのautosaveを行わない。UC-EXP-02〜UC-EXP-06の成功は業務監査へ記録する。
通知または会計連携の外部I/O失敗は、承認・却下・精算のtransactionをrollbackしない。

### 11.5 Expense検索Use Case

UC-EXP-Q01は§10の閲覧scopeをquery条件として適用し、scope外のdataを取得後に隠す実装を行わない。

| Actor | 取得できる申請 |
|---|---|
| 申請者 | 自身の申請。状態を問わない |
| 承認者 | 担当部門の`DRAFT`以外 |
| 経理 | 全部門の`APPROVED`と`SETTLED` |

承認待ち一覧は申請者名・部門名を含む複数集約queryであり、Tier 2のQuery Portとread modelを
`application.query`が所有し、Outbound AdapterがJdbcClientでmaterializeする。

### 11.6 Phase 4の派生処理

| ID | 起点 | 正常flowと事後条件 | 代表的な拒否・失敗flow |
|---|---|---|---|
| UC-NOT-01 | `ExpenseApproved`または`ExpenseRejected` | 通知記録を冪等に作成し、対象者へ結果を送信する | 重複Eventでは二重通知せず、一時失敗を再送可能な状態にする |
| UC-ACC-01 | `ExpenseSettled` | 同じ申請から仕訳を1回だけ生成し、会計systemへ連携して結果を記録する | 重複Eventでは再生成せず、一時失敗を再処理可能な状態にする |
| UC-BAT-01 | scheduleされたsystem job | 未処理申請のremindを通知へ委譲し、月次締めはUC-EXP-06を使用する | 通常Use Caseの認可、不変条件、状態遷移を迂回しない |

Phase 4のEventは識別子と処理に必要な値だけを持ち、JPA Entityを含めない。Event payload、
Spring Modulith Level 2、耐久性、再送、監視の具体方式はPhase 4の設計・実装検証で確定する。

### 11.7 記録区分

グランドデザイン§15.2の3分類に従う。Role・Permission変更やmaster管理等の管理者操作も、
管理業務transactionと同時に成立するため、独立した監査種別を作らず業務監査へ含める。

| Architecture分類 | Referenceでの対象 |
|---|---|
| 業務監査 | 提出、承認、却下、差戻し、精算、Role・Permissionの付与・剥奪、部門・経費科目の登録・更新・廃止、通知・会計systemとの外部連携を実行した事実 |
| セキュリティ監査 | Login成功・失敗、Logout、Account Lock、認証試行の閾値超過、認可拒否 |
| 副作用・連携 | 実際の通知送信、仕訳生成、会計systemとの外部通信、および各moduleが所有する配信・連携の処理状態。業務transactionのcommit後に実行する |

`DRAFT`の単純な作成・編集履歴はv0.1の必須監査対象外とする。監査recordの必須fieldと保持期間は
Architecture側の監査規約に従い、業務仕様へFramework実装詳細を持ち込まない。moduleが持つ
配信・連携の処理状態を、外部連携を実行した事実の業務監査の代用にはしない。

## 12. Module間連携

| 送信Module | Event | 受信Module | 意味 | 実行方式 | 業務結果 |
|---|---|---|---|---|---|
| `master` | `DepartmentDeactivating` | `expense` | 部門廃止前に`DRAFT`、`SUBMITTED`、`RETURNED`、`APPROVED`の申請を検査する | 同期 | 未処理申請があれば廃止transactionをrollback |
| `expense` | `ExpenseApproved` | `notification` | 承認結果を通知する | 非同期（Phase 4） | 通知記録を冪等に作成して送信 |
| `expense` | `ExpenseRejected` | `notification` | 却下結果を通知する | 非同期（Phase 4） | 通知記録を冪等に作成して送信 |
| `expense` | `ExpenseSettled` | `accounting` | 精算済み申請から仕訳を生成する | 非同期（Phase 4） | 仕訳を冪等に生成し、会計system連携へ進める |

Eventは識別子と値のみを持つ不変なDomain Eventとし、JPA Entityを含めない。
非同期eventの耐久性、再送、監視はPhase 4のArchitecture判断と実装検証で確定する。

Walking Skeletonで同期rollbackを実証済みなのは`DRAFT`と`SUBMITTED`である。正式Referenceでは
`RETURNED`と`APPROVED`を追加し、Phase 3の統合testで4状態の拒否と終端2状態の成功を検証する。

## 13. 競合・拒否・監査

業務仕様として少なくとも次の結果を区別し、各Use Caseの拒否flowへ割り当てる。

| 分類 | 代表例 | 期待する業務結果 |
|---|---|---|
| 業務不変条件違反 | 明細合計不一致、自己承認、未処理申請のある部門廃止 | 状態を変更せず理由を通知する |
| 状態遷移違反 | `DRAFT`の承認、`APPROVED`の再編集 | 状態を変更せず操作を拒否する |
| 認可拒否 | 他人の申請編集、scope外部門の承認 | 情報露出を抑えてbackendで拒否する |
| 楽観lock競合 | 他の承認者が先に処理した | 最新状態を再取得し、操作結果を上書きしない |
| 外部連携失敗 | 通知または会計systemが一時失敗 | Phase 4で定める再試行・再送へ委譲する |

業務監査、セキュリティ監査、副作用・連携の対象は§11.7に従う。記録fieldと保持期間は
Architecture側の監査規約で確定する。

## 14. Phase別受入条件

**Owner Review:** ACCEPTED（2026年8月17日、Shuichi Kataoka）

本sectionはReference Applicationの受入条件を定めるものであり、グランドデザイン§27の
Phase全体DoDを置き換えない。各Phaseの完了には、本sectionと§27の両方を満たす必要がある。

### 14.1 共通判定規則

- 正常flowだけでなく、代表的な拒否flowと永続化後の状態を検証する。
- 業務状態はUI表示ではなくbackendの処理結果で判定する。
- 受入証拠は自動test、CI結果、または実演記録から追跡可能にする。
- 実装方式が未確定の事項は業務上の結果だけを本書で固定し、該当Phaseで設計・検証する。

### 14.2 Phase 2 — Identityと認可基盤

| ID | Given | When | Then |
|---|---|---|---|
| AC-P2-01 | identity管理者と一般ユーザーが存在する | 管理者がRole・Permissionを付与または剥奪する | 変更後の認可判断へ反映され、管理業務の業務監査に残る |
| AC-P2-02 | Actorが対象操作のPermissionを持たない | UIを経由せず保護対象へ直接requestする | backendが拒否し、保護対象の状態を変更しない |
| AC-P2-03 | 同一アカウントで認証失敗が規定回数続く | 閾値を超える認証を試行する | アカウントがlockされ、認証失敗とlockがセキュリティ監査に残る |
| AC-P2-04 | セキュリティ監査を伴う認証失敗が発生する | 認証処理が失敗またはrollbackする | セキュリティ監査は失われない。業務監査とのrollback対比はPhase 3で完成させる |

認証方式、Session / JWT、HTTP status等の具体方式はPhase 2のSecurity設計で確定し、
本sectionでは特定方式へ固定しない。

### 14.3 Phase 3 — Reference Vertical Slice

| ID | Given | When | Then |
|---|---|---|---|
| AC-P3-01 | master管理者がmaster管理Permissionを持つ | 部門・経費科目を登録または更新する | 有効なmasterとして保存され、管理業務の業務監査へ記録される |
| AC-P3-02 | 申請者、有効な部門・経費科目、および正しい明細がある | 経費申請を作成・編集・提出する | 不変条件を維持して`DRAFT`から`SUBMITTED`へ遷移する |
| AC-P3-03 | 申請額と明細合計が異なる、または利用日・金額等が不正である | 経費申請を作成、編集、または提出する | 操作を拒否し、不正なAggregateを永続化しない |
| AC-P3-04 | 担当部門の他者による`SUBMITTED`申請がある | 承認者が承認、却下、または差戻しする | §9で許可された状態へ遷移し、業務監査が同じtransactionで記録される |
| AC-P3-05 | 自己申請、部門scope外、または不正な状態の申請がある | 承認操作を直接requestする | backendが拒否し、申請状態と業務監査を変更しない |
| AC-P3-06 | 未処理申請から参照される有効な部門がある | master管理者が部門を廃止する | `expense`側が拒否し、部門廃止transaction全体がrollbackする |
| AC-P3-07 | 対象部門に申請が存在しない、または`REJECTED`・`SETTLED`の申請しか存在しない | master管理者が部門を廃止する | 部門を廃止できる |
| AC-P3-08 | 同じ版の経費申請を2つのsessionが取得している | 両者が順に更新する | 先行更新だけが成立し、後続更新は競合として拒否される |
| AC-P3-09 | 各Actorの閲覧対象が複数存在する | 経費申請を検索・参照する | §10のscope内だけをqueryし、scope外dataを取得後に隠す処理を行わない |
| AC-P3-10 | 同じApplication Use Caseを呼ぶMVCと最小REST API経路がある | 同じActorが同じ業務操作を行う | transportにかかわらず同じ認可、不変条件、状態遷移結果となる |

Phase 3では最小REST APIと`/api/v1`のversioningを実装する。React SPAは導入せず、
EntityのView露出防止、楽観lock、同期event rollbackを自動testで検証する。

### 14.4 Phase 4 — Enterprise Integration

| ID | Given | When | Then |
|---|---|---|---|
| AC-P4-01 | 承認または却下が成立している | 対応するeventを`notification`が処理する | 通知記録を作成し、同一eventを再処理しても二重通知しない |
| AC-P4-02 | 通知先が一時的に失敗する | 承認・却下と通知処理を実行する | 元の業務transactionはrollbackせず、失敗を検知して再送できる |
| AC-P4-03 | 経費申請の精算が成立している | `ExpenseSettled`を`accounting`が処理する | 申請ごとに仕訳を一度だけ生成し、連携結果を記録する |
| AC-P4-04 | 会計test endpointが一時的に失敗する | 外部連携を実行する | 精算状態はrollbackせず、失敗を再処理可能な状態として記録する |
| AC-P4-05 | MVC、REST API、React SPAの各経路が存在する | 同じActorが同じ経費操作を行う | 同じApplication Use Case、業務認可、不変条件を使用する |
| AC-P4-06 | 未処理申請が存在する | system jobがremindを実行する | 対象scopeだけに通知処理を起動し、重複起動でも二重通知しない |
| AC-P4-07 | `APPROVED`とそれ以外の申請が存在する | 月次締めjobを実行する | 通常操作と同じ精算Use Caseにより`APPROVED`だけを`SETTLED`へ遷移させる |
| AC-P4-08 | 非同期eventが未処理の状態でapplicationを停止する | applicationを再起動する | 未処理eventが失われず再処理され、結果を追跡できる |

Phase 4ではPhase 2の認証基盤とPhase 3のREST APIを利用してReact SPAを追加し、
MVC / SPA併用時の認証とCSRFを実証する。SPAの認証方式、CSRF方式、Spring Modulith Level 2の
具体構成、retry設定値、および外部通信protocolは、承認済みの業務結果を変えない範囲でPhase 4に確定する。

## 15. Traceability

| 本書のsection | 根拠 | DoD 0-6で確定する内容 |
|---|---|---|
| §4 | §26.2〜§26.5 | Scope、Non-goals |
| §5、§10 | §3.4、§26.2 | actor、権限matrix |
| §6、§12 | ADR-043、§26.3 | module Ownershipと連携 |
| §7〜§9 | §26.3、Glossary | 状態遷移、不変条件、業務概念 |
| §11、§13 | DoD 0-6 | 代表flow、拒否flow |
| §14 | §26.3、§26.4 | 受入条件 |

## 16. Owner Review論点

### 16.1 解決済み

| 論点 | Decision | 承認 |
|---|---|---|
| `RETURNED`の扱い | 再編集開始により`DRAFT`へ戻し、編集可能なのは`DRAFT`だけとする | §9 ACCEPTED |
| `REJECTED`の扱い | 終端状態とし、流用時も別の新規申請を作る | §9 ACCEPTED |
| `SETTLED`への遷移 | `APPROVED`からだけ許可し、通常操作と月次締めjobで同じ規則を使う | §9 ACCEPTED |
| Roleの兼務 | Roleは加算的に権限を付与し、自己承認等の明示的禁止を優先する | §10 ACCEPTED |
| 承認者の部門scope | 単一階層の部門ID完全一致とし、初回提出時の申請部門snapshotで判定する | §10 ACCEPTED |
| 閲覧scope | 申請者は自身の全状態、承認者は担当部門の`DRAFT`以外、経理は全部門の`APPROVED`・`SETTLED`を閲覧できる | §10 ACCEPTED |
| 管理者とsystem job | 管理者Roleに全権を与えず、system jobは指定Use Caseだけを実行するservice actorとする | §10 ACCEPTED |
| 明細合計と申請額 | 新規作成、Draft編集、提出の各業務操作で一致を保証し、不一致状態を永続化しない | §8 ACCEPTED |
| 未処理申請 | `DRAFT`、`SUBMITTED`、`RETURNED`、`APPROVED`を非終端の未処理状態とし、参照部門の廃止を拒否する | §8 ACCEPTED |
| 代表Use Case | Identity、Master、Expense更新・検索、通知、会計連携、Batchの13件と代表拒否flowを採用する | §11 ACCEPTED |
| 精算と会計の境界 | `expense`が精算状態を所有し、`ExpenseSettled`を受けた`accounting`が仕訳生成・外部連携を所有する | §11 ACCEPTED |
| 記録区分 | 業務監査、セキュリティ監査、副作用・連携を分け、管理者操作を業務監査へ含め、Draft単純編集は必須監査対象外とする | §11・§14 ACCEPTED |
| ScopeとNon-goals | §11の13 Use Caseを業務scopeとし、組織階層、高度なworkflow、多通貨・税計算・証憑管理、実在product連携等をv0.1 Non-goalsとする | §4 ACCEPTED |
| 経費データの最小範囲 | 単一通貨（受入scenarioは日本円）、正の金額、有効な経費科目、提出日以前の利用日、摘要・目的を扱い、入力額を税込金額とする | §4・§7 ACCEPTED |
| 技術方式の扱い | REST、SPA、認証方式、CSRF、Level 2、外部通信protocol等は業務Non-goalsにせず、該当Phaseへ保留する | §4 ACCEPTED |
| Phase別受入条件 | Reference固有のGiven / When / ThenをPhase 2〜4に定め、Phase 3で最小REST API、Phase 4でReact SPAとMVC / SPA併用構成を実証する | §14 ACCEPTED |

### 16.2 継続Review

なし。本書全体とDoD 0-6は§17で最終承認済みである。

## 17. 承認記録

| 項目 | 内容 |
|---|---|
| Decision | ACCEPTED。Phase 0 DoD 0-6 COMPLETE |
| Evidence | §26、ADR-043、Glossary v0.1との整合review、§4・§8〜§11・§14のOwner Review、および§16の全論点解決 |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月17日 |
| Revisit trigger | 業務scope、actor、状態遷移、不変条件、権限、またはReferenceの実証範囲を変更する場合 |

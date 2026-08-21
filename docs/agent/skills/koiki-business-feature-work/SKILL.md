---
name: koiki-business-feature-work
description: Plan, implement, and review KOIKI business-module changes by deciding module ownership, Tier, responsibility placement, persistence, read models, events, view boundaries, and verification. Use for new or changed business features, controllers, use cases, domain models, repositories, module collaboration, MVC, or REST endpoints.
---

# KOIKI Business Feature Work

業務変更を、モジュール境界と内部責務を崩さずに設計・実装・レビューする。

## 変更前に判断する

次を順番に決め、重要な選択理由を実装またはchangeの設計記録へ残す。

```text
Ownership
  -> Business module
    -> Public boundary
      -> Tier
        -> Responsibility placement
          -> Persistence / read model
            -> Module collaboration
              -> View / API boundary
                -> Verification
```

既存のOpenSpec changeがある場合はproposal、specs、design、tasksを先に読み、Skillをchange固有要求の代わりにしない。

## 1. 所有モジュールと公開境界を決める

- 変更の主語となる業務機能を所有モジュールにする。
- 技術レイヤーではなく業務機能を第一の分割軸にする。
- 他モジュールのApplication Use Case、Domain Model、Repository、Adapterを直接呼ばない。
- 複数モジュールに同じ業務ルールを複製しない。所有者を決める。
- モジュール間契約には、JPA Entityを含まない不変な値のイベントを使う。

境界を決められない場合は、shared-kernelへ逃がさず、業務用語と変更理由から所有者を確認する。

## 2. Tierを選ぶ

Tier 1 SIMPLEを開始点とし、次のいずれかに該当する場合はTier 2 RICHを選ぶ、または昇格を提案する。

1. 業務状態が3状態以上あり、状態遷移規則がある。
2. 複数Entityにまたがる不変条件がある。
3. 同じ業務ルールを2つ以上のUse Caseが必要とする。
4. Use Caseの条件分岐が技術都合ではなく業務ルールにより増えている。

- **Tier 1**: Domain Modelを作らず、単純な業務判断と処理調整をApplication Use Caseに置く。
- **Tier 2**: 不変条件と状態遷移をDomain Modelへ置く。単一モデルへ自然に属さない業務判断だけをDomain Serviceへ置く。

未使用の`domain.service`、Port、Gatewayなどを将来用に作らない。1モジュール内でTierを混在させない。

## 3. 責務を配置する

| 場所 | 置くもの | 置かないもの |
|---|---|---|
| Inbound Adapter | REST/MVC/Event/Batch/Messageの受付、形式検証、DTO変換、応答整形 | 業務ルール、Repository操作、Domain Modelの外部露出 |
| Application Use Case | トランザクション、権限呼出、処理順序、Domain操作、イベント発行 | HTTP詳細、SQL、画面描画 |
| Domain Model | Tier 2の不変条件、状態遷移、Value Object | Controller、HTTP DTO、SQL、`EntityManager`直接操作 |
| Domain Service | ModelやValue Objectのどれにも自然に属さないTier 2業務判断 | 単なる処理手順、DTO変換、Repositoryの薄いラッパー |
| Outbound Adapter | 外部API、File、Messaging、複雑queryとread modelのmaterialize実装 | Use Caseの判断、Application所有のQuery契約 |
| Configuration | Bean構成とAdapter選択 | 業務ルール |

`@Service`などのSpring stereotypeではなく、実際の責務で配置を判断する。

## 4. 永続化とモデル方式を選ぶ

- 更新系はSpring Data JPAを既定とする。
- SQL指向の更新または既存SQL資産の移行では、モジュール単位でMyBatisを選ぶ。
- 1モジュールの更新系でJPAとMyBatisを混在させない。
- Tier 2 + JPAではDomain ModelとJPA Entityの共有方式を既定とする。
- setterを公開せず、状態変更を意味のある業務メソッドへ閉じ込める。

次のいずれかに該当する場合だけ、Domain Modelと永続化モデルの分離を検討する。

1. 変更できないschemaと業務モデルが大きく乖離する。
2. 同じ業務モデルを複数の永続化先へ保存する。
3. schema制約によりモデル側の不変条件を表現できない。
4. MyBatisを採用する。

分離方式やMyBatisの詳細規約は後続Phaseの証拠を確認し、未検証の構造を推測で固定しない。

## 5. read modelを選ぶ

- 単一集約から導出する単純な一覧・詳細にはJPA射影を使う。
- 複数集約、集計、帳票、既存SQLにはJdbcClientを使う。
- MyBatisモジュールではMyBatis Mapperを使い、更新技術との一貫性を保つ。
- Query契約と戻り値のread modelは`application.query`が所有し、Outbound Adapterが永続化技術で実装する。ApplicationからAdapterを直接参照しない。
- Tier 2のread modelは`record`として最終形を返す。
- Tier 1では専用read modelを先行導入せず、必要なApplication DTOを使う。

同一トランザクションでJPA書き込み後にJdbcClientやMyBatisで同じデータを読まない。必要なら読み取りを先に行うか、設計上必要な明示flushを検討する。

## 6. モジュール間連携を選ぶ

直接Bean呼出ではなくDomain Eventを使う。

- 受け手の成功が送り手の業務成立条件なら同期`@EventListener`を使う。
- 副作用または派生処理なら非同期候補とする。
- 外部API、メール、File、Object Storageなどの外部I/Oをlistenerで行う場合は非同期とする。
- listenerは`adapter.inbound.event`へ置き、自モジュールのApplication Use Caseへ委譲する。
- 受け手が送り手の未コミットDBデータを読まないよう、必要な識別子と値をイベントへ含める。

Level 2到達前に非同期実装が必要になった場合は停止し、Spring Modulith採用Levelと耐久性・冪等性・監視を設計判断として扱う。`@TransactionalEventListener`を暫定手段として追加しない。

## 7. ViewとAPIの境界を守る

- Controllerをリクエスト受信、形式検証、Use Case呼出、HTTP応答整形に限定する。
- ControllerからRepositoryを直接呼ばない。
- Domain ModelまたはJPA EntityをControllerの引数、戻り値、MVC Modelへ渡さない。
- トランザクション内で不変なDTOまたはview/read modelへ変換する。
- 業務例外からHTTPへの変換をControllerごとに実装しない。

Problem Details、API Versioning、Security、SPAの具体的実装は該当Phaseの正式構成を確認する。現在の方針だけを根拠に共通例外階層や独自Starterを先行作成しない。

## 8. Frameworkへの昇格を抑制する

業務機能の実装中に生まれた抽象を、その場でFrameworkへ移さない。少なくとも次を確認する。

- 2つ以上の独立案件で同じ契約として使われたか。
- 契約が安定しているか。
- Spring標準で代替できないか。
- 業務語彙を含まないか。
- Public APIの互換義務を受け入れられるか。

条件を満たす可能性がある場合も、昇格は別changeとADRで扱う。

## 9. 検証する

- 対象moduleのDomain、Application、MVC、Persistence、Eventの成功・拒否経路をテストする。
- Repository、transaction、Lazy Loading、rollbackが重要ならTestcontainersの実DBで検証する。
- リポジトリが指定するMaven検証とアーキテクチャ検証を実行する。
- 意図的な負例はtest fixtureへ隔離し、本番コードへ失敗スイッチを入れない。
- 違反時はArchUnit等のエラーメッセージとADR参照に従い、Skillへ機械規則を複製しない。
- 実装が設計の前提を否定した場合は、コードを正当化せず検証記録と設計更新を提案する。

## 判断結果を示す

重要な選択がある作業では、実装前またはレビュー時に次を簡潔に示す。

```text
Ownership / module:
Tier and triggers:
Responsibility placement:
Persistence / model:
Read model:
Module collaboration:
View / API boundary:
Verification:
Deferred decisions:
```

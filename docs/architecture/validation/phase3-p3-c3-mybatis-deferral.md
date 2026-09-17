# Phase 3 P3-C3 MyBatis規約fixture延期判断

## 1. Decision

- Decision date: 2026年9月16日
- Decided by: Shuichi Kataoka, Architecture Owner
- Baseline commit: `9f2247f9be3c4d6a71b0355d7b6d0de0364b05eb`
- Decision: `DEFERRED — MyBatis adoption trigger required`
- Phase 3 next CP: P3-C4 Journey / ADR / Skill / DoD trace
- Production / Public API / dependency change: 0

P3-C3で予定していたMyBatis規約fixture、`PersistenceModel.SEPARATED`、ArchUnit Rule 25〜27 / 30〜37、
`@MybatisTest`およびMyBatis moduleの正式許可は実装しない。MyBatisを否定する判断ではなく、具体的な
採用理由と業務要件が存在しない状態でFramework Public APIと配布規約を固定しないための明示的延期である。

## 2. Context

Phase 3 Referenceの`master`はTier 1 / JPA、`expense`はTier 2 / JPA共有モデルとして実装され、
Domain、MVC / HTMX、REST、楽観lock、Audit、DBおよびcritical journeyの回帰が成立している。
現時点でMyBatisを必要とするReferenceまたはCustomer要件、変更不能schema、既存SQL移行、
JPAでは満たせない性能要件は確認されていない。

一方、MyBatis採用の大枠はグランドデザインとADR-039に定義済みであり、次の安全な停止状態にある。

- `PersistenceTechnology.MYBATIS`は選択肢として定義済みである。
- MyBatis Spring Boot Starter `4.1.0`はBOM管理済みだが、production dependencyへ追加されていない。
- `PersistenceModel`は`SHARED`だけであり、未検証の`SEPARATED`を公開していない。
- KOIKI-ARCH-008 / Rule 8はMyBatis宣言を拒否し、未検証構成の採用を防いでいる。
- Rule 25〜27 / 30〜37、Mapper、converter、schema、SQLおよびfixtureは未実装である。
- Reference productionはJPAのままであり、変更対象がない。

非配布Toolingであっても、fixtureを正規moduleとして成立させるには`SEPARATED`のPublic API追加、Rule 8の
許可matrix変更およびdeferred ruleの適用scope確定が必要になる。具体的なadoption use caseがない段階では、
得られる証拠より契約固定と再reviewの負担が先行すると判断した。

## 3. Approved deferral boundary

| Item | Decision |
|---|---|
| `build-support/mybatis-convention-verification` | 作成しない |
| `PersistenceModel.SEPARATED` | Public APIへ追加しない |
| Rule 8 / KOIKI-ARCH-008 | 現行のMyBatis拒否を維持する |
| Rule 25〜27 | 未実装のdeferred ruleとして維持する |
| Rule 30〜37 | 未実装のdeferred ruleとして維持する |
| `mybatis-spring-boot-starter-test` | dependencyへ追加しない |
| Mapper / converter / `reconstitute` fixture | 作成しない |
| schema / SQL / migration | 追加しない |
| Reference `master` / `expense` | JPA baselineを変更しない |
| MyBatis accounting / Starter | Phase 3では開始しない |
| BOM上のMyBatis version | 既存baselineを変更しない |

P3-C3開始準備で検討したRule 35〜37単独案、およびRule 25〜27 / 30〜37一体実装案のどちらも、
今回のOwner判断では実装承認しない。検討内容は再開時の入力として
`docs/development/phase3-p3-c3-start-handoff-20260916.md`に保存する。

## 4. Adoption triggers

次のいずれかが具体的な要求として成立した場合にだけ、MyBatis adoption Gateを新たに開く。

1. 実際の業務moduleでSQL指向の更新要件が発生する。
2. 変更不能schemaまたは既存SQL資産との統合が必要になる。
3. JPAでは満たせないことを計測で示した性能またはSQL制御要件が発生する。
4. Phase 4 `accounting`をMyBatis分離方式で開始するOwner判断を行う。

「将来使うかもしれない」、BOMにversionがある、またはグランドデザインに選択肢があることだけでは、
再開トリガーとしない。

## 5. Required review when reopened

再開時は過去のP3-C3案を自動承認済みとして扱わず、少なくとも次を採用moduleの実要件から再reviewする。

1. 採用理由、Ownership、Tier、schema OwnershipおよびJPA不採用理由。
2. `PersistenceModel.SEPARATED`を含むPublic API変更を型単位で承認する。
3. Rule 25〜27 / 30〜37の適用scope、検査可能性、positive / negative fixtureを確定する。
4. Mapper、converter、`reconstitute`、read model直接materialize、transaction境界を設計する。
5. 更新件数0を`OptimisticLockingFailureException`へ変換する楽観lock競合testを必須化する。
6. `@MybatisTest`、実PostgreSQL、migration / SQL portabilityおよび既存JPA回帰を検証する。
7. fixtureからFrameworkへ昇格する契約と、業務moduleに閉じる実装を分離する。
8. Public API compatibility、dependency、配布artifactおよびCustomer migration影響を再評価する。

Phase 4 `accounting`が最初の採用候補になる場合も、accounting実装と同時に暗黙有効化せず、実装開始前の
blocking adoption Gateとして上記を閉じる。

## 6. Phase 3 / Gate C impact

グランドデザインのPhase 3 DoD 3-1〜3-11にはMyBatis fixtureまたはRule 35〜37の完了条件が存在しない。
P3-C3はPhase共通の先行規約整備として実行計画に配置されていたため、本延期はReference Vertical Sliceの
承認済み業務scope、DoDまたはAC-P3-01〜10を削除・緩和しない。

Gate Cでは次を確認する。

- P3-C3が未完了ではなく、Owner判断により明示的に`DEFERRED`であること。
- Rule 8がMyBatisを拒否し、`SEPARATED`と関連dependencyが追加されていないこと。
- P3-C4のdeferred inventoryが本Evidence、ADR-039、グランドデザイン、実行計画、Skillと一致すること。
- DoD 3-11の実CI PASSをRemote Gate / Gate Cで別途充足すること。

## 7. Supersession and next step

本判断は、P3-C2 close時点の「次はP3-C3」、P3-C3開始引継ぎの実装提案、およびグランドデザインの
「Phase 3末尾で実施」という時期指定を、実施時期についてのみsupersedeする。MyBatisの選択基準、
分離model規約、Rule 25〜27 / 30〜37の設計意図およびPhase 4 accounting候補は破棄しない。

P3-C3は`DEFERRED — MyBatis adoption trigger required`として閉じ、次はP3-C4を開始する。
P3-C4では本延期をJourney / ADR / Skill / DoD traceとdeferred inventoryへ含める。MyBatis adoption Gateを
再開せずにMyBatis production code、Public API、dependency、migrationまたはfixtureを追加してはならない。

## 8. Repository verification

2026年9月16日の文書反映後に次を確認した。

- `build-support/mybatis-convention-verification`は存在しない。
- `PersistenceModel`に`SEPARATED`は存在しない。
- Maven POMに`mybatis-spring-boot-starter-test`は存在しない。
- `BusinessModuleRuleSet`は引き続き`MYBATIS requires PersistenceModel.SEPARATED, which is not yet provided`
  としてMyBatis宣言を拒否する。
- 変更対象は`AGENTS.md`と`docs/`配下だけであり、production / test source、POM、migration、workflowは変更していない。
- `git diff --check`はPASSした。

文書と安全柵の現状確認だけであり、code変更がないためMaven testは実行していない。

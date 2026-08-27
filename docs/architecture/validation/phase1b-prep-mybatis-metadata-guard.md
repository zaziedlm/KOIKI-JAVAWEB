# Phase 1b着手前 MyBatis metadata guard検証

## 1. 目的

グランドデザイン§11.7および全Phase共通DoD項番30に従い、
`PersistenceModel.SEPARATED`が未提供の期間に
`persistence = MYBATIS, persistenceModel = SHARED`が承認済みmetadataとして通過しないことを、
Phase 1b着手前に機械検査へ反映する。

## 2. Scopeと判断

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b着手前のPhase 1a architecture contract補正 |
| Ownership | Framework |
| 対象 | `koiki-archunit-rules`のKOIKI-ARCH-008 |
| Public API | 変更なし |
| japicmp baseline | 変更なし |
| ADR | ADR-039の既存判断を明確化。新規判断なし |
| 後続判断 | `MYBATIS`定数の維持／削除と`SEPARATED`提供時期は保留 |

## 3. 実装

- KOIKI-ARCH-008のADR参照へADR-039を追加した。
- `MYBATIS`宣言を検出した場合、`SEPARATED`必須かつ未提供であることをfailure reportへ出力する。
- `JPA + SHARED`だけを現時点の正常系として検証する。
- 既存の`metadata.rich` fixtureは`MYBATIS + SHARED`の負例として維持する。
  これにより`ModuleMetadata`のenum値読取テストを失わず、同じ宣言がrule8では拒否されることを検証する。
- `PersistenceTechnology`、`PersistenceModel`、`KoikiModule`およびPublic API inventoryは変更しない。

## 4. 検証

| 検証 | 結果 |
|---|---|
| `./mvnw -pl koiki-archunit-rules -am test` | SUCCESS。Architecture Contract 4件、ArchUnit Rules 66件、failure / error / skip 0 |
| `./mvnw clean verify` | SUCCESS。正式5-project Reactor、Architecture Contract 4件、ArchUnit Rules 66件、failure / error / skip 0 |
| `verify-public-api-fixtures.ps1` | SUCCESS。package-private変更はinventory MATCH / japicmp `NONE`、return type破壊と未承認追加は期待failure |
| `git diff --check` | SUCCESS |

新規テスト`rule8RejectsMybatisWithoutSeparatedPersistenceModel`により、failure reportへ
`KOIKI-ARCH-008`、ADR-039、`MYBATIS`、`PersistenceModel.SEPARATED`、未提供理由が含まれることを確認した。
正式なC1 timestamped baselineとのjapicmp比較は認証を必要とするためlocalでは再実行せず、PRの
`Public API Compatibility` required checkで確認する。Public API sourceと`public-api.txt`には差分がない。

## 5. Deferred decisions

- `PersistenceTechnology.MYBATIS`をPublic APIへ維持するか、破壊的変更手続きで削除するか。
- `PersistenceModel.SEPARATED`の追加とMyBatis詳細規約の正式実装。
- MyBatis Starter、Mapper配置、`@Entity`排除等のDoD 31〜34の正式実装。

これらはPhase 1bのRuntime Foundationへ先行実装せず、グランドデザインとADR-039が定める
Phase 3末尾〜Phase 4の実装検証、およびPublic API baselineのrevisit triggerで扱う。

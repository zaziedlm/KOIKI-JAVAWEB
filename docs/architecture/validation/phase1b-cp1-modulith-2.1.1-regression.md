# Phase 1b CP1 Spring Modulith 2.1.1回帰検証

## 1. 目的と判定

Phase 1aでtest scopeに限定していたSpring Modulith 2.1.0を2.1.1へ更新し、Level 0、
Feature Template、KOIKI Architecture Rulesおよびruntime依存境界が維持されるかを、
Phase 1b CP1の最初の独立commit pointとして検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP1 IN PROGRESS |
| Ownership | Framework BOM / Tooling validation |
| Source | `feature/phase1b-runtime-core`、`9483c796675b765b0c1f342fa974cb6732db1712`からのworking tree |
| Base main | `c87e7a5561dff24afea7452f63cce165c666df82` |
| 変更 | `spring-modulith.version`を2.1.0から2.1.1へ更新 |
| 判定 | **2.1.1 ADOPTED** |

検証日時は2026年8月28日 11:23〜11:27 JSTである。Starter、Customer-like Runtime Consumer、
Public APIおよびproduction runtime依存の追加とは混在させていない。

## 2. Root回帰

Command:

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Result: `BUILD SUCCESS`、16.177秒。

- Reactor 5 projectはすべてSUCCESS。
- Architecture Contractは4件、failure / error / skip 0。
- ArchUnit Rulesは66件、failure / error / skip 0。
- Java 21 build contract、Parent、BOMおよび既存正式artifactに回帰はない。

## 3. Feature Template回帰

Command:

```powershell
pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1
```

Result: `Feature Template positive, Tier-specific negative, and restore verification succeeded.`

- Tier 1 `catalog`とTier 2 `approval`の生成、unit test、KOIKI Architecture Rulesおよび
  Spring Modulith Level 0が成功した。
- Tier別ArchUnit negativeは、module metadata欠落を期待どおり拒否した。
- Tier別NullAway negativeは、non-null contract違反を期待どおり拒否した。
- fixture復元後の`clean verify`は成功した。
- runtime dependency treeに`org.springframework.modulith`が存在しないことをscriptが確認した。

negative経路内のMaven `BUILD FAILURE`は期待結果であり、script全体の終了codeは0である。

## 4. 実効依存version

Command:

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress -f build-support/feature-templates/verification/pom.xml dependency:tree '-Dscope=test' '-Dincludes=org.springframework.modulith:*'
```

Result: `BUILD SUCCESS`。

- `spring-modulith-events-api:2.1.1`は`koiki-archunit-rules`のtest scopeで解決された。
- `spring-modulith-starter-test:2.1.1`と配下の`test`、`core`、`api`、`docs`は、
  Feature Template architecture testsのtest scopeで解決された。
- 2.1.0の混在はない。

## 5. 結論と次の境界

Spring Modulith 2.1.1はPhase 1aのLevel 0契約とruntime非依存を維持するため、Phase 1b baselineへ採用する。
既存ADRの前提変更はなく、ADR追加・改訂は不要である。

CP1の次のcommit pointでは、`koiki-starter-api`、細粒度runtime fixtureおよび独立した
Customer-like Runtime Consumerを同時に成立させる。空module、仮Public APIまたはFramework内部型への
Consumer依存は追加しない。

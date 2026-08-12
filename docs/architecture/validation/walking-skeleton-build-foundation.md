# Walking Skeleton — Build Foundation Validation

**Status:** Not Started

| ID | 検証 | 期待結果 | 実結果 | 判断 / メモ |
|---|---|---|---|---|
| WS-B01 | Root Reactor / Parent / BOM | Multi-module `verify` 成功 |  |  |
| WS-B02 | Build JDK | JDK 21で成功 |  |  |
| WS-B03 | Enforcer negative | JDK 25でMaven実行すると失敗 |  |  |
| WS-B04 | Java release 21 | class major version 65 |  |  |
| WS-B05 | Java 25 runtime | Java 21成果物がJava 25で起動 |  |  |
| WS-B06 | JSpecify / NullAway | 正常コードで成功 |  |  |
| WS-B07 | NullAway negative | 意図的違反でbuild失敗 |  |  |
| WS-B08 | NullAway revert | 修正後に再度成功 |  |  |
| WS-B09 | Maven Wrapper | Wrapper経由で同一build成功 |  |  |
| WS-B10 | Container | layer extract / JRE / non-rootで起動 |  |  |

## Version Snapshot

| Item | Version |
|---|---:|
| Spring Boot | 4.1.0 |
| Maven | 3.9.16 |
| Maven Compiler Plugin | 3.15.0 |
| Maven Enforcer Plugin | 3.6.3 |
| Maven Toolchains Plugin | 3.3.0 |
| Error Prone | 2.50.0 |
| NullAway | 0.13.8 |
| JSpecify | 1.0.0 |

## Findings

### PASS

### PASS WITH CHANGE

### FAIL

## Phase 1aへ持ち込む設定

TBD

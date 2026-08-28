# Phase 1b CP0開始baseline検証

## 1. 目的と状態

Phase 1b Runtime Foundationのproduction実装前に、開始元Git identity、別PCのJava / Maven / Docker環境、
既存Phase 1a quality gatesおよびPostgreSQL container起動可否を再検証する。

| 項目 | 結果 |
|---|---|
| Phase / status | Phase 1b CP0開始準備 / COMPLETE |
| Ownership | Tooling validation |
| Source | `feature/phase1b-runtime-core`、`9483c796675b765b0c1f342fa974cb6732db1712` |
| Base main | `c87e7a5561dff24afea7452f63cce165c666df82` |
| Production change | なし |
| Gate 1 | PENDING |

## 2. Git identity

- `git fetch --prune origin`後、`origin/main`は`c87e7a5561dff24afea7452f63cce165c666df82`であった。
- branchと`origin/feature/phase1b-runtime-core`は`9483c796675b765b0c1f342fa974cb6732db1712`で一致した。
- merge baseは`c87e7a5`、`origin/main...HEAD`はleft 0 / right 1である。
- branch固有差分は引継ぎ文書とDevelopment READMEだけで、`git diff --check`は成功した。
- `git fsck --connectivity-only --no-dangling`は成功した。
- baseline scriptのpositive / negative / restore後もtracked worktreeはcleanであった。

ユーザーglobal Git ignoreへのsandbox access warningは出るが、Repositoryの履歴、差分または検証結果には
影響しなかった。

## 3. Environment identity

検証時刻は2026年8月28日 10:32〜10:44 JST、timezoneは`Tokyo Standard Time`である。

| 項目 | 実測値 |
|---|---|
| OS | Microsoft Windows 10.0.26100（Maven表示はWindows 11）、amd64 |
| Host logical CPU | 24 |
| Java 21 | Eclipse Temurin 21.0.12、runtime build 21.0.12.1+1-LTS |
| Java 25 | Eclipse Temurin 25.0.4.1+1-LTS |
| Maven Wrapper | Wrapper 3.3.4、Apache Maven 3.9.16 |
| Container frontend | Rancher Desktop |
| Docker client / server | 29.1.4-rd / 29.1.3 |
| Docker server | Linux amd64、WSL2 kernel 6.18.33.2 |
| Docker allocation | 24 CPU / 15.5 GiB |
| PostgreSQL image | `postgres:17-alpine`、local image ID `18cfe3ef5e68` |

`JAVA_HOME`と`JAVA25_HOME`は有効である。`java`自体はPowerShell PATHにないが、Maven Wrapperは
`JAVA_HOME`のJava 21を使用して正常に動作した。

## 4. Root baseline

Command:

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Result:

| 項目 | 結果 |
|---|---|
| Exit | 0 / BUILD SUCCESS |
| Total time | 15.981秒 |
| Reactor | BOM、Parent、Architecture Contract、ArchUnit Rules、Root Aggregatorの5 project |
| Architecture Contract | 4 tests、failure / error / skip 0 |
| ArchUnit Rules | 66 tests、failure / error / skip 0 |
| Java target | release 21 |

TemurinのCDS archive version差warningがfork JVMのnative streamへ出力され、Surefireがcorrupted channel
warningとdumpstreamを生成した。test failureではなくexit 0であり、同じ環境の後続検証も成功した。

## 5. Feature Template baseline

Command:

```powershell
pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1
```

Result: SUCCESS。

- Tier 1 `catalog`とTier 2 `approval`の生成およびpositive `clean verify`
- Tier別ArchUnit negativeの期待failure
- Tier別NullAway negativeの期待failure
- templateからのrestoreと再度のpositive `clean verify`
- runtime dependency treeの検査
- 最終メッセージ`Feature Template positive, Tier-specific negative, and restore verification succeeded.`

negative実行のMaven failureはscriptが要求する期待結果であり、最終process exitは0である。

## 6. NullAway baseline

Command:

```powershell
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
```

Result: SUCCESS。

- positive buildは3.000秒で成功した。
- `@Nullable`値をnon-null returnから返すnegativeはNullAwayにより期待どおりcompile failureとなった。
- restore buildは2.932秒で成功した。
- 最終メッセージ`NullAway positive, negative, and restore verification succeeded.`

## 7. PostgreSQL preflight

Phase 0の検証imageと同じ`postgres:17-alpine`を、
`koiki-phase1b-preflight-postgres`という一時container名で起動した。

1. 同名containerが存在しないことを確認した。
2. `docker run --rm --detach`で起動した。
3. 1回目の`pg_isready`は`no response`、2回目は`accepting connections`となった。
4. container stateが`running`でimageが`postgres:17-alpine`であることを確認した。
5. `docker stop`後、`--rm`により同名containerが残っていないことを確認した。

Result: `POSTGRES_PREFLIGHT=SUCCESS`。

これは別PCのcontainer起動可否を示す環境証拠であり、Phase 1bのTestcontainers実装、正式image固定、
DoD 1b-4完了または性能baselineを意味しない。

## 8. 判定

| 判定 | 結果 |
|---|---|
| Git開始点 | PASS |
| Java / Maven | PASS |
| Docker / PostgreSQL | PASS |
| Phase 1a baseline回帰 | PASS |
| CP0 environment gate | COMPLETE |
| Phase 1b Gate 1 | PENDING |

CP0実行計画とGate 1 Owner Reviewへ進める。Gate 1が`ACCEPTED`になるまではruntime module、Starter、
Public API、migration、production codeまたはCI workflowを追加しない。

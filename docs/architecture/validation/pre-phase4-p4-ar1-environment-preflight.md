# Pre-Phase 4 P4-AR1 clean-main environment preflight

## 1. Status and boundary

| Item | Result |
|---|---|
| Verification date | 2026年9月17日 |
| Status | `COMPLETE — READY FOR P4-AR2` |
| Baseline source | `21e6be4de3e485c376a05779c42114794ed4e504` |
| Source origin | PR #36 merge commit |
| Local branch at preflight start | `main` |
| Evidence branch | `docs/p4-ar1-environment-preflight` |
| Ownership | Tooling / Architecture Evidence |
| Production change | 0 |

本書は、Owner承認済みのPre-Phase 4 Framework Adoption Readiness計画に基づき、P4-AR1のclean-main
environment preflightを記録する。P4-AR2以降のFramework、ConsumerおよびReference検証を開始する前に、
source identity、Java / Maven、Docker、network / proxy、credential、IDEおよびresource状態を確認する。

本preflightはPhase 4 production実装、Public API変更、artifact配布、snapshot publish、workflow変更または
実案件repository操作を行わない。

## 2. Source and remote baseline

| Check | Result |
|---|---|
| PR #36 | `MERGED` |
| Final PR HEAD | `59739fe4e6f40d6ec8908befe03128b399e67ce4` |
| Merge commit | `21e6be4de3e485c376a05779c42114794ed4e504` |
| Local `main` | merge commitと一致 |
| `origin/main` | merge commitと一致 |
| Worktree at preflight start | clean |
| CI | run [35202154793](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35202154793) `SUCCESS`（7 / 7 jobs） |
| Java Runtime Compatibility | run [35202154787](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/35202154787) `SUCCESS`（2 / 2 jobs） |

main merge後CIとJava Runtime Compatibilityの全job成功を確認し、source baselineとlocal preflight結果を確定した。

## 3. Local development environment

| Area | Observed result | Disposition |
|---|---|---|
| OS | Windows 11 / amd64 | supported local development environment |
| Java build | Temurin 21.0.12.1 | PASS |
| Java runtime compatibility | Temurin 25.0.4.1 | PASS |
| `JAVA_HOME` | 設定済み、Java 21.0.12.1 | PASS |
| `JAVA21_HOME` | 設定済み、Java 21.0.12.1 | PASS |
| `JAVA25_HOME` | 設定済み、Java 25.0.4.1 | PASS |
| Maven Wrapper | Wrapper 3.3.4 / Maven 3.9.16 / `bin` distribution | PASS |
| PowerShell | 7.6.6 | PASS |
| Git | 2.55.0.windows.3 | PASS |
| VS Code | 1.138.0 / x64 | PASS |
| Docker | Rancher Desktop client / server 29.5.3、Linux x86_64、6 CPU、約16 GB | PASS |
| PostgreSQL 17 image | `postgres:17-alpine` local imageあり、registry manifest取得成功 | PASS |

Maven toolchains fileは存在しないが、承認済みverification scriptとCIが使用する`JAVA_HOME`、`JAVA21_HOME`、
`JAVA25_HOME`は設定済みで、各Java executableのfeature versionを直接確認した。P4-AR2ではこの環境変数方式を使用し、
未承認のtoolchains設定を追加しない。

## 4. Network, TLS, proxy and credential boundary

| Check | Result | Disposition |
|---|---|---|
| Maven Central HTTPS | HTTP 200 | PASS |
| GitHub API HTTPS | HTTP 200 | PASS |
| GitHub Packages unauthenticated endpoint | HTTP 404 | TLS到達済み。artifact取得可否の証拠にはしない |
| Docker Hub manifest | `postgres:17-alpine` manifest取得成功 | PASS |
| Maven / Java TLS | 空のOS temp Maven repositoryでHelp Plugin 3.5.1を取得・実行 | PASS |
| Maven isolated result | root version `0.1.0-SNAPSHOT`、BUILD SUCCESS | PASS |
| `HTTP_PROXY` / `HTTPS_PROXY` / `NO_PROXY` | process環境では未設定 | direct TLS pathを確認 |
| `KOIKI_PACKAGES_TOKEN` | 未設定 | P4-AR2 Public API local比較の明示precondition |

Maven / Java TLS確認では通常の`~/.m2`を使用せず、GUID付きOS temp directoryを`maven.repo.local`に指定した。
実行後に専用directoryを削除し、残存0を確認した。これにより既存local cacheだけによる成功ではなく、現在のJDK / Maven
経路からMaven Centralへ接続してpluginを取得できることを確認した。

Public API compatibilityのlocal実行には`read:packages`だけを持つPAT classicが必要である。現在のGitHub CLI認証を
当該credentialとして自動流用しない。P4-AR2で実行する前に、secure promptまたは対象process限定の
`KOIKI_PACKAGES_TOKEN`で供給する。credential未供給時は当該検査を成功扱いしない。

## 5. IDE inventory

Java / Maven開発に関係するVS Code extension directoryとして次を確認した。

- `pleiades.java-extension-pack-jdk-2026.624.0`
- `redhat.java-1.57.2026091208-win32-x64`
- `redhat.vscode-xml-0.29.3-win32-x64`
- `vmware.vscode-spring-boot-2.4.0`
- `vscjava.vscode-maven-0.45.2026081903`
- `vscjava.vscode-spring-boot-dashboard-0.14.0`

Pleiades Java global storageには17、21、25およびlatestのJDK directoryが存在する。P4-ARではbuild / runtimeの
正本をcommand lineで確認したJDK 21 / 25とし、IDEが選ぶJREとMaven実行JDKを同一と推測しない。

## 6. Resource and cleanup state

| Resource | Result |
|---|---|
| Running Docker containers | 0。Ownerが不要な別案件のPostgreSQL 15 container 2件を事前停止済み |
| Port 18080 | free |
| Port 55432 | free |
| Workspace drive free space | C: 約59.9 GiB free |
| Maven probe temp repository | removed / residual 0 |
| Tracked source mutation by preflight | 0 |

別案件containerはP4-AR所有resourceではないため、Framework側から停止・削除していない。Ownerが不要と確認して停止した後、
P4-AR側ではread-onlyにrunning container 0を確認した。停止済みcontainer、volumeまたはimageは削除していない。

## 7. Findings and next boundary

| ID | Classification | Finding | Treatment |
|---|---|---|---|
| AR1-F1 | Resolved baseline condition | merge後`main` CI 7 / 7 jobsとJava Runtime Compatibility 2 / 2 jobsが成功 | remote baseline確定。追加rerun不要 |
| AR1-F2 | Credential precondition | `KOIKI_PACKAGES_TOKEN`未設定 | P4-AR2 Public API local比較前にread:packages-only PATをsecureに供給する |
| AR1-F3 | Nonblocking environment observation | Maven toolchains fileなし | 承認済み環境変数方式を維持し、無断で設定を追加しない |

local environmentに、P4-AR2開始を妨げる機能上のblockerは検出していない。AR1-F1は解消し、
P4-AR2 Framework build / quality baselineへ進める。AR1-F2はP4-AR2全体の開始条件ではなく、同工程内の
timestamped baselineとのPublic API実比較を開始する直前までに満たす。

## 8. P4-AR2 execution readiness

P4-AR2は、次の順序でWindows clean-main baselineを検証する。これは実行準備の固定であり、本preflight中に
P4-AR2のPASS判定またはFramework変更を先行させない。

| Order | Verification | Command / condition | Credential | Cleanup / stop condition |
|---|---|---|---|---|
| 1 | Source identity | commit、branch、`git status`を再確認 | 不要 | tracked source差分があれば停止 |
| 2 | Root Reactor | `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify` | 不要 | build failure時は後段を成功扱いしない |
| 3 | Feature Template | `pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1` | 不要 | script所有のtemp / generated resourceを確認 |
| 4 | NullAway | `pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1` | 不要 | negative fixtureの期待failureを通常failureと混同しない |
| 5 | Public API fixture | `pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-fixtures.ps1` | 不要 | fixture JAR、隔離repository、reportの削除を確認 |
| 6 | Public API baseline | `pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-compatibility.ps1` | `read:packages`だけを持つPAT classic | credential未供給・scope不適合・baseline取得不能をPASSにしない |
| 7 | Runtime build | `pwsh -NoProfile -File build-support/runtime-compatibility-fixture/build-runtime-fixture.ps1` | 不要 | Java 21 buildとmanifest生成を確認 |
| 8 | Runtime Java 21 | `pwsh -NoProfile -File build-support/runtime-compatibility-fixture/verify-runtime-fixture.ps1 -ExpectedJavaFeature 21` | 不要 | manifest、class major、hashを照合 |
| 9 | Runtime Java 25 | `pwsh -NoProfile -File build-support/runtime-compatibility-fixture/verify-runtime-fixture.ps1 -ExpectedJavaFeature 25` | 不要 | 同一artifactのhashを照合 |
| 10 | Runtime guards | `pwsh -NoProfile -File build-support/runtime-compatibility-fixture/verify-runtime-negative-guards.ps1` | 不要 | Java 25 build、hash改変、major不一致の期待failureを確認 |
| 11 | Final hygiene | `git status`、Docker container、port、temp残存を再確認 | 不要 | tracked source変更または所有resource残存があれば未完了 |

実行結果はP4-AR2の独立Evidenceへ記録する。Root Reactor、Template、NullAway、Public API fixtureおよびruntime
compatibilityはcredentialなしで実行できるため、Public API baseline用PATの準備と切り離して進められる。ただし、
Public API baseline比較を省略した状態ではP4-AR2全体を完了扱いにしない。

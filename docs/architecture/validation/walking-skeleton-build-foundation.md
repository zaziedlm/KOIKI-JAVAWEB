# Walking Skeleton — Build Foundation Validation

**Status:** Completed

| ID | 検証 | 期待結果 | 実結果 | 判断 / メモ |
|---|---|---|---|---|
| WS-B01 | Root Reactor / Parent / BOM | Multi-module `verify` 成功 | PASS | BOM → Parent → smoke modulesを含む5 projectで成功 |
| WS-B02 | Build JDK | JDK 21で成功 | PASS | Temurin 21.0.12、EnforcerとToolchain選択成功 |
| WS-B03 | Enforcer negative | JDK 25でMaven実行すると失敗 | PASS | Temurin 25.0.4で終了コード1、`RequireJavaVersion`が拒否 |
| WS-B04 | Java release 21 | class major version 65 | PASS | `GreetingService.class`でmajor version 65 |
| WS-B05 | Java 25 runtime | Java 21成果物がJava 25で起動 | PASS | Temurin 25.0.4でBoot JAR起動、smoke message出力、終了コード0 |
| WS-B06 | JSpecify / NullAway | 正常コードで成功 | PASS WITH CHANGE | Error Proneをforked javacで実行して成功 |
| WS-B07 | NullAway negative | 意図的違反でbuild失敗 | PASS | `return null`を検出し終了コード1、検証後に正常コードへ復元 |
| WS-B08 | NullAway revert | 修正後に再度成功 | PASS | 正常コード復元後、全5 projectの`clean verify`が終了コード0 |
| WS-B09 | Maven Wrapper | Wrapper経由で同一build成功 | PASS | Wrapper 3.3.4 / Maven 3.9.16で`clean verify`成功 |
| WS-B10 | Container | layer extract / JRE / non-rootで起動 | PASS | Rancher Desktop Mobyでbuild・起動成功、JRE 21、UID/GID 999、終了コード0 |

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

- 2026-08-12: JDK 21でRoot Reactor / BOM / Parent / smoke library / smoke applicationの`clean verify`が成功。
- 2026-08-12: 生成された`GreetingService.class`のmajor versionが65であることを確認。
- 2026-08-12: 公式Maven Wrapper 3.3.4を`only-script`方式で生成し、Maven 3.9.16で同一buildが成功。
- 2026-08-12: Maven 3.9.16をTemurin 25.0.4で実行すると、ParentのEnforcer `RequireJavaVersion`が終了コード1でbuildを拒否。smoke modulesはコンパイル前にSKIPされた。
- 2026-08-12: Temurin 21.0.12で生成したSpring Boot JARをTemurin 25.0.4の`java.exe -jar`で直接起動。`Started SmokeApplication`と`KOIKI Walking Skeleton: hello Spring Boot`を確認し、終了コード0。
- 2026-08-12: `@NullMarked`配下の`GreetingService.greeting`を一時的に`return null`へ変更。NullAwayが`returning @Nullable expression from method with @NonNull return type`として検出し、終了コード1。検証後に正常コードへ復元。
- 2026-08-12: 正常コード復元後、Temurin 21.0.12とMaven Wrapper 3.9.16で全5 projectの`clean verify`が再び成功し、終了コード0。NullAwayのPASS → FAIL → PASSを完了。
- 2026-08-12: Rancher Desktop Moby 29.5.3（Linux / amd64）で`Dockerfile.ws`からimage build成功。Spring Boot toolsによる`dependencies` / `spring-boot-loader` / `snapshot-dependencies` / `application`のlayer extractと個別COPYを確認。
- 2026-08-12: Runtime imageはTemurin JRE 21.0.11で`javac`なし。image metadataと実行時の両方でnon-rootユーザー`koiki`（UID/GID 999）を確認。
- 2026-08-12: imageの既定ENTRYPOINTでcontainerを起動し、`Started SmokeApplication`と`KOIKI Walking Skeleton: hello Spring Boot`を確認。終了コード0、`--rm`後の残存containerなし。
- 2026-08-12: PC再起動後、Windows PowerShellの通常の`docker build` / `docker run`からWS-B10を再実施。layer extract、JRE-only、non-root、アプリ起動、終了コード0を再確認。

### PASS WITH CHANGE

- 初回compileではError ProneがJDK compiler moduleへアクセスできず`IllegalAccessError`となった。
- `maven-compiler-plugin`へ`<fork>true</fork>`を追加した。既存の`-J--add-exports` / `-J--add-opens`を外部javacへ適用するために必要だった。

### FAIL

### Environment Notes

- 初回はRancher DesktopのWindows側Docker named pipeが`timed out dialing Hyper-V socket`となったため、管理VM内のMoby engineへ`rdctl shell docker ...`で接続して検証した。
- PC再起動後にnamed pipeは復旧し、Windows PowerShellからServer情報を取得できた。通常の`docker`コマンドでもWS-B10が再度PASSしたため、初回の接続障害はDockerfileまたはimageの不成立ではなく、一時的な環境問題と判断する。

## Phase 1aへ持ち込む設定

- Root Reactor / BOM / Parentの責務分離を継続する。
- Build JDK 21をEnforcerで強制し、target release 21を維持する。
- Error Prone / NullAwayはforked javacで実行する。
- Container候補はlayer extract、JRE runtime、non-root実行を維持する。正式base imageとsecurity設定はPhase 1aで判断する。

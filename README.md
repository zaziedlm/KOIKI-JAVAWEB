# KOIKI-JavaWeb-FW — Phase 0 Walking Skeleton

この構成は、`KOIKI-JavaWeb-FW グランドデザイン v0.2` と
`Walking Skeleton 実装計画 v1.0` に基づく、**捨てる前提の実装検証環境**です。
2026年8月14日に全検証項目の回答と最終再実行を完了しました。

## 検証結果

- Root Reactor / Parent / BOM、Java 21 Build Contract、NullAway
- Java 21成果物のJava 25 runtime起動
- コンテナのレイヤ抽出、JRE、非root実行
- ArchUnit主要26規則と外部Consumerへの配布
- FlywayのKOIKI / Customer二階層
- Tier 2実務感、OSIV境界、同期Domain Event
- OpenSpecワークフローと最小Agent Skills

Walking Skeletonの完了判定とPhase 1aへの引継ぎ境界は
`docs/architecture/validation/walking-skeleton-phase0-completion.md`を参照してください。
Phase 0 Architecture Baseline全体の完了判定は
`docs/architecture/KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md`に記録しています。
個別の実装証拠は`docs/architecture/validation/`に記録しています。

## 重要

`walking-skeleton/` 以下は正式な `koiki-reference-app` ではありません。
Javaクラス、Template、migration SQLを正式コードへ直接移植しません。
設定値、規約実装で得た知見、検証記録だけをPhase 1aの正式構成へ反映します。

## 一時 Maven 座標

Walking Skeleton の名前を正式仕様化しないため、一時 namespace を使用します。

```text
dev.koiki.walkingskeleton
```

正式な `groupId` / Java base package は `org.koikifw` に決定しています。
上記の一時namespaceはWalking Skeletonの検証コードだけで使用し、
Phase 1aの正式成果物へは引き継ぎません。

## 前提

- JDK 21
- Java 25（runtime compatibility test 用）
- Maven 3.9.16
- Docker / OCI-compatible container engine（コンテナ検証を行う場合）

## 再検証

### Root Reactor

```powershell
.\mvnw.cmd clean verify
```

Maven自体もJava 21で実行します。`koiki-parent`のEnforcerがJava 21以外を拒否します。

### V2 / V7

```powershell
.\walking-skeleton\ws-flyway-two-tier\verify-flyway-two-tier.ps1
.\walking-skeleton\archunit-external-consumer\verify-external-consumer.ps1
```

V7の外部Consumerは意図的なArchUnit違反で失敗し、検証スクリプト全体は成功するのが期待値です。

### Java runtime

```powershell
.\build-support\scripts\verify-class-version.ps1
.\build-support\scripts\run-with-java25.ps1
```

Java 21のclass major version `65`と、Java 25 runtimeでの起動を確認します。

PowerShellの実行ポリシーで停止する場合は、現在のプロセスに限定して
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File <script>`を使用します。

## NullAway negative test

正常系コードは最初からbuildが通る状態を意図しています。
意図的違反の例は `walking-skeleton/negative-tests/nullaway/README.md` にあります。

1. `mvn clean verify` → PASS
2. READMEの違反コードを一時的に適用
3. `mvn clean verify` → FAIL
4. 元に戻す
5. `mvn clean verify` → PASS

### Container

最初にJARを作成します。

```powershell
.\mvnw.cmd clean package
docker build -f walking-skeleton/ws-smoke-app/Dockerfile.ws -t koiki-ws-smoke .
docker run --rm koiki-ws-smoke
```

このDockerfileは検証用です。正式なReference Application用Dockerfileではありません。

## 検証Version Snapshot

| 項目 | Version |
|---|---:|
| Spring Boot | 4.1.0 |
| Maven | 3.9.16 |
| Maven Compiler Plugin | 3.15.0 |
| Maven Enforcer Plugin | 3.6.3 |
| Maven Toolchains Plugin | 3.3.0 |
| Error Prone | 2.50.0 |
| NullAway | 0.13.8 |
| JSpecify | 1.0.0 |

これらは **Walking Skeletonの検証用固定値** であり、KOIKIの恒久固定値ではありません。

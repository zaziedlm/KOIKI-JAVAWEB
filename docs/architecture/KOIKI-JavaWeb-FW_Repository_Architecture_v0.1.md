# KOIKI-JavaWeb-FW Repository Architecture v0.1

## 1. 目的

Repository Architectureでは「何をどこが所有するか」と
「何を混ぜてはならないか」を先に固定する。

Root Reactor / Parent / BOM、初期Maven Module、Package Naming、
Flyway所属等の詳細はWalking Skeletonで試した後に確定する。

## 2. Repository Scope

KOIKI本体はFramework / Starter / Testing / Architecture Rules /
Reference Application / Project Template / Migration Support /
Documentation / Build / Operations Supportを管理する。

Customer Applicationは別Repositoryとし、KOIKIをMaven成果物として利用する。

## 3. Canonical Top-level Structure

```text
koiki-javaweb-fw/
├── koiki-parent/
├── koiki-dependencies-bom/
├── koiki-framework/
├── koiki-starters/
├── koiki-testing/
├── koiki-archunit-rules/
├── koiki-migration-recipes/
├── koiki-reference-app/
├── koiki-project-template/
├── docs/
│   └── agent/skills/
├── ops/
└── build-support/
```

これはLogical / Target Structureであり、空Directoryを一括生成しない。

## 4. Ownership

- Framework: 複数案件へ提供する正式なKOIKI成果物
- Reference Application: 正規利用例・統合試験・Smoke Test・AI参照実装
- Customer Application: 別Repository。顧客固有業務をKOIKIへ持ち込まない
- Tooling: Testing / ArchUnit Rules / Migration / Build Support

## 5. Public API

基本方式:

```text
Public:   org.koikifw.<module>....
Internal: org.koikifw.<module>.internal....
```

`internal`参照禁止はArchUnitで機械検査する。
重要SPIのみ必要に応じて `-api` / `-impl` 分割する。
JPMSは採用しない。

正式なMaven `groupId`とJava base packageは、KOIKIが保有する
`koikifw.org`の逆ドメイン名である`org.koikifw`とする。

別プロジェクトで使用している`org.koikifw.libkoiki.batch`は、
バッチ実行基盤固有のnamespaceであり、本Repositoryのpackage階層には持ち込まない。

Walking Skeletonの`dev.koiki.walkingskeleton`は検証用の一時namespaceとして維持し、
Phase 1aの正式成果物へは引き継がない。

## 6. Dependency Principle

```text
Customer Application
        ↓
KOIKI Starter / Public API
        ↓
KOIKI Framework
        ↓
Spring
```

Reference ApplicationからKOIKIへの依存は許可する。
FrameworkからReference / Customerへの依存は禁止する。

## 7. Empty Structure Policy

将来必要になる可能性だけで空Module / Packageを作らない。

## 8. Walking Skeleton

次を実装検証してからPhase 1aの正規構成を決める。

- Reactor / Parent / BOM
- Java Build Contract
- Null Safety
- ArchUnit
- Flyway
- Tier 1 / Tier 2
- Spring Modulith
- Container
- Agent Skills
- OpenSpec試行

Walking Skeleton codeは捨てる。
設定・規約・知見のみPhase 1aへ持ち込む。

# KOIKI-JavaWeb-FW Baseline Compatibility

**版:** v0.1<br>
**作成日:** 2026年8月19日<br>
**状態:** ACCEPTED<br>
**Architecture Owner:** Shuichi Kataoka<br>
**対象:** KOIKI / Spring Boot / Java baselineとsupport管理<br>
**基準Commit:** `8d90ea1`

## 1. 目的

KOIKIの各release lineが対応するSpring Boot line、Java target・runtime、およびsupport状態を、
Repositoryから一意に確認できるようにする。本書をグランドデザイン§8.1が要求する
「KOIKI ⇔ Spring Boot ⇔ Java ベースライン対応表」の正本とする。

本書はversionを自動的に最新へ追従させるものではない。Phaseまたはrelease単位で固定し、
検証証拠とArchitecture Ownerの承認を伴って更新する。

## 2. 正本と実効設定

| 項目 | 正本または実効設定 |
|---|---|
| 公開するcompatibilityとsupport状態 | 本書 |
| Spring Boot・Spring Modulith等のdependency version | `../../koiki-dependencies-bom/pom.xml` |
| Java release、build JDK、build plugin | `../../koiki-parent/pom.xml` |
| Maven実行version | `../../.mvn/wrapper/maven-wrapper.properties` |
| 技術方針 | `grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §6、§8 |
| 判断と承認 | `governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md` |

本書とPOM・Wrapperが不一致の場合、どちらかへ黙って追従しない。不一致をrelease blockerとして扱い、
実効dependency、Java runtime matrix、build結果を確認してから同じ変更で整合させる。

## 3. 初期Baseline

### 3.1 KOIKI / Spring Boot / Java対応表

| Baseline | KOIKI line | Spring Boot | Java target bytecode | Build JDK | 対応runtime | 推奨runtime | 状態 |
|---|---|---|---:|---|---|---|---|
| Phase 0 development | 未公開（`0.0.1-SNAPSHOT`） | 4.1.0 | 21 | 21 | Java 21 | Java 25（互換確認対象） | DEVELOPMENT |

`0.0.1-SNAPSHOT`はWalking Skeletonの候補Parent / BOMを識別する開発versionであり、
正式なKOIKI release、公開Maven座標、またはsupport対象lineではない。
正式な`org.koikifw`成果物とrelease versionはPhase 1以降で確定する。

### 3.2 再現用Component Snapshot

| Component | Version / 条件 | 根拠 |
|---|---|---|
| Spring Boot | 4.1.0 | `koiki-dependencies-bom/pom.xml` |
| Spring Modulith | 2.1.0 | `koiki-dependencies-bom/pom.xml` |
| Maven | 3.9.16 | Maven Wrapper |
| Maven Compiler Plugin | 3.15.0 | `koiki-parent/pom.xml` |
| Maven Enforcer Plugin | 3.6.3 | `koiki-parent/pom.xml` |
| Maven Toolchains Plugin | 3.3.0 | `koiki-parent/pom.xml` |
| Error Prone | 2.50.0 | `koiki-parent/pom.xml` |
| NullAway | 0.13.8 | `koiki-parent/pom.xml` |
| JSpecify | 1.0.0 | `koiki-dependencies-bom/pom.xml` |

Component Snapshotはbaselineを再現するための補助情報であり、すべてのcomponent更新が
KOIKI major更新を要求するものではない。Spring Boot minorの変更はKOIKI major更新として扱い、
build pluginや検査toolは互換性と検証結果を確認したうえで同一KOIKI line内でも更新できる。

Spring Modulith 2.1.0はPhase 0実装の実効dependencyを示す。versionの記載だけを根拠として、
Level 2以降の機能、runtime依存、または運用方式を正式採用したとは扱わない。

### 3.3 検証証拠

| 検証 | 結果 | 証拠 |
|---|---|---|
| JDK 21 build | Temurin 21.0.12で成功 | `validation/walking-skeleton-build-foundation.md` |
| Java 21 bytecode | class major version 65 | `validation/walking-skeleton-build-foundation.md` |
| Java 21 runtime | Root Reactorおよびcontainerで成功 | `validation/walking-skeleton-phase0-completion.md` |
| Java 25 runtime compatibility | Temurin 25.0.4でBoot JAR起動、終了コード0 | `validation/walking-skeleton-build-foundation.md` |
| Maven Wrapper | Maven 3.9.16で`clean verify`成功 | `validation/walking-skeleton-build-foundation.md` |

Java 25はPhase 0時点で推奨runtimeの互換確認対象であり、build JDKではない。
Java 21で生成したbytecodeがJava 21とJava 25の双方で動作する契約を維持する。

**Owner Review（§1〜§3）:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

## 4. Support対応表

| KOIKI line | Release日 | 対応Spring Boot line | Spring Boot OSS support終了日 | KOIKI OSS support終了日 | 商用延長 | 状態 | 確認日・根拠 |
|---|---|---|---|---|---|---|---|
| Phase 0 development | 未release | 4.1.x development baseline | 対象外 | 対象外 | 対象外 | DEVELOPMENT | Phase 0実装証拠（2026年8月14日） |

Phase 0 development baselineには顧客向けsupport期間を設定しない。Phase 5 DoD 5-3で、
正式releaseごとにSpring公式情報を確認し、具体的な終了日、確認日、参照元を記録する。
「LTS」という包括的表現は使用せず、OSS supportと商用延長supportを分ける。

KOIKI OSS support終了日は、対応するSpring Boot minorの公式OSS support終了日を上限とし、
固定月数から独自に算出しない。最新lineと直前lineを管理対象としても、対応Spring Bootの
OSS support終了後まで無条件にsecurity patchまたは重大bug修正を提供することを意味しない。

商用延長supportは、顧客が有効な商用support契約とprivate repositoryの利用権を持ち、
KOIKIがそのrepositoryからBOM・Starterのdependency解決とbuildを検証した場合に限り`EXTENDED`とする。
契約の存在だけでは状態を変更せず、商用artifactをKOIKIから再配布しない。

最新lineと直前line以外も対応履歴として削除せず、support終了後は`EOL`へ更新して表に残す。

**Owner Review（§4）:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

## 5. 更新契機

| 契機 | 必須確認 |
|---|---|
| 各Phaseの完了判定 | OSS support中のSpring Boot minorか、POM・runtime matrix・本書が一致するか |
| KOIKI major release | 対応Spring Boot minor、Java baseline、support終了日、移行対象line |
| 同一KOIKI majorのminor / patch release | Spring Boot minorを跨いでいないか、patch追従の互換証拠があるか |
| Spring BootまたはJava方針の変更 | ADR、Grand Design、Public API、migrationへの影響 |
| Spring公式support日または商用条件の変更 | 終了日、確認日、参照元、顧客通知への影響 |
| 重大脆弱性・利用不能・artifact取得不能 | 緊急性、影響line、代替version、互換性、顧客通知、再判定条件 |
| 四半期Architecture Review | baselineの陳腐化、第三者library、未完了の互換検証 |

Phase途中では原則としてbaselineを変更しない。重大な脆弱性や利用不能など、変更を待てない場合は
Governanceに従って例外理由、影響、検証証拠、再判定条件を記録する。緊急更新は通常の待機期間を
短縮できるが、互換検証とArchitecture Ownerの承認を省略しない。

正式releaseまたは外部supportを開始する場合は、baseline更新と同時にGovernanceの代理者・継続性、
support運用、および顧客通知経路を見直す。

## 6. 更新手順

1. Architecture Ownerが更新契機と対象KOIKI lineを確認する。
2. Spring公式情報、Java release情報、商用repository等の一次情報を確認し、確認日を記録する。
3. BOM、Parent、Wrapperで候補versionを固定し、未承認候補を`PLANNED`として扱う。既存の`SUPPORTED`行は上書きしない。
4. Root Reactor、外部consumer、Java runtime matrix、および該当Phaseのintegration testを実行する。
5. Public API、migration、support window、第三者libraryへの影響を確認する。
6. 本書、POM、ADRまたはGrand Design、release noteを同じ変更候補として整合させる。
7. Architecture Ownerが候補をreviewし、`Decision`、`Evidence`、`Rationale`、`Decided by`、`Date`、
   `Revisit trigger`を変更履歴へ記録する。
8. 承認後に候補を有効化し、必要な行を`SUPPORTED`、`EXTENDED`または`EOL`へ更新する。

一人projectではArchitecture Ownerが実装者とreviewerを兼ねてもよいが、検証結果と判断記録を省略しない。

## 7. 状態の定義

| 状態 | 意味 |
|---|---|
| DEVELOPMENT | 未公開の開発baseline。顧客support対象外 |
| PLANNED | release候補。検証または承認が未完了 |
| SUPPORTED | 公開済みで、明示したOSS support期間内 |
| EXTENDED | 商用artifact構成でKOIKIのbuild検証を継続するline。一般公開supportではなく、契約とrepository利用権を持つ顧客だけが対象 |
| EOL | support終了。新規採用せず更新を促すline |

基本的な状態遷移は次のとおりとする。

```text
DEVELOPMENT -> PLANNED -> SUPPORTED -> EOL
                              |
                              +-> EXTENDED -> EOL
```

商用延長を適用しないlineは`SUPPORTED`から`EOL`へ移行する。公開済みlineを`DEVELOPMENT`または
`PLANNED`へ戻さない。終了済みlineを再提供する必要がある場合は、新しいKOIKI lineとして評価する。

### 7.1 変更履歴

| Date | KOIKI line | Transition | Decision | Evidence | Rationale | Decided by | Revisit trigger |
|---|---|---|---|---|---|---|---|
| 2026年8月19日 | Phase 0 development | New → DEVELOPMENT | ACCEPTED | `validation/walking-skeleton-build-foundation.md`、`validation/walking-skeleton-phase0-completion.md` | POM・Wrapperの実効versionとJava 21 / 25検証結果を未公開baselineとして記録する | Shuichi Kataoka | Phase完了、Spring Boot / Java方針変更、support条件変更 |

**Owner Review（§5〜§7）:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

## 8. Owner Review観点

- 正本path、更新責任、更新契機、review手順が一意か。
- 未公開development baselineを正式releaseと誤認しないか。
- POM・Wrapperの実効versionと対応表が一致しているか。
- Java target、build JDK、runtime、推奨runtimeを混同していないか。
- support終了日を推測せず、release時の一次情報と確認日を記録する設計か。
- Phase 5 DoD 5-3へ具体的な日付公開を正しく保留しているか。
- 未承認・未検証の候補を`PLANNED`のまま管理し、`SUPPORTED`として公開していないか。
- 状態遷移と変更履歴にGovernanceの必須項目が記録され、公開済みlineを開発状態へ戻していないか。
- `EXTENDED`を一般公開supportまたは商用artifactの再配布と誤認させず、契約、repository利用権、build検証を条件としているか。
- 緊急更新でも互換性検証とArchitecture Ownerの承認を省略していないか。
- 本書、BOM、Parent、Wrapper、ADRまたはGrand Designを同じ変更候補で整合させる設計か。
- `EOL`のlineを削除せず、対応履歴として保持する設計か。

## 9. 参照

- `grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §6、§8、§27.3、§27.9、§31
- `adr/README.md` ADR-001、ADR-002、ADR-003、ADR-017
- `governance/KOIKI-JavaWeb-FW_Architecture_Governance_v0.1.md`
- `KOIKI-JavaWeb-FW_Phase0_DoD_Closeout_v0.1.md`
- `validation/walking-skeleton-build-foundation.md`
- `validation/walking-skeleton-phase0-completion.md`
- `../../koiki-dependencies-bom/pom.xml`
- `../../koiki-parent/pom.xml`
- `../../.mvn/wrapper/maven-wrapper.properties`

Spring BootおよびJavaのsupport条件は、本節の記載を固定値として流用しない。各baseline更新時に
一次情報を改めて確認し、参照元と確認日をSupport対応表または変更履歴へ記録する。

**Owner Review（§8〜§9）:** ACCEPTED（2026年8月19日、Shuichi Kataoka）

## 10. Owner Review Result

| 項目 | 判定 |
|---|---|
| 対象 | `KOIKI-JavaWeb-FW_Baseline_Compatibility_v0.1.md`全体 |
| Decision | ACCEPTED |
| Evidence | 本書§1〜§9のOwner Review、`koiki-dependencies-bom/pom.xml`、`koiki-parent/pom.xml`、Maven Wrapper、`validation/walking-skeleton-build-foundation.md`、`validation/walking-skeleton-phase0-completion.md` |
| Rationale | Phase 0の未公開development baselineと実効設定が一致し、正式releaseと誤認しない境界、support状態、更新契機、承認手順、およびPhase 5へ保留する具体的support終了日の範囲が明確である |
| Decided by | Shuichi Kataoka |
| Date | 2026年8月19日 |
| Revisit trigger | Phase完了、正式release準備、Spring BootまたはJava方針変更、support条件変更、重大脆弱性・利用不能・artifact取得不能、四半期Architecture Review |

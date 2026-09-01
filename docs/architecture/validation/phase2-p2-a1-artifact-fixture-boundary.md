# Phase 2 P2-A1 artifact and fixture boundary

## 1. Status and scope

- **Validation date:** 2026年9月1日
- **Work package:** `P2-A1 / A1-0〜A1-1`
- **Status:** `A1-0 COMPLETE / A1-1 OWNER REVIEWED`
- **Baseline HEAD:** `d30fa7fb9504305e1324099d5be453dffbbd8764`
- **Production code / dependency / module change:** 0
- **Workflow / remote environment / secret change:** 0

本記録は、P2-A1のproduction実装を始める前に、正式成果物`koiki-starter-security`と
非配布Security verification fixtureのOwnership、配置、release unit、Consumerおよび検証入口への影響を確定する。
Auto Configuration class、property、matcherおよびCustomer overrideの具体契約はT0 / T1 Evidenceより前に固定しない。

## 2. A1-0 preflight evidence

2026年9月1日にRepository Maven Wrapperを使用して次を確認した。

| Item | Result |
|---|---|
| Branch | `feature/phase2-security-foundation`。`origin/feature/phase2-security-foundation`をtracking |
| HEAD | `d30fa7f` (`docs: add P2-A1 cross-PC start handoff`) |
| Java | Eclipse Temurin 21.0.12 LTS。`JAVA_HOME`からWrapperが使用 |
| Maven Wrapper | Wrapper 3.3.4、Apache Maven 3.9.16、`distributionType=bin` |
| Root baseline | `./mvnw --batch-mode --no-transfer-progress verify`が10 / 10 projectsで`BUILD SUCCESS` |
| Test result | Architecture Contract 4件、ArchUnit Rules 66件、計70件がfailure / error / skip 0 |
| Existing worktree item | 未追跡のSpring事前学習計画v1.1が存在。P2-A1では変更しない |

Surefireの`dumpstream`警告は、使用JDKのCDS shared archiveが別buildで作られたというJVM警告であり、
test failureまたはKOIKI sourceの不整合ではない。

## 3. Existing implementation inventory

### 3.1 Formal Framework release unit

Root `pom.xml`はAggregator専用で、BOM、Parent、Architecture Contract、ArchUnit Rules、Starter 4件、
Testing Supportを集約する。現行root buildはAggregatorを含む10 projects、deploy可能な正式成果物は9 artifactsである。

既存Starterは次の規則を共有する。

- `koiki-starters/koiki-starter-<area>`をCanonical ownership locationとする。
- `koiki-parent`を親とし、`koiki-dependencies-bom`経由でSpring Boot 4.1.1のversion authorityを使用する。
- Auto Configurationと実装classは`org.koikifw.starter.<area>.internal`へ閉じる。
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`で登録する。
- `public-api.txt`で公開Java型、公開configuration property、internal packageをinventoryする。
- 重要SPIが実証されない限り`-api` / `-impl`へ分割しない。

### 3.2 Tooling and Consumer

`build-support/runtime-foundation-verification`と`build-support/runtime-foundation-consumer`は、root reactor外の
Tooling-owned Maven buildである。検証scriptは正式release unitを空の隔離Maven repositoryへstageし、
Framework source reactorに依存しないCustomer-like Consumerをbuildする。

`koiki-testing`は正式配布artifactであるが、現時点ではSecurity test dependencyやSecurity fixture utilityを所有しない。
P2-A1だけを理由に同artifactへSecurity依存を追加しない。

### 3.3 Current fixed inventories

- `build-support/runtime-foundation-verification/verify-cp10-closeout.ps1`はPhase 1bで承認した10 projectsを
  exact matchとして検証する。
- `.github/workflows/ci.yml`の`Milestone C Closeout`は上記CP10 scriptを現在も実行する。
- `.github/workflows/publish-snapshot.yml`はPhase 1bの9 deployable artifactsを明示選択する。
- 現行Public API compatibilityのpublished baselineはArchitecture Contract / ArchUnit Rulesを対象とする。
  Starterの公開型0とproperty inventoryは各Starterの`public-api.txt`およびRuntime Foundation verificationで検査する。

## 4. Approved placement for P2-A1 implementation proposal

### 4.1 Formal Security Starter

| Item | Placement / rule |
|---|---|
| Owner | Framework |
| Directory | `koiki-starters/koiki-starter-security/` |
| Maven coordinate | `org.koikifw:koiki-starter-security:0.1.0-SNAPSHOT` |
| Root reactor | 正式moduleとして追加 |
| BOM | KOIKI artifact coordinateだけを追加。Security個別version propertyや独立BOMは追加しない |
| A1 production dependency | Boot-managed `spring-boot-starter-security`だけを第一候補とする |
| Java package | `org.koikifw.starter.security.internal`から開始 |
| Public Java API | 0型を第一案とする |
| Public API inventory | `public-api.txt`を配置する。property名と件数はT0 / T1後に確定 |
| Exclusions | test user / route / key、Customer role / permission、login UI、identity persistence、OAuth2、Session JDBC、Migration |

Security Starterは既存Starterと同じ正式leaf artifactとする。空の将来module、`koiki-security-api`、
`koiki-security-impl`、Reference Applicationまたは独自Security abstractionは作らない。

### 4.2 Non-distributed Security verification fixture

| Item | Placement / rule |
|---|---|
| Owner | Tooling |
| Directory | `build-support/security-foundation-verification/` |
| Distribution | root reactor、BOM、正式release unit、snapshot publishから除外 |
| Maven identity | `org.koikifw.buildsupport`配下の検証専用座標 |
| Initial shape | P2-A1の単一fixture Maven project。共有test library用の別moduleは作らない |
| A1 test dependencies | Boot-managed `spring-boot-starter-test`と`spring-boot-starter-security-test`をtest scopeで使用 |
| Source boundary | test application、test route、test user、failure switchはtest sourceだけに置く |
| Reuse | A2 / A3以降も同directoryのHarnessへscenarioを累積し、CPごとに作り直さない |

既存Runtime Foundation ConsumerへSecurity Starterを追加しない。同ConsumerはPhase 1b runtime契約の回帰証拠であり、
Securityのdefault denyやtest routeを混入させない。P2-A1では専用fixtureをCustomer-likeな最小consumer境界として使う。

### 4.3 Verification entry point

`build-support/security-foundation-verification/verify-p2-a1-security-foundation.ps1`をA1のlocal aggregate入口候補とする。
scriptはtracked sourceを書き換えず、temporary directory配下の隔離Maven repositoryを使い、次を順に検証する。

1. 正式root release unitを隔離repositoryへstageする。
2. Security Starterとfixtureのfocused `verify`を実行する。
3. production / test dependency treeを記録し、Boot BOM authorityと除外dependencyを検査する。
4. T0 ApplicationContext matrixとT1 request / deny scenarioを実行する。
5. JAR、response、log、test reportにtest credential、secret、token、private keyまたはPIIがないことを検査する。
6. root `verify`、Public API inventory、既存Null Safety / architecture検査へ接続する。

## 5. Impact of adding the formal module

| Area | Current | After P2-A1 module addition | A1 treatment |
|---|---|---|---|
| Root reactor | 10 projects | 11 projects | root `pom.xml`へSecurity Starterを追加 |
| Deployable release unit | 9 artifacts | 10 artifacts | BOMと隔離repository stageへ含める |
| Parent | 共通build policy | 変更不要 | Security固有plugin / versionを追加しない |
| BOM | KOIKI 7 coordinates + Boot BOM等 | Security Starter coordinateを追加 | Spring Security versionはBoot BOM管理 |
| Existing Runtime Consumer | Phase 1b runtime回帰 | 変更なし | Security fixtureと分離 |
| `koiki-testing` | 共通DB / Testcontainers support | 変更なし | Security test dependencyを追加しない |
| Production Migration | 0 Framework SQL | 変更なし | A1では追加禁止 |
| Remote snapshot publish | Phase 1bの9 artifacts固定 | Security artifact未公開 | A1ではworkflow / publishを変更せず、Gate A側で別途承認 |

## 6. Required alignment before A1-2 production implementation

### 6.1 Phase 1b closeout CI

Security Starterをrootへ追加すると、現行`verify-cp10-closeout.ps1`のexact 10-project assertionは必ず失敗する。
過去のPhase 1b Evidenceを11 projectsへ書き換えたように扱わないため、A1 implementationでは次を分離する。

1. CP10 scriptはPhase 1bで承認したartifact / Consumer invariantの回帰として扱い、後続Phase artifactを許容する
   subset検査へ限定する。
2. P2-A1専用scriptが現行11-project root、10-artifact release unit、Security Public API inventoryをexactに検査する。
3. `.github/workflows/ci.yml`へのSecurity専用lane追加やrequired check変更はA1では行わず、localで安定化した後に
   Phase 2計画どおりOwner reviewする。

これにより既存workflowを変更せずCIを維持しながら、Phase 1bの歴史的な「当時10 projects」というEvidenceと、
Phase 2の現行release unitを混同しない。

### 6.2 Root agent guidance

Root `AGENTS.md`の見出しと実装scopeは現在もPhase 1aであり、Security成果物を先行実装しないよう指示している。
一方、Grand Design、Phase 2実行計画、Gate P2-2承認および本branchはP2-A1開始可能を示す。
Root指示を過去Phaseのまま黙って無視しないため、production codeを追加するA1-2より前に、Phase 0 / 1aの
不変条件を維持したPhase 2向けAgent Guidanceへ更新し、Architecture Owner review対象とする。

## 7. Deferred implementation decisions

次はA1-1で固定せず、A1-2〜A1-5のT0 / T1 Evidenceから判断する。

- Auto Configuration classと具体package / resource entry
- Security有効化と必要propertyの具体名、空白 / 欠落時のfail-fast条件
- Customerが明示するroute / matcher contract
- Customer-owned `SecurityFilterChain`とのback-offまたは合成方式
- 401 / 403 / redirectのprofile別表現
- CSRF / Security Headerの明示override contract
- 公開configuration propertyの最小集合
- P2-A1で公開Java型0を維持できるか

## 8. A1-1 result

A1-1の配置案は、Framework成果物とTooling fixtureを分離し、既存Starter、BOM、隔離Consumer検証の実装規則に整合する。
新規module追加前に必要なroot reactor、release unit、Consumer、CIおよびremote publishへの影響を本記録で提示した。

A1-2へ進む条件は、§6のPhase 1b closeout検査の前方互換化方針とRoot Agent GuidanceのPhase 2整合をreviewし、
その後のproduction差分を`koiki-starter-security`、Boot-managed Security dependencyおよびP2-A1 fixtureだけへ限定することである。

### 8.1 Owner review record

2026年9月1日、Architecture OwnerはA1-1の棚卸しと配置案を確認し、次の作業へ進むことを承認した。
これにより、A1-2では§4の正式Starter / 非配布fixture境界と§6の整合方針に従ってdependency baselineを実装する。

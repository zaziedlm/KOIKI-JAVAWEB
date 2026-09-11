# Phase 2 P2-C2 package / Consumer contract review

## 1. Status and scope

- 作成日: 2026年9月11日
- 作業パッケージ: `P2-C2`
- baseline commit: `4d9230ff19105ddf4f6a5a2e213ef0cee349a90b`
- baseline branch: `feature/phase2-p2-c1-postgresql-migration`
- Architecture Owner承認日: 2026年9月11日
- status: `CONTRACT REVIEW COMPLETE / ARCHITECTURE OWNER APPROVED`
- Ownership: Framework Packaging / Tooling Evidence
- production change at preparation: 0

本reviewは、P2-C1までに承認したFramework artifactを変更せず、配布単位、Root Reactor外Consumer、
Public API baseline / compatibilityおよびOpenRewrite試作の境界を確定する。契約承認前にproduction artifact、
Public API、Consumer fixture、OpenRewrite recipe、CI、remote packageまたはrulesetを変更しない。

## 2. Authoritative baseline and current inventory

| Subject | Current fact | P2-C2 implication |
|---|---|---|
| Root Reactor | Referenceを含む15 projects | 全体回帰の単位として維持する |
| formal Framework release unit | Referenceを除く14 reactor projects | P2-C2で増減させない |
| distributed JAR | Architecture Contract、ArchUnit Rules、Starter 8件、`koiki-testing`の計11件 | 全artifact inventory候補とする |
| Reference | Root参加、BOM / formal release unit非収載 | ConsumerやFramework artifactへ昇格させない |
| existing external Consumer | Phase 1b `build-support/runtime-foundation-consumer`、Gate A `build-support/security-foundation-consumer` | 両baselineを変更しない |
| existing published API baseline | Architecture Contract / ArchUnit Rulesの2 artifact | 現行比較を維持する |
| Phase 2 API | Security 0型、Audit 6型、Identity 10型、Session 3型 | 過去baselineなし。現inventoryはbaseline候補 |
| OpenRewrite | 方針はADR-029で承認、正式artifact / CIはPhase 5 | P2-C2は非配布の試作と境界Evidenceだけ |
| OpenSpec | 採用済みchangeなし | P2-C2の必須toolingにしない |

正式release unitの14 projectsは、Root aggregator、BOM、Parentおよび11 JAR moduleから成る。
P2-C2はpackage / Consumer検証であり、新機能、migrationまたは新しいFramework Public APIを追加する工程ではない。

## 3. C2-C1 — Release unit and artifact placement

### Proposed decision

- formal Framework release unitを14 projectsのまま維持する。
- `koiki-reference-app`をBOM、Framework repositoryおよびConsumer成果物へ含めない。
- P2-C2のために`koiki-migration-recipes`その他のproduction Maven moduleを追加しない。
- ConsumerとOpenRewrite試作は`build-support`配下の非配布Toolingとし、Root Reactor、BOM、snapshot publish、
  `koiki-testing`およびFramework Public APIへ含めない。

正式OpenRewrite artifactはGrand Design §8.6とADR-029どおりPhase 5のrelease判断へ残す。

## 4. C2-C2 — Package and isolated repository contract

空の隔離Maven repositoryへformal Framework release unitだけをstageし、次を機械検査する。

1. Parent、BOMおよび11 JAR coordinatesがすべて解決できる。
2. Reference、P2-C2 Consumer、検証fixture、temporary artifactが収載されない。
3. package済みJAR内のAuto Configuration、migrationおよびPublic / internal package inventoryが承認済み状態と一致する。
4. Consumer buildはRepository内source path、通常local Maven cacheおよびreactor buildへ依存しない。
5. package / test / runtimeが同じstaged coordinatesだけを利用する。

P2-C1のmigration検査とGate Bのartifact検査を再利用し、同じassertionを別正本として複製しない。

## 5. C2-C3 — Root Reactor外Consumer

### Proposed placement

`build-support/security-foundation-consumer`はGate Aで採用済みのCustomer-likeな非配布fixtureである。C2-3ではこのPOM / source /
testを置換せず、同directory配下の独立`postgresql` Consumerとして分離する。いずれも正式Reference、Project Template、
Customer成果物またはFramework moduleではない。Phase 1bの`build-support/runtime-foundation-consumer`も承認済み回帰資産として
変更しない。この記述はC2-3着手時に既存Gate A Consumerを再確認して明確化したもので、承認済みOwnership判断を変更しない。

### Proposed Consumer contract

- Root Reactor外の独立POMとし、KOIKI Parentは`relativePath`なし、dependency versionはBOMから解決する。
- package済み`koiki-starter-session-jdbc`を入口として承認済みSecurity / Identity / Audit / Data dependency closureを利用する。
- PostgreSQL driver、Flyway PostgreSQL moduleおよびSpring WebMVCなどApplication所有dependencyはConsumerが明示する。
- Customer configuration、Security chain、synthetic user / credential、test routeおよびCustomer migrationはfixtureだけが所有する。
- Framework Public API経由でIdentity query / administration、AuditおよびSession cleanupの最小利用をcompile / runtime検証する。
- package済み同一JARを起動し、default deny、認証、Customer route、Framework migration 3件 / 11 table、
  Customer history分離、再起動no-opおよびstartup failureを外部観測する。
- Java 21でbuild / test / packageし、package済みの同一JARをJava 21 / 25で起動してruntime互換性を確認する。
- Consumerの業務語彙、fixture API、migration、credentialまたはfailure switchを正式artifactへ昇格させない。

Reference production codeをcopyするのではなく、同じ公開契約を別の最小Consumerから利用することで配布可能性を検証する。

## 6. C2-C4 — Public API inventory and compatibility baseline

### Existing baseline

Architecture ContractとArchUnit Rulesについては、Phase 1aで公開したtimestamped snapshot、SHA-256、inventoryおよび
`japicmp`比較を変更せず維持する。

### Phase 2 baseline candidate

Security 0型、Audit 6型、Identity 10型、Session 3型を含む11 JARすべてについて、package済みartifactから
正規化signature inventoryを生成する。internal packageのpublic classは外部Public APIとして数えず、internal非露出を
別assertionで検査する。型名だけでなくconstructor、method、field、enum値、annotation metadata、nullness annotationを
比較可能な形で記録する。

Phase 2 artifactには比較可能な過去のpublished baselineがない。したがって、最初のsnapshot公開前に
「binary compatible」と主張しない。P2-C2で確認できるのは、承認済みinventoryとの一致、Consumerからの利用可能性、
および将来比較に使うbaseline候補の完全性である。

## 7. C2-C5 — Publish and `japicmp` boundary

次の順序を推奨する。

1. C2-C1〜C7をArchitecture Ownerが承認する。
2. localでformal release stage、全JAR inventory、Consumerおよびcompatibility fixtureを成功させる。
3. candidate artifactごとの座標、signature、SHA-256および公開対象をOwner reviewする。
4. snapshot publishを個別にOwner承認する。
5. 承認済み同一commitをremote repositoryへ一度だけ公開し、timestampとSHA-256をEvidenceへ記録する。
6. 公開済みbaselineの取得、hash一致および`japicmp`実行を確認する。

初回baselineと同じsourceを比較した`japicmp`成功は、baseline取得経路の成立確認であり、過去versionとの互換性実績ではない。
以後の変更で初めてbinary / source compatibility gateとして使用する。baseline JAR、token、settings実値およびreportはcommitしない。

## 8. C2-C6 — OpenRewrite prototype boundary

P2-C2では、正式recipe提供ではなく次のfeasibilityだけを確認する。

- `build-support`配下のRoot外・非配布fixtureでOpenRewriteのrecipe test harnessを実行できる。
- 固定したsynthetic old Consumer inputに対し、KOIKI所有のpackage / property / API変更を模した単一変換を適用する。
- before / after source、冪等性、変換後compile / test、および未変換の手動残件reportを検査する。
- Spring Boot自身の移行recipeを再実装しない。
- 実在しない過去KOIKI contractへの対応や、実Customer sourceの完全自動移行を主張しない。
- prototype dependency、recipe class、fixtureおよびreportをformal release unitやCI required checkへ追加しない。

現時点には正式なKOIKI旧メジャー入力が存在しないため、prototypeのsynthetic変換を`koiki-migration-recipes`へ昇格させない。
正式recipe、旧版Reference / Project Template fixtureおよびrelease CIはPhase 5で改めてOwner reviewする。

## 9. C2-C7 — Verification, cleanup and handoff

P2-C2 closeoutでは次を同一clean HEADで確認する。

- focused package / Consumer / API / OpenRewrite検証
- Root Reactor、Null SafetyおよびGate B回帰
- P2-C1 static / PostgreSQL migration回帰
- staged repository、Consumer JAR、Public API reportおよびOpenRewrite reportのsensitive-output検査
- process、PostgreSQL container、temporary repository / directoryおよびfixture targetのcleanup
- formal release unit 14、Root 15、Reference分離およびTooling非配布の最終inventory

P2-C2 EvidenceとOwner承認を記録した後、P2-C3 Developer Journey / DoD closeoutへ引き渡す。
workflow、remote run、required check、ruleset、PRおよびmergeはGate Cまたは個別Owner承認まで実施しない。

## 10. Proposed work breakdown

| Slice | Scope | Commit point |
|---|---|---|
| C2-1 | contract / inventory review | Owner承認済み契約だけをcommit |
| C2-2 | formal package manifest / isolated staging | 14-project release境界と11 JAR inventory |
| C2-3 | Root外Security Consumer | independent package / PostgreSQL runtime Evidence |
| C2-4 | full Public API inventory / compatibility fixture | baseline candidateとnegative test |
| C2-5 | publish review | Owner承認時だけremote snapshotを公開・固定 |
| C2-6 | OpenRewrite feasibility prototype | 非配布synthetic fixtureと手動残件Evidence |
| C2-7 | closeout | Root / Null Safety / Gate B / C1回帰、cleanup、Owner承認 |

C2-5はlocal成果物ではなくremote stateを変更するため、C2-1の契約承認だけでは実行を許可しない。

## 11. Architecture Owner decisions requested

1. formal Framework release unitを14 projects、11 JARのまま維持し、P2-C2でproduction moduleを追加しないか。
2. Phase 1b Consumerを変更せず、P2-C2専用Root外Consumerを非配布Toolingとして分離するか。
3. Consumerはisolated repositoryからParent / BOM / Starterを解決し、package済み同一JARをJava 21 / 25とPostgreSQLで起動して公開契約を実証するか。
4. 11 JARの正規化Public API inventoryをbaseline候補とし、過去baselineのないPhase 2 artifactへ互換性を過大claimしないか。
5. snapshot publishをlocal実装とは別のOwner判断とし、timestamp / SHA-256固定後から将来`japicmp`比較を開始するか。
6. OpenRewriteはsynthetic inputによる非配布feasibilityに限定し、正式recipe artifact / CIをPhase 5へ残すか。
7. C1 / Gate B回帰、sensitive-output、cleanupおよびrelease / Tooling inventoryをC2 closeout条件とするか。

推奨結論はC2-C1〜C7を上記案で承認し、C2-2 formal package manifest / isolated stagingから開始することである。

2026年9月11日、Architecture OwnerはC2-C1〜C7をすべて推奨案どおり承認した。C2-C5のsnapshot publishは
local実装とは別のOwner判断であり、本承認だけではremote publishを実施しない。これによりC2-1を`COMPLETE`とし、
次はC2-2 formal package manifest / isolated stagingへ進む。

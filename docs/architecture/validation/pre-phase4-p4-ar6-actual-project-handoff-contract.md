# Pre-Phase 4 P4-AR6 Actual-project Handoff Contract

## 1. Status and boundary

| Item | Current position |
|---|---|
| Status | `PREPARATION — ACTUAL-TEAM INPUT REQUIRED` |
| Ownership | Architecture / Framework / Customer joint decision |
| Source baseline | P4-AR5 commit `c68648e6c9746a8ef9c7ab6969b8872f41fe1ede` (`docs: complete P4-AR5 developer handoff readiness`) |
| Primary output | Repository topology、artifact handoff、Framework / Customer / joint Evidence責任分担、issue routing |
| R2 stage / manifest design | `REINFORCED / FRAMEWORK REHEARSAL OWNER APPROVED — ACTUAL-TEAM RUN PENDING` |
| Production change | 0 |
| Customer repository operation | 0。対象、影響、Ownerの別承認前には実施しない |
| Phase 4 start | 未承認。Gate P4-AR acceptance後に別途判断する |

P4-AR6では、P4-AR5でFramework側が準備した引継ぎ内容を実際のアプリ開発チームへ提示し、受け取る側の理解、
実行可能性および責任分担を確認する。本書はその実施準備と記録先であり、Customer固有code、schema、credential、
個人情報または機密logをKOIKI Repositoryへ取り込むものではない。

## 2. Authoritative inputs

| Input | P4-AR6で使用する内容 |
|---|---|
| [Pre-Phase 4計画](../../development/KOIKI-JavaWeb-FW_Pre-Phase4_Adoption_Readiness計画_v0.1.md) | P4-AR6 / AR-D10 / AR-CP4の境界、責任分担候補、停止条件 |
| [P4-AR5 Evidence](pre-phase4-p4-ar5-developer-handoff-readiness.md) | 受渡し候補inventory、技術checkpoint、finding、非提供境界 |
| [アプリ開発チーム向け引継ぎガイド](../../development/application-team-handoff-guide.md) | 10項目の一本道、30〜60分journey、8項目の受入条件、実チーム記録様式 |
| [Repository Architecture](../KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md) | Customer Applicationは別Repositoryとし、KOIKIをMaven成果物として利用する承認済み境界 |
| [Grand Design](../grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md) | 別Repository、version付き依存、FrameworkからCustomerへ依存しない上位方針 |

上位Architectureが定める正式形は、Customer ApplicationをFrameworkと物理的に別Repositoryへ置き、Parent / BOM / Starter / Public APIを
version付きMaven artifactとして利用する構成である。P4-AR6はこの境界を変更せず、正式artifact repositoryが未整備の移行期間に
Frameworkの変化を安全に追跡する方法を決める。

## 3. Repository topology decision preparation

### 3.1 Decision principles

Repository構成は、単に同時にbuildしやすいかではなく、次の観点で判断する。

1. Customer業務code、migration、configurationおよびcredentialのOwnershipがFrameworkから分離される。
2. Customer buildがFramework source tree、Root Reactor、relative pathまたは偶発的local artifactへ依存しない。
3. 利用したFrameworkのversion、source commit、artifact checksumおよびPublic API互換性を追跡できる。
4. Framework変更をCustomer側で早期に確認できるが、未承認変更へ自動追随しない。
5. Customer側でもArchitecture Rules、NullAwayおよび必要なtestを実行できる。
6. 将来、正式Maven repositoryへ移行するときにCustomer source配置を変更しない。

### 3.2 Topology options

次の表は、正式到達形と移行期の候補を同じ基準で比較する。`Current P4-AR6 position`は準備時点の提案であり、
実チームの環境・権限・運用を確認するまで最終決定ではない。

| ID | Topology | Framework変化の追跡 | Ownership / build isolation | Current P4-AR6 position |
|---|---|---|---|---|
| R1 | Customer別Repository + managed Maven repository | version更新、release note、互換性検査で追跡 | 最も明確 | **Target.** 正式release、repository、support条件の承認後に採用する |
| R2 | VS Code multi-root workspace等の同一local workspaceにFramework / Customerの別Git Repositoryを並置し、固定Framework commitからisolated Maven repositoryへstage | source差分を確認しながら、version / commit / checksumを固定できる | IDE上は同時に扱いつつ、Git履歴、POM、reactor、credentialを分離できる | **Recommended transitional candidate.** P4-AR6で実チームの再現性を確認する |
| R3 | 第三のintegration workspace / orchestration repositoryから両Repositoryを取得・検証 | 両者の組合せをmanifestで固定できる | 分離を保てるが、追加ToolingとOwnerが必要 | 必要性が確認された場合だけ設計する。先行生成しない |
| R4 | Customer ApplicationをKOIKI Root ReactorのMaven moduleとして追加 | source変更を即時に見られる | CustomerとFrameworkの履歴、release、CI、secret、migrationが混在する | **Do not adopt as normal handoff.** Accepted Repository Architectureの変更とADR reviewなしには採用しない |
| R5 | Framework source / ReferenceをCustomer Repositoryへcopyまたはfork | 独自差分には追随できるが、upstream identityを失う | Public / internal境界とupgrade責任が崩れる | **Prohibited.** 採用しない |

#### 3.2.1 `isolated Maven repository`の意味

現在のKOIKI検証Toolingでいう`isolated Maven repository`は、Nexus、ArtifactoryまたはGitHub Packagesのような
常設serverではない。検証開始時に空のlocal directoryを作り、Mavenへ`-Dmaven.repo.local=<directory>`を渡して、
その実行専用のlocal repositoryとして使用する。

```text
fixed Framework commit
  -> 空のisolated Maven local repositoryを作成
    -> formal release unit候補をbuild / install
      -> Parent / BOMのPOM、Starter / libraryのJAR・POMをrepository layoutで格納
        -> Customer buildも同じrepository pathを指定して通常のMaven coordinatesで解決
          -> build / test後にmanifestを記録してdirectoryをcleanup
```

このdirectoryにはKOIKI artifactだけでなく、Maven Central等から解決した外部dependencyのcacheも入る。隔離の対象は、
特に`org.koikifw`配下へstageされるartifactと、通常の`~/.m2/repository`に残っている偶発artifactへの依存である。
Reference、Tooling、Customer migrationまたはsource templateがformal release unitへ混入していないことも別途検査する。

したがって「移行期専用のJARビルド向けrepository」という理解は概ね近いが、次のように区別する。

| Question | Answer |
|---|---|
| JARをbuildするsource workspaceか | いいえ。build元は固定commitのFramework checkoutである |
| JARだけを保管するか | いいえ。Parent / BOM等のPOM、artifact metadata、外部dependency cacheも格納する |
| repository serverを新設するか | local受入検証では不要。空のlocal directoryで実行できる |
| Customer POMを特殊構成にするか | しない。通常のMaven coordinates / versionで依存し、実行時にrepository pathを指定する |
| チーム全員が共有する正式配布先か | いいえ。temporary / disposableな移行期検証用であり、正式releaseまたはsupport対象ではない |

開発者ごとのlocal受入ではこの方式を使用できる。一方、複数人・CI・長期間の共同開発で同じartifactを共有する場合は、
local directoryの手渡しではなくmanaged Maven repositoryが必要になる。その場合はR1として、repository製品、URL、
credential、publish権限、versioning、retention、checksum、source commit、supportおよび削除方針を別途承認する。

現行の`0.1.0-SNAPSHOT`等を共有repositoryで上書きし続ける方式は、どのFramework commitを使用したか曖昧になるため、
R1へ移行する前にversion identityを決める。R2のlocal検証では毎回空のrepositoryを作り、source commitとartifact checksumを
manifestへ記録することで、この曖昧さを閉じ込める。

#### 3.2.2 R2 local stage procedure contract

R2のstage処理はFramework-owned Toolingとして提供し、Customer側がFrameworkのMaven module構成や除外条件を
組み立てなくても実行できることを目標とする。2026-09-18、Architecture Ownerは本設計契約と§3.2.5の範囲に限定した
非配布Tooling実装を承認した。script名およびparameterの詳細は実装時に本契約へ適合させる。

| Phase | Procedure | Required observation | Failure / stop condition |
|---|---|---|---|
| 0. Source preflight | Framework checkoutの`HEAD`、clean worktree、JDK 21、Maven Wrapper、networkを確認する | 40桁source commit、tracked / untrackedを含むdirty=false、tool version | detached / branch状態自体は問わないが、expected commit不一致、Git管理対象外を含む非ignored差分、JDK不一致で停止 |
| 1. Safe stage root | Framework / Customer Repository外の明示pathをcanonical pathへ解決し、存在しないか空であること、pathの既存祖先を含めてjunction / symbolic link / reparse pointでないことを確認してTooling所有markerを置く | resolved stage root、開始時entry 0、所有marker | filesystem root、home、通常`~/.m2/repository`、Repository配下、非空directory、link / reparse pointまたは安全確認不能で停止 |
| 2. Formal unit stage | 固定commitから現行formal release unit候補を`clean install -DskipTests`でstageする | 15 projects / 12 JAR、POM 3、`org.koikifw`座標の完全一致 | Maven失敗、欠落・余剰coordinate、packaging不一致で停止 |
| 3. Boundary inspection | Reference、Tooling、Customer migration、source template、想定外groupを検査する | forbidden artifact 0 | `koiki-reference-app`または非配布artifact混入で停止 |
| 4. Manifest draft | artifact identityと実行環境をstage外の明示Evidence出力先へJSONで記録し、schemaとSHA-256形式を自己検査する | §3.2.3の必須field、artifact count、payload hash | field欠落、duplicate coordinate、hash形式不正、secret候補または絶対user path混入で停止 |
| 5. Customer resolution | Customer POMを同じ`maven.repo.local`でsnapshot更新を抑止して`clean verify`し、KOIKI dependency treeとArchitecture Rulesを使用するtest source／compiled test class／Surefire実行結果を検査する。実行前後で全KOIKI payloadを再走査する | consumer-visible KOIKI座標1件以上、`koiki-archunit-rules`解決、KOIKI座標・payload SHA-256がmanifestと完全一致、Architecture Rules test成功1件以上、Framework source / reactor依存0、internal package参照0 | KOIKI依存0件、`koiki-archunit-rules`未解決、Architecture Rules未実行、Framework sourceを指す`relativePath`、`systemPath`、internal package、manifest外・追加・欠落・変更KOIKI artifactまたは通常local repoへの依存で停止 |
| 6. Result capture | PASS / FAIL、所要時間、Customer側source identityの非機密表現、finding IDをfinal manifestへ記録する | 再現に必要な最小情報、KOIKI payload不変 | Customer source path、credential、個人情報、業務dataをEvidenceへ出力した場合、またはCustomer verify後のKOIKI payload差分がある場合はblocking |
| 7. Cleanup / finalize | Toolingは既存祖先を含むnon-link path、開始時と同じcanonical pathおよび所有markerを再検査して、Tooling所有stage rootだけを削除する。Customer process、port、container、一時credentialは実チーム受入セッションで別途cleanup・確認し、final manifest外のsession Evidenceへ記録する | stage residual 0、stage root削除、session resourceは別Evidence必須、final manifest、外部記録したmanifest SHA-256 | marker / path不一致時はstage rootを削除せずFAIL。stage cleanup不能もFAILとしてfinalizeする。所有を証明できないprocess / container等をToolingが自動削除せず、session cleanup未確認をP4-AR6全体の成功扱いにしない |

Phase 2履歴の`p2-c2-formal-release-unit.txt`は書き換えない。Phase 3で追加した`koiki-starter-web-mvc`を含む現行境界は、
既存`verify-p2-c2-package-static.ps1 -CurrentFormalReleaseUnit`とRuntime CP8 / CP10の15 projects / 12 JAR検査を
再利用する。ただし、既存検証scriptはtemporary repositoryをfinallyで削除するため、R2 Toolingは共通化可能なinventory / assertionを
再利用または抽出し、検証scriptのcleanupを無効化して流用しない。

Root aggregator POMを含む15 projectsはstage inventoryの完全性確認に使用する。Customerが依存対象として選べるのは、
Parent、BOM、12 JARのうち案件に必要なartifactだけであり、Root aggregatorをCustomer dependencyとして公開しない。

source preflightの`dirty=false`は、tracked差分だけでなく、Gitが無視していないuntracked fileも存在しない状態を指す。
未追跡のJava sourceやresourceもMaven buildへ混入し得るため、`git status --porcelain --untracked-files=all`相当の結果が
空でなければ停止する。stage後にも同じ検査を再実行し、固定commitからのbuildであることを確認する。

stage rootのcleanupは、単に空で開始したという理由だけでは許可しない。Toolingが作成した所有marker、開始時に確定した
canonical path、link / reparse pointでないことを削除直前に再検査し、すべて一致した場合だけstage rootを削除する。
不一致時は対象を残してFAILとし、推測した別pathや親directoryを再帰削除しない。

#### 3.2.3 R2 manifest contract

manifestはlocal stageと使用Frameworkを一意に対応付けるためのhash inventoryである。Customer業務情報やartifact本体を
Evidenceへ埋め込むものではない。full manifestはstage root外のcaller指定Evidence出力先へ保存し、stage cleanup後も
合意したretention期間だけ保持する。Framework / Customerのsource Repositoryへ既定で書き込まず、P4-AR6 Evidenceには次の
非機密summaryだけを転記する。

- schema version / kind
- Framework source commit / dirty=false
- logical Framework version
- manifest SHA-256
- project / coordinate / JAR count
- Java / Maven Wrapper version
- Tooling contract version / script SHA-256
- stage / Customer verificationのPASS / FAILと時刻
- finding ID（存在する場合）

必須JSON構造の設計案は次のとおりとする。次はfinal状態の例であり、値は実行結果ではない。draftでは
`finalizedAtUtc`を`null`、未完了のverification statusを`PENDING`としてよいが、final manifestでは`PENDING`を残さない。

```json
{
  "schemaVersion": 2,
  "kind": "p4Ar6LocalStageManifest",
  "stagedAtUtc": "2026-09-18T00:00:00.0000000Z",
  "finalizedAtUtc": "2026-09-18T00:10:00.0000000Z",
  "source": {
    "repository": "KOIKI-JAVAWEB",
    "commit": "<40 lowercase hex>",
    "dirty": false
  },
  "build": {
    "javaFeature": 21,
    "javaRuntime": "<vendor and exact version>",
    "mavenWrapper": "<exact version>",
    "os": "<non-user-identifying OS description>"
  },
  "tooling": {
    "contractVersion": 2,
    "scriptSha256": "<64 uppercase hex>"
  },
  "customer": {
    "sourceIdentityKind": "commit",
    "sourceIdentity": "<approved non-secret identity>"
  },
  "releaseUnit": {
    "logicalVersion": "0.1.0-SNAPSHOT",
    "projectCount": 15,
    "coordinateCount": 15,
    "jarCount": 12,
    "pomOnlyCount": 3
  },
  "repository": {
    "kind": "local-isolated",
    "startedEmpty": true,
    "absolutePathRecorded": false
  },
  "artifacts": [
    {
      "groupId": "org.koikifw",
      "artifactId": "koiki-starter-api",
      "version": "0.1.0-SNAPSHOT",
      "packaging": "jar",
      "role": "runtime",
      "consumerVisible": true,
      "payloads": [
        {
          "extension": "pom",
          "relativePath": "org/koikifw/koiki-starter-api/0.1.0-SNAPSHOT/koiki-starter-api-0.1.0-SNAPSHOT.pom",
          "sizeBytes": 0,
          "sha256": "<64 uppercase hex>"
        },
        {
          "extension": "jar",
          "relativePath": "org/koikifw/koiki-starter-api/0.1.0-SNAPSHOT/koiki-starter-api-0.1.0-SNAPSHOT.jar",
          "sizeBytes": 0,
          "sha256": "<64 uppercase hex>"
        }
      ]
    }
  ],
  "verification": {
    "formalInventory": "PASS",
    "forbiddenContent": "PASS",
    "customerBuild": {
      "status": "PASS",
      "durationSeconds": 0,
      "koikiPayloadsUnchanged": true,
      "internalPackageReferences": 0,
      "koikiDependencyCount": 1,
      "architectureRuleTestCount": 1
    },
    "cleanup": {
      "status": "PASS",
      "scope": "tool-owned-stage-root",
      "stageResidualResourceCount": 0,
      "stageRootRemoved": true,
      "sessionResourceStatus": "SEPARATE_EVIDENCE_REQUIRED"
    }
  },
  "result": "PASS",
  "findingIds": []
}
```

`artifacts`には15 coordinateをartifactId昇順で格納する。Root aggregatorは`role=aggregator`かつ
`consumerVisible=false`、Parent / BOMは`packaging=pom`、その他12 artifactは`packaging=jar`とする。
各coordinateはPOM payloadを1件、JAR packagingはさらにJAR payloadを1件持つ。SHA-256はfile contentから計算し、
relative pathはstage rootからの`/`区切りとする。

`result`はToolingが担当する全verificationが`PASS`で、Tooling所有stage residualが0の場合だけ`PASS`とする。この値は
Customer process、port、containerまたは一時credentialのcleanupを証明せず、実チーム受入セッションの別Evidenceがない状態で
P4-AR6全体を`PASS`としてはならない。処理途中の例外でも
可能な範囲でfinal manifestを生成し、`result=FAIL`、失敗したverification statusおよびfinding IDを記録する。
final manifest自身のSHA-256はmanifest内へ埋め込まない。自己参照を避けるため、Evidence summaryまたはmanifestと並べた
sidecarへ記録し、full manifestと同じretention / 非露出条件で扱う。
Framework source commitはartifact生成元、`tooling.scriptSha256`は検証処理のidentityとして別々に記録する。

次はmanifestへ記録しない。

- stage root、Framework checkoutまたはCustomer checkoutの絶対path
- user name、home directory、machine name、IP address
- repository credential、token、Cookie、password、private key
- Customer groupId / artifactIdが機密となる場合の実値
- Customer source、dependency tree全文、業務dataまたはlog全文

manifest自体は署名済みrelease証明ではない。local R2ではpayload SHA-256とsource commitの対応を検査するために使い、
R1 managed repositoryへ移行する場合は、publish identity、resolved snapshot version、workflow run、署名、retentionを含む
Phase 2 publish manifest相当の強い契約を別途設計する。

#### 3.2.4 Customer invocation and POM boundary

Customer POMにはstage directoryの絶対pathを記述しない。KOIKI artifactは通常の`groupId` / `artifactId` / `version`で宣言し、
stage pathはFramework / Customerの実行wrapperまたはcommand parameterから`-Dmaven.repo.local`として渡す。
KOIKI Parentを継承する場合は、Maven既定のfilesystem上の親POM探索を無効にする空の`<relativePath/>`を要求する。
Framework sourceを指す値付き`relativePath`、`systemPath`またはfilesystem repository URLは許可しない。

Parent継承とCustomer-owned Parent + KOIKI BOM importのどちらを標準入口とするかはP4-AR6 decision pendingとする。
R2 stageにはKOIKI ParentとBOMの両方を含め、実チームが選択候補を評価できるようにする。いずれの場合もStarterを
個別versionでばらばらに指定せず、単一のKOIKI version identityへ揃える。

Customer verificationで記録するdependency treeは`org.koikifw`座標だけへsanitizationし、manifest外version、
Framework source directoryへの参照およびCustomer側で再installしたKOIKI artifactを拒否する。Java packageの
`org.koikifw.*.internal`参照はdependency treeでは判定できないためsource検査で別に確認する。さらに、Customer test sourceが
`KoikiArchitectureRules`を利用していること、compiled test classが同型を参照していること、およびそのtest classがSurefireで
実際に成功したことを確認する。KOIKI dependencyが0件、`koiki-archunit-rules`が未解決、Architecture Rules test source／
compiled reference／成功した該当test caseのいずれかが0件なら、一般の`clean verify`が成功してもFAILとする。

Customer buildではMavenの`--no-snapshot-updates`を指定し、実行直前と直後にstage内の全`org.koikifw` coordinate / payloadを再走査する。
manifestにない追加、欠落、sizeまたはSHA-256の変化が1件でもあれば、Customer build自体が成功してもR2検証はFAILとする。
この契約はKOIKI artifact identityを固定するものであり、Customer固有の外部dependency全体をoffline lockするものではない。

#### 3.2.5 Proposed Tooling ownership

R2 Toolingを実装する場合は`build-support` Ownershipとし、正式Framework artifact、`koiki-testing`、Reference JARまたは
Customer Repositoryへ含めない。候補interfaceは次の責務へ限定する。

| Input | Contract |
|---|---|
| Expected Framework commit | 必須。40桁commitと`HEAD`の完全一致を検査する |
| Stage root | 必須。Framework / Customer Repository外のsafe / empty directoryだけを許可する |
| Manifest output | 必須。stage root外かつsource Repository外の明示file。親directoryだけを作成してよい |
| Customer POM | Customer verify実行時だけ必須。read-onlyで受け取り、POMまたはsourceを変更しない |
| Expected Framework version | 任意の二重確認。省略時は承認済みroot POMから取得し、manifestへ記録する |

| Tooling responsibility | Required behavior |
|---|---|
| Prepare | expected commit、tracked / untrackedを含むclean source、safe / empty stage rootとmanifest出力先について既存祖先を含むnon-link pathを検査し、所有markerを作成する |
| Stage | current formal release unitだけをbuild / installする |
| Inspect | coordinate / packaging / forbidden contentを完全一致で検査する |
| Manifest | §3.2.3のJSONとSHA-256 summaryを生成・検証する |
| Consumer verify | 外部Customer POMを変更せず、snapshot更新を抑止した明示parameterで`clean verify`し、consumer-visible KOIKI依存1件以上、`koiki-archunit-rules`解決、前後のKOIKI payload完全一致、Architecture Rules test source／compiled reference／Surefire成功結果を検査する |
| Cleanup | success / failureの双方でcanonical／non-link pathと所有markerを再確認し、Tooling所有stage rootだけを削除する。Customer process / port / container / 一時credentialはsession Evidenceへ分離する |

Toolingは`build-support/adoption-readiness-verification/invoke-p4-ar6-r2-handoff.ps1`として実装する。Public API、Starter、
dependency、migrationまたはworkflowを変更しない。Customer Repositoryを操作する場合は、対象path、実行commandおよび
影響を示して別途承認を得る。

#### 3.2.6 Framework-side Tooling rehearsal

2026-09-18、commit `3ecb836d23cec8a747ba2dcecbbb57156487a40f`のclean local cloneをFramework sourceとして、
Repository外の短い一時pathからR2 Toolingのfull rehearsalを実行した。これはFramework側の実装検証であり、実アプリ開発チームの
受入を代替しない。

| Observation | Result |
|---|---|
| Framework source | clean / expected commit一致 |
| Tooling script SHA-256 | `22987D2AFA385D73E4906818F1ECBCF24FE3F82F7F6EAB6F659F397D0A0524B3` |
| Formal stage | 15 projects / 12 JAR / 3 POM-only / 15 coordinates |
| Manifest payload | 15 POM + 12 JAR = 27 payload、各size / SHA-256記録 |
| Boundary inspection | Reference / Tooling / Customer migration / source template混入0 |
| Customer-like Consumer | `clean verify`成功、37 tests、failure / error / skip 0 |
| KOIKI artifact reinspection | coordinate、version、size、SHA-256変更0 |
| Cleanup | stage root削除、residual resource 0、実行後container 0 |
| Manifest lifecycle | Customer build前に`PENDING` draftを外部出力し、cleanup後に`PASS` finalへ置換 |
| Final manifest | `PASS`、SHA-256 `B2247CB24B025BA8B2205F292D35D29B2CE96DDC9850C1217A23E4318702C7E6` |
| Duration | 142.882秒 |

full manifest、isolated repositoryおよびclean cloneは検証終了後に破棄し、本書には非機密summaryだけを残した。初回の深いOS一時pathを
使ったlocal cloneはWindowsのpath lengthでcheckout不能となったためstage前に停止し、短い専用一時pathで再実行した。
この結果から、WindowsではFramework checkoutとstage rootを十分短いpathへ置くことをREADMEの実行前提とする。

#### 3.2.7 Owner review finding remediation rehearsal

同日、Owner reviewで、cleanup結果の対象範囲、KOIKI依存0件、Architecture Rules実行証明およびpath祖先の
reparse point検査を補強対象とした。Tooling contractをversion 2へ更新し、次を反映した。

- manifestのcleanup対象をTooling所有stage rootへ限定し、process / port / container / 一時credentialを
  実チーム受入セッションの別Evidenceへ分離した。
- consumer-visible KOIKI依存1件以上と`koiki-archunit-rules`の解決を必須にした。
- Architecture Rules test source、compiled test classの型参照、およびSurefireの成功結果を相互確認するようにした。
- stage root、manifest出力先、Framework / Customer Repositoryについて、既存祖先を含むreparse pointを拒否した。
- AR6-P2に残っていた実装前の表現を、Framework側rehearsal完了／実チーム検証未実施へ修正した。

補強後Toolingを、直前のTooling実装commit `5ea6c9d07964d045bb10829594699973b83a62d9`のclean local cloneと、
同commit内のRuntime Foundation Customer-like Consumerに対して再実行した。実Customer Repositoryは操作していない。

| Observation | Reinforced rehearsal result |
|---|---|
| Manifest / Tooling contract | schema 2 / contract 2 |
| Framework source | clean / expected commit一致 |
| Tooling script SHA-256 | `5CDC16B923AFF5D24DEB4CFB8A15F9A054428FCE20997C8624E39741A54A4A43` |
| Formal stage | 15 projects / 12 JAR / 3 POM-only / 27 payload |
| Customer-like Consumer | `clean verify`成功、37 tests、failure / error / skip 0 |
| KOIKI dependency proof | consumer-visible KOIKI 7座標、`koiki-archunit-rules`解決 |
| Architecture Rules proof | source / compiled reference / Surefire成功test 1件 |
| KOIKI artifact reinspection | coordinate、version、size、SHA-256変更0 |
| Cleanup | Tooling所有stage root削除、stage residual 0。session resourceは別Evidence必須 |
| Final manifest | `PASS`、SHA-256 `1229A3287417B443FEC7DFE68AB3E7C313ECAE546B64B2A7BF833DCACB8CDB0A` |
| Duration | 全体130.569秒、Customer verify 67.485秒 |

誤った40桁expected commitを与えた事前negative実行は`framework-source-preflight`でFAILし、所有marker照合後に
stage rootを削除してfinal manifestを`FAIL`とした。補強後のpositive実行は上表のとおりPASSした。
`sessionResourceStatus=SEPARATE_EVIDENCE_REQUIRED`であるため、このPASSを実チームのprocess / port / container / credential
cleanupまたはP4-AR6全体のPASSへ読み替えない。
full manifest、失敗時manifest、clean cloneおよびisolated repositoryはsummary確認後に破棄し、検証用rootの残存entryは0件とした。

R2では「同じIDE workspaceで開く」ことを、同じRepositoryまたは同じMaven reactorへ入ることと同一視しない。
VS Codeではmulti-root workspaceとして両Repositoryを同時に開けるが、`.git`、POM、build lifecycleおよびcredential境界は
それぞれ独立させる。想定するlocal配置は次のとおりである。これはProject Templateではなく、Repository Ownershipを
説明するための概念図である。

```text
development-workspace/                 # Git Repositoryにはしない
├── koiki-javaweb-fw/                   # Framework Git Repository
│   └── .git/
├── customer-application/               # Customer Git Repository
│   └── .git/
└── local-artifact-stage/               # isolated Maven repository / disposable / Git管理外
```

この図の`local-artifact-stage/`が、前述のisolated Maven repositoryに相当する。検証ごとに空で作成する専用の
Maven local repositoryであり、常設repository serverやFramework sourceのbuild workspaceではない。
FrameworkとCustomerの両buildから`-Dmaven.repo.local=<local-artifact-stage>`を指定し、検証終了後に破棄できる前提とする。

- Framework側は承認済みcommitからformal release unit候補だけを`local-artifact-stage`へstageする。
- Customer側は通常のPOM dependencyとして明示versionを参照し、Framework sourceへの`relativePath`、`systemPath`、source copyを使用しない。
- stage manifestにはsource commit、Maven coordinates、version、checksum、JDKおよび生成時刻を記録する。
- Customer buildが成功しても、そのartifactを正式releaseまたはsupport対象とは呼ばない。
- framework sourceへのread accessは学習・診断・変更差分確認に利用できるが、Customer業務moduleの配置先にはしない。

### 3.3 Customer Repository ownership layout

次は、P4-AR6で配置責任を確認するための概念例であり、生成可能なProject Templateや固定package名ではない。

```text
customer-application/
├── pom.xml                              # Customer-owned Parent / KOIKI BOM・Starter dependency
├── src/main/java/<customer-base>/
│   ├── <business-module-a>/             # 業務能力単位のmodule
│   └── <business-module-b>/
├── src/main/resources/
│   └── db/migration/customer/           # Customer-owned migration
├── src/test/java/<customer-base>/       # unit / application / architecture / integration test
└── docs/                                # Customer設計、運用、判断記録
```

実際のMaven module分割、base package、frontend配置、deployment単位およびCIはCustomer要件を確認して決める。
P4-AR6で空module、Project Templateまたは案件固有codeをKOIKI Repositoryへ先行生成しない。

## 4. Artifact handoff contract worksheet

実チームへ渡すものは、source treeの見え方ではなく、受取単位、identity、利用方法、非提供境界および問い合わせ先で定義する。

| Handoff item | Identity / delivery candidate | Customer use | Not provided / caution | Decision status |
|---|---|---|---|---|
| Parent / BOM / Runtime Starters | Maven coordinates、version、source commit、checksum | Customer POMから依存 | 正式repository / support条件は未決定 | P4-AR6 decision pending |
| Architecture Contract / ArchUnit Rules | test dependencyとrule version | Customer module境界をCIで検査 | 案件都合の暗黙除外は提供しない | P4-AR6 decision pending |
| `koiki-testing` | test scope dependency | 公開契約のtest支援 | Reference fixture / production runtimeではない | P4-AR6 decision pending |
| Framework source checkout | fixed commit / read access | 学習、診断、変更差分確認 | Customer codeの配置先またはsource dependencyではない | R2採用時に条件決定 |
| Reference Application | source / package済みJAR / Local Run Guide | 利用例と統合動作の理解 | Project Template、業務codeのcopy元ではない | P4-AR5 approved input |
| Consumer / verification Tooling | selected command / result | artifact利用経路と診断の理解 | Customer CIへaggregateを必須化しない | P4-AR6で提示範囲決定 |

## 5. Actual-team reception session plan

P4-AR6は一方向の説明会ではなく、実チームが入口を選び、実行し、判断を説明できるかを確認する。
実施時は[実チーム受入セッション worksheet](../../development/p4-ar6-actual-team-reception-worksheet.md)を使用し、
Customer機密情報をFramework Repositoryへ記録せず、実行前承認からcleanup、findingおよびclose review入力までを追跡する。

| Step | Activity | Evidence to retain | Stop / escalation condition |
|---|---|---|---|
| 1 | 参加role、KOIKI経験、利用可能なJDK / Maven / Docker / networkを非機密情報で確認する | 前提と不足だけを記録 | credential、個人情報、Customer sourceが必要になったら停止 |
| 2 | Handoff Guide §1〜§6から提供物、非提供物、UI / auth profile、Starter候補を選ぶ | 選択理由と未決定事項 | 未実装profileを実装済みとして扱わない |
| 3 | ReferenceまたはConsumerを30〜60分で起動する | 所要時間、成功観測点、迷った箇所、cleanup | 検査不能をPASSにしない |
| 4 | Handoff Guide §8で最初のCustomer-owned moduleを説明する | Ownership、Tier、責務、永続化、公開境界 | Framework / Reference packageへ配置しようとしたら境界を再確認 |
| 5 | R1〜R3からRepository / artifact handoff候補を評価する | 採否、理由、必要権限、再現手順、Owner | R4 / R5が必要ならArchitecture reviewへ戻す |
| 6 | §6の責任分担表と§7のissue routingを確認する | Framework / Customer / joint、acceptance Evidence、次Gate | OwnerまたはEvidenceが曖昧なら決定を保留 |
| 7 | resourceと一時credentialをcleanupし、findingsを分類する | residual 0、F1〜F6、priority、Owner | cleanup不能またはsecret非露出違反はblocking |

## 6. Phase 4 responsibility allocation draft

次の表は実チームreview用の初期案であり、P4-AR6の入力を得るまで確定しない。

| Phase 4 area | Framework responsibility candidate | Customer responsibility candidate | Evidence / decision needed |
|---|---|---|---|
| Notification / Level 2 | event、idempotency、retry、purgeの共通契約と検証可能性 | 業務通知内容、provider、運用 | 採用trigger、実装Owner、Framework / Customer acceptance |
| Accounting / MyBatis | adoption Gate、構造規約、Architecture Rules、共通error contract | 実schema、SQL、会計連携 | trigger成立前はRule 8拒否を維持 |
| MVC / SPA coexistence | Security profile、Session / CSRF / CORS契約 | 実画面、frontend構成、UX | MVC / same-origin React / BFF / direct Tokenの選択 |
| External API resilience | timeout、retry、concurrency、error translation契約 | 接続先固有Adapter、credential、SLA | protocol、障害責任、test環境 |
| Batch / file / object storage | 単一実行、共通処理境界、検証指針 | 業務job、file format、storage設定 | deployable、再実行、運用Owner |
| Observability | correlation、metric / trace契約、非露出 | 実監視基盤、alert、runbook | production観測点とretention |
| Container / ECS / VT | Framework runtime要件、Reference Evidence | deployment、resource sizing、network | platform Ownerと非機能acceptance |

## 7. Issue routing and finding treatment

| Finding | Primary route | P4-AR6 treatment |
|---|---|---|
| F1 Framework defect | Framework Owner | P4-AR blocking。再現手順、source identity、期待契約を記録する |
| F2 Developer-experience gap | Framework documentation / Tooling Owner | 優先度を決め、P4-AR7候補へ送る |
| F3 Environment-specific issue | Customer development environment / organization Security | Framework既定を歪めず、環境Ownerの手順へ分離する |
| F4 Customer requirement | Customer product / application Owner | Customer backlogへ置き、Framework昇格条件を別reviewする |
| F5 Phase 4 feature | Phase 4 planning Owner | 見直し後Phase 4計画へ入力し、P4-AR中に先行実装しない |
| F6 Optional Gate | Requesting project + Architecture Owner | 個別Gate承認前に開始しない |

Frameworkへ戻すissueには、業務能力、選択profile、使用artifact identity、最小再現、期待するPublic contract、
Security / Audit / migrationへの影響、Customer隔離の可否および希望時期を含める。Customer source、data、credentialまたは
機密logを添付しない。

## 8. P4-AR6 acceptance checklist

- [ ] 実チームがHandoff Guideを単一入口として利用し、迷った箇所を記録している。
- [ ] ReferenceまたはConsumerのbuild / run / representative operation / diagnosis / cleanupを実チーム条件で確認している。
- [ ] R1〜R3のRepository topology候補を評価し、Customer業務codeをFramework Repositoryへ混在させていない。
- [x] Framework-owned R2 Toolingがclean source、safe / empty / ancestorを含むnon-link stage、所有marker、formal inventory、manifest、Customer verify、artifact不変確認、Tooling所有stageの安全なcleanupを一つの入口から実行できる（Framework rehearsal）。
- [x] Framework artifactのversion、source commit、checksumおよび解決経路を追跡できる（Framework rehearsal）。
- [ ] 提供artifact、Reference、検証専用Tooling、非提供物を区別できる。
- [ ] Phase 4成果物ごとのFramework / Customer / joint責任とacceptance Evidenceを合意している。
- [ ] findingsにF1〜F6、priority、Owner、次Gateを割り当てている。
- [ ] Customer機密情報を記録せず、P4-AR6で使用したresourceと一時credentialをcleanupしている。
- [ ] Architecture OwnerがAR-D10の充足可否とP4-AR7 / Gate P4-ARへの進行可否を判断している。

## 9. Preparation findings and open decisions

| ID | Finding / decision | Current assessment | Required input / Owner |
|---|---|---|---|
| AR6-P1 | Customer RepositoryをKOIKI Root Reactorへ追加するか | Accepted Repository Architectureと競合するため通常案にはしない | 例外要求がある場合のみArchitecture Owner / ADR review |
| AR6-P2 | 移行期にFramework変更をどう追跡するか | R2を暫定推奨。stage手順とmanifest契約、Tooling実装およびFramework側rehearsalは完了。実チーム検証は未実施 | Framework release Owner + actual application team |
| AR6-P3 | managed Maven repository / version / support条件 | 未決定。R1移行のblocking decision | Framework release Owner + organization platform |
| AR6-P4 | actual Customer repository構成 | Customer要件未取得。概念Ownershipだけを提示 | Customer application lead |
| AR6-P5 | actual-team session schedule / participant | 実行前承認からclose入力までのworksheetは準備済み。schedule / participantは未決定 | Project / application Owner |
| AR6-P6 | R2 stage Toolingを実装するか | `REINFORCED / FRAMEWORK REHEARSAL PASS`。`build-support` Ownershipの非配布Toolingとして、§3.2.2〜§3.2.5のversion 2契約を実証した | 実Customer Repository操作は別承認 |
| AR6-P7 | R2契約review補強 | 2026-09-18、Architecture Ownerはclean source、cleanup安全性、artifact不変、final manifest、Parent / internal package検査境界の5点と、その反映後の最終契約を承認した | 反映済み |
| AR6-P8 | Tooling identity | 別checkoutにも適用できるため、Framework commitとは別に実行script SHA-256をmanifestへ追加した。絶対pathやsourceは記録しない | Framework rehearsal PASS。Owner close reviewで確認 |
| AR6-P9 | Tooling Owner review findings | cleanup scope、KOIKI依存0件、Architecture Rules実行証明、既存path祖先のreparse point検査をversion 2で補強した | 補強後Framework rehearsalをArchitecture Ownerが承認。実Customer Repository操作は別承認 |
| AR6-P10 | actual-team reception worksheet | Handoff Guideを入口に、非機密情報だけでpreflight、操作承認、journey、topology、責任分担、finding、cleanupおよびclose入力を記録する様式をArchitecture Ownerが承認した | 実チーム実施と結果記録は未実施 |

### 9.1 R2 contract approval record

2026-09-18、Architecture Ownerは、補強後のR2 stage / manifest契約をPre-Phase 4移行期のartifact受渡し検証方式として
承認した。あわせて、同契約を実証する非配布Toolingを`build-support` Ownershipで実装することを承認した。

実装範囲は、cleanな固定Framework commit、isolated Maven repository、現行formal release unit、manifest、
Customer-like Consumerまたは専用fixtureによる検証、artifact不変確認および安全なcleanupに限定する。Root Reactor、BOM、
formal release unit、Framework Public API、Starter、dependency、migrationまたはworkflowを変更しない。

本承認は、実Customer Repositoryの操作、正式artifact配布、managed Maven repository、P4-AR6完了、Gate P4-AR acceptance
またはPhase 4開始を承認するものではない。

### 9.2 R2 Tooling version 2 / Framework rehearsal approval record

2026-09-18、Architecture Ownerは、R2 Tooling version 2について、Owner reviewで指摘したcleanup保証範囲、
KOIKI依存0件、Architecture Rules実行証明およびpath祖先のreparse point検査が補強されたことを確認した。
§3.2.7の補強後Framework rehearsalはPASSしており、非配布Toolingとしての実装とFramework側検証結果を承認した。

実Customer Repositoryでの利用は、対象path、実行command、出力、影響およびsession resource cleanup方法を提示した上で、
別途承認する。本承認は、実Customer Repository操作、正式artifact配布、managed Maven repository、P4-AR6完了、
Gate P4-AR acceptanceまたはPhase 4開始を承認するものではない。

### 9.3 Actual-team reception worksheet approval record

2026-09-18、Architecture Ownerは、[実チーム受入セッション worksheet](../../development/p4-ar6-actual-team-reception-worksheet.md)が
Handoff Guideを単一入口として、実行前承認、環境確認、受入journey、Customer-owned module、Repository topology、
artifact受渡し、Phase 4責任分担、finding、cleanupおよびclose review入力を一貫して記録できることを確認し、
P4-AR6実チーム受入の実施様式として承認した。

Customer機密情報をFramework Repositoryへ持ち込まず、Framework側rehearsalを実チーム受入の代替とせず、検査不能または
cleanup不能をPASSにしない境界を維持する。本承認は、実Customer Repository操作、個別実行command、正式artifact配布、
P4-AR6完了、Gate P4-AR acceptanceまたはPhase 4開始を承認するものではない。

## 10. Owner review boundary

P4-AR6のclose reviewでは次を確認する。

1. 実チーム受入をFramework開発者側rehearsalで代替していない。
2. Repository topologyが別Repository / Maven artifact境界を維持し、R4 / R5を通常経路として採用していない。
3. R2を採用する場合もclean source、source commit、version、checksum、isolated repository、所有markerおよび安全なcleanupを追跡できる。
4. manifestがformal unitの全coordinateとpayload SHA-256を持ち、Customer verify前後で不変であり、絶対path、credentialまたはCustomer機密情報を持たない。
5. final manifestの状態、失敗時の記録および自己参照しないmanifest SHA-256の保存先が明確である。
6. Parent継承またはBOM importの入口、空の`<relativePath/>`、Customer POMへstage pathを埋め込まない境界、およびinternal package検査の担当が明確である。
7. Framework / Customer / joint責任分担とacceptance Evidenceが成果物ごとに明確である。
8. Customer固有code、schema、credential、個人情報または機密logをKOIKI Repositoryへ持ち込んでいない。
9. F1〜F6 findingのOwner、priority、次Gateが決まっている。
10. AR-D10を満たし、P4-AR7またはGate P4-ARへ進める状態か。

本書の骨格作成はRepository topology、artifact配布、実案件repository操作、P4-AR6完了、Gate P4-AR acceptanceまたは
Phase 4開始を承認するものではない。

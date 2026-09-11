# Phase 2 P2-C2 C2-5 snapshot publish review

## 1. Status and boundary

- review preparation date: 2026年9月11日
- work package: `P2-C2 / C2-5`
- source HEAD: `50a3329aaacadc1c0210ff5e7c62c8910ae22281`
- implementation baseline ancestor: `6793956d6ee9d8a0b30c5ac9927e4442a14a4bef`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- status: `WORKFLOW IMPLEMENTATION APPROVED / LOCAL VERIFIED / REMOTE PUBLISH NOT APPROVED`
- Ownership: Framework Packaging / Tooling Evidence
- production artifact / Framework Public API change: 0

C2-5のremote publishは未承認・未実施である。本書は候補座標、C2-4 signature、local SHA-256、公開境界、
取得経路および秘密情報を残さないEvidence方式をArchitecture Ownerへ提示する。条件付き承認後、Phase 2専用workflow候補と
local検証toolをworktreeへ追加したが、GitHub Packages、environment、required check、ruleset、PRおよびbranchは変更していない。

## 2. Resume and source identity

`git fetch origin`後、local / originのbranch先端はともに`50a3329`で一致し、`6793956`をancestorに含み、worktreeは
cleanだった。JavaはTemurin 21.0.12.1、Maven Wrapperは3.9.16である。

C2-4の実装baseline後に追加された`50a3329`はhandoff文書だけであり、production sourceまたはPOMを変更していない。
ただし、実publish対象は可変なbranch名や`6793956`ではなく、publish実装とreview記録を含めてOwnerが改めて承認する
40文字のclean source HEADへ固定する。

## 3. Candidate coordinates and publication boundary

formal Framework release unitはC2-2承認どおり14 projects（POM 3 / JAR 11）である。このうちRoot
`koiki-javaweb-fw-reactor`はPOM自身が`Reactor/Aggregator only`と定義し、既存snapshot workflowでも配布対象外である。
したがってremote publish候補はRootを除く次の13 coordinates（POM 2 / JAR 11）とする。

| Packaging | Coordinate | Public types | Position |
|---|---|---:|---|
| POM | `org.koikifw:koiki-dependencies-bom:0.1.0-SNAPSHOT` | — | Phase 1b公開済み、現release unitを管理 |
| POM | `org.koikifw:koiki-parent:0.1.0-SNAPSHOT` | — | Phase 1b公開済み、現release unitのParent |
| JAR | `org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT` | 4 | Phase 1a固定baselineを維持 |
| JAR | `org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT` | 1 | Phase 1a固定baselineを維持 |
| JAR | `org.koikifw:koiki-starter-api:0.1.0-SNAPSHOT` | 0 | Phase 1b公開済み |
| JAR | `org.koikifw:koiki-starter-data:0.1.0-SNAPSHOT` | 0 | Phase 1b公開済み |
| JAR | `org.koikifw:koiki-starter-data-jpa:0.1.0-SNAPSHOT` | 0 | Phase 1b公開済み |
| JAR | `org.koikifw:koiki-starter-observability:0.1.0-SNAPSHOT` | 0 | Phase 1b公開済み |
| JAR | `org.koikifw:koiki-starter-audit:0.1.0-SNAPSHOT` | 6 | Phase 2初回baseline候補 |
| JAR | `org.koikifw:koiki-starter-security:0.1.0-SNAPSHOT` | 0 | Phase 2初回baseline候補 |
| JAR | `org.koikifw:koiki-starter-identity:0.1.0-SNAPSHOT` | 10 | Phase 2初回baseline候補 |
| JAR | `org.koikifw:koiki-starter-session-jdbc:0.1.0-SNAPSHOT` | 3 | Phase 2初回baseline候補 |
| JAR | `org.koikifw:koiki-testing:0.1.0-SNAPSHOT` | 0 | Phase 1b公開済み |

`koiki-reference-app`、Root aggregator、Consumer、fixture、OpenRewrite prototype、source、javadocおよび一時reportは
公開しない。既存9 coordinatesに新しいtimestamped snapshotを追加するが、Phase 1a C1の固定timestamp / SHA-256を
差し替えず、既存Public API compatibility scriptの比較元も変更しない。

## 4. Signature identity

C2-4でArchitecture Owner承認済みの正規化signatureは
`build-support/security-foundation-verification/p2-c2-public-api.txt`であり、SHA-256は次のとおりである。

```text
200EDA9A02D226B4E802A98FE1A2D7F40D67BF3B08792CD9F9C0644CD32CAF53
```

このsignatureは現行release unit 11 JAR / 24 Public型のaggregate identityであり、型・継承・member・generic・例外・enum・annotation metadata / default・
JSpecify nullnessおよびPublic型0件のartifact sectionを含む。`.internal.` packageはPublic APIから除外する。
Phase 2の初回published baselineはAudit 6型、Identity 10型、Session 3型およびSecurity 0型に限定し、Architecture Contract / ArchUnit
RulesのPhase 1a固定baseline 5型を差し替えない。
初回同一source比較の成功は取得経路の成立確認であり、過去versionとのbinary / source compatibility実績とは表現しない。

review文書作成後に次を再実行し、11 JAR / 24 Public型のsignature一致、nullnessおよびjapicmp正負fixture、
一時repository / reportのcleanupが成功した。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-p2-c2-public-api.ps1
```

```text
Phase 2 P2-C2 C2-4 Public API verification succeeded (11 JARs / 24 public types / full signatures / nullness and negative fixtures).
```

## 5. Local file repository dry run

GitHubへ接続しないGUID付き一時file repositoryに対し、Rootを除く13 projectsを明示選択し、Maven Deploy Plugin 3.1.4、
`altSnapshotDeploymentRepository`および`deployAtEnd=true`で単一session deployした。13 / 13 projectsは
`BUILD SUCCESS`となり、空のlocal repositoryでは全13 coordinatesが同じtimestamped snapshotになった。一時builder / file repositoryは
終了時に削除した。

```text
source HEAD: 50a3329aaacadc1c0210ff5e7c62c8910ae22281
local timestamped snapshot: 0.1.0-20260911.124618-1
```

local candidate payloadのSHA-256は次のとおりである。

| Artifact | POM SHA-256 | JAR SHA-256 |
|---|---|---|
| `koiki-dependencies-bom` | `04963AEE6EEBDCA1629C674CE1572099B98BB5A7051F54BB490BEF8FA23572AF` | — |
| `koiki-parent` | `ADC149D5C693BDCCBA008FD5F6BE8D5DF3BE5F43DCEBACCB47A72892C4BDAE37` | — |
| `koiki-architecture-contract` | `7BE7635FE5E776FB0F5B5E4935DB0054D08F6D3CCE01EE7016275C685E1D926F` | `3F91A9D2179FACB9040D39B295D3BD82C37EE18775B2D997BC7649C8D0680CAA` |
| `koiki-archunit-rules` | `7B24A824B9EBD55794B7A626AE0FBB52A0781FFD1ECEFC18A899B629F4FEDA45` | `3860143CE2E8DA12E4A83B420693C1F8069D9C357309438416F1D8A4D734D0C0` |
| `koiki-starter-api` | `28DB09CB014066E08B52918CC4B7795CCAD810EF16042858AA69DF4A43DB975F` | `E01C11113CFE1B71E0A8339AAE74FBAE89309CF5DE7AB52383C547B3B486C646` |
| `koiki-starter-data` | `1F96C4F07BF37313786E9F241A20190B2EC09E0147220012D5898503399B8640` | `0E421B878EC6DF790FA4B75BFDEE3972ED3942FA643DC72898B8CE928636F7A4` |
| `koiki-starter-data-jpa` | `549DA4561E27244A86C034A9FC621898BE7AFD53E4E9D8450C7D31DFE85C5F77` | `CBD58DC336E556A946F0453C9A8C94F70A68454B044D6A97239360271FB90558` |
| `koiki-starter-observability` | `19369DA546AAFC60BB4B61E23410C386F562EEC008CCA63101A4C4C81EBC0286` | `7DCEE67FF659688B0BD520D61F1A3343757481EA38301B229DF4B98A7DFEC62D` |
| `koiki-starter-audit` | `4B84B6BF698DFA2144DB55957DD2CB03B39839B77D9AB221353AD94F3F0C5AFB` | `C3A0B6A17FBCDC725FF8A603935A7D576A61951D54DC5A9054F31BAD60A0DD34` |
| `koiki-starter-security` | `04276D1A1AE1EB861BE14D0517C2561E9B73D5E6A0C33329A19E64015E98BBD5` | `780943991405F2790D0A55804B6BE835FFD8FCF971F859C37E2379953D5C8B89` |
| `koiki-starter-identity` | `AE648740D19D47407220B5B061A433F42BA52E8279446F81659BFF0B6E8E9C3D` | `9090270D5A8108C89D409E4D06FCB6C0B2752AABEB0A40DF0C3701138C439D96` |
| `koiki-starter-session-jdbc` | `D800230028223733100B924155A22A8526EE001204094F00149CD81CC0219231` | `FBA411C6888D743E8A86BD9A890DD1E6DD45DD07A494041FA8A86C18D5B63241` |
| `koiki-testing` | `F7708688C0A126C80B8717BD623BFD18EC03BB04D3C72D794222C52DF3D0755F` | `7AC0F5CFE986EF3E0BD96500FBA97D99E74006E38E5B29D11765CFE962A4C381` |

この共通versionは空repositoryに対するlocal方式成立Evidenceであり、公開履歴が座標ごとに異なるremote repositoryの共通
`buildNumber`期待値にはしない。POMはsource内容が同じ限り決定的だが、JARはbuild timestampにより再build時にSHA-256が
変わり得る。したがって上記local
JAR hashを将来のremote hash期待値として固定しない。実publish sessionでdeploy直前の24 payload hashを採取し、remoteから
座標ごとのresolved snapshot valueで取得した24 payloadと一致させる。再buildした未照合JARを公開しない。

## 6. Implemented remote publication and acquisition path

### Repository and authentication

- repository: `https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB`
- publish: GitHub Actionsのrepository-scoped `GITHUB_TOKEN`、publish jobだけ`contents: read` / `packages: write`
- acquire: fresh jobの`GITHUB_TOKEN`、`contents: read` / `packages: read`
- local fallback: PAT classicを使う場合は`read:packages`だけとし、publish用PATをRepository secretへ保存しない
- Maven server ID: `github`。credentialは`actions/setup-java`からprocessへ供給し、認証付きURLを作らない

既存`.github/workflows/publish-snapshot.yml`はPhase 1bの9-artifact契約、preflightおよびenvironmentを保存するため変更していない。
条件付きOwner承認に基づき、Phase 2専用の`workflow_dispatch` workflowを別fileとして実装し、次を満たす。

1. `main`とOwner承認済み40文字`expected_commit`の完全一致をpublish前に検査する。
2. read-only preflightでC2-2 package、C2-4 Public APIおよび必要なC2-5 dry runを成功させる。
3. publish jobだけが新しいprotected environmentと`packages: write`を使い、13 projectsを明示選択する。
4. `deployAtEnd=true`、単一concurrency、手動dispatchとし、自動retry / deleteを行わない。
5. publish sessionが生成した13 POM / 11 JAR hash manifestだけを短期workflow artifactへ保存し、JAR、settings実値、token、reportを保存しない。
6. fresh verify jobが空のMaven repositoryへ13 coordinatesを座標ごとのresolved snapshot valueで取得し、24 payload hash一致、
   C2-4 signature一致および11 JARの同一source `japicmp`変更0を確認する。
7. 部分公開、timestamp不一致、hash不一致、signature不一致、credential露出または取得失敗時は再実行せずOwner reviewへ戻す。

GitHub PackagesのApache Maven registryはpublic packageを含めて認証を必要とし、Actions内では同じRepositoryに関連するpackageの
publish / installへ`GITHUB_TOKEN`を使用できる。Snapshot取得は有効化したrepository設定から行う。

参照した現行仕様:

- [Working with the Apache Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [About permissions for GitHub Packages](https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages)

### Timestamp and Evidence

各coordinateの`0.1.0-SNAPSHOT/maven-metadata.xml`をremoteから取得し、`timestamp` / `buildNumber`と
`snapshotVersions`を正規化する。公開履歴が座標ごとに異なるため共通`buildNumber`を要求せず、POM / JARごとのresolved valueで
13 POM / 11 JARを取得する。Evidenceへ残すのはsource commit、workflow run ID / URL、actor、実行日時、13 coordinates、
座標ごとのresolved snapshot value、24 SHA-256、
signature SHA-256、`japicmp`変更0およびcredential非露出確認だけとする。

## 7. Current remote readiness and stop condition

再開時に失効していたGitHub CLI認証は、Architecture Ownerによる再認証後、keyring保存、active account `zaziedlm`、
`read:packages`を含むscopeとして回復した。token実値はEvidenceまたはlogへ記録していない。認証回復後、write操作を行わず
次を確認した。

| Subject | Read-only result |
|---|---|
| Repository | `zaziedlm/KOIKI-JAVAWEB`、public、default branch `main`、Owner admin権限あり |
| existing Maven packages | Phase 1bまでの9 packageがRepositoryに紐づいたpublic packageとして存在 |
| existing versions | 9 packageすべて`0.1.0-SNAPSHOT` 1 version。delete / restore / publishは未実施 |
| Phase 2 new packages | Audit / Security / Identity / Session JDBCの4 packageは未作成 |
| Phase 1a environment | required reviewer `zaziedlm`、`main`だけ、self-review許可、admin bypass無効 |
| Phase 1b environment | required reviewer `zaziedlm`、`main`だけ、self-review許可、admin bypass無効 |
| workflow default permission | `read`、Pull Request approval不可 |
| existing publish workflow | local branchと`origin/main`に差分0。Phase 1b 9-artifact契約を維持 |
| existing publish runs | run `32951187676` / `33311794583`がいずれも`workflow_dispatch` / success |
| fixed baseline acquisition | main run `34475233082`の`Public API Compatibility` jobがsuccess |

最新mainのPublic API job成功により、Phase 1a固定timestamp / SHA-256を用いる既存取得・japicmp経路が維持されていることを
確認した。C2-5ではその固定値や既存scriptを変更しない。

remote側に残る未実施項目は、protected environmentの作成、PR / main CI、最終source commit承認、
manual dispatchおよびpublish後検証である。したがってread-only remote readinessは`VERIFIED`だが、remote publish自体は
引き続き`NOT APPROVED / NO-GO`とする。専用workflow実装のOwner review、PR / main CIおよびpublish対象commitのOwner承認が
揃うまでremote publishを実行しない。

## 8. Architecture Owner decisions and approval record

1. formal release unit 14 projectsのうちRoot aggregatorを公開対象外とし、POM 2 / JAR 11の13 coordinatesをC2-5候補としてよいか。
2. C2-4 signature SHA-256と24 Public型を現行release unitのaggregate identityとし、Phase 2初回published baselineをAudit 6型、
   Identity 10型、Session 3型、Security 0型としてPhase 1a固定baselineから分離してよいか。
3. 13 / 13 local file repository dry run、空repositoryでの共通timestampおよび24 local payload SHA-256をlocal方式の成立Evidenceとし、
   remoteの共通`buildNumber`を要求しない境界でよいか。
4. 既存Phase 1b workflowを変更せず、Phase 2専用manual workflow / protected environmentを別途実装reviewする方針でよいか。
5. 同一workflow run / source commit、座標ごとのresolved snapshot value、publish sessionのlocal hashとfresh remote取得hash、
   signatureおよび同一source`japicmp`を照合するEvidence方式でよいか。
6. CLI認証と既存remote stateのread-only確認は完了したが、専用workflow / environment、PR / mainおよび最終source commitは
   未承認のため、現時点ではremote publishを`NO-GO`のまま保持してよいか。

推奨結論は上記6点を承認し、確認済みremote readinessを前提に専用workflowの実装・local reviewへ進むことである。
remote snapshot publishそのものは、その結果と最終source commitに対する追加の個別Owner承認後だけ一度実行する。

2026年9月11日、Architecture Ownerは上記6点を修正条件付きで承認した。1、4、6は提案どおり承認し、2は24型全体を
現行release unitのaggregate signature、Phase 2初回published baselineをAudit 6型、Identity 10型、Session 3型、Security 0型として
Phase 1a固定baselineから分離した。3、5はremoteの共通`buildNumber`を要求せず、同一workflow run / source commit、座標ごとの
resolved snapshot value、24 payload SHA-256、signatureおよび同一source`japicmp`でrelease unitを固定する方式へ修正した。
修正反映後のPhase 2専用workflow実装とlocal reviewを承認する。remote snapshot publish自体は未承認とする。

同日、Architecture Ownerは6判断に対する承認条件の反映を確認し、13座標のpublish指定と検証manifestの乖離を防止する
機械的検査の追加をworkflow実装承認の修正条件とした。修正後のlocal verificationまでを承認対象とし、protected environment作成、
push、PR、main反映、workflow dispatchおよびremote snapshot publishは承認に含めない。

## 9. Workflow implementation and local review result

条件付き承認を反映し、次を実装した。

- `.github/workflows/publish-phase2-snapshot.yml`: `workflow_dispatch`、`main` / 40文字承認commit一致、read-only preflight、
  `phase2-internal-snapshot` environmentを使うpublish、hash-only manifest、fresh verify jobを分離した。
- `p2-c2-publish-unit.txt`: Root / Referenceを含まないPOM 2 / JAR 11の13座標を唯一の検証manifestとした。
- `p2-c2-publish-unit.ps1`: formal 14-project release unitからRoot aggregator POMだけを除いた集合とpublish manifestの
  packaging / artifact ID / module path完全一致を検査する共有loaderとした。
- `invoke-p2-c2-publish.ps1`: workflowとdry runのMaven `-pl`をpublish manifestから生成し、別の13座標一覧を保持しない。
- `verify-p2-c2-publish-state.ps1`: 座標別metadataからresolved snapshot valueを取得し、13 POM / 11 JARのSHA-256、
  source commit / workflow run、aggregate signatureおよび11 JARのsame-source `japicmp`をCapture / Verifyする。
- `verify-p2-c2-publish-dry-run.ps1`: GUID付き一時file repositoryでremote経路を再現し、終了時に一時repository、payload、
  manifestおよびreportを削除する。

2026年9月11日、source HEAD `50a3329aaacadc1c0210ff5e7c62c8910ae22281`に対してlocal dry runを再実行した。
初回実装reviewで通常artifactのmetadataに`classifier`要素が存在しない場合のStrictMode停止と、検証用POMに対する
`japicmp` skipを検出したため、それぞれ欠落classifierを空文字として扱い、`skipPomModules=false`で実比較を強制するよう修正した。
修正後は13座標deploy、座標別resolved value、24 payload hash、24型aggregate signatureおよび11 JARの実`japicmp`比較が
すべて成功し、各比較で`.diff` / XML生成を確認した。一時成果物のcleanupも成功した。

追加Owner条件に対しては、workflowのhard-coded `-pl`を削除し、workflow / dry runが同じpublish helperと13座標manifestを使用する
よう修正した。共有loaderはmanifestをformal 14-project release unitからRoot aggregatorだけを除いた集合と照合し、publish、
CaptureおよびVerifyの各経路で必ず実行する。修正後の完全dry runも成功した。

```text
Phase 2 P2-C2 C2-5 publish-state verification succeeded (13 coordinates / 24 hashes / aggregate signature / 11 japicmp comparisons).
Phase 2 P2-C2 C2-5 local publish dry run succeeded (13 coordinates / per-coordinate snapshot values / 24 hashes / signature / japicmp).
```

workflow YAMLのremote実行、protected environment作成、push、PR、manual dispatchおよびGitHub Packagesへのpublishは実施していない。
現在の判定は`WORKFLOW IMPLEMENTATION APPROVED / LOCAL VERIFIED / REMOTE PUBLISH NO-GO`である。

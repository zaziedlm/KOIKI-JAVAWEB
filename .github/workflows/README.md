# GitHub Actions

KOIKIでは、CIとartifact公開を別の権限境界として扱います。

## `ci.yml`

- pull requestと`main`へのpushで実行します。
- `ubuntu-24.04` / Temurin 21で、Maven Wrapperから`clean verify`を実行します。production runtimeを
  Linux-onlyとしたため、Windows matrixは再採用判断まで無効です。
- NullAwayのpositive、意図的違反のexpected failure、restoreを隔離fixtureから検証します。
- `Security Foundation Integration` jobは、隔離Maven repositoryへrelease unitをstageし、Root Reactor外の
  Customer-like Consumerをbuild／test／packageします。Java 21でbuildした同一JARをJava 21／25で実行し、
  Security依存、Public API fixture、secret non-exposureおよびcleanupを累積検証します。
- `Security Foundation Integration`はFramework側の外部利用互換性を検証するjobであり、Customer業務アプリの
  CI Templateではありません。`contents: read`だけを使用し、secretまたはPackages権限を追加しません。
- Phase 1b Milestone Aでは、隔離Maven repositoryへrelease unitをstageし、Starter細粒度fixtureと
  独立Customer-like ConsumerのProblem Details／Validation／Jackson例外経路を累積検証します。
- Milestone AのCP3 aggregate scriptは当時の後続依存禁止contractを保存し、Milestone B以降の通常CIからは
  呼び出しません。CP2／CP3回帰は、承認済みData依存を含む現在のConsumerを検証するCP7 aggregateへ引き継ぎます。
- 独立した`Milestone B Integration` jobは、CP7のaggregate scriptから隔離Maven repositoryへrelease unitを
  stageし、PostgreSQL 17 Testcontainersを使うCustomer-like Consumer、Flyway、transaction、structured log、
  DB healthのDOWN／restore、OSIV、同期Domain EventおよびMyBatis BOM境界を累積検証します。
- `Milestone B Integration`はPR #25で3回連続成功し、実行時間とTestcontainers cleanupの安定性を
  Owner Reviewしたうえで、main rulesetのrequired checkへ追加済みです。このjobは`contents: read`だけを使用し、
  secretまたはPackages権限を追加しません。
- `Milestone C Closeout` jobはCP10 aggregate scriptを実行し、CP8実OS process、package済みConsumerの
  Developer Journey、release unit／Public API／migration／table inventoryおよびCP9短縮Smokeを検証します。
  CP9性能数値を閾値にせず、`contents: read`だけで実行し、secretやPackages権限を追加しません。
- 通常の`Verify` jobは`contents: read`だけで、secretやpackage権限を使用しません。
- `Security Foundation Integration`は、final HEADのremote PRで1回成功し、cleanupと実行時間をOwner Reviewしたうえで、
  main rulesetのrequired checkへ追加済みです。同一commitの意図的な複数rerunは条件にしません。
- `Local Identity Session Audit Integration`はPhase 2 Milestone Bの累積CI jobです。B1 Audit、B2 Identity、B3 Session、
  B4 package済みReference journeyを3ラウンド連続実行し、Root Reactor、Null Safety、Public API / dependency / migration
  inventory、sensitive-outputおよび所有resource cleanupを累積検証します。
- このjobは`ubuntu-24.04`、Temurin 21、`contents: read`だけを使用し、secret、Packages、environment権限または
  Maven cacheを追加しません。job timeoutは60分、aggregate stepは52分とし、`if: always()`の最終stepへ5分を確保します。
- aggregateは一次失敗を保持したまま最終残留検査も報告します。最終stepはexpected HEAD、clean worktree、所有container、
  外部起動したJava child process、一時directoryおよびfixture targetを独立検査し、GitHub hosted runnerの破棄だけを
  cleanup成功証拠にしません。
- `Local Identity Session Audit Integration`は、同一final HEADで39分11秒、32分20秒、34分31秒のremote
  3回連続成功と全回cleanup成功をOwner Reviewし、2026年9月10日にmain rulesetのrequired checkへ追加済みです。
  Framework Repository固有のGateであり、Customer業務アプリへ同一の3ラウンドaggregateを必須化しません。
- 独立した`Public API Compatibility` jobだけが`contents: read`と`packages: read`を持ち、C1 baseline、
  inventory、japicmp正常系とGate 3 fixtureを同じTooling scriptで検証します。
- Public API jobはRepositoryの`GITHUB_TOKEN`だけを使用し、追加PAT secret、Maven cache、
  package write / deleteまたは認証済みGit credentialを使用しません。
- checkout後に認証済みGit操作を行わないため、credentialをlocal Git configへ保持しません。
- 外部Actionはfull commit SHAで固定します。tag更新時も差分reviewを必要とします。
- 通常の`Verify` jobのMaven cacheは高速化だけに用い、配布artifactまたは検証証拠として扱いません。

内部snapshot公開は独立workflowで扱います。`ci.yml`へpackage公開権限を追加しません。

## `runtime-compatibility.yml`

- pull request、`main` push、nightly schedule（03:17 JST）および`workflow_dispatch`で実行します。
- `Build Runtime Fixture (Java 21)` jobはTemurin 21とRepository WrapperでCLI JARを一度だけ生成し、
  class major `65`、source commit、working tree、JAR SHA-256をmanifestへ固定します。
- build jobはGate 3 negative guardsとJava 21 / 25 positive restoreを再現した後、JARとmanifestだけを
  retention 1日のimmutable workflow artifactとしてuploadします。packageまたはrelease artifactではありません。
- `Java Runtime Compatibility` jobはworkflow artifactをdownloadし、manifest commit、major、SHA-256を
  再確認してから、同一JARをTemurin 21 / 25で実行します。
- runtime jobはMaven、`javac`、compile、package、cacheまたはruntime別artifact生成を行いません。
- workflow全体は`contents: read`だけを使用し、secret、PAT、Packages権限、artifact公開または
  認証済みGit credentialを使用しません。
- checkout、setup-java、upload-artifact、download-artifactは公式release commitのfull SHAで固定します。
- `Java Runtime Compatibility`はC4のPR fresh runner成功とOwner承認後、main rulesetのrequired checkへ
  追加済みです。既存`Verify (ubuntu-24.04)`、`Public API Compatibility`、strict policy、PR保護および
  bypassなしを維持します。

## `publish-snapshot.yml`

- `workflow_dispatch`だけで起動し、preflight前にmain refとOwnerが承認した40文字のcommit SHAとの一致を検査します。
- `ubuntu-24.04`のpreflightは`contents: read`だけでCP10 closeout aggregateを完走します。
- preflight成功後のpublish jobだけが`packages: write`を持ち、`phase1b-internal-snapshot` environmentを使います。
- Maven server ID `github`へRepository固有の`GITHUB_TOKEN`を接続し、PATをRepository secretへ保存しません。
- 配布先はworkflowの`altSnapshotDeploymentRepository`だけに指定し、BOM / ParentやCustomer POMへ継承させません。
- Root Aggregatorを除く9 deployable artifactsだけを明示選択し、fixtureを公開しません。
- `deployAtEnd=true`と単一実行のconcurrencyにより、build失敗時と同時実行時の部分公開riskを抑えます。
- 実公開はPOM / workflowのreview、local file repository dry run、main CIおよびOwner承認後にだけ実行します。
- C1 timestamped baselineを上書きせず、Architecture Contract／ArchUnit Rulesのjapicmpは引き続きC1の固定
  timestampとSHA-256を比較元にします。Starter 4件とTestingはPhase 1b初回baseline候補です。
- publish後はfresh runnerと空Maven repositoryから9座標をGitHub Packages経由で取得し、Root Reactor外の
  Customer-like Consumerをbuild／testします。通常Maven cacheやFramework source pathで成功させません。
- credentialはRepositoryの`GITHUB_TOKEN`だけを使用し、checkout credential、PAT、認証付きURLまたは
  token実値をsource、log、artifactへ残しません。
- environment設定、workflow dispatch、実公開、remote resolve EvidenceはGate 10-4で個別にOwner確認し、
  main最終CIやremote検証前にPhase 1bを`COMPLETE`としません。

Phase 1a C1の4-artifact公開実績はvalidation文書とGit履歴で保持し、現在のpublish入口はこの1 workflowへ
統合する。package済みConsumerのweb／maintenance journeyは同じ承認SHAのpreflightで既に外部processとして検証するため、
publish後は配布境界に固有な9座標のremote resolveとConsumer build／testを必須とし、同じjourneyの重複実行は必須にしない。
packaging、配布設定または正式Application Templateが変わる場合は、remote artifactからの起動journeyを再び必須化する。

## `publish-phase2-snapshot.yml`

- Phase 1bの9-artifact workflowを変更せず、Phase 2 C2-5専用の`workflow_dispatch`入口とする。
- main refとOwner承認済み40文字commitをauthorizeし、C2-2 package、C2-4 aggregate signatureおよびC2-5 local publish dry runを
  read-only preflightで検証する。
- Root aggregatorを除くPOM 2 / JAR 11の13座標を明示し、publish jobだけが`phase2-internal-snapshot` environmentと
  `packages: write`を使用する。
- publish jobとlocal dry runは同じ13座標manifestからMaven project listを生成し、formal 14-project unitからRoot aggregatorだけを
  除いた集合との完全一致をpublish / Capture / Verify前に機械検査する。
- publish sessionのhash-only manifestをretention 1日で受け渡し、fresh jobが座標ごとのresolved snapshot value、13 POM / 11 JARの
  SHA-256、aggregate signatureおよび11 JARの同一source `japicmp`変更0を検査する。
- 座標ごとに既存snapshot公開回数が異なるため、共通build numberをrelease unit identityにしない。
- publish直前に各座標のunclassified POM / JAR snapshotVersion集合を保存し、publish後に増えた値が各payloadでちょうど1件で
  あることを要求する。POM / JAR座標では両者のresolved value一致も要求し、過去履歴、checksum entryまたは別classifierを
  今回のpublish identityへ混入させない。
- pre-publish inventoryはpublish前にretention 7日のworkflow artifactとして保存する。Capture / Verifyはmetadata上の最新値を
  推測せず、同一runの差分で確定したtimestamped valueとhash-only manifestを使用する。
- environment作成、push、PR、main merge、dispatchおよびremote publishは、それぞれのOwner承認前に実施しない。
- Phase 2初回accepted baselineはsource commit `af7b4f71d885fe4991e5fcf85fcf8888aec6a539`、workflow run
  `34701933485`でpublish / Verify済みである。詳細は[Gate C remote Evidence](../../docs/architecture/validation/phase2-gate-c-c2-remote-evidence.md)に記録する。

# Phase 1a Internal Snapshot — C1実装・検証計画

**準備日:** 2026年8月26日<br>
**対象branch:** `feature/phase1a-internal-snapshot`<br>
**状態:** C1 COMPLETE / GATE 4 ACCEPTED<br>
**Ownership:** Tooling（Architecture Contract成果物のみFramework）<br>
**対象:** C1 BOM / Parent / Architecture Contract / ArchUnit Rules内部snapshot公開<br>
**開始baseline:** `main` / `9aa0b1a`（B5完了PR #13 merge）<br>
**調査commit:** `9a04921`

## 1. 目的と完了条件

Phase 1a実行計画のG2、G4、Milestone CおよびDoD 1a-3に基づき、承認済み4成果物を
`0.1.0-SNAPSHOT`の同一release unitとしてGitHub Packagesへ公開し、repository上のPOM / JAR、
timestamped snapshot、checksumおよび依存解決を実証する。

C1は次をすべて満たしたときだけ`COMPLETE`とする。

1. 公開対象が承認済み4成果物だけで、Root Reactor、fixture、TemplateおよびWalking Skeletonを含まない。
2. BOM、Parent、Contract、ArchUnit Rulesを依存順に同一Maven sessionからdeployする。
3. publish前にRoot Reactorと既存CI相当のquality gateが成功する。
4. 通常CIとpreflightは`contents: read`だけを維持し、publish jobだけが`packages: write`を持つ。
5. `GITHUB_TOKEN`を使用し、PAT、credential実値または認証付きURLをRepositoryとlogへ保存しない。
6. repository上のPOM / JAR、timestamped snapshot、公開元commitおよびSHA-256を記録する。
7. GitHub Packages URLから隔離local repositoryへ4成果物を解決し、Reactorまたは通常local installへの
   フォールバックがないことを確認する。
8. 同時実行、失敗、手動再実行およびsnapshot保持の挙動を制御し、既存quality gateを壊さない。
9. Repository外Consumerと意図的ArchUnit違反はC2へ残す。

## 2. 2026年8月26日のread-only調査結果

### 2.1 Git・runtime・baseline

| 項目 | 結果 |
|---|---|
| branch / remote | `feature/phase1a-internal-snapshot`、local / remoteとも`9a04921` |
| main / origin/main | ともに`9aa0b1a` |
| worktree | clean |
| Java | Temurin 21.0.12.1 |
| Maven Wrapper | 3.9.16 |
| Repository | `zaziedlm/KOIKI-JAVAWEB`、public、default branch `main` |
| GitHub公開Packages画面 | Maven filterで`0 packages`。C1の4座標名なし |

GitHub CLI 2.95.0は保存tokenが失効しており、認証済みPackages REST APIは使用できなかった。
公開画面上は既存Maven packageがないが、実公開直前にCLIを再認証し、認証済みAPIまたはRepository UIで
対象packageとversionが未作成であることを再確認する。

### 2.2 実効Reactorと公開対象

通常のRoot Reactorは次の5 projectを順に処理する。

```text
1. org.koikifw:koiki-dependencies-bom:0.1.0-SNAPSHOT       pom
2. org.koikifw:koiki-parent:0.1.0-SNAPSHOT                 pom
3. org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT jar
4. org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT         jar
5. org.koikifw:koiki-javaweb-fw-reactor:0.1.0-SNAPSHOT     pom
```

次の明示的なproject listでは1〜4だけが同じ順で選択され、Root Reactorは除外された。

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress `
  -pl koiki-dependencies-bom,koiki-parent,koiki-architecture-contract,koiki-archunit-rules `
  validate
```

`-am`は4 projectをすべて明示選択しているため不要とする。将来moduleを自動的にdeploy対象へ加えないため、
artifactIdの明示listをworkflowと検証の契約として維持する。

### 2.3 生成artifactと実効配布設定

| 成果物 | 現在のmain artifact | sources / javadoc |
|---|---|---|
| BOM | project POM | なし |
| Parent | project POM | なし |
| Architecture Contract | POM + main JAR（15 entries） | なし |
| ArchUnit Rules | POM + main JAR（33 entries） | なし |

sources / javadoc artifactはG2またはC1の承認対象ではないため追加しない。Contract / Rulesのtest classや
test dependencyはmain JARへ含まれていない。

現在は全projectで`distributionManagement`が未定義である。Maven 3.9.16の実効lifecycleは
Maven Deploy Plugin 3.1.4を使用するが、RepositoryのPOMではversionと`deployAtEnd`を固定していない。

### 2.4 baseline regression

| 検証 | 結果 |
|---|---|
| Root Reactor `clean verify` | SUCCESS。Contract 4件、ArchUnit Rules 65件成功 |
| Feature Template script | SUCCESS。正常系、Tier別ArchUnit負例2経路、Tier別NullAway負例2経路、復元成功 |
| Null Safety script | SUCCESS。positive、expected negative、restore成功 |

負例buildの非0終了はscriptが期待する結果であり、最終restoreを含む各script全体は終了コード0で完走した。

## 3. 現行仕様との照合

2026年8月26日にGitHub DocsとApache Maven公式文書を再確認した。

1. GitHub PackagesのApache Maven registryは`SNAPSHOT`をサポートする。
2. Actionsから同じRepositoryに公開する場合は`GITHUB_TOKEN`を使用でき、jobに
   `contents: read`と`packages: write`を与える。
3. Maven registryはrepository-scopedで、packageは紐づくRepositoryの可視性と権限を継承する。
4. Maven server IDと`distributionManagement`またはalternate deployment repositoryのIDを一致させる。
5. Mavenは`0.1.0-SNAPSHOT`を同一session timestampとbuild numberを持つ一意snapshotへ変換し、
   version-level metadataでConsumer解決を行う。
6. Maven Deploy Plugin 3.1.4の`deployAtEnd=true`は、multi-module buildが失敗した場合に
   reactor projectをdeployしない。

確認先:

- https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry
- https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages
- https://docs.github.com/en/actions/tutorials/publish-packages/publish-java-packages-with-maven
- https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html
- https://maven.apache.org/repositories/remote.html

## 4. Gate 1A承認済み実装

### 4.1 POM ownershipと配布設定

正式成果物POMは配布先から中立に保ち、配布先IDとURLはpublish workflowの実行引数だけに指定する。

```text
id: github
snapshot URL: https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB
```

- BOM、Parent、Contract、RulesおよびRoot Reactorには`distributionManagement`を定義しない。
- Maven Deploy PluginのC1固有設定もBOM / Parentへ定義せず、Feature Templateと将来Customerへ継承させない。
- `altSnapshotDeploymentRepository`と`deployAtEnd=true`は、4成果物を明示選択したpublish invocationだけへ渡す。
- release用`repository`は定義せず、C1設定を正式releaseへ流用できないようにする。
- `groupId`、`artifactId`、version、module graph、Public APIおよびproduction dependencyは変更しない。

### 4.2 publish workflow

`.github/workflows/publish-snapshot.yml`は通常CIから分離し、次の境界とする。

| 項目 | 案 |
|---|---|
| trigger | `workflow_dispatch`のみ |
| ref | `refs/heads/main`だけを許可 |
| commit確認 | preflight前のauthorize jobでrequired inputのexpected commit SHAと`github.sha`を照合 |
| concurrency | C1固定group、`cancel-in-progress: false` |
| preflight | Windows / Ubuntu、`contents: read`のみ、既存`ci.yml`相当の3検証 |
| publish | Ubuntu、preflight成功後、`contents: read` + `packages: write` |
| environment | `phase1a-internal-snapshot`。実公開前にOwner approval ruleをRepository側で確認 |
| checkout | full commit SHA固定Action、`persist-credentials: false` |
| Maven auth | `actions/setup-java`がserver ID `github`へ`GITHUB_ACTOR` / `GITHUB_TOKEN`参照を生成 |
| deploy対象 | artifactId明示の4 project、workflow限定のalternate snapshot repository、`clean deploy`、`deployAtEnd=true` |
| timeout | preflight / publishそれぞれ明示し、無期限実行を避ける |

通常`ci.yml`には`packages: write`、publish triggerまたはcredentialを追加しない。fork PR、
`pull_request_target`、Repository secretのPAT、認証付きURLおよび自動削除tokenは使用しない。

### 4.3 local file repository dry run

Gate 1承認後、POMとworkflow実装前後の差を隔離された一時file repositoryで検証する。

```text
clean deploy
  -> artifactIdを明示した4 projectだけ
  -> altSnapshotDeploymentRepositoryで一時file URLへ接続
  -> deployAtEnd=true
  -> credentialなし
```

dry runでは次を機械確認する。

1. `org/koikifw`以下に4 artifactIdだけが存在し、Root / fixture / Template / `ws-*`が存在しない。
2. BOM / ParentはPOM、Contract / RulesはPOM + JARだけを持つ。
3. 4成果物のversion-level metadataが同一timestampを示す。
4. JARとPOMのchecksumを記録する。
5. 別の隔離local repositoryからfile repository経由で4座標を解決できる。
6. 一時directoryを削除した後もtracked worktreeがcleanである。

### 4.4 実公開とrepository検証

実公開は、実装PRをmainへmergeし、main CI、environment、対象commitおよびPackages既存状態を
Ownerが再確認した後に、Ownerが手動dispatchする。

成功workflowでは次をEvidenceとして保持する。

- Repository、workflow run URL / ID、公開元`github.sha`、actor、実行日時
- Mavenが解決した4成果物のtimestamped snapshot
- remoteから隔離local repositoryへ取得したBOM / Parent POM、Contract / Rules POM + JAR
- 上記6ファイルのSHA-256
- 4成果物の依存関係と、Root / fixture / Template / `ws-*`がpackage一覧にないこと
- credential実値、認証headerおよび認証付きURLがlog / artifactにないこと

同じbase snapshotの再実行は別timestampを生成し得る。自動retryと自動削除は行わず、部分公開または
network failure時はpackage状態をread-only確認してからOwner Reviewへ戻す。C1 / C2 / C3で参照する
snapshotはPhase 1a中に削除しない。

## 5. C1 / C2責務境界

| C1 | C2 |
|---|---|
| 同一Repositoryのpublish workflowとGitHub Packages上のartifact成立 | 別Repositoryの独立Consumer |
| 4成果物のPOM / JAR / metadata / checksum | snapshotだけを使うConsumer build |
| Repository URL経由の隔離解決 | 意図的ArchUnit違反とADR message |
| credential非露出とpublish権限境界 | Consumer用PAT / `GITHUB_TOKEN`運用の実演 |

C1内の解決確認はartifact成立を検査するだけで、Customer / Reference相当のsource、業務moduleまたは
意図的architecture violationを追加しない。

## 6. Owner Review Gate

| Gate | Review対象 | 承認条件 | 状態 |
|---:|---|---|---|
| 1 | 調査結果、公開対象、POM配置、workflow、credential、dry run、C2境界、stop条件 | 4成果物以外を公開せず、通常CIをread-onlyに保ち、実公開前にlocal dry runと追加Reviewを置く | ACCEPTED（2026年8月26日、Shuichi Kataoka） |
| 1A | Parent配布設定の継承影響とworkflow限定overrideへの変更 | Customer / TemplateへKOIKI repositoryを継承させず、4成果物のpublish invocationだけに配布先を与える | ACCEPTED（2026年8月26日、Shuichi Kataoka） |
| 2 | POM / workflow実装とlocal file repository dry run | 4成果物だけが同一timestampでdeploy / resolveされ、tracked worktreeと既存quality gateが正常 | ACCEPTED（2026年8月26日、Shuichi Kataoka） |
| 3 | PR / main CI、environment、package既存状態、公開commit | Windows / Ubuntu成功、Ownerが実公開対象commitと手動dispatchを承認 | ACCEPTED（2026年8月26日、Shuichi Kataoka） |
| 4 | 実公開、remote resolve、checksum、credential非露出 | repository上の6ファイルと4座標を確認し、Evidenceが揃う | ACCEPTED（2026年8月26日、Shuichi Kataoka） |

Gate 1承認前はPOM、workflow、credentialおよびGitHub Packagesを変更しない。Gate 2 / 3承認前は
GitHub Packagesへ書き込まない。

2026年8月26日のOwner ReviewでGate 1案を承認し、Parent継承問題の検出後、同日の追加ReviewでGate 1A案も
承認した。Gate 1Aに従ってPOMを配布先中立へ戻し、workflow限定override、local file repository dry run、
実効POMおよび既存quality gateを再検証した。Ownerは同日にGate 2も承認した。PR #14のenvironment設定、
PR CIおよびGitHub Packages既存状態をread-only確認した後、Ownerは同日にGate 3も承認した。PR #14をmainへ
mergeし、main CI成功を確認した後、Ownerは手動dispatchでGate 4の実公開を実施した。workflow run、
timestamped snapshot、remote resolve、checksumおよびcredential非露出を確認し、同日にOwnerはGate 4も承認した。
C1は完了した。

### 6.1 Gate 1実装時のParent継承問題とGate 1A

Gate 1承認後、BOMとParentへ`snapshotRepository`、Deploy Plugin 3.1.4および`deployAtEnd=true`を
実装し、4成果物の実効POMとlocal file repository dry runを確認した。4成果物自体は承認どおりの
配布構成になったが、Parentの`distributionManagement`とplugin設定はContract / Rulesだけでなく、
Parentを利用するFeature Templateおよび将来のCustomer projectにも継承されることを実効POMで確認した。

```text
org.koikifw.templateverification:catalog-feature
  distributionManagement.snapshotRepository.url
    = https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB
```

この状態ではCustomer成果物がKOIKI Repositoryをdeploy先として持ち、Framework / Customer Ownershipと
「4成果物だけを公開する」境界に反する。Gate 1のPOM配置案をそのまま確定せず、Gate 1Aへ戻す。

推奨修正は次のとおり。

1. BOM / ParentからC1用`distributionManagement`とDeploy Plugin設定を除去し、正式成果物POMを
   repository固有のpublish設定から中立に保つ。
2. `publish-snapshot.yml`の4成果物を明示したdeploy invocationだけに、Maven Deploy Plugin 3.1.4の
   `altSnapshotDeploymentRepository=github::https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB`と
   `deployAtEnd=true`を渡す。
3. Maven server ID `github`、job権限、credential境界およびRoot除外方式はGate 1承認内容を維持する。
4. local file repository dry runも同じ`altSnapshotDeploymentRepository`経路で再実行する。

2026年8月26日にOwnerがGate 1Aを承認し、上記4点を実装した。

### 6.2 Gate 2 local dry run最終結果

Gate 1Aの承認方式と同じalternate snapshot repository経路で、builder、配布先およびresolverの3 repositoryを
一時directoryへ分離し、file repositoryへのdeployと別の隔離local repositoryからのresolveに成功した。

| 項目 | 結果 |
|---|---|
| deploy対象 | BOM、Parent、Architecture Contract、ArchUnit Rulesの4 artifactIdだけ |
| timestamped snapshot | `0.1.0-20260826.072829-1`（4成果物で同一） |
| payload | BOM / Parent POM、Contract / Rules POM + JARの6ファイル |
| sources / javadoc / Root / fixture | なし |
| isolated resolve | `--non-recursive`でReactor workspaceを除外し、4座標すべてSUCCESS |

| payload | SHA-256 |
|---|---|
| Architecture Contract JAR | `894782E8814367053A14DC8E357C58E3F93439A4F6AB8BA65929FC146F3519D1` |
| Architecture Contract POM | `7BE7635FE5E776FB0F5B5E4935DB0054D08F6D3CCE01EE7016275C685E1D926F` |
| ArchUnit Rules JAR | `7D231DDAC95059EDCC1A75A2EFF6F70CDC3CDD67A2C57ECDE42439316E066FCF` |
| ArchUnit Rules POM | `7B24A824B9EBD55794B7A626AE0FBB52A0781FFD1ECEFC18A899B629F4FEDA45` |
| Dependencies BOM POM | `63C7AB55E1BB2FE290E795A59212B6314F0347104DC9B536BD4EBDBE903183DF` |
| Parent POM | `ADC149D5C693BDCCBA008FD5F6BE8D5DF3BE5F43DCEBACCB47A72892C4BDAE37` |

最初のresolve確認ではRoot ReactorのMaven workspace readerがBOM / Parent POMをRepository外から補完したため、
`--non-recursive`を追加した。最終dry runではbuilder local repositoryも分離し、共有`.m2`の既存KOIKI metadataに
依存せず、4座標をfile repositoryから取得した。一時directoryは検証後に削除し、GitHubへ接続していない。

### 6.3 実効POMとregression

| 検証 | 結果 |
|---|---|
| BOM / Parent / Contract / Rules実効POM | `distributionManagement=0`、C1固有`deployAtEnd=0` |
| 生成Catalog / Approval実効POM | `distributionManagement=0`、C1固有`deployAtEnd=0` |
| Root Reactor `clean verify` | SUCCESS。Contract 4件、ArchUnit Rules 65件成功 |
| Feature Template script | SUCCESS。正常系、Tier別ArchUnit負例2経路、Tier別NullAway負例2経路、restore成功 |
| Null Safety script | SUCCESS。positive、expected negative、restore成功 |
| `git diff --check` | SUCCESS |

workflowはActionをfull commit SHAで固定し、publish前authorize、権限分離、明示4 project、workflow限定URLを
目視確認した。local環境には`actionlint`とPyYAMLがないため、GitHub Actionsによるworkflow構文検証は
Gate 3のPR / main CIで確定する。

### 6.4 Gate 3 environment・CI・package確認結果

commit `aa5fe0b`を対象とするPR #14に対して、environment設定、PR CI（main CI相当）およびGitHub Packages
既存状態をGitHub CLI経由のread-only呼び出しで確認した。workflow dispatchおよびPackagesへの書き込みは
実施していない。

| 確認項目 | 結果 |
|---|---|
| PR | #14（`feature/phase1a-internal-snapshot` -> `main`）、`OPEN` / draft、`mergeable` |
| PR CI | `Verify (windows-2025)` SUCCESS、`Verify (ubuntu-24.04)` SUCCESS（run `32944075389`） |
| environment `phase1a-internal-snapshot` required reviewer | `zaziedlm`登録済み |
| prevent_self_review | `false`（承認者が申請者本人のみのため意図通り） |
| deployment branch policy | `main`のみ許可（custom branch policy 1件） |
| administrators bypass | `can_admins_bypass=false`（無効化済み） |
| GitHub Packages既存package | `GET /users/zaziedlm/packages?package_type=maven`の結果`[]`。C1対象4座標は未作成 |

2026年8月26日にOwnerがGate 3を承認した。

### 6.5 PR #14 merge結果とGate 4対象commit

Gate 3承認後、Gate 3記録commit（`8b88ed2`）をpushし、PR #14をready for reviewへ変更した。
ready化後の再実行CIも成功し、OwnerがGitHub UI上でPR #14をmainへmergeした。

| 項目 | 結果 |
|---|---|
| PR ready化後CI | `Verify (windows-2025)` SUCCESS、`Verify (ubuntu-24.04)` SUCCESS（run `32949037333`） |
| PR状態 | `MERGED`（`mergedAt` 2026-08-26T08:51:43Z） |
| merge commit | `9573b1cf38713d51707a14884230d5bd5e1d97fb` |
| main CI（merge commit） | SUCCESS（run `32949946012`） |

Gate 4の`expected_commit`入力値は、上記merge commit `9573b1cf38713d51707a14884230d5bd5e1d97fb`とする。

### 6.6 Gate 4 実公開Evidence

Owner手動dispatch（workflow run `32951187676`、`event: workflow_dispatch`、対象commit
`9573b1cf38713d51707a14884230d5bd5e1d97fb`）により、`authorize` / `preflight`（Windows・Ubuntu）/
`publish`の4 jobがすべて成功した。

| 項目 | 結果 |
|---|---|
| timestamped snapshot | `0.1.0-20260826.091429-1`（4成果物で同一、buildNumber 1） |
| GitHub Packages公開package | `koiki-dependencies-bom`、`koiki-parent`、`koiki-architecture-contract`、`koiki-archunit-rules`の4件だけ |
| sources / javadoc / Root / fixture / Template / `ws-*` | なし |
| 隔離local repositoryへのremote resolve | 認証済みcurlで6ファイルを取得し成功 |
| credential露出 | publish job logで`AUTHORIZATION: basic ***`とマスク済み。`GITHUB_TOKEN`実値および認証付きURLの露出なし |

remoteから取得した6ファイルのSHA-256は次のとおり。

| ファイル | SHA-256 |
|---|---|
| Architecture Contract JAR | `947EE8CF0E109FE58D81E6008A56C06C8F4C035FF76BDF462F8F6BD9BB50DE45` |
| Architecture Contract POM | `7BE7635FE5E776FB0F5B5E4935DB0054D08F6D3CCE01EE7016275C685E1D926F` |
| ArchUnit Rules JAR | `A51E26E7386D19E53C18BD63BC4E4F95EC1EAE471F39D519D6AE0CBC7C2DF3F2` |
| ArchUnit Rules POM | `7B24A824B9EBD55794B7A626AE0FBB52A0781FFD1ECEFC18A899B629F4FEDA45` |
| Dependencies BOM POM | `63C7AB55E1BB2FE290E795A59212B6314F0347104DC9B536BD4EBDBE903183DF` |
| Parent POM | `ADC149D5C693BDCCBA008FD5F6BE8D5DF3BE5F43DCEBACCB47A72892C4BDAE37` |

4件のPOMのSHA-256はGate 2 local dry runの値と一致した（POM内容が決定的なため）。JARのSHA-256はビルドごとの
タイムスタンプ差異により、local dry run時の値とは異なる。検証に使用した一時ディレクトリは確認後に削除し、
tracked worktreeは変更なし（`git status`で本文書の編集差分のみ）。

2026年8月26日にOwnerがGate 4を承認し、C1は完了した。

### 6.7 GitHub Enterprise Cloud移管時の再認定

将来の会社GitHub Enterprise Cloudへの移管後も、POM中立、明示4成果物、最小権限、Owner Gateおよび
checksum証拠の原則を維持する。移管方式が通常Repository transferかGitHub Enterprise Importerかにより
packageの扱いが異なるため、次を移管前後の再認定対象とする。

1. Maven endpointのOwner / Repository名、Repository visibilityおよびpackage access。
2. GitHub Packagesの移管可否。Enterprise ImporterではPackagesを別途再公開・照合する。
3. `GITHUB_TOKEN`、別Repository Consumer、開発者取得およびEnterprise policyの認証境界。
4. Environment承認、Actions policy、runner、snapshot保持、監査および正式release repository。

この再認定は現在のC1構造を変更する条件ではなく、移管環境固有の配布先と権限を差し替えるGateとする。

## 7. 見積もり再校正

C1の残作業は次のrangeで扱う。経過日数または納期ではない。

| 作業 | 想定range |
|---|---|
| POM / workflow実装、local dry run、Validation更新 | 2〜4時間 |
| PR / Windows・Ubuntu CI確認と修正 | 1〜3時間 |
| GitHub認証・environment確認、実公開、remote検証 | 1〜3時間 |
| Owner Review | 各Gate 10〜30分程度 |

GitHub Packagesの初回package作成、timestamped metadata、認証および実公開後の取得は未実証のため、
合計rangeを4〜10時間とする。公開失敗時の調査やGitHub側障害待ちはrange外とする。

## 8. Stop condition

- 承認済み4成果物以外をdeploy対象へ含める必要がある。
- Maven coordinates、version、module graphまたはPublic APIの変更が必要になる。
- PAT、高権限token、credential実値または認証付きURLの保存が必要になる。
- PR、fork由来codeまたはpreflight jobへ`packages: write`を与える必要がある。
- Root Reactor、既存CI相当quality gateまたはlocal dry runが失敗する。
- 4成果物が同一Maven sessionのtimestamped snapshotとして成立しない。
- package ownership、対象version、environmentまたはActions accessを実公開前に確認できない。
- credential値がlog / artifactへ出力される。
- 部分公開、409等の競合または予期しない再公開挙動が発生する。
- C2以降または後続Phaseの成果物がC1完了に必要になる。

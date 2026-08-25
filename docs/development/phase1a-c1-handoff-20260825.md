# Phase 1a C1 作業引継ぎ — 2026-08-25

## 1. この文書の位置づけ

この文書は、Phase 1a Milestone B完了後に作業を区切り、新規AIセッションでC1「内部snapshot公開」を
安全に開始するための運用上の引継ぎメモである。設計判断やC1の実装証拠の正本ではない。
C1固有の計画・Owner Review・実装証拠は、次回セッションで
`docs/architecture/validation/phase1a-internal-snapshot.md`を作成して管理する。

判断が競合する場合は、次の順に正本を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/development/KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md`
4. 同計画のG2 Maven coordinates、G4 CI / artifact repository、Milestone C
5. `docs/architecture/KOIKI-JavaWeb-FW_Repository_Architecture_v0.1.md`
6. `docs/architecture/adr/`と`docs/architecture/validation/`
7. 実効構成であるRoot / release unitのPOM、Maven Wrapper、GitHub Actions workflow

C1は業務機能を変更しないため、開始時の適用Skillは`koiki-project-overview`である。Controller、Use Case、
Domain Model、Repository等の業務成果物を変更する必要が生じた場合だけ、作業を止めて
`koiki-business-feature-work`の適用要否を確認する。

## 2. 引継ぎ時点のGit・remote状態

| 項目 | 状態 |
|---|---|
| B5 PR | [PR #13](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/13)をmainへMerge済み |
| `main` merge commit | `9aa0b1a5db57f2632e98682a74cf281146a83f4a` |
| main CI | [run #32826705894](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/32826705894)、Windows / UbuntuともSUCCESS |
| Windows job | [Verify (windows-2025)](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/32826705894/job/97736207689) |
| Ubuntu job | [Verify (ubuntu-24.04)](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/32826705894/job/97736207984) |
| C1 branch | `feature/phase1a-internal-snapshot` |
| C1 branch baseline | `main` / `9aa0b1a5db57f2632e98682a74cf281146a83f4a` |
| C1実装 | 未着手。branch作成と本handoffだけ |
| B5 branch | ローカル・remoteとも当面残置するOwner方針 |

branch作成前の`main`は`origin/main`と同期し、worktreeはcleanであった。C1 branchは同commitから作成した。
本handoffと`docs/development/README.md`の索引更新をbranchの最初のcommitとして保存する。remote pushと
upstream設定は未実施とし、必要になった時点でOwnerが判断する。

## 3. Phase / Ownership / 対象

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1a Milestone C / Milestone A・B COMPLETE / C1 START READY |
| Workflow | C1 内部snapshot公開 |
| 実装Ownership | Tooling（publish workflow、Maven配布設定、検証・証拠） |
| 成果物Ownership | BOM / Parent / ArchUnit RulesはTooling、Architecture ContractはFramework |
| Repository | GitHub Packages Apache Maven registry |
| 検証 | 4成果物のpublish、repository上のPOM / JAR、checksum、依存解決、credential非露出 |
| 後続 | C2 Repository外Consumer、C3 japicmp、C4 runtime matrix、C5 Closeout |

C1は、G2で承認した同一version release unitをG4で承認した内部repositoryへ公開し、repository上の
成果物として成立することを証明するWPである。C2の独立Consumer実装、正式release、一般公開または
Maven Centralへの配布を行う作業ではない。

## 4. C1開始baseline

### 4.1 公開先

```text
https://maven.pkg.github.com/zaziedlm/KOIKI-JAVAWEB
```

GitHub PackagesのApache Maven registryをPhase 1a内部snapshot repositoryとして使用する。

### 4.2 公開対象

次の4成果物だけを`0.1.0-SNAPSHOT`の同一version release unitとして公開する。

```text
org.koikifw:koiki-dependencies-bom:0.1.0-SNAPSHOT
org.koikifw:koiki-parent:0.1.0-SNAPSHOT
org.koikifw:koiki-architecture-contract:0.1.0-SNAPSHOT
org.koikifw:koiki-archunit-rules:0.1.0-SNAPSHOT
```

Root Reactor、Feature Template、検証fixture、Walking Skeleton、`ws-*`、将来moduleおよび正式release
versionは公開しない。既存Maven coordinates、version、module graphおよびPublic APIは変更しない。

### 4.3 現在の未実装接続点

引継ぎ時点では、通常CIは`contents: read`だけで動作し、package書込み権限を持たない。
`distributionManagement`、C1用publish workflow、公開用settings、checksum記録およびsnapshot公開証拠は
まだ実装していない。ローカルMaven Repositoryへの`install`成功はC1の配布証明として扱わない。

## 5. G4で承認済みの公開・credential境界

1. `publish-snapshot.yml`を通常PR CIから分離する。
2. 起点は`workflow_dispatch`または検証済みmainへのpushとし、`clean verify`成功後だけ公開する。
3. publish jobだけに`contents: read`と`packages: write`を付与する。
4. Repository固有の`GITHUB_TOKEN`を使用し、publish用PATをRepository secretとして保持しない。
5. `pull_request_target`を使用せず、fork PRへsecretまたはwrite tokenを渡さない。
6. credential実値をPOM、workflow、settings template、logまたはValidationへ保存しない。
7. Consumer用のsettings例が必要な場合は環境変数を参照し、利用者ごとの設定で注入する。
8. 可変な`0.1.0-SNAPSHOT`だけで識別せず、公開元commit、timestamped snapshot、POM / JAR checksumを
   Evidenceへ記録する。
9. C1 / C2 / C3が参照するsnapshotをPhase 1a中に削除しない。自動削除用の高権限tokenを導入しない。

## 6. C1の完了条件候補

次回セッションでは、実効構成とGitHub Packagesの現在仕様を確認したうえで、少なくとも次を
Owner Review Gateへ具体化する。

1. 公開対象が承認済み4成果物だけで、Root Reactorやfixtureが除外される。
2. 4成果物が同一`0.1.0-SNAPSHOT` release unitとして、依存順序を保ってdeployできる。
3. publish前にRoot Reactorと必要なquality gateが成功する。
4. publish workflowだけが`packages: write`を持ち、通常CIとPRはread-onlyを維持する。
5. GitHub Packages上のPOM / JARと依存関係を確認し、公開元commitとchecksumを記録する。
6. local `install`や同一Reactor参照ではなく、repository URLを通した依存解決を確認する。
7. credential値をRepositoryとlogへ残さず、失敗時にも漏えいしない。
8. 同一snapshotの再公開、既存package、保持方針およびGitHub Packages側設定を事前確認する。
9. Windows / Ubuntu CIと既存B1〜B5のquality gateを壊さない。

C1とC2の依存解決証拠の分担は次回Gate 1で明確化する。C1はRepository内または公開workflowからの
artifact成立確認を担当し、別Repository Consumerと意図的ArchUnit違反はC2へ残す。

## 7. C1で行わないこと

- C2の別Repository Consumer作成、認証運用の実演、意図的ArchUnit違反
- C3のPublic API inventory確定、japicmp baseline、破壊検出
- C4のJava 21 build artifactを使ったJava 21 / 25 runtime検証
- C5のPhase 1a CloseoutとWalking Skeleton残置物の最終処置
- 正式release version、署名、Maven Central、Support、SBOM、Container / Cloud配布
- Runtime、Security、Reference業務、REST、SPA、Flyway Starter、MyBatis詳細
- Spring Modulith Level 1 / 2、Named Interface、非同期event
- 新規Starter、空module、未使用Public APIまたは将来package

## 8. Stop / Return condition

次のいずれかが必要または発生した場合は、公開や暫定回避へ進まずOwner Reviewへ戻す。

- 承認済み4成果物以外をdeploy対象へ含める必要がある。
- Maven coordinates、versionまたは同一version release unitを変更する必要がある。
- Repository secretとしてpublish用PATまたは高権限tokenを保存する必要がある。
- PRまたはfork由来codeへ`packages: write`を与える必要がある。
- credential、token、認証付きURLまたは秘密値がlog / artifactへ出力される。
- GitHub Packagesのpackage ownership、Actions accessまたは既存snapshotとの衝突を確認できない。
- 実公開前のdry runでRoot Reactorまたは既存quality gateが失敗する。
- local `install`だけで成功し、GitHub Packagesを通した成立証拠を作れない。
- C2以降または後続Phaseの成果物がC1完了に必要になる。

実際の`deploy`とGitHub Packagesへの書込みは外部状態を変更する。workflow実装とdry runをreviewした後、
公開対象、commit、workflow triggerおよび再実行方針をOwnerが承認してから実行する。

## 9. 次回セッションの開始手順

```powershell
git switch feature/phase1a-internal-snapshot
git status --short --branch
git log -5 --oneline --decorate
git show --stat --oneline main
java -version
.\mvnw.cmd -version
```

remoteへbranchをpushした場合は、先に次を追加する。

```powershell
git fetch origin
git pull --ff-only
```

期待状態:

- branchが`feature/phase1a-internal-snapshot`
- baselineに`9aa0b1a`が存在する
- 本handoffのcommitが履歴に存在する
- worktreeがclean
- Java 21とMaven Wrapper 3.9.16を実行可能
- C1用publish設定とValidationは未実装

本handoff作成時のCodex sandboxでは直接`java`がPATHから見えず、Maven Wrapperもsandbox内のnetwork制約で
配布物確認を完走できなかった。main CIは同baselineで成功済みだが、次回セッション開始時にJDK PATHと
Wrapperをローカル環境で再確認する。

## 10. 次回最初に行うこと

C1実装やsnapshot公開前に、次を調査してOwnerへ提示する。

1. 4成果物の実効POM、deploy順、生成されるPOM / JAR、source / javadoc artifactの現在状態
2. Root Reactorをdeploy対象から除外し、4成果物だけを同一release unitとして選択する方式
3. `distributionManagement`の配置Ownerと、GitHub Packages repository ID / URL
4. `publish-snapshot.yml`のtrigger、permissions、concurrency、environmentおよび失敗時挙動
5. `GITHUB_TOKEN`とMaven `settings.xml`の安全な接続方法、log masking
6. package未作成時と既存snapshot存在時のGitHub Packages側挙動
7. publish前dry run、実publish、repository上のartifact確認、checksum取得の手順
8. C1とC2の責務境界、Owner Review Gate、commit境界、Validation記録案
9. C1の作業負荷再校正とstop / return condition

最初にread-only調査とローカルdry run設計を行い、Owner Review Gate 1の承認前にworkflow追加、
credential接続またはGitHub Packagesへの実公開を行わない。

## 11. 新規セッションへの開始依頼文

次の文章を、新しいセッションの最初の依頼として使用できる。

> KOIKI-JavaWeb-FWのPhase 1a C1「内部snapshot公開」を開始します。Repository rootの`AGENTS.md`、
> `docs/agent/skills/koiki-project-overview/SKILL.md`、
> `docs/development/phase1a-c1-handoff-20260825.md`、Phase 1a実行計画のG2・G4・Milestone C、
> Repository Architecture、Root / release unitのPOMとGitHub Actionsを確認してください。作業branchは
> `feature/phase1a-internal-snapshot`、baselineはB5をmergeしたmainの`9aa0b1a`です。Milestone A・Bは
> COMPLETE、C1実装は未着手です。最初にGit・JDK・Maven環境、4成果物の実効配布構成、GitHub Packagesの
> 現在状態をread-onlyで確認し、公開対象、workflow / permission / credential境界、dry run、実公開、
> checksum証拠、C2との分担、Owner Review Gate、作業負荷、stop conditionを具体化してください。
> Gate 1承認前にGitHub Packagesへの書込みやC2以降の実装へ進めないでください。

## 12. 引継ぎ時点の検証

- PR #13とmain merge後CI run #32826705894はWindows / UbuntuともSUCCESS。
- C1 branchはCI成功済みmain `9aa0b1a`から作成した。
- branch作成前のmain worktreeはcleanで、`origin/main`と同期していた。
- C1のPOM、workflow、settings、credential、ValidationおよびGitHub Packagesは変更していない。
- 本handoffは作業再開用情報であり、C1設計承認、publish承認または実装完了を意味しない。

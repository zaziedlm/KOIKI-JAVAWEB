# Phase 1b CP10開始引継ぎ — 2026-08-30

## 1. この文書の位置づけ

この文書は、Phase 1b CP9を`LOCAL COMPLETE / OFFICIAL BASELINE RECORDED`として区切り、新規AI対話セッションから
同じMilestone C branchでCP10 Developer Journey／DoD／Gate 2 closeoutを安全に再開するための運用引継ぎである。
CP0〜CP9の設計判断や検証結果を置き換える正本ではない。CP10で得る最終証拠は、新規closeout validationと
Phase 1b実行計画へ記録する。

判断が競合する場合は、次の順に正本と実効構成を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. 業務moduleを変更する場合の`docs/agent/skills/koiki-business-feature-work/SKILL.md`
4. `docs/development/KOIKI-JavaWeb-FW_Phase1b実行計画_v0.1.md`
5. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md` §27.2、§27.5
6. `docs/architecture/validation/phase1b-cp1-*.md`から`phase1b-cp9-performance-baseline.md`
7. `docs/architecture/adr/`
8. 実効構成である各`pom.xml`、source、test、Maven Wrapper、検証script、workflow

CP10は新しいFramework機能を作る作業ではなく、Phase 1b成果物を利用者視点で再実行し、DoD、Ownership、
release unit、Public API、migration、ADR、Skills、CIおよびdeferred scopeを最終確認するcloseout作業である。

## 2. 引継ぎ時点のGit・remote状態

| 項目 | 状態 |
|---|---|
| branch | `feature/phase1b-operations-closeout`を継続する。CP10用の別branchは作成しない |
| HEAD | `f14f587bed4aec7aba5402e598251fd9f0c02a60` |
| HEAD subject | `docs(runtime): record CP9 official performance baseline` |
| main merge base | `b3973e66134898765b95796c3622aaa68759b4fd`（PR #25 merge commit） |
| upstream | `origin/feature/phase1b-operations-closeout` |
| remote差分 | 引継ぎ作成前はupstreamより3 commit ahead、behind 0 |
| worktree | 引継ぎ作成前はclean。このhandoffだけが次の未commit差分になる |
| push | CP10 local closeout commitまで必須ではない。新規セッション開始時に自動pushしない |
| Milestone C PR | 未作成。CP10 local完了後に同branchから1 PRとして接続する |

upstreamよりaheadの3 commitは次のとおりである。

| Commit | 内容 |
|---|---|
| `679b6f6` | CP8単一実行contractとlocal Evidence |
| `dff9d8c` | CP9性能baseline harness |
| `f14f587` | CP9公式baseline resultと完了Evidence |

新規セッションでは、localにあるこの3 commitをremote状態で上書きしない。最初にidentityを確認し、`git pull`、rebase、
reset、別branch作成またはpushを自動実行しない。

## 3. Phase / Ownership / 停止境界

| 項目 | 内容 |
|---|---|
| Phase / status | Phase 1b Runtime Foundation / CP1〜CP9 LOCAL COMPLETE / Milestone C IN PROGRESS |
| 次のCP | CP10 Developer Journey／DoD／Gate 2 closeout |
| Framework ownership | 既に実装済みのruntime Starter、設定契約、Architecture Contract。CP10で機能を追加しない |
| Tooling ownership | Customer-like Consumer、aggregate検証、closeout Evidence、必要最小限のCI接続 |
| Customer ownership | Consumer内の業務語彙、migration、maintenance task。Frameworkへ昇格しない |
| Public API | 新規追加を予定しない。既存inventoryと互換性を最終確認する |
| Production migration | 新規追加を予定しない。KOIKI／Customer ownership inventoryを再確認する |
| Deferred | Security、正式Reference、Level 2、非同期Domain Event、Oracle、cloud固有、正式release、Project Template |

CP10で新しいStarter、Public API、業務module、migrationまたは将来用packageが必要になった場合は、closeoutへ混在させず、
要求、Ownership、後続Phaseとの境界を提示してOwner判断を待つ。

## 4. CP0〜CP9の確定baseline

### 4.1 Milestone A — Runtime Core

- `koiki-starter-api`とCore Configuration、Jackson 3、Spring標準Resilience、API Versioning、
  Problem Details／Validationを正式release unitとCustomer-like Consumerから検証済み。
- Spring Modulith 2.1.1はLevel 0のtest scopeで、runtime依存を追加していない。
- ConsumerはRoot Reactor外で通常Maven coordinatesからartifactを解決し、Framework internal型を参照しない。

### 4.2 Milestone B — Data & Runtime Integration

- `koiki-starter-data`でKOIKI／Customer二階層FlywayとPostgreSQLを検証済み。
- `koiki-starter-observability`で構造化log、request correlation、`@Async`伝播とthread再利用漏えい負例を検証済み。
- Actuator DB healthのUP／DOWN／restore、readiness／liveness分類を検証済み。
- `koiki-starter-data-jpa`でOSIV false既定、Entity／Domain Web露出負例を検証済み。
- Tier 1／2 module間の値だけの同期Domain Event、rollback、楽観的lock、Modulith Level 0を検証済み。
- MyBatisはBOM 4.1.0管理だけで、Starter、Mapper、`SEPARATED`実装、runtime dependencyを追加していない。
- Milestone BはPR #25、main CI、required checksを経て`COMPLETE / ACCEPTED`済み。

### 4.3 CP8 — 単一実行

- 同じConsumer executable JARを明示的maintenance modeでnon-web processとして起動する。
- PostgreSQL session advisory lockを専用JDBC connectionで保持し、同一task keyのwinner／contenderを分離する。
- winner exit `0`、contender exit `10`、invalid input exit `64`を確認済み。
- 同一keyのDB副作用1回、異なるkeyの独立性、process kill後のlock解放／retryを実OS processで確認済み。
- Framework artifact、Public Java API、Framework production migrationは追加していない。
- 正本は`docs/architecture/validation/phase1b-cp8-single-execution.md`である。

### 4.4 CP9 — 性能baseline

- Tooling-ownedな`build-support/performance-baseline`で、同一fixture binaryをbare Spring Boot／KOIKIの2 assemblyから測る。
- workloadは`http-success`、`validation-rejection`、`db-write`、`startup`だけである。
- harnessはstatus／response contract、DB／log件数、sample件数、failure、再集計、3 schema、2 negative、cleanupを検査する。
- 性能数値をrequired check、案件SLA、保証値またはPC間の優劣判定に使用しない。

公式baseline identity:

| 項目 | 結果 |
|---|---|
| Harness commit | `dff9d8c0a1eb1b5e399e5dbf435534d4dec912b5` |
| Result commit | `f14f587bed4aec7aba5402e598251fd9f0c02a60` |
| Run ID | `4b236a7e99b74ca0bc1a542aa668d30a` |
| Result directory | `build-support/performance-baseline/results/20260829-232318` |
| Fingerprint | `gitDirty=false`、3 fork、startup 3 fork、warm-up 200、measurement 1,000、concurrency 1 |
| Raw result | 18,006 sample、failure 0件 |
| Aggregate | 8 variant／workload系列、4 paired comparison |

結果の正本は同directoryの`fingerprint.json`、`raw-results.json`、`aggregate.json`と、
`docs/architecture/validation/phase1b-cp9-performance-baseline.md`である。CP10でharnessまたは比較対象を変更しない限り、
公式性能値を再採取して上書きしない。構造回帰が必要な場合は`-Smoke`を使用する。

## 5. 現在の実効構成

### 5.1 正式release unit

Root release unitは10 projectsである。Aggregator、BOM、Parent、Architecture Contract、ArchUnit Rulesと、
Phase 1bで追加したruntime Starter群から構成される。実効project一覧はRoot Maven Reactorを正本とし、handoffの列挙を
固定inventoryの代わりにしない。

主なruntime leaf:

- `koiki-starter-api`
- `koiki-starter-data`
- `koiki-starter-data-jpa`
- `koiki-starter-observability`

### 5.2 Tooling-owned Consumerと検証

- `build-support/runtime-foundation-consumer`
  - `workitem`
  - `workreview`
  - `application`
- `build-support/runtime-foundation-verification`
- `build-support/performance-baseline`

これらはRoot Reactorへ追加せず、配布artifact、正式Reference、Customer成果物またはProject Templateへ昇格しない。

### 5.3 主な既存検証script

| Script | 主な範囲 |
|---|---|
| `verify-cp1-runtime-foundation.ps1` | release unit、独立Consumer骨格 |
| `verify-cp2-runtime-core.ps1` | Core／Jackson／Resilience／Versioning |
| `verify-cp3-runtime-core.ps1` | Problem Details／Validation |
| `verify-cp4-data-runtime.ps1` | PostgreSQL／Flyway／transaction |
| `verify-cp5-observability.ps1` | structured log／correlation／async |
| `verify-cp6-health-osiv.ps1` | health／OSIV／Web境界 |
| `verify-cp7-domain-event-mybatis.ps1` | Domain Event／Named Interface／MyBatis BOM |
| `verify-cp8-single-execution.ps1` | CP1〜CP8 aggregate、実process排他／crash recovery |
| `performance-baseline/verify-performance-baseline.ps1` | CP8回帰、paired performance、schema／cleanup |

script pathの前半は、CP1〜CP8が`build-support/runtime-foundation-verification/`、CP9が
`build-support/performance-baseline/`である。

## 6. CP10の目的

Phase 1b実行計画 §6.2、§7、§8、§12に基づき、次を一連のDeveloper Journeyとして再現し、Gate 2へ提示する。

```text
KOIKI BOM / Parent / Starterを選ぶ
  -> business moduleを配置する
    -> Customer migrationを追加する
      -> applicationを起動する
        -> version付きHTTP requestを受ける
          -> Use Case / Domain / Repositoryを通る
            -> Problem Details / log / healthを確認する
              -> maintenance processをWeb外から起動する
```

CP10は内部unit testの総和だけで完了させず、空の隔離Maven repositoryから正式artifactをstage／resolveし、packageした
Customer-like Consumerを外部から観測する。

## 7. CP10の完了条件

### 7.1 Developer Journey

1. Root release unitをclean buildできる。
2. 空の隔離Maven repositoryへ正式artifactをstageできる。
3. ConsumerがRoot Reactor、source path、Framework internal型、通常local cacheへ依存せずbuildできる。
4. packageしたConsumerを起動し、version付きHTTP、Problem Details、DB／migration、structured log、healthを確認できる。
5. 同じConsumer JARをnon-web maintenance processとして起動し、単一実行contractを確認できる。
6. dependency、artifact、Public API、migration、tableおよびcleanup境界を機械検査できる。

### 7.2 DoD／Governance

1. DoD 1b-1〜1b-8のEvidence所在と最終状態が揃う。
2. 全Phase共通DoDのBoot baseline、ADR／Owner approval、CI、Skills、table／Flyway ownershipを再判定する。
3. release unit、Public API inventory、japicmp baseline／snapshot方針を確定する。
4. Framework、Tooling、Customer、ReferenceのOwnershipが混在していない。
5. Security、Reference、Level 2、Oracle、cloud固有、正式release等をdeferred scopeとして明示する。
6. local最終検証、Milestone C PR required checks、merge後main CIの順序と完了状態を先取りしない。

### 7.3 完了状態

- local最終検証とOwner Review後: `CP10 LOCAL COMPLETE / GATE 2 LOCAL READY`
- Milestone C PR required checks成功後: `ACCEPTED — MAIN CI PENDING`
- merge後main最終CI成功後: `PHASE 1B COMPLETE / ACCEPTED`

## 8. 推奨する作業順

### Gate 10-1 — preflight／inventory

- Git identity、Java、Maven、Docker、branch／upstream差分を確認する。
- Root Reactor、release unit、Public API、runtime dependency、migration／table、workflow、validation Evidenceをinventoryする。
- CP1〜CP9の証拠とDoD 1b-1〜1b-8を対応付け、欠落だけを抽出する。
- ADR追加／改訂、KOIKI Skills更新、Developer向けREADME更新、CI接続の要否を判断する。

### Gate 10-2 — closeout設計／Owner確認

- CP10 aggregate scriptを新規作成するか、既存scriptの安全な合成で足りるかを比較する。
- Developer Journeyの起動command、観測点、credential境界、cleanup、所要時間を確定する。
- Milestone C CIでCP8実processとCP9短縮Smokeのどこまでをrequiredにするかを決める。
- remote artifact実証とpush／PRの実施タイミングを決める。
- 新規production機能なし、性能数値非required、正式baseline非上書きを確認する。

Owner確認前にworkflow、snapshot、Public API、production code、migrationまたはSkill正本を変更しない。

### Gate 10-3 — local実装／最終検証

- 承認された最小のcloseout tooling／文書だけを実装する。
- Developer Journey、DoD trace、positive／negative／restore、Repository hygieneを実行する。
- CP10 closeout validationへcommand、件数、時間、identity、失敗／手戻り、deferred scopeを記録する。
- 差分review後に`CP10 LOCAL COMPLETE / GATE 2 LOCAL READY`をOwnerへ提示する。

### Gate 10-4 — remote／Phase closeout

- local closeout commit後に同branchをpushする。
- Milestone C PRを作成し、required checks、ruleset、remote artifact経路を確認する。
- Owner Review後にmergeし、main最終CI成功を確認する。
- main最終CI成功前にMilestone CまたはPhase 1bを`COMPLETE`と記載しない。

## 9. CP10の変更候補

次は候補であり、Gate 10-1／10-2の結果なしに全て作成しない。

- `docs/architecture/validation/phase1b-closeout.md`等の最終validation
- Phase 1b実行計画のDoD／Gate 2／Milestone C状態更新
- Consumer READMEのDeveloper Journey補足
- 既存検証scriptを合成する最小CP10 aggregate script
- `.github/workflows/ci.yml`のMilestone C構造検証接続
- ADR／Skillsの「変更なし」判断、または実証で必要になった最小更新

Framework production source、Customer-like Consumerの新業務機能、Public API、production migration、正式Reference、
Project Templateまたは正式release artifactは通常のCP10変更候補ではない。

## 10. local検証の組み立て方

最初から全検証を反復せず、inventory、focused、aggregateの順に進める。

### 10.1 preflight

```powershell
git status --short --branch
git log -8 --oneline --decorate
java -version
.\mvnw.cmd -version
docker version
```

### 10.2 focused候補

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
pwsh -NoProfile -File build-support/feature-templates/verify-feature-templates.ps1
pwsh -NoProfile -File build-support/null-safety/verify-null-safety.ps1
pwsh -NoProfile -File build-support/api-compatibility/verify-public-api-fixtures.ps1
pwsh -NoProfile -File build-support/runtime-compatibility-fixture/verify-runtime-negative-guards.ps1
```

Public APIのremote baseline比較やsnapshot公開でcredentialが必要になる場合、既存のPR／workflow経路を優先し、PAT、token、
settings.xmlまたはsecret値を文書、command output、log、commitへ残さない。

### 10.3 runtime aggregate候補

```powershell
pwsh -NoProfile -File build-support/runtime-foundation-verification/verify-cp8-single-execution.ps1
pwsh -NoProfile -File build-support/performance-baseline/verify-performance-baseline.ps1 -Smoke -SkipRegression
```

CP8 scriptはCP1〜CP8回帰を含む。CP9 Smokeはharness contractだけを短縮再確認する。CP9公式baselineは既に採取済みのため、
CP10変更がharnessまたは比較対象を変えない限り、引数なしの公式計測を再実行しない。

最終command setはGate 10-1／10-2で重複と所要時間を整理してから確定する。

## 11. remote／push境界

- 新規セッション開始、preflight、inventory、closeout設計、local focused検証にpushは不要である。
- 現在のbranchはupstreamよりaheadであるため、remoteをlocalの正本とみなしてresetしない。
- CP10 local closeout commit後に、Milestone C PR用としてまとめてpushするのが推奨である。
- remote artifact取得、required checks、PR review、merge、main最終CIにはpushが必要である。
- push、PR作成、merge、ruleset変更、snapshot公開、secret登録は、それぞれ実施段階でユーザー確認を得る。

## 12. Stop conditions

- CP10 closeoutを理由に新機能、Starter、Public API、production migrationまたは業務moduleを追加する。
- Consumer、performance fixtureまたはvalidation toolingをFramework release unitへ昇格する。
- Root Reactor classpath、通常local Maven cacheまたはFramework source pathに依存してConsumerを成功させる。
- CP9性能数値をrequired閾値、性能保証値またはPC間比較へ転用する。
- CP9公式baselineを理由なく再採取／上書きする。
- Security、正式Reference、Level 2、非同期event、MyBatis実装、Oracle、cloud固有、正式releaseを混入する。
- credentialをsource、文書、log、result JSON、Maven settings templateまたはGit差分へ残す。
- CI不安定、cleanup漏れ、remote artifact不成立またはDoD Evidence欠落を文書だけで完了扱いする。
- PR required checksまたはmain最終CI前にMilestone C／Phase 1bを`COMPLETE`とする。
- 実装証拠がGrand Design、ADR、Phase 1b実行計画またはCP1〜CP9 Evidenceを否定する。

該当時は修正を拡大せず、証拠、影響範囲、Ownership、代替案、後続Phaseとの境界をOwnerへ提示する。

## 13. 新規セッションの開始手順

Repository rootで次を実行する。

```powershell
git branch --show-current
git status --short --branch
git log -8 --oneline --decorate
git rev-parse HEAD
git merge-base HEAD main
java -version
.\mvnw.cmd -version
docker version
```

期待状態:

- branchは`feature/phase1b-operations-closeout`
- CP10用の別branchを作成しない
- handoff commit後の履歴に`f14f587`と本handoff commitが存在する
- merge baseは`b3973e66134898765b95796c3622aaa68759b4fd`
- handoff commit直後のworktreeはclean
- upstreamよりaheadでも異常ではない。自動pull／reset／pushをしない
- Java 21、Maven Wrapper 3.9.16、Docker Linux daemonを利用可能

開始時にremote更新を確認する必要がある場合も、最初は`git fetch origin`までとし、local ahead commitとremoteの関係を
確認してから統合方針を決める。handoffを読むだけのためにCP8 aggregateまたはCP9公式計測を再実行しない。

## 14. 新規セッションで最初に行うこと

1. Repository identityとenvironmentを確認する。
2. 本handoff、Phase 1b実行計画、CP8／CP9 validation、実効POM／workflow／検証scriptを読む。
3. CP10 Gate 10-1としてrelease unit、Public API、migration／table、DoD、ADR、Skills、CI Evidenceをinventoryする。
4. 既存scriptの合成でDeveloper Journeyを再現できる範囲と、欠けている外部観測点だけを抽出する。
5. CP10 aggregate script、closeout validation、README、CI変更の要否を比較する。
6. Gate 10-2として変更対象、検証command、remote／credential境界、完了状態をOwnerへ提示する。
7. Owner確認後に最小差分を実装し、focusedからaggregateへ検証を進める。

## 15. 新規セッションへの開始依頼文

次の文章を、新しいセッションの最初の依頼として使用できる。

> KOIKI-JavaWeb-FWのPhase 1b CP10 Developer Journey／DoD／Gate 2 closeoutを開始します。
> Repository rootの`AGENTS.md`、`docs/agent/skills/koiki-project-overview/SKILL.md`、
> `docs/development/phase1b-cp10-start-handoff-20260830.md`、Phase 1b実行計画、CP8／CP9 validationを
> 確認してください。作業branchは`feature/phase1b-operations-closeout`を継続し、CP10用の別branchを作成しません。
> baseline HEADはCP9 result commit `f14f587`、main merge baseは`b3973e6`です。最初にGit／Java／Maven／Docker状態を
> 確認し、Gate 10-1としてrelease unit、Public API、migration／table、DoD 1b-1〜1b-8、ADR、Skills、CI Evidence、
> Developer Journeyの欠落をinventoryしてください。Gate 10-2のOwner確認前にproduction code、Public API、migration、
> workflow、snapshotまたはSkill正本を変更せず、pushもしないでください。CP9公式baselineは再採取しません。

## 16. 引継ぎ時点の判定

- CP0、Gate 1、CP1〜CP9は完了し、Milestone A／Bは`COMPLETE / ACCEPTED`、Milestone Cは`IN PROGRESS`である。
- CP8単一実行contract、CP9 performance harness、CP9公式baseline resultはlocal commitへ確定済みである。
- DoD 1b-1〜1b-8の個別Evidenceは揃っているが、CP10の横断Developer Journey、全Phase共通DoD、remote CI、Gate 2は未完了である。
- 現在branchを継続し、CP10用の別branchは作成しない。
- pushはCP10 local closeoutまで必須ではなく、PR、required checks、remote artifact、merge、main CIの段階で必要になる。
- このhandoffはCP10の実装許可、Gate 2承認またはPhase 1b完了判定ではない。

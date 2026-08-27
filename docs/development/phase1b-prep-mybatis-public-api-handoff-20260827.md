# Phase 1b着手前 — PersistenceTechnology.MYBATIS Public API不整合 引継ぎ — 2026-08-27

## 1. この文書の位置づけ

Phase 1a（Milestone A〜C、C5 Closeoutまで）は完了済みである（正本:
[docs/architecture/validation/phase1a-closeout.md](../architecture/validation/phase1a-closeout.md)）。
Phase 1b着手前の棚卸点検により、`PersistenceTechnology.MYBATIS`が公開APIとして固定された状態と
グランドデザインの設計制約が矛盾している問題を発見した。本文書はこの問題を、これまで検討を主導した
Codexセッションへ引き継ぐための、問題内容・検証済み事実・対応方針の記録である。

**本文書はコード変更を含まない。** 調査と記録のみを行った。実装（前半PRの作成、後半の判断・実装、
baseline republish等）は本文書を踏まえてCodexセッションが担当する。

**改訂の経緯:** 初版では対応を単一の「MYBATIS定数を削除する（案A）」として整理したが、Owner検討により
実害の所在が絞り込まれた。実害は「`MYBATIS`定数が存在すること」自体ではなく「ArchUnitルールが
`MYBATIS`+`SHARED`という、グランドデザインが禁じる組合せを承認済みとして通していること」にある。
このルール修正はPublic APIを一切変更しないため、japicmp baselineに触れずに通常の1 PRで即座に対応
できる。そのため対応を**前半（Phase 1b前に今すぐ実施）**と**後半（MYBATIS定数の削除可否、判断を保留）**
に分割した。本改訂はその分割を反映する。

判断が競合する場合は、次の順に正本を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`（§11.6、§11.7、DoD項番30・31）
4. `docs/architecture/adr/README.md`（ADR-039、ADR-041のRegister）
5. `docs/architecture/validation/phase1a-architecture-contract.md`、
   `docs/architecture/validation/phase1a-public-api-compatibility.md`（Public API・japicmp Gate Evidence）
6. 本文書

Public APIとArchUnit ruleの変更を扱うため、開始時の適用Skillは`koiki-project-overview`である。

## 2. 引継ぎ時点のGit状態

| 項目 | 状態 |
|---|---|
| `main`最新commit | `8a19cf1`（PR #21 merge、Phase 1a C5 Closeout完了時点） |
| 本文書の作業branch | `feature/phase1b-prep-mybatis-handoff`（`main`から分岐、本文書のみを含む） |
| worktree | 本文書作成時点でclean、他の未commit変更なし |
| コード変更 | なし（本文書のみ） |

## 3. 問題の要約

`org.koikifw.architecture.PersistenceTechnology`は`JPA`と`MYBATIS`の2定数を公開APIとして持つ。一方
`org.koikifw.architecture.PersistenceModel`は`SHARED`のみを持ち、`SEPARATED`は未定義である（Phase 1a
実行計画G3で意図的に見送り）。

グランドデザインは次を恒久的な設計制約として定めている（時限的な「未検証だから」という制約ではない）。

- §11.7-4:「永続化技術としてMyBatisを採用する場合」は分離方式（`persistenceModel = SEPARATED`）へ強制
  オプトインする。理由は「dirty checkingが存在しないため、兼用方式が成立しない」。JPAの兼用方式が
  成立する4つの根拠（dirty checking、`@Version`楽観ロック、遅延ロード、DB採番の透過利用）はいずれも
  JPA固有の機能であり、MyBatisにはすべて存在しない
  （[グランドデザイン_v0.2.md:972-1008](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#L972-L1008)）。
- §11.6/1105:「`persistence = MYBATIS` を宣言した場合、`persistenceModel = SEPARATED` が必須となる
  （ArchUnitで検査）」
  （[グランドデザイン_v0.2.md:1102-1106](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#L1102-L1106)）。
- DoD項番30:「`persistence = MYBATIS` かつ `persistenceModel = SHARED` の組み合わせが存在しない」
  （[グランドデザイン_v0.2.md:2269](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#L2269)）。
- DoD項番31:「`persistence = MYBATIS` のモジュールに`@Entity`を付与したクラスが存在しない」
  （[グランドデザイン_v0.2.md:2270](../architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md#L2270)）。

つまり`MYBATIS`が意味を持つ唯一の正しい組合せは`SEPARATED`であり、`SEPARATED`はまだ存在しない。
それにもかかわらず、現在のArchUnit実装は逆方向に実装されている。

## 4. 検証済みの事実（このRepositoryのファイルで直接確認済み）

1. **`BusinessModuleRuleSet`のrule8（KOIKI-ARCH-008）は`MYBATIS`+`SHARED`を「承認済み」として明示的に
   通す。** グランドデザインが唯一禁じる組合せを、ArchUnitルールが積極的に許可している。

   ```java
   // koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java:927-935
   } else if (!Set.of(PersistenceTechnology.JPA, PersistenceTechnology.MYBATIS)
                   .contains(metadata.persistence())
           || metadata.persistenceModel() != PersistenceModel.SHARED) {
       addViolation(..., "has unsupported persistence metadata");
   }
   ```

   読み下すと「`persistence`が`JPA`か`MYBATIS`、かつ`persistenceModel`が`SHARED`であること」を要求して
   おり、`MYBATIS + SHARED`は違反にならない。DoD項番30相当のルール（`persistence = MYBATIS`かつ
   `persistenceModel = SHARED`を拒否する）は未実装（`BusinessModuleRuleSet.rules()`が呼ぶルールは
   rule1〜4, 6〜9, 11, 12, 14〜22, 24, 28, 38, 39であり、30・31は欠番）。

2. **テストがこの状態を「正常系」として固定している。**

   - [BusinessModuleRuleSetTest.java:130-137](../../koiki-archunit-rules/src/test/java/org/koikifw/archunit/BusinessModuleRuleSetTest.java#L130-L137)
     の`rule8AcceptsApprovedPersistenceDeclarations`は、`persistence=MYBATIS, persistenceModel=SHARED`を
     宣言する`fixture/metadata/rich`パッケージを「違反なし」と明示的にアサートしている。
   - [fixture/metadata/rich/package-info.java:5-6](../../koiki-archunit-rules/src/test/java/org/koikifw/archunit/fixture/metadata/rich/package-info.java#L5-L6)
     がその宣言そのもの（`persistence = PersistenceTechnology.MYBATIS, persistenceModel = PersistenceModel.SHARED`）。
   - [ModuleMetadataTest.java:37-38](../../koiki-archunit-rules/src/test/java/org/koikifw/archunit/ModuleMetadataTest.java#L37-L38)
     も同じfixtureで`MYBATIS`+`SHARED`の読み取りを検証している。
   - [RequiredNegativeArchitectureRulesTest.java:114](../../koiki-archunit-rules/src/test/java/org/koikifw/archunit/RequiredNegativeArchitectureRulesTest.java#L114)
     がrule8を`"008"`として登録している。

3. **`PersistenceTechnology.MYBATIS`はすでにPublic APIとしてfreeze済み。** Phase 1a C3
   （[phase1a-public-api-compatibility.md](../architecture/validation/phase1a-public-api-compatibility.md)）
   がjapicmpによる互換性検査を`Public API Compatibility` required checkとしてmain branch rulesetへ
   追加済み（Gate 4、9.7節）。以後`MYBATIS`定数の削除・変更は、ADR-041の破壊的変更手続き（Owner Review）
   対象になる。

   - [build-support/api-compatibility/public-api.txt:18](../../build-support/api-compatibility/public-api.txt#L18)
     に`ENUM org.koikifw.architecture.PersistenceTechnology MYBATIS`として記録済み。
   - [build-support/api-compatibility/verify-public-api-compatibility.ps1:25-26](../../build-support/api-compatibility/verify-public-api-compatibility.ps1#L25-L26)
     が`$expectedTimestamp = '0.1.0-20260826.091429-1'`と2つのSHA-256をハードコードし、GitHub Packages
     上のC1 baseline JAR（`0.1.0-20260826.091429-1`）と現在のJARをjapicmpで直接比較する構造。
     baselineはmainのコードから生成されるため、「`MYBATIS`を削除したmain」が存在しない限り新baselineは
     作れない（＝1 PRでは閉じないチキンエッグ構造）。
   - [.github/workflows/publish-snapshot.yml](../../.github/workflows/publish-snapshot.yml)は
     `workflow_dispatch` + `expected_commit`一致検証 + `environment: phase1a-internal-snapshot`ゲート
     付きの人手承認前提のpublishフロー。実行するとGitHub Packagesへ実際に成果物を公開する
     （元に戻しにくい・他者から見える操作）。

4. **`BusinessModuleRuleSet`はPublic APIに含まれない。** `koiki-archunit-rules`の公開面は
   `KoikiArchitectureRules`の`businessModuleRules(String)`と`frameworkOwnershipRules(String, String...)`
   の2 staticメソッドのみであり（`public-api.txt:20-22`）、`BusinessModuleRuleSet`・`ModuleMetadata`等の
   実装クラスはpackage-privateである。テストfixture（`src/test`配下）も配布JARには含まれない。
   **したがって、rule8のロジック修正・fixture修正・テスト修正は、japicmp baselineにもPublic API
   inventoryにも一切触れない。** これが§6の前半／後半分割の技術的根拠である。

5. **`ADR-039`は「MyBatis」を「確定（Boot 4対応StarterをBOM管理するLevel B方針の承認）」として登録
   しているが、「詳細規約と実装検証はPhase 3末尾〜Phase 4」と明記している**
   （[docs/architecture/adr/README.md:68](../architecture/adr/README.md#L68)）。すなわち`MYBATIS`
   自体の宣言可能性を今のPhaseで承認したわけではない。

6. **Agent Skillの業務実装導線が、既に`MYBATIS`を選択肢として提示している。**
   `docs/agent/skills/koiki-business-feature-work/SKILL.md`は
   [68行](../agent/skills/koiki-business-feature-work/SKILL.md#L68)「SQL指向の更新または既存SQL資産の
   移行では、モジュール単位でMyBatisを選ぶ。」、
   [78行](../agent/skills/koiki-business-feature-work/SKILL.md#L78)（分離方式を検討する4条件の1つとして）
   「MyBatisを採用する。」と明示している。
   [80行](../agent/skills/koiki-business-feature-work/SKILL.md#L80)に「分離方式やMyBatisの詳細規約は
   後続Phaseの証拠を確認し、未検証の構造を推測で固定しない。」という散文の注意書きはあるが、機械検査
   （ArchUnit）が`MYBATIS + SHARED`を承認する状態のままでは、この注意書きより「ビルドが通る」という
   事実の方が実質的な力を持つ。SQL指向モジュールの実装時にAIエージェントがこの導線を辿ると、
   `SEPARATED`が存在しないため、コンパイル・ArchUnitの両方を通る唯一の道である`MYBATIS + SHARED`へ
   自然に落ち着いてしまう。

7. **Phase 1a実行計画G3は`PersistenceModel.SEPARATED`を「未検証方式を先行固定しない」という基準で
   意図的に見送った**
   （[Phase1a実行計画_v0.1.md:347-350](KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md#L347-L350)）。
   同じ基準を`PersistenceTechnology.MYBATIS`（実装検証はPhase 3末尾〜Phase 4、ADR-039）に当てはめると、
   `MYBATIS`も同時に見送るべきだったと考えられる。`SEPARATED`だけを落として`MYBATIS`を残す非対称の
   理由は、確認した文書のどこにも記載がない。

8. **`docs/agent/skills/koiki-project-overview/SKILL.md`はそもそも「MyBatisの詳細実装規約」を
   Phase 1a確定範囲外の未確定事項として明記している**（同ファイル87行）。

9. **Phase 1a Closeoutは「Public APIの変更」を明示的なRevisit triggerとして記録している。**
   （[phase1a-closeout.md:397](../architecture/validation/phase1a-closeout.md#L397)）
   「Revisit trigger | Phase 1b開始、baseline / Public API / required checks変更、正式releaseまたは
   support開始」。後半（`MYBATIS`定数の扱い）の判断はこのtriggerに紐づけて再判定できる。

## 5. 何が問題か（まとめ）

- 今この瞬間、`persistence = MYBATIS, persistenceModel = SHARED`を宣言してもArchUnit検査は**通る**
  （「宣言すると即座にArchUnitが弾く」という意味ではない）。
- しかし`MYBATIS`が意味を持つ唯一の正しい組合せ（`SEPARATED`）はまだ存在せず、MyBatis実装・Starter・
  Feature Templateも未提供。
- グランドデザインの設計が完成した瞬間（`SEPARATED`定義 + DoD項番30/31実装）、現在ArchUnitが
  「承認済み」と教えている`MYBATIS + SHARED`宣言は、自動的に違反へ転落することが設計文書上確定して
  いる。
- Agent Skillの業務実装導線（§4-6）が既に`MYBATIS`を選択肢として提示しており、Phase 1bでSQL指向
  モジュールを実装する場面が来ると、この経路が実際に踏まれるリスクがある。
- `MYBATIS`定数自体は既にjapicmpでfreeze済み・CI必須check化されているため、削除・訂正するのは
  ADR-041の破壊的変更手続き（Owner Review、baseline republish）を要する。**ただし、この重い手続きが
  必要なのは「定数を削除する場合」だけであり、「ArchUnitルールを設計どおりに直す」だけなら不要**
  （§4-4参照）。

## 6. 対応方針 — 前半と後半に分割する

実害の所在を切り分けると、対応は次の2段階に分かれる。

| | 内容 | 時期 | Public API / japicmp影響 |
|---|---|---|---|
| **前半（今すぐ実施）** | rule8をグランドデザインDoD項番30どおりに修正し、`MYBATIS + SHARED`を明示的に拒否する。fixture・テストを追随修正し、ADRへ理由を補記する | **Phase 1b着手前に1 PR** | **なし**（`BusinessModuleRuleSet`はpackage-private、fixtureはJARに含まれない） |
| **後半（判断保留）** | `PersistenceTechnology.MYBATIS`定数そのものを削除するか、残したまま`SEPARATED`追加を待つか | Phase 1bのbaseline方針が見えてから再判定（Revisit trigger該当） | **あり**（削除する場合は前回整理した3 PR + `publish-snapshot`手動実行が必要） |

前半を実施すると、`MYBATIS`は「宣言できるが、宣言するとビルドが落ちる」状態になる。これは
**グランドデザインDoD項番30が指定している状態そのもの**であり、KOIKI自身の「規約は機械検査する」
という原則にも合致する。文書とコードのズレはこの時点で解消する。

### 6.1 前半の対象ファイル（3箇所、Public API/japicmpに触れない）

| # | ファイル | 変更内容 |
|---|---|---|
| 1 | `koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java:927` | `persistence == MYBATIS`かつ`persistenceModel != SEPARATED`（現状は`SHARED`しか存在しないため、実質「`persistence == MYBATIS`は無条件で拒否」）を違反として明示するロジックへ変更。DoD項番30相当。メッセージにADR-039参照と「`SEPARATED`未提供のため宣言不可」の理由を含める |
| 2 | `koiki-archunit-rules/src/test/java/org/koikifw/archunit/fixture/metadata/rich/package-info.java:5` | `persistence = PersistenceTechnology.MYBATIS` → `PersistenceTechnology.JPA`（「承認済み」fixtureからグランドデザインが禁じる組合せを除去する） |
| 3 | `koiki-archunit-rules/src/test/java/org/koikifw/archunit/ModuleMetadataTest.java:37` | 期待値を`PersistenceTechnology.MYBATIS`→`PersistenceTechnology.JPA`に修正（fixtureの変更に追随） |

**触らないもの**: `koiki-architecture-contract/src/test/java/org/koikifw/architecture/KoikiModuleContractTest.java`
（enum定数一覧の期待値はPublic APIそのものなので変更不要）、
`build-support/api-compatibility/public-api.txt`（同様に変更不要）。

`BusinessModuleRuleSetTest.rule8AcceptsApprovedPersistenceDeclarations`は「`JPA + SHARED`のみ承認」に
文言・意図を更新し、`MYBATIS`拒否を確認する新規テストケース（`rule8RejectsMybatisWithoutSeparatedModel`
相当）を追加することが望ましい。`RequiredNegativeArchitectureRulesTest`のrule "008" registrationは
変更不要（rule8自体は残る）。

あわせて、ADR Register（`docs/architecture/adr/README.md`）のReview Logまたはグランドデザイン該当箇所
（§16.2「MyBatisの提供範囲」相当）に、「`MYBATIS`は`SEPARATED`と対で扱い、単独では宣言不可（ArchUnit
KOIKI-ARCH-008で検査）」という理由を1行残す。**この補記は前半のPRに必ず含める。** コードのルールだけ
残すと、Phase 1b途中で「なぜ`MYBATIS`を宣言するとビルドが落ちるのか」の経緯が分からなくなる。

見積り: 半日以内。

### 6.2 後半の選択肢（判断保留、Phase 1bのbaseline方針決定時に再判定）

前半を先に入れると、後半の性格が変わる。`MYBATIS`定数を残したままrule30相当が働く状態は、
**維持しても悪くない選択肢**になる。

| 選択肢 | 内容 | 評価 |
|---|---|---|
| 維持 | `MYBATIS`定数を残す。Phase 4で`SEPARATED`の1定数だけを追加する | 前半さえ入っていれば実害はない。Phase 4の変更量が「定数1つの追加」で済み、承認済みAPI変更の手続きを通す回数が1回減る |
| 削除 | `MYBATIS`定数を削除する。Phase 4で`MYBATIS`と`SEPARATED`を2定数同時に追加する | 「未検証方式を先行固定しない」というG3基準に最も忠実。ただし単独実施する場合は3 PR + `publish-snapshot`手動実行（前回整理済み、§7参照）が必要 |

判断を左右する要因は、**Phase 1bがrelease unit構成（4成果物: `koiki-dependencies-bom`,
`koiki-parent`, `koiki-architecture-contract`, `koiki-archunit-rules`）を変えるかどうか**である。

- Phase 1bがRuntime Foundationの成果物（framework、starter類）をrelease unitに加えるなら、baselineは
  どのみち切り直しになる。そのタイミングなら`MYBATIS`削除は数行の相乗りで済み、単独実施の3 PR +
  `workflow_dispatch`が不要になる。**削除するなら、このタイミングが最も安い。**
- Phase 1bが現行4成果物の構成を変えないなら相乗り先がないため、削除する場合はPhase 1b末のcloseoutで
  単独実施、という形になる。
- どちらのケースでも、「今すぐ単独で削除する」より安いか同等である。

**結論: 後半（削除するか維持するか）は、Phase 1bのbaseline方針が見えるまで判断を保留してよい。**
前半さえ入っていれば、保留による実害はない。

## 7. 後半で「削除」を選んだ場合の3 PR構成（参考、前回整理を維持）

`verify-public-api-compatibility.ps1`は「`public-api.txt`との一致」と「GitHub Packages上のC1
baseline JARとのjapicmp比較」という2つの独立判定を持つ。前者は同一PR内で変更できるが、後者は
baselineがmainのマージ済みコードから生成されるため、1 PRでは閉じない。次の3段階が必要と考えられる
（詳細な実装判断はCodexセッションに委ねる）。

1. **PR-A（Public APIは変更しない）**: baseline識別情報（`$expectedTimestamp`・`$baselineVersion`・
   2つのSHA-256）を外部化し、Architecture Owner承認済みの変更であることが分かる場合にjapicmp比較を
   スキップし、代わりに変更前後のinventory差分が承認記録と一致するかを検証する仕組みを追加する。
   この段階ではAPIを変えないため、既存のrequired check 3本は通常どおり通過するはずである。
2. **PR-B（API変更本体）**: `PersistenceTechnology.java`から`MYBATIS`定数を削除し、
   `KoikiModuleContractTest.java:55`の期待配列を`{JPA}`のみへ、`public-api.txt:18`の
   `ENUM ... MYBATIS`行を削除する。PR-Aで追加した承認済み変更の記録を埋める。japicmp比較はスキップ
   され、inventory差分が承認記録と一致することで通過する想定。
3. **PR-B merge後、`publish-snapshot.yml`をOwnerが`workflow_dispatch`で手動実行**し、新しい
   timestamped snapshotとSHA-256を採取する（`environment: phase1a-internal-snapshot`のOwner承認が
   必要）。
4. **PR-C（通常モードへの復帰）**: baseline識別情報を新しいtimestamp・SHA-256へ更新し、承認済み変更の
   記録を解除して、japicmp比較を通常どおり有効化する。

**注意:** 上記の`publish-snapshot`手動実行はGitHub Packagesへの実公開を伴う、元に戻しにくく他者から
見える操作である。実行前にArchitecture Owner（ユーザー本人）の明示的な承認を得ること。

## 8. 前半すら入れずに繰り延べた場合の具体的リスク

前半（1 PR、半日以内、Public API非破壊）を入れずに進めると、Phase 1b中に次が起きうる。

1. **Agent Skillsが誤った導線を持つ。** `koiki-business-feature-work`のSkill本文（68行・78行）は
   SQL指向の更新系またはMyBatis採用時にMyBatisを選ぶ導線を明示している。Phase 1b中にAIエージェントが
   SQL指向モジュールを実装しようとすると、この導線を正しく辿った結果`SEPARATED`が存在しないことに
   突き当たり、コンパイル・ArchUnitの両方が通る唯一の道である`MYBATIS + SHARED`に落ち着く。80行の
   「未検証の構造を推測で固定しない」という散文の注意書きより、「機械検査がOKと言っている」という
   事実の方が実質的に勝る。前半を入れれば、ここは機械が止める。
2. **ルール記述パターンの伝播。** Phase 1bはRuntime Foundationであり、Flyway所有権（ADR-042）や
   runtime関連のルール追加が想定される。新ルールを書く際、927行付近の宣言検証が「宣言検証の書き方の
   見本」として参照される可能性があり、誤った条件式が複製されるリスクがある。
3. **Feature TemplateとReferenceの汚染。** 現在のテンプレートは`JPA`/`SHARED`のみで安全だが、
   Phase 1bでテンプレートやリファレンス実装が増える過程で、上記1・2の経路を通じて`MYBATIS`が
   紛れ込む余地が残る。

これら3つのリスクは、前半の1 PRで塞がる。

## 9. Phase 1b中の前提条件（後半を保留する場合）

- **ADR補記を前半のPRに必ず含める**（「`MYBATIS`は`SEPARATED`と対で扱い、単独では宣言不可」）。
  コードのルールだけだと、なぜ落ちるのかがPhase 1bの途中で分からなくなる。
- **Phase 1bのbaseline方針を決める時点で、後半（削除するか維持するか）の可否を再判定する。**
  closeoutのRevisit trigger（[phase1a-closeout.md:397](../architecture/validation/phase1a-closeout.md#L397)）
  に「Public APIの変更」が挙がっているので、そこに紐づける。
- **Phase 1b中、`PersistenceModel`／`PersistenceTechnology`に新しい定数を足さない。** 今の
  1定数ずつ（`PersistenceModel.SHARED`、実質的に有効な`PersistenceTechnology.JPA`）の状態をPhase 4
  まで凍結する。

## 10. 未決定事項（Owner判断が必要）

- 前半（rule30相当の実装）自体は実施する方向で整理済み。実装の詳細（エラーメッセージ文言、ADR補記の
  置き場所）はCodexセッションでの実装時に確定する。
- 後半（`MYBATIS`定数の維持／削除）は、Phase 1bのrelease unit構成・baseline方針が見えるまで判断を
  保留する。Phase 1b開始時または途中でrelease unit構成の変更予定が判明した時点で再判定すること。

## 11. 次回セッションの開始手順

```powershell
git switch main
git pull --ff-only
git switch feature/phase1b-prep-mybatis-handoff
git status --short --branch
git log -3 --oneline --decorate
```

期待状態:

- `feature/phase1b-prep-mybatis-handoff`が`main`（`8a19cf1`）から分岐し、本文書を追加・改訂した
  commitを持つ
- worktreeがclean

## 12. 新規セッションへの開始依頼文（例）

> KOIKI-JavaWeb-FWはPhase 1a（C5 Closeoutまで）完了済みで、`main`は`8a19cf1`です。Phase 1b着手前の
> 棚卸点検で、`PersistenceTechnology.MYBATIS`が公開APIとして固定されているが、グランドデザインの
> 設計制約（`persistence=MYBATIS`は`persistenceModel=SEPARATED`必須、`SHARED`との組合せは禁止）と
> 矛盾しており、しかも現在のArchUnitルールはその禁止された組合せを「承認済み」として通してしまう
> 問題を確認しました。対応は前半（rule8をDoD項番30どおりに修正する1 PR、Public API非破壊、今すぐ
> 実施）と後半（`MYBATIS`定数の削除可否、Phase 1bのbaseline方針が見えてから判断）に分割済みです。
> 詳細は`docs/development/phase1b-prep-mybatis-public-api-handoff-20260827.md`
> （branch `feature/phase1b-prep-mybatis-handoff`）にまとめてあります。まず前半のPRから着手して
> ください。作業前に`AGENTS.md`と`docs/agent/skills/koiki-project-overview/SKILL.md`を確認して
> ください。

## 13. 引継ぎ時点の検証

- 本文書が引用するコード箇所・行番号（`BusinessModuleRuleSet.java:927`、fixture、テスト、
  Agent Skill該当行、Revisit trigger該当行を含む）は、本セッション内でRead/Grepツールにより実物を
  確認済み。
- japicmpのbaseline構造・`publish-snapshot.yml`のゲート構成、および`koiki-archunit-rules`の
  Public API面が`KoikiArchitectureRules`の2メソッドのみであることも実ファイルを確認済み。
- 本文書作成時点で、Repository構成・workflow・Public API・ArchUnit ruleに対する実装変更は行っていない
  （本文書の追加・改訂のみ）。
- Maven Central／外部ネットワークへの到達性は本セッションでは検証していない（前半PRの実装後、ローカルで
  `./mvnw verify`等の実行可否を確認することを推奨）。

# Phase 1b着手前 — PersistenceTechnology.MYBATIS Public API不整合 引継ぎ — 2026-08-27

## 1. この文書の位置づけ

Phase 1a（Milestone A〜C、C5 Closeoutまで）は完了済みである（正本:
[docs/architecture/validation/phase1a-closeout.md](../architecture/validation/phase1a-closeout.md)）。
Phase 1b着手前の棚卸点検により、`PersistenceTechnology.MYBATIS`が公開APIとして固定された状態と
グランドデザインの設計制約が矛盾している問題を発見した。本文書はこの問題を、これまで検討を主導した
Codexセッションへ引き継ぐための、問題内容・検証済み事実・対応案の記録である。

**本文書はコード変更を含まない。** 調査と記録のみを行った。実装（案の選定、コード修正、PR作成、
baseline republish等）は本文書を踏まえてCodexセッションが担当する。

判断が競合する場合は、次の順に正本を確認する。

1. Repository rootの`AGENTS.md`
2. `docs/agent/skills/koiki-project-overview/SKILL.md`
3. `docs/architecture/grand-design/KOIKI-JavaWeb-FW_グランドデザイン_v0.2.md`（§11.6、§11.7、DoD項番30）
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

4. **`ADR-039`は「MyBatis」を「確定（Boot 4対応StarterをBOM管理するLevel B方針の承認）」として登録
   しているが、「詳細規約と実装検証はPhase 3末尾〜Phase 4」と明記している**
   （[docs/architecture/adr/README.md:68](../architecture/adr/README.md#L68)）。すなわち`MYBATIS`
   自体の宣言可能性を今のPhaseで承認したわけではない。

5. **Phase 1a実行計画G3は`PersistenceModel.SEPARATED`を「未検証方式を先行固定しない」という基準で
   意図的に見送った**
   （[Phase1a実行計画_v0.1.md:347-350](KOIKI-JavaWeb-FW_Phase1a実行計画_v0.1.md#L347-L350)）。
   同じ基準を`PersistenceTechnology.MYBATIS`（実装検証はPhase 3末尾〜Phase 4、ADR-039）に当てはめると、
   `MYBATIS`も同時に見送るべきだったと考えられる。`SEPARATED`だけを落として`MYBATIS`を残す非対称の
   理由は、確認した文書のどこにも記載がない。

6. **`docs/agent/skills/koiki-project-overview/SKILL.md`はそもそも「MyBatisの詳細実装規約」を
   Phase 1a確定範囲外の未確定事項として明記している**（同ファイル87行）。

## 5. 何が問題か（まとめ）

- 今この瞬間、`persistence = MYBATIS, persistenceModel = SHARED`を宣言してもArchUnit検査は**通る**
  （「宣言すると即座にArchUnitが弾く」という意味ではない）。
- しかし`MYBATIS`が意味を持つ唯一の正しい組合せ（`SEPARATED`）はまだ存在せず、MyBatis実装・Starter・
  Feature Templateも未提供。
- グランドデザインの設計が完成した瞬間（`SEPARATED`定義 + DoD項番30/31実装）、現在ArchUnitが
  「承認済み」と教えている`MYBATIS + SHARED`宣言は、自動的に違反へ転落することが設計文書上確定して
  いる。
- `MYBATIS`定数自体は既にjapicmpでfreeze済み・CI必須check化されているため、後から削除・訂正するのは
  軽い変更ではなく、ADR-041の破壊的変更手続き（Owner Review）を要する。

結果として「宣言できるが、宣言すればいずれ必ず設計違反になることが確定しており、しかも機械検査は
それを承認済みとして素通りさせる」という状態が、公開APIとして固定されている。Feature Template・
Skill・AIコーディングエージェントがこの定数を見て「使える」と誤認するリスクもある。

## 6. 対応案

| 案 | 内容 | 評価 |
|---|---|---|
| A（推奨） | `PersistenceTechnology`を`JPA`のみにし、Phase 4で`MYBATIS`と`SEPARATED`を同時に追加する。ArchUnit側の`Set.of(JPA, MYBATIS)`も`Set.of(JPA)`へ修正 | G3の判断基準（未検証方式を先行固定しない）に最も忠実。有効な組合せが常に閉じた状態を保てる |
| B | `PersistenceModel.SEPARATED`を今追加し、DoD項番30/31相当のArchUnitルールを実装する | G3の判断（`SEPARATED`は未検証のため見送り）を覆すことになり、実証がPhase 4なら早すぎる可能性 |
| C | 現状維持 + ADRで「`MYBATIS`宣言はPhase 4まで禁止」と明文化し、DoD項番30相当のルールだけ先に実装する | 最小変更だが、公開APIに「宣言できるが宣言してはいけない定数」が残り続ける |

案Aの根拠: `0.1.0-SNAPSHOT`・外部consumerゼロ・内部snapshot 1本のみという現時点であれば、影響範囲は
小さい。ただし前述の通りjapicmpのchicken-and-egg構造（baselineはmainのコードから生成されるため
1 PRでは閉じない）があり、少なくとも「変更を許可する仕組みを作るPR」と「実際にAPIを変更するPR」の
分離、および`publish-snapshot`のOwner承認済み手動再実行が必要になる。

## 7. 案Aを選ぶ場合の影響範囲（実測、6箇所）

| # | ファイル | 変更内容 |
|---|---|---|
| 1 | `koiki-architecture-contract/src/main/java/org/koikifw/architecture/PersistenceTechnology.java` | `MYBATIS`定数を削除 |
| 2 | `koiki-architecture-contract/src/test/java/org/koikifw/architecture/KoikiModuleContractTest.java:55` | 期待配列を`{JPA}`のみに変更 |
| 3 | `koiki-archunit-rules/src/main/java/org/koikifw/archunit/BusinessModuleRuleSet.java:927` | `Set.of(JPA, MYBATIS).contains(...)` を `metadata.persistence() != PersistenceTechnology.JPA` 相当へ |
| 4 | `koiki-archunit-rules/src/test/java/org/koikifw/archunit/fixture/metadata/rich/package-info.java:5` | `MYBATIS` → `JPA` |
| 5 | `koiki-archunit-rules/src/test/java/org/koikifw/archunit/ModuleMetadataTest.java:37` | 期待値を`JPA`に変更 |
| 6 | `build-support/api-compatibility/public-api.txt:18` | `ENUM ... MYBATIS`行を削除 |

Feature Template（`build-support/feature-templates/templates/tier1-simple/`、`tier2-rich/`配下の
`package-info.java.template`）は既に`JPA`/`SHARED`を使用しているため変更不要（確認済み）。

副作用として`PersistenceTechnology`が単一定数enumになり、`persistence`/`persistenceModel`属性は
当面すべてのモジュールで`JPA`/`SHARED`固定の儀式的な宣言になる。ただし属性（annotation element）
自体は削除しないこと。annotation elementの後付けはdefault必須になりG3の「defaultを設けない」という
設計意図と衝突するが、enum定数の後付けはバイナリ互換であるため、「器（属性）は残し、中身（定数）だけ
絞る」方が将来のPhase 4追加コストが低い。

## 8. japicmp Gateを通すための手順（案A採用時、3 PR構成の見込み）

`verify-public-api-compatibility.ps1`は「`public-api.txt`との一致」と「GitHub Packages上のC1
baseline JARとのjapicmp比較」という2つの独立判定を持つ。前者は同一PR内で変更できるが、後者は
baselineがmainのマージ済みコードから生成されるため、1 PRでは閉じない。次の3段階が必要と考えられる
（詳細な実装判断はCodexセッションに委ねる）。

1. **PR-A（Public APIは変更しない）**: baseline識別情報（`$expectedTimestamp`・`$baselineVersion`・
   2つのSHA-256）を外部化し、Architecture Owner承認済みの変更であることが分かる場合にjapicmp比較を
   スキップし、代わりに変更前後のinventory差分が承認記録と一致するかを検証する仕組みを追加する。
   この段階ではAPIを変えないため、既存のrequired check 3本は通常どおり通過するはずである。
2. **PR-B（API変更本体）**: 上記§7の6箇所を変更し、PR-Aで追加した承認済み変更の記録を埋める。
   japicmp比較はスキップされ、inventory差分が承認記録と一致することで通過する想定。
3. **PR-B merge後、`publish-snapshot.yml`をOwnerが`workflow_dispatch`で手動実行**し、新しい
   timestamped snapshotとSHA-256を採取する（`environment: phase1a-internal-snapshot`のOwner承認が
   必要）。
4. **PR-C（通常モードへの復帰）**: baseline識別情報を新しいtimestamp・SHA-256へ更新し、承認済み変更の
   記録を解除して、japicmp比較を通常どおり有効化する。

あわせて、グランドデザイン§16.2「MyBatisの提供範囲」相当の記述に「Phase 4まで`@KoikiModule`では
宣言できない」旨を残し、ADR Register（`ADR-039`）のReview Logに本件の決定を1行追記することが望ましい。

**注意:** 上記の`publish-snapshot`手動実行はGitHub Packagesへの実公開を伴う、元に戻しにくく他者から
見える操作である。実行前にArchitecture Owner（ユーザー本人）の明示的な承認を得ること。

## 9. 未決定事項（Owner判断が必要）

- 案A／B／Cのどれを採用するか（本文書時点では未確定。前セッションではAが有力候補として議論された）。
- PR-A/B/Cの3段階構成、または別の実装方式を採用するか。
- `docs/architecture/grand-design/`や`ADR Register`への追記要否とタイミング（Step 0として先行させるか、
  PR-Bと同時にするか）。

## 10. 次回セッションの開始手順

```powershell
git switch main
git pull --ff-only
git switch feature/phase1b-prep-mybatis-handoff
git status --short --branch
git log -3 --oneline --decorate
```

期待状態:

- `feature/phase1b-prep-mybatis-handoff`が`main`（`8a19cf1`）から分岐し、本文書のみを追加したcommitを
  1つ持つ
- worktreeがclean

## 11. 新規セッションへの開始依頼文（例）

> KOIKI-JavaWeb-FWはPhase 1a（C5 Closeoutまで）完了済みで、`main`は`8a19cf1`です。Phase 1b着手前の
> 棚卸点検で、`PersistenceTechnology.MYBATIS`が公開APIとして固定されているが、グランドデザインの
> 設計制約（`persistence=MYBATIS`は`persistenceModel=SEPARATED`必須、`SHARED`との組合せは禁止）と
> 矛盾しており、しかも現在のArchUnitルールはその禁止された組合せを「承認済み」として通してしまう
> 問題を確認しました。詳細と対応案は
> `docs/development/phase1b-prep-mybatis-public-api-handoff-20260827.md`
> （branch `feature/phase1b-prep-mybatis-handoff`）にまとめてあります。これを読んで、対応方針の
> 確定と実装を進めてください。作業前に`AGENTS.md`と`docs/agent/skills/koiki-project-overview/SKILL.md`
> を確認してください。

## 12. 引継ぎ時点の検証

- 本文書が引用するコード箇所・行番号は、本セッション内でRead/Grepツールにより実物を確認済み。
- japicmpのbaseline構造・`publish-snapshot.yml`のゲート構成も実ファイルを確認済み。
- 本文書作成時点で、Repository構成・workflow・Public API・ArchUnit ruleに対する実装変更は行っていない
  （本文書の追加のみ）。
- Maven Central／外部ネットワークへの到達性は本セッションでは検証していない（案Aの実装前にローカルで
  `./mvnw verify`等の実行可否を確認することを推奨）。

# Phase 4 S1 B2通常build対策：別端末・別セッション引継ぎ（2026-10-09）

状態：`REVIEW RECORDED / SESSION HANDOFF / IMPLEMENTATION START NOT APPROVED`。

Ownerは、別端末・別セッションで再開するため、本件の引継ぎ文書作成を指示した。本書は確認済み事象、対処案、branch方針の推奨と再開手順を記録する。code／test／script／POM／CI変更、検証実行、local commit／remote操作の開始承認ではない。「S1区切りで対処する」というOwner判断は、本セッションで確認できる承認記録がないため確定事項として記載しない。

## 1. 次回セッションへ注入する依頼文

以下を次回AIセッションへ貼り付ける。本書も読める状態で渡す。

> `docs/development/phase4-s1-b2-normal-build-remediation-session-handoff-20261009.md`を読み、B2 Referenceテストの通常build対策を再開してください。最初にAGENTS.md、KOIKI project overview Skill、必要に応じてbusiness feature Skill、既存のB1／B2別端末引継ぎを読み、branch／HEAD／作業treeと確認基点からの差分を読取確認してください。本件は、このbranchで追加したB2の3テストが通常Surefire対象になり、Git管理外の固定tmp classpath、Tooling target、java.exeに依存する問題です。推奨は、現在のfeature branch内で通常build対策だけを独立commitにまとめることです。広いTooling整理とR4の置換・廃止は別作業に分けます。まず現状を再確認し、A案を第一候補として具体的な変更path・準備手順・検証集合・資源／時間上限・cleanup・停止条件をOwnerレビュー用に提示してください。本依頼文だけで実装やMaven／Docker実行を開始しません。後続の明示承認が既にあれば、その範囲に従い、同じ承認を再要求しないでください。元端末のtmp／target／cache不在を既存B2受入結果の失敗へ読み替えず、旧manifest・受入結果を保持してください。既存のS1目的対応表／R0開始資料の作業方針は維持し、本件だけを理由に非同期実証の方向を変更しないでください。副Agentは使用しません。local commit／push／PR／mergeは個別承認に従います。

## 2. 確認基点と今回の操作範囲

- repository：`KOIKI-JAVAWEB`。
- branch：`feature/phase4-s1-reference-foundation`。
- 読取確認時のHEAD：`6939799a48ed0c4e876bf92e74c22a7dc7453918`。
- 本書作成前の作業tree：clean。
- 今回行ったこと：source、POM、CI定義、固定manifest、既存引継ぎ・方針文書の読取りと、本書の新規作成。
- 今回行っていないこと：code／test／script／POM／CIの変更、Maven／Docker／CI実行、commit／fetch／push／PR／merge。

上記HEADは本書追加前の確認基点であり、本書を含むcommitではない。本書は作成時点では未commit・未pushである。別端末から自動取得できるとは扱わない。commit／転送はOwner操作または個別承認による。本書を含むcommitが作成された場合は、新端末でそのIDと内容を確認する。

## 3. 確認済み事象

| 対象 | 読取確認結果 |
|---|---|
| root `pom.xml` | `koiki-reference-app`はReactor内。`build-support/phase4-level2-verification`はReactor外 |
| `.github/workflows/ci.yml` | `pull_request`とmainへのpushで起動し、ubuntu-24.04でroot `clean verify`を実行する。branchへのpushだけではこのworkflowは起動しない |
| Reference／Parent POM | 対象B2テストを除外する設定・専用profileは確認されない |
| `B2ProtectedIssueReadTest`、`B2IssueBoundaryTest`、`B2IssueTransactionTest` | 実行を絞る条件annotationはなく、各`@BeforeEach`で`B2ReadConnectionHarness`を作成する |
| `referenceacceptance/notification/B2ReadConnectionHarness` | `B2FrozenSourceProcess`を起動し、実DB・Reference contextへ接続する補助クラス |
| `referenceacceptance/notification/B2FrozenSourceProcess` | L29〜33でTooling `target/`、固定`tmp/b2-preflight-0321079-20261008/tooling-runtime-jdbc-classpath.txt`、Tooling `target/test-classes`／`target/classes`、`java.home/bin/java.exe`を要求する |
| `B2FrozenSourceRegistrationTest` | live supplierを使わないcontext登録テスト。今回の端末依存問題による既定除外対象には含めない |
| B2固定manifest | `SourceFiles`は`verify-s1-b2.ps1`を含む18件。「source20件＋script」ではない |

root buildはToolingを準備しない。通常のclean checkoutからのroot buildには、上記3テストを実行する前提が不足する。元端末でtmp／targetが残っていても、可搬性・再現性の証明にはならない。

CI失敗はsourceからの予測であり、今回の実行結果ではない。CI実行履歴は今回確認していないため、「このbranchでCIが一度も走っていない」とは断定しない。B2の専用検証結果をclean環境のroot build PASSと読み替えない。

## 4. 対処branchと作業の分離

**推奨：現在のfeature branch内で、通常build対策を独立した差分・commitにまとめる。** このbranchで追加したテストの実行前提に起因し、main取込み前に同じ変更系列で解消するとレビューとEvidence追跡が容易になる。これはbranch方針の推奨であり、作成・実行・commit承認ではない。

| 作業 | 推奨する扱い |
|---|---|
| 3テストの既定実行制御、明示実行時の前提指定・準備手順、必要な可搬性対応 | 現在のfeature branchで通常build対策として扱う |
| Tooling全体の保管分類、履歴scriptの扱い、README・Evidence注記 | S1区切りの整理として別途判断。通常build対策と差分を分ける |
| B2凍結Adapter／protocolの置換・廃止 | R4の方式判断と開始条件に従う作業へ分ける。今回先行しない |

作業隔離のため別branchが必要なら、現在のfeature branchから派生させ、対策を元branchへ統合してからmain向けPRを作る案をOwnerへ提示する。未承認でbranch作成・統合しない。mainにはまだ本件B2追加sourceがないため、mainから独立した修正branchを作る前提にはしない。

S1区切りより先に別端末での通常root buildやPR検証が必要になった場合は、対処時期をOwnerへ提示し直す。既知の環境依存失敗を無視してroot build PASSとは扱わない。

## 5. 対処方式の候補と必要条件

### A案：Reference内に残し、既定実行を制御する（第一候補）

- JUnit条件またはMaven profile等で対象3クラスを既定では実行しない。具体的な方式・property名は未採用。
- classpathファイルの所在を明示入力にし、元端末の固定tmp pathを前提にしない。
- Java起動pathをOSに対応させる。
- Toolingのcompile／package、coordinator用test-classes、runtime依存classpathの生成手順を具体化する。実行条件だけを追加して再実行手順を欠いた状態にしない。
- 明示実行を選択した場合、前提不足は明確な失敗とする。条件不成立によるskipをB2検証PASSと扱わない。
- root buildへのTooling module追加、Reference mainからTooling Java型への依存、正式配布物へのTooling混入を行わない。

変更量を抑える狙いの候補であり、採用・変更範囲・検証開始はOwner判断で確定する。

### B案：Tooling側の受入テストへ移す

3テストと補助クラスの移動案。ただしReferenceの既存test helper等への依存があるため、単純なファイル移動で済むとは扱わない。必要な依存関係、fixture Ownership、Reference artifactとの接続方法を確認してから比較する。POM／依存変更やtest helper配布を自動で許可する案ではない。

## 6. 広い分別整理へ引き継ぐ事項

以下は通常build対策とは別のOwner判断事項とする。

- `verify-s1-b2.ps1`は固定tmp path、端末内承認記録、`Win32_Process`等に依存する履歴script。保管する場合は「履歴保存・通常実行対象外・再実行には新しい環境準備と対応する開始判断が必要」とREADME等へ明記する案。mainに残すかは未判断。
- `ProcessCrashRecoveryIT`だけでなく、`PublicationRecoveryTest`も`B1ResourceLimits`へ依存する。
- `B1ProcessHarness`は`B1ReadContract`・`B1SourceCollector`等へ依存する。分類上の別行だけを根拠にhelperを単独抽出・削除しない。
- B2の置換見込み対象には、Adapter／Configuration以外に`FrozenRecoverySourceSettings`、`RecoveryIssueProtectionPort`、`ProtectedRecoveryIssueService`、FoundationConfigurationのImport、対応テスト・SQLも含めて扱いを点検する。全てを廃止対象と決めたものではなく、再利用／置換／保管／未判断を分ける。
- 現行方針は「置換見込み」。R3／R4の保護モデルには全停止メンテナンス型も残るため、「R4で必ず廃止」と断定しない。
- Toolingのtest／script／runtimeをReference・Framework配布物へ混入させない。一方、B2で個別承認されたReference-owned Adapter等の存在自体をTooling混入と扱わない。

## 7. 別端末での再開手順

1. `AGENTS.md`、`docs/agent/skills/koiki-project-overview/SKILL.md`、関連するbusiness feature Skillを読み、後続承認がないか確認する。副Agentは使用しない。
2. 次の読取commandでsourceを確認する。未commit差分がある場合は保全し、reset／clean／強制上書きしない。

   ```powershell
   git status --short
   git branch --show-current
   git rev-parse HEAD
   git log -5 --oneline
   git diff --stat 6939799a48ed0c4e876bf92e74c22a7dc7453918 HEAD
   ```

3. 本書と既存引継ぎの所在・内容を確認する。先端が進んでいれば、本件に関わるsource・POM・CI・承認記録の差分を読む。branch名だけで確認済みsourceと同一とは扱わない。
4. 元端末のtmp／target／raw／JAR／Maven・Docker cacheはGitから移らない。新端末の不存在を明示し、旧固定runnerをそのまま起動しない。
5. §3の問題がまだ残るか再確認する。実装済みなら重複修正せず、承認・変更・検証Evidenceを照合する。
6. A／Bの採用案、変更path、明示実行手順、検証集合、資源／時間／作業量・再実行上限、cleanup・停止条件を具体化してOwner判断へ提示する。本書だけを根拠にroot `clean verify`を先行しない。
7. 開始承認後は必要なsource固定・環境preflightを満たしてから作成・検証する。旧B2残予算を新作業へ自動流用しない。権限エラー時はAGENTSと実行環境の承認手順に従い、迂回しない。

### Ownerレビューに提示する検証候補

- 新端末のcleanな検証用source・出力でroot `clean verify`を行い、対象3クラスの既定実行制御と既存回帰の成立を確認する。必要な依存・Docker環境を先に確認する。
- 対象3クラスを明示選択し、準備から実行・終了・cleanupまで確認する。既存B2の新規54件の構成と変更影響を照合し、登録8件・Tooling18件・Reference28件のうち必要な回帰集合をOwner判断で確定する。
- OS対応を変更する場合は、Windows専用pathが残らないことと、Linux CI条件に対する確認方法を提示する。WindowsだけのPASSをLinux実行済みと扱わない。
- 関連失敗・上限超・前提不一致・cleanup失敗では停止する。追加修正・再実行を自動で拡張しない。

上記は検証候補であり、実行を承認した集合・上限ではない。CI実行は対応するremote承認に従う。

## 8. Evidenceと承認境界

- B1／B2の受入済み結果と旧固定manifestは保持する。新しい変更後sourceを旧hashに合わせて差し替えたり、過去の受入結果を新sourceの検証結果として扱ったりしない。
- source移動・条件追加・準備手順変更は、新しいEvidenceで旧path／commit／hashと新path／commit／結果の対応を記録する。Git改行正規化によるraw byte hash差も区別する。
- 本件対処はB2初回issue／read受入の再オープン、配送／復旧・DoD PASS、Phase 4全体開始ではない。
- Reference Level 2 runtime、publication／migration、肯定consume／close、sender／listener／runner、worker委譲、Framework API／Rules／依存変更を本件へ混ぜない。
- local commitはOwner操作または操作前のOwner確認。push／PR／merge等はRemote Gateの個別承認に従う。過去のpush指示を今回の恒常的許可にしない。

## 9. 関連正本・引継ぎ

- [B1／B2別端末・別AIセッション引継ぎ](phase4-s1-b1-b2-cross-device-session-handoff-20261008.md)：S1全体の再開入口。目的対応表・R0開始資料の作業方針を維持する。
- [B1／B2振り返り・目的適合確認](phase4-s1-b1-b2-purpose-retrospective-20261008.md)：成果の再利用・知見・置換見込み。
- [Reference非同期経路の軌道修正案](phase4-s1-async-reference-direction-correction-draft-20261008.md)：R0〜R5と保護モデルの判断事項。
- [B2成果固定・次作業引継ぎ](phase4-s1-stage-b2-fixed-results-next-session-handoff-20261008.md)：source・raw／artifact所在と再実行条件。
- [B2固定manifest](../architecture/validation/phase4-s1-stage-b2-fixed-evidence-manifest-20261008.json)：旧source18件・artifact・証拠index。
- [B2受入Evidence](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md)：受入範囲、artifact epoch、失敗履歴・訂正・cleanup。
- [B2限定開始票](phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md)：旧作業の承認境界。今回の追加変更を自動承認しない。

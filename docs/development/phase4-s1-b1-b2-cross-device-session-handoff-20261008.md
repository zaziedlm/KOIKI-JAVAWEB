# Phase 4 S1 B1／B2：別端末・別AI作業session引継ぎ（2026-10-08）

状態：`B1/B2 ACCEPTED / DIRECTION REVISED / SESSION HANDOFF`。Ownerは本書と振り返りのcommit、ここまでの作業のremote pushを明示指示した。2026-10-09、Ownerは[軌道修正案](phase4-s1-async-reference-direction-correction-draft-20261008.md)の方向を次回作業の方針として採用し、本書・振り返りへの反映とcommit／pushを指示した（同案§9）。後続操作の実装／検証開始は未承認。本書を次回AIへ渡し、source／環境差分の確認、目的対応表の整理、R0開始判断資料の作成から再開する。

## 1. branch・source基点とremote操作

- repository：`https://github.com/zaziedlm/KOIKI-JAVAWEB.git`
- branch／push先：`feature/phase4-s1-reference-foundation`／`origin`の同名branch。
- B2実装固定：`d70b883a1edface5f524272e564f715afb1bab25`（18 file）。
- B2受入・固定manifest・引継ぎ：`e4b44ac0c7788cfc3a30ffe874313d8efa1577c8`（6 file）。
- 振り返り・本書の初版：`249309310e63c6f134ade9030dc8e86fa23c882b`（remote反映済み）。
- 方針見直しの反映（軌道修正案の追加と本書・振り返りの更新）を含むcommitが、session終了時のsource基点。そのIDは次のread-only commandで取得する。

```powershell
git log -1 --format=%H -- docs/development/phase4-s1-b1-b2-cross-device-session-handoff-20261008.md
git status --short
git rev-parse HEAD
```

push前の対象branch fetchでremote behind0／local ahead18を確認。今回の2文書commitを加え計19 commitを通常pushする。force push・PR・merge・workflow dispatch・publishは指示に含めない。実push成功／remote先端一致は操作結果で確認し、commit済み本文だけから成功と推定しない。

方針見直しの反映時（2026-10-09）は、fetch後にremote先端＝local HEAD＝`2493093`（behind0／ahead0）を確認し、方針見直しcommit 1件を同branchへ通常pushする。指示範囲は前回と同じで、force push・PR・merge等を含めない。

別端末では同branchを通常のGit手順で取得し、remote tip／HEAD・本書のcommitを照合する。既存未commit差分があれば保全して確認し、reset／clean／強制上書きしない。先端が進んでいたら以後の差分を確認して基点を決める。

## 2. 最初に読む正本

1. `AGENTS.md`と`docs/agent/skills/koiki-project-overview/SKILL.md`。業務機能設計時はbusiness-feature-work正本も読む。
2. [振り返り・目的適合確認](phase4-s1-b1-b2-purpose-retrospective-20261008.md)。Phase 4全体とS1、B1／B2、今後の出口を区別する。続けて[Reference非同期経路の軌道修正案](phase4-s1-async-reference-direction-correction-draft-20261008.md)を読む。方向は§9でOwner採用済み、詳細の一部は次回確認事項。
3. [S1完遂方針§3](phase4-s1-completion-direction-decision-20261002.md#3-s1で完遂を目指すもの)と[DoD実演計画](phase4-s1-dod-demonstration-plan-20261005.md)。拒否・保留と配信／復旧完遂を区別する。
4. [段階B契約](phase4-s1-stage-b-operational-integration-contract-review-draft-20261008.md)と[B2方式review](phase4-s1-stage-b2-use-boundary-review-draft-20261008.md)。B-C01〜10・操作別開始・D11／D12を保持する。
5. [B1 Evidence§23〜24](../architecture/validation/phase4-s1-stage-b1-read-connection-20261008.md#23-限定検証の完了owner結果レビュー入力2026-10-08)、[B2 Evidence§17〜22](../architecture/validation/phase4-s1-stage-b2-issue-read-20261008.md#17-30承認済み再installe2e初実行と最終audit2026-10-08)、[B2開始票](phase4-s1-stage-b2-issue-read-limited-start-review-20261008.md)。提出時のpending／失敗履歴より後段の承認を優先する。
6. [B2固定引継ぎ](phase4-s1-stage-b2-fixed-results-next-session-handoff-20261008.md)と[固定manifest](../architecture/validation/phase4-s1-stage-b2-fixed-evidence-manifest-20261008.json)。証拠所在・artifact epoch・再実行条件を確認する。

## 3. 完了済みと承認境界

B1はTooling test所有の読取接続52件と採用済み回帰・package／cleanupまで完遂し、Owner受入済み。B2初回issue／readは新規54＋回帰301＝355件、artifact整合・package非混入・cleanup成立、Owner受入済み。B2 source18件と文書等6件は上記2commitで固定済み。受入範囲と同じ方式を再承認対象へ戻さない。

B2は有限凍結sourceとReference-owned保護scopeによる初回issue transactionの成立まで。通常無効・未接続拒否、evidence Port empty、既存Identity／scope／TTL・保存／Audit・Securityを維持する。肯定consume／close・送信／復旧・worker委譲・Reference Level 2は未開始。

今回のpush承認は当該branchのここまでのcommitを転送する許可。後続push／PR／mergeや追加操作の恒常的許可ではない。Framework／Customer Ownershipを混在させず、副Agentは使用しない。

## 4. 別端末に移るもの／移らないもの

Gitから取得できるのはsource／test／SQL／検証script・README／承認記録／Evidence文書・manifest／本書と振り返り。manifestの証拠indexはhash・所在であり、raw本文ではない。

以下は元端末内で保全済みだが、push対象外。別媒体移管／backupは未実施。

- `tmp/b2-verification-0321079-20261008/`とB1各raw：XML／exit／result・失敗原本・最終audit・sourcehash。
- `tmp/b2-preflight-0321079-20261008/`：classpathと全依存manifest。
- `tmp/b2-isolated-compile-r2-0321079-20261008/`：sourceコピー／class／Reference JAR。
- `tmp/b2-artifact-alignment-0321079-20261008/`と`tmp/b2-e2e-finish-0321079-20261008/`：Framework生成JAR・entry比較・旧cache backup／before-after。
- `tmp/b2-runtime-route-r2-0321079-20261008/`：端末内訂正／続行runner・cache復元helper。
- 各moduleの`target/`、`~/.m2`、Docker image／Chromium cache、実行時ENV。

Reference受入後JAR `5F78A9...`、整合済みWeb MVC `347542...`、Tooling `E72019...`、元Reference `604412...`の全文SHAは固定manifestと固定引継ぎ参照。別端末の同SNAPSHOT座標のJAR一致を仮定しない。元targetのconstructor Nullable／旧cache生成履歴UNKNOWNを解消したと扱わない。manifestのraw byte hashはGitの改行正規化によるcheckout hashと区別する。

文書による結果再見・設計reviewはGit取得だけで開始可能。元raw／JARの直接照合には原本の移管が必要。別端末での実行は新しい隔離出力とpreflightが必要で、旧runnerの固定tmp pathをそのまま起動しない。証拠移管が必要になった場合は秘密・接続情報を含む原log等の取扱い、対象／保存先を確認し、hash照合する。

## 5. 次回の作業順序

**方針見直し（2026-10-09 Owner採用）：** 本書初版の作成後の点検とOwner判断により、consume／close接続の深掘りより先に、Reference appへ最小の非同期経路を組み込み、正常系から異常系・停止後回復へ進める方向へ修正した。根拠・段階案・維持する境界は[軌道修正案](phase4-s1-async-reference-direction-correction-draft-20261008.md)、採用範囲は同案§9を参照する。初版で次の入口としていた「肯定consume接続reviewの準備」は、R4の残課題へ移す。

1. source基点／status／AGENTSと承認履歴を確認し、上記の証拠・artifactが新端末に存在するかを明示する。欠落を既存受入結果の失敗へ読み替えない。
2. **S1の目的対応表を作成する。** DoD 4-1／4-2／4-3／4-4／4-5／4-12について、成立済み・未成立・次に必要な実証／証拠・担う機構・Ownership・開始条件を整理する。軌道修正案§3の役割分担（4-3は送信境界の冪等key主軸、permitは4-4主軸）と§4の段階R0〜R5を対応付ける。B1／B2のPASSを配送・復旧のDoD PASSへ転記しない。
3. **R0開始判断資料を作成する。** Reference限定Level 2 runtime、依存、publication schema／migration、ADR要否、Ownership、AGENTS／SkillのLevel 2開始制限の限定解除差分、R1の対象path・検証上限・停止条件・cleanupを示す。軌道修正案§7のreview軽量化の範囲も、この資料でOwnerへ諮る。
4. §5の保護モデル（全停止メンテナンス型／稼働中個別復旧型）は、R3開始前の判断事項として対応表とR0資料に明記する。
5. permit側の肯定consume／close、D11歴史対象閉鎖／別publication抑止、D12後続競合は、R4（復旧操作の統制）で既存基盤をReferenceの復旧経路へ接続する際の残課題として保持する。
6. R0のOwner開始判断前に、code／SQL／POM・Level 2依存・肯定操作を先行しない。

今回は上記対応表・R0資料は未作成。次回の文書整理入口であり、実装開始を指示するものではない。

## 6. 実行へ進む場合の新環境確認

JDK／Wrapper Maven、offline依存、Tooling compile `jdbc,s1-contract,s1-web`とruntime `jdbc`、Reference実解決classpath・受入artifact／package内依存、Docker Server、PG17／Ryuk／Chromium、空きmemory／disk、当該process／container baselineを確認する。過去PID2660／4684は元端末IDEの記録で、新端末の停止対象／baselineに流用しない。

既存上限はmemory8 GiB／disk10 GiB、heap768 MiB、最大JDK4・PG1等。新しい操作には個別の数値／予算を確定する。B2記録済み約34.61分＋未計測管理5分、回帰約21.70分、B1保守的管理150分は旧実行の管理仮定。別端末で予算をリセットせず、未計測作業・今回文書整理を含む不確かさを保守的に再評価する。B2残枠を後続操作へ自動流用しない。

元端末の最終auditはcontainer／当該JDK残留・ready file／検証ENV0。現在や新端末の環境状態を保証する記録ではない。Docker named pipe／Maven cache権限エラー時はAGENTSの承認済み手順と実行環境の権限付き承認を使い、迂回しない。

## 7. 次回AIへの依頼文

> 本書・振り返り・軌道修正案を読み、remote branch・HEAD・作業treeと元端末からのsource／環境／証拠差分を確認してください。B1・B2初回issue／readのOwner受入とlocal固定は完了済みです。軌道修正案の方向はOwner採用済みです（同案§9）。まずS1全体の目的対応表（成立済み／未成立／次に必要な証拠／担う機構）を作り、続けてReference appへ最小の非同期経路（expense承認→notification listener→provider stub、固定通知key）を組み込むR0開始判断資料を作成し、正常系→異常系→停止後回復の順に進める計画を具体化してください。permit基盤は復旧操作の認可・Audit（DoD 4-4）へ後から接続します。Level 2依存・code／SQL・肯定consume／close・送信／remote等は、具体的な個別承認前に開始しません。

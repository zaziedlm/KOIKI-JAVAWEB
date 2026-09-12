# Phase 2 Gate C C-2 publish capture correction review

## 1. Status and boundary

- 記録日: 2026年9月12日
- Gate: `Phase 2 / Gate C / C-2 second corrective slice`
- failed run: `34693368000`
- published source: `e10f88c82824b9c4b34215480730fa52b8c19e0d`
- corrective branch: `fix/phase2-publish-metadata-history`
- status: `RECOVERY DIRECTION AND TOOLING CORRECTION OWNER APPROVED / COMMIT PENDING`
- Ownership: Tooling / Workflow / Architecture Evidence
- production artifact / Framework Public API / migration change: 0

## 2. Failed Capture and remote state

run `34693368000`ではsource authorization、formal package boundary、aggregate Public API、local publish dry run、
protected environment approvalおよび13座標publishが成功した。`Capture exact remote snapshot identity`は
`Expected one pom snapshotVersion; found 3.`で失敗し、manifest uploadとfresh remote Verifyはskipされた。

remote metadataのread-only調査では、checksum extensionとは別に過去のunclassified POM / JAR snapshotVersionが保持されていた。
今回publishのtimestampは全座標で`20260912.130848`、座標別resolved valueは次のとおりである。

| resolved value | Coordinates |
|---|---:|
| `0.1.0-20260912.130848-3` | 4 |
| `0.1.0-20260912.130848-2` | 5 |
| `0.1.0-20260912.130848-1` | 4 |

13座標のpublish stepは成功したが、24 payload SHA-256、aggregate signatureおよび11 same-source `japicmp`をremote payloadへ
結び付けたmanifestがないため、本runをaccepted baselineとしない。package / version削除、workflow rerunまたは無断再publishは行わない。

## 3. Architecture Owner recovery direction

Architecture Ownerは、別runによるread-only recoveryではなく、同一workflow run / source commit内でpublish、Captureおよび
fresh remote Verifyを完結させる要件を厳格維持し、修正後に改めてpublishする方針を選択した。
failed runのpayloadはhistorical unaccepted publishとして保持し、新しい成功runのmanifestだけをPhase 2 baseline候補とする。

## 4. Durable metadata contract

今回のtimestampまたはbuild numberを固定値として扱わず、将来の反復publishでも次の契約を適用する。

1. publish直前に13座標のunclassified POM / JAR snapshotVersion集合を取得する。
2. pre-publish inventoryをpublish前にworkflow artifactとして保存する。
3. publish後metadataとの差分を座標 / extensionごとに求め、追加値がちょうど1件であることを要求する。
4. JAR座標ではPOM / JARのresolved valueが同一であることを要求する。
5. checksum extension、classifier付きartifactおよび過去履歴を今回のidentityへ含めない。
6. 複数追加、追加0、部分公開、POM / JAR不一致またはinventory identity不一致はfail-closedとする。
7. manifestへsource commit、workflow run / attempt、pre-publish inventory SHA-256、座標別resolved valueおよび24 payload SHA-256を固定する。
8. fresh Verifyはmetadata上のlatestを再解決せず、manifestのtimestamped valueを取得する。

workflow concurrencyは同じPhase 2 publish workflowの並行実行を防ぐが、差分が複数なら選択を推測せず失敗させる。
これによりrepository履歴が増えた後の別タイミングのpublishでも同じ検査を利用できる。

## 5. Implementation and local verification

- `p2-c2-snapshot-metadata.ps1`: 履歴集合抽出、publish前後差分の一意選択、POM / JAR一致検査
- `verify-p2-c2-snapshot-metadata.ps1`: 3世代POM、2世代JAR、checksum、classifier、追加0 / 2件、不一致の正負fixture
- `verify-p2-c2-publish-state.ps1`: `Inventory` mode、同一run / attempt検査、inventory hash、差分Capture
- `verify-p2-c2-publish-dry-run.ps1`: inventory → publish → Capture → Verifyの同一順序
- `publish-phase2-snapshot.yml`: publish前inventory保存とCaptureへの引き渡し

local full dry runは、履歴fixture、13 / 13 deploy、座標別resolved value、24 payload SHA-256、aggregate signatureおよび
11 same-source `japicmp`をすべてSUCCESSとした。production artifact、Public API、migration、13座標publish unit、
environment、rulesetまたはrequired checksは変更していない。

## 6. Proposed remote sequence

1. 本corrective sliceをOwner reviewしcommitする。
2. branchをpushし、`main`向けPRを作成する。
3. required checks 7 / 7成功後にmerge commit方式でmergeする。
4. 新しいmerge後main CI / runtime全8 job成功を確認する。
5. environment / ruleset設定が不変であることをread-backする。
6. 新しい40文字main SHAを指定してworkflowを新規runとして一度dispatchする。
7. protected environment承認後、同一runのpublish、Capture、manifest uploadおよびfresh Verifyを確認する。

run `34693368000`はrerunしない。新runが失敗した場合も自動retry、package削除または追加publishを行わず、Evidenceを保全して再reviewする。

## 7. Architecture Owner review points

1. run `34693368000`をpublish成功 / Capture失敗 / Verify skipとして受け入れ、accepted baselineにしないか。
2. failed runのpayloadをhistorical unaccepted publishとして保持し、削除またはrerunしないか。
3. §3の同一run要件を厳格維持した再publish方針を正式な回復方法としてよいか。
4. §4のpublish前後metadata差分を、今回と将来の反復publishに対する恒久契約としてよいか。
5. pre-publish inventoryをpublish前にretention 7日のartifactとして保存してよいか。
6. 複数差分、差分0、部分公開またはPOM / JAR不一致をfail-closedとしてよいか。
7. §5のlocal verificationを実装受入れEvidenceとしてよいか。
8. production artifact / Public API / migration / publish unit / environment / ruleset / required checksが不変であることを受け入れるか。
9. 本修正のcommit後、§6の新しいPR / main CI / new main SHA / new workflow run順序へ進めてよいか。

推奨結論は上記9点を承認し、同一run要件を維持した新しいpublish / Capture / Verifyを§6の順序で実施することである。
Owner承認前はcommit、push、PR、workflow dispatchまたは追加publishを行わない。

## 8. Architecture Owner approval

2026年9月12日、Architecture Ownerは§7の9 review pointsを確認し、提案どおり承認した。
run `34693368000`をpublish成功 / Capture失敗 / Verify skipのhistorical unaccepted publishとして保持し、
package / version削除またはfailed runのrerunを行わない。

同一workflow run / source commit内でpublish、Capture、manifest uploadおよびfresh remote Verifyを完結する要件を厳格維持し、
修正後に新しいmain SHAで改めてpublishする。publish前後metadata差分、pre-publish inventoryのretention 7日、
座標内POM / JAR resolved value一致、同一run / attemptおよびfail-closed条件を、今回と将来の反復publishに対する恒久契約として承認する。

production artifact、Framework Public API、migration、13座標publish unit、environment、rulesetおよびrequired checksは変更しない。
本修正と承認記録をcommitした後、§6のbranch push、PR、main CI、設定read-back、新しいworkflow runの順序へ進めてよい。
新runが失敗した場合も、自動retry、package削除または追加publishは行わず、Evidenceを保全して再reviewする。

# S1 R0-B：最終結果・Owner受入票（現行、2026-10-10）

**COMPLETE / OWNER ACCEPTED（2026-10-10）。** 必要最小限の検証は全て成立し、Ownerが最終結果を受け入れた。追加実行・監視網羅拡張を行わず、ここで区切る。次作業の入口は[区切り・残作業引継ぎ](phase4-s1-r0-closeout-remaining-tasks-handoff-20261010.md)。

[成立結果表](../architecture/validation/phase4-s1-r0-rules-source-fixed-preflight-20261009.md)。[最小実行の途中履歴](../archive/phase4-s1-r0-20261010/minimal-execution/README.md)と[それ以前の承認・監視履歴](../archive/phase4-s1-r0-20261010/README.md)はアーカイブへ分離した。

## 1 目的と今回の受入対象

module単位Level選択を伴うRule28／29限定改訂、旧API bytecode互換性、Referenceの保護と通常buildからのS1／B2専用test分離を確認する。source固定HEADは`c11dbbceb0241f1257f9bfc153105d8f55125c1c`、branchは`feature/phase4-s1-reference-foundation`。Framework Rulesの今回source・test変更は固定commit済み、後続実行によるproduction code／POM／依存／Security／migration変更なし。

## 2 必要集合の最終結果

| 集合 | 最終結果 | 扱い |
|---|---|---|
| Rules 新規40＋既存67 | 107 PASS、failure／error／skip0 | 単独固定結果を保持、今回root内107も成立、二重加算なし |
| PL2既存規約 | 4 PASS、failure／error／skip0 | 旧候補との整合確認、候補の既知の検出限界は維持 |
| package・旧Consumer互換性 | PACKAGE_LEGACY_PASS | 旧bytecode再compileなし／不変、実行・公開descriptor成立、test／fixture非混入 |
| Reference E2E | 既存journey1 PASS、failure／error／skip0 | 固定Reference JAR、既存業務／Security／Audit／DB／log assertion・cleanup成立 |
| 通常root | ORDINARY_ROOT_PASS、297実行＋92skip | fresh60 XML／389 invocation、failure／error0、S1 7 class64＋B2 3 class28だけskip、他skip0 |
| S1／B2専用92件 | 通常buildの分離成立 | 専用再検証は実行しない |
| 監視pure74・途中失敗 | 本体進捗へ加算しない | 凍結・アーカイブ保持、網羅拡張なし |

## 3 証拠・artifact epoch・資源の照合

source1156／元code・test・POM不変（承認文書差分除外）、root XML60 fileのhashと389 invocationを照合済み。root内Rules107を単独107と二重加算しない。最大有限観測JVM3／PG1／Ryuk2／client11、memory・disk下限とraw／run上限内。これは有限sampleの成立で、全時点連続監視・本番資源保証を主張しない。Maven Exit0／BUILD SUCCESS・JobEmpty、保護Java2件のidentity保持、container cleanupと最終baseline照合成立。追加live読取を行ったという意味ではない。

package／E2Eで成立したRules／Reference JARはtarget外へ別保存。root clean verifyは新epochとしてhash固定した。Rules main26 class・Reference main169 entry・nested KOIKI9 JARの全非directory entry内容が保存済みpackageと一致し、外部依存107 JARはbyte一致。新root JARのhashを旧E2E実行hashへ読み替えず、同sourceの再package整合として記録する。追加E2E／旧Consumer実行は不要と判断し、実行0。

端末証拠は`tmp/r0-rules-9c41aa4-20261009-first/minimal-preparation-20261010/`の`pl2-four-fixed-evidence.json`、`package-legacy-fixed-evidence.json`、`reference-e2e-one-fixed-evidence.json`、`root-rerun1-fixed-evidence.json`、`r0-minimal-final-evidence.json`。raw／script／JARはGit対象外で保存し、Git commitだけでは別端末へ移らない。

通常root初回208秒は停止・未成立として保持。追加1回244秒で成立、root累計452秒／600秒枠内、残148秒を追加実行理由に使わない。現在cleanup成立の読取証拠は過去Cleanup=falseを書き換えない。初回SQL失敗の詳細原因は未確定であり、TCP起動確認と有限診断保存等の訂正後成立を根本原因の完全証明へ読み替えない。

## 4 Owner最終受入と残境界

2026-10-10、Ownerは「最終結果を確認し、Owner受け入れを承認します。」と明示した。上表のR0限定検証結果を**COMPLETE / OWNER ACCEPTED**とする。受入記録・履歴分離文書の追加local commitは操作前のOwner確認またはOwner操作、remoteは個別承認に従う。今回の追加commit／remoteは0。

R1／Reference Level2・非同期runtime／sender・listener・復旧接続、実運用・DoD／Gate・Phase4全体、専用S1／B2再検証、monitor一般化は開始しない。次工程はR1開始案の再確定と個別開始判断を経る。端末の最終manifestに残るOWNER_PENDINGは受入前の履歴として保持し、別の`r0-owner-acceptance-20261010.json`へ今回の受入を追記保存する。

## 5 最終予算と停止位置

Owner最終root244秒を含め累計2566秒、証拠照合・最終固定・履歴分離1秒を計上して累計2567秒、残1033秒（17分13秒）、cleanup300秒予約。必要な検証集合は成立したため、残時間を消化する追加実行は行わない。source／証拠／Git差分確認だけで終了する。

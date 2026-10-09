# S1 R0-B：成立結果・Owner受入記録（現行、2026-10-10）

**COMPLETE / OWNER ACCEPTED（2026-10-10）。** Ownerの「最終結果を確認し、Owner受け入れを承認します。」により、以下の限定結果を受入済み。R0の追加検証は残さない。

| 必要な確認 | 成立結果 |
|---|---|
| Rules 新規40＋既存67 | 107 PASS、failure／error／skip0。root内107も成立、二重加算なし |
| PL2既存規約4件 | 4 PASS、failure／error／skip0 |
| package・旧Consumer | PACKAGE_LEGACY_PASS、旧bytecode不変／公開descriptor・非混入確認 |
| Reference E2E 1件 | 1 PASS、failure／error／skip0、既存assertionと固定JAR不変 |
| 通常root | 297実行＋S1 64／B2 28skip＝389、failure／error／他skip0、60 fresh XML |
| 資源・cleanup・source | 承認済み上限内、Job空・container cleanup成立、保護Java保持、source1156不変 |
| S1／B2専用92件 | 通常buildの分離だけ確認、専用再検証なし |

source1156／元code・test・POM不変（承認文書差分除外）、root XML60 fileのhashと389 invocationを照合済み。root内Rules107を単独107と二重加算しない。最大有限観測JVM3／PG1／Ryuk2／client11、memory・disk下限とraw／run上限内。これは有限sampleの成立で、全時点連続監視・本番資源保証を主張しない。Maven Exit0／BUILD SUCCESS・JobEmpty、保護Java2件のidentity保持、container cleanupと最終baseline照合成立。追加live読取を行ったという意味ではない。

source固定HEAD `c11dbbceb0241f1257f9bfc153105d8f55125c1c`。独立した107件は`evidence/isolated-rules-tests-xml/`、後段は`minimal-preparation-20261010/`の固定manifest4件と`r0-minimal-final-evidence.json`に保持する。通常rootの途中187件・旧XML・monitor74例を今回完全集合へ加算しない。

| artifact epoch | Rules JAR SHA256 | Reference JAR SHA256 |
|---|---|---|
| package／旧Consumer／E2E入力 | `F9544EA78132BB49E422CC28F70047BED6BDE7370C6C56EA33D1F0995DD1AB04` | `313C3F72FF04BDA3D96D3746BFEB006727D468A7D431E6CAE4F30ECEB08B0804` |
| root-rerun1 clean verify成果 | `2A0E8ADF31088C2A4B08C029DDFF62304E031AA6EF3DAB7069C4BFC4A22BC86A` | `68C1A2B6FFB1A83C12598B2C5E32161E23D5F69DEC11A969179EE058869CF890` |

両epochのRules26 class・Reference main169 entry・nested KOIKI9 JAR全entry内容が一致し、外部依存107 JARはbyte一致。root新hashでE2Eを実行したとは扱わず、追加再検証0。旧Consumer bytecodeは不変。保存入力JARはtarget外へ別保存した。

root初回208秒STOPPED／Cleanup=falseは歴史として保持、その後の読取で現在cleanup成立、追加1回244秒で通常root全体成立。SQL失敗の詳細原因は未確定で、有限訂正後の成立と区別する。実行上限はJVM4／PG1／Ryuk2／client14、最大有限sample3／1／2／11。Root累計452秒／600秒枠内。最終保全後の累計2567秒・残1033秒、cleanup300秒予約。

本体の検証集合は全て成立し、Owner受入済みとしてここで停止する。追加local commit・remote・R1開始は実施していない。[最終結果・Owner受入票](../../development/phase4-s1-r0-root-connection-limit-correction-owner-review-20261009.md)に受入対象と残境界を集約。次工程は[区切り・残作業引継ぎ](../../development/phase4-s1-r0-closeout-remaining-tasks-handoff-20261010.md)から進める。[最小実行の途中履歴](../../archive/phase4-s1-r0-20261010/minimal-execution/README.md)と[旧監視等の履歴](../../archive/phase4-s1-r0-20261010/README.md)はアーカイブだけで参照する。

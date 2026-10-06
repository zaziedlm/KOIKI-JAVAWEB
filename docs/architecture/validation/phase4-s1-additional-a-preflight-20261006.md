# S1追加局所検証A preflight（2026-10-06）

**本preflight時点の状態: BLOCKED — 必須artifact欠落。** この時点ではA1〜A5は未作成・未実行であり、JPA方式の不成立を示す結果ではない。後続の[環境整備実地検証](framework-local-artifact-readiness-20261006.md)で不足を解消し、[Aの18件LOCAL PASS](phase4-s1-additional-a-jpa-20261006.md)へ続行した。以下は停止時点の証拠を保持する。

## 承認範囲と開始状態

[追加契約§6.1](../../development/phase4-s1-additional-local-verification-contract-and-estimate-draft-20261005.md#61-a単位のowner開始判定記録2026-10-05)と[引継ぎ](../../development/phase4-s1-additional-a-next-session-handoff-20261005.md)に基づき、Owner一人＋Codexの順次協働でA preflightを開始した。OwnershipはTooling、対象は`build-support/phase4-level2-verification/`。project overview / business featureの正本SkillとAGENTS.mdを確認した。

開始branchは`docs/daily-development-workflow`、HEADとlocal remote-tracking refはともに`6a76b81b034f5d841699cea03d779c62c0cce009`、作業ツリーclean。承認記録のcommitを確認した。remote通信による最新状態の確認は実施していない。

## 現在値と権限手順

| 項目 | 確認結果 |
|---|---|
| Java | Temurin 21.0.12、runtime build 21.0.12.1+1-LTS |
| Wrapper | Maven 3.9.16、既存cacheから権限付き実行で起動 |
| Docker | Client 29.1.4-rd / Server 29.1.3、linux/amd64 |
| 固定DB image | `postgres@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73`をlocal inspectで確認 |
| available memory | 7,914,800 KiB（証拠保存時）、4 GiB以上 |
| C disk free | 515,599,327,232 bytes（証拠保存時）、10 GiB以上 |
| container | 診断時0件。今回DB / Ryukは起動していない |
| 実効設定 | `-o -Pjdbc help:effective-pom`成功。Surefire 3.5.6、既存argLine指定なし |

通常sandboxではDocker config / named pipe、CIM照会を拒否された。Wrapperもcacheへ到達できず`DefaultDownloader`から接続を試み、`SocketException: Permission denied: connect`で終了した。`-o`はWrapper自体の取得を防ぐ保証ではない。download成功の記録はない。

AGENTS.mdの承認済み手順と実行環境の承認を使い、既存Wrapper cacheの存在をread-onlyで確認してから、同じoffline Maven操作を権限付きで再実行した。Docker Serverの応答を確認し、権限拒否をEngine故障と判定していない。OS / AI権限設定は変更していない。

## artifact解決と停止理由

`dependency:tree`はexit 0だが、`spring-modulith-starter-jdbc:2.1.1`のPOM欠落警告がある。tree成功だけをartifact存在の証拠にせず、`dependency:resolve`をofflineで実行した。

```powershell
.\mvnw.cmd -o -f build-support/phase4-level2-verification/pom.xml -Pjdbc dependency:resolve "-DoutputFile=target/s1-additional-evidence/s1-a-20261006-preflight/resolved-artifacts.txt"
```

2026-10-06 10:05:40 JST、Maven exit 1 / BUILD FAILURE（0.799秒）。直接原因は`org.springframework.modulith:spring-modulith-starter-jdbc:jar:2.1.1`が未取得でoffline解決不能。権限付きの`Get-Item`でも当該JARの不存在を確認した。Surefire 3.5.6 JARは存在する。effective POM取得時にはFailsafe 3.5.6の欠落警告もあったが、Aは`test`であり、その警告をAの直接失敗原因とはしない。他の不足がないことは未保証。

契約§1 / §3 / §6.1のartifact欠落時停止・download / install対象外に従い、fixture作成前で停止した。POM / profile変更、別cacheへの切替、追加download / install / publishによる解消は行っていない。原因未変更rerunも行っていない。

## 保存証拠と再開条件

raw証拠は`build-support/phase4-level2-verification/target/s1-additional-evidence/s1-a-20261006-preflight/`。実効POM、dependency tree / console、resolve console / exit、Docker version、DB digest、container一覧、memory / diskを保存した。targetはGit管理外であり、下記checksumは内容照合用で、raw保存の代替ではない。

| 証拠 | SHA-256 |
|---|---|
| resolve-console.txt | `84ca9c086137b0e3481b3006c1d00feaa2a8879fdc95dd40b7266520ad78be8c` |
| effective-pom.xml | `678780f989f1db37b1cf784e878c79615f84d4059c9f1c106d2399777354e353` |
| dependency-tree.txt | `1dd8d64508f6350c4b2e23ed004beb5532a3b5d79e1e5c433086eeaf2d6697e5` |

再開には必要artifactのcache準備とoffline解決成立が必要。download / installを今回のA作業へ含めるにはOwnerの別判断が必要であり、既存A作成・実行承認の取り直しは不要。artifact準備後に資源・実効設定・checksumを再確認し、A1〜A5のmethod / invocation期待件数固定 → test専用Configuration / model / SQL作成 → A単独実行へ進む。現時点では件数対応・source固定・JPA SQL / flush / lock・role実動・送信probeの証拠は未作成。

既存L1〜4、SQL、正式source、POMは変更していない。B / C、正式Tier / Reference、DoD / Gate、remote操作は未実施。今回はdocsとGit管理外のpreflight証拠だけを作成した。

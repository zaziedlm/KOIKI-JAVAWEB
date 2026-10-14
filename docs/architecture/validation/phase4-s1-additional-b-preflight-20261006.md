# S1追加局所検証B：preflight（2026-10-06）

**状態:** BOM不整合RESOLVED／offline依存解決PASS（2026-10-06）。初回BLOCKED後、OwnerのBOM単独local install承認に従って修復した。B fixture作成・実行へ再開した。
**開始契約:** [B専用開始票](../../development/phase4-s1-additional-b-start-review-20261006.md)§2・6・8。

## 実施範囲と結果

承認されたTooling POMの選択profile `s1-contract`に、既存Identity／Auditのtest依存だけを追加した。A source／resources、正式Framework source、migration、remoteは変更していない。

`"-Pjdbc,s1-contract"`を指定し、offline effective POM、dependency tree、dependency resolveを実行した。各exit codeは0だが、Identity artifactのPOMがinvalidでAudit依存versionが欠落する警告を検出した。treeではIdentityがleafになり、期待するSecurity等の推移依存が展開されない。BUILD SUCCESSだけではpreflight成立としない。

最初のcommandはPowerShellでprofile引数のcommaが解釈され、parse errorとなりMaven未実行だった。引数全体をquoteして修正した。上記は修正後の実行結果。

Dockerの権限付きread-only診断ではServer応答を確認した。available memoryは9,289,340 KiB、Cドライブ空きは514,930,016,256 bytesで、承認済み4 GiB／10 GiB条件を満たした。DB container／testは起動していない。cacheの取得・install・publishは実施していない。

## 原因の照合

cached Parent／Identity／Audit POMは現在のsource POMとSHA-256が一致した。一方、同じ`org.koikifw:koiki-dependencies-bom:0.1.0-SNAPSHOT`のcached POMはsourceと異なり、改行差を除いても不一致だった。

| BOM POM | SHA-256 |
|---|---|
| cached | `2004A9E143AE550C0925EF5A47AA6E577ACFFFF3749B32EAE0CCB272DEE68D2A` |
| source | `6CB0E6738694056E0A026340A438A31F6A69EE2178DA867C5F21E1933FD5E031` |

cached BOMにはAudit／Identityの管理座標がなく、source BOMには両方のversion定義がある。Identity POMはParent経由でこのBOMを利用し、Audit依存に個別versionを記載しない。この不整合が今回のmissing version警告の原因である。POM一致だけからcached JARを現在HEAD製とは認定しない。BOM修復後もJAR／migrationの出自照合は残る。

## 必要な対処案と再開条件

Owner判断へ提示する最小差分は、現在sourceのBOM POMだけを同じlocal repositoryへinstallすること。親を持たないpackaging=pomの単独moduleを対象とし、他のKOIKI artifactを一括再build／installしない。

```powershell
.\mvnw.cmd -o -N -f koiki-dependencies-bom/pom.xml install
```

同一SNAPSHOT座標のcached BOMとlocal metadataを更新するため、同cacheを利用する他のlocal buildにも影響する。承認後、更新前BOMをEvidenceへ保存し、更新後hashを照合する。offline実行でplugin不足等が出れば、追加取得へ自動拡張しない。

続いてBのeffective POM／tree／offline resolveを再確認し、警告解消と推移依存展開、JAR／migrationの出自を確認する。別の不整合が残れば必要差分を提示する。preflight成立後は既に承認済みのB条件でfixture作成へ進む。

B開始票§2は「本票はKOIKI再install・snapshot取得／publishの包括許可ではない」とし、§6はartifact不整合時の停止を指定している。このためBOM local installは今回実施せず、個別判断へ戻す。Skillによる追加承認要求ではなく、Owner確認済み開始票の停止条件を適用した。

## raw Evidence

`build-support/phase4-level2-verification/target/s1-additional-evidence/s1-b-20261006-preflight/`にeffective-pom.xml、dependency-tree.txt、artifacts.txt、各console／exit、cache-source-comparison.json、docker-version.txt、memory.json、disk.jsonを保存した。targetは一時証拠であり、上記の判定とhashを本書に保持する。

## BOM単独修復の承認・実行と再開（2026-10-06）

Ownerは「現在sourceのBOMだけをlocal installする対処を承認します」と判断した。上記commandをoffline・単独moduleで実行し、install plugin 3.1.4、exit 0、BUILD SUCCESS、0.373秒、12:27:09 JSTで終了した。更新前POMを`bom-before-install.pom`へ保存した。更新後cache SHA-256はsourceの`6CB0E6738694056E0A026340A438A31F6A69EE2178DA867C5F21E1933FD5E031`と一致した。他のKOIKI artifactのinstall／取得／publishはしていない。

続いて同じprofileでeffective POM／tree／resolveをoffline再実行し、全exit 0。Identity POM invalid／Audit version欠落の警告は解消した。treeはIdentity → Security／Data JPA、Security → Boot Security／OAuth2の推移依存を展開した。今回のBOM修復だけでこの依存解決不足は解消した。

既存cache JARをRepositoryの既存`koiki-starters/<artifact>/target/` JARとentryごとに比較した。Identity 46／Audit 20／Security 6／Data JPA 7 entryについてclass差分は0。Identity／Data JPAは全entry一致、Audit SQLとSecurity AutoConfiguration.importsのbyte差は改行を正規化すると一致した。これは既存local buildとの内容照合であり、現在HEADからの再buildやsource commitの認定ではない。開始票記載のJAR hashを固定し、fixtureはその実artifactからSQLを読み込む。

| migration | cached JAR内SHA-256 | 現在sourceとの照合 |
|---|---|---|
| Audit V2026090300 | `339659E8F17F690D6FE15BCB990832A4CBFC02BAFCDA6CC9C13A958E8FB5C434` | 改行差のみ。source byte hashは`D178378869493743626847FBDDC2D863C879A2A27A19974DBFE3027B38B8A208` |
| Identity V2026090301 | `FBE8BDF78D35C8D437185388BBA2B5F34219C602A71F66BE55028048A9D8A4FF` | byte hash一致 |

最初のmigration照合commandはsourceのprefix `koiki-starters/`を欠き、SourceSha256がnullになったため、`jar-provenance-review.json`単独を照合成功の証拠としない。正しいpathで作成した`jar-target-entry-comparison.json`と`jar-resource-text-comparison.json`を採用した。JAR manifestはJava／plugin情報のみでcommit情報はなく、cache記録はlocal metadataである。この限界を保持し、正式配布のprovenance受入へ拡張しない。

再実行のrawは`*-after-bom-*`、`bom-install-*`、`bom-after-install-hashes.json`と上記照合JSONへ保持した。Bean／schema／Spring transactionの実行結果は別のB検証Evidenceへ記録する。

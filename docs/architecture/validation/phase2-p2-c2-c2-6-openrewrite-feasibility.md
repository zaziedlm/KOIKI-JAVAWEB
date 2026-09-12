# Phase 2 P2-C2 C2-6 OpenRewrite feasibility review

## 1. Status and boundary

- verification date: 2026年9月12日
- work package: `P2-C2 / C2-6`
- implementation base HEAD: `4d03eab`
- branch: `feature/phase2-p2-c1-postgresql-migration`
- status: `COMPLETE / LOCAL VERIFIED / ARCHITECTURE OWNER REVIEW PENDING`
- Ownership: Tooling Evidence
- production artifact / Framework Public API change: 0

C2-6は、OpenRewriteによるKOIKI所有API変更の適用可能性だけを、`build-support`配下のRoot Reactor外・非配布fixtureで確認する。
正式recipe artifact、実在する旧KOIKI version、Customer source、Spring Boot移行、required CIおよびremote publishは対象外とする。
正式な`koiki-migration-recipes` moduleとrelease判断はPhase 5に残す。

## 2. Implemented feasibility prototype

`build-support/openrewrite-feasibility`に次を追加した。

- `recipe`: syntheticな旧型
  `org.koikifw.legacy.identity.LegacyFrameworkUserId`を現行の
  `org.koikifw.identity.FrameworkUserId`へ変更する単一recipe
- `fixture`: 固定した旧Consumer source、旧型のsynthetic定義およびConsumer test
- `expected`: 承認対象となる変換後source
- `manual-actions.md`: dependency除去、意味差review、property / annotation等の別recipe、Spring公式recipe、Customer testを手動残件として固定
- `verify-p2-c2-openrewrite.ps1`: 隔離Maven repositoryとGUID付き一時directoryでbefore / after、冪等性、変換後test、境界およびcleanupを検査

OpenRewrite Maven PluginはMaven Centralから認証なしで取得できる`6.46.1`、recipe APIはpluginと揃えた`8.89.0`へ固定した。
このversion pinは非配布Toolingだけに存在し、Root Parent / BOMおよびFramework artifactへ伝播しない。

旧API定義はConsumer移行対象ではないため、fixtureのplugin設定で`LegacyFrameworkUserId.java`を除外する。検証scriptはその
SHA-256が変換前後で不変であることを別途検査し、利用箇所だけが変更されたことを保証する。

## 3. Local verification result

次を実行した。

```powershell
pwsh -NoProfile -File build-support/openrewrite-feasibility/verify-p2-c2-openrewrite.ps1
```

結果は成功し、次を確認した。

1. 現行`koiki-starter-identity`と必要なdependencyを空の隔離Maven repositoryへstageできる。
2. recipe unit testは変換正例とCustomer所有型の非変更例の2件が成功する。
3. 旧Consumerは変換前にcompile / testできる。
4. 旧型のimport、field型およびfactory callだけが現行`FrameworkUserId`へ変換され、追跡済みexpected sourceと完全一致する。
5. 旧API定義のSHA-256は変化しない。
6. 2回目のrecipe適用後もJava / XML source treeのSHA-256が変化せず、冪等である。
7. 変換後Consumerは現行`koiki-starter-identity`に対してcompile / testできる。
8. prototypeはRoot Reactor、formal release unit、BOM、snapshot publishおよび`koiki-testing`へ含まれない。
9. GUID付き一時directory、隔離repository、fixture targetおよびrecipe targetは終了時に削除される。

```text
Phase 2 P2-C2 C2-6 OpenRewrite feasibility succeeded (before/after / idempotence / transformed test / manual actions / non-distribution).
```

remote state、GitHub environment、workflow、package、PRおよびbranchへの変更は行っていない。C2-5のremote snapshot publishも
引き続き`NOT APPROVED / NO-GO`である。

## 4. Interpretation and limitations

この結果が示すのは、型情報を利用した単一のKOIKI所有Java API変更について、固定fixtureに対する変換、期待値比較、冪等性および
変換後testを非配布Toolingとして構成できることだけである。実在する旧KOIKI contractへの互換性、Customer applicationの完全自動移行、
意味的同値性または全KOIKI API / property変更の網羅を主張しない。

Spring Boot自身の変更はSpring側で保守されるrecipeを利用する方針とし、本prototypeでは再実装しない。dependency除去とCustomer固有の
domain behavior確認も自動化対象外であり、`manual-actions.md`をrelease時の移行手順にそのまま昇格させない。

## 5. Architecture Owner decisions requested

1. C2-6をRoot Reactor外・非配布のTooling feasibilityとして受け入れ、production artifact / Public API変更0と判定してよいか。
2. syntheticなKOIKI所有型変更1件について、before / after完全一致、旧定義非変更、冪等性および変換後compile / testを成立Evidenceとしてよいか。
3. Customer所有型の非変更testと手動残件reportを、過大claimを防ぐ境界Evidenceとしてよいか。
4. OpenRewrite plugin / APIのversion pinとrecipe class、fixture、expected sourceおよび検証scriptを、正式release unit / required CIへ含めないか。
5. 正式recipe artifact、旧版Reference / Project Template fixture、Spring Boot recipe構成およびrelease CIをPhase 5の個別Owner reviewへ残してよいか。
6. C2-5 remote publishを`NO-GO`のまま維持し、C2-6承認後はC2-7 closeoutへ進んでよいか。

推奨結論は上記6点を承認し、C2-6を`COMPLETE / ARCHITECTURE OWNER APPROVED`としてC2-7の全体回帰、cleanup、inventoryおよび
handoff作成へ進むことである。

2026年9月12日、Architecture Ownerは上記承認文案を理解し、6判断を提案どおり承認した。本承認はsyntheticなKOIKI所有型変更
1件のfeasibility成立を対象とし、実Customer sourceの完全自動移行、意味的同値性、過去KOIKI versionとの互換性または正式recipe
提供を認定しない。C2-6を`COMPLETE / ARCHITECTURE OWNER APPROVED`とし、成果物のcommit後、clean HEADでC2-7 local closeoutへ
進めてよい。

OpenRewrite prototypeは初期フレームワーク運用に含めない。具体的には、Framework artifact、Starter、BOM、Root Reactor、
`koiki-testing`、Customer配布物、標準導入手順、runtime、required CIおよびsnapshot publishの対象外とする。Repository内には
`build-support`配下の非配布Tooling Evidenceとしてのみ保持し、C2-6の再検証時に明示実行する。正式recipe artifact、旧版fixture、
Spring Boot recipe構成およびrelease CIへの昇格はPhase 5の個別Owner reviewまで行わない。

C2-5 remote snapshot publish、protected environment作成、push、PR、main反映、workflow dispatchその他のremote変更は本承認に
含めず、引き続き`NOT APPROVED / NO-GO`とする。

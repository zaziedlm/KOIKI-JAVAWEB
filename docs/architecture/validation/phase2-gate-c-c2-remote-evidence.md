# Phase 2 Gate C C-2 remote snapshot evidence

## 1. Status and boundary

- 記録日: 2026年9月13日
- Gate: `Phase 2 / Gate C / C-2 remote Evidence`
- final PR: [#33](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/33)
- PR head: `4f58cfb8a1447009020cf6b2c7ec25acca409412`
- published main source: `af7b4f71d885fe4991e5fcf85fcf8888aec6a539`
- workflow run: [`34701933485`](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/34701933485) / attempt `1`
- status: `COMPLETE / ACCEPTED`
- Ownership: Tooling / Workflow / Architecture Evidence
- Phase 2 final status: `COMPLETE / ACCEPTED`

本記録は、Architecture Ownerが承認した同一workflow run / source commit要件に従う再publishの最終Evidenceである。
production artifactのsource、Framework Public API、migration、13座標publish unit、environment、rulesetまたはrequired checksを
変更するものではない。

## 2. Final PR, main CI and settings read-back

PR #33のrequired checks 7件を含む全8 jobは成功し、merge commit `af7b4f71d885fe4991e5fcf85fcf8888aec6a539`として
`main`へ反映された。同commitに対するpush eventでは、CI run
[`34700027409`](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/34700027409)の6 jobとJava Runtime Compatibility run
[`34700027420`](https://github.com/zaziedlm/KOIKI-JAVAWEB/actions/runs/34700027420)の2 job、合計8 / 8が成功した。

publish dispatch前のGitHub設定read-backは次の承認済み値と一致した。

| Subject | Read-back |
|---|---|
| Environment | `phase2-internal-snapshot` / ID `21785949807` |
| Required reviewer | `zaziedlm`、prevent self review `false` |
| Admin bypass | `false` |
| Deployment branch policy | custom branch `main`のみ / ID `59779136` |
| Environment secret / variable | 0 / 0 |
| Main ruleset | `main-merge-protection` / ID `21140116` / active / strict |
| Ruleset bypass | 0 / current user bypass `never` |
| Required status checks | 承認済み7 contextsのまま |

## 3. Same-run execution result

final main SHAを`expected_commit`に指定し、workflowを新規runとして一度だけdispatchした。source、run、attemptを途中で
切り替えず、次の全job / stepが成功した。

| Job / step | Result |
|---|---|
| Confirm approved Phase 2 source | SUCCESS |
| Phase 2 C2-5 publish preflight | SUCCESS |
| Capture pre-publish snapshot inventory | SUCCESS |
| Preserve pre-publish snapshot inventory | SUCCESS |
| Publish the approved thirteen-coordinate unit | SUCCESS |
| Capture exact remote snapshot identity | SUCCESS |
| Upload hash-only publish manifest | SUCCESS |
| Verify Phase 2 remote snapshot | SUCCESS |
| Verify exact remote payloads and same-source API | SUCCESS |

run全体のconclusionは`success`である。fresh runnerの最終ログは
`13 coordinates / 24 hashes / aggregate signature / 11 japicmp comparisons`の成功を明示している。

## 4. Manifest identity

| Field | Value |
|---|---|
| schema / kind | `2` / `publishManifest` |
| source commit | `af7b4f71d885fe4991e5fcf85fcf8888aec6a539` |
| workflow run / attempt | `34701933485` / `1` |
| workflow ref | `zaziedlm/KOIKI-JAVAWEB/.github/workflows/publish-phase2-snapshot.yml@refs/heads/main` |
| actor | `zaziedlm` |
| captured at | `2026-09-12T15:29:31.2077298Z` |
| pre-publish inventory SHA-256 | `726882DDABE8A8EDD6FE36B7085A1EF6F6C7E5E8EBE69714F00CD10AEA74161E` |
| aggregate signature SHA-256 | `850F77939712DE06275CB8CE1F5192B415422C3919371F3FCDDE5A911AE157AE` |

GitHub Actions artifactとして、inventory
`p2-c2-pre-publish-inventory-34701933485`（artifact ID `10300965024`、retention 7日）とmanifest
`p2-c2-publish-manifest-34701933485`（artifact ID `10300541391`、retention 1日）を保存した。
archive digestはそれぞれ
`sha256:2ef9b71f129a995353d720419d4b1325678e4a1ea14783a6b0f917f1b8f85473`、
`sha256:b5137eb32a38d434bbca74e8f7f028d8b5fa6f89644de95696adb5b6aa30ede5`である。

## 5. Resolved payload identity

13座標は共通build numberを要求せず、同じtimestamp `20260912.152800`に対する座標別resolved valueで固定した。
JAR座標は各座標内のPOM / JAR resolved valueが一致している。

| Artifact | Resolved value | POM SHA-256 | JAR SHA-256 |
|---|---|---|---|
| `koiki-dependencies-bom` | `0.1.0-20260912.152800-4` | `04963AEE6EEBDCA1629C674CE1572099B98BB5A7051F54BB490BEF8FA23572AF` | — |
| `koiki-parent` | `0.1.0-20260912.152800-4` | `ADC149D5C693BDCCBA008FD5F6BE8D5DF3BE5F43DCEBACCB47A72892C4BDAE37` | — |
| `koiki-architecture-contract` | `0.1.0-20260912.152800-4` | `7BE7635FE5E776FB0F5B5E4935DB0054D08F6D3CCE01EE7016275C685E1D926F` | `3B76596E81314E1C2A13DBC26194806902CFE248BD71C30E89415B983220949D` |
| `koiki-archunit-rules` | `0.1.0-20260912.152800-4` | `7B24A824B9EBD55794B7A626AE0FBB52A0781FFD1ECEFC18A899B629F4FEDA45` | `6EE4AD89A3A2645BCAD1D0391541FFBF4DE6A2195E79D277771001101FAA70A0` |
| `koiki-starter-api` | `0.1.0-20260912.152800-3` | `28DB09CB014066E08B52918CC4B7795CCAD810EF16042858AA69DF4A43DB975F` | `A855E2ABD07F7F79803FA4B5AB7DA8F4C00A6B2ECBAAB2CA117831920CDBFC54` |
| `koiki-starter-data` | `0.1.0-20260912.152800-3` | `1F96C4F07BF37313786E9F241A20190B2EC09E0147220012D5898503399B8640` | `5F753122E8F78AA8CA4966452A2DCA2252E14539FB4DC54E05C4E5CD74C76207` |
| `koiki-starter-data-jpa` | `0.1.0-20260912.152800-3` | `549DA4561E27244A86C034A9FC621898BE7AFD53E4E9D8450C7D31DFE85C5F77` | `850BB59DE21270D35F01A224270F7AD1F7AA37E9FC92A9D14AC7757795664430` |
| `koiki-starter-observability` | `0.1.0-20260912.152800-3` | `19369DA546AAFC60BB4B61E23410C386F562EEC008CCA63101A4C4C81EBC0286` | `56FE5EB89A54DAE7706D1E85EB4385F2236FA972915B69672D79F233976680EE` |
| `koiki-starter-audit` | `0.1.0-20260912.152800-2` | `4B84B6BF698DFA2144DB55957DD2CB03B39839B77D9AB221353AD94F3F0C5AFB` | `716BBBB70996500EFA30B63E38632C491721451A5AF21A240B1866A2C0F10161` |
| `koiki-starter-security` | `0.1.0-20260912.152800-2` | `04276D1A1AE1EB861BE14D0517C2561E9B73D5E6A0C33329A19E64015E98BBD5` | `E721FEBD496A305E8C1741ECE92FA5506AFC0DF8A74526A5AAB098C85DD2C048` |
| `koiki-starter-identity` | `0.1.0-20260912.152800-2` | `AE648740D19D47407220B5B061A433F42BA52E8279446F81659BFF0B6E8E9C3D` | `D16F2187DDAED135F590F34E3C71DEBF28D1D3BA57ECFF60532E4729B44E9FD4` |
| `koiki-starter-session-jdbc` | `0.1.0-20260912.152800-2` | `D800230028223733100B924155A22A8526EE001204094F00149CD81CC0219231` | `F67E485A4D906AC1747F732917D791B11958F2A504903817CA53DAC850748F97` |
| `koiki-testing` | `0.1.0-20260912.152800-3` | `F7708688C0A126C80B8717BD623BFD18EC03BB04D3C72D794222C52DF3D0755F` | `74FAC0C9F92A05ADD9A9B66626D1430BEE4FD05EEED7A459E0F966C3DB91601F` |

## 6. Public API and failure-history treatment

fresh Verifyは11 JARを同一source local buildと比較し、11 same-source `japicmp` comparisonをすべて成功させた。
aggregate signatureは現行release unit 24型を固定し、Phase 2初回published baselineの内訳をAudit 6型、Identity 10型、
Session 3型、Security 0型とする。Phase 1a固定baselineとは分離し、差し替えない。

run `34686248900`はpreflight失敗 / remote mutation 0、run `34693368000`はpublish成功 / Capture失敗 /
Verify skipのhistorical unaccepted publishとして保持する。削除、rerunまたはaccepted baselineへの昇格は行わない。
accepted baselineはrun `34701933485`のmanifestが固定するpayloadだけである。

## 7. Gate C-3 Architecture Owner review points

1. PR #33、merge commitおよびmerge後main CI / runtime 8 / 8成功をfinal main Evidenceとして受け入れるか。
2. dispatch前のenvironment / ruleset read-backが承認済み設定と一致したことを受け入れるか。
3. run `34701933485` / attempt 1 / source `af7b4f71d885fe4991e5fcf85fcf8888aec6a539`で全工程が完結したことを同一run要件の充足とするか。
4. §4と§5の13座標、24 payload SHA-256およびaggregate signatureをPhase 2 snapshot baseline identityとして承認するか。
5. 11 same-source `japicmp`成功と24型の内訳をPhase 2 Public API remote Evidenceとして受け入れるか。
6. 先行失敗runをhistorical unacceptedとして保持し、成功runのmanifestだけをaccepted baselineとするか。
7. P2-C3 local closeoutと本remote Evidenceを合わせ、Phase 2 Security FoundationのDoDを満たしたと判断するか。
8. production artifact、Public API、migration、publish unit、environment、rulesetおよびrequired checksに追加変更がないことを受け入れるか。
9. Gate CおよびPhase 2を`COMPLETE / ACCEPTED`として最終closeoutしてよいか。

推奨結論は上記9点を承認し、run `34701933485`のmanifestをPhase 2初回published baselineのidentityとして
Gate CおよびPhase 2を`COMPLETE / ACCEPTED`として最終closeoutすることである。

## 8. Architecture Owner approval

2026年9月13日、Architecture Ownerは§7の1〜9について、review point 3を40文字の完全SHAへ変更し、
Phase 2最終状態を既存Phaseの表記と統一して`COMPLETE / ACCEPTED`とすることを条件に、全体を承認した。
本記録はこの2条件を反映済みである。

PR #33、merge commit、merge後main CI / runtime 8 / 8、GitHub設定read-back、およびrun `34701933485` /
attempt 1 / source `af7b4f71d885fe4991e5fcf85fcf8888aec6a539`の同一run完結を最終Evidenceとして受け入れる。
同runのmanifestが固定する13座標、24 payload SHA-256、aggregate signatureおよび11 same-source `japicmp`を
Phase 2初回published baselineとして承認する。

先行失敗runはhistorical unaccepted publishとして保持し、accepted baselineへ昇格させない。P2-C3 local closeoutと
本remote Evidenceを合わせ、Gate CおよびPhase 2 Security Foundationを`COMPLETE / ACCEPTED`として最終closeoutする。

本承認は正式release、Customer配布、追加snapshot publish、OpenRewrite prototypeの正式運用・配布、Oracle対応または
その他のdeferred scopeの実装承認を含まない。これらは各後続Phase / Gateの個別Owner reviewに従う。

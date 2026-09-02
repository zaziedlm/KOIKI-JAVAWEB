# Phase 2 Gate A Security Foundation verification

## 1. Status and scope

| Item | Result |
|---|---|
| Phase / milestone | Phase 2 / Milestone A |
| Gate | Gate A local acceptance COMPLETE / implementation HEAD remote CI COMPLETE / required check review PENDING |
| Validation date | 2026年9月2日 |
| Branch | `feature/phase2-security-foundation` |
| Start commit | `2bd67a5`（P2-A3完了） |
| Java / Maven | Java 21.0.12.1、Java 25 runtime / Maven 3.9.16 |

本記録は、P2-A1〜A3で成立させたSecurity Foundationを、Root Reactor外Consumer、Public API境界、
Java runtime matrixおよびsecret non-exposureを含むGate A aggregateとして検証したEvidenceである。
local aggregateに加え、Draft PR #28のimplementation HEADで`Security Foundation Integration`を含む
remote CIの成立を確認した。required check変更、Ready for review、mergeまたはsnapshot publishは実施していない。

## 2. Gate A Consumer boundary

`build-support/security-foundation-consumer`へTooling-owned Customer-like Consumerを置いた。
このConsumerはRoot Reactorへ含めず、空の隔離Maven repositoryへstageした正式KOIKI release unitだけを解決する。

- Consumerは`koiki-starter-security`とSpring Boot Web MVCを通常dependencyとして利用する。
- Customer-owned `SecurityFilterChain`とFramework fallback chainが2 chainとして合成される。
- public routeは200とSecurity Header、private / unmatched routeは401かつ空bodyとなる。
- `org.koikifw.*.internal`を参照せず、Frameworkの公開Spring標準seamだけを利用する。
- test identity、OIDC issuer、JWT key、secretまたは業務機能をConsumerへ持ち込まない。
- Consumer、test routeおよびruntime probeは正式artifact、Template、Referenceまたは`koiki-testing`へ昇格しない。

## 3. Aggregate verification

再現commandは次である。

```powershell
pwsh -NoProfile -File build-support/security-foundation-verification/verify-gate-a-security-foundation.ps1
```

| Verification | Observable result |
|---|---|
| P2-A3 cumulative acceptance | T0〜T3 23 tests、failure / error / skip 0 |
| Formal release stage | Root Reactor 11 / 11 artifactsを隔離repositoryへstage |
| External Consumer | clean package success、実server HTTP test 1 / 1 success |
| Chain composition | Customer public chain + Framework fallback chain、public 200、private / unmatched 401 |
| Runtime matrix | Java 21でbuildした同一executable JARをJava 21 / 25で起動成功 |
| Same-artifact proof | Java 21 / 25実行の前後でConsumer JAR SHA-256不変 |
| Runtime dependency boundary | Security Starter、OAuth2 Client、Resource Serverあり |
| Deferred dependency absence | Authorization Server、SAML、Redis、WebFlux、Reactorなし |
| Public API fixture | package-private変更success、return type破壊と無承認public追加を期待どおりfailure検出 |
| Public API inventory | P2-A1 baselineと完全一致、Public Java型 / property / error codeの追加なし |
| Root regression | Root Reactor 11 / 11 success、Architecture Contract 4 + ArchUnit Rules 66 tests success |
| Secret non-exposure | 正式artifact / fixture / Consumer JAR / Surefire reportのpattern走査success |
| Cleanup | Gate A、Security fixture、Public API fixtureの一時repository / reportを削除 |

最終コードに対して上記aggregateを3回連続実行し、3回とも
`Phase 2 Gate A local Security aggregate succeeded.`で完了した。

## 4. Java 21 / 25 same-artifact result

Consumer executable JARはJava 21で一度だけpackageした。各runtimeでは非Web probeとしてApplicationContextを起動し、
Java feature、`SecurityFilterChain`、OAuth2 `ClientRegistration`および`JwtDecoder`の解決を確認した。
runtimeごとのrebuildやJAR差し替えは行っていない。

```text
KOIKI_SECURITY_CONSUMER_RUNTIME_SUCCESS expected=21 actual=21
KOIKI_SECURITY_CONSUMER_RUNTIME_SUCCESS expected=25 actual=25
```

各連続実行ではbuild時刻等によりJAR hash自体は異なるが、1回のaggregate内ではJava 21 / 25実行前後の
同一JAR hashが不変であることを機械判定した。

## 5. Public API and ownership review

| Contract category | Gate A result |
|---|---:|
| Public Java types | 0（P2-A1 baselineから変更なし） |
| Public configuration properties | 0 |
| Public Security error codes | 0 |
| Customer customization bean names | 1（変更なし） |

OIDC registration、issuer、client credential、audience、scope mapping、matcherおよびCORSはCustomer / deployment所有を
維持する。FrameworkはSpring / Boot標準seamとdefault-deny fallbackだけを提供し、test fixtureやConsumer固有契約を
公開しない。

## 6. Remote PR verification

2026年9月2日、Draft PR [#28](https://github.com/zaziedlm/KOIKI-JAVAWEB/pull/28)
`feat(security): establish Phase 2 Milestone A security foundation`でremote CIを確認した。

| Item | Observable result |
|---|---|
| PR boundary | base `main` / head `feature/phase2-security-foundation` / Draft |
| Implementation HEAD | `b762549859649e52edebbc6891780271f23c4228` |
| Merge test ref | `73d321da1e5d8166f0063b92b170475fdcb07d38` |
| CI run | `33599209707` |
| Security job | `Security Foundation Integration` / job `100148955356` / success |
| Execution time | 2026-09-02 06:31:23Z〜06:34:17Z、約2分54秒 |
| Runtime matrix | `expected=21 actual=21`、`expected=25 actual=25` |
| Same-artifact proof | SHA-256 `BFD4764B14B6CB210338EEA2A7B1016647845649F5307B6EC537894A1D3CF74A`不変 |
| Cumulative tests | 23 tests、failure / error / skip 0 |
| Public API fixture | `C3 Gate 3 Public API fixture verification: SUCCESS` |
| Final marker | `Phase 2 Gate A local Security aggregate succeeded.` |
| Permissions | `contents: read`、`metadata: read`のみ。secret / Packages権限なし |
| Cleanup | setup-java、checkoutおよびorphan process cleanupを含むpost job完了 |
| PR check rollup | `Security Foundation Integration`を含む7 checksすべてsuccess |
| PR state | mergeable `MERGEABLE` / merge state `CLEAN` |

remote上でもJava 21でbuildした同一Consumer JARをJava 21 / 25で実行でき、local Gate Aと同じ最終markerまで
到達した。別jobの`Public API Compatibility`もsuccessとなり、published baselineとの比較を含む既存required
checksに回帰がないことを確認した。

## 7. Remote-operation boundary and Gate decision

localではPublic APIのpositive / negative fixtureによりcompatibility detector自体を確認した。
published baseline artifactとの比較はpackage tokenとremote artifactを必要とするためlocalでは実施せず、
Draft PRの`Public API Compatibility` job成功をremote Evidenceとした。
Framework側CIへ`Security Foundation Integration` jobを追加し、Temurin 21をbuild JDK、Temurin 25を追加runtimeとして
Gate A aggregateを実行する構成にした。このjobは`contents: read`だけを明示し、secretやPackages権限を持たない。
2026年9月2日、Architecture Ownerは、外部IdP、DB、containerまたは常駐processを使用しないGate Aでは、local aggregateの
3回連続成功に加えて同一commitをremoteで3回rerunする技術的効果は限定的と判断した。required化の前提をfinal HEADでの
remote 1回成功、cleanupと実行時間の確認およびOwner Reviewへ変更し、Milestone Bの3回連続成功条件は維持する。
remoteでのjob実行、published baseline compatibility、push / Draft PRおよびremote checksは完了した。
`Security Foundation Integration`のrequired check化、Ready for reviewおよびmergeは、実行計画§8に従い
それぞれOwner判断後に実施する。

したがって、Gate Aのlocal implementation、implementation HEADでのremote acceptanceおよびEvidence作成は完了しているが、
Gate A全体を`ACCEPTED`とはまだ判定しない。次の判断点は、本Evidenceを反映したfinal HEADのremote checksを確認した後、
`Security Foundation Integration`をrequired checkへ追加するかのOwner Reviewである。Milestone BはGate Aのremote review / mergeを
完了するまで開始しない。

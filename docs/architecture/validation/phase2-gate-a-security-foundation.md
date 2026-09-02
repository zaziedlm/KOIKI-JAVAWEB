# Phase 2 Gate A Security Foundation verification

## 1. Status and scope

| Item | Result |
|---|---|
| Phase / milestone | Phase 2 / Milestone A |
| Gate | Gate A local acceptance COMPLETE / remote required checks PENDING |
| Validation date | 2026年9月2日 |
| Branch | `feature/phase2-security-foundation` |
| Start commit | `2bd67a5`（P2-A3完了） |
| Java / Maven | Java 21.0.12.1、Java 25 runtime / Maven 3.9.16 |

本記録は、P2-A1〜A3で成立させたSecurity Foundationを、Root Reactor外Consumer、Public API境界、
Java runtime matrixおよびsecret non-exposureを含むGate A local aggregateとして検証したEvidenceである。
workflow変更、required check変更、push、PR、mergeまたはsnapshot publishは実施していない。

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

## 6. Remote-operation boundary and Gate decision

localではPublic APIのpositive / negative fixtureによりcompatibility detector自体を確認した。
一方、published baseline artifactとの比較はpackage tokenとremote artifactを必要とするため実施していない。
ローカルで3回成功したGate A aggregateの既存CIへの組み込みとrequired化、published baseline compatibility、
push / PRおよびremote required checksは、実行計画§8に従いOwner承認後に実施する。

したがって、Gate Aのlocal implementationとacceptance Evidenceは完了しているが、Gate A全体を`ACCEPTED`とはまだ判定しない。
次の判断点は、この差分をcommitした後、remote operationを開始するOwner承認である。Milestone BはGate Aのremote review / mergeを
完了するまで開始しない。

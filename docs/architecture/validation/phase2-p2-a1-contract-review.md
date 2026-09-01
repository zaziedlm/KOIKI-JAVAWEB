# Phase 2 P2-A1 contract and evidence review

## 1. Status and scope

- **Validation date:** 2026年9月1日
- **Work package:** `P2-A1 / A1-5`
- **Status:** `COMPLETE / ARCHITECTURE OWNER APPROVED`
- **Branch:** `feature/phase2-security-foundation`
- **Baseline HEAD:** `ef708f251bcc3bcdd7829fd232b1f8a114bb6c04`

本記録はA1-1〜A1-4の実装Evidenceから、P2-A1で固定するcontractと後続CPへ延期するdecisionをreviewした結果である。
Frameworkの正式成果物は`koiki-starter-security`、検証codeはroot reactor外のTooling fixtureが所有する。

## 2. Contract decisions

### 2.1 Artifact and dependency

- 正式成果物は`org.koikifw:koiki-starter-security`の単一leaf artifactとする。
- production dependencyはBoot BOM管理の`spring-boot-starter-security`だけとする。
- Security API / implementation分割、独自JWT、OAuth2、Session、MigrationをP2-A1へ追加しない。
- test route、user、credential marker、MockMvc utilityは非配布fixtureだけが所有する。

### 2.2 Auto Configuration and FilterChain

- Servlet applicationかつSpring Security servlet classpathでだけinternal Auto Configurationを適用する。
- Spring Boot標準Servlet security auto-configurationより先にKOIKI fallbackを構成し、Bootのlogin UI / HTTP Basic既定へfallbackしない。
- fallback chainは`Ordered.LOWEST_PRECEDENCE`で全requestにmatchし、`denyAll`とする。
- anonymous denyはempty bodyの401、authenticated denyはempty bodyの403として観測した。
- CSRFとSpring Security標準Headerを有効のまま維持する。

### 2.3 Customer customization

通常のCustomer customizationは、明示matcherを持つCustomer-owned `SecurityFilterChain`をfallbackより高い順序で
合成する方式とする。別chainが存在するだけではfallbackをback-offさせず、Customer matcher外を必ずdenyする。

fallback全体を明示的に置換する必要がある場合だけ、Customerは
`koikiSecurityFallbackFilterChain`というbean名で`SecurityFilterChain`を提供する。このbean名はJava型ではないが、
Customer customization contractとして`public-api.txt`へ記録する。Spring Securityの`HttpSecurity`、Filter class、
Filter order定数およびinternal Auto Configuration classはKOIKI Public APIにしない。

## 3. Public API inventory

| Contract category | P2-A1 result |
|---|---:|
| Public Java types | 0 |
| Public configuration properties | 0 |
| Public Security error codes | 0 |
| Customer customization bean names | 1 |

T0 / T1はSpring標準型とCustomer-owned beanだけで成立した。独自principal、Permission、callback、annotation、
configuration propertiesまたはProblem Details error codeを追加する必要はない。

専用aggregateはinventoryの完全一致、正式JAR内classがinternal packageの2 classだけであること、
configuration property metadataが存在しないこと、Auto Configuration importsが1 entryだけであることを検査する。

## 4. Deferred property and profile decisions

P2-A1では`koiki.security.*` propertyを提供しない。Security有効／無効の独自switch、required property、blank value、
profile matcher、matcher重複validationをA1で仮定すると、local browser、OIDC、Bearerおよびedge profileの
Owning CPより先に契約を固定してしまうためである。

profile別matcher、required property、fail-fast validationはP2-A3で実際のchainとnegative fixtureを提示して判断する。
local identity、principal、Permission、audit、sessionはP2-B1以降へ延期する。

## 5. ADR and MFA review

Phase 2計画がP2-A1に割り当てたSecurity artifact / profile ADRは、実装結果に基づきADR-046として
Grand Design §30とADR registerへ追加した。Architecture Ownerは2026年9月1日に内容を確認し、`ACCEPTED`とした。

MFAは`phase2-start-preflight.md` §4.2の判断を変更しない。Spring Security 7.1の標準MFA機構をPhase 2で
有効化せず、独自MFAも実装しない。factor登録、recovery、policy、管理、監査、運用手順とacceptanceが
Owner承認された時点を再判断条件とする。P2-A1のdependency tree、production source、property inventoryに
MFA固有成果物は存在しないため、追加ADRは不要と判断する。

## 6. Evidence alignment

| Evidence | Decision supported |
|---|---|
| `phase2-p2-a1-artifact-fixture-boundary.md` | Framework artifactとTooling fixtureのOwnership、release境界 |
| `phase2-p2-a1-dependency-baseline.md` | Boot BOM authority、最小production dependency、除外dependency |
| `phase2-p2-a1-t0-t1-verification.md` | context条件、Customer override、401 / 403、CSRF / Header、credential非fallback |
| `koiki-starter-security/public-api.txt` | 公開型／property／error code 0件、customization bean名1件 |

実装結果はGrand DesignのSpring標準優先、default deny、Customer policy分離およびPublic API最小化を否定しなかった。

## 7. Verification result

| Verification | Result |
|---|---|
| P2-A1 focused aggregate | success |
| Isolated release stage | 11 / 11 projects success |
| T0 / T1 fixture | 10 tests、failure / error / skip 0 |
| Public API inventory | approved 7 entriesと完全一致 |
| Formal Security JAR | internal class 2件、property metadata 0件、Auto Configuration import 1件 |
| Dependency / sensitive-content boundary | deferred dependency 0、credential / secret / private key / PII leak 0 |

## 8. A1-5 conclusion

P2-A1の正式contractは、単一Security Starter、internal Auto Configuration、最下位fallback deny、
Customer chain合成／明示置換、およびPublic Java型・configuration property・Security error code 0件で成立する。
A1-5でproduction codeを変更する必要はない。

A1-6ではfocused module / fixture、root verify、Public API・Null Safety・ArchUnit、dependency・secret境界、
P2-A1 scope差分を最終検査し、P2-A1単位でcommitする。

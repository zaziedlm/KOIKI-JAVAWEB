# Phase 2 P2-A1 T0 / T1 verification

## 1. Status and scope

- **Validation date:** 2026年9月1日
- **Work package:** `P2-A1 / A1-4`
- **Status:** `A1-4 IMPLEMENTED / VERIFICATION PASSED`
- **Branch:** `feature/phase2-security-foundation`
- **Baseline HEAD:** `ef708f251bcc3bcdd7829fd232b1f8a114bb6c04`

本記録は、A1-3で追加した最小Security Auto Configuration候補を、非配布Harnessから
ApplicationContextとHTTP境界で検証したEvidenceである。test application、route、user、credential markerは
`build-support/security-foundation-verification/src/test`だけが所有し、正式Starterや`koiki-testing`へ追加しない。

## 2. T0 ApplicationContext matrix

| Scenario | External result |
|---|---|
| Servlet + KOIKI Auto Configuration | `koikiSecurityFallbackFilterChain` 1件が起動 |
| Non-Web application | Servlet fallback chainを生成しない |
| KOIKI Auto Configuration無効 | KOIKI beanは存在せず、Spring Boot標準default chain 1件が起動 |
| Customer matcher chainを追加 | Customer chainとKOIKI fallbackが合計2件で共存 |
| Customerがfallback bean名を明示置換 | KOIKI候補はback-offし、Customer bean 1件だけが起動 |

P2-A1では公開configuration propertyを導入していないため、独自のenabled property、required property、
blank valueおよびmatcher duplicate validationは存在しない。profile matcherの具体契約はP2-A3所有であり、
A1-4で仮propertyを先行固定しない。A1時点の有効／無効はAuto Configurationの適用有無として検証した。

## 3. T1 request boundary

MockMvcへSpring Security filter chainを適用し、controller到達後の内部状態ではなくHTTP結果を観測した。

| Scenario | Result |
|---|---|
| fallbackへanonymous GET | `401 Unauthorized`、empty body |
| fallbackへauthenticated GET | `403 Forbidden`、empty body |
| fallbackへunsafe POST | CSRFなし／ありの双方がdenyされ、Security Headerを維持 |
| Customer matcherへauthenticated GET | `200 OK` |
| Customer matcherへanonymous GET | `401 Unauthorized` |
| Customer matcherへPOST | CSRFなし`403`、CSRFあり`200` |
| Customer matcher外へauthenticated GET | fallbackが`403` |
| Bearer / edge header / cookie形状の入力 | authenticationへfallbackせず`401` |
| Header baseline | `X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY` |

この結果から、A1-3候補のdefault deny、CSRF、Security Header、401 / 403およびCustomer chain合成は成立した。
login UI、HTTP Basic、test user、credential parser、OAuth2、session、Customer role / permissionは本番実装へ追加していない。

## 4. Sensitive-content guard

T1はcredential markerがresponse bodyまたはresponse headerへ現れないことを直接assertする。
aggregate scriptはさらに次をbyte scanする。

- 隔離repositoryへstageした正式Security Starter JAR
- 非配布fixture JAR
- Surefire XML / text / dumpstream report

検査対象はfixture credential marker、private key material、password / client secret / access token代入形状、
email形状のPIIである。fixtureのcompiled test classは配布artifactではないためscan対象artifactへ含めず、
test markerを保持するsource / test-classesと「漏えい先」を混同しない。

## 5. Verification result

| Verification | Result |
|---|---|
| Focused fixture test | 10 tests、failure / error / skip 0 |
| T0 context matrix | 5 scenarios success |
| T1 HTTP boundary | 4 scenario groups success |
| Sensitive response assertion | marker leak 0 |
| Artifact / report scan | credential / secret / private key / PII pattern 0 |

## 6. A1-4 conclusion

A1-3の最小production foundationは、Servlet applicationでSpring Boot標準default chainを置き換え、
Customer matcher chainの後段で未一致requestをdenyする。Customerは明示chainを合成でき、fallback bean名を
明示した場合だけ置換できる。A1-4の外部観測からproduction classの追加修正は不要と判断する。

次のA1-5では、本Evidenceを基にPublic API 0型／公開property 0件を維持できるか、override contractと
README記述を最終reviewする。

# KOIKI Security Starter

Spring Security標準機能をKOIKIのSecurity Foundationとして構成するFramework-owned Starter。

P2-A1では、Servlet applicationへ最下位順序のfallback `SecurityFilterChain`をAuto Configurationし、
Customerの明示chainに一致しなかったrequestをdenyする。認証方式、Customer route、Role / Permissionは推測しない。
CSRFとSpring Security標準Headerは無効化せず、Customerが明示的に構成するchainでだけoverrideできる。

Customerは対象matcherを持つ`SecurityFilterChain`をfallbackより高い優先順位で定義して合成する。
どのCustomer chainにも一致しないrequestはfallbackが処理する。fallback全体を意図的に置換する場合だけ、
`koikiSecurityFallbackFilterChain`というbean名でCustomer-owned `SecurityFilterChain`を定義する。
単に別の`SecurityFilterChain`が存在するだけではfallbackは無効にならない。

Auto Configuration classとSpring Security componentはinternal実装であり、KOIKI Public Java APIではない。
matcher合成、Customer override、401 / 403、CSRF / Headerの外部挙動は、A1-4 T0 / T1
verification fixtureで確認し、`docs/architecture/validation/phase2-p2-a1-t0-t1-verification.md`へ記録する。
P2-A1の公開Java型、公開configuration property、Security error codeは0件である。
`koiki.security.*`のenabled propertyやprofile matcher propertyはA1では提供せず、Owning CPのEvidence前に固定しない。

P2-A2ではStarterのServlet Auto ConfigurationがSpring標準Method Securityを有効化する。
local Form Login、HTTP Session、application-owned URL chain、RoleからPermissionへの展開は、非配布T2 fixtureで
Spring標準componentによる成立性を実証する。test identity、Permission文字列、routeまたはcredentialを正式Starterへ置かず、
公開Java型、configuration property、Security error codeは引き続き0件とする。

test user、test route、test keyは非配布fixtureだけに置き、正式Starterのproduction sourceには含めない。
Customer固有Role / Permission、login UI、identity persistence、Spring Session JDBCおよびMigrationは
Owning CPまで追加しない。Authorization ServerはPhase 2対象外とする。

P2-A3ではSpring Boot標準のOAuth2 ClientとResource Server starterをdependencyとして提供する。
Customerはbrowser / callback pathとBearer API pathを別の`SecurityFilterChain`で明示し、OIDC LoginのHTTP Sessionと
Bearer JWTを同じpathでfallback認証させない。issuer、client registration、audience、scope変換、CORS allowlistは、
Boot標準`spring.security.oauth2.*`設定またはCustomer-owned Spring beanで構成する。

Starter独自のOIDC / JWT parser、token発行、client secret、profile matcher property、AWS header認証は提供しない。
署名・issuer・audience・時刻・scopeの検証、authorization-codeのstate / nonce、Session / Bearer分離、CORS negativeは
非配布T3 fixtureで確認する。P2-A3でも公開Java型、configuration property、Security error codeは0件のままである。

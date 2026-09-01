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

test user、test route、test keyは非配布fixtureだけに置き、正式Starterのproduction sourceには含めない。
Customer固有Role / Permission、login UI、identity persistence、OAuth2 Client / Resource Server、
Spring Session JDBCおよびMigrationはOwning CPまで追加しない。Authorization ServerはPhase 2対象外とする。

# Security Foundation Consumer

Phase 2 Gate AのTooling-owned Customer-like Consumerである。Root Reactorには含めず、隔離Maven repositoryへ
stageした正式KOIKI artifactだけを解決してbuildする。

Servlet testではCustomer-owned `SecurityFilterChain`とFramework fallback chainの合成、public route、
unmatched denyおよびSecurity Headerを確認する。test identity、OIDC issuer、JWT keyまたは業務機能は置かない。

package済みexecutable JARはJava 21で一度だけbuildし、`--koiki.consumer.runtime-probe=<feature>`を指定して
Java 21 / 25で同一SHA-256のまま起動する。runtime probeはOAuth2 Client / Resource Serverの標準型を解決し、
Java feature一致を確認して終了する。Consumer、testおよびruntime probeはFramework正式artifactではない。

このConsumerが実証するのは、`koiki-starter-security`を隔離repositoryから解決し、Customer-owned chainを合成できる
package / runtime境界である。実業務、identity store、固定user、production credentialまたはProject Templateを提供しない。

エンジニアは[Phase 2 Developer Journey](../../docs/development/phase2-developer-journey.md)でprofileとOwnershipを判断し、
Starter固有の責務は[Starter index](../../koiki-starters/README.md)から確認する。PostgreSQL上でIdentity / Audit / Sessionの
公開契約まで確認する場合は[`postgresql/README.md`](postgresql/README.md)へ進む。

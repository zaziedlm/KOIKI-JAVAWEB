# Security Foundation Consumer

Phase 2 Gate AのTooling-owned Customer-like Consumerである。Root Reactorには含めず、隔離Maven repositoryへ
stageした正式KOIKI artifactだけを解決してbuildする。

Servlet testではCustomer-owned `SecurityFilterChain`とFramework fallback chainの合成、public route、
unmatched denyおよびSecurity Headerを確認する。test identity、OIDC issuer、JWT keyまたは業務機能は置かない。

package済みexecutable JARはJava 21で一度だけbuildし、`--koiki.consumer.runtime-probe=<feature>`を指定して
Java 21 / 25で同一SHA-256のまま起動する。runtime probeはOAuth2 Client / Resource Serverの標準型を解決し、
Java feature一致を確認して終了する。Consumer、testおよびruntime probeはFramework正式artifactではない。

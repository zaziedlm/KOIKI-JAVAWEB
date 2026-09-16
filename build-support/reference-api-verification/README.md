# Reference API verification

P3-C1の非配布Toolingである。Root Reactorには含めず、package済み`koiki-reference-app` JARを
別JVMとして起動し、使い捨てPostgreSQL 17とtest-only OIDC/JWKS issuerに対して実HTTP Bearer journeyを行う。

```powershell
.\mvnw.cmd -pl koiki-reference-app -am -DskipTests package
.\mvnw.cmd -f .\build-support\reference-api-verification\pom.xml test
```

fixtureの鍵、token、user、DBは各test process内だけで生成し、Framework artifact、Reference JAR、
`koiki-testing`、Project Template、production設定へ含めない。

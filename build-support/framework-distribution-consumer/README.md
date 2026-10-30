# E1 independent distribution Consumer

This is non-distributed verification tooling, outside the Root Reactor. It uses
the fixed candidate Parent and installed distribution JARs, without Framework
source-relative references. See the E1 distribution plan for the approved scope.

The initial topology is PostgreSQL 17, one DataSource, the default `public` schema,
and separate Framework / Customer Flyway histories. Customer migrations reference
Framework tables before creating the Customer table, proving the migration order.
The fixture uses a non-superuser role with rights only in its disposable database.

Customer Entity scanning is explicit using the Spring Boot public annotation:

```java
@EntityScan(basePackageClasses = CustomerRecord.class)
```

This registers the Customer's own package; existing Framework auto-configurations
register Identity/Audit packages. No Framework internal class is imported. The
journey checks coexistence, persistence, default HTTP denial, restart and the
failure boundary when Customer scanning is disabled. This is a limited R-05
configuration contract, not automatic scanning for every application layout.

`koiki-starter-data` is declared explicitly because the migration strategy belongs
to that Starter; Data JPA alone is not assumed to provide the migration contract.
The Customer owns its JDBC driver and database-specific Flyway dependency.
Customer Flyway baseline version 0 is explicit because Framework migration makes
the shared schema nonempty before Customer Flyway begins. This is not a policy
for adopting arbitrary pre-existing schemas. Separate schemas, migration-only
connections and replacement Flyway strategies are not tested by this Consumer.

Use the sibling `framework-distribution-verification/verify-consumer.py` runner,
an isolated prepared Maven repository and Docker. It creates one loopback-only DB,
passes fixture passwords by environment, executes the one JUnit journey, captures
the KOIKI dependency tree and removes its owned container even on failure.
The packaged Consumer JAR is verification output and must not be put in the
Framework distribution. The HTTP startup checks run from the test application
contexts; they are not a separate packaged-Consumer process acceptance test.

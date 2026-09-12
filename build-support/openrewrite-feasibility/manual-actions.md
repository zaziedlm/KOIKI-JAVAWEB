# C2-6 synthetic migration manual actions

The prototype automates only the synthetic KOIKI-owned Java type move from
`org.koikifw.legacy.identity.LegacyFrameworkUserId` to `org.koikifw.identity.FrameworkUserId`.

Manual work intentionally remains:

- remove the synthetic legacy API dependency or source after every usage has migrated;
- review semantic differences that cannot be inferred from an equal method shape;
- migrate KOIKI-owned property names, annotations and other API changes with separately reviewed recipes;
- use Spring-maintained recipes for Spring Boot changes;
- run the migrated Customer application's own tests and review domain behavior.

This is not a Customer migration guarantee, a historical KOIKI compatibility claim, or a Phase 5 recipe artifact.

package org.koikifw.buildsupport.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeLegacyFrameworkUserIdTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeLegacyFrameworkUserId())
                .parser(JavaParser.fromJavaVersion().dependsOn(
                        """
                        package org.koikifw.legacy.identity;
                        public final class LegacyFrameworkUserId {
                            public static LegacyFrameworkUserId parse(String value) { return null; }
                        }
                        """,
                        """
                        package org.koikifw.identity;
                        public final class FrameworkUserId {
                            public static FrameworkUserId parse(String value) { return null; }
                        }
                        """))
                .cycles(2);
    }

    @Test
    void changesOnlyTheSyntheticKoikiOwnedType() {
        rewriteRun(spec -> spec.expectedCyclesThatMakeChanges(1), java(
                """
                package com.example.consumer;

                import org.koikifw.legacy.identity.LegacyFrameworkUserId;

                final class SessionOwner {
                    LegacyFrameworkUserId owner(String rawUserId) {
                        return LegacyFrameworkUserId.parse(rawUserId);
                    }
                }
                """,
                """
                package com.example.consumer;

                import org.koikifw.identity.FrameworkUserId;

                final class SessionOwner {
                    FrameworkUserId owner(String rawUserId) {
                        return FrameworkUserId.parse(rawUserId);
                    }
                }
                """));
    }

    @Test
    void leavesUnrelatedCustomerCodeUnchanged() {
        rewriteRun(spec -> spec.expectedCyclesThatMakeChanges(0), java(
                """
                package com.example.consumer;

                final class CustomerIdentifier {
                    String value(String rawValue) {
                        return rawValue;
                    }
                }
                """));
    }
}

package org.koikifw.buildsupport.openrewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;

/** Synthetic feasibility recipe for a KOIKI-owned API type move. */
public final class ChangeLegacyFrameworkUserId extends Recipe {

    private static final String OLD_TYPE = "org.koikifw.legacy.identity.LegacyFrameworkUserId";
    private static final String NEW_TYPE = "org.koikifw.identity.FrameworkUserId";

    @Override
    public String getDisplayName() {
        return "Use FrameworkUserId";
    }

    @Override
    public String getDescription() {
        return "Replace a synthetic legacy KOIKI user identifier type with the current FrameworkUserId API.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeType(OLD_TYPE, NEW_TYPE, true).getVisitor();
    }
}

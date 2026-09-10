package org.koikifw.audit;

import java.util.Objects;
import java.util.Optional;

/** Immutable reference to the trusted actor of an audited operation. */
public final class AuditActor {

    private static final String USER = "USER";
    private static final String SYSTEM = "SYSTEM";
    private static final String ANONYMOUS = "ANONYMOUS";

    private final String type;
    private final String id;

    private AuditActor(String type, String id) {
        this.type = type;
        this.id = id;
    }

    /** Creates a user actor from an immutable Framework user ID, never a login identifier. */
    public static AuditActor user(String immutableFrameworkUserId) {
        String id = requireIdentifier(immutableFrameworkUserId, "immutableFrameworkUserId");
        if (id.indexOf('@') >= 0) {
            throw new IllegalArgumentException("A user audit actor must not be an email address.");
        }
        return new AuditActor(USER, id);
    }

    /** Creates an actor for a Framework-owned background operation. */
    public static AuditActor system(String systemId) {
        return new AuditActor(SYSTEM, requireIdentifier(systemId, "systemId"));
    }

    /** Creates an unauthenticated actor without retaining a presented login identifier. */
    public static AuditActor anonymous() {
        return new AuditActor(ANONYMOUS, "");
    }

    /** Returns the stable actor category. */
    public String type() {
        return type;
    }

    /** Returns the opaque actor ID when the actor is not anonymous. */
    public Optional<String> id() {
        return id.isEmpty() ? Optional.empty() : Optional.of(id);
    }

    private static String requireIdentifier(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 255 || containsControl(normalized)) {
            throw new IllegalArgumentException(label + " must be a safe non-blank identifier.");
        }
        return normalized;
    }

    private static boolean containsControl(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof AuditActor that && type.equals(that.type) && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public String toString() {
        return "AuditActor[redacted]";
    }
}

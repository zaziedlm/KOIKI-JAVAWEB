package org.koikifw.audit;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Immutable, persistence-neutral data supplied to an audit recorder. */
public final class AuditEvent {

    private final String eventType;
    private final AuditActor actor;
    private final String action;
    private final AuditResult result;
    private final @Nullable String subjectId;
    private final @Nullable String resourceType;
    private final @Nullable String resourceId;
    private final @Nullable String reasonCode;

    private AuditEvent(
            String eventType,
            AuditActor actor,
            String action,
            AuditResult result,
            @Nullable String subjectId,
            @Nullable String resourceType,
            @Nullable String resourceId,
            @Nullable String reasonCode) {
        this.eventType = eventType;
        this.actor = actor;
        this.action = action;
        this.result = result;
        this.subjectId = subjectId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.reasonCode = reasonCode;
    }

    /** Creates an event containing the mandatory semantic fields. */
    public static AuditEvent of(
            String eventType, AuditActor actor, String action, AuditResult result) {
        return new AuditEvent(
                requireCode(eventType, "eventType"),
                Objects.requireNonNull(actor, "actor"),
                requireCode(action, "action"),
                Objects.requireNonNull(result, "result"),
                null,
                null,
                null,
                null);
    }

    /** Returns a new event with an opaque subject ID. */
    public AuditEvent withSubject(String subjectId) {
        return new AuditEvent(
                eventType,
                actor,
                action,
                result,
                requireIdentifier(subjectId, "subjectId"),
                resourceType,
                resourceId,
                reasonCode);
    }

    /** Returns a new event with an opaque resource type and ID. */
    public AuditEvent withResource(String resourceType, String resourceId) {
        return new AuditEvent(
                eventType,
                actor,
                action,
                result,
                subjectId,
                requireCode(resourceType, "resourceType"),
                requireIdentifier(resourceId, "resourceId"),
                reasonCode);
    }

    /** Returns a new event with a safe internal reason code. */
    public AuditEvent withReason(String reasonCode) {
        return new AuditEvent(
                eventType,
                actor,
                action,
                result,
                subjectId,
                resourceType,
                resourceId,
                requireCode(reasonCode, "reasonCode"));
    }

    public String eventType() {
        return eventType;
    }

    public AuditActor actor() {
        return actor;
    }

    public String action() {
        return action;
    }

    public AuditResult result() {
        return result;
    }

    public Optional<String> subjectId() {
        return Optional.ofNullable(subjectId);
    }

    public Optional<String> resourceType() {
        return Optional.ofNullable(resourceType);
    }

    public Optional<String> resourceId() {
        return Optional.ofNullable(resourceId);
    }

    public Optional<String> reasonCode() {
        return Optional.ofNullable(reasonCode);
    }

    private static String requireCode(String value, String label) {
        return requireSafe(value, label, 128);
    }

    private static String requireIdentifier(String value, String label) {
        return requireSafe(value, label, 255);
    }

    private static String requireSafe(String value, String label, int maximumLength) {
        Objects.requireNonNull(value, label);
        String normalized = value.strip();
        if (normalized.isEmpty()
                || normalized.length() > maximumLength
                || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " must be a safe non-blank value.");
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditEvent that)) {
            return false;
        }
        return eventType.equals(that.eventType)
                && actor.equals(that.actor)
                && action.equals(that.action)
                && result == that.result
                && Objects.equals(subjectId, that.subjectId)
                && Objects.equals(resourceType, that.resourceType)
                && Objects.equals(resourceId, that.resourceId)
                && Objects.equals(reasonCode, that.reasonCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                eventType, actor, action, result, subjectId, resourceType, resourceId, reasonCode);
    }

    @Override
    public String toString() {
        return "AuditEvent[redacted]";
    }
}

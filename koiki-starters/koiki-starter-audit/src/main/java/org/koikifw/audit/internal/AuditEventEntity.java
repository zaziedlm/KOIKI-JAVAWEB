package org.koikifw.audit.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "koiki_audit_event")
class AuditEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "audit_type", nullable = false, length = 16)
    private String auditType;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "actor_type", nullable = false, length = 16)
    private String actorType;

    @Column(name = "actor_id", length = 255)
    private @Nullable String actorId;

    @Column(name = "subject_id", length = 255)
    private @Nullable String subjectId;

    @Column(name = "resource_type", length = 128)
    private @Nullable String resourceType;

    @Column(name = "resource_id", length = 255)
    private @Nullable String resourceId;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "result", nullable = false, length = 16)
    private String result;

    @Column(name = "reason_code", length = 128)
    private @Nullable String reasonCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "request_id", length = 128)
    private @Nullable String requestId;

    @Column(name = "trace_id", length = 128)
    private @Nullable String traceId;

    protected AuditEventEntity() {
        this.eventId = new UUID(0L, 0L);
        this.auditType = "";
        this.eventType = "";
        this.actorType = "";
        this.action = "";
        this.result = "";
        this.occurredAt = Instant.EPOCH;
    }

    AuditEventEntity(
            UUID eventId,
            String auditType,
            String eventType,
            String actorType,
            @Nullable String actorId,
            @Nullable String subjectId,
            @Nullable String resourceType,
            @Nullable String resourceId,
            String action,
            String result,
            @Nullable String reasonCode,
            Instant occurredAt,
            @Nullable String requestId,
            @Nullable String traceId) {
        this.eventId = eventId;
        this.auditType = auditType;
        this.eventType = eventType;
        this.actorType = actorType;
        this.actorId = actorId;
        this.subjectId = subjectId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = action;
        this.result = result;
        this.reasonCode = reasonCode;
        this.occurredAt = occurredAt;
        this.requestId = requestId;
        this.traceId = traceId;
    }
}

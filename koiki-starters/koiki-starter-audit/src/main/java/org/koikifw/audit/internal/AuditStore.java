package org.koikifw.audit.internal;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.koikifw.audit.AuditEvent;
import org.slf4j.MDC;

final class AuditStore {

    private final EntityManager entityManager;
    private final Clock clock;

    AuditStore(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    void persistAndFlush(String auditType, AuditEvent event) {
        Instant occurredAt = clock.instant();
        AuditEventEntity entity = new AuditEventEntity(
                UUID.randomUUID(),
                auditType,
                event.eventType(),
                event.actor().type(),
                event.actor().id().orElse(null),
                event.subjectId().orElse(null),
                event.resourceType().orElse(null),
                event.resourceId().orElse(null),
                event.action(),
                event.result().name(),
                event.reasonCode().orElse(null),
                occurredAt,
                MDC.get("requestId"),
                MDC.get("traceId"));
        entityManager.persist(entity);
        entityManager.flush();
    }
}

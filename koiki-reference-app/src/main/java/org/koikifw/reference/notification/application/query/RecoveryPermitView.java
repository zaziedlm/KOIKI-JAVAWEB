package org.koikifw.reference.notification.application.query;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Final observation value; no entity escapes the transaction boundary. */
public record RecoveryPermitView(UUID permitId, RecoveryTarget target, UUID actorId,
        Instant issuedAt, Instant expiresAt, @Nullable Instant closedAt,
        @Nullable UUID confirmedBy, @Nullable String resultRef, long version,
        @Nullable UUID operationId, @Nullable String workerGeneration, @Nullable Instant consumedAt) { }

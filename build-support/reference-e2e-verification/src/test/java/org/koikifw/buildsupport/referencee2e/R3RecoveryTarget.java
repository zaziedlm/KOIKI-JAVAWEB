package org.koikifw.buildsupport.referencee2e;

import java.util.UUID;

/** Tooling snapshot captured independently from the packaged Reference process. */
record R3RecoveryTarget(String environmentId, UUID requestId, UUID publicationId,
        UUID eventId, UUID recipientId, String listenerId, String status, int attempts,
        String key, String payloadHash) { }

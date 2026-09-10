package org.koikifw.identity.internal;

import java.util.UUID;

record AccountProtectionState(UUID userId, boolean eligibleForAccountAttempt) {}

package org.koikifw.identity.internal;

record LoginFailureState(boolean accountLockedNow, boolean sourceBlockedNow) {}

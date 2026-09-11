package com.example.consumer;

import org.koikifw.legacy.identity.LegacyFrameworkUserId;

final class SessionOwner {

    LegacyFrameworkUserId owner(String rawUserId) {
        return LegacyFrameworkUserId.parse(rawUserId);
    }
}

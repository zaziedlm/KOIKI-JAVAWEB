package com.example.consumer;

import org.koikifw.identity.FrameworkUserId;

final class SessionOwner {

    FrameworkUserId owner(String rawUserId) {
        return FrameworkUserId.parse(rawUserId);
    }
}

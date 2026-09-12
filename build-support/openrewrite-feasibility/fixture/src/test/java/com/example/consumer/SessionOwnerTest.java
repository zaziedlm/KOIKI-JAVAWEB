package com.example.consumer;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionOwnerTest {

    @Test
    void parsesTheOwnerWithTheMigratedKoikiApi() {
        String value = "123e4567-e89b-12d3-a456-426614174000";

        assertEquals(UUID.fromString(value), new SessionOwner().owner(value).value());
    }
}

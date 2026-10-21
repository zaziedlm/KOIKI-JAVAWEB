package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.util.UUID;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** DTO-local canonical UUID parsing, without changing existing REST coercion. */
final class CanonicalUuidDeserializer extends ValueDeserializer<UUID> {
    static boolean isCanonical(String value) {
        return value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Override
    public UUID deserialize(JsonParser parser, DeserializationContext context) {
        String value = parser.currentToken() == JsonToken.VALUE_STRING ? parser.getString() : null;
        if (value == null || !isCanonical(value)) {
            return context.reportInputMismatch(UUID.class, "A canonical UUID string is required.");
        }
        return UUID.fromString(value);
    }
}

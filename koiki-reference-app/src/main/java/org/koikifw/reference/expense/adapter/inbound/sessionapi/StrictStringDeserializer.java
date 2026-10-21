package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.util.Objects;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Keeps JSON numbers from silently becoming decimal strings. */
final class StrictStringDeserializer extends ValueDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            return context.reportInputMismatch(String.class, "A JSON string is required.");
        }
        return Objects.requireNonNull(parser.getString());
    }
}

package org.koikifw.reference.externalcheck.application.dto;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure.Kind;

/** Application-owned values; controllers map them into separate transport records. */
public final class ExternalCheckData {
    private ExternalCheckData() { }

    public record Query(String base, String quote, LocalDate date) {
        public Query {
            Objects.requireNonNull(base);
            Objects.requireNonNull(quote);
            Objects.requireNonNull(date);
            if (!(base.equals("USD") || base.equals("EUR")) || !quote.equals("JPY")) {
                throw new ExternalCheckFailure(Kind.INVALID_INPUT);
            }
        }
    }

    public record Rate(UUID operationId, String base, String quote, LocalDate date, String rate) { }
}

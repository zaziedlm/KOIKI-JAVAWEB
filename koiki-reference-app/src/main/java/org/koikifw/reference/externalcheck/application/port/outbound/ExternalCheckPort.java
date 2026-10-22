package org.koikifw.reference.externalcheck.application.port.outbound;

import java.util.UUID;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Rate;

/** A read-only reference value, with no reservation, payment, or receipt side effect. */
public interface ExternalCheckPort {
    Rate lookup(UUID operationId, Query query);
}

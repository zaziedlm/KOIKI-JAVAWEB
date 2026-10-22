package org.koikifw.reference.externalcheck.adapter.outbound.external;

import java.util.UUID;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Rate;
import org.koikifw.reference.externalcheck.application.port.outbound.ExternalCheckPort;
import org.springframework.resilience.annotation.Retryable;

/** Separate proxy boundary: one retry layer, only for explicit provider 503. */
@Retryable(includes = ExternalCheckFailure.TransientRateFailure.class,
        maxRetriesString = "${koiki.api.resilience.retry.max-retries}",
        delayString = "${koiki.api.resilience.retry.delay}",
        timeoutString = "${koiki.api.resilience.retry.timeout}")
public class RetryingRateLookup implements ExternalCheckPort {
    private final HttpExternalCheckAdapter adapter;
    public RetryingRateLookup(HttpExternalCheckAdapter adapter) { this.adapter = adapter; }

    @Override
    public Rate lookup(UUID operationId, Query query) { return adapter.lookup(operationId, query); }
}

package org.koikifw.reference.externalcheck.application;

import java.util.UUID;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Rate;
import org.koikifw.reference.externalcheck.application.port.outbound.ExternalCheckPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Coordinates a bounded, authorized lookup outside all business transactions. */
public class ExternalCheckService {
    private static final Logger logger = LoggerFactory.getLogger(ExternalCheckService.class);
    private final ExternalCheckPort port;

    public ExternalCheckService(ExternalCheckPort port) { this.port = port; }

    @PreAuthorize("hasAuthority('EXTERNALCHECK:USE')")
    @ConcurrencyLimit(limit = 2, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
    public Rate lookup(Query query) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new ExternalCheckFailure(ExternalCheckFailure.Kind.INVALID_CONTEXT);
        }
        UUID operationId = UUID.randomUUID();
        try {
            Rate rate = port.lookup(operationId, query);
            logger.info("External rate lookup operationId={} outcome=SUCCESS", operationId);
            return rate;
        } catch (ExternalCheckFailure failure) {
            logger.info("External rate lookup operationId={} outcome={}", operationId, failure.kind());
            throw failure;
        }
    }
}

package org.koikifw.reference.notification.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.UserStatus;
import org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryIdentityPort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTargetPort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.koikifw.reference.notification.application.query.RecoveryPermitView;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.reference.notification.domain.model.RecoveryConsumption;
import org.koikifw.reference.notification.domain.model.RecoveryPermit;
import org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository;
import org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordinates persistence only. There is no delivery, recovery runner or external I/O here. */
public class RecoveryPermitService {
    private static final Logger logger = LoggerFactory.getLogger(RecoveryPermitService.class);
    private final RecoveryPermitRepository permits;
    private final RecoveryConsumptionRepository consumptions;
    private final RecoveryIdentityPort identities;
    private final RecoveryScopePort scopes;
    private final RecoveryTtlPolicyPort policy;
    private final RecoveryTargetPort targets;
    private final RecoveryEvidencePort evidence;
    private final BusinessAuditRecorder business;
    private final SecurityAuditRecorder security;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public RecoveryPermitService(RecoveryPermitRepository permits, RecoveryConsumptionRepository consumptions,
            RecoveryIdentityPort identities, RecoveryScopePort scopes, RecoveryTtlPolicyPort policy,
            RecoveryTargetPort targets, RecoveryEvidencePort evidence, BusinessAuditRecorder business,
            SecurityAuditRecorder security, Clock clock, PlatformTransactionManager manager) {
        this.permits = permits; this.consumptions = consumptions; this.identities = identities;
        this.scopes = scopes; this.policy = policy; this.targets = targets; this.evidence = evidence;
        this.business = business; this.security = security; this.clock = clock;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(10);
    }

    public UUID issue(RecoveryTarget target, String reasonCode) {
        return execute(() -> {
            UUID actor = authorize("ISSUE", target.environmentId(), target.publicationId());
            requireCurrentTarget(target);
            Duration ttl = policy.durationFor(target).orElseThrow(() -> hold("TTL policy unavailable"));
            if (ttl.isNegative() || ttl.isZero()) throw hold("TTL policy invalid");
            requireSafeReference(reasonCode, 128);
            var issued = clock.instant();
            var permit = RecoveryPermit.issue(UUID.randomUUID(), target.environmentId(), target.publicationId(),
                    target.eventId(), target.listenerId(), target.expectedAttempt(), actor, reasonCode, issued, issued.plus(ttl));
            permits.insert(permit);
            permits.flush();
            audit("PERMIT_ISSUED", actor, permit.permitId());
            return permit.permitId();
        });
    }

    public void consume(UUID permitId, RecoveryTarget target, UUID operationId, String workerGeneration) {
        execute(() -> {
            UUID actor = authorize("EXECUTE", target.environmentId(), target.publicationId());
            RecoveryPermit permit = locked(permitId, target);
            // The initial boundary has no worker authentication/delegation entrypoint.
            if (!permit.actorId().equals(actor)) throw hold("Issuer execution identity mismatch");
            requireCurrentTarget(target);
            var proof = evidence.consumption(permitId, target, operationId, workerGeneration)
                    .orElseThrow(() -> hold("Consumption evidence unavailable"));
            if (!permitId.equals(proof.permitId()) || !target.equals(proof.target())
                    || !operationId.equals(proof.operationId()) || !workerGeneration.equals(proof.workerGeneration())) {
                throw hold("Consumption evidence mismatch");
            }
            requireSafeReference(proof.evidenceRef(), 255);
            var now = clock.instant();
            permit.requireConsumable(target.environmentId(), target.publicationId(), target.eventId(),
                    target.listenerId(), target.expectedAttempt(), now);
            if (consumptions.findByPermitId(permitId).isPresent()) throw hold("Permit already consumed");
            permits.advanceVersion(permit);
            consumptions.insert(RecoveryConsumption.record(permitId, operationId, workerGeneration, now));
            permits.flush();
            audit("PERMIT_CONSUMED", permit.actorId(), permitId);
            return Boolean.TRUE;
        });
    }

    public void close(UUID permitId, RecoveryTarget target, String resultRef) {
        execute(() -> {
            UUID actor = authorize("CLOSE", target.environmentId(), target.publicationId());
            RecoveryPermit permit = locked(permitId, target);
            requireCurrentTarget(target);
            var proof = evidence.closure(permitId, target, actor, resultRef)
                    .orElseThrow(() -> hold("Closure evidence unavailable"));
            if (!permitId.equals(proof.permitId()) || !target.equals(proof.target())
                    || !actor.equals(proof.confirmedBy()) || !resultRef.equals(proof.resultRef())) {
                throw hold("Closure evidence mismatch");
            }
            requireSafeReference(proof.resultRef(), 255);
            // Match all target dimensions even when closing an expired permit.
            requireStoredTarget(permit, target);
            permit.close(clock.instant(), actor, proof.resultRef());
            permits.flush();
            audit("PERMIT_CLOSED", actor, permitId);
            return Boolean.TRUE;
        });
    }

    public RecoveryPermitView read(UUID permitId, String environmentId, UUID publicationId) {
        return execute(() -> {
            authorize("READ", environmentId, publicationId);
            var permit = permits.findScoped(permitId, environmentId, publicationId)
                    .orElseThrow(() -> hold("Record unavailable"));
            var consumption = consumptions.findByPermitId(permitId);
            return new RecoveryPermitView(permitId,
                    new RecoveryTarget(permit.environmentId(), permit.publicationId(), permit.eventId(),
                            permit.listenerId(), permit.expectedAttempt()), permit.actorId(), permit.issuedAt(), permit.expiresAt(),
                    permit.closedAt(), permit.confirmedBy(), permit.resultRef(), Objects.requireNonNull(permit.version()),
                    consumption.map(RecoveryConsumption::operationId).orElse(null),
                    consumption.map(RecoveryConsumption::workerGeneration).orElse(null),
                    consumption.map(RecoveryConsumption::consumedAt).orElse(null));
        });
    }

    private UUID authorize(String action, String environment, UUID publication) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) {
            throw new Denied(AuditActor.anonymous(), "ACCESS_DENIED");
        }
        org.koikifw.identity.IdentityUser user;
        try {
            user = identities.current(principal.userId())
                    .orElseThrow(() -> new Denied(AuditActor.anonymous(), "ACCESS_DENIED"));
        } catch (Denied denied) {
            throw denied;
        } catch (RuntimeException failure) {
            throw new Denied(AuditActor.anonymous(), "AUTHORIZATION_UNAVAILABLE");
        }
        var actor = AuditActor.user(user.userId().toString());
        String capability = "NOTIFICATION:PERMIT:" + action;
        if (user.status() != UserStatus.ACTIVE || !user.permissionCodes().contains(capability)) {
            throw new Denied(actor, "ACCESS_DENIED");
        }
        RecoveryScopePort.Decision scope;
        try { scope = scopes.check(user.userId().value(), capability, environment, publication); }
        catch (RuntimeException failure) { throw new Denied(actor, "AUTHORIZATION_UNAVAILABLE"); }
        if (scope != RecoveryScopePort.Decision.ALLOWED) {
            throw new Denied(actor, scope == RecoveryScopePort.Decision.OUTSIDE ? "ACCESS_DENIED" : "AUTHORIZATION_UNAVAILABLE");
        }
        return user.userId().value();
    }

    private RecoveryPermit locked(UUID id, RecoveryTarget target) {
        return permits.lockScoped(id, target.environmentId(), target.publicationId())
                .orElseThrow(() -> hold("Record unavailable"));
    }

    private void requireCurrentTarget(RecoveryTarget target) {
        if (!targets.current(target.environmentId(), target.publicationId()).filter(target::equals).isPresent()) {
            throw hold("Current target unavailable or mismatched");
        }
    }

    private void requireStoredTarget(RecoveryPermit permit, RecoveryTarget target) {
        if (!permit.environmentId().equals(target.environmentId()) || !permit.publicationId().equals(target.publicationId())
                || !permit.eventId().equals(target.eventId()) || !permit.listenerId().equals(target.listenerId())
                || permit.expectedAttempt() != target.expectedAttempt()) throw hold("Stored target mismatch");
    }

    private void audit(String action, UUID actor, UUID permit) {
        business.record(AuditEvent.of("NOTIFICATION_RECOVERY_CHANGE", AuditActor.user(actor.toString()), action, AuditResult.SUCCESS)
                .withResource("NOTIFICATION_RECOVERY_PERMIT", permit.toString()));
    }

    private <T> T execute(Supplier<T> operation) {
        // Complete the owning transaction before denial Audit. Do not suspend caller-held locks.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Recovery operation requires a transaction-free application boundary");
        }
        try {
            return Objects.requireNonNull(transaction.execute(status -> operation.get()));
        } catch (Denied denied) {
            try {
                security.record(AuditEvent.of("NOTIFICATION_RECOVERY_ACCESS", denied.actor, "PERMIT_ACCESS_DENIED", AuditResult.FAILURE)
                        .withReason(denied.reason));
            } catch (RuntimeException failure) {
                logger.error("Notification recovery access denied; Security Audit unavailable");
            }
            throw new IllegalStateException("Recovery access denied");
        } catch (RuntimeException failure) {
            // The transaction has ended. Keep storage/domain/provider details out of callers.
            // Technical conflict and missing proof do not become Security incidents or retries.
            throw hold("Persistence or verification failed");
        }
    }

    private static IllegalStateException hold(String reason) {
        // Fixed internal classifications only; no target, actor, payload or exception message is logged.
        logger.warn("Notification recovery HOLD: {}", reason);
        return new IllegalStateException("Recovery operation unavailable");
    }

    private static void requireSafeReference(String value, int maximum) {
        if (value.isBlank() || value.length() > maximum || value.codePoints().anyMatch(Character::isISOControl)) {
            throw hold("Invalid code or opaque reference");
        }
    }

    private static final class Denied extends RuntimeException {
        private final AuditActor actor;
        private final String reason;
        private Denied(AuditActor actor, String reason) { this.actor = actor; this.reason = reason; }
    }
}

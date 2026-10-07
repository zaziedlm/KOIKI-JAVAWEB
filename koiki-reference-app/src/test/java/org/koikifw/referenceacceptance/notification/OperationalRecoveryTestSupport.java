package org.koikifw.referenceacceptance.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.port.outbound.*;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Process-local test model only: no platform, provider, durable store or delivery guarantee. */
public final class OperationalRecoveryTestSupport {
    public static final Instant FROM = NotificationFoundationDbHarness.NOW;
    public static final UUID USER = NotificationFoundationDbHarness.USER;
    public static final String WORKER = "test-recovery-generation-1";
    private OperationalRecoveryTestSupport() { }

    public enum Acceptance { NOT_ACCEPTED, REJECTED, ACCEPTED, UNKNOWN, CONTRADICTORY }
    public record Window(Instant from, Instant until) {
        public boolean active(Instant now) { return !now.isBefore(from) && now.isBefore(until); }
    }
    public record Observation(RecoveryTarget target, long revision, boolean eligible, Window window) { }
    public record StopReceipt(RecoveryTarget target, String process, String generation, String deploy,
            String issuer, long controlGeneration, boolean stopped, boolean drained, Window window) { }
    public record ProviderReceipt(String key, String payload, String recipient, Acceptance acceptance,
            UUID acceptanceId, Window window, Instant keyUntil) { }
    public record EvidenceKey(String environment, UUID permit, UUID operation, String worker) { }
    public record Body(EvidenceKey key, RecoveryTarget target, long revision, StopReceipt stop,
            ProviderReceipt provider, String reference, Window window) { }
    public record Resolution(UUID permit, RecoveryTarget target, UUID actor, String reference,
            String reason, ProviderReceipt provider, Window window) { }

    public static Window window() { return new Window(FROM, FROM.plusSeconds(60)); }
    public static RecoveryTarget target() { return ManagedRecoveryTestSupport.target(); }

    public static final class Model implements RecoveryTargetPort, RecoveryEvidencePort {
        public final Clock clock;
        public final String expectedNotificationKey = "test-logical-notification";
        public final String expectedPayload = "test-payload-1", expectedRecipient = "test-recipient";
        public volatile Observation observed;
        public volatile long expectedRevision = 1;
        public volatile boolean sourceAvailable = true;
        public volatile boolean processStopped = true, drained = true, transferred, restarted, controlAvailable = true;
        public volatile boolean recoveryLockHeld; // Independent observation; never used as stop evidence.
        public volatile String process = "test-process", generation = "test-start-1", deploy = "test-deploy-1", issuer = "test-platform";
        public volatile long controlGeneration = 1;
        public volatile ProviderReceipt provider;
        public volatile boolean providerAvailable = true, storeAvailable = true, saveFails, ambiguous;
        public volatile boolean transactionObserved;
        public volatile Runnable beforeEvidence = () -> { }, afterEvidence = () -> { };
        public final Map<EvidenceKey, Body> bodies = new ConcurrentHashMap<>();
        private final Map<EvidenceKey, Body> sealed = new ConcurrentHashMap<>();
        public final Set<EvidenceKey> revoked = ConcurrentHashMap.newKeySet();
        private final Map<String, Resolution> resolutions = new ConcurrentHashMap<>();
        private final Map<String, ProviderReceipt> accepted = new ConcurrentHashMap<>();

        public Model(Clock clock, RecoveryTarget target) {
            this.clock = clock;
            observed = new Observation(target, 1, true, window());
            provider = receipt("test-logical-notification", "test-payload-1", "test-recipient", Acceptance.NOT_ACCEPTED);
        }
        public ProviderReceipt receipt(String key, String payload, String recipient, Acceptance state) {
            return new ProviderReceipt(key, payload, recipient, state, UUID.randomUUID(), window(), FROM.plusSeconds(600));
        }
        public synchronized ProviderReceipt accept(String key, String payload, String recipient) {
            var previous = accepted.get(key);
            if (previous != null) {
                if (!previous.payload().equals(payload) || !previous.recipient().equals(recipient)
                        || !clock.instant().isBefore(previous.keyUntil())) throw new IllegalStateException("Test key conflict");
                return previous;
            }
            var result = receipt(key, payload, recipient, Acceptance.ACCEPTED);
            accepted.put(key, result);
            return result;
        }
        @Override public Optional<RecoveryTarget> current(String environment, UUID publication) {
            var value = observed;
            return sourceAvailable && value != null && value.eligible() && value.revision() == expectedRevision
                    && value.window().active(clock.instant()) && value.target().environmentId().equals(environment)
                    && value.target().publicationId().equals(publication) ? Optional.of(value.target()) : Optional.empty();
        }
        public StopReceipt stop(RecoveryTarget target) {
            return new StopReceipt(target, process, generation, deploy, issuer, controlGeneration,
                    processStopped, drained, window());
        }
        public boolean validStop(StopReceipt value, RecoveryTarget target) {
            return controlAvailable && !transferred && !restarted && processStopped && drained
                    && value.stopped() && value.drained() && value.target().equals(target)
                    && value.process().equals(process) && value.generation().equals(generation)
                    && value.deploy().equals(deploy) && value.issuer().equals(issuer)
                    && value.controlGeneration() == controlGeneration && value.window().active(clock.instant());
        }
        public boolean validProvider(ProviderReceipt value) {
            return providerAvailable && value.equals(provider) && value.key().equals(expectedNotificationKey)
                    && value.payload().equals(expectedPayload) && value.recipient().equals(expectedRecipient)
                    && value.window().active(clock.instant())
                    && clock.instant().isBefore(value.keyUntil());
        }
        public boolean mayRetry(ProviderReceipt value) {
            return validProvider(value) && !accepted.containsKey(value.key())
                    && (value.acceptance() == Acceptance.NOT_ACCEPTED || value.acceptance() == Acceptance.REJECTED);
        }
        public Body collect(UUID permit, RecoveryTarget target, UUID operation, String worker) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Test acquisition inside transaction");
            return new Body(new EvidenceKey(target.environmentId(), permit, operation, worker), target,
                    expectedRevision, stop(target), provider, "test-evidence-" + UUID.randomUUID(), window());
        }
        public synchronized Body save(Body body) {
            if (TransactionSynchronizationManager.isActualTransactionActive() || saveFails || !storeAvailable)
                throw new IllegalStateException("Test evidence storage unavailable");
            var previous = sealed.get(body.key());
            if (previous != null && !previous.equals(body)) throw new IllegalStateException("Test evidence replacement");
            sealed.putIfAbsent(body.key(), body);
            bodies.putIfAbsent(body.key(), body);
            return sealed.get(body.key());
        }
        public Optional<Body> resolve(EvidenceKey key) {
            var body = bodies.get(key);
            return storeAvailable && !ambiguous && body != null && body.equals(sealed.get(key))
                    ? Optional.of(body) : Optional.empty();
        }
        public boolean usable(Body body) {
            return resolve(body.key()).filter(body::equals).isPresent() && !revoked.contains(body.key())
                    && body.window().active(clock.instant()) && body.revision() == expectedRevision
                    && current(body.target().environmentId(), body.target().publicationId()).filter(body.target()::equals).isPresent()
                    && validStop(body.stop(), body.target()) && mayRetry(body.provider());
        }
        @Override public Optional<ConsumptionProof> consumption(UUID permit, RecoveryTarget target, UUID operation, String worker) {
            transactionObserved = TransactionSynchronizationManager.isActualTransactionActive();
            beforeEvidence.run();
            var result = resolve(new EvidenceKey(target.environmentId(), permit, operation, worker))
                    .filter(body -> body.target().equals(target)).filter(this::usable)
                    .map(body -> new ConsumptionProof(permit, target, operation, worker, body.reference()));
            afterEvidence.run(); // Test hook exposes the gap after the final successful verification.
            return result;
        }
        public void saveResolution(Resolution resolution) {
            if (TransactionSynchronizationManager.isActualTransactionActive() || !storeAvailable || saveFails)
                throw new IllegalStateException("Test resolution storage unavailable");
            var previous = resolutions.putIfAbsent(resolution.reference(), resolution);
            if (previous != null && !previous.equals(resolution)) throw new IllegalStateException("Test resolution replacement");
        }
        @Override public Optional<ClosureProof> closure(UUID permit, RecoveryTarget target, UUID actor, String result) {
            var value = resolutions.get(result);
            return storeAvailable && !ambiguous && value != null && value.permit().equals(permit)
                    && value.target().equals(target) && value.actor().equals(actor) && !value.reason().isBlank()
                    && value.window().active(clock.instant()) && validProvider(value.provider())
                    && value.provider().acceptance() == Acceptance.ACCEPTED
                    ? Optional.of(new ClosureProof(permit, target, actor, result)) : Optional.empty();
        }
        public boolean executionCandidate(Body body, boolean consumptionCommitKnown) {
            return consumptionCommitKnown && usable(body);
        }
    }

    /** Same production service, with only target/evidence replaced; real ports, repositories and Recorders retained. */
    public static RecoveryPermitService service(ConfigurableApplicationContext context, Model model) {
        return new RecoveryPermitService(
                context.getBean(org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository.class),
                context.getBean(org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository.class),
                context.getBean(RecoveryIdentityPort.class), context.getBean(RecoveryScopePort.class),
                context.getBean(RecoveryTtlPolicyPort.class), model, model,
                context.getBean(org.koikifw.audit.BusinessAuditRecorder.class),
                context.getBean(org.koikifw.audit.SecurityAuditRecorder.class), model.clock,
                context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
    }
}

package org.koikifw.buildsupport.phase4.b1fixture;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** B-1 observations only. No value in this contract authorizes delivery or permit use. */
public final class B1ReadContract {
    public static final String ENVIRONMENT = "b1-local";
    public static final String SOURCE = "pl2-tooling-23379b3";
    public static final String EVENT_TYPE = "org.koikifw.buildsupport.phase4.ProbeApproved";
    public static final String LISTENER = "org.koikifw.buildsupport.phase4.NotificationProbe.on(" + EVENT_TYPE + ")";
    public enum Provider { ACCEPTED, UNKNOWN, FIXTURE_NOT_ACCEPTED }
    public record Target(String environment, UUID publication, UUID event, String eventType,
                         String listener, int attempt) {
        public Target {
            Objects.requireNonNull(environment); Objects.requireNonNull(publication);
            Objects.requireNonNull(event); Objects.requireNonNull(eventType); Objects.requireNonNull(listener);
            if (attempt < 0) throw new IllegalArgumentException("invalid attempt");
        }
    }
    /** Established before event creation; the generated publication is sealed separately once. */
    public record Manifest(UUID run, UUID event, String key, String payload, String recipient,
                           String source, String listener, String jarHash, Instant keyUntil) { }
    public record Stop(UUID run, long generation, String source, boolean allEnded,
                       boolean forced, boolean controlled, Instant observed, Instant until) {
        public boolean matches(UUID expectedRun, long expectedGeneration, String expectedSource, Instant now) {
            return run.equals(expectedRun) && generation == expectedGeneration && source.equals(expectedSource)
                    && allEnded && controlled && !now.isBefore(observed) && now.isBefore(until);
        }
    }
    public record Snapshot(Target target, UUID run, long revision, String source, String sourceFingerprint,
                           String key, String payload, String recipient, String status, Provider provider,
                           Long receipt, Stop stop, Instant observed, Instant until, Instant keyUntil) {
        public String canonical() {
            return fields("b1-v1", target.environment(), target.publication(), target.event(), target.eventType(),
                    target.listener(), target.attempt(), run, revision, source, sourceFingerprint, key, payload,
                    recipient, status, provider, receipt, stop.run(), stop.generation(), stop.source(),
                    stop.allEnded(), stop.forced(), stop.controlled(), stop.observed(), stop.until(),
                    observed, until, keyUntil);
        }
        public boolean validAt(Instant now) {
            return !now.isBefore(observed) && now.isBefore(until) && now.isBefore(keyUntil);
        }
    }
    public record EvidenceKey(String environment, UUID permit, UUID operation, long workerGeneration) { }
    public record Evidence(UUID reference, EvidenceKey key, UUID run, String subject,
                           String canonical, Instant until) { }
    public record Read(boolean observed, Provider provider, String reason, Snapshot snapshot) {
        public static Read reject(String reason) { return new Read(false, Provider.UNKNOWN, reason, null); }
    }
    public static String fields(Object... values) {
        StringBuilder result = new StringBuilder();
        for (Object value : values) {
            String text = value == null ? "<null>" : value.toString();
            result.append(text.length()).append(':').append(text);
        }
        return result.toString();
    }
    public static String digest(String text) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private B1ReadContract() { }
}

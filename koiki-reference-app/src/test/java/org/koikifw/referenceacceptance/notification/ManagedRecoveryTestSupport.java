package org.koikifw.referenceacceptance.notification;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoveryConfigurationLoader;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoverySnapshot;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.reference.notification.configuration.ManagedRecoveryConfiguration;

/** Finite test data; never included in the packaged Reference. */
public final class ManagedRecoveryTestSupport {
    public static final Instant FROM = NotificationFoundationDbHarness.NOW;
    public static final UUID USER = NotificationFoundationDbHarness.USER;
    public static final UUID PUBLICATION = UUID.fromString("75000000-0000-0000-0000-000000000001");
    public static final String ENV = "test-environment";
    private ManagedRecoveryTestSupport() { }
    public static RecoveryTarget target() { return new RecoveryTarget(ENV, PUBLICATION, UUID.randomUUID(), "test-listener", 0); }
    public static String grant(String capability) {
        return "{\"userId\":\"" + USER + "\",\"capability\":\"NOTIFICATION:PERMIT:" + capability
                + "\",\"publicationId\":\"" + PUBLICATION + "\"}";
    }
    public static String json(String grants) {
        return "{\"schemaVersion\":1,\"revision\":\"test-r1\",\"environmentId\":\"" + ENV
                + "\",\"validFrom\":\"" + FROM + "\",\"validUntil\":\"" + FROM.plusSeconds(1800)
                + "\",\"permitTtl\":\"PT5M\",\"grants\":[" + grants + "]}";
    }
    public static Path file(Path directory, String json) throws Exception {
        return Files.writeString(directory.resolve("assignment.json"), json, StandardCharsets.UTF_8);
    }
    public static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    public static ManagedRecoverySnapshot load(Path path) throws Exception {
        return new ManagedRecoveryConfigurationLoader().load(path, hash(path), "test-r1", ENV);
    }
    public static ManagedRecoverySnapshot snapshot(Duration ttl) {
        return new ManagedRecoverySnapshot("test-r1", "a".repeat(64), ENV, FROM, FROM.plusSeconds(1800), ttl,
                List.of(new ManagedRecoverySnapshot.Grant(USER, "NOTIFICATION:PERMIT:ISSUE", PUBLICATION)));
    }
    public static Map<String, Object> properties(Path path) throws Exception {
        String prefix = ManagedRecoveryConfiguration.PREFIX;
        return Map.of(prefix + "mode", "file", prefix + "path", path.toString(), prefix + "expected-sha256", hash(path),
                prefix + "expected-revision", "test-r1", prefix + "environment-id", ENV);
    }

    /** Imports the real, package-private persistence configurations without copying EntityScan declarations. */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.context.annotation.ComponentScan(
            basePackages = {"org.koikifw.reference.master.adapter.outbound.persistence", "org.koikifw.reference.expense.adapter.outbound.persistence"},
            useDefaultFilters = false,
            includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                    type = org.springframework.context.annotation.FilterType.ANNOTATION, classes = org.springframework.context.annotation.Configuration.class))
    public static class ExistingPersistence { }
}

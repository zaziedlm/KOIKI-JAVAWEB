package org.koikifw.referenceacceptance.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTargetPort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.reference.notification.configuration.NotificationFoundationConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Test-owned finite ports only. IdentityQuery, Recorders, JPA and transactions stay real. */
public final class NotificationFoundationDbHarness implements AutoCloseable {
    public enum Mode { PERMIT, CONSUMER, READER }
    public static final UUID USER = UUID.fromString("74000000-0000-0000-0000-000000000001");
    public static final UUID ROLE = UUID.fromString("74000000-0000-0000-0000-000000000002");
    public static final List<String> CAPABILITIES = List.of("ISSUE", "READ", "EXECUTE", "CLOSE");
    public static final Instant NOW = Instant.parse("2026-10-07T00:00:00.123456789Z");
    public final MutableClock clock = new MutableClock();
    public final FinitePorts ports = new FinitePorts();
    private final PostgreSQLContainer postgres;
    private final String url;
    private final Map<Mode, String> passwords = new HashMap<>();
    private final Map<Mode, ConfigurableApplicationContext> contexts = new HashMap<>();

    public static PostgreSQLContainer database() {
        return new PostgreSQLContainer("postgres:17-alpine").withPassword(UUID.randomUUID().toString())
                .withImagePullPolicy(image -> false).withStartupTimeout(Duration.ofSeconds(60))
                .withCommand("postgres", "-c", "max_connections=16", "-c", "lock_timeout=10s",
                        "-c", "statement_timeout=10s", "-c", "transaction_timeout=10s")
                .withCreateContainerCmdModifier(command -> Objects.requireNonNull(command.getHostConfig())
                        .withMemory(1073741824L).withNanoCPUs(1000000000L));
    }

    public NotificationFoundationDbHarness(PostgreSQLContainer postgres) throws Exception {
        this.postgres = postgres;
        assertThat(System.getProperty("koiki.reference.verification.resource-limits.enabled")).isEqualTo("true");
        assertThat(Runtime.getRuntime().maxMemory()).isEqualTo(805306368L);
        assertThat(postgres.getContainerInfo().getHostConfig().getMemory()).isEqualTo(1073741824L);
        assertThat(postgres.getContainerInfo().getHostConfig().getNanoCPUs()).isEqualTo(1000000000L);
        String name = "s1_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) { statement.execute("CREATE DATABASE " + name); }
        url = postgres.getJdbcUrl().replace("/test", "/" + name);
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration/koiki").table("koiki_flyway_history").load().migrate();
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration/kkref", "classpath:db/migration/kkref-notification")
                .table("kkref_flyway_history").baselineOnMigrate(true).baselineVersion("0").load().migrate();
        sql("CREATE ROLE kkref_notification_owner NOLOGIN NOINHERIT");
        for (Mode mode : Mode.values()) {
            String password = UUID.randomUUID().toString();
            passwords.put(mode, password);
            sql("CREATE ROLE " + role(mode) + " LOGIN NOINHERIT PASSWORD '" + password + "'");
        }
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/notification/s1-isolated-grants.sql"))) {
            sql(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThat(rows("SHOW lock_timeout")).containsExactly("10s");
        assertThat(rows("SHOW statement_timeout")).containsExactly("10s");
        assertThat(rows("SHOW transaction_timeout")).containsExactly("10s");
        assertThat(rows("SHOW max_connections")).containsExactly("16");
        assertThat(rows("SELECT count(*) FROM pg_auth_members WHERE member IN (SELECT oid FROM pg_roles WHERE rolname LIKE 'kkref_notification_%')"))
                .containsExactly("0");
        assertThat(rows("SELECT count(*) FROM pg_roles WHERE rolname LIKE 'kkref_notification_%' AND (rolinherit OR rolsuper OR rolcreatedb OR rolcreaterole)"))
                .containsExactly("0");
        System.out.println("S1_RESOURCE dbId=" + postgres.getContainerId()
                + " memory=1073741824 nanoCpus=1000000000 maxConnections=16 poolBudget=8 heap=805306368");
    }

    public static String role(Mode mode) { return "kkref_notification_" + mode.name().toLowerCase(java.util.Locale.ROOT); }

    public ConfigurableApplicationContext context(Mode mode) {
        return contexts.computeIfAbsent(mode, key -> open(mode, true));
    }

    public ConfigurableApplicationContext open(Mode mode, boolean finitePorts) {
        return open(mode, finitePorts, Map.of(), false);
    }

    public ConfigurableApplicationContext open(Mode mode, boolean finitePorts, Map<String, Object> overrides, boolean finiteEvidence,
            Class<?>... extraSources) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", role(mode));
        properties.put("spring.datasource.password", passwords.get(mode));
        properties.put("spring.datasource.hikari.maximum-pool-size", "2");
        properties.put("spring.datasource.hikari.minimum-idle", "0");
        properties.put("spring.datasource.hikari.connection-timeout", "10000");
        properties.put("spring.main.banner-mode", "off");
        properties.putAll(overrides);
        var builder = new SpringApplicationBuilder(Bootstrap.class).sources(extraSources).properties(properties);
        builder.initializers(context -> {
            context.getBeanFactory().registerSingleton("s1TestClock", clock);
            if (finitePorts) context.getBeanFactory().registerSingleton("s1FinitePorts", ports);
            if (finiteEvidence) {
                context.getBeanFactory().registerSingleton("s1ManagedTestTargets", (RecoveryTargetPort) ports::current);
                context.getBeanFactory().registerSingleton("s1ManagedTestEvidence", new RecoveryEvidencePort() {
                    @Override public Optional<ConsumptionProof> consumption(UUID permit, RecoveryTarget target, UUID operation, String worker) {
                        return ports.consumption(permit, target, operation, worker);
                    }
                    @Override public Optional<ClosureProof> closure(UUID permit, RecoveryTarget target, UUID actor, String result) {
                        return ports.closure(permit, target, actor, result);
                    }
                });
            }
        });
        var context = builder.run("--spring.main.web-application-type=none", "--spring.flyway.enabled=false",
                "--koiki.data.flyway.enabled=false", "--koiki.identity.local-authentication.enabled=false",
                "--koiki.reference.notification.foundation.enabled=true", "--spring.session.jdbc.initialize-schema=never",
                "--spring.session.jdbc.table-name=koiki_session", "--spring.session.jdbc.cleanup-cron=-",
                "--server.servlet.session.cookie.http-only=true");
        var pool = context.getBean(HikariDataSource.class);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(2);
        assertThat(pool.getMinimumIdle()).isZero();
        assertThat(pool.getConnectionTimeout()).isEqualTo(10000);
        return context;
    }

    public RecoveryPermitService service(Mode mode) { return context(mode).getBean(RecoveryPermitService.class); }

    public Connection admin() throws Exception { return DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword()); }
    public Connection runtime(Mode mode) throws Exception { return DriverManager.getConnection(url, role(mode), passwords.get(mode)); }
    public void sql(String query) throws Exception {
        try (var connection = admin(); var statement = connection.createStatement()) { statement.execute(query); }
    }
    public List<String> rows(String query) throws Exception {
        try (var connection = admin(); var statement = connection.createStatement(); var result = statement.executeQuery(query)) {
            List<String> values = new ArrayList<>();
            while (result.next()) {
                List<String> fields = new ArrayList<>();
                for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) fields.add(String.valueOf(result.getObject(i)));
                values.add(String.join("|", fields));
            }
            return values;
        }
    }

    public <T> T tx(Mode mode, Supplier<T> operation) {
        var template = new TransactionTemplate(context(mode).getBean(PlatformTransactionManager.class));
        template.setTimeout(10);
        return template.execute(status -> operation.get());
    }

    public void reset() throws Exception {
        clock.now = NOW; ports.decision = RecoveryScopePort.Decision.ALLOWED;
        ports.ttl = Optional.of(Duration.ofMinutes(1)); ports.snapshots.clear();
        ports.consumptionProofs.clear(); ports.closureProofs.clear();
        sql("TRUNCATE kkref_notification_recovery_consumption, kkref_notification_recovery_permit, koiki_audit_event");
        sql("DELETE FROM koiki_user_role; DELETE FROM koiki_role_permission; DELETE FROM koiki_permission; DELETE FROM koiki_role; DELETE FROM koiki_user");
        sql("INSERT INTO koiki_user(user_id,email,canonical_email,status) VALUES ('" + USER + "','s1@example.test','s1@example.test','ACTIVE')");
        sql("INSERT INTO koiki_role(role_id,role_code) VALUES ('" + ROLE + "','S1_TEST')");
        sql("INSERT INTO koiki_user_role VALUES ('" + USER + "','" + ROLE + "')");
        for (String capability : CAPABILITIES) {
            UUID id = UUID.randomUUID();
            sql("INSERT INTO koiki_permission(permission_id,permission_code) VALUES ('" + id + "','NOTIFICATION:PERMIT:" + capability + "');"
                    + "INSERT INTO koiki_role_permission VALUES ('" + ROLE + "','" + id + "')");
        }
        authenticate(USER);
    }

    public RecoveryTarget target() {
        RecoveryTarget target = new RecoveryTarget("test-environment", UUID.randomUUID(), UUID.randomUUID(), "test-listener", 0);
        ports.snapshots.put(target.publicationId(), target);
        return target;
    }

    public UUID issue(RecoveryTarget target) { return service(Mode.PERMIT).issue(target, "TEST_REASON"); }
    public void proof(UUID permit, RecoveryTarget target, UUID operation, String worker) {
        ports.consumptionProofs.put(permit, new RecoveryEvidencePort.ConsumptionProof(permit, target, operation, worker, "test-stopped-reconciled"));
    }
    public void closure(UUID permit, RecoveryTarget target, String reference) {
        ports.closureProofs.put(permit, new RecoveryEvidencePort.ClosureProof(permit, target, USER, reference));
    }
    public void removeCapability(String capability) throws Exception {
        sql("DELETE FROM koiki_role_permission WHERE permission_id IN (SELECT permission_id FROM koiki_permission WHERE permission_code='NOTIFICATION:PERMIT:" + capability + "')");
    }
    public static void authenticate(UUID user) {
        FrameworkPrincipal principal = new TestPrincipal(FrameworkUserId.parse(user.toString()));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, "unused", List.of()));
    }
    private record TestPrincipal(FrameworkUserId userId) implements FrameworkPrincipal {
        @Override public AuthenticationSource authenticationSource() { return AuthenticationSource.LOCAL; }
        // Deliberately stale claimed permissions: only the real current IdentityQuery must decide.
        @Override public Set<String> permissions() { return Set.of("NOTIFICATION:PERMIT:ISSUE", "NOTIFICATION:PERMIT:READ", "NOTIFICATION:PERMIT:EXECUTE", "NOTIFICATION:PERMIT:CLOSE"); }
    }

    @Override public void close() {
        contexts.values().forEach(ConfigurableApplicationContext::close);
        contexts.clear();
        SecurityContextHolder.clearContext();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(NotificationFoundationConfiguration.class)
    public static class Bootstrap { }

    public static final class MutableClock extends Clock {
        public volatile Instant now = NOW;
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    public static final class FinitePorts implements RecoveryScopePort, RecoveryTargetPort, RecoveryTtlPolicyPort, RecoveryEvidencePort {
        public volatile RecoveryScopePort.Decision decision = RecoveryScopePort.Decision.ALLOWED;
        public volatile Optional<Duration> ttl = Optional.of(Duration.ofMinutes(1));
        public final Map<UUID, RecoveryTarget> snapshots = new ConcurrentHashMap<>();
        public final Map<UUID, ConsumptionProof> consumptionProofs = new ConcurrentHashMap<>();
        public final Map<UUID, ClosureProof> closureProofs = new ConcurrentHashMap<>();
        @Override public Decision check(UUID user, String capability, String environment, UUID publication) { return decision; }
        @Override public Optional<Duration> durationFor(RecoveryTarget target) { return ttl; }
        @Override public boolean allowsIssuanceAt(RecoveryTarget target, Instant issued, Instant expires) {
            return snapshots.containsKey(target.publicationId()) && ttl.filter(value -> !value.isNegative() && !value.isZero())
                    .filter(value -> issued.plus(value).equals(expires)).isPresent();
        }
        @Override public Optional<RecoveryTarget> current(String environment, UUID publication) {
            return Optional.ofNullable(snapshots.get(publication)).filter(target -> target.environmentId().equals(environment));
        }
        @Override public Optional<ConsumptionProof> consumption(UUID permit, RecoveryTarget target, UUID operation, String worker) {
            return Optional.ofNullable(consumptionProofs.get(permit));
        }
        @Override public Optional<ClosureProof> closure(UUID permit, RecoveryTarget target, UUID actor, String reference) {
            return Optional.ofNullable(closureProofs.get(permit));
        }
    }
}

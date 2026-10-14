package org.koikifw.buildsupport.phase4.s1fixture;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.koikifw.audit.*;
import org.koikifw.identity.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** B-only, explicit non-Web auto-configuration; no internal Framework imports or real sender. */
public final class S1IdentityAuditFixture implements AutoCloseable {
    public static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000301");
    public static final UUID PUBLICATION = UUID.fromString("00000000-0000-0000-0000-000000000100");
    public static final UUID EVENT = UUID.fromString("00000000-0000-0000-0000-000000000200");
    public static final Instant ISSUED = Instant.parse("2026-10-06T00:00:00Z");
    public static final Instant EXPIRES = ISSUED.plusSeconds(1800);
    private final ConfigurableApplicationContext context;
    private final EntityManagerFactory emf;
    private final TransactionTemplate tx;
    public final IdentityQuery identity;
    public final BusinessAuditRecorder business;
    public final SecurityAuditRecorder security;
    public final MutableClock clock;
    public final SqlCapture capture;
    public int sends;
    public int auditFailures;

    public S1IdentityAuditFixture(String url, String role) {
        var app = new SpringApplication(Configuration.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        context = app.run("--spring.datasource.url=" + url,
                "--spring.datasource.username=" + role, "--spring.datasource.password=s1-fixture-only",
                "--spring.datasource.hikari.maximum-pool-size=4", "--spring.datasource.hikari.minimum-idle=0",
                "--spring.datasource.hikari.connection-timeout=10000",
                "--spring.datasource.hikari.connection-init-sql=SET statement_timeout = '10s'",
                "--spring.jpa.hibernate.ddl-auto=validate", "--spring.jpa.open-in-view=false",
                "--spring.jpa.mapping-resources=s1-additional/orm-identity-audit.xml",
                "--spring.flyway.enabled=false", "--spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "--spring.modulith.republish-outstanding-events-on-restart=false",
                "--spring.autoconfigure.exclude=org.springframework.modulith.events.config.EventPublicationAutoConfiguration,org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration",
                "--spring.main.banner-mode=off");
        try {
            emf = context.getBean(EntityManagerFactory.class);
            tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            tx.setTimeout(10);
            identity = context.getBean(IdentityQuery.class);
            business = context.getBean(BusinessAuditRecorder.class);
            security = context.getBean(SecurityAuditRecorder.class);
            clock = context.getBean(MutableClock.class);
            capture = context.getBean(SqlCapture.class);
            capture.dataSource = context.getBean(DataSource.class);
            if (!context.getBeansOfType(IdentityAdministration.class).isEmpty()
                    || context.containsBean("eventPublicationRegistry")
                    || !context.getBeansOfType(org.springframework.scheduling.TaskScheduler.class).isEmpty()
                    || context.getBeansOfType(DataSource.class).size() != 1
                    || context.getBeansOfType(EntityManagerFactory.class).size() != 1
                    || context.getBeansOfType(PlatformTransactionManager.class).size() != 1) {
                throw new IllegalStateException("B requires one Spring JPA transaction stack and no administration");
            }
            if (context.getBean(HikariDataSource.class).getMaximumPoolSize() != 4) {
                throw new IllegalStateException("B pool ceiling must be 4");
            }
            transaction(() -> {
                if (!scalar("SELECT current_user").equals(role)
                        || !scalar("SELECT rolinherit::text FROM pg_roles WHERE rolname=current_user").equals("false")
                        || !scalar("SELECT has_table_privilege(current_user,'koiki_audit_event','UPDATE')::text").equals("false")
                        || !scalar("SELECT has_table_privilege(current_user,'koiki_audit_event','DELETE')::text").equals("false")
                        || !scalar("SELECT has_table_privilege(current_user,'koiki_user','UPDATE')::text").equals("false")
                        || !scalar("SELECT has_table_privilege(current_user,'koiki_password_credential','SELECT')::text").equals("false")) {
                    throw new IllegalStateException("B restricted role contract must hold");
                }
                return null;
            });
            System.out.println("S1-B beans=" + identity.getClass().getName() + ","
                    + business.getClass().getName() + "," + security.getClass().getName() + " role=" + role);
        } catch (RuntimeException | Error failure) { context.close(); throw failure; }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    public static class Configuration {
        @Bean MutableClock clock() { return new MutableClock(); }
        @Bean SqlCapture sqlCapture() { return new SqlCapture(); }
        @Bean org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer capture(SqlCapture capture) {
            return properties -> {
                properties.put("hibernate.session_factory.statement_inspector", capture);
                properties.put("hibernate.jdbc.time_zone", "UTC");
            };
        }
    }
    public static final class MutableClock extends Clock {
        private Instant now = ISSUED.plusSeconds(10);
        public void set(Instant now) { this.now = now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }
    public static final class SqlCapture implements StatementInspector {
        public final List<String> statements = new CopyOnWriteArrayList<>();
        DataSource dataSource;
        public String auditPid;
        public String auditTransaction;
        public int activeAtAudit;
        @Override public String inspect(String sql) {
            statements.add(sql); System.out.println("S1-B SQL " + sql);
            if (dataSource != null && sql.startsWith("insert into koiki_audit_event")) {
                var connection = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(dataSource);
                try (var statement = connection.createStatement();
                        var rows = statement.executeQuery("select pg_backend_pid(),txid_current()")) {
                    rows.next(); auditPid=rows.getString(1); auditTransaction=rows.getString(2);
                    activeAtAudit=((HikariDataSource)dataSource).getHikariPoolMXBean().getActiveConnections();
                    System.out.println("S1-B Audit connection="+auditPid+" transaction="+auditTransaction+" pool-active="+activeAtAudit);
                } catch (java.sql.SQLException failure) { throw new IllegalStateException(failure); }
                finally { org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(connection,dataSource); }
            }
            return sql;
        }
    }
    public <T> T transaction(Supplier<T> action) {
        return tx.execute(status -> {
            em().createNativeQuery("SET LOCAL lock_timeout = '10s'").executeUpdate();
            return action.get();
        });
    }
    public void rollback(Runnable action) {
        tx.executeWithoutResult(status -> { action.run(); status.setRollbackOnly(); });
    }
    public EntityManager em() { return Objects.requireNonNull(EntityManagerFactoryUtils.getTransactionalEntityManager(emf)); }
    public String scalar(String sql) { return em().createNativeQuery(sql).getSingleResult().toString(); }
    public List<String> sql() { return List.copyOf(capture.statements); }
    public AuditEvent businessEvent(UUID permit, String action) {
        return AuditEvent.of("S1_TEST_CHANGE", AuditActor.user(USER.toString()), action, AuditResult.SUCCESS)
                .withResource("S1_TEST_PERMIT", permit.toString());
    }
    private boolean permitted(UUID actor, String... codes) {
        var found = identity.findById(FrameworkUserId.parse(actor.toString()));
        return found.isPresent() && found.get().status() == UserStatus.ACTIVE
                && found.get().permissionCodes().containsAll(List.of(codes));
    }
    private boolean scope(UUID actor, String environment, UUID publication) {
        return actor.equals(USER) && environment.equals("fixture-env") && publication.equals(PUBLICATION);
    }
    public void issue(UUID id) {
        transaction(() -> { issueInside(id); return null; });
    }
    public void issueInside(UUID id) {
        if (!permitted(USER, "S1_TEST_ISSUE") || !scope(USER, "fixture-env", PUBLICATION)) throw new Denied();
        em().persist(new Permit(id)); em().flush();
        business.record(businessEvent(id, "S1_TEST_ISSUED"));
    }
    public void closeInside(UUID id) {
        if (!permitted(USER, "S1_TEST_CLOSE")) throw new Denied();
        var permit = readScoped(id, "fixture-env", PUBLICATION);
        em().lock(permit, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        permit.closed = clock.instant(); permit.confirmed = USER.toString(); permit.result = "fixture-close";
        em().flush(); business.record(businessEvent(id, "S1_TEST_CLOSED"));
    }
    public record Snapshot(String environment, UUID publication, UUID event, String listener, int attempt) {
        public static Snapshot matching() { return new Snapshot("fixture-env", PUBLICATION, EVENT, "fixture-listener", 1); }
    }
    public Permit readScoped(UUID id, String environment, UUID publication) {
        if (!scope(USER, environment, publication)) throw new Denied();
        if (!permitted(USER, "S1_TEST_READ")) throw new Denied();
        var rows = em().createQuery("select p from S1BPermit p where p.id=:id and p.environment=:env and p.publication=:pub", Permit.class)
                .setParameter("id", id).setParameter("env", environment).setParameter("pub", publication).getResultList();
        if (rows.size() != 1) throw new Denied();
        return rows.getFirst();
    }
    public void consumeInside(UUID id, Snapshot snapshot) {
        var permit = readScoped(id, snapshot.environment, snapshot.publication);
        if (!permitted(UUID.fromString(permit.actor), "S1_TEST_ISSUE", "S1_TEST_EXECUTE")) throw new Denied();
        em().lock(permit, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        if (permit.closed != null || !clock.instant().isBefore(permit.expires)
                || !permit.event.equals(snapshot.event) || !permit.listener.equals(snapshot.listener)
                || permit.attempt != snapshot.attempt || em().find(Consumption.class, id) != null) throw new Denied();
        em().persist(new Consumption(id, clock.instant())); em().flush();
        business.record(businessEvent(id, "S1_TEST_CONSUMED"));
    }
    public String execute(UUID id, Snapshot snapshot, Runnable afterCommit) {
        try { transaction(() -> { consumeInside(id, snapshot); return null; }); }
        catch (RuntimeException failure) { denyAudit(); return "DENIED"; }
        afterCommit.run();
        boolean valid;
        try {
            valid = transaction(() -> {
                var permit = readScoped(id, snapshot.environment, snapshot.publication);
                return permit.closed == null && clock.instant().isBefore(permit.expires)
                        && permitted(UUID.fromString(permit.actor), "S1_TEST_ISSUE", "S1_TEST_EXECUTE");
            });
        } catch (RuntimeException failure) { valid = false; }
        if (!valid) { denyAudit(); return "HOLD"; }
        sends++; return "SENT";
    }
    public boolean authorized() { return transaction(() -> permitted(USER, "S1_TEST_ISSUE", "S1_TEST_EXECUTE")); }
    public void denyAudit() {
        AuditActor actor;
        try { actor = identity.findById(FrameworkUserId.parse(USER.toString())).isPresent()
                ? AuditActor.user(USER.toString()) : AuditActor.anonymous(); }
        catch (RuntimeException failure) { actor = AuditActor.anonymous(); }
        try { security.record(AuditEvent.of("S1_TEST_DENIAL", actor, "S1_TEST_EXECUTE", AuditResult.FAILURE).withReason("S1_TEST_DENIED")); }
        catch (AuditRecordingException failure) { auditFailures++; }
    }
    public static final class Denied extends RuntimeException {
        public Denied() { super("S1_TEST_DENIED"); }
    }
    @Override public void close() { context.close(); }

    // XML registration only; never discovered by other test applications.
    @Table(name="permit", schema="s1b") @DynamicUpdate
    public static class Permit {
        @Id @Column(name="permit_id",updatable=false) UUID id;
        @Column(name="environment_id",updatable=false) String environment = "fixture-env";
        @Column(name="publication_id",updatable=false) UUID publication = PUBLICATION;
        @Column(name="event_id",updatable=false) UUID event = EVENT;
        @Column(name="listener_id",updatable=false) String listener = "fixture-listener";
        @Column(name="expected_attempt",updatable=false) int attempt = 1;
        @Column(name="actor_id",updatable=false) String actor = USER.toString();
        @Column(name="reason_code",updatable=false) String reason = "OWNER_REVIEW";
        @Column(name="issued_at",updatable=false) Instant issued = ISSUED;
        @Column(name="expires_at",updatable=false) Instant expires = EXPIRES;
        @Column(name="closed_at",insertable=false) Instant closed;
        @Column(name="confirmed_by",insertable=false) String confirmed;
        @Column(name="result_ref",insertable=false) String result;
        @Version @Column(name="version",insertable=false) long version;
        protected Permit() {}
        public Permit(UUID id) { this.id = id; }
    }
    @Table(name="consumption",schema="s1b")
    public static class Consumption {
        @Id @Column(name="permit_id") UUID permit;
        @Column(name="operation_id") UUID operation;
        @Column(name="worker_generation") String generation = "fixture-worker";
        @Column(name="consumed_at") Instant consumed;
        protected Consumption() {}
        public Consumption(UUID permit, Instant now) { this.permit=permit; operation=UUID.randomUUID(); consumed=now; }
    }
}

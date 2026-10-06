package org.koikifw.buildsupport.phase4.s1fixture;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Explicit, test-only JPA configuration. No Boot scanning, Flyway or real audit/provider. */
public final class S1JpaFixture implements AutoCloseable {
    public static final Instant ISSUED = Instant.parse("2026-10-06T00:00:00Z");
    public static final Instant EXPIRES = ISSUED.plusSeconds(1800);
    private final AnnotationConfigApplicationContext context;
    private final EntityManagerFactory factory;
    private final SqlCapture capture;

    public S1JpaFixture(String url, String role, boolean pooled) {
        capture = new SqlCapture(role);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(SqlCapture.class, () -> capture);
        if (pooled) {
            context.registerBean(HikariDataSource.class, () -> {
                var options = new HikariConfig();
                options.setJdbcUrl(url);
                options.setUsername(role);
                options.setPassword("s1-fixture-only");
                options.setPoolName(role);
                options.setMaximumPoolSize(2);
                options.setMinimumIdle(0);
                options.setConnectionTimeout(10_000);
                options.setConnectionInitSql("SET statement_timeout = '10s'");
                return new HikariDataSource(options);
            });
        } else {
            context.registerBean(DriverManagerDataSource.class,
                    () -> new DriverManagerDataSource(url, role, "s1-fixture-only"));
        }
        context.register(JpaConfiguration.class);
        try {
            context.refresh();
            factory = context.getBean(EntityManagerFactory.class);
        } catch (RuntimeException | Error failure) {
            context.close();
            throw failure;
        }
    }

    public Tx begin() { return new Tx(factory.createEntityManager()); }
    public List<String> sql() { return List.copyOf(capture.statements); }
    public void clearSql() { capture.statements.clear(); }
    @Override public void close() { context.close(); }

    @TestConfiguration(proxyBeanMethods = false)
    public static class JpaConfiguration {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, SqlCapture capture) {
            var bean = new LocalContainerEntityManagerFactoryBean();
            bean.setDataSource(dataSource);
            bean.setManagedTypes(PersistenceManagedTypes.of(Permit.class.getName(), Consumption.class.getName()));
            bean.setMappingResources("s1-additional/orm.xml");
            bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate",
                    "hibernate.session_factory.statement_inspector", capture,
                    "hibernate.jdbc.time_zone", "UTC"));
            return bean;
        }
    }

    public static final class SqlCapture implements StatementInspector {
        private final String role;
        private final CopyOnWriteArrayList<String> statements = new CopyOnWriteArrayList<>();
        SqlCapture(String role) { this.role = role; }
        @Override public String inspect(String sql) {
            statements.add(sql);
            System.out.println("S1-A SQL role=" + role + " " + sql);
            return sql;
        }
    }

    public static final class Tx implements AutoCloseable {
        private final EntityManager manager;
        private final EntityTransaction transaction;
        Tx(EntityManager manager) {
            this.manager = manager;
            transaction = manager.getTransaction();
            transaction.begin();
            manager.createNativeQuery("SET LOCAL lock_timeout = '10s'").executeUpdate();
            manager.createNativeQuery("SET LOCAL statement_timeout = '10s'").executeUpdate();
        }
        public EntityManager em() { return manager; }
        public void commit() { manager.flush(); transaction.commit(); }
        public void rollback() { transaction.rollback(); }
        @Override public void close() {
            try { if (transaction.isActive()) { transaction.rollback(); } }
            finally { manager.close(); }
        }
    }

    // Registered only through explicit orm.xml, so Boot entity scanning cannot discover it.
    @Table(name = "permit", schema = "s1a")
    @DynamicUpdate
    public static class Permit {
        @Id @Column(name = "permit_id", updatable = false) private UUID id;
        @Column(name = "environment_id", updatable = false) private String environment;
        @Column(name = "publication_id", updatable = false) private UUID publication;
        @Column(name = "event_id", updatable = false) private UUID event;
        @Column(name = "listener_id", updatable = false) private String listener;
        @Column(name = "actor_id", updatable = false) private String actor;
        @Column(name = "reason_code", updatable = false) private String reason;
        @Column(name = "issued_at", updatable = false) private Instant issued;
        @Column(name = "expires_at", updatable = false) private Instant expires;
        @Column(name = "closed_at", insertable = false) private @Nullable Instant closed;
        @Column(name = "confirmed_by", insertable = false) private @Nullable String confirmedBy;
        @Column(name = "result_ref", insertable = false) private @Nullable String resultRef;
        @Version @Column(name = "version", insertable = false) private long version;
        protected Permit() {}
        public Permit(UUID id, UUID publication) {
            this.id = id;
            this.publication = publication;
            environment = "fixture-env";
            event = UUID.fromString("00000000-0000-0000-0000-000000000200");
            listener = "fixture-listener";
            actor = "fixture-actor";
            reason = "OWNER_REVIEW";
            issued = ISSUED;
            expires = EXPIRES;
        }
        public UUID id() { return id; }
        public UUID publication() { return publication; }
        public String actor() { return actor; }
        public Instant issued() { return issued; }
        public Instant expires() { return expires; }
        public long version() { return version; }
        public boolean closed() { return closed != null; }
        public void confirm(String action) {
            closed = EXPIRES.plusSeconds(1);
            confirmedBy = "fixture-reviewer";
            resultRef = "fixture-" + action;
        }
    }

    // Registered only through explicit orm.xml, so Boot entity scanning cannot discover it.
    @Table(name = "consumption", schema = "s1a")
    public static class Consumption {
        @Id @Column(name = "permit_id") private UUID permit;
        @Column(name = "operation_id") private UUID operation;
        @Column(name = "worker_generation") private String generation;
        @Column(name = "consumed_at") private Instant consumed;
        protected Consumption() {}
        public Consumption(UUID permit, UUID operation) {
            this.permit = permit;
            this.operation = operation;
            generation = "fixture-worker";
            consumed = ISSUED.plusSeconds(10);
        }
        // Deliberate negative fixture: DB, rather than a hidden Java API, must reject UPDATE.
        public void corruptForNegativeTest() { generation = "forged"; }
    }
}

package org.koikifw.buildsupport.phase4.b1fixture;

import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Sole supply-side raw reader. JDBC credentials never appear in observations. */
public final class B1SourceCollector {
    private static final Pattern EVENT = Pattern.compile("\\s*\\{\\s*\"eventId\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"\\s*}\\s*");
    public static UUID eventId(String json) {
        var match = EVENT.matcher(json);
        if (!match.matches()) throw new IllegalArgumentException("invalid event representation");
        return UUID.fromString(match.group(1));
    }
    public static String fingerprint(ResultSet row, String prefix) throws SQLException {
        return digest(fields(row.getString(prefix + "event_type"), row.getString(prefix + "listener"),
                row.getString(prefix + "serialized_event"), row.getString(prefix + "status"),
                row.getInt(prefix + "attempt"), row.getObject(prefix + "publication_date"),
                row.getObject(prefix + "completion_date"), row.getObject(prefix + "resubmission_date")));
    }
    public static Manifest manifest(Instant now) {
        UUID event = UUID.randomUUID();
        try {
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("target","phase4-level2-verification-0.1.0-SNAPSHOT.jar"))));
            return new Manifest(UUID.randomUUID(), event, event.toString(), "test-payload-sha", "test-recipient-sha",
                    SOURCE, LISTENER, hash, now.plusSeconds(600));
        } catch(Exception failure) { throw new IllegalStateException("fixture JAR identity unavailable"); }
    }
    /** Setup is admin-only. Binding precedes any child launch/event/provider fixture creation. */
    public static final class Fixture implements AutoCloseable {
        public final PostgreSQLContainer postgres = B1ResourceLimits.container();
        private final String readerSecret = UUID.randomUUID().toString();
        private final String writerSecret = UUID.randomUUID().toString();
        private final String fixtureSecret = UUID.randomUUID().toString();
        private boolean viewsReady;
        public Fixture() throws Exception { this(false); }
        public Fixture(boolean ordinaryChildMigrations) throws Exception {
            try {
                postgres.start();
                B1ResourceLimits.assertDatabase(postgres);
                try (Connection c = admin(); Statement s = c.createStatement()) {
                    s.setQueryTimeout(10);
                    s.execute("CREATE ROLE b1_reader LOGIN PASSWORD '" + readerSecret + "'");
                    s.execute("CREATE ROLE b1_writer LOGIN PASSWORD '" + writerSecret + "'");
                    s.execute("CREATE ROLE b1_fixture LOGIN PASSWORD '" + fixtureSecret + "'");
                    s.execute("GRANT USAGE,CREATE ON SCHEMA public TO b1_fixture");
                    if (!ordinaryChildMigrations) {
                        s.execute("SET ROLE b1_fixture");
                        s.execute(resource("db/migration/V1__probe_schema.sql"));
                        s.execute(resource("db/migration/V2__failed_transition_probe.sql"));
                        s.execute("RESET ROLE");
                    }
                    String sql = resource("s1-b1/read-source.sql");
                    int viewStart=sql.indexOf("CREATE VIEW b1_read.target");
                    s.execute(sql.substring(0,viewStart));
                    if (!ordinaryChildMigrations) { s.execute(sql.substring(viewStart)); viewsReady=true; }
                }
            } catch (Exception | AssertionError failure) { postgres.close(); throw failure; }
        }
        public String url() {
            String url = postgres.getJdbcUrl();
            return url + (url.contains("?") ? "&" : "?") + "connectTimeout=10&socketTimeout=10";
        }
        public Connection admin() throws SQLException {
            return DriverManager.getConnection(url(), postgres.getUsername(), postgres.getPassword());
        }
        public Connection writer() throws SQLException { return DriverManager.getConnection(url(), "b1_writer", writerSecret); }
        public Connection reader() throws SQLException { return DriverManager.getConnection(url(), "b1_reader", readerSecret); }
        public Connection wrongReader() throws SQLException { return DriverManager.getConnection(url(), "b1_reader", "incorrect-test-only"); }
        public void fixtureEnvironment(ProcessBuilder builder) {
            builder.environment().put("SPRING_DATASOURCE_URL",postgres.getJdbcUrl());
            builder.environment().put("SPRING_DATASOURCE_USERNAME","b1_fixture");
            builder.environment().put("SPRING_DATASOURCE_PASSWORD",fixtureSecret);
        }
        public void readerEnvironment(ProcessBuilder builder) {
            builder.environment().put("B1_READER_URL",url());
            builder.environment().put("B1_READER_USERNAME","b1_reader");
            builder.environment().put("B1_READER_PASSWORD",readerSecret);
        }
        public void initializeViews() throws Exception {
            if (viewsReady) return;
            String sql=resource("s1-b1/read-source.sql");
            try(Connection c=admin();Statement s=c.createStatement()) {
                s.setQueryTimeout(10);s.execute(sql.substring(sql.indexOf("CREATE VIEW b1_read.target")));
                viewsReady=true;
            }
        }
        public void execute(String sql, Object... arguments) throws SQLException {
            try (Connection c = admin(); PreparedStatement s = c.prepareStatement(sql)) {
                s.setQueryTimeout(10); bind(s, arguments); s.execute();
            }
        }
        public Manifest prepare(Instant now, String environment) throws SQLException {
            Manifest m = manifest(now);
            execute("INSERT INTO b1_source.binding VALUES (?,?,?,NULL,?,?,?,?,?)", m.run(), environment,
                    m.event(), m.key(), m.payload(), m.recipient(), m.source(), Timestamp.from(m.keyUntil()));
            return m;
        }
        /** Finite raw fixtures for focused DB branches. IT obtains rows from the ordinary child. */
        public Target seed(Manifest m, String environment, Instant now, boolean accepted) throws SQLException {
            UUID publication = UUID.randomUUID();
            Target t = new Target(environment, publication, m.event(), EVENT_TYPE, m.listener(), 1);
            execute("INSERT INTO public.event_publication(id,listener_id,event_type,serialized_event,publication_date,status,completion_attempts) VALUES (?,?,?,?,?,'PROCESSING',1)",
                    publication, t.listener(), EVENT_TYPE, "{\"eventId\":\"" + m.event() + "\"}", Timestamp.from(now));
            seal(m, t);
            if (accepted) execute("INSERT INTO public.probe_provider_send(event_id,idempotency_key) VALUES (?,?)", m.event(), m.event());
            return t;
        }
        /** Generated target discovery strictly decodes all candidate events; ambiguity is rejected. */
        public Target discover(Manifest m) throws SQLException {
            Target found = null;
            try (Connection c = writer(); PreparedStatement s = c.prepareStatement(
                    "SELECT id,event_type,serialized_event,listener_id,completion_attempts FROM public.event_publication WHERE event_type=?")) {
                s.setQueryTimeout(10); s.setString(1, EVENT_TYPE);
                try (ResultSet r = s.executeQuery()) {
                    int rows = 0;
                    while (r.next()) {
                        if (++rows > 4) throw new SQLException("finite discovery budget exceeded");
                        if (!eventId(r.getString("serialized_event")).equals(m.event())) continue;
                        if (!m.listener().equals(r.getString("listener_id"))) throw new SQLException("unexpected listener");
                        if (found != null) throw new SQLException("ambiguous publication");
                        found = new Target(ENVIRONMENT, r.getObject("id", UUID.class), m.event(), EVENT_TYPE,
                                r.getString("listener_id"), r.getInt("completion_attempts"));
                    }
                }
            }
            if (found == null) throw new SQLException("publication absent");
            seal(m, found); return found;
        }
        public void seal(Manifest m, Target t) throws SQLException {
            if(!m.event().equals(t.event()) || !m.listener().equals(t.listener()) || !EVENT_TYPE.equals(t.eventType()))
                throw new SQLException("manifest target mismatch");
            try (Connection c = admin(); PreparedStatement s = c.prepareStatement(
                    "UPDATE b1_source.binding SET publication=? WHERE run=? AND publication IS NULL AND event=?")) {
                s.setQueryTimeout(10); bind(s, t.publication(), m.run(), t.event());
                if (s.executeUpdate() != 1) throw new SQLException("publication binding already sealed or mismatched");
            }
        }
        public void reset() throws SQLException {
            if(!viewsReady) return;
            execute("TRUNCATE b1_evidence.revocation,b1_evidence.document,b1_source.snapshot,b1_source.binding,public.probe_provider_send,public.event_publication,public.probe_approval RESTART IDENTITY CASCADE");
        }
        @Override public void close() { postgres.close(); }
    }
    public static Snapshot collect(Fixture f, Manifest m, Target t, Stop stop, Instant now,
                                   boolean fixtureNoDelayedRequests) throws SQLException {
        try (Connection c = f.writer()) {
            c.setAutoCommit(false); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try {
                String fingerprint; String status;
                try (PreparedStatement s = c.prepareStatement("SELECT event_type,listener_id AS listener,serialized_event,status,completion_attempts AS attempt,publication_date,completion_date,last_resubmission_date AS resubmission_date FROM public.event_publication WHERE id=?")) {
                    s.setQueryTimeout(10); s.setObject(1, t.publication());
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next() || !t.event().equals(eventId(r.getString("serialized_event")))
                                || !t.eventType().equals(r.getString("event_type")) || !t.listener().equals(r.getString("listener"))
                                || t.attempt() != r.getInt("attempt")) throw new SQLException("target mismatch");
                        fingerprint = fingerprint(r, ""); status = r.getString("status");
                    }
                }
                Long receipt = null; int receiptCount = 0;
                try (PreparedStatement s = c.prepareStatement("SELECT id,event_id,idempotency_key FROM public.probe_provider_send WHERE event_id=? OR idempotency_key=?")) {
                    s.setQueryTimeout(10); bind(s, t.event(), t.event());
                    try (ResultSet r = s.executeQuery()) {
                        while (r.next()) {
                            if (++receiptCount > 1 || !t.event().equals(r.getObject(2, UUID.class))
                                    || !t.event().equals(r.getObject(3, UUID.class))) throw new SQLException("provider contradiction");
                            receipt = r.getLong(1);
                        }
                    }
                }
                Provider provider = receipt != null ? Provider.ACCEPTED : fixtureNoDelayedRequests
                        && stop.matches(m.run(), stop.generation(), m.source(), now) ? Provider.FIXTURE_NOT_ACCEPTED : Provider.UNKNOWN;
                long revision;
                try (PreparedStatement s = c.prepareStatement("""
                    INSERT INTO b1_source.snapshot(environment,publication,event,event_type,listener,attempt,run,source,
                    fingerprint,notification_key,payload_identity,recipient_identity,status,provider,receipt,generation,
                    all_ended,forced,controlled,stop_observed,stop_until,observed,valid_until,key_until)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING revision
                    """)) {
                    s.setQueryTimeout(10);
                    bind(s, t.environment(), t.publication(), t.event(), t.eventType(), t.listener(), t.attempt(), m.run(),
                            m.source(), fingerprint, m.key(), m.payload(), m.recipient(), status, provider.name(), receipt,
                            stop.generation(), stop.allEnded(), stop.forced(), stop.controlled(), Timestamp.from(stop.observed()),
                            Timestamp.from(stop.until()), Timestamp.from(now), Timestamp.from(now.plusSeconds(60)), Timestamp.from(m.keyUntil()));
                    try (ResultSet r = s.executeQuery()) { r.next(); revision = r.getLong(1); }
                }
                c.commit();
                return new Snapshot(t, m.run(), revision, m.source(), fingerprint, m.key(), m.payload(), m.recipient(),
                        status, provider, receipt, stop, now, now.plusSeconds(60), m.keyUntil());
            } catch (SQLException | RuntimeException failure) { c.rollback(); throw failure; }
        }
    }
    public static void bind(PreparedStatement s, Object... args) throws SQLException {
        for (int i=0; i<args.length; i++) s.setObject(i+1, args[i]);
    }
    private static String resource(String path) throws Exception {
        try (var input = B1SourceCollector.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("test resource missing");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private B1SourceCollector() { }
}

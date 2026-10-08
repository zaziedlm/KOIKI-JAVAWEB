package org.koikifw.buildsupport.phase4.b2fixture;

import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.koikifw.buildsupport.phase4.b1fixture.B1SourceCollector;

/** Supplier-owned frozen protocol. No permit operation or delivery permission. */
public final class B2FrozenSourceProtocol {
    private final B1SourceCollector.Fixture fixture;
    private final String readerSecret;
    private boolean frozen;

    public B2FrozenSourceProtocol(B1SourceCollector.Fixture fixture,String readerSecret) throws Exception {
        this.fixture=Objects.requireNonNull(fixture);
        if(!readerSecret.matches("[0-9a-f-]{36}")) throw new IllegalArgumentException("invalid fixture secret");
        this.readerSecret=readerSecret;
        try(Connection c=fixture.admin();Statement s=c.createStatement()) {
            s.setQueryTimeout(10);
            s.execute("CREATE ROLE b2_reader LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+readerSecret+"'");
            try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/s1-b2/frozen-source.sql"))) {
                s.execute(new String(input.readAllBytes(),StandardCharsets.UTF_8));
            }
        }
    }

    public synchronized boolean frozen() { return frozen; }

    public synchronized void freeze(Manifest manifest,Snapshot snapshot,Stop currentStop,
                                    boolean launchClosed,Instant now) throws SQLException {
        if(frozen || !launchClosed || !currentStop.matches(manifest.run(),snapshot.stop().generation(),manifest.source(),now)
                || !currentStop.equals(snapshot.stop()) || !snapshot.validAt(now)
                || snapshot.provider()!=Provider.FIXTURE_NOT_ACCEPTED) throw new IllegalStateException("freeze unavailable");
        String database=fixture.postgres.getDatabaseName();
        if(!database.matches("[a-z][a-z0-9_]*")) throw new IllegalStateException("fixture database invalid");
        try(Connection c=fixture.admin()) {
            c.setAutoCommit(false);
            try {
                try(Statement s=c.createStatement()) {
                    s.setQueryTimeout(10);
                    try(ResultSet r=s.executeQuery("SELECT count(*) FROM pg_roles r WHERE rolname IN ('b1_fixture','b1_writer') AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT EXISTS(SELECT 1 FROM pg_auth_members m WHERE m.member=r.oid)")) {
                        r.next();if(r.getInt(1)!=2) throw new IllegalStateException("writer authority invalid");
                    }
                    try(ResultSet r=s.executeQuery("SELECT count(*) FROM pg_class t JOIN pg_namespace n ON n.oid=t.relnamespace JOIN pg_roles r ON r.oid=t.relowner WHERE n.nspname='public' AND t.relname IN ('probe_approval','event_publication','probe_provider_send') AND r.rolname='b1_fixture'")) {
                        r.next();if(r.getInt(1)!=3) throw new IllegalStateException("fixture ownership invalid");
                    }
                    s.execute("ALTER ROLE b1_fixture NOLOGIN");s.execute("ALTER ROLE b1_writer NOLOGIN");
                    s.execute("REVOKE ALL ON ALL TABLES IN SCHEMA public,b1_source,b1_evidence FROM b1_fixture,b1_writer");
                    s.execute("REVOKE ALL ON ALL SEQUENCES IN SCHEMA public,b1_source,b1_evidence FROM b1_fixture,b1_writer");
                    s.execute("REVOKE CREATE ON SCHEMA public FROM b1_fixture");
                    s.execute("REVOKE CONNECT ON DATABASE "+database+" FROM PUBLIC,b1_fixture,b1_writer,b1_reader");
                    s.execute("GRANT CONNECT ON DATABASE "+database+" TO b2_reader");
                }
                c.commit(); // Publish NOLOGIN before checking all old connections.
                try(Statement s=c.createStatement()) {
                    s.setQueryTimeout(10);
                    try(ResultSet r=s.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid()")) {
                        r.next();if(r.getInt(1)!=0) throw new IllegalStateException("source connection remains");
                    }
                }
                verifySource(c,snapshot);
                try(PreparedStatement s=c.prepareStatement("""
                    INSERT INTO b2_source.freeze
                    SELECT t.run,1,t.environment,t.publication,t.event,t.event_type,t.listener,t.attempt,
                    t.source,?,t.revision,t.fingerprint,t.notification_key,t.payload_identity,t.recipient_identity,
                    t.generation,?,?,t.current_publication_date,t.current_completion_date,
                    t.current_resubmission_date,t.current_serialized_event,t.current_status
                    FROM b1_read.target t WHERE t.run=? AND t.revision=?
                    """)) {
                    s.setQueryTimeout(10);
                    B1SourceCollector.bind(s,manifest.jarHash(),Timestamp.from(now),Timestamp.from(now.plusSeconds(60)),manifest.run(),snapshot.revision());
                    if(s.executeUpdate()!=1) throw new IllegalStateException("freeze registration ambiguous");
                }
                c.commit();frozen=true;
            } catch(SQLException|RuntimeException failure) {c.rollback();throw failure;}
        }
    }

    private static void verifySource(Connection c,Snapshot expected) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM b1_read.target WHERE run=? AND revision=?")) {
            s.setQueryTimeout(10);B1SourceCollector.bind(s,expected.run(),expected.revision());
            try(ResultSet r=s.executeQuery()) {
                if(!r.next() || r.getLong("current_revision")!=expected.revision()
                        || !expected.sourceFingerprint().equals(B1SourceCollector.fingerprint(r,"current_"))
                        || !expected.target().event().equals(B1SourceCollector.eventId(r.getString("current_serialized_event")))
                        || !expected.target().publication().equals(r.getObject("bound_publication",UUID.class))
                        || !expected.key().equals(r.getString("bound_key"))
                        || !expected.payload().equals(r.getString("bound_payload"))
                        || !expected.recipient().equals(r.getString("bound_recipient"))
                        || r.getInt("current_receipt_count")!=0 || r.next()) throw new IllegalStateException("source changed");
            }
        }
    }

    public Connection reader() throws SQLException {return DriverManager.getConnection(fixture.url(),"b2_reader",readerSecret);}

    public boolean read(Manifest manifest,Snapshot expected,Instant now) {
        if(!frozen || !expected.validAt(now) || expected.provider()!=Provider.FIXTURE_NOT_ACCEPTED) return false;
        try(Connection c=reader()) {
            c.setReadOnly(true);c.setAutoCommit(false);
            try(PreparedStatement s=c.prepareStatement("SELECT * FROM b2_read.frozen_target WHERE run=? AND environment=? AND publication=?")) {
                s.setQueryTimeout(10);B1SourceCollector.bind(s,manifest.run(),expected.target().environment(),expected.target().publication());
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next()) return false;
                    boolean matching=r.getInt("protocol_version")==1 && r.getBoolean("writers_disabled") && r.getBoolean("current_matches")
                        && r.getBoolean("all_ended") && r.getBoolean("controlled")
                        && manifest.source().equals(r.getString("source_id")) && manifest.jarHash().equals(r.getString("jar_sha256"))
                        && expected.revision()==r.getLong("revision") && expected.sourceFingerprint().equals(r.getString("fingerprint"))
                        && expected.target().event().equals(r.getObject("event",UUID.class))
                        && expected.target().eventType().equals(r.getString("event_type"))
                        && expected.target().listener().equals(r.getString("listener")) && expected.target().attempt()==r.getInt("attempt")
                        && expected.key().equals(r.getString("notification_key")) && expected.payload().equals(r.getString("payload_identity"))
                        && expected.recipient().equals(r.getString("recipient_identity")) && expected.stop().generation()==r.getLong("generation")
                        && !now.isBefore(r.getTimestamp("frozen_at").toInstant()) && now.isBefore(r.getTimestamp("admit_until").toInstant())
                        && "FIXTURE_NOT_ACCEPTED".equals(r.getString("provider"));
                    if(r.next()) return false;c.commit();return matching;
                }
            }
        } catch(SQLException|RuntimeException failure) {return false;}
    }
}

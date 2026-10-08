package org.koikifw.buildsupport.phase4.b1fixture;

import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;
import static org.koikifw.buildsupport.phase4.b1fixture.B1SourceCollector.bind;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

/** Separate schema; durable registration precedes publication of the reference. */
public final class B1EvidenceLedger {
    public static Evidence save(B1SourceCollector.Fixture f, EvidenceKey key, Snapshot snapshot, String subject)
            throws SQLException {
        UUID reference = UUID.randomUUID();
        try (Connection c = f.writer()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement("""
                    INSERT INTO b1_evidence.document VALUES (?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(environment,permit,operation,worker_generation) DO NOTHING
                    """)) {
                    s.setQueryTimeout(10);
                    bind(s, reference, key.environment(), key.permit(), key.operation(), key.workerGeneration(),
                            snapshot.run(), subject, snapshot.canonical(), Timestamp.from(snapshot.until()));
                    s.executeUpdate();
                }
                Evidence stored;
                try (PreparedStatement s = c.prepareStatement("SELECT * FROM b1_evidence.document WHERE environment=? AND permit=? AND operation=? AND worker_generation=?")) {
                    s.setQueryTimeout(10); bind(s, key.environment(), key.permit(), key.operation(), key.workerGeneration());
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next()) throw new SQLException("evidence absent");
                        stored = map(r);
                        if (r.next() || !stored.canonical().equals(snapshot.canonical())
                                || !stored.run().equals(snapshot.run()) || !stored.subject().equals(subject))
                            throw new SQLException("evidence key conflict");
                    }
                }
                c.commit(); return stored;
            } catch (SQLException | RuntimeException failure) { c.rollback(); throw failure; }
        }
    }
    public static Evidence resolve(Connection c, UUID reference) throws SQLException {
        Evidence evidence;
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM b1_read.evidence WHERE reference=?")) {
            s.setQueryTimeout(10); s.setObject(1, reference);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("evidence absent");
                evidence = map(r);
                if (r.next()) throw new SQLException("evidence ambiguous");
            }
        }
        try (PreparedStatement s = c.prepareStatement("SELECT count(*) FROM b1_read.revocation WHERE reference=?")) {
            s.setQueryTimeout(10); s.setObject(1, reference);
            try (ResultSet r = s.executeQuery()) {
                r.next(); if (r.getInt(1) != 0) throw new SQLException("evidence revoked");
            }
        }
        return evidence;
    }
    public static void revoke(B1SourceCollector.Fixture f, UUID reference, Instant now) throws SQLException {
        try (Connection c = f.writer(); PreparedStatement s = c.prepareStatement("INSERT INTO b1_evidence.revocation VALUES (?,?)")) {
            s.setQueryTimeout(10); bind(s, reference, Timestamp.from(now)); s.executeUpdate();
        }
    }
    private static Evidence map(ResultSet r) throws SQLException {
        return new Evidence(r.getObject("reference", UUID.class),
                new EvidenceKey(r.getString("environment"), r.getObject("permit", UUID.class),
                        r.getObject("operation", UUID.class), r.getLong("worker_generation")),
                r.getObject("run", UUID.class), r.getString("subject"), r.getString("canonical"),
                r.getTimestamp("valid_until").toInstant());
    }
    /** §24 adopted test-only entry, restricted to E10/I07. All output is non-secret. */
    public static void main(String[] args) {
        try (Connection c=DriverManager.getConnection(System.getenv("B1_READER_URL"),
                System.getenv("B1_READER_USERNAME"),System.getenv("B1_READER_PASSWORD"))) {
            if(args.length!=1) throw new IllegalArgumentException("invalid reader arguments");
            c.setReadOnly(true);c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            Evidence evidence=resolve(c,UUID.fromString(args[0]));
            if(!Instant.now().isBefore(evidence.until())) throw new IllegalArgumentException("expired evidence");
            c.commit();System.out.println(evidence.reference()+":"+digest(evidence.canonical()));
        } catch(Exception failure) { System.err.println("B1_READER_REJECTED");System.exit(2); }
    }
    private B1EvidenceLedger() { }
}

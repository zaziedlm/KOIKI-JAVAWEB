package org.koikifw.buildsupport.phase4.b1fixture;

import static org.koikifw.buildsupport.phase4.b1fixture.B1ReadContract.*;

import java.sql.*;
import java.time.Instant;

/** SELECT-only connection; all checks share a bounded read-only repeatable-read transaction. */
public final class B1JdbcReadClient {
    public static Read read(B1SourceCollector.Fixture fixture, Snapshot expected, Evidence evidence,
                            Stop currentStop, Instant now) {
        if (!expected.validAt(now) || !now.isBefore(evidence.until())) return Read.reject("EXPIRED_OR_FUTURE");
        if (!expected.source().equals(SOURCE) || !currentStop.matches(expected.run(), expected.stop().generation(), SOURCE, now)
                || !currentStop.equals(expected.stop())) return Read.reject("STOP_OR_SOURCE_MISMATCH");
        try (Connection c = fixture.reader()) {
            c.setReadOnly(true); c.setAutoCommit(false); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            Evidence actual = B1EvidenceLedger.resolve(c, evidence.reference());
            if (!actual.equals(evidence) || !actual.canonical().equals(expected.canonical())
                    || !actual.run().equals(expected.run()) || !actual.key().environment().equals(expected.target().environment()))
                return Read.reject("EVIDENCE_MISMATCH");
            try (PreparedStatement s = c.prepareStatement("SELECT * FROM b1_read.target WHERE revision=? AND publication=? AND environment=?")) {
                s.setQueryTimeout(10);
                B1SourceCollector.bind(s, expected.revision(), expected.target().publication(), expected.target().environment());
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) return Read.reject("TARGET_ABSENT");
                    Snapshot observed = map(r);
                    boolean matching = observed.equals(expected) && expected.revision()==r.getLong("current_revision")
                            && expected.target().publication().equals(r.getObject("bound_publication", java.util.UUID.class))
                            && expected.key().equals(r.getString("bound_key")) && expected.payload().equals(r.getString("bound_payload"))
                            && expected.recipient().equals(r.getString("bound_recipient")) && SOURCE.equals(r.getString("bound_source"))
                            && expected.keyUntil().equals(r.getTimestamp("bound_key_until").toInstant())
                            && expected.sourceFingerprint().equals(B1SourceCollector.fingerprint(r, "current_"))
                            && expected.target().event().equals(B1SourceCollector.eventId(r.getString("current_serialized_event")));
                    if (!matching || r.next()) return Read.reject("TARGET_CHANGED_OR_AMBIGUOUS");
                    // FAILED is an observation. It never implies a delivery/retry permission.
                    if (!java.util.Set.of("PROCESSING", "COMPLETED", "FAILED", "PUBLISHED").contains(expected.status()))
                        return Read.reject("INELIGIBLE_OBSERVATION");
                }
            }
            // Receipt recheck is part of the same transaction and does not call ProviderStub.send.
            try (PreparedStatement s = c.prepareStatement("SELECT current_receipt_count,current_receipt FROM b1_read.target WHERE revision=?")) {
                s.setQueryTimeout(10); s.setLong(1, expected.revision());
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) return Read.reject("PROVIDER_ABSENT");
                    int count = r.getInt(1); Long receipt = r.getObject(2, Long.class);
                    if (count>1 || !java.util.Objects.equals(receipt, expected.receipt())
                            || (expected.provider()==Provider.ACCEPTED && (count!=1 || receipt==null))
                            || (expected.provider()!=Provider.ACCEPTED && count!=0)) return Read.reject("PROVIDER_CONTRADICTION");
                }
            }
            c.commit(); return new Read(true, expected.provider(), "OBSERVATION_ONLY", expected);
        } catch (SQLException | IllegalArgumentException failure) {
            // Do not expose driver message, credentials, SQL, or cached affirmative results.
            return Read.reject("SUPPLY_UNAVAILABLE_OR_INVALID");
        }
    }
    private static Snapshot map(ResultSet r) throws SQLException {
        Target target = new Target(r.getString("environment"), r.getObject("publication", java.util.UUID.class),
                r.getObject("event", java.util.UUID.class), r.getString("event_type"), r.getString("listener"), r.getInt("attempt"));
        var run = r.getObject("run", java.util.UUID.class);
        Stop stop = new Stop(run, r.getLong("generation"), r.getString("source"), r.getBoolean("all_ended"),
                r.getBoolean("forced"), r.getBoolean("controlled"), r.getTimestamp("stop_observed").toInstant(),
                r.getTimestamp("stop_until").toInstant());
        return new Snapshot(target, run, r.getLong("revision"), r.getString("source"), r.getString("fingerprint"),
                r.getString("notification_key"), r.getString("payload_identity"), r.getString("recipient_identity"),
                r.getString("status"), Provider.valueOf(r.getString("provider")), r.getObject("receipt", Long.class), stop,
                r.getTimestamp("observed").toInstant(), r.getTimestamp("valid_until").toInstant(), r.getTimestamp("key_until").toInstant());
    }
    private B1JdbcReadClient() { }
}

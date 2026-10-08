package org.koikifw.reference.notification.adapter.outbound.operational;

import java.sql.*;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.koikifw.reference.notification.adapter.outbound.configuration.FrozenRecoverySourceSettings;
import org.koikifw.reference.notification.application.port.outbound.RecoveryIssueProtectionPort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTargetPort;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** SELECT-only supplier protocol; no Tooling Java or base-table dependency. */
public final class JdbcProtectedRecoveryTargetAdapter implements RecoveryTargetPort,RecoveryIssueProtectionPort {
    private final FrozenRecoverySourceSettings settings;
    private final Clock clock;
    private final Semaphore connectionBudget=new Semaphore(1,true);
    private final ThreadLocal<Optional<Guard>> current=ThreadLocal.withInitial(Optional::empty);
    public JdbcProtectedRecoveryTargetAdapter(FrozenRecoverySourceSettings settings,Clock clock) {
        this.settings=Objects.requireNonNull(settings);this.clock=Objects.requireNonNull(clock);
    }
    @Override public Scope open(RecoveryTarget target) {
        if(TransactionSynchronizationManager.isActualTransactionActive() || current.get().isPresent()
                || !verified(target)) throw unavailable();
        Guard guard=new Guard(target);current.set(Optional.of(guard));return guard;
    }
    @Override public Optional<RecoveryTarget> current(String environment,UUID publication) {
        var active=current.get();
        if(active.isEmpty()) return Optional.empty();
        Guard guard=active.orElseThrow();
        if(!guard.active() || !guard.target.environmentId().equals(environment) || !guard.target.publicationId().equals(publication)
                || !verified(guard.target)) return Optional.empty();
        return Optional.of(guard.target);
    }
    private boolean verified(RecoveryTarget expected) {
        if(!settings.environment().equals(expected.environmentId())) return false;
        boolean acquired=false;
        try {
            acquired=connectionBudget.tryAcquire(10,TimeUnit.SECONDS);
            if(!acquired) return false;
            try(Connection c=DriverManager.getConnection(settings.jdbcUrl(),settings.username(),settings.password())) {
                c.setReadOnly(true);c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                try(PreparedStatement s=c.prepareStatement("SELECT * FROM b2_read.frozen_target WHERE run=? AND environment=? AND publication=?")) {
                    s.setQueryTimeout(10);s.setObject(1,settings.run());s.setString(2,settings.environment());s.setObject(3,expected.publicationId());
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) return false;
                        Instant now=clock.instant();
                        Instant frozen=r.getTimestamp("frozen_at").toInstant();
                        Instant admission=r.getTimestamp("admit_until").toInstant();
                        Instant observed=r.getTimestamp("observed").toInstant();
                        boolean valid=r.getInt("protocol_version")==1 && r.getBoolean("writers_disabled") && r.getBoolean("current_matches")
                            && r.getBoolean("all_ended") && r.getBoolean("controlled")
                            && settings.source().equals(r.getString("source_id")) && settings.jarHash().equals(r.getString("jar_sha256"))
                            && settings.revision()==r.getLong("revision") && r.getLong("generation")>0
                            && expected.eventId().equals(r.getObject("event",UUID.class)) && expected.listenerId().equals(r.getString("listener"))
                            && expected.expectedAttempt()==r.getInt("attempt") && "FIXTURE_NOT_ACCEPTED".equals(r.getString("provider"))
                            && !now.isBefore(frozen) && now.isBefore(admission) && !now.isBefore(observed)
                            && now.isBefore(r.getTimestamp("valid_until").toInstant()) && now.isBefore(r.getTimestamp("key_until").toInstant());
                        if(r.next()) return false;c.commit();return valid;
                    }
                }
            }
        } catch(InterruptedException interrupted) {Thread.currentThread().interrupt();return false;}
        catch(SQLException|RuntimeException failure) {return false;}
        finally {if(acquired) connectionBudget.release();}
    }
    private final class Guard implements Scope {
        private final Thread owner=Thread.currentThread();
        private final RecoveryTarget target;
        private boolean open=true;
        Guard(RecoveryTarget target) {this.target=target;}
        @Override public RecoveryTarget target() {if(!active()) throw unavailable();return target;}
        @Override public boolean active() {return open && Thread.currentThread()==owner && current.get().filter(g->g==this).isPresent();}
        @Override public void close() {
            if(Thread.currentThread()!=owner) throw unavailable();
            open=false;current.remove(); // DB freeze persists through fixture destruction, not scope close.
        }
    }
    private static IllegalStateException unavailable() {return new IllegalStateException("Recovery protection unavailable");}
}

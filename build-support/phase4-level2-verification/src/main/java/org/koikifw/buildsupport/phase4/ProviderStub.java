package org.koikifw.buildsupport.phase4;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Separate transaction simulates an external provider that has already accepted a send. */
@Service
public class ProviderStub {

    private final JdbcTemplate jdbc;
    private final AtomicBoolean idempotent = new AtomicBoolean(true);

    public ProviderStub(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void useIdempotencyKey(boolean enabled) {
        idempotent.set(enabled);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(UUID eventId) {
        if (idempotent.get()) {
            jdbc.update("""
                    INSERT INTO probe_provider_send(event_id, idempotency_key)
                    VALUES (?, ?) ON CONFLICT (idempotency_key) DO NOTHING
                    """, eventId, eventId);
        } else {
            jdbc.update("INSERT INTO probe_provider_send(event_id) VALUES (?)", eventId);
        }
    }
}

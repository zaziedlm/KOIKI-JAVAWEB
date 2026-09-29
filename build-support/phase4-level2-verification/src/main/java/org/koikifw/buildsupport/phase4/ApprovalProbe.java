package org.koikifw.buildsupport.phase4;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalProbe {

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final CorrelationProbe correlation;

    public ApprovalProbe(JdbcTemplate jdbc, ApplicationEventPublisher events, CorrelationProbe correlation) {
        this.jdbc = jdbc;
        this.events = events;
        this.correlation = correlation;
    }

    @Transactional
    public void approve(UUID eventId) {
        jdbc.update("INSERT INTO probe_approval(event_id) VALUES (?)", eventId);
        correlation.record("publish-request", eventId);
        events.publishEvent(new ProbeApproved(eventId));
    }
}

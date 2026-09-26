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

    public ApprovalProbe(JdbcTemplate jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional
    public void approve(UUID eventId) {
        jdbc.update("INSERT INTO probe_approval(event_id) VALUES (?)", eventId);
        events.publishEvent(new ProbeApproved(eventId));
    }
}

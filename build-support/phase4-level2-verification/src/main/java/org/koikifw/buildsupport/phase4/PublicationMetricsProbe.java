package org.koikifw.buildsupport.phase4;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Candidate measurements for FAILED count and age; names are fixture-only. */
@Component
public class PublicationMetricsProbe {

    public PublicationMetricsProbe(MeterRegistry registry, JdbcTemplate jdbc) {
        Gauge.builder("phase4.probe.publication.failed.count", jdbc,
                        source -> source.queryForObject(
                                "SELECT count(*) FROM event_publication WHERE status = 'FAILED'",
                                Integer.class))
                .register(registry);
        Gauge.builder("phase4.probe.publication.failed.oldest.seconds", jdbc,
                        source -> ageOfOldestFailed(source))
                .register(registry);
    }

    private static long ageOfOldestFailed(JdbcTemplate jdbc) {
        Timestamp first = jdbc.queryForObject(
                "SELECT min(publication_date) FROM event_publication WHERE status = 'FAILED'",
                Timestamp.class);
        return first == null ? 0 : Math.max(0, Duration.between(first.toInstant(), Instant.now()).toSeconds());
    }
}

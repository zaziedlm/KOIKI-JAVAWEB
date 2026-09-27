package org.koikifw.buildsupport.phase4;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Tooling-only log and context observations for one event across initial and recovery work. */
@Component
public class CorrelationProbe {

    private static final Logger log = LoggerFactory.getLogger(CorrelationProbe.class);

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void record(String stage, UUID eventId) {
        Map<String, String> current = MDC.getCopyOfContextMap();
        Map<String, String> snapshot = current == null ? Map.of() : Map.copyOf(current);
        entries.add(new Entry(stage, eventId, snapshot));
        log.info("PL2 correlation stage={} eventId={}", stage, eventId);
    }

    public List<Entry> forEvent(UUID eventId) {
        return entries.stream().filter(entry -> entry.eventId().equals(eventId)).toList();
    }

    public record Entry(String stage, UUID eventId, Map<String, String> context) {
    }
}

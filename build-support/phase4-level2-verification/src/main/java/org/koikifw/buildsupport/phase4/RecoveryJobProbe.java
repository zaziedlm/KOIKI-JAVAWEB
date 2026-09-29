package org.koikifw.buildsupport.phase4;

import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.stereotype.Component;

/** Separate job entry that reconstructs event correlation without an HTTP request context. */
@Component
public class RecoveryJobProbe {

    private final FailedEventPublications failed;
    private final CorrelationProbe correlation;

    public RecoveryJobProbe(FailedEventPublications failed, CorrelationProbe correlation) {
        this.failed = failed;
        this.correlation = correlation;
    }

    public void retry(UUID eventId, UUID jobId, UUID retryId) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            MDC.clear();
            MDC.put("eventId", eventId.toString());
            MDC.put("jobId", jobId.toString());
            MDC.put("retryId", retryId.toString());
            correlation.record("recovery-job", eventId);
            failed.resubmit(ResubmissionOptions.defaults().withFilter(publication ->
                    publication.getEvent() instanceof ProbeApproved event
                            && event.eventId().equals(eventId)));
        } finally {
            if (previous == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }
}

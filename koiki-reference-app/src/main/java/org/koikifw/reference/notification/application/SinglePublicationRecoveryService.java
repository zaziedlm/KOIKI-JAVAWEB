package org.koikifw.reference.notification.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.koikifw.reference.notification.application.port.outbound.IsolatedPublicationPort;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;
import org.springframework.modulith.events.IncompleteEventPublications;

/** R3-A isolated, single-publication resubmission; never registered for normal operation. */
public final class SinglePublicationRecoveryService {
    private final IsolatedPublicationPort port;
    private final IncompleteEventPublications publications;
    private final NotificationProviderPort provider;

    public SinglePublicationRecoveryService(IsolatedPublicationPort port, IncompleteEventPublications publications,
            NotificationProviderPort provider) {
        this.port = Objects.requireNonNull(port);
        this.publications = Objects.requireNonNull(publications);
        this.provider = Objects.requireNonNull(provider);
    }

    public void recover(IsolatedRecoverySelection selection) {
        try (var lease = port.acquire(selection)) {
            lease.assertUnchanged();
            var event = lease.event();
            var inspection = provider.inspect(event);
            if (inspection.state() != NotificationProviderPort.InspectionState.ACCEPTED_MATCH
                    && (inspection.state() != NotificationProviderPort.InspectionState.NOT_FOUND_WITHIN_VALID_WINDOW
                        || inspection.validUntil().isEmpty()
                        || !inspection.validUntil().orElseThrow().isAfter(Instant.now()))) {
                throw new RecoveryHoldException(inspection.state().name(), selection.publicationId(),
                        selection.eventId(), selection.payloadSha256());
            }
            if (inspection.state() == NotificationProviderPort.InspectionState.ACCEPTED_MATCH
                    && inspection.acceptanceId().isEmpty()) {
                throw new RecoveryHoldException("ACCEPTANCE_ID_ABSENT", selection.publicationId(),
                        selection.eventId(), selection.payloadSha256());
            }
            lease.assertUnchanged();
            publications.resubmitIncompletePublications(publication ->
                    publication.getIdentifier().equals(selection.publicationId()));
            lease.awaitCompleted(Duration.ofSeconds(30));
        }
    }
}

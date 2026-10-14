package org.koikifw.reference.notification.application;

import java.time.Duration;
import java.util.Objects;
import org.koikifw.reference.notification.application.port.outbound.IsolatedPublicationPort;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;
import org.springframework.modulith.events.IncompleteEventPublications;

/** R3-A isolated, single-publication resubmission; never registered for normal operation. */
public final class SinglePublicationRecoveryService {
    private final IsolatedPublicationPort port;
    private final IncompleteEventPublications publications;

    public SinglePublicationRecoveryService(IsolatedPublicationPort port, IncompleteEventPublications publications) {
        this.port = Objects.requireNonNull(port);
        this.publications = Objects.requireNonNull(publications);
    }

    public void recover(IsolatedRecoverySelection selection) {
        try (var lease = port.acquire(selection)) {
            lease.assertUnchanged();
            publications.resubmitIncompletePublications(publication ->
                    publication.getIdentifier().equals(selection.publicationId()));
            lease.awaitCompleted(Duration.ofSeconds(30));
        }
    }
}

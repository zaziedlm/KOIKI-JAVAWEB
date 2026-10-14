package org.koikifw.reference.notification.application.port.outbound;

import java.time.Duration;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;

/** Keeps the single-process advisory lock while a selected publication is rechecked and completed. */
public interface IsolatedPublicationPort {
    Lease acquire(IsolatedRecoverySelection selection);

    interface Lease extends AutoCloseable {
        void assertUnchanged();
        void awaitCompleted(Duration timeout);
        @Override void close();
    }
}

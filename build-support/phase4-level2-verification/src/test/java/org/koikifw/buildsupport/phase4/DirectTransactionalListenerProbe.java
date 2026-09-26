package org.koikifw.buildsupport.phase4;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Test-only listener used to distinguish delivery from durable publication registration. */
@Component
@ConditionalOnProperty(name = "probe.direct-listener.enabled", havingValue = "true")
class DirectTransactionalListenerProbe {

    private final AtomicInteger invocations = new AtomicInteger();

    @TransactionalEventListener
    public void on(ProbeApproved event) {
        invocations.incrementAndGet();
    }

    int invocations() {
        return invocations.get();
    }
}

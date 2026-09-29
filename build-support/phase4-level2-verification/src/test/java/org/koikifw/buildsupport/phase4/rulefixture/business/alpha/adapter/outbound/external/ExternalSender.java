package org.koikifw.buildsupport.phase4.rulefixture.business.alpha.adapter.outbound.external;

import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.application.port.SendPort;

public final class ExternalSender implements SendPort {

    @Override
    public void send() {
        // No external side effect is needed to inspect the dependency graph.
    }
}

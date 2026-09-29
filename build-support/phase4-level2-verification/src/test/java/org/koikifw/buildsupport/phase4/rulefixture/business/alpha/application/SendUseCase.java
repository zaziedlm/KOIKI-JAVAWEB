package org.koikifw.buildsupport.phase4.rulefixture.business.alpha.application;

import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.application.port.SendPort;

public final class SendUseCase {

    private final SendPort port;

    public SendUseCase(SendPort port) {
        this.port = port;
    }

    public void send() {
        port.send();
    }
}

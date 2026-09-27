package org.koikifw.buildsupport.phase4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationProbe {

    private final ProviderStub provider;
    private final CorrelationProbe correlation;
    private final String pauseFile;
    private final String pauseAfterSendFile;
    private final AtomicBoolean failAfterSend = new AtomicBoolean();

    public NotificationProbe(
            ProviderStub provider,
            CorrelationProbe correlation,
            @Value("${probe.pause.file:}") String pauseFile,
            @Value("${probe.pause.after-send.file:}") String pauseAfterSendFile) {
        this.provider = provider;
        this.correlation = correlation;
        this.pauseFile = pauseFile;
        this.pauseAfterSendFile = pauseAfterSendFile;
    }

    public void failOnceAfterSend() {
        failAfterSend.set(true);
    }

    @ApplicationModuleListener
    public void on(ProbeApproved event) {
        correlation.record("listener", event.eventId());
        pauseAt(pauseFile, event);
        provider.send(event.eventId());
        pauseAt(pauseAfterSendFile, event);
        if (failAfterSend.getAndSet(false)) {
            throw new IllegalStateException("PL2 probe: listener stopped after provider accepted send");
        }
    }

    private static void pauseAt(String marker, ProbeApproved event) {
        if (marker.isBlank()) {
            return;
        }
        try {
            Files.writeString(Path.of(marker), event.eventId().toString());
            while (true) {
                Thread.sleep(1_000);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("PL2 probe pause marker failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PL2 probe pause interrupted", exception);
        }
    }
}

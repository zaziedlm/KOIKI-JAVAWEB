package org.koikifw.buildsupport.phase4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Keeps a process alive so the integration test can kill it at a precise listener point. */
@Component
public class ProbeRunner implements ApplicationRunner {

    private final ApprovalProbe approvals;
    private final ExclusiveRecoveryProbe recovery;
    private final String approvalId;
    private final String recoveryId;
    private final String recoveryStatusFile;
    private final String readyFile;
    private final boolean keepAlive;

    public ProbeRunner(
            ApprovalProbe approvals,
            ExclusiveRecoveryProbe recovery,
            @Value("${probe.approve.id:}") String approvalId,
            @Value("${probe.recover.id:}") String recoveryId,
            @Value("${probe.recover.status.file:}") String recoveryStatusFile,
            @Value("${probe.ready.file:}") String readyFile,
            @Value("${probe.keep-alive:false}") boolean keepAlive) {
        this.approvals = approvals;
        this.recovery = recovery;
        this.approvalId = approvalId;
        this.recoveryId = recoveryId;
        this.recoveryStatusFile = recoveryStatusFile;
        this.readyFile = readyFile;
        this.keepAlive = keepAlive;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!approvalId.isBlank()) {
            approvals.approve(UUID.fromString(approvalId));
        }
        if (!recoveryId.isBlank()) {
            if (recoveryStatusFile.isBlank()) {
                throw new IllegalArgumentException("PL2 recovery status marker is required");
            }
            recovery.recover(UUID.fromString(recoveryId), Path.of(recoveryStatusFile));
        }
        if (!readyFile.isBlank()) {
            try {
                Files.writeString(Path.of(readyFile), "ready");
            } catch (IOException exception) {
                throw new IllegalStateException("PL2 probe ready marker failed", exception);
            }
        }
        while (keepAlive) {
            Thread.sleep(1_000);
        }
    }
}

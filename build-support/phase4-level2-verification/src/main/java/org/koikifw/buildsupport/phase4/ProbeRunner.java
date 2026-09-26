package org.koikifw.buildsupport.phase4;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Keeps a process alive so the integration test can kill it at a precise listener point. */
@Component
public class ProbeRunner implements ApplicationRunner {

    private final ApprovalProbe approvals;
    private final String approvalId;
    private final boolean keepAlive;

    public ProbeRunner(
            ApprovalProbe approvals,
            @Value("${probe.approve.id:}") String approvalId,
            @Value("${probe.keep-alive:false}") boolean keepAlive) {
        this.approvals = approvals;
        this.approvalId = approvalId;
        this.keepAlive = keepAlive;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        if (!approvalId.isBlank()) {
            approvals.approve(UUID.fromString(approvalId));
        }
        while (keepAlive) {
            Thread.sleep(1_000);
        }
    }
}

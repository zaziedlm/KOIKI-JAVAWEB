package org.koikifw.runtimeconsumer.workitem.application.usecase;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

/** Runs the Customer-like asynchronous workitem processing path. */
public class ProcessWorkItemAsyncUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessWorkItemAsyncUseCase.class);

    @Async
    public CompletableFuture<UUID> process(UUID id) {
        LOGGER.atInfo()
                .addKeyValue("operation", "processWorkItemAsync")
                .addKeyValue("result", "success")
                .log("work item async processed");
        return CompletableFuture.completedFuture(id);
    }
}

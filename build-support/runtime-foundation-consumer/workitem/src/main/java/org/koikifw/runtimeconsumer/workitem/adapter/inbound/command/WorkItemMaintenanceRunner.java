package org.koikifw.runtimeconsumer.workitem.adapter.inbound.command;

import java.util.List;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workitem.application.usecase.RunWorkItemMaintenanceUseCase;
import org.koikifw.runtimeconsumer.workitem.application.usecase.WorkItemMaintenanceResult;
import org.koikifw.runtimeconsumer.workitem.application.usecase.WorkItemMaintenanceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;

/** Converts maintenance process arguments and results at the command boundary. */
public final class WorkItemMaintenanceRunner implements ApplicationRunner, ExitCodeGenerator {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_FAILURE = 1;
    static final int EXIT_CONTENDED = 10;
    static final int EXIT_INVALID_ARGUMENT = 64;

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkItemMaintenanceRunner.class);
    private static final String TASK_KEY_OPTION = "koiki.consumer.task-key";

    private final RunWorkItemMaintenanceUseCase useCase;
    private int exitCode = EXIT_FAILURE;

    public WorkItemMaintenanceRunner(RunWorkItemMaintenanceUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> values = arguments.getOptionValues(TASK_KEY_OPTION);
        if (values == null || values.size() != 1) {
            invalidArgument(
                    values == null ? "(missing)" : "(multiple)",
                    values == null ? "missing task key" : "task key must occur once");
            return;
        }

        String taskKey = values.getFirst();
        WorkItemMaintenanceTask task = WorkItemMaintenanceTask.fromExternalKey(taskKey)
                .orElse(null);
        if (task == null) {
            invalidArgument(taskKey, "unknown task key");
            return;
        }

        WorkItemMaintenanceResult result = useCase.run(task);
        exitCode = switch (result.outcome()) {
            case SUCCEEDED -> EXIT_SUCCESS;
            case CONTENDED -> EXIT_CONTENDED;
            case FAILED -> EXIT_FAILURE;
        };
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void invalidArgument(String taskKey, String reason) {
        UUID executionId = UUID.randomUUID();
        LOGGER.atError()
                .addKeyValue("operation", "workItemMaintenance")
                .addKeyValue("result", "failed")
                .addKeyValue("taskKey", taskKey)
                .addKeyValue("executionId", executionId)
                .addKeyValue("lockOwner", executionId)
                .addKeyValue("errorCode", "INVALID_ARGUMENT")
                .addKeyValue("reason", reason)
                .log("work item maintenance rejected");
        exitCode = EXIT_INVALID_ARGUMENT;
    }
}

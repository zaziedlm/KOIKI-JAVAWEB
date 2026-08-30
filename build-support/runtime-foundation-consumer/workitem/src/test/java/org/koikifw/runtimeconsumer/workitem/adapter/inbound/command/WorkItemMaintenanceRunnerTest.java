package org.koikifw.runtimeconsumer.workitem.adapter.inbound.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemMaintenanceExecution;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemMaintenanceRepository;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock;
import org.koikifw.runtimeconsumer.workitem.application.usecase.ExecuteWorkItemMaintenanceUseCase;
import org.koikifw.runtimeconsumer.workitem.application.usecase.RunWorkItemMaintenanceUseCase;
import org.koikifw.runtimeconsumer.workitem.application.usecase.WorkItemMaintenanceTask;
import org.springframework.boot.DefaultApplicationArguments;

class WorkItemMaintenanceRunnerTest {

    @Test
    void mapsSuccessAndContentionToDistinctExitCodes() throws Exception {
        WorkItemMaintenanceRunner success = runner(true, true);
        success.run(arguments(WorkItemMaintenanceTask.PRIMARY.externalKey()));
        assertEquals(WorkItemMaintenanceRunner.EXIT_SUCCESS, success.getExitCode());

        WorkItemMaintenanceRunner contended = runner(false, true);
        contended.run(arguments(WorkItemMaintenanceTask.PRIMARY.externalKey()));
        assertEquals(WorkItemMaintenanceRunner.EXIT_CONTENDED, contended.getExitCode());
    }

    @Test
    void mapsInvalidInputAndTaskFailureToDistinctExitCodes() throws Exception {
        WorkItemMaintenanceRunner missing = runner(true, true);
        missing.run(new DefaultApplicationArguments());
        assertEquals(WorkItemMaintenanceRunner.EXIT_INVALID_ARGUMENT, missing.getExitCode());

        WorkItemMaintenanceRunner unknown = runner(true, true);
        unknown.run(arguments("unknown-task"));
        assertEquals(WorkItemMaintenanceRunner.EXIT_INVALID_ARGUMENT, unknown.getExitCode());

        WorkItemMaintenanceRunner failed = runner(true, false);
        failed.run(arguments(WorkItemMaintenanceTask.PRIMARY.externalKey()));
        assertEquals(WorkItemMaintenanceRunner.EXIT_FAILURE, failed.getExitCode());
    }

    private static DefaultApplicationArguments arguments(String taskKey) {
        return new DefaultApplicationArguments("--koiki.consumer.task-key=" + taskKey);
    }

    private static WorkItemMaintenanceRunner runner(boolean acquired, boolean registered) {
        WorkItemMaintenanceRepository repository = new SingleMaintenanceRepository(registered);
        WorkItemExecutionLock lock = (namespace, taskId) ->
                acquired ? Optional.of(() -> { }) : Optional.empty();
        var execute = new ExecuteWorkItemMaintenanceUseCase(repository);
        return new WorkItemMaintenanceRunner(new RunWorkItemMaintenanceUseCase(lock, execute));
    }

    private static final class SingleMaintenanceRepository
            implements WorkItemMaintenanceRepository {

        private final @Nullable WorkItemMaintenanceExecution execution;

        private SingleMaintenanceRepository(boolean registered) {
            execution = registered
                    ? new WorkItemMaintenanceExecution(WorkItemMaintenanceTask.PRIMARY.externalKey())
                    : null;
        }

        @Override
        public Optional<WorkItemMaintenanceExecution> findById(String taskKey) {
            return Optional.ofNullable(execution);
        }

        @Override
        public WorkItemMaintenanceExecution save(WorkItemMaintenanceExecution saved) {
            return saved;
        }
    }
}

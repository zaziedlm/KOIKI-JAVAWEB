package org.koikifw.runtimeconsumer.workitem.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemMaintenanceExecution;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemMaintenanceRepository;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock;

class RunWorkItemMaintenanceUseCaseTest {

    @Test
    void recordsOneSideEffectAndReleasesTheAcquiredLock() {
        InMemoryMaintenanceRepository repository = registeredRepository();
        StubExecutionLock lock = new StubExecutionLock(true);
        RunWorkItemMaintenanceUseCase useCase = useCase(lock, repository);

        WorkItemMaintenanceResult result = useCase.run(WorkItemMaintenanceTask.PRIMARY);

        assertEquals(WorkItemMaintenanceResult.Outcome.SUCCEEDED, result.outcome());
        assertEquals(1L, repository.primary().getExecutionCount());
        assertEquals(result.executionId(), repository.primary().getLastExecutionId());
        assertTrue(lock.closed);
    }

    @Test
    void skipsTheSideEffectWhenTheTaskKeyIsContended() {
        InMemoryMaintenanceRepository repository = registeredRepository();
        StubExecutionLock lock = new StubExecutionLock(false);
        RunWorkItemMaintenanceUseCase useCase = useCase(lock, repository);

        WorkItemMaintenanceResult result = useCase.run(WorkItemMaintenanceTask.PRIMARY);

        assertEquals(WorkItemMaintenanceResult.Outcome.CONTENDED, result.outcome());
        assertEquals(0L, repository.primary().getExecutionCount());
    }

    @Test
    void reportsFailureAndStillReleasesTheLock() {
        InMemoryMaintenanceRepository repository = new InMemoryMaintenanceRepository();
        StubExecutionLock lock = new StubExecutionLock(true);
        RunWorkItemMaintenanceUseCase useCase = useCase(lock, repository);

        WorkItemMaintenanceResult result = useCase.run(WorkItemMaintenanceTask.PRIMARY);

        assertEquals(WorkItemMaintenanceResult.Outcome.FAILED, result.outcome());
        assertTrue(lock.closed);
    }

    private static RunWorkItemMaintenanceUseCase useCase(
            WorkItemExecutionLock lock,
            WorkItemMaintenanceRepository repository) {
        return new RunWorkItemMaintenanceUseCase(
                lock, new ExecuteWorkItemMaintenanceUseCase(repository));
    }

    private static InMemoryMaintenanceRepository registeredRepository() {
        InMemoryMaintenanceRepository repository = new InMemoryMaintenanceRepository();
        repository.save(new WorkItemMaintenanceExecution(
                WorkItemMaintenanceTask.PRIMARY.externalKey()));
        return repository;
    }

    private static final class StubExecutionLock implements WorkItemExecutionLock {

        private final boolean acquired;
        private boolean closed;

        private StubExecutionLock(boolean acquired) {
            this.acquired = acquired;
        }

        @Override
        public Optional<LockHandle> tryAcquire(int namespace, int taskId) {
            return acquired ? Optional.of(() -> closed = true) : Optional.empty();
        }
    }

    private static final class InMemoryMaintenanceRepository
            implements WorkItemMaintenanceRepository {

        private final Map<String, WorkItemMaintenanceExecution> executions = new HashMap<>();

        @Override
        public Optional<WorkItemMaintenanceExecution> findById(String taskKey) {
            return Optional.ofNullable(executions.get(taskKey));
        }

        @Override
        public WorkItemMaintenanceExecution save(WorkItemMaintenanceExecution execution) {
            executions.put(execution.getTaskKey(), execution);
            return execution;
        }

        private WorkItemMaintenanceExecution primary() {
            return Objects.requireNonNull(
                    executions.get(WorkItemMaintenanceTask.PRIMARY.externalKey()));
        }
    }
}

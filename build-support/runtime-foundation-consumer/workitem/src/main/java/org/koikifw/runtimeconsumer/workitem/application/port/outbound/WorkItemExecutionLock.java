package org.koikifw.runtimeconsumer.workitem.application.port.outbound;

import java.util.Optional;

/** Acquires a process-external lock for one workitem maintenance task. */
public interface WorkItemExecutionLock {

    Optional<LockHandle> tryAcquire(int namespace, int taskId);

    /** Keeps the underlying lock session alive until the task is complete. */
    interface LockHandle extends AutoCloseable {

        @Override
        void close();
    }
}

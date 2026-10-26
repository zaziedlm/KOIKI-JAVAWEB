package org.koikifw.buildsupport.vt.observer;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.core.env.Environment;

/** In-process read-only metrics; never borrows connections or exposes remote JMX. */
final class VtResourceSampler implements AutoCloseable {
    private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(
            task -> Thread.ofPlatform().name("d2-resource-sampler").daemon(true).unstarted(task));

    VtResourceSampler(HikariDataSource source, VtRequestObserver observer, Environment environment) {
        observer.record("CONFIG|" + Runtime.version().feature() + "|" + Runtime.version()
                + "|" + environment.getRequiredProperty("spring.threads.virtual.enabled")
                + "|" + Runtime.getRuntime().maxMemory() + "|" + source.getMaximumPoolSize()
                + "|" + source.getMinimumIdle() + "|" + source.getConnectionTimeout());
        sampler.scheduleAtFixedRate(() -> {
            try {
                var pool = source.getHikariPoolMXBean();
                if (pool != null) {
                    var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                    observer.record("M|" + System.currentTimeMillis() + "|" + pool.getActiveConnections()
                            + "|" + pool.getIdleConnections() + "|" + pool.getTotalConnections()
                            + "|" + pool.getThreadsAwaitingConnection() + "|" + observer.activeRequests()
                            + "|" + ManagementFactory.getThreadMXBean().getThreadCount()
                            + "|" + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()
                            + "|" + os.getProcessCpuTime());
                }
            } catch (RuntimeException exception) { observer.record("FAIL|SAMPLER_FAILURE"); }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override public void close() throws InterruptedException {
        sampler.shutdownNow();
        if (!sampler.awaitTermination(5, TimeUnit.SECONDS)) throw new IllegalStateException("D2 sampler did not stop");
    }
}

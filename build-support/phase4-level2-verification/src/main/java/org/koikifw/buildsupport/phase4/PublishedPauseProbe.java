package org.koikifw.buildsupport.phase4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Test-only executor that pauses an async listener before it can claim a publication. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("probe.pause.before-async.file")
class PublishedPauseProbe {

    @Bean("taskExecutor")
    ThreadPoolTaskExecutor pausedTaskExecutor(
            @Value("${probe.pause.before-async.file}") String marker) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("published-pause-");
        executor.setTaskDecorator(task -> () -> {
            try {
                Files.writeString(Path.of(marker), "before-listener");
                while (true) {
                    Thread.sleep(1_000);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("PL2 async pause marker failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("PL2 async pause interrupted", exception);
            }
        });
        return executor;
    }
}

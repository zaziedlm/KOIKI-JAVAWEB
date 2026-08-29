package org.koikifw.runtimeconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/** Executable Customer-like application used only for Phase 1b acceptance. */
@SpringBootApplication
@EnableAsync
public class RuntimeFoundationConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuntimeFoundationConsumerApplication.class, args);
    }
}

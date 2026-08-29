package org.koikifw.runtimeconsumer;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/** Executable Customer-like application used only for Phase 1b acceptance. */
@SpringBootApplication
@EnableAsync
public class RuntimeFoundationConsumerApplication {

    public static void main(String[] args) {
        boolean maintenanceMode = Arrays.asList(args).contains("--koiki.consumer.mode=maintenance");
        SpringApplication application = new SpringApplication(RuntimeFoundationConsumerApplication.class);
        if (maintenanceMode) {
            application.setWebApplicationType(WebApplicationType.NONE);
        }
        var context = application.run(args);
        if (maintenanceMode) {
            System.exit(SpringApplication.exit(context));
        }
    }
}

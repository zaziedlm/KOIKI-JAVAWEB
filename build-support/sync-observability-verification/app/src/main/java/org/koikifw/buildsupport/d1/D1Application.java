package org.koikifw.buildsupport.d1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/** Loopback-only, non-distributable consumer fixture; no business or security contract. */
@SpringBootApplication
public class D1Application {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(D1Application.class);
        app.addListeners((ApplicationReadyEvent event) -> {
            var ctx = event.getApplicationContext();
            var env = ctx.getEnvironment();
            Properties ready = new Properties();
            ready.setProperty("port", env.getRequiredProperty("local.server.port"));
            ready.setProperty("java", System.getProperty("java.runtime.version"));
            ready.setProperty("virtual", env.getRequiredProperty("spring.threads.virtual.enabled"));
            ready.setProperty("export", env.getRequiredProperty("management.tracing.export.otlp.enabled"));
            ready.setProperty("exporters", Integer.toString(ctx.getBeansOfType(SpanExporter.class).size()));
            Path target = Path.of(env.getRequiredProperty("d1.ready-file"));
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            try {
                try (var out = Files.newOutputStream(temporary)) { ready.store(out, "D1 safe runtime identity"); }
                Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot write ready identity", ex); }
        });
        app.run(args);
    }
}

package org.koikifw.reference.notification.configuration;

import java.nio.file.Path;
import java.time.Clock;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoveryConfigurationLoader;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoveryScopeAdapter;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoverySnapshot;
import org.koikifw.reference.notification.adapter.outbound.configuration.ManagedRecoveryTtlPolicyAdapter;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration(proxyBeanMethods = false)
@Conditional({NotificationFoundationConfiguration.Enabled.class, ManagedRecoveryConfiguration.FileMode.class})
public class ManagedRecoveryConfiguration {
    public static final String PREFIX = "koiki.reference.notification.managed-config.";

    @Bean
    ManagedRecoverySnapshot notificationManagedSnapshot(Environment environment) {
        ManagedRecoverySnapshot snapshot;
        try {
            snapshot = new ManagedRecoveryConfigurationLoader().load(Path.of(required(environment, "path")),
                    required(environment, "expected-sha256"), required(environment, "expected-revision"), required(environment, "environment-id"));
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Managed recovery configuration invalid");
        }
        LoggerFactory.getLogger(ManagedRecoveryConfiguration.class).info("Managed recovery configuration validated revision={} sha256={}",
                snapshot.revision(), snapshot.sha256());
        return snapshot;
    }

    @Bean
    RecoveryScopePort notificationManagedScope(ManagedRecoverySnapshot snapshot, ObjectProvider<Clock> clocks) {
        return new ManagedRecoveryScopeAdapter(snapshot, clocks.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    RecoveryTtlPolicyPort notificationManagedTtl(ManagedRecoverySnapshot snapshot, ObjectProvider<Clock> clocks) {
        return new ManagedRecoveryTtlPolicyAdapter(snapshot, clocks.getIfAvailable(Clock::systemUTC));
    }

    private static String required(Environment environment, String name) {
        String value = environment.getProperty(PREFIX + name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Managed recovery manifest incomplete");
        return value;
    }

    private static boolean fileMode(ConditionContext context) {
        String mode = context.getEnvironment().getProperty(PREFIX + "mode", "disabled");
        if (!mode.equals("disabled") && !mode.equals("file")) throw new IllegalArgumentException("Managed recovery mode invalid");
        return mode.equals("file");
    }

    static final class FileMode implements Condition {
        @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) { return fileMode(context); }
    }
    static final class DisabledMode implements Condition {
        @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) { return !fileMode(context); }
    }
}

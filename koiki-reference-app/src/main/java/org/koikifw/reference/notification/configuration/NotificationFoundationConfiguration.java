package org.koikifw.reference.notification.configuration;

import org.koikifw.reference.notification.domain.model.RecoveryPermit;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Adds only notification mappings when explicitly enabled; no operational entrypoint yet. */
@Configuration(proxyBeanMethods = false)
@Conditional(NotificationFoundationConfiguration.Enabled.class)
@EntityScan(basePackageClasses = RecoveryPermit.class)
public class NotificationFoundationConfiguration {
    static final class Enabled implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(
                    "koiki.reference.notification.foundation.enabled", "false");
            if (!value.equals("true") && !value.equals("false")) {
                throw new IllegalArgumentException("Invalid notification foundation enabled property");
            }
            return value.equals("true");
        }
    }
}

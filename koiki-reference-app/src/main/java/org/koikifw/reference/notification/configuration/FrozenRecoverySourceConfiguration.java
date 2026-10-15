package org.koikifw.reference.notification.configuration;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.koikifw.reference.notification.adapter.outbound.configuration.FrozenRecoverySourceSettings;
import org.koikifw.reference.notification.adapter.outbound.operational.JdbcProtectedRecoveryTargetAdapter;
import org.koikifw.reference.notification.application.ProtectedRecoveryIssueService;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Explicit local fixture mode. No positive evidence Port is registered. */
@Configuration(proxyBeanMethods=false)
@Conditional({NotificationFoundationConfiguration.Enabled.class,FrozenRecoverySourceConfiguration.Selected.class})
public class FrozenRecoverySourceConfiguration {
    public static final String PREFIX="koiki.reference.notification.frozen-source.";
    @Bean FrozenRecoverySourceSettings notificationFrozenSourceSettings(Environment environment) {
        Map<String,String> secrets=new HashMap<>();
        if(!(environment instanceof ConfigurableEnvironment configurable)) throw new IllegalArgumentException("Frozen source ENV unavailable");
        var source=configurable.getPropertySources().get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        if(!(source instanceof SystemEnvironmentPropertySource)) throw new IllegalArgumentException("Frozen source ENV unavailable");
        for(String key:new String[]{"B2_SOURCE_JDBC_URL","B2_SOURCE_READER_USERNAME","B2_SOURCE_READER_PASSWORD"}) {
            Object value=source.getProperty(key);
            if(!(value instanceof String text)) throw new IllegalArgumentException("Frozen source ENV incomplete");
            secrets.put(key,text);
        }
        try {
            return new FrozenRecoverySourceSettings(required(environment,"environment-id"),required(environment,"run-id"),
                required(environment,"source-id"),required(environment,"expected-jar-sha256"),
                Long.parseLong(required(environment,"expected-revision")),secrets);
        } catch(RuntimeException invalid) {throw new IllegalArgumentException("Frozen recovery source invalid");}
    }
    @Bean JdbcProtectedRecoveryTargetAdapter notificationFrozenTarget(FrozenRecoverySourceSettings settings,ObjectProvider<Clock> clocks) {
        return new JdbcProtectedRecoveryTargetAdapter(settings,clocks.getIfAvailable(Clock::systemUTC));
    }
    @Bean ProtectedRecoveryIssueService notificationProtectedIssueService(RecoveryPermitService permits,JdbcProtectedRecoveryTargetAdapter target) {
        return new ProtectedRecoveryIssueService(permits,target);
    }
    private static String required(Environment environment,String name) {
        String value=environment.getProperty(PREFIX+name);
        if(value==null || value.isBlank()) throw new IllegalArgumentException("Frozen recovery settings incomplete");return value;
    }
    static final class Selected implements Condition {
        @Override public boolean matches(ConditionContext context,AnnotatedTypeMetadata metadata) {
            String mode=context.getEnvironment().getProperty(PREFIX+"mode","disabled");
            if(!mode.equals("disabled") && !mode.equals("tooling-jdbc-v1")) throw new IllegalArgumentException("Frozen recovery mode invalid");
            return mode.equals("tooling-jdbc-v1");
        }
    }
}

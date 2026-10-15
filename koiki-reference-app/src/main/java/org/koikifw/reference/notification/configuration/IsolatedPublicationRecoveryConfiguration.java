package org.koikifw.reference.notification.configuration;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.koikifw.reference.notification.adapter.outbound.operational.JdbcIsolatedPublicationAdapter;
import org.koikifw.reference.notification.application.SinglePublicationRecoveryService;
import org.koikifw.reference.notification.application.RecoveryHoldException;
import org.koikifw.reference.notification.application.port.outbound.IsolatedPublicationPort;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.IncompleteEventPublications;
import tools.jackson.databind.ObjectMapper;

/** Explicit R3-A isolated process only. No recovery bean exists in normal startup. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "koiki.reference.notification.recovery.enabled", havingValue = "true")
public class IsolatedPublicationRecoveryConfiguration {
    private static final String PREFIX = "koiki.reference.notification.recovery.";

    @Bean
    IsolatedPublicationPort isolatedPublicationPort(DataSource dataSource, ObjectMapper mapper) {
        return new JdbcIsolatedPublicationAdapter(dataSource, mapper);
    }

    @Bean
    SinglePublicationRecoveryService singlePublicationRecoveryService(IsolatedPublicationPort port,
            IncompleteEventPublications publications, NotificationProviderPort provider) {
        return new SinglePublicationRecoveryService(port, publications, provider);
    }

    @Bean
    ApplicationRunner isolatedPublicationRecoveryRunner(Environment environment,
            ConfigurableApplicationContext context, SinglePublicationRecoveryService service) {
        var selection = selection(environment);
        return ignored -> {
            try {
                service.recover(selection);
                LoggerFactory.getLogger(IsolatedPublicationRecoveryConfiguration.class)
                        .info("Isolated publication recovery completed publication={}", selection.publicationId());
            } catch (RecoveryHoldException hold) {
                LoggerFactory.getLogger(IsolatedPublicationRecoveryConfiguration.class).error("{}", hold.evidenceLine());
                throw hold;
            } finally {
                context.close();
            }
        };
    }

    private static IsolatedRecoverySelection selection(Environment environment) {
        if (!"true".equals(environment.getProperty("koiki.reference.notification.async.enabled"))
                || !"none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type"))
                || !"false".equals(environment.getProperty(
                        "spring.modulith.events.republish-outstanding-events-on-restart"))) {
            throw new IllegalArgumentException("Isolated recovery requires non-web async without automatic replay");
        }
        URI endpoint = URI.create(required(environment, "koiki.reference.notification.async.provider-endpoint"));
        if (!"http".equals(endpoint.getScheme()) || !"127.0.0.1".equals(endpoint.getHost())
                || endpoint.getPort() < 1 || !"/notifications".equals(endpoint.getPath())) {
            throw new IllegalArgumentException("Isolated recovery requires the loopback provider");
        }
        if (!required(environment, "spring.datasource.url").equals(required(environment, PREFIX + "expected-jdbc-url"))) {
            throw new IllegalArgumentException("Isolated recovery database selection mismatch");
        }
        long previousPid = Long.parseLong(required(environment, PREFIX + "previous-pid"));
        Instant.parse(required(environment, PREFIX + "previous-start-instant"));
        if (previousPid < 1 || ProcessHandle.of(previousPid).isPresent()) {
            throw new IllegalArgumentException("Original Reference process is not confirmed stopped");
        }
        return new IsolatedRecoverySelection(required(environment, PREFIX + "environment-id"),
                UUID.fromString(required(environment, PREFIX + "publication-id")),
                UUID.fromString(required(environment, PREFIX + "event-id")),
                required(environment, PREFIX + "listener-id"), required(environment, PREFIX + "status"),
                Integer.parseInt(required(environment, PREFIX + "completion-attempts")),
                required(environment, PREFIX + "notification-key"),
                required(environment, PREFIX + "payload-sha256"));
    }

    private static String required(Environment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Isolated recovery setting missing: " + name);
        return value;
    }
}

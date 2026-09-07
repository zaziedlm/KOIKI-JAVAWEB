package org.koikifw.session.internal;

import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

/** Guards the Framework-owned Spring Session JDBC schema and cleanup boundary. */
@AutoConfiguration(afterName = "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration")
@ConditionalOnClass(JdbcIndexedSessionRepository.class)
@ConditionalOnBean(DataSource.class)
public class KoikiSessionJdbcAutoConfiguration {

    @Bean
    InitializingBean koikiSessionJdbcSettingsGuard(Environment environment) {
        return () -> {
            requireValue(environment, "spring.session.jdbc.initialize-schema", "never");
            requireValue(environment, "spring.session.jdbc.table-name", "koiki_session");
            requireValue(environment, "spring.session.jdbc.cleanup-cron", "-");
            requireValue(environment, "server.servlet.session.cookie.http-only", "true");
        };
    }

    private static void requireValue(Environment environment, String property, String expected) {
        if (!expected.equalsIgnoreCase(environment.getProperty(property, ""))) {
            throw new IllegalStateException(
                    "KOIKI Session JDBC requires the approved value for " + property);
        }
    }
}

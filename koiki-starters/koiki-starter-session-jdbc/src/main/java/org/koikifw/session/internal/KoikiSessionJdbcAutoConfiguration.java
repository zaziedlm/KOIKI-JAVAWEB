package org.koikifw.session.internal;

import javax.sql.DataSource;
import org.koikifw.identity.UserSessionInvalidator;
import org.koikifw.session.SessionCleanup;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Guards the Framework-owned Spring Session JDBC schema and cleanup boundary. */
@AutoConfiguration(
        afterName = "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration",
        beforeName = "org.koikifw.identity.internal.KoikiIdentityAutoConfiguration")
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

    @Bean
    @ConditionalOnMissingBean(SessionRepository.class)
    JdbcIndexedSessionRepository koikiNonWebSessionRepository(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            Environment environment) {
        JdbcIndexedSessionRepository repository = new JdbcIndexedSessionRepository(
                new JdbcTemplate(dataSource), new TransactionTemplate(transactionManager));
        repository.setTableName("koiki_session");
        repository.setCleanupCron("-");
        repository.setFlushMode(FlushMode.ON_SAVE);
        repository.setSaveMode(SaveMode.ON_SET_ATTRIBUTE);
        repository.setDefaultMaxInactiveInterval(environment.getProperty(
                "spring.session.timeout", java.time.Duration.class,
                java.time.Duration.ofMinutes(30)));
        return repository;
    }

    @Bean
    @ConditionalOnMissingBean(UserSessionInvalidator.class)
    @ConditionalOnBean(JdbcIndexedSessionRepository.class)
    UserSessionInvalidator koikiJdbcUserSessionInvalidator(
            JdbcIndexedSessionRepository sessionRepository) {
        return new KoikiJdbcUserSessionInvalidator(sessionRepository);
    }

    @Bean
    @ConditionalOnMissingBean(SessionCleanup.class)
    SessionCleanup koikiSessionCleanup(
            DataSource dataSource, JdbcIndexedSessionRepository sessionRepository) {
        return new KoikiPostgresqlSessionCleanup(dataSource, sessionRepository);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(HttpSecurity.class)
    @ConditionalOnBean(CookieSerializer.class)
    static class ServletLogoutConfiguration {

        @Bean
        LogoutHandler koikiSessionLogoutHandler(CookieSerializer cookieSerializer) {
            return new KoikiSessionLogoutHandler(cookieSerializer);
        }

        @Bean
        Customizer<HttpSecurity> koikiSessionLogoutCustomizer(
                LogoutHandler koikiSessionLogoutHandler) {
            return http -> http.logout(logout -> logout
                    .invalidateHttpSession(false)
                    .addLogoutHandler(koikiSessionLogoutHandler));
        }
    }
}

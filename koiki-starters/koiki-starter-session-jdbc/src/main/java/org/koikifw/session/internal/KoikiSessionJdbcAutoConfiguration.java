package org.koikifw.session.internal;

import javax.sql.DataSource;
import org.koikifw.identity.UserSessionInvalidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;

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
    @ConditionalOnMissingBean(UserSessionInvalidator.class)
    @ConditionalOnBean(JdbcIndexedSessionRepository.class)
    UserSessionInvalidator koikiJdbcUserSessionInvalidator(
            JdbcIndexedSessionRepository sessionRepository) {
        return new KoikiJdbcUserSessionInvalidator(sessionRepository);
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

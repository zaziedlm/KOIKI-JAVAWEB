package org.koikifw.reference.externalcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.externalcheck.adapter.inbound.sessionapi.ExternalCheckApiExceptionHandler;
import org.koikifw.reference.externalcheck.adapter.inbound.sessionapi.ExternalCheckController;
import org.koikifw.reference.externalcheck.application.ExternalCheckService;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Rate;
import org.koikifw.reference.identity.adapter.inbound.web.ReferenceSessionCsrfController;
import org.koikifw.reference.identity.configuration.ReferenceSecurityConfiguration;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.koikifw.session.internal.KoikiSessionJdbcDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ExternalCheckSessionSecurityTest {
    private static final String PATH = "/spa-api/v1/external-checks/rate?base=USD&quote=JPY&date=2026-10-22";
    private final AtomicInteger calls = new AtomicInteger();
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withInitializer(context -> new KoikiSessionJdbcDefaultsEnvironmentPostProcessor()
                    .postProcessEnvironment(context.getEnvironment(), new SpringApplication()))
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class, SessionAutoConfiguration.class,
                    JdbcSessionAutoConfiguration.class, KoikiSessionJdbcAutoConfiguration.class,
                    org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration.class))
            .withUserConfiguration(ReferenceSecurityConfiguration.class, FixtureConfiguration.class)
            .withPropertyValues("server.servlet.session.cookie.secure=false", "koiki.reference.external-check.enabled=true")
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(JdbcIndexedSessionRepository.class, () -> mock(JdbcIndexedSessionRepository.class))
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
            .withBean(ExternalCheckService.class, () -> new ExternalCheckService((operation, query) -> {
                calls.incrementAndGet();
                return new Rate(operation, query.base(), query.quote(), query.date(), "150");
            }));

    @Test void anonymousGetIs401AndDoesNotCallProvider() {
        runner.run(context -> {
            MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
                    .perform(get(PATH)).andExpect(status().isUnauthorized());
            assertThat(calls.get()).isZero();
        });
    }

    @Test void expensePermissionDoesNotAuthorizeExternalChecks() {
        runner.run(context -> {
            MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
                    .perform(get(PATH).with(user("applicant").authorities(() -> "EXPENSE:APPLY")))
                    .andExpect(status().isForbidden());
            assertThat(calls.get()).isZero();
        });
    }

    @Test void authorizedSessionCanReadWithoutAddingAPostRoute() {
        runner.run(context -> {
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(get(PATH).with(user("external-reader").authorities(() -> "EXTERNALCHECK:USE")))
                    .andExpect(status().isOk());
            mvc.perform(get("/spa-api/v1/external-checks/receipts")
                            .with(user("external-reader").authorities(() -> "EXTERNALCHECK:USE")))
                    .andExpect(status().isNotFound());
            assertThat(calls.get()).isEqualTo(1);
        });
    }

    @Test void expiredSessionCookieDoesNotReachProvider() {
        runner.run(context -> {
            MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
                    .perform(get(PATH).cookie(new Cookie("SESSION", "ZXhwaXJlZA==")))
                    .andExpect(status().isUnauthorized());
            assertThat(calls.get()).isZero();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({ExternalCheckController.class, ExternalCheckApiExceptionHandler.class, ReferenceSessionCsrfController.class})
    static class FixtureConfiguration {
        @Bean
        @org.springframework.context.annotation.Primary
        org.springframework.security.authentication.password.CompromisedPasswordChecker passwordChecker() {
            return password -> new org.springframework.security.authentication.password.CompromisedPasswordDecision(false);
        }

        @Bean UserDetailsService users() {
            return new InMemoryUserDetailsManager(User.withUsername("reader").password("{noop}fixture-password")
                    .authorities("EXTERNALCHECK:USE").build());
        }
    }
}

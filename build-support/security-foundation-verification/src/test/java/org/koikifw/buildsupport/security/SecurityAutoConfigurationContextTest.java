package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

class SecurityAutoConfigurationContextTest {

    private static final AutoConfigurations SECURITY_AUTO_CONFIGURATIONS = AutoConfigurations.of(
            KoikiSecurityAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            ServletWebSecurityAutoConfiguration.class);

    private final WebApplicationContextRunner servletRunner =
            new WebApplicationContextRunner().withConfiguration(SECURITY_AUTO_CONFIGURATIONS);

    @Test
    void createsFailClosedFallbackForServletApplication() {
        servletRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("koikiSecurityFallbackFilterChain");
            assertThat(context).getBeans(SecurityFilterChain.class).hasSize(1);
        });
    }

    @Test
    void doesNotCreateServletSecurityInNonWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(SECURITY_AUTO_CONFIGURATIONS)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("koikiSecurityFallbackFilterChain");
                    assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
                });
    }

    @Test
    void bootOwnsTheDefaultChainWhenKoikiAutoConfigurationIsNotEnabled() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("koikiSecurityFallbackFilterChain");
                    assertThat(context).getBeans(SecurityFilterChain.class).hasSize(1);
                });
    }

    @Test
    void composesCustomerChainAheadOfFallback() {
        servletRunner.withUserConfiguration(CustomerChainConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("customerSecurityFilterChain");
            assertThat(context).hasBean("koikiSecurityFallbackFilterChain");
            assertThat(context).getBeans(SecurityFilterChain.class).hasSize(2);
        });
    }

    @Test
    void backsOffWhenCustomerExplicitlyReplacesFallbackBean() {
        servletRunner.withUserConfiguration(CustomerReplacementConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("koikiSecurityFallbackFilterChain");
            assertThat(context).getBeans(SecurityFilterChain.class).hasSize(1);
            assertThat(context.getBean("koikiSecurityFallbackFilterChain"))
                    .isSameAs(context.getBean(CustomerReplacementConfiguration.class).replacement);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomerChainConfiguration {

        @Bean
        @Order(1)
        SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/fixture/customer/**");
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated());
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomerReplacementConfiguration {

        private SecurityFilterChain replacement;

        @Bean("koikiSecurityFallbackFilterChain")
        @Order(Ordered.LOWEST_PRECEDENCE)
        SecurityFilterChain replacement(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            replacement = http.build();
            return replacement;
        }
    }
}

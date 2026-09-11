package com.example.koikiconsumer.c2;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Consumer-owned assembly required by the public Identity administration contract. */
@Configuration(proxyBeanMethods = false)
class C2ConsumerConfiguration {

    @Bean
    CompromisedPasswordChecker c2ConsumerCompromisedPasswordChecker() {
        return password -> new CompromisedPasswordDecision(false);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class WebProbeConfiguration {

        @Bean
        UserDetailsService c2ConsumerFixtureUsers(Environment environment) {
            String login = environment.getRequiredProperty("koiki.consumer.fixture-login");
            String secretValue =
                    environment.getRequiredProperty("koiki.consumer.fixture-credential");
            return new InMemoryUserDetailsManager(
                    User.withUsername(login).password("{noop}" + secretValue).roles("PROBE").build());
        }

        @Bean
        @Order(1)
        SecurityFilterChain c2ConsumerSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/consumer/c2/**");
            http.authorizeHttpRequests(requests -> requests
                    .requestMatchers("/consumer/c2/public")
                    .permitAll()
                    .anyRequest()
                    .authenticated());
            http.httpBasic(withDefaults());
            return http.build();
        }
    }
}

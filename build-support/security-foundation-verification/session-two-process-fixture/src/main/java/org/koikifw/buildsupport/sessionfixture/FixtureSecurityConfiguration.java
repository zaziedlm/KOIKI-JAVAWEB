package org.koikifw.buildsupport.sessionfixture;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class FixtureSecurityConfiguration {

    @Bean
    @Order(0)
    SecurityFilterChain fixtureBootstrapSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/fixture/readiness", "/fixture/setup");
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/fixture/setup"));
        http.headers(withDefaults());
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain fixtureSessionSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/login", "/logout", "/fixture/session");
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/login").permitAll()
                .requestMatchers("/fixture/session").hasAuthority("ORDER:READ")
                .anyRequest().authenticated());
        http.formLogin(withDefaults());
        return http.build();
    }
}

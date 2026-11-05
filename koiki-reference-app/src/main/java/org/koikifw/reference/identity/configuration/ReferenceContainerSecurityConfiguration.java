package org.koikifw.reference.identity.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Reference-only opt-in container probes; all other paths keep their existing chains. */
@Configuration(proxyBeanMethods = false)
@Profile("d3-container")
public class ReferenceContainerSecurityConfiguration {
    @Bean
    @Order(-20)
    SecurityFilterChain referenceContainerProbeSecurityFilterChain(HttpSecurity http) throws Exception {
        String live = "/actuator/health/liveness";
        String ready = "/actuator/health/readiness";
        http.securityMatcher(live, ready);
        http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(HttpMethod.GET, live, ready).permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}

package org.koikifw.starter.security.internal;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/** Provides the fail-closed fallback for servlet applications using KOIKI Security. */
@AutoConfiguration(
        beforeName =
                "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, SecurityFilterChain.class})
@EnableMethodSecurity
public class KoikiSecurityAutoConfiguration {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "koikiSecurityFallbackFilterChain")
    SecurityFilterChain koikiSecurityFallbackFilterChain(HttpSecurity http) throws Exception {
        http.csrf(withDefaults());
        http.headers(withDefaults());
        http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());
        http.exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}

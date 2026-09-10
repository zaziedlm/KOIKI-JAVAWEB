package org.koikifw.reference.identity.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/** Assembles the Reference browser security boundary. */
@Configuration(proxyBeanMethods = false)
public class ReferenceSecurityConfiguration {

    @Bean
    CompromisedPasswordChecker referenceCompromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

    @Bean
    CookieSerializer referenceCookieSerializer() {
        return new DefaultCookieSerializer();
    }

    @Bean
    @Order(0)
    SecurityFilterChain referenceIdentitySecurityFilterChain(
            HttpSecurity http, Customizer<HttpSecurity> koikiSessionLogoutCustomizer)
            throws Exception {
        http.securityMatcher("/login", "/logout", "/identity/**");
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/login")
                .permitAll()
                .requestMatchers("/identity/**")
                .hasAuthority("IDENTITY:ADMIN")
                .anyRequest()
                .authenticated());
        http.formLogin(form -> form.defaultSuccessUrl("/identity/users", true));
        http.csrf(withDefaults());
        http.headers(withDefaults());
        koikiSessionLogoutCustomizer.customize(http);
        return http.build();
    }
}

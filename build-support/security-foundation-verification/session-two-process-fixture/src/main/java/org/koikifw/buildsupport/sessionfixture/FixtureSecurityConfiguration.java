package org.koikifw.buildsupport.sessionfixture;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
class FixtureSecurityConfiguration {

    @Bean
    CookieSerializer fixtureCookieSerializer() {
        return new DefaultCookieSerializer();
    }

    @Bean
    @Order(0)
    SecurityFilterChain fixtureBootstrapSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(
                "/fixture/readiness", "/fixture/setup", "/fixture/setup-external", "/fixture/reset");
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        http.csrf(csrf -> csrf.ignoringRequestMatchers(
                "/fixture/setup", "/fixture/setup-external", "/fixture/reset"));
        http.headers(withDefaults());
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain fixtureSessionSecurityFilterChain(
            HttpSecurity http, Customizer<HttpSecurity> sessionLogoutCustomizer)
            throws Exception {
        http.securityMatcher(
                "/login", "/logout", "/fixture/session", "/fixture/csrf", "/fixture/admin/**");
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/login").permitAll()
                .requestMatchers("/fixture/session", "/fixture/csrf").authenticated()
                .requestMatchers("/fixture/admin/**").hasAuthority("IDENTITY:ADMIN")
                .anyRequest().authenticated());
        http.formLogin(withDefaults());
        sessionLogoutCustomizer.customize(http);
        return http.build();
    }

    @Bean
    CompromisedPasswordChecker fixtureCompromisedPasswordChecker() {
        return password -> new CompromisedPasswordDecision(false);
    }
}

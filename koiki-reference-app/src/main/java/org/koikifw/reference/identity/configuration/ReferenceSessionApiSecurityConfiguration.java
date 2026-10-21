package org.koikifw.reference.identity.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;

/** Same-origin Session API; never interprets Bearer credentials. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class)
public class ReferenceSessionApiSecurityConfiguration {
    @Bean
    CookieCsrfTokenRepository referenceSpaCsrfTokenRepository(ServerProperties properties) {
        var cookie = properties.getServlet().getSession().getCookie();
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(builder -> {
            builder.secure(Boolean.TRUE.equals(cookie.getSecure()));
            if (cookie.getSameSite() != null) {
                builder.sameSite(cookie.getSameSite().attributeValue());
            }
        });
        return repository;
    }

    @Bean
    ReferenceSessionApiProblemWriter referenceSessionApiProblemWriter() {
        return new ReferenceSessionApiProblemWriter();
    }

    @Bean
    @Order(-10)
    SecurityFilterChain referenceSessionApiSecurityFilterChain(HttpSecurity http,
            CookieCsrfTokenRepository referenceSpaCsrfTokenRepository,
            CookieSerializer referenceCookieSerializer,
            Customizer<HttpSecurity> koikiSessionLogoutCustomizer,
            ReferenceSessionApiProblemWriter problems) throws Exception {
        http.securityMatcher("/spa-api/**");
        http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.NEVER));
        http.requestCache(cache -> cache.disable());
        http.csrf(csrf -> csrf.spa().csrfTokenRepository(referenceSpaCsrfTokenRepository));
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/spa-api/v1/expense-options", "/spa-api/v1/expense-requests",
                        "/spa-api/v1/expense-requests/**").hasAuthority("EXPENSE:APPLY")
                .anyRequest().authenticated());
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> problems.write(
                        request, response, HttpStatus.UNAUTHORIZED,
                        "Session authentication is required.", "KOIKI-REF-SESSION-001"))
                .accessDeniedHandler((request, response, exception) -> problems.write(
                        request, response, HttpStatus.FORBIDDEN,
                        exception instanceof CsrfException ? "CSRF validation failed." : "The user is not authorized.",
                        exception instanceof CsrfException ? "KOIKI-REF-CSRF-001" : "KOIKI-REF-SESSION-002")));
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                response.setHeader("Cache-Control", "no-store");
                chain.doFilter(request, response);
            }
        }, CsrfFilter.class);
        koikiSessionLogoutCustomizer.customize(http);
        http.logout(logout -> logout
                .logoutRequestMatcher(request -> "POST".equals(request.getMethod())
                        && (request.getContextPath() + "/spa-api/logout").equals(request.getRequestURI()))
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        http.addFilterBefore(new ReferenceSessionLogoutBoundary("/spa-api/logout",
                referenceSpaCsrfTokenRepository, referenceCookieSerializer, problems), LogoutFilter.class);
        return http.build();
    }
}

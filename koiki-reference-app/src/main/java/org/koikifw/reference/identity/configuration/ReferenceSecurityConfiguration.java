package org.koikifw.reference.identity.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
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
        http.securityMatcher(
                "/login", "/logout", "/", "/identity/**", "/master/**", "/expenses/**",
                "/koiki-web/**", "/webjars/**");
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/login", "/koiki-web/**", "/webjars/**")
                .permitAll()
                .requestMatchers("/identity/**")
                .hasAuthority("IDENTITY:ADMIN")
                .requestMatchers("/master/**")
                .hasAuthority("MASTER:ADMIN")
                .requestMatchers("/expenses/**", "/")
                .authenticated()
                .anyRequest()
                .authenticated());
        http.formLogin(form -> form.defaultSuccessUrl("/", true));
        http.csrf(withDefaults());
        http.headers(withDefaults());
        RequestHeaderRequestMatcher htmxRequest =
                new RequestHeaderRequestMatcher("HX-Request", "true");
        http.exceptionHandling(exceptions -> exceptions
                .defaultAccessDeniedHandlerFor((request, response, exception) -> {
                    response.setStatus(403);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.TEXT_HTML_VALUE);
                    response.getWriter().write(
                            "<section class=\"error\" role=\"alert\" tabindex=\"-1\" "
                                    + "data-koiki-focus>この操作は許可されていません。</section>");
                }, htmxRequest));
        koikiSessionLogoutCustomizer.customize(http);
        return http.build();
    }
}

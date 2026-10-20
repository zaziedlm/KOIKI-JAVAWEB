package org.koikifw.reference.identity.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
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
@EnableConfigurationProperties(ServerProperties.class)
public class ReferenceSecurityConfiguration {

    @Bean
    CompromisedPasswordChecker referenceCompromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

    @Bean
    CookieSerializer referenceCookieSerializer(ServerProperties serverProperties) {
        var cookie = serverProperties.getServlet().getSession().getCookie();
        var serializer = new DefaultCookieSerializer();
        var mapper = PropertyMapper.get();
        mapper.from(cookie::getName).to(serializer::setCookieName);
        mapper.from(cookie::getDomain).to(serializer::setDomainName);
        mapper.from(cookie::getPath).to(serializer::setCookiePath);
        mapper.from(cookie::getHttpOnly).to(serializer::setUseHttpOnlyCookie);
        mapper.from(cookie::getSecure).to(serializer::setUseSecureCookie);
        mapper.from(cookie::getMaxAge).asInt(java.time.Duration::getSeconds).to(serializer::setCookieMaxAge);
        mapper.from(cookie::getSameSite).to(sameSite -> serializer.setSameSite(sameSite.attributeValue()));
        mapper.from(cookie::getPartitioned).to(serializer::setPartitioned);
        return serializer;
    }

    @Bean
    @Order(0)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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

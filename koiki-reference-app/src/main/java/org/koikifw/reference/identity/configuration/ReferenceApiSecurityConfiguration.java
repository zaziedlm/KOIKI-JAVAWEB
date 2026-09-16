package org.koikifw.reference.identity.configuration;

import java.util.List;
import org.koikifw.identity.IdentityQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

/** P3-C1 Reference-only stateless Bearer boundary for {@code /api/**}. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "koiki.reference.api.bearer",
        name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(ReferenceBearerProperties.class)
public class ReferenceApiSecurityConfiguration {

    @Bean
    ReferenceApiSecurityProblemWriter referenceApiSecurityProblemWriter(ObjectMapper objectMapper) {
        return new ReferenceApiSecurityProblemWriter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder referenceApiJwtDecoder(ReferenceBearerProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(properties.issuer()).build();
        configureValidators(decoder, properties);
        return decoder;
    }

    static void configureValidators(
            NimbusJwtDecoder decoder, ReferenceBearerProperties properties) {
        var defaults = JwtValidators.createDefaultWithIssuer(properties.issuer());
        var audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                values -> values != null && values.contains(properties.audience()));
        var accessToken = (org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt>) jwt ->
                "access".equals(jwt.getClaimAsString("token_use"))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token", "Access token required", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience, accessToken));
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain referenceApiSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            IdentityQuery identities,
            ReferenceApiSecurityProblemWriter problems) throws Exception {
        ReferenceJwtAuthenticationConverter authenticationConverter =
                new ReferenceJwtAuthenticationConverter(identities);
        http.securityMatcher("/api/**");
        http.sessionManagement(
                sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.securityContext(context -> context.requireExplicitSave(false));
        http.requestCache(cache -> cache.disable());
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(requests -> requests
                .anyRequest()
                .hasAuthority(ReferenceJwtAuthenticationConverter.APPLY_PERMISSION));
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                        problems.unauthorized(request, response))
                .accessDeniedHandler((request, response, exception) ->
                        problems.forbidden(request, response)));
        http.oauth2ResourceServer(resourceServer -> resourceServer
                .authenticationEntryPoint((request, response, exception) ->
                        problems.unauthorized(request, response))
                .accessDeniedHandler((request, response, exception) ->
                        problems.forbidden(request, response))
                .jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(authenticationConverter)));
        return http.build();
    }
}

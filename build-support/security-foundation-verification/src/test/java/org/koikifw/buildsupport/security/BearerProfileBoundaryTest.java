package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class BearerProfileBoundaryTest {

    private static final String ISSUER = "https://issuer.fixture.invalid";
    private static final String AUDIENCE = "fixture-api";
    private static final String ALLOWED_ORIGIN = "https://spa.fixture.invalid";
    private static final String READ_SCOPE = "fixture.read";
    private static final String READ_AUTHORITY = "fixture:read";

    private final KeyPair signingKey = rsaKeyPair();
    private final JwtEncoder encoder = encoder(signingKey);
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KoikiSecurityAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class))
            .withBean(JwtDecoder.class, () -> decoder(signingKey))
            .withUserConfiguration(FixtureWebConfiguration.class, BearerSecurityConfiguration.class);

    @Test
    void acceptsActualSignedAccessTokenAndReachesProtectedProcessing() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);
            InvocationCounter counter = context.getBean(InvocationCounter.class);

            mvc.perform(get("/fixture/api/read")
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(encoder, READ_SCOPE))))
                    .andExpect(status().isOk())
                    .andExpect(content().string("fixture-api-ok"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));

            assertThat(counter.value()).isEqualTo(1);
        });
    }

    @Test
    void rejectsInvalidSignatureIssuerAudienceAndTimeClaimsWithoutLeakingToken() {
        JwtEncoder otherEncoder = encoder(rsaKeyPair());
        List<String> rejectedTokens = List.of(
                accessToken(otherEncoder, READ_SCOPE),
                token(encoder, ISSUER + "/other", List.of(AUDIENCE), READ_SCOPE,
                        "access", Instant.now().minusSeconds(1), Instant.now().plusSeconds(300)),
                token(encoder, ISSUER, List.of("other-api"), READ_SCOPE,
                        "access", Instant.now().minusSeconds(1), Instant.now().plusSeconds(300)),
                token(encoder, ISSUER, List.of(AUDIENCE), READ_SCOPE,
                        "access", Instant.now().minusSeconds(600), Instant.now().minusSeconds(300)),
                token(encoder, ISSUER, List.of(AUDIENCE), READ_SCOPE,
                        "access", Instant.now().plusSeconds(300), Instant.now().plusSeconds(600)));

        runner.run(context -> {
            MockMvc mvc = mockMvc(context);
            InvocationCounter counter = context.getBean(InvocationCounter.class);

            for (String rejectedToken : rejectedTokens) {
                MvcResult result = mvc.perform(get("/fixture/api/read")
                                .header(HttpHeaders.AUTHORIZATION, bearer(rejectedToken)))
                        .andExpect(status().isUnauthorized())
                        .andReturn();
                assertSafeResponse(result, rejectedToken);
            }

            assertThat(counter.value()).isZero();
        });
    }

    @Test
    void mapsOnlyExactAllowlistedScopeAndRejectsUnknownCaseOrMalformedValues() {
        List<String> rejectedScopes = List.of("fixture.write", "Fixture.Read", "fixture.read:extra");

        runner.run(context -> {
            MockMvc mvc = mockMvc(context);
            InvocationCounter counter = context.getBean(InvocationCounter.class);

            for (String rejectedScope : rejectedScopes) {
                mvc.perform(get("/fixture/api/read")
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(encoder, rejectedScope))))
                        .andExpect(status().isForbidden());
            }

            assertThat(counter.value()).isZero();
        });
    }

    @Test
    void doesNotTreatCookieQueryRawEdgeHeaderOrIdTokenAsBearerAuthentication() {
        String accessToken = accessToken(encoder, READ_SCOPE);
        String idToken = token(encoder, ISSUER, List.of(AUDIENCE), READ_SCOPE,
                "id", Instant.now().minusSeconds(1), Instant.now().plusSeconds(300));

        runner.run(context -> {
            MockMvc mvc = mockMvc(context);
            InvocationCounter counter = context.getBean(InvocationCounter.class);

            mvc.perform(get("/fixture/api/read").cookie(new Cookie("fixture-session", accessToken)))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/fixture/api/read").queryParam("access_token", accessToken))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/fixture/api/read").header("X-Amzn-Oidc-Data", accessToken))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/fixture/api/read")
                            .header(HttpHeaders.AUTHORIZATION, bearer(idToken)))
                    .andExpect(status().isUnauthorized());

            assertThat(counter.value()).isZero();
        });
    }

    @Test
    void allowsOnlyExplicitCorsOriginMethodAndHeader() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);

            mvc.perform(options("/fixture/api/read")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
            mvc.perform(options("/fixture/api/read")
                            .header(HttpHeaders.ORIGIN, "https://unknown.fixture.invalid")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                    .andExpect(status().isForbidden());
            mvc.perform(options("/fixture/api/read")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                    .andExpect(status().isForbidden());
            mvc.perform(options("/fixture/api/read")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Unapproved"))
                    .andExpect(status().isForbidden());
        });
    }

    private String accessToken(JwtEncoder tokenEncoder, String scope) {
        return token(tokenEncoder, ISSUER, List.of(AUDIENCE), scope,
                "access", Instant.now().minusSeconds(1), Instant.now().plusSeconds(300));
    }

    private static String token(
            JwtEncoder tokenEncoder,
            String issuer,
            List<String> audience,
            String scope,
            String tokenUse,
            Instant notBefore,
            Instant expiresAt) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(300) : now;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("fixture-subject")
                .audience(audience)
                .issuedAt(issuedAt)
                .notBefore(notBefore)
                .expiresAt(expiresAt)
                .claim("scope", scope)
                .claim("token_use", tokenUse)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return tokenEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static JwtEncoder encoder(KeyPair keyPair) {
        return NimbusJwtEncoder.withKeyPair(
                        (RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate())
                .build();
    }

    private static JwtDecoder decoder(KeyPair keyPair) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(ISSUER);
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, values -> values != null && values.contains(AUDIENCE));
        OAuth2TokenValidator<Jwt> accessToken = jwt -> "access".equals(jwt.getClaimAsString("token_use"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Access token required", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience, accessToken));
        return decoder;
    }

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create test-only RSA key pair", ex);
        }
    }

    private static MockMvc mockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static void assertSafeResponse(MvcResult result, String rejectedToken) throws Exception {
        assertThat(result.getResponse().getContentAsString()).doesNotContain(rejectedToken);
        assertThat(result.getResponse().getHeaderNames())
                .allSatisfy(name -> assertThat(result.getResponse().getHeaders(name))
                        .noneMatch(value -> value.contains(rejectedToken)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class FixtureWebConfiguration {

        @Bean
        InvocationCounter invocationCounter() {
            return new InvocationCounter();
        }

        @Bean
        FixtureApiController fixtureApiController(InvocationCounter invocationCounter) {
            return new FixtureApiController(invocationCounter);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BearerSecurityConfiguration {

        @Bean
        @Order(1)
        SecurityFilterChain bearerSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
            http.securityMatcher("/fixture/api/**");
            http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            http.csrf(csrf -> csrf.disable());
            http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
            http.authorizeHttpRequests(requests -> requests
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().hasAuthority(READ_AUTHORITY));
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())));
            return http.build();
        }

        private static UrlBasedCorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of(ALLOWED_ORIGIN));
            configuration.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.OPTIONS.name()));
            configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION));
            configuration.setAllowCredentials(false);
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/fixture/api/**", configuration);
            return source;
        }

        private static JwtAuthenticationConverter jwtAuthenticationConverter() {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(BearerSecurityConfiguration::authorities);
            return converter;
        }

        private static Collection<GrantedAuthority> authorities(Jwt jwt) {
            String scope = jwt.getClaimAsString("scope");
            if (READ_SCOPE.equals(scope)) {
                return List.of(new SimpleGrantedAuthority(READ_AUTHORITY));
            }
            return List.of();
        }
    }

    @RestController
    static class FixtureApiController {

        private final InvocationCounter invocationCounter;

        FixtureApiController(InvocationCounter invocationCounter) {
            this.invocationCounter = invocationCounter;
        }

        @GetMapping("/fixture/api/read")
        String read() {
            invocationCounter.increment();
            return "fixture-api-ok";
        }
    }

    static class InvocationCounter {

        private final AtomicInteger count = new AtomicInteger();

        void increment() {
            count.incrementAndGet();
        }

        int value() {
            return count.get();
        }
    }
}

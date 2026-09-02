package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class OidcProfileCoexistenceTest {

    private static final String CLIENT_ID = "fixture-bff";
    private static final String CLIENT_SECRET = "fixture-runtime-only-secret";

    @Test
    void authorizationCodeCreatesBrowserSessionWithoutAuthenticatingApiOrRawBearerBrowserRequest() throws Exception {
        try (FixtureOidcIssuer issuer = new FixtureOidcIssuer()) {
            runner(issuer).run(context -> {
                MockMvc mvc = mockMvc(context);
                jakarta.servlet.http.HttpSession session = new org.springframework.mock.web.MockHttpSession();

                MvcResult authorization = mvc.perform(get("/oauth2/authorization/fixture").session((org.springframework.mock.web.MockHttpSession) session))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
                URI redirect = URI.create(authorization.getResponse().getHeader(HttpHeaders.LOCATION));
                Map<String, String> parameters = queryParameters(redirect);
                assertThat(redirect.getPath()).isEqualTo("/authorize");
                assertThat(parameters).containsEntry("client_id", CLIENT_ID).containsKeys("state", "nonce");
                issuer.useNonce(parameters.get("nonce"));

                mvc.perform(get("/login/oauth2/code/fixture")
                                .session((org.springframework.mock.web.MockHttpSession) session)
                                .queryParam("code", "fixture-code")
                                .queryParam("state", parameters.get("state")))
                        .andExpect(status().is3xxRedirection());

                mvc.perform(get("/fixture/browser/read").session((org.springframework.mock.web.MockHttpSession) session))
                        .andExpect(status().isOk())
                        .andExpect(content().string("fixture-browser-ok"));
                mvc.perform(get("/fixture/api/read").session((org.springframework.mock.web.MockHttpSession) session))
                        .andExpect(status().isUnauthorized());
                mvc.perform(get("/fixture/browser/read")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer opaque-fixture-value"))
                        .andExpect(status().is3xxRedirection());
                mvc.perform(get("/fixture/unmatched").session((org.springframework.mock.web.MockHttpSession) session))
                        .andExpect(status().isForbidden());

                assertThat(issuer.tokenExchangeCount()).isEqualTo(1);
            });
        }
    }

    @Test
    void stateMismatchFailsGenericallyBeforeTokenExchange() throws Exception {
        try (FixtureOidcIssuer issuer = new FixtureOidcIssuer()) {
            runner(issuer).run(context -> {
                MockMvc mvc = mockMvc(context);
                org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();

                mvc.perform(get("/oauth2/authorization/fixture").session(session))
                        .andExpect(status().is3xxRedirection());
                MvcResult failure = mvc.perform(get("/login/oauth2/code/fixture")
                                .session(session)
                                .queryParam("code", "fixture-rejected-code")
                                .queryParam("state", "fixture-invalid-state"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();

                assertThat(failure.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/login?error");
                assertThat(failure.getResponse().getContentAsString())
                        .doesNotContain("fixture-rejected-code", CLIENT_SECRET);
                assertThat(issuer.tokenExchangeCount()).isZero();
            });
        }
    }

    private static WebApplicationContextRunner runner(FixtureOidcIssuer issuer) {
        return new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        KoikiSecurityAutoConfiguration.class,
                        SecurityAutoConfiguration.class,
                        ServletWebSecurityAutoConfiguration.class))
                .withBean(FixtureOidcIssuer.class, () -> issuer)
                .withUserConfiguration(FixtureWebConfiguration.class, ProfileSecurityConfiguration.class);
    }

    private static MockMvc mockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static Map<String, String> queryParameters(URI uri) {
        Map<String, String> parameters = new ConcurrentHashMap<>();
        Arrays.stream(uri.getRawQuery().split("&"))
                .map(value -> value.split("=", 2))
                .forEach(pair -> parameters.put(
                        URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        return parameters;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class FixtureWebConfiguration {

        @Bean
        FixtureController fixtureController() {
            return new FixtureController();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProfileSecurityConfiguration {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository(FixtureOidcIssuer issuer) {
            ClientRegistration registration = ClientRegistration.withRegistrationId("fixture")
                    .clientId(CLIENT_ID)
                    .clientSecret(CLIENT_SECRET)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile")
                    .authorizationUri(issuer.baseUrl() + "/authorize")
                    .tokenUri(issuer.baseUrl() + "/token")
                    .jwkSetUri(issuer.baseUrl() + "/jwks")
                    .issuerUri(issuer.baseUrl())
                    .userNameAttributeName("sub")
                    .clientName("Fixture OIDC")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }

        @Bean
        OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
            return new InMemoryOAuth2AuthorizedClientService(registrations);
        }

        @Bean
        OAuth2AuthorizedClientRepository authorizedClientRepository(OAuth2AuthorizedClientService service) {
            return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(service);
        }

        @Bean
        JwtDecoder fixtureJwtDecoder(FixtureOidcIssuer issuer) throws Exception {
            return issuer.jwtDecoder();
        }

        @Bean
        @Order(1)
        SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
            http.securityMatcher("/fixture/api/**");
            http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            http.csrf(csrf -> csrf.disable());
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated());
            http.exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.decoder(jwtDecoder)));
            return http.build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain browserSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/fixture/browser/**", "/oauth2/**", "/login/**");
            http.authorizeHttpRequests(requests -> requests
                    .requestMatchers("/oauth2/**", "/login/**").permitAll()
                    .anyRequest().authenticated());
            http.oauth2Login(Customizer.withDefaults());
            return http.build();
        }
    }

    @RestController
    static class FixtureController {

        @GetMapping("/fixture/browser/read")
        String browserRead() {
            return "fixture-browser-ok";
        }

        @GetMapping("/fixture/api/read")
        String apiRead() {
            return "fixture-api-ok";
        }

        @GetMapping("/fixture/unmatched")
        String unmatched() {
            return "fixture-unmatched";
        }
    }

    static final class FixtureOidcIssuer implements AutoCloseable {

        private final HttpServer server;
        private final RSAKey signingKey;
        private final JwtEncoder encoder;
        private volatile String nonce;
        private int tokenExchangeCount;

        FixtureOidcIssuer() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            signingKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID("fixture-key")
                    .build();
            encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(signingKey)));
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/token", this::tokenEndpoint);
            server.createContext("/jwks", this::jwksEndpoint);
            server.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void useNonce(String nonce) {
            this.nonce = nonce;
        }

        int tokenExchangeCount() {
            return tokenExchangeCount;
        }

        JwtDecoder jwtDecoder() throws Exception {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(signingKey.toRSAPublicKey()).build();
            OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(baseUrl());
            OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                    "aud", values -> values != null && values.contains("fixture-api"));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
            return decoder;
        }

        private void tokenEndpoint(HttpExchange exchange) throws IOException {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String authorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (!"POST".equals(exchange.getRequestMethod())
                    || !requestBody.contains("grant_type=authorization_code")
                    || !requestBody.contains("code=fixture-code")
                    || authorization == null
                    || !authorization.startsWith("Basic ")
                    || nonce == null) {
                respond(exchange, 400, "{\"error\":\"invalid_grant\"}", "application/json");
                return;
            }
            tokenExchangeCount++;
            Instant now = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(baseUrl())
                    .subject("fixture-subject")
                    .audience(List.of(CLIENT_ID))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300))
                    .claim("nonce", nonce)
                    .build();
            String idToken = encoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(SignatureAlgorithm.RS256).keyId(signingKey.getKeyID()).build(), claims))
                    .getTokenValue();
            String body = "{\"access_token\":\"fixture-access-token\",\"token_type\":\"Bearer\","
                    + "\"expires_in\":300,\"scope\":\"openid profile\",\"id_token\":\"" + idToken + "\"}";
            respond(exchange, 200, body, "application/json");
        }

        private void jwksEndpoint(HttpExchange exchange) throws IOException {
            respond(exchange, 200, new JWKSet(signingKey.toPublicJWK()).toString(), "application/json");
        }

        private static void respond(HttpExchange exchange, int status, String body, String contentType)
                throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}

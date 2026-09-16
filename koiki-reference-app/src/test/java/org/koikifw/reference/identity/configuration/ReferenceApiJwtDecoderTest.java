package org.koikifw.reference.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class ReferenceApiJwtDecoderTest {

    private static final String KEY_ID = "p3-c1-fixture";
    private static final String AUDIENCE = "koiki-reference-api";
    private static final KeyPair SIGNING_KEY = rsaKeyPair();
    private static final KeyPair OTHER_KEY = rsaKeyPair();
    private static HttpServer issuerServer;
    private static String issuer;

    @BeforeAll
    static void startIssuer() throws IOException {
        issuerServer = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        issuer = "http://127.0.0.1:" + issuerServer.getAddress().getPort();
        issuerServer.createContext("/.well-known/openid-configuration", exchange -> respond(
                exchange,
                """
                {"issuer":"%s","jwks_uri":"%s/jwks"}
                """.formatted(issuer, issuer)));
        issuerServer.createContext("/jwks", exchange -> respond(exchange, jwks(SIGNING_KEY)));
        issuerServer.start();
    }

    @AfterAll
    static void stopIssuer() {
        issuerServer.stop(0);
    }

    @Test
    void acceptsSignedAccessTokenAndRejectsSignatureIssuerAudienceTimeAndIdToken() {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) new ReferenceApiSecurityConfiguration()
                .referenceApiJwtDecoder(new ReferenceBearerProperties(true, issuer, AUDIENCE));
        Instant now = Instant.now();

        var decoded = decoder.decode(token(
                SIGNING_KEY,
                issuer,
                List.of(AUDIENCE),
                "access",
                now.minusSeconds(1),
                now.plusSeconds(300)));
        assertThat(decoded.getIssuer()).isNotNull().hasToString(issuer);
        assertThat(decoded.getAudience()).containsExactly(AUDIENCE);

        List<String> rejected = List.of(
                token(OTHER_KEY, issuer, List.of(AUDIENCE), "access",
                        now.minusSeconds(1), now.plusSeconds(300)),
                token(SIGNING_KEY, issuer + "/other", List.of(AUDIENCE), "access",
                        now.minusSeconds(1), now.plusSeconds(300)),
                token(SIGNING_KEY, issuer, List.of("other-api"), "access",
                        now.minusSeconds(1), now.plusSeconds(300)),
                token(SIGNING_KEY, issuer, List.of(AUDIENCE), "access",
                        now.minusSeconds(600), now.minusSeconds(300)),
                token(SIGNING_KEY, issuer, List.of(AUDIENCE), "access",
                        now.plusSeconds(300), now.plusSeconds(600)),
                token(SIGNING_KEY, issuer, List.of(AUDIENCE), "id",
                        now.minusSeconds(1), now.plusSeconds(300)));

        assertThat(rejected).allSatisfy(value -> assertThatThrownBy(() -> decoder.decode(value))
                .isInstanceOf(JwtException.class));
    }

    private static String token(
            KeyPair keyPair,
            String tokenIssuer,
            List<String> audience,
            String tokenUse,
            Instant notBefore,
            Instant expiresAt) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(300) : now;
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(tokenIssuer)
                .subject("fixture-subject")
                .audience(audience)
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiresAt))
                .claim("scope", "expense.apply")
                .claim("token_use", tokenUse)
                .claim("koiki_user_id", "36000000-0000-0000-0000-000000000001")
                .build();
        SignedJWT signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
        try {
            signed.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
            return signed.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign test-only JWT", exception);
        }
    }

    private static String jwks(KeyPair keyPair) {
        RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
        return """
                {"keys":[{"kty":"RSA","kid":"%s","use":"sig","alg":"RS256","n":"%s","e":"%s"}]}
                """.formatted(KEY_ID, base64Url(key.getModulus()), base64Url(key.getPublicExponent()));
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        int offset = bytes.length > 1 && bytes[0] == 0 ? 1 : 0;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(java.util.Arrays.copyOfRange(bytes, offset, bytes.length));
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var response = exchange.getResponseBody()) {
            response.write(bytes);
        }
    }

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create test-only RSA key pair", exception);
        }
    }
}

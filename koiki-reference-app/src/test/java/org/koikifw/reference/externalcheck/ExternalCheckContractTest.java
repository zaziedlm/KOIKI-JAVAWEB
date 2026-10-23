package org.koikifw.reference.externalcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.reference.externalcheck.adapter.outbound.external.HttpExternalCheckAdapter;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.koikifw.reference.externalcheck.configuration.ExternalCheckProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.json.JsonMapper;

class ExternalCheckContractTest {
    @Test
    void acceptsOnlyReferenceCurrencyPairs() {
        assertThat(new Query("USD", "JPY", LocalDate.of(2026, 10, 22)).base()).isEqualTo("USD");
        assertThat(new Query("EUR", "JPY", LocalDate.of(2026, 10, 22)).quote()).isEqualTo("JPY");
    }

    @ParameterizedTest
    @ValueSource(strings = {"usd", "GBP", "JPY", "US", "USD "})
    void rejectsUnsupportedInput(String base) {
        assertThatThrownBy(() -> new Query(base, "JPY", LocalDate.of(2026, 10, 22)))
                .isInstanceOf(ExternalCheckFailure.class);
    }

    @Test
    void preservesDecimalPrecisionAndRejectsMalformedProviderValues() throws Exception {
        try (var fixture = new ExternalRateFixture(); var adapter = new HttpExternalCheckAdapter(
                new ExternalCheckProperties(fixture.baseUri(), Duration.ofMillis(200), Duration.ofMillis(300)),
                JsonMapper.builder().build())) {
            String prefix = "{\"base\":\"USD\",\"quote\":\"JPY\",\"date\":\"2026-10-22\",\"rate\":";
            fixture.rawBody = prefix + "\"9007199254740993.12345678\"}";
            var query = new Query("USD", "JPY", LocalDate.of(2026, 10, 22));
            UUID operation = UUID.randomUUID();
            assertThat(adapter.lookup(operation, query).rate()).isEqualTo("9007199254740993.12345678");
            assertThat(adapter.lookup(operation, query).operationId()).isEqualTo(operation);
            for (String value : new String[]{"0", "-1", "1e2", "1.123456789", "NaN"}) {
                fixture.rawBody = prefix + "\"" + value + "\"}";
                assertThatThrownBy(() -> adapter.lookup(operation, query)).isInstanceOf(ExternalCheckFailure.class);
            }
            fixture.rawBody = prefix + "150}";
            assertThatThrownBy(() -> adapter.lookup(operation, query)).isInstanceOf(ExternalCheckFailure.class);
        }
    }

    @Test
    void adoptsOverridableDefaultsAndRefusesUnsafeConfiguration() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverride",
                Map.of("koiki.api.http.read-timeout", "100ms")));
        new org.koikifw.starter.api.internal.KoikiApiDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());
        assertThat(environment.getProperty("koiki.api.http.read-timeout")).isEqualTo("100ms");
        assertThat(environment.getProperty("koiki.api.http.connect-timeout")).isEqualTo("200ms");
        for (Duration duration : new Duration[]{Duration.ZERO, Duration.ofMillis(-1), Duration.ofSeconds(3)}) {
            assertThatThrownBy(() -> new ExternalCheckProperties(URI.create("http://127.0.0.1:1234"),
                    duration, Duration.ofMillis(300))).isInstanceOf(IllegalArgumentException.class);
        }
        for (String uri : new String[]{"http://example.test:1234", "http://localhost:1234", "http://user@127.0.0.1:1234",
                "http://127.0.0.1:1234?target=other", "http://127.0.0.1:1234/other", "https://127.0.0.1:1234"}) {
            assertThatThrownBy(() -> new ExternalCheckProperties(URI.create(uri), Duration.ofMillis(200),
                    Duration.ofMillis(300))).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

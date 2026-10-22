package org.koikifw.reference.externalcheck.configuration;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Opt-in loopback provider and explicitly adopted KOIKI finite HTTP defaults. */
public record ExternalCheckProperties(URI providerBaseUri, Duration connectTimeout, Duration readTimeout) {
    public ExternalCheckProperties {
        Objects.requireNonNull(providerBaseUri);
        validateTimeout(connectTimeout);
        validateTimeout(readTimeout);
        String host = providerBaseUri.getHost();
        if (!"http".equals(providerBaseUri.getScheme())
                || !("127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host))
                || providerBaseUri.getPort() < 1 || providerBaseUri.getPort() > 65535
                || providerBaseUri.getRawUserInfo() != null || providerBaseUri.getRawQuery() != null
                || providerBaseUri.getRawFragment() != null
                || !("".equals(providerBaseUri.getPath()) || "/".equals(providerBaseUri.getPath()))) {
            throw new IllegalArgumentException("External check requires a numeric loopback base URI");
        }
    }

    private static void validateTimeout(Duration value) {
        Objects.requireNonNull(value);
        if (value.toMillis() < 1 || value.compareTo(Duration.ofSeconds(2)) > 0) {
            throw new IllegalArgumentException("External check timeout must be 1ms through 2s");
        }
    }
}

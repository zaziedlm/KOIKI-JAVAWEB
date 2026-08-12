package dev.koiki.walkingskeleton.flyway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ws.flyway")
public record FlywayValidationProperties(int expectedKoikiVersion) {
}

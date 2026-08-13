package org.koikifw.archunit.fixture.v1.violation.simple.application;

import org.springframework.web.client.RestTemplate;

public class LegacyHttpClient {
    private final RestTemplate client = new RestTemplate();
}

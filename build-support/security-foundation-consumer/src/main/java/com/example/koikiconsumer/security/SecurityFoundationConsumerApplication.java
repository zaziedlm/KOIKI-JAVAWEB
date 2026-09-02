package com.example.koikiconsumer.security;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Executable Customer-like application used only for Phase 2 Gate A acceptance. */
@SpringBootApplication
public class SecurityFoundationConsumerApplication {

    private static final String PROBE_ARGUMENT = "--koiki.consumer.runtime-probe=";
    private static final String SUCCESS_MARKER = "KOIKI_SECURITY_CONSUMER_RUNTIME_SUCCESS";

    public static void main(String[] args) throws ClassNotFoundException {
        Integer expectedFeature = expectedFeature(args);
        SpringApplication application = new SpringApplication(SecurityFoundationConsumerApplication.class);
        if (expectedFeature != null) {
            application.setWebApplicationType(WebApplicationType.NONE);
        }

        ConfigurableApplicationContext context = application.run(args);
        if (expectedFeature != null) {
            verifyRuntime(expectedFeature);
            System.exit(SpringApplication.exit(context));
        }
    }

    private static @Nullable Integer expectedFeature(String[] args) {
        return Arrays.stream(args)
                .filter(argument -> argument.startsWith(PROBE_ARGUMENT))
                .map(argument -> Integer.valueOf(argument.substring(PROBE_ARGUMENT.length())))
                .findFirst()
                .orElse(null);
    }

    private static void verifyRuntime(int expectedFeature) throws ClassNotFoundException {
        int actualFeature = Runtime.version().feature();
        if (actualFeature != expectedFeature) {
            throw new IllegalStateException(
                    "Java runtime feature mismatch: expected=" + expectedFeature + " actual=" + actualFeature);
        }
        Class.forName("org.springframework.security.web.SecurityFilterChain");
        Class.forName("org.springframework.security.oauth2.client.registration.ClientRegistration");
        Class.forName("org.springframework.security.oauth2.jwt.JwtDecoder");
        System.out.println(SUCCESS_MARKER + " expected=" + expectedFeature + " actual=" + actualFeature);
    }
}

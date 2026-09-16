package org.koikifw.reference.identity.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Reference-only settings for the P3-C1 representative Bearer profile. */
@Validated
@ConfigurationProperties("koiki.reference.api.bearer")
record ReferenceBearerProperties(
        boolean enabled,
        @NotBlank String issuer,
        @NotBlank String audience) {}

package org.koikifw.starter.api.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Internal activation of Spring Framework resilience annotations for Servlet API applications. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(EnableResilientMethods.class)
@ConditionalOnProperty(prefix = "koiki.api", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "koiki.api.resilience", name = "enabled", matchIfMissing = true)
@EnableResilientMethods
public class KoikiApiAutoConfiguration {
}

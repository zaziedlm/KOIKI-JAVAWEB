package org.koikifw.reference.expense.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Limits path-segment API version resolution to the Reference REST boundary. */
@Configuration(proxyBeanMethods = false)
class ExpenseApiVersionConfiguration implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.usePathSegment(
                1, path -> path.pathWithinApplication().value().startsWith("/api/"));
    }
}

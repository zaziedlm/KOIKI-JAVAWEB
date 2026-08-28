package org.koikifw.starter.api.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Internal KOIKI API runtime configuration for Servlet applications. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler.class)
@ConditionalOnProperty(prefix = "koiki.api", name = "enabled", matchIfMissing = true)
public class KoikiApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
    @ConditionalOnProperty(prefix = "koiki.api.problem-details", name = "enabled", matchIfMissing = true)
    KoikiProblemDetailsExceptionHandler koikiProblemDetailsExceptionHandler() {
        return new KoikiProblemDetailsExceptionHandler();
    }
}

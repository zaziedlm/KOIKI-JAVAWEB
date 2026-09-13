package org.koikifw.starter.webmvc.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Internal MVC defaults for KOIKI-owned classpath resources. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class KoikiWebMvcAutoConfiguration {

    @Bean
    WebMvcConfigurer koikiWebMvcResourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/koiki-web/**")
                        .addResourceLocations("classpath:/META-INF/resources/koiki-web/");
            }
        };
    }
}

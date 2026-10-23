package org.koikifw.reference.expense.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Exposes only the explicit SPA shell routes when its build is present. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnResource(resources = "classpath:/META-INF/resources/spa/index.html")
public class ReferenceSpaWebConfiguration implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/spa").setViewName("forward:/spa/index.html");
        registry.addViewController("/spa/").setViewName("forward:/spa/index.html");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                return true;
            }
        }).addPathPatterns("/spa", "/spa/", "/spa/index.html");
    }
}

package org.koikifw.buildsupport.vt.observer;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Non-distributed, external observation only. No routes or replacement application beans. */
@AutoConfiguration
@ConditionalOnProperty(name = "koiki.vt.observe.enabled", havingValue = "true")
public class VtObservationAutoConfiguration {
    @Bean(destroyMethod = "close")
    VtRequestObserver vtRequestObserver(Environment environment) throws java.io.IOException {
        return new VtRequestObserver(java.nio.file.Path.of(environment.getRequiredProperty("koiki.vt.observe.output")));
    }

    @Bean
    FilterRegistrationBean<VtRequestObserver> vtOuterObserver(VtRequestObserver observer) {
        var bean = new FilterRegistrationBean<>(observer);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 99);
        bean.setName("vtToolingOuterObserver");
        return bean;
    }

    @Bean
    WebMvcConfigurer vtMvcObserver(VtRequestObserver observer) {
        return new WebMvcConfigurer() {
            @Override public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(observer);
            }
        };
    }

    @Bean(destroyMethod = "close")
    VtResourceSampler vtResourceSampler(DataSource source, VtRequestObserver observer, Environment environment) {
        if (!(source instanceof HikariDataSource hikari)) {
            throw new IllegalStateException("D2 observer requires the existing Hikari DataSource");
        }
        return new VtResourceSampler(hikari, observer, environment);
    }
}

package org.koikifw.performance.fixture.configuration;

import org.koikifw.performance.fixture.adapter.outbound.persistence.PerformanceItem;
import org.koikifw.performance.fixture.adapter.outbound.persistence.PerformanceItemRepository;
import org.koikifw.performance.fixture.application.CreatePerformanceItemUseCase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Assembles the shared fixture without depending on a KOIKI runtime type. */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = PerformanceItem.class)
@EnableJpaRepositories(basePackageClasses = PerformanceItemRepository.class)
public class PerformanceFixtureConfiguration {

    @Bean
    CreatePerformanceItemUseCase createPerformanceItemUseCase(
            PerformanceItemRepository repository) {
        return new CreatePerformanceItemUseCase(repository);
    }
}

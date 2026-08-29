package org.koikifw.runtimeconsumer;

import java.util.stream.Stream;
import org.springframework.modulith.core.ApplicationModuleDetectionStrategy;
import org.springframework.modulith.core.ApplicationModuleInformation;
import org.springframework.modulith.core.JavaPackage;
import org.springframework.modulith.core.NamedInterfaces;

/** Test-scope Modulith strategy exposing only each module's domain.event package. */
public final class KoikiDomainEventDetectionStrategy
        implements ApplicationModuleDetectionStrategy {

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage basePackage) {
        return ApplicationModuleDetectionStrategy.directSubPackage()
                .getModuleBasePackages(basePackage);
    }

    @Override
    public NamedInterfaces detectNamedInterfaces(
            JavaPackage basePackage, ApplicationModuleInformation information) {
        return NamedInterfaces.builder(basePackage)
                .recursive()
                .matching("domain.event")
                .build();
    }
}

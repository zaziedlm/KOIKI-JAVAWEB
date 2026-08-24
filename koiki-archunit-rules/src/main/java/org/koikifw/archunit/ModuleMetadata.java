package org.koikifw.archunit;

import com.tngtech.archunit.core.domain.JavaPackage;
import java.util.Objects;
import java.util.Optional;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;
import org.jspecify.annotations.Nullable;

/** Architecture declaration read from an imported module root package. */
record ModuleMetadata(
        String name,
        ModuleTier tier,
        PersistenceTechnology persistence,
        PersistenceModel persistenceModel) {

    static Optional<ModuleMetadata> from(@Nullable JavaPackage modulePackage) {
        JavaPackage source = Objects.requireNonNull(
                modulePackage,
                "modulePackage must not be null");
        return source.tryGetAnnotationOfType(KoikiModule.class)
                .map(ModuleMetadata::from);
    }

    private static ModuleMetadata from(KoikiModule declaration) {
        return new ModuleMetadata(
                declaration.name(),
                declaration.tier(),
                declaration.persistence(),
                declaration.persistenceModel());
    }
}

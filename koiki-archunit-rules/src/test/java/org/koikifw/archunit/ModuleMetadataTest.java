package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

class ModuleMetadataTest {

    private static final String SIMPLE_PACKAGE =
            "org.koikifw.archunit.fixture.metadata.simple";
    private static final String RICH_PACKAGE =
            "org.koikifw.archunit.fixture.metadata.rich";
    private static final String UNDECLARED_PACKAGE =
            "org.koikifw.archunit.fixture.metadata.undeclared";

    @Test
    void readsEveryArchitectureDeclarationValueFromAnImportedPackage() {
        JavaClasses classes = new ClassFileImporter().importPackages(SIMPLE_PACKAGE, RICH_PACKAGE);

        ModuleMetadata simple = ModuleMetadata.from(classes.getPackage(SIMPLE_PACKAGE)).orElseThrow();
        ModuleMetadata rich = ModuleMetadata.from(classes.getPackage(RICH_PACKAGE)).orElseThrow();

        assertEquals("simple", simple.name());
        assertEquals(ModuleTier.SIMPLE, simple.tier());
        assertEquals(PersistenceTechnology.JPA, simple.persistence());
        assertEquals(PersistenceModel.SHARED, simple.persistenceModel());
        assertEquals("rich", rich.name());
        assertEquals(ModuleTier.RICH, rich.tier());
        assertEquals(PersistenceTechnology.MYBATIS, rich.persistence());
        assertEquals(PersistenceModel.SHARED, rich.persistenceModel());
    }

    @Test
    void representsAnUndeclaredModuleRootWithoutInventingDefaults() {
        JavaClasses classes = new ClassFileImporter().importPackages(UNDECLARED_PACKAGE);

        assertTrue(ModuleMetadata.from(classes.getPackage(UNDECLARED_PACKAGE)).isEmpty());
    }

    @Test
    void rejectsANullImportedPackageWithTheParameterName() {
        NullPointerException failure = assertThrows(
                NullPointerException.class,
                () -> ModuleMetadata.from(null));

        assertTrue(Objects.requireNonNull(failure.getMessage()).contains("modulePackage"));
    }
}

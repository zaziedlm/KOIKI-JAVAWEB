package org.koikifw.validation.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

class CustomerModuleContractTest {

    @Test
    void consumesTheContractFromAnExternalPackageAlongsideNullMarked() {
        Package customerPackage = CustomerModuleMarker.class.getPackage();
        KoikiModule declaration = customerPackage.getAnnotation(KoikiModule.class);

        assertNotNull(declaration);
        assertTrue(customerPackage.isAnnotationPresent(NullMarked.class));
        assertEquals("customer", declaration.name());
        assertEquals(ModuleTier.RICH, declaration.tier());
        assertEquals(PersistenceTechnology.JPA, declaration.persistence());
        assertEquals(PersistenceModel.SHARED, declaration.persistenceModel());
    }
}

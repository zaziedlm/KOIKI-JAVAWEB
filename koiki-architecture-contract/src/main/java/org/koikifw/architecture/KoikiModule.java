package org.koikifw.architecture;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the architecture characteristics of a KOIKI module root package.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface KoikiModule {

    /**
     * Returns the module name, which must match the module root package name.
     *
     * @return module name
     */
    String name();

    /**
     * Returns the module tier.
     *
     * @return module tier
     */
    ModuleTier tier();

    /**
     * Returns the persistence technology declared for the module.
     *
     * @return persistence technology
     */
    PersistenceTechnology persistence();

    /**
     * Returns the persistence model declared for the module.
     *
     * @return persistence model
     */
    PersistenceModel persistenceModel();
}

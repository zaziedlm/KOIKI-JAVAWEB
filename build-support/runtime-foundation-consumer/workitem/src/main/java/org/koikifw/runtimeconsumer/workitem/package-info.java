/** Tier 1 SIMPLE business module. */
@NullMarked
@KoikiModule(
        name = "workitem",
        tier = ModuleTier.SIMPLE,
        persistence = PersistenceTechnology.JPA,
        persistenceModel = PersistenceModel.SHARED)
package org.koikifw.runtimeconsumer.workitem;

import org.jspecify.annotations.NullMarked;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

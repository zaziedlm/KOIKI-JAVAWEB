/** Tier 2 RICH business module collaborating through a domain event. */
@NullMarked
@KoikiModule(
        name = "workreview",
        tier = ModuleTier.RICH,
        persistence = PersistenceTechnology.JPA,
        persistenceModel = PersistenceModel.SHARED)
package org.koikifw.runtimeconsumer.workreview;

import org.jspecify.annotations.NullMarked;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

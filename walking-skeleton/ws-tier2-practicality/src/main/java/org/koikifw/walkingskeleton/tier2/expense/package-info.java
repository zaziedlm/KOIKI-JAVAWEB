@NullMarked
@KoikiModule(
        name = "expense",
        tier = ModuleTier.RICH,
        persistence = PersistenceTechnology.JPA,
        persistenceModel = PersistenceModel.SHARED)
@ApplicationModule(allowedDependencies = "masterdata::events")
package org.koikifw.walkingskeleton.tier2.expense;

import dev.koiki.walkingskeleton.architecture.KoikiModule;
import dev.koiki.walkingskeleton.architecture.ModuleTier;
import dev.koiki.walkingskeleton.architecture.PersistenceModel;
import dev.koiki.walkingskeleton.architecture.PersistenceTechnology;
import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;

/** Reference-owned expense request and approval module. */
@NullMarked
@KoikiModule(
        name = "expense",
        tier = ModuleTier.RICH,
        persistence = PersistenceTechnology.JPA,
        persistenceModel = PersistenceModel.SHARED)
package org.koikifw.reference.expense;

import org.jspecify.annotations.NullMarked;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;


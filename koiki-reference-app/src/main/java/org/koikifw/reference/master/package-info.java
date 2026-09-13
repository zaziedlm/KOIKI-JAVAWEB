/** Reference-owned department and expense-category master module. */
@NullMarked
@KoikiModule(
        name = "master",
        tier = ModuleTier.SIMPLE,
        persistence = PersistenceTechnology.JPA,
        persistenceModel = PersistenceModel.SHARED)
package org.koikifw.reference.master;

import org.jspecify.annotations.NullMarked;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

/** Opt-in Reference-only read-only external rate check; no financial calculation or receipt operation. */
@NullMarked
@KoikiModule(name = "externalcheck", tier = ModuleTier.SIMPLE,
        persistence = PersistenceTechnology.JPA, persistenceModel = PersistenceModel.SHARED)
package org.koikifw.reference.externalcheck;

import org.jspecify.annotations.NullMarked;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

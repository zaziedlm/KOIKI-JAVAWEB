/** Reference-owned notification permit and append-only consumption foundation. */
@NullMarked
@KoikiModule(name = "notification", tier = ModuleTier.RICH,
        persistence = PersistenceTechnology.JPA, persistenceModel = PersistenceModel.SHARED)
package org.koikifw.reference.notification;

import org.jspecify.annotations.NullMarked;
import org.koikifw.architecture.KoikiModule;
import org.koikifw.architecture.ModuleTier;
import org.koikifw.architecture.PersistenceModel;
import org.koikifw.architecture.PersistenceTechnology;

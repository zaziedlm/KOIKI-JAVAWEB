package org.koikifw.walkingskeleton.tier2.masterdata.domain.event;

import java.util.Objects;
import java.util.UUID;

public record CategoryDeactivating(UUID categoryId) {

    public CategoryDeactivating {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
    }
}

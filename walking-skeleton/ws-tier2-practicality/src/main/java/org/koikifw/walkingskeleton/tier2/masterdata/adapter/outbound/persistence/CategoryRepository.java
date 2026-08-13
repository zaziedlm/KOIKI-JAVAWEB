package org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

public interface CategoryRepository extends Repository<Category, UUID> {

    Optional<Category> findById(UUID id);

    Category save(Category category);
}

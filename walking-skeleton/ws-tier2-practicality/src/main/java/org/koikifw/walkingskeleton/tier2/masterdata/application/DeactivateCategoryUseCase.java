package org.koikifw.walkingskeleton.tier2.masterdata.application;

import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence.Category;
import org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence.CategoryRepository;
import org.koikifw.walkingskeleton.tier2.masterdata.domain.event.CategoryDeactivating;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateCategoryUseCase {

    private final CategoryRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeactivateCategoryUseCase(
            CategoryRepository repository,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CategoryResult deactivate(UUID categoryId) {
        Category category = repository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "category not found: " + categoryId));
        if (!category.active()) {
            throw new IllegalStateException("category is already inactive: " + categoryId);
        }

        eventPublisher.publishEvent(new CategoryDeactivating(categoryId));
        category.setActive(false);
        repository.save(category);
        return new CategoryResult(category.id(), category.active());
    }

    public record CategoryResult(UUID categoryId, boolean active) {
    }
}

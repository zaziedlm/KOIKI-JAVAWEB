package org.koikifw.performance.fixture.application;

import java.util.UUID;
import org.koikifw.performance.fixture.adapter.outbound.persistence.PerformanceItem;
import org.koikifw.performance.fixture.adapter.outbound.persistence.PerformanceItemRepository;
import org.springframework.transaction.annotation.Transactional;

/** Executes the shared Tier 1 persistence workload. */
public class CreatePerformanceItemUseCase {

    private final PerformanceItemRepository repository;

    public CreatePerformanceItemUseCase(PerformanceItemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UUID create(String label) {
        UUID id = UUID.randomUUID();
        repository.save(new PerformanceItem(id, label));
        return id;
    }
}

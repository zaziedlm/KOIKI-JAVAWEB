package org.koikifw.performance.fixture.adapter.outbound.persistence;

import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Minimal Spring Data repository shared by both performance variants. */
public interface PerformanceItemRepository extends Repository<PerformanceItem, UUID> {

    PerformanceItem save(PerformanceItem item);
}

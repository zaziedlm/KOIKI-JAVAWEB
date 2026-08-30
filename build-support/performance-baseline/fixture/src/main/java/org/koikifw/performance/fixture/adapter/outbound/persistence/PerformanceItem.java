package org.koikifw.performance.fixture.adapter.outbound.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Persistence model used only by the performance fixture. */
@Entity
@Table(name = "perf_item")
public class PerformanceItem {

    @Id
    private UUID id;

    private String label = "";

    protected PerformanceItem() { }

    public PerformanceItem(UUID id, String label) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}

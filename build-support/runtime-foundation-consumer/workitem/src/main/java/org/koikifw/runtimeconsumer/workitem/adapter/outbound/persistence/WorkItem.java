package org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Behavior-free persistence model for the Tier 1 feature. */
@Entity
@Table(name = "kkbiz_work_item")
public class WorkItem {

    @Id
    private UUID id = UUID.randomUUID();

    private String label = "";

    protected WorkItem() {
    }

    public WorkItem(UUID id, String label) {
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

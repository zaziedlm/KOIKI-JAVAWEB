package org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ws_category")
public class Category {

    @Id
    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Category() {
        // JPA only
    }

    public Category(UUID id, String name, boolean active) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = requireName(name);
        this.active = active;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }
}

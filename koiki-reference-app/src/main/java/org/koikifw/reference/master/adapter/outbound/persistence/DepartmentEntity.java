package org.koikifw.reference.master.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Behavior-light persistence model for a Reference department. */
@Entity(name = "ReferenceDepartment")
@Table(name = "kkref_department")
public class DepartmentEntity {

    @Id
    @Column(name = "department_id", nullable = false, updatable = false)
    private UUID departmentId;

    @Column(name = "department_code", nullable = false, length = 100)
    private String departmentCode;

    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DepartmentEntity() {
        this.departmentId = new UUID(0L, 0L);
        this.departmentCode = "";
        this.departmentName = "";
        this.active = true;
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    public DepartmentEntity(
            UUID departmentId, String departmentCode, String departmentName, Instant now) {
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId");
        this.departmentCode = Objects.requireNonNull(departmentCode, "departmentCode");
        this.departmentName = Objects.requireNonNull(departmentName, "departmentName");
        this.active = true;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public void rename(String departmentName, Instant now) {
        this.departmentName = Objects.requireNonNull(departmentName, "departmentName");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public UUID departmentId() {
        return departmentId;
    }

    public long version() {
        return version;
    }
}

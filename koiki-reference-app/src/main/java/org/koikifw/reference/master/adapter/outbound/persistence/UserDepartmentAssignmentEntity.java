package org.koikifw.reference.master.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Reference-owned current department assignment for an immutable Framework user ID. */
@Entity(name = "ReferenceUserDepartmentAssignment")
@Table(name = "kkref_user_department_assignment")
public class UserDepartmentAssignmentEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserDepartmentAssignmentEntity() {
        this.userId = new UUID(0L, 0L);
        this.departmentId = new UUID(0L, 0L);
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    public UserDepartmentAssignmentEntity(UUID userId, UUID departmentId, Instant now) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public void changeDepartment(UUID departmentId, Instant now) {
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public long version() {
        return version;
    }
}

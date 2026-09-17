package org.koikifw.reference.master.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Behavior-light persistence model for a Reference expense category. */
@Entity(name = "ReferenceExpenseCategory")
@Table(name = "kkref_expense_category")
public class ExpenseCategoryEntity {

    @Id
    @Column(name = "expense_category_id", nullable = false, updatable = false)
    private UUID expenseCategoryId;

    @Column(name = "expense_category_code", nullable = false, length = 100)
    private String expenseCategoryCode;

    @Column(name = "expense_category_name", nullable = false, length = 200)
    private String expenseCategoryName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExpenseCategoryEntity() {
        this.expenseCategoryId = new UUID(0L, 0L);
        this.expenseCategoryCode = "";
        this.expenseCategoryName = "";
        this.active = true;
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    public ExpenseCategoryEntity(
            UUID expenseCategoryId,
            String expenseCategoryCode,
            String expenseCategoryName,
            Instant now) {
        this.expenseCategoryId = Objects.requireNonNull(expenseCategoryId, "expenseCategoryId");
        this.expenseCategoryCode = Objects.requireNonNull(expenseCategoryCode, "expenseCategoryCode");
        this.expenseCategoryName = Objects.requireNonNull(expenseCategoryName, "expenseCategoryName");
        this.active = true;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public void rename(String expenseCategoryName, Instant now) {
        this.expenseCategoryName = Objects.requireNonNull(expenseCategoryName, "expenseCategoryName");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public UUID expenseCategoryId() {
        return expenseCategoryId;
    }

    public long version() {
        return version;
    }
}

package org.koikifw.archunit.fixture.v1.violation.rich.domain.repository;

import org.koikifw.archunit.fixture.v1.violation.rich.domain.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadExpenseRepository extends JpaRepository<Expense, Long> {
}

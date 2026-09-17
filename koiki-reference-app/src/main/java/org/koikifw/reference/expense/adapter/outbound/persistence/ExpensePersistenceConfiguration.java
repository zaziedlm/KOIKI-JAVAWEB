package org.koikifw.reference.expense.adapter.outbound.persistence;

import org.koikifw.reference.expense.domain.model.ExpenseRequest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

/** Registers the expense shared Domain/JPA model. */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = ExpenseRequest.class)
class ExpensePersistenceConfiguration {
}


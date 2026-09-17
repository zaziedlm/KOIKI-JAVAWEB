package org.koikifw.reference.master.adapter.outbound.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

/** Registers the Reference master persistence model alongside Framework-owned entities. */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = DepartmentEntity.class)
class MasterPersistenceConfiguration {}

package org.koikifw.reference.master.adapter.outbound.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Master-owned department persistence boundary. */
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    boolean existsByDepartmentIdAndActiveTrue(UUID departmentId);
}

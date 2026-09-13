package org.koikifw.reference.master.adapter.outbound.persistence;

import java.util.UUID;
import org.koikifw.reference.master.application.dto.DepartmentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Master-owned department persistence boundary. */
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    boolean existsByDepartmentIdAndActiveTrue(UUID departmentId);

    @Query("""
            select new org.koikifw.reference.master.application.dto.DepartmentSummary(
                department.departmentId,
                department.departmentCode,
                department.departmentName,
                department.active,
                department.version)
            from ReferenceDepartment department
            where :search = ''
               or lower(department.departmentCode) like concat('%', :search, '%')
               or lower(department.departmentName) like concat('%', :search, '%')
            order by department.departmentCode
            """)
    Page<DepartmentSummary> findSummaries(String search, Pageable pageable);
}

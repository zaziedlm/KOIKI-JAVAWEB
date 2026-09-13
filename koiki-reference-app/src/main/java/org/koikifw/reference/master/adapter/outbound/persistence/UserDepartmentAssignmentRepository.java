package org.koikifw.reference.master.adapter.outbound.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Master-owned user-to-department assignment persistence boundary. */
public interface UserDepartmentAssignmentRepository
        extends JpaRepository<UserDepartmentAssignmentEntity, UUID> {

    @Query("""
            select count(assignment) > 0
            from ReferenceUserDepartmentAssignment assignment,
                 ReferenceDepartment department
            where assignment.userId = :userId
              and assignment.departmentId = :departmentId
              and department.departmentId = assignment.departmentId
              and department.active = true
            """)
    boolean existsActiveAssignment(
            @Param("userId") UUID userId,
            @Param("departmentId") UUID departmentId);
}

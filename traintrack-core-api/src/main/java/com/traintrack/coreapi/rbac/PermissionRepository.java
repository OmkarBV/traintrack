package com.traintrack.coreapi.rbac;

import com.traintrack.coreapi.domain.Permission;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    @Query(
            """
            select distinct p.code from Permission p
            join RolePermission rp on rp.permission = p
            join UserRole ur on ur.role = rp.role
            where ur.user.id = :userId
            """)
    Set<String> findCodesByUserId(@Param("userId") UUID userId);
}

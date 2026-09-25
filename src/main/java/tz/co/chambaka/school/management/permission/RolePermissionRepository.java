package tz.co.chambaka.school.management.permission;

import tz.co.chambaka.school.management.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole(Role role);

    boolean existsByRoleAndPermissionCode(Role role, String permissionCode);
}

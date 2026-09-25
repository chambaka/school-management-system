package tz.co.chambaka.school.management.permission;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppPermissionRepository extends JpaRepository<AppPermission, String> {
}

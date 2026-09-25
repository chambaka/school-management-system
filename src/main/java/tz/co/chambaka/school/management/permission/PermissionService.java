package tz.co.chambaka.school.management.permission;

import tz.co.chambaka.school.management.model.enums.Role;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService implements ApplicationRunner {

    private final AppPermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(
            AppPermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public void seed() {
        PermissionCatalog.definitions().forEach((code, description) -> {
            AppPermission permission = permissionRepository.findById(code).orElseGet(AppPermission::new);
            permission.setCode(code);
            permission.setDescription(description);
            permissionRepository.save(permission);
        });
        for (Role role : Role.values()) {
            for (String code : PermissionCatalog.forRole(role)) {
                if (!rolePermissionRepository.existsByRoleAndPermissionCode(role, code)) {
                    RolePermission link = new RolePermission();
                    link.setRole(role);
                    link.setPermissionCode(code);
                    rolePermissionRepository.save(link);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<String> codesFor(Role role) {
        List<String> stored = rolePermissionRepository.findByRole(role).stream()
                .map(RolePermission::getPermissionCode)
                .toList();
        return stored.isEmpty() ? PermissionCatalog.forRole(role) : stored;
    }
}

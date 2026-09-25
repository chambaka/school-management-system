package tz.co.chambaka.school.management.permission;

import tz.co.chambaka.school.management.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock AppPermissionRepository permissionRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @InjectMocks PermissionService service;

    @Test
    void seedsCatalogAndReadsStoredCodes() throws Exception {
        when(permissionRepository.findById(any())).thenReturn(Optional.empty());
        when(permissionRepository.save(any(AppPermission.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rolePermissionRepository.existsByRoleAndPermissionCode(any(), any())).thenReturn(false);
        when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));
        service.run(new DefaultApplicationArguments());

        RolePermission link = new RolePermission();
        link.setRole(Role.HEADMASTER);
        link.setPermissionCode(PermissionCatalog.EXAM_APPROVE);
        when(rolePermissionRepository.findByRole(Role.HEADMASTER)).thenReturn(List.of(link));
        assertThat(service.codesFor(Role.HEADMASTER)).contains(PermissionCatalog.EXAM_APPROVE);
        when(rolePermissionRepository.findByRole(Role.STAFF)).thenReturn(List.of());
        assertThat(service.codesFor(Role.STAFF)).isEmpty();
        assertThat(PermissionCatalog.roles()).contains(Role.SCHOOL_ADMIN, Role.INVIGILATOR);
        assertThat(PermissionCatalog.forRole(Role.SCHOOL_ADMIN)).contains(PermissionCatalog.PEOPLE_MANAGE);
        assertThat(PermissionCatalog.forRole(Role.ACADEMIC_MASTER)).contains(
                PermissionCatalog.EXAM_APPROVE, PermissionCatalog.TIMETABLE_MANAGE);
        assertThat(PermissionCatalog.forRole(Role.INVIGILATOR)).contains(PermissionCatalog.EXAM_INVIGILATE);
        assertThat(PermissionCatalog.forRole(Role.SUPER_ADMIN)).hasSize(PermissionCatalog.definitions().size());
        var ctor = PermissionCatalog.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }
}

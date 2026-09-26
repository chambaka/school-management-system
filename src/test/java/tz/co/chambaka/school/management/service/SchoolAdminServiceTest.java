package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.admin.CreateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.admin.UpdateSchoolAdminRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private CampusService campusService;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private TenantService tenantService;
    @InjectMocks
    private SchoolAdminService service;

    @Test
    void listGetCreateUpdate() {
        User admin = Fixtures.user(5L, Role.HEADMASTER);
        when(userRepository.findBySchoolIdAndRoleIn(1L, Role.schoolOfficers(), PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(admin)));
        assertThat(service.list(1L, PageRequest.of(0, 10)).totalElements()).isEqualTo(1);

        when(userRepository.findByIdAndSchoolIdAndRoleIn(5L, 1L, Role.schoolOfficers())).thenReturn(Optional.of(admin));
        assertThat(service.get(1L, 5L).email()).isEqualTo(admin.getEmail());

        when(userAccountService.create(1L, "Asha", "asha@x.com", "HaloCampus1!", Role.ACCOUNTANT, "07"))
                .thenReturn(admin);
        when(campusService.requirePrimary(1L)).thenReturn(Fixtures.campus());
        var created = service.create(1L, new CreateSchoolAdminRequest("Asha", "asha@x.com", "HaloCampus1!", "07", Role.ACCOUNTANT));
        assertThat(created.id()).isEqualTo(5L);
        assertThat(admin.getCampusId()).isEqualTo(Fixtures.CAMPUS_ID);

        service.update(1L, 5L, new UpdateSchoolAdminRequest("New", "08", false, Role.ACCOUNTANT));
        assertThat(admin.getName()).isEqualTo("New");
        assertThat(admin.isEnabled()).isFalse();
        assertThat(admin.getRole()).isEqualTo(Role.ACCOUNTANT);
        assertThatThrownBy(() -> service.update(1L, 5L, new UpdateSchoolAdminRequest(null, null, null, Role.TEACHER)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRejectsWeakPasswordAndMissingAdmin() {
        assertThatThrownBy(() -> service.create(1L, new CreateSchoolAdminRequest("Asha", "asha@x.com", "weak", null, Role.HEADMASTER)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(1L, new CreateSchoolAdminRequest("Asha", "asha@x.com", "HaloCampus1!", null, Role.TEACHER)))
                .isInstanceOf(BusinessException.class);
        when(userRepository.findByIdAndSchoolIdAndRoleIn(9L, 1L, Role.schoolOfficers())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assertCanManageAllowsPlatformAndHeadmaster() {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        service.assertCanManage(1L, Fixtures.principal(Role.SUPER_ADMIN));

        service.assertCanManage(1L, Fixtures.principal(Role.HEADMASTER));
        when(tenantService.requireSchoolInTenant(Fixtures.TENANT_ID, 1L)).thenReturn(Fixtures.school());
        service.assertCanManage(1L, Fixtures.principal(Role.ORGANIZATION_ADMIN));
        assertThatThrownBy(() -> service.assertCanManage(9L, Fixtures.principal(Role.HEADMASTER)))
                .isInstanceOf(ApiException.class);

        when(schoolRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assertCanManage(9L, Fixtures.principal(Role.SUPER_ADMIN)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.assertCanManage(1L, Fixtures.principal(Role.TEACHER)))
                .isInstanceOf(ApiException.class);
    }
}
